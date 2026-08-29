import React, { useState, useRef, useEffect } from 'react';
import {
  Bell,
  CheckCircle2,
  ShieldAlert,
  ShieldCheck,
  RotateCcw,
  Lock,
  X,
  ChevronDown,
  ChevronUp,
  Trash2,
  CheckCheck,
  Zap,
  AlertTriangle,
  ExternalLink,
  Info,
  Volume2,
  VolumeX,
  Hourglass,
  Wifi,
  WifiOff
} from 'lucide-react';
import { useNotifications } from '../context/NotificationContext';
import type { NotificationItem, NotificationType } from '../context/NotificationContext';

export default function NotificationBell() {
  const {
    notifications,
    unreadCount,
    activeToast,
    isSoundEnabled,
    sseConnected,
    toggleSound,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearAll,
    closeToast,
    simulateSuccess,
    simulateFraudAndRefund
  } = useNotifications();

  const [isOpen, setIsOpen] = useState(false);
  const [filter, setFilter] = useState<'ALL' | 'SUCCESS' | 'FRAUD_ESCROW' | 'REFUNDS' | 'SECURITY'>('ALL');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [isRinging, setIsRinging] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  // Trigger bell shake animation on new toast alert
  useEffect(() => {
    if (activeToast) {
      setIsRinging(true);
      const timer = setTimeout(() => setIsRinging(false), 1200);
      return () => clearTimeout(timer);
    }
  }, [activeToast]);

  // Close panel on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const togglePanel = () => {
    setIsOpen(!isOpen);
  };

  const toggleExpand = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedId(expandedId === id ? null : id);
    markAsRead(id);
  };

  const filteredNotifications = notifications.filter(n => {
    if (filter === 'ALL') return true;
    if (filter === 'SUCCESS') return n.type === 'SUCCESS';
    if (filter === 'FRAUD_ESCROW') return n.type === 'FRAUD_ALERT';
    if (filter === 'REFUNDS') return n.type === 'ESCROW_REFUND' || n.type === 'REFUND_INITIATED';
    if (filter === 'SECURITY') return n.type === 'SECURITY';
    if (filter === 'REMINDERS') return n.type === 'REMINDER';
    return true;
  });

  const formatTimestamp = (iso: string) => {
    const date = new Date(iso);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
  };

  const getIcon = (type: NotificationType) => {
    switch (type) {
      case 'SUCCESS':
        return <CheckCircle2 size={20} color="#10b981" />;
      case 'FRAUD_ALERT':
        return <ShieldAlert size={20} color="#ef4444" />;
      case 'ESCROW_REFUND':
        return <RotateCcw size={20} color="#8b5cf6" />;
      case 'REFUND_INITIATED':
        return <Hourglass size={20} color="#f59e0b" />;
      case 'SECURITY':
        return <Lock size={20} color="#3b82f6" />;
      case 'REMINDER':
        return <Zap size={20} color="#0284c7" />;
      default:
        return <Bell size={20} color="#059669" />;
    }
  };

  const getTypeTheme = (type: NotificationType) => {
    switch (type) {
      case 'SUCCESS':
        return {
          bg: '#10b98115',
          border: 'rgba(16, 185, 129, 0.3)',
          badgeBg: '#10b98120',
          badgeColor: '#059669',
          label: 'SUCCESS'
        };
      case 'FRAUD_ALERT':
        return {
          bg: '#ef444415',
          border: 'rgba(239, 68, 68, 0.4)',
          badgeBg: '#ef444425',
          badgeColor: '#dc2626',
          label: 'FRAUD BLOCKED'
        };
      case 'ESCROW_REFUND':
        return {
          bg: '#8b5cf615',
          border: 'rgba(139, 92, 246, 0.35)',
          badgeBg: '#8b5cf625',
          badgeColor: '#7c3aed',
          label: 'REFUND COMPLETED'
        };
      case 'REFUND_INITIATED':
        return {
          bg: '#f59e0b15',
          border: 'rgba(245, 158, 11, 0.35)',
          badgeBg: '#f59e0b25',
          badgeColor: '#d97706',
          label: 'REFUND INITIATED'
        };
      case 'SECURITY':
        return {
          bg: '#3b82f615',
          border: 'rgba(59, 130, 246, 0.3)',
          badgeBg: '#3b82f620',
          badgeColor: '#2563eb',
          label: 'VAULT'
        };
      case 'REMINDER':
        return {
          bg: '#0284c715',
          border: 'rgba(2, 132, 199, 0.35)',
          badgeBg: '#0284c725',
          badgeColor: '#0284c7',
          label: 'DUE REMINDER'
        };
      default:
        return {
          bg: 'rgba(16, 185, 129, 0.1)',
          border: 'rgba(16, 185, 129, 0.25)',
          badgeBg: 'rgba(16, 185, 129, 0.15)',
          badgeColor: '#059669',
          label: 'ALERT'
        };
    }
  };

  return (
    <div style={{ position: 'relative' }} ref={panelRef}>
      {/* Bell Icon Trigger */}
      <button
        onClick={togglePanel}
        className={`bell-btn ${isRinging ? 'ring-animation' : ''}`}
        aria-label="Notifications"
        style={{
          background: isOpen ? '#10b98125' : '#10b98115',
          border: '2px solid #10b981',
          borderRadius: '50%',
          width: 44,
          height: 44,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          position: 'relative',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          boxShadow: '0 2px 10px rgba(16, 185, 129, 0.25)'
        }}
      >
        <Bell size={22} color="#059669" strokeWidth={2.4} />

        {unreadCount > 0 && (
          <span
            style={{
              position: 'absolute',
              top: -3,
              right: -3,
              background: 'linear-gradient(135deg, #ef4444, #dc2626)',
              color: 'white',
              fontSize: 11,
              fontWeight: 800,
              borderRadius: '12px',
              minWidth: 20,
              height: 20,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '0 4px',
              border: '2px solid white',
              boxShadow: '0 2px 6px rgba(239, 68, 68, 0.5)',
              animation: 'pulse 2s infinite'
            }}
          >
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Slide-in Floating Toast for Real-Time Alerts */}
      {activeToast && (
        <div
          className="toast-container"
          style={{
            position: 'fixed',
            top: 90,
            right: 24,
            width: 'calc(100vw - 48px)',
            maxWidth: 420,
            background: 'rgba(255, 255, 255, 0.98)',
            backdropFilter: 'blur(20px)',
            borderRadius: 16,
            padding: 16,
            boxShadow: '0 12px 40px rgba(0,0,0,0.15), 0 0 0 1px var(--color-border)',
            borderLeft: `5px solid ${
              activeToast.type === 'FRAUD_ALERT' ? '#ef4444' :
              activeToast.type === 'ESCROW_REFUND' ? '#8b5cf6' :
              activeToast.type === 'REFUND_INITIATED' ? '#f59e0b' :
              '#10b981'
            }`,
            zIndex: 9999,
            animation: 'slideInRight 0.4s cubic-bezier(0.16, 1, 0.3, 1)'
          }}
        >
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <div style={{ padding: 6, borderRadius: '50%', background: getTypeTheme(activeToast.type).bg }}>
              {getIcon(activeToast.type)}
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                <span style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', color: getTypeTheme(activeToast.type).badgeColor }}>
                  {getTypeTheme(activeToast.type).label}
                </span>
                <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>Just now</span>
              </div>
              <h4 style={{ fontSize: 14, fontWeight: 700, margin: '0 0 4px 0', color: 'var(--color-text-primary)' }}>
                {activeToast.title}
              </h4>
              <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', margin: 0, lineHeight: 1.4 }}>
                {activeToast.message}
              </p>
            </div>
            <button
              onClick={closeToast}
              style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)', padding: 4 }}
            >
              <X size={16} />
            </button>
          </div>
        </div>
      )}

      {/* Notification Dropdown Panel */}
      {isOpen && (
        <div
          className="notification-panel"
          style={{
            position: 'absolute',
            top: 56,
            right: 0,
            width: 'calc(100vw - 32px)',
            maxWidth: 440,
            maxHeight: '85vh',
            background: 'rgba(255, 255, 255, 0.98)',
            backdropFilter: 'blur(25px)',
            borderRadius: 20,
            boxShadow: '0 20px 50px rgba(0,0,0,0.18), 0 0 0 1px var(--color-border)',
            border: '1px solid var(--color-border)',
            zIndex: 1000,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            animation: 'fadeInScale 0.25s ease-out'
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: '18px 20px',
              borderBottom: '1px solid var(--color-border)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: 'linear-gradient(to right, #f8fafc, #ffffff)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ background: '#10b98120', padding: 8, borderRadius: 12 }}>
                <Bell size={18} color="#10b981" />
              </div>
              <div>
                <h3 style={{ fontSize: 16, fontWeight: 700, margin: 0, color: 'var(--color-text-primary)' }}>
                  Notifications
                </h3>
                <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                  {unreadCount > 0 ? `${unreadCount} unread alerts` : 'All caught up!'}
                  {sseConnected ? (
                    <span title="Live stream connected" style={{ display: 'inline-flex', alignItems: 'center', gap: 2, color: '#10b981' }}>
                      <Wifi size={10} /> Live
                    </span>
                  ) : (
                    <span title="Live stream disconnected" style={{ display: 'inline-flex', alignItems: 'center', gap: 2, color: '#94a3b8' }}>
                      <WifiOff size={10} />
                    </span>
                  )}
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <button
                onClick={toggleSound}
                title={isSoundEnabled ? "Notification sound is ON (Click to mute)" : "Notification sound is MUTED (Click to unmute)"}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 4,
                  padding: '6px 8px',
                  borderRadius: 8,
                  border: '1px solid var(--color-border)',
                  background: isSoundEnabled ? '#10b98115' : '#f1f5f9',
                  color: isSoundEnabled ? '#059669' : '#94a3b8',
                  fontSize: 11,
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                {isSoundEnabled ? <Volume2 size={15} color="#059669" /> : <VolumeX size={15} color="#94a3b8" />}
              </button>

              {unreadCount > 0 && (
                <button
                  onClick={markAllAsRead}
                  title="Mark all as read"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                    padding: '6px 10px',
                    borderRadius: 8,
                    border: '1px solid var(--color-border)',
                    background: 'white',
                    fontSize: 11,
                    fontWeight: 600,
                    cursor: 'pointer',
                    color: 'var(--color-text-primary)'
                  }}
                >
                  <CheckCheck size={14} color="#10b981" /> Read
                </button>
              )}
              {notifications.length > 0 && (
                <button
                  onClick={clearAll}
                  title="Clear all"
                  style={{
                    padding: '6px 8px',
                    borderRadius: 8,
                    border: '1px solid var(--color-border)',
                    background: 'white',
                    cursor: 'pointer',
                    color: 'var(--color-text-secondary)'
                  }}
                >
                  <Trash2 size={14} />
                </button>
              )}
              <button
                onClick={() => setIsOpen(false)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  padding: 4,
                  color: 'var(--color-text-secondary)'
                }}
              >
                <X size={18} />
              </button>
            </div>
          </div>

          {/* Simulation Action Bar */}
          <div
            style={{
              padding: '10px 16px',
              background: '#f1f5f9',
              borderBottom: '1px solid var(--color-border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 8
            }}
          >
            <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
              <Zap size={13} color="#f59e0b" /> TEST ALERTS:
            </span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                onClick={() => simulateSuccess(1200, 'starbucks@okaxis')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 12,
                  border: '1px solid #10b981',
                  background: '#10b98115',
                  color: '#059669',
                  fontSize: 10,
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                + Success
              </button>
              <button
                onClick={() => simulateFraudAndRefund(4500, 'lottery.winner881@ybl')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 12,
                  border: '1px solid #ef4444',
                  background: '#ef444415',
                  color: '#dc2626',
                  fontSize: 10,
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                + Fraud & Refund
              </button>
              <button
                onClick={async () => {
                  try {
                    await fetch('/api/v1/bills/run-reminders', { method: 'POST' });
                  } catch (e) {
                    console.warn('Manual bill reminder API error:', e);
                  }
                }}
                style={{
                  padding: '4px 10px',
                  borderRadius: 12,
                  border: '1px solid #0284c7',
                  background: '#0284c715',
                  color: '#0284c7',
                  fontSize: 10,
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                + Bill Due
              </button>
            </div>
          </div>

          {/* Filter Tabs */}
          <div
            style={{
              display: 'flex',
              padding: '8px 16px',
              gap: 6,
              borderBottom: '1px solid var(--color-border)',
              background: 'white',
              overflowX: 'auto'
            }}
          >
            {[
              { key: 'ALL', label: 'All' },
              { key: 'SUCCESS', label: 'Payments' },
              { key: 'REMINDERS', label: 'Reminders' },
              { key: 'FRAUD_ESCROW', label: 'Fraud' },
              { key: 'REFUNDS', label: 'Refunds' },
              { key: 'SECURITY', label: 'Vault' }
            ].map(tab => (
              <button
                key={tab.key}
                onClick={() => setFilter(tab.key as any)}
                style={{
                  padding: '5px 12px',
                  borderRadius: 16,
                  border: 'none',
                  fontSize: 11,
                  fontWeight: 600,
                  cursor: 'pointer',
                  whiteSpace: 'nowrap',
                  background: filter === tab.key ? 'var(--color-primary)' : '#f1f5f9',
                  color: filter === tab.key ? 'white' : 'var(--color-text-secondary)',
                  transition: 'all 0.2s'
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Notifications List Body */}
          <div style={{ flex: 1, overflowY: 'auto', maxHeight: '52vh', padding: '12px 16px' }}>
            {filteredNotifications.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--color-text-secondary)' }}>
                <ShieldCheck size={40} color="#10b981" style={{ opacity: 0.5, marginBottom: 8 }} />
                <p style={{ fontSize: 14, fontWeight: 600, margin: 0 }}>No notifications in this tab</p>
                <p style={{ fontSize: 12, marginTop: 4 }}>New transaction & fraud alerts will appear here in real-time.</p>
              </div>
            ) : (
              filteredNotifications.map(n => {
                const theme = getTypeTheme(n.type);
                const isExpanded = expandedId === n.id;

                return (
                  <div
                    key={n.id}
                    onClick={() => markAsRead(n.id)}
                    style={{
                      marginBottom: 12,
                      padding: 14,
                      borderRadius: 14,
                      background: n.isRead ? 'white' : theme.bg,
                      border: `1px solid ${n.isRead ? 'var(--color-border)' : theme.border}`,
                      boxShadow: n.isRead ? 'none' : '0 4px 14px rgba(0,0,0,0.03)',
                      transition: 'all 0.2s ease',
                      position: 'relative',
                      cursor: 'pointer'
                    }}
                  >
                    {/* Top Row: Type Pill, Timestamp, Unread Indicator */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span
                          style={{
                            padding: '2px 8px',
                            borderRadius: 6,
                            fontSize: 10,
                            fontWeight: 800,
                            background: theme.badgeBg,
                            color: theme.badgeColor,
                            letterSpacing: '0.03em'
                          }}
                        >
                          {theme.label}
                        </span>
                        {!n.isRead && (
                          <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#ef4444', display: 'inline-block' }} />
                        )}
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                          {formatTimestamp(n.timestamp)}
                        </span>
                        <button
                          onClick={(e) => { e.stopPropagation(); deleteNotification(n.id); }}
                          style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#94a3b8', padding: 2 }}
                        >
                          <X size={13} />
                        </button>
                      </div>
                    </div>

                    {/* Main Content */}
                    <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                      <div style={{ marginTop: 2 }}>{getIcon(n.type)}</div>
                      <div style={{ flex: 1 }}>
                        <h4 style={{ fontSize: 13, fontWeight: 700, margin: '0 0 3px 0', color: 'var(--color-text-primary)' }}>
                          {n.title}
                        </h4>
                        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', margin: 0, lineHeight: 1.4 }}>
                          {n.message}
                        </p>

                        {/* Extra Metadata Chips */}
                        <div style={{ display: 'flex', gap: 6, marginTop: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                          {n.amount && (
                            <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--color-text-primary)', background: '#f1f5f9', padding: '2px 8px', borderRadius: 6 }}>
                              ₹{n.amount.toLocaleString('en-IN')}
                            </span>
                          )}
                          {n.upiId && (
                            <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', background: '#f8fafc', border: '1px solid var(--color-border)', padding: '2px 6px', borderRadius: 6 }}>
                              {n.upiId}
                            </span>
                          )}
                          {n.referenceId && (
                            <span style={{ fontSize: 10, color: '#64748b', background: '#f8fafc', padding: '2px 6px', borderRadius: 6 }}>
                              Ref: {n.referenceId}
                            </span>
                          )}
                        </div>

                        {/* Expandable AI Fraud & Escrow Breakdown */}
                        {n.fraudDetails && (
                          <div style={{ marginTop: 10 }}>
                            <button
                              onClick={(e) => toggleExpand(n.id, e)}
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 4,
                                background: 'transparent',
                                border: 'none',
                                color: n.type === 'FRAUD_ALERT' ? '#dc2626' : '#7c3aed',
                                fontSize: 11,
                                fontWeight: 700,
                                cursor: 'pointer',
                                padding: 0
                              }}
                            >
                              {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                              {isExpanded ? 'Hide AI Evidence & Escrow Proof' : 'View AI Reasoning & Escrow Refund Proof'}
                            </button>

                            {isExpanded && (
                              <div
                                style={{
                                  marginTop: 8,
                                  padding: 12,
                                  borderRadius: 10,
                                  background: 'rgba(255, 255, 255, 0.95)',
                                  border: '1px solid var(--color-border)',
                                  fontSize: 11,
                                  display: 'flex',
                                  flexDirection: 'column',
                                  gap: 6
                                }}
                              >
                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                  <span style={{ color: 'var(--color-text-secondary)' }}>Threat Classification:</span>
                                  <strong style={{ color: '#ef4444' }}>{n.fraudDetails.threatCategory}</strong>
                                </div>
                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                  <span style={{ color: 'var(--color-text-secondary)' }}>RAG Vector Pattern Match:</span>
                                  <strong style={{ color: '#8b5cf6' }}>{n.fraudDetails.similarityMatch}% Cosine Match</strong>
                                </div>
                                {n.fraudDetails.refundId && (
                                  <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px dashed var(--color-border)', paddingTop: 4 }}>
                                    <span style={{ color: 'var(--color-text-secondary)' }}>Razorpay Escrow Refund ID:</span>
                                    <strong style={{ color: '#10b981' }}>{n.fraudDetails.refundId}</strong>
                                  </div>
                                )}
                                <div style={{ background: '#f8fafc', padding: 6, borderRadius: 6, marginTop: 4, color: '#475569', fontStyle: 'italic' }}>
                                  "{n.fraudDetails.matchedPatternDescription}"
                                </div>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Footer Status */}
          <div
            style={{
              padding: '10px 16px',
              borderTop: '1px solid var(--color-border)',
              background: '#f8fafc',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              fontSize: 11,
              color: 'var(--color-text-secondary)'
            }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <ShieldCheck size={13} color="#10b981" /> SafePe Escrow & AI Defense Active
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              {sseConnected ? (
                <><span style={{ width: 6, height: 6, borderRadius: '50%', background: '#10b981', display: 'inline-block', animation: 'pulse 2s infinite' }} /> Kafka Live</>
              ) : (
                <><span style={{ width: 6, height: 6, borderRadius: '50%', background: '#94a3b8', display: 'inline-block' }} /> Offline</>
              )}
            </span>
          </div>
        </div>
      )}

      {/* Keyframe Animations */}
      <style>{`
        .ring-animation {
          animation: bellShake 0.8s ease-in-out;
        }

        @keyframes bellShake {
          0% { transform: rotate(0); }
          15% { transform: rotate(15deg); }
          30% { transform: rotate(-15deg); }
          45% { transform: rotate(10deg); }
          60% { transform: rotate(-10deg); }
          75% { transform: rotate(4deg); }
          85% { transform: rotate(-4deg); }
          100% { transform: rotate(0); }
        }

        @keyframes slideInRight {
          from { opacity: 0; transform: translateX(50px); }
          to { opacity: 1; transform: translateX(0); }
        }

        @keyframes fadeInScale {
          from { opacity: 0; transform: translateY(-10px) scale(0.96); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
      `}</style>
    </div>
  );
}
