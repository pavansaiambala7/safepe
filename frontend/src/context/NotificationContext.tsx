import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { useAuth } from '@clerk/react';
import { audioAlerts } from '../utils/audioAlert';

export type NotificationType = 'SUCCESS' | 'FRAUD_ALERT' | 'ESCROW_REFUND' | 'REFUND_INITIATED' | 'SECURITY' | 'REMINDER';

export interface FraudDetail {
  threatCategory: string;
  similarityMatch: number;
  matchedPatternDescription: string;
  merchantName: string;
  merchantUpi: string;
  reportedCount: number;
  action: 'BLOCK' | 'FLAG_VERIFICATION' | 'ALLOW';
  refundId?: string;
  refundAmount?: number;
}

export interface NotificationItem {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  amount?: number;
  upiId?: string;
  referenceId?: string;
  fraudDetails?: FraudDetail;
}

interface NotificationContextType {
  notifications: NotificationItem[];
  unreadCount: number;
  activeToast: NotificationItem | null;
  isSoundEnabled: boolean;
  sseConnected: boolean;
  toggleSound: () => void;
  addNotification: (n: Omit<NotificationItem, 'id' | 'timestamp' | 'isRead'>) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  deleteNotification: (id: string) => void;
  clearAll: () => void;
  closeToast: () => void;
}

// Notification history is stored per user, so a second account signed in on the
// same browser never sees the first account's notifications. Older builds used
// shared keys (and received every user's events); those are discarded.
const LEGACY_STORAGE_KEYS = ['safepe_notifications', 'safepe_notifications_v2'];
const storageKeyFor = (userId: string | null | undefined) =>
  userId ? `safepe_notifications_v3_${userId}` : null;

const readSaved = (key: string | null): NotificationItem[] => {
  if (!key) return [];
  try {
    LEGACY_STORAGE_KEYS.forEach(k => localStorage.removeItem(k));
    const saved = localStorage.getItem(key);
    return saved ? JSON.parse(saved) : [];
  } catch {
    return [];
  }
};

const STREAM_URL = '/api/v1/notifications/stream';
const MAX_RETRY_DELAY_MS = 30000;

/** Splits a Server-Sent Events chunk into complete events, returning the unparsed remainder. */
const parseSseEvents = (buffer: string): { events: { name: string; data: string }[]; rest: string } => {
  const events: { name: string; data: string }[] = [];
  const blocks = buffer.split(/\r?\n\r?\n/);
  const rest = blocks.pop() ?? '';
  for (const block of blocks) {
    let name = 'message';
    const data: string[] = [];
    for (const line of block.split(/\r?\n/)) {
      if (line.startsWith('event:')) name = line.slice(6).trim();
      else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''));
    }
    if (data.length) events.push({ name, data: data.join('\n') });
  }
  return { events, rest };
};

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isSignedIn, userId, getToken } = useAuth();
  const storageKey = storageKeyFor(isSignedIn ? userId : null);

  const [notifications, setNotifications] = useState<NotificationItem[]>(() => readSaved(storageKey));
  const [owner, setOwner] = useState<string | null>(storageKey);

  // Switch to the signed-in user's own history as soon as the user changes
  // (React's "adjust state during render" pattern, so no frame shows the old list).
  if (owner !== storageKey) {
    setOwner(storageKey);
    setNotifications(readSaved(storageKey));
  }

  const [activeToast, setActiveToast] = useState<NotificationItem | null>(null);
  const [isSoundEnabled, setIsSoundEnabled] = useState<boolean>(() => audioAlerts.isEnabled());
  const [sseConnected, setSseConnected] = useState(false);

  useEffect(() => {
    if (!storageKey || owner !== storageKey) return;
    try {
      localStorage.setItem(storageKey, JSON.stringify(notifications));
    } catch {
      /* storage unavailable (private mode) - keep notifications in memory only */
    }
  }, [notifications, storageKey, owner]);

  // ── Per-user SSE stream ───────────────────────────────────────────────
  // EventSource cannot send an Authorization header, so the stream is read with
  // fetch. The gateway verifies the token and the service sends this user only
  // their own events.
  const getTokenRef = useRef(getToken);
  getTokenRef.current = getToken;
  const handleEventRef = useRef<(name: string, data: string) => void>(() => {});
  handleEventRef.current = (name, data) => {
    if (name === 'connected') {
      setSseConnected(true);
      return;
    }
    if (name !== 'notification') return;
    try {
      const event = JSON.parse(data);
      const fraudDetails: FraudDetail | undefined =
        (event.threatCategory || event.refundId) ? {
          threatCategory: event.threatCategory || '',
          similarityMatch: event.similarityMatch || 0,
          matchedPatternDescription: event.matchedPatternDescription || '',
          merchantName: event.merchantName || '',
          merchantUpi: event.merchantUpi || event.upiId || '',
          reportedCount: event.reportedCount || 0,
          action: event.action || 'ALLOW',
          refundId: event.refundId,
          refundAmount: event.refundAmount
        } : undefined;

      addNotification({
        type: event.type as NotificationType,
        title: event.title,
        message: event.message,
        amount: event.amount,
        upiId: event.upiId,
        referenceId: event.referenceId,
        fraudDetails
      });
    } catch (err) {
      console.warn('[SafePe SSE] Failed to parse notification:', err);
    }
  };

  useEffect(() => {
    if (!isSignedIn || !userId) {
      setSseConnected(false);
      return;
    }

    let stopped = false;
    let controller: AbortController | null = null;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;
    let failures = 0;

    const connect = async () => {
      controller = new AbortController();
      let connected = false;
      try {
        const token = await getTokenRef.current();
        const res = await fetch(STREAM_URL, {
          headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
          signal: controller.signal
        });
        if (!res.ok || !res.body) throw new Error(`stream returned HTTP ${res.status}`);

        const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
        let buffer = '';
        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          const parsed = parseSseEvents(buffer + value);
          buffer = parsed.rest;
          parsed.events.forEach(e => {
            if (e.name === 'connected') connected = true;
            handleEventRef.current(e.name, e.data);
          });
        }
      } catch (err) {
        if (!stopped) console.warn('[SafePe SSE] Stream error:', err);
      }
      if (stopped) return;
      failures = connected ? 0 : failures + 1;

      // The server closes the stream every 30 minutes; reconnect with a fresh
      // token, backing off only when connecting keeps failing.
      setSseConnected(false);
      const delay = failures === 0 ? 1000 : Math.min(1000 * 2 ** failures, MAX_RETRY_DELAY_MS);
      retryTimer = setTimeout(connect, delay);
    };

    connect();

    return () => {
      stopped = true;
      clearTimeout(retryTimer);
      controller?.abort();
      setSseConnected(false);
    };
  }, [isSignedIn, userId]);

  const toggleSound = () => {
    const newState = audioAlerts.toggleSound();
    setIsSoundEnabled(newState);
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const addNotification = (n: Omit<NotificationItem, 'id' | 'timestamp' | 'isRead'>) => {
    const newNotif: NotificationItem = {
      ...n,
      id: `notif-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
      timestamp: new Date().toISOString(),
      isRead: false
    };

    // Play synthesized acoustic chime for this notification type
    audioAlerts.playSoundForType(n.type);

    setNotifications(prev => [newNotif, ...prev]);
    setActiveToast(newNotif);

    // Auto-dismiss toast after 6 seconds
    setTimeout(() => {
      setActiveToast(current => (current?.id === newNotif.id ? null : current));
    }, 6000);
  };

  const markAsRead = (id: string) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
  };

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
  };

  const deleteNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  };

  const clearAll = () => {
    setNotifications([]);
  };

  const closeToast = () => {
    setActiveToast(null);
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        activeToast,
        isSoundEnabled,
        sseConnected,
        toggleSound,
        addNotification,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        clearAll,
        closeToast
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};
