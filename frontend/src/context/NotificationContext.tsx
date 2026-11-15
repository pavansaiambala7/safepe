import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
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

const STORAGE_KEY = 'safepe_notifications_v2';

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    // v2: earlier builds seeded demo notifications under 'safepe_notifications';
    // drop those so users only ever see their real notifications.
    try {
      localStorage.removeItem('safepe_notifications');
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [activeToast, setActiveToast] = useState<NotificationItem | null>(null);
  const [isSoundEnabled, setIsSoundEnabled] = useState<boolean>(() => audioAlerts.isEnabled());
  const [sseConnected, setSseConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(notifications));
    } catch {
      /* storage unavailable (private mode) - keep notifications in memory only */
    }
  }, [notifications]);

  // ── SSE Connection to Backend Kafka Notification Stream ──────────────
  useEffect(() => {
    let retryTimer: ReturnType<typeof setTimeout>;
    let retryCount = 0;
    const MAX_RETRIES = 10;

    const connectSSE = () => {
      // Close existing connection if any
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }

      const eventSource = new EventSource('/api/v1/public/notifications/stream');
      eventSourceRef.current = eventSource;

      eventSource.addEventListener('connected', () => {
        console.log('[SafePe SSE] Connected to notification stream');
        setSseConnected(true);
        retryCount = 0; // Reset retry counter on successful connection
      });

      eventSource.addEventListener('notification', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SafePe SSE] Received notification:', data.type, data.title);

          // Map backend NotificationEvent to frontend NotificationItem
          const notifType = data.type as NotificationType;
          const fraudDetails: FraudDetail | undefined =
            (data.threatCategory || data.refundId) ? {
              threatCategory: data.threatCategory || '',
              similarityMatch: data.similarityMatch || 0,
              matchedPatternDescription: data.matchedPatternDescription || '',
              merchantName: data.merchantName || '',
              merchantUpi: data.merchantUpi || data.upiId || '',
              reportedCount: data.reportedCount || 0,
              action: data.action || 'ALLOW',
              refundId: data.refundId,
              refundAmount: data.refundAmount
            } : undefined;

          addNotification({
            type: notifType,
            title: data.title,
            message: data.message,
            amount: data.amount,
            upiId: data.upiId,
            referenceId: data.referenceId,
            fraudDetails
          });
        } catch (err) {
          console.warn('[SafePe SSE] Failed to parse notification:', err);
        }
      });

      eventSource.onerror = () => {
        setSseConnected(false);
        eventSource.close();
        eventSourceRef.current = null;

        // Exponential backoff retry
        if (retryCount < MAX_RETRIES) {
          const delay = Math.min(1000 * Math.pow(2, retryCount), 30000);
          retryCount++;
          console.log(`[SafePe SSE] Reconnecting in ${delay}ms (attempt ${retryCount}/${MAX_RETRIES})...`);
          retryTimer = setTimeout(connectSSE, delay);
        } else {
          console.log('[SafePe SSE] Max retries reached. SSE notifications offline.');
        }
      };
    };

    connectSSE();

    return () => {
      clearTimeout(retryTimer);
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
