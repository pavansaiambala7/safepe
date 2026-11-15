import { useState } from 'react';
import { Radio, Activity, AlertTriangle, CheckCircle, XCircle, Clock, Bell } from 'lucide-react';
import { useNotifications, type NotificationItem } from '../context/NotificationContext';

// Real events only: notification-service consumes the Kafka topics and pushes
// each event to the browser over Server-Sent Events (NotificationContext).
// Nothing on this page is simulated.

type Filter = 'ALL' | 'PAYMENTS' | 'FRAUD' | 'REMINDERS';

const matches = (n: NotificationItem, f: Filter) => {
  if (f === 'ALL') return true;
  if (f === 'PAYMENTS') return n.type === 'SUCCESS';
  if (f === 'FRAUD') return n.type === 'FRAUD_ALERT' || n.type === 'ESCROW_REFUND' || n.type === 'REFUND_INITIATED';
  return n.type === 'REMINDER';
};

const style = (n: NotificationItem) => {
  if (n.type === 'FRAUD_ALERT') return { label: '🚨 ALERT', color: '#ef4444', icon: <XCircle size={16} /> };
  if (n.type === 'REMINDER') return { label: '⏰ BILL', color: '#f59e0b', icon: <Bell size={16} /> };
  if (n.type === 'SUCCESS') return { label: '💸 TXN', color: '#3b82f6', icon: <CheckCircle size={16} /> };
  if (n.type === 'ESCROW_REFUND' || n.type === 'REFUND_INITIATED') return { label: '↩ REFUND', color: '#10b981', icon: <AlertTriangle size={16} /> };
  return { label: 'EVENT', color: '#6b7280', icon: <Activity size={16} /> };
};

export default function KafkaEventMonitor() {
  const { notifications, sseConnected } = useNotifications();
  const [filter, setFilter] = useState<Filter>('ALL');

  const events = notifications.filter(n => matches(n, filter));

  const formatTime = (iso: string) =>
    new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit' });

  const topic = (name: string) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 16px', borderRadius: 12, background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border)' }}>
      <div style={{ width: 8, height: 8, borderRadius: '50%', background: sseConnected ? '#10b981' : '#6b7280', animation: sseConnected ? 'pulse-dot 1.5s infinite' : 'none' }} />
      <span style={{ fontSize: 12, fontWeight: 600 }}>{name}</span>
      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', background: 'var(--color-bg-primary)', padding: '2px 6px', borderRadius: 4 }}>3 partitions</span>
    </div>
  );

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '24px 16px' }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
          <Radio size={36} color="#06b6d4" />
          <h1 style={{ fontSize: 28, fontWeight: 700, margin: 0, background: 'linear-gradient(135deg, #06b6d4, #8b5cf6)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            Kafka Event Monitor
          </h1>
        </div>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 14, margin: 0 }}>
          Live events from the transaction-events and bill-reminders Kafka topics
        </p>
      </div>

      {/* Controls */}
      <div className="surface-panel" style={{ marginBottom: 24, padding: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {([
            ['ALL', '📡 All'], ['PAYMENTS', '💸 Payments'], ['FRAUD', '🚨 Fraud'], ['REMINDERS', '⏰ Bills']
          ] as const).map(([f, label]) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              style={{
                padding: '6px 14px', borderRadius: 20, fontSize: 12, fontWeight: 600, cursor: 'pointer',
                border: filter === f ? '2px solid #8b5cf6' : '1px solid var(--color-border)',
                background: filter === f ? '#8b5cf620' : 'transparent',
                color: filter === f ? '#8b5cf6' : 'var(--color-text-secondary)'
              }}
            >
              {label}
            </button>
          ))}
        </div>

        <span style={{ fontSize: 12, fontWeight: 600, color: sseConnected ? '#059669' : 'var(--color-text-secondary)' }}>
          {sseConnected ? '● Connected to event stream' : '○ Connecting to event stream...'} • {notifications.length} events
        </span>
      </div>

      {/* Topic Badges */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        {topic('transaction-events')}
        {topic('bill-reminders')}
      </div>

      {/* Event Stream */}
      <div className="surface-panel" style={{ padding: 0, overflow: 'hidden' }}>
        {events.length === 0 ? (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--color-text-secondary)' }}>
            <Radio size={48} style={{ opacity: 0.3, marginBottom: 12 }} />
            <p style={{ margin: 0 }}>No events yet. Make a payment and its Kafka events will appear here.</p>
          </div>
        ) : (
          events.map((n, i) => {
            const s = style(n);
            return (
              <div
                key={n.id}
                style={{
                  padding: '14px 20px', borderBottom: '1px solid var(--color-border)',
                  display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap',
                  animation: i === 0 ? 'slideIn 0.4s ease' : 'none'
                }}
              >
                <div style={{ padding: '4px 10px', borderRadius: 8, fontSize: 10, fontWeight: 700, minWidth: 90, textAlign: 'center', background: s.color + '20', color: s.color }}>
                  {s.label}
                </div>

                <div style={{ flex: 1, minWidth: 200 }}>
                  <p style={{ margin: 0, fontSize: 13, fontWeight: 600 }}>{n.title}</p>
                  <p style={{ margin: '2px 0', fontSize: 12, color: 'var(--color-text-secondary)' }}>{n.message}</p>
                  <p style={{ margin: 0, fontSize: 11, color: 'var(--color-text-secondary)' }}>
                    <Clock size={10} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                    {formatTime(n.timestamp)}{n.referenceId ? ` • ID: ${n.referenceId}` : ''}
                  </p>
                </div>

                {n.fraudDetails?.action && (
                  <span style={{ padding: '4px 10px', borderRadius: 8, fontSize: 11, fontWeight: 700, background: s.color + '20', color: s.color, display: 'flex', alignItems: 'center', gap: 4 }}>
                    {s.icon} {n.fraudDetails.action}
                  </span>
                )}

                {n.amount !== undefined && n.amount !== null && (
                  <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--color-primary)' }}>
                    ₹{Number(n.amount).toLocaleString('en-IN')}
                  </span>
                )}
              </div>
            );
          })
        )}
      </div>

      <style>{`
        @keyframes slideIn {
          from { opacity: 0; transform: translateY(-20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes pulse-dot {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }
      `}</style>
    </div>
  );
}
