import { useState, useEffect } from 'react';
import { Radio, Activity, AlertTriangle, CheckCircle, XCircle, RefreshCw, Zap, Clock } from 'lucide-react';
import { useNotifications } from '../context/NotificationContext';

interface EventItem {
  id: string;
  type: 'TRANSACTION' | 'FRAUD_ALERT';
  transactionId: string;
  timestamp: string;
  details: string;
  riskScore?: number;
  action?: string;
  amount?: string;
  upiId?: string;
}

// Demo events for visualization (in production, these come from a WebSocket/SSE endpoint)
const DEMO_EVENTS: EventItem[] = [
  {
    id: 'evt-001', type: 'TRANSACTION', transactionId: 'txn-a1b2c3',
    timestamp: new Date().toISOString(), details: 'Payment of ₹2,500 to reliance.retail@oksbi',
    amount: '2500', upiId: 'reliance.retail@oksbi'
  },
  {
    id: 'evt-002', type: 'FRAUD_ALERT', transactionId: 'txn-a1b2c3',
    timestamp: new Date().toISOString(), details: 'Agentic AI analysis complete — ALLOW',
    riskScore: 15, action: 'ALLOW'
  },
  {
    id: 'evt-003', type: 'TRANSACTION', transactionId: 'txn-d4e5f6',
    timestamp: new Date().toISOString(), details: 'Payment of ₹85,000 to unknown@paytm',
    amount: '85000', upiId: 'unknown@paytm'
  },
  {
    id: 'evt-004', type: 'FRAUD_ALERT', transactionId: 'txn-d4e5f6',
    timestamp: new Date().toISOString(), details: 'High-value transaction flagged — FLAG_VERIFICATION',
    riskScore: 62, action: 'FLAG_VERIFICATION'
  },
  {
    id: 'evt-005', type: 'TRANSACTION', transactionId: 'txn-g7h8i9',
    timestamp: new Date().toISOString(), details: 'Payment of ₹1,50,000 to suspicious.merchant@upi',
    amount: '150000', upiId: 'suspicious.merchant@upi'
  },
  {
    id: 'evt-006', type: 'FRAUD_ALERT', transactionId: 'txn-g7h8i9',
    timestamp: new Date().toISOString(), details: 'Transaction blocked — BLOCK',
    riskScore: 89, action: 'BLOCK'
  },
];

export default function KafkaEventMonitor() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [filter, setFilter] = useState<'ALL' | 'TRANSACTION' | 'FRAUD_ALERT'>('ALL');
  const [eventCount, setEventCount] = useState(0);
  const { addNotification } = useNotifications();

  // Simulate live streaming of Kafka events
  const startStreaming = () => {
    setIsStreaming(true);
    setEvents([]);
    setEventCount(0);

    DEMO_EVENTS.forEach((event, index) => {
      setTimeout(() => {
        setEvents(prev => [{ ...event, id: `evt-${Date.now()}-${index}`, timestamp: new Date().toISOString() }, ...prev]);
        setEventCount(prev => prev + 1);

        // When a high risk fraud event arrives in Kafka stream, push to Bell notification!
        if (event.action === 'BLOCK') {
          addNotification({
            type: 'FRAUD_ALERT',
            title: '🚨 High-Risk Fraud Intercepted in Kafka Bus',
            message: `Kafka stream flagged ${event.details} (Risk Score: ${event.riskScore}%). Automated escrow refund triggered.`,
            amount: 150000,
            upiId: 'suspicious.merchant@upi',
            referenceId: event.transactionId,
            fraudDetails: {
              threatCategory: 'HIGH_VALUE_UNVERIFIED_MERCHANT',
              similarityMatch: 89.0,
              matchedPatternDescription: 'Kafka event-driven consumer intercepted transaction before bank nodal settlement.',
              merchantName: 'Suspicious Fake Merchant',
              merchantUpi: 'suspicious.merchant@upi',
              merchantTrustScore: 0.1,
              reportedCount: 15,
              action: 'BLOCK',
              refundId: `rzp_rfnd_${Date.now().toString().slice(-7)}`,
              refundAmount: 150000
            }
          });
        } else if (event.type === 'TRANSACTION' && event.upiId === 'reliance.retail@oksbi') {
          addNotification({
            type: 'SUCCESS',
            title: '✅ Transaction Completed',
            message: event.details,
            amount: 2500,
            upiId: event.upiId,
            referenceId: event.transactionId
          });
        }
      }, (index + 1) * 1200);
    });

    setTimeout(() => setIsStreaming(false), DEMO_EVENTS.length * 1200 + 500);
  };

  const filteredEvents = events.filter(e => filter === 'ALL' || e.type === filter);

  const getActionColor = (action?: string) => {
    switch (action) {
      case 'BLOCK': return '#ef4444';
      case 'FLAG_VERIFICATION': return '#f59e0b';
      case 'ALLOW': return '#10b981';
      default: return '#6b7280';
    }
  };

  const getActionIcon = (action?: string) => {
    switch (action) {
      case 'BLOCK': return <XCircle size={16} />;
      case 'FLAG_VERIFICATION': return <AlertTriangle size={16} />;
      case 'ALLOW': return <CheckCircle size={16} />;
      default: return <Activity size={16} />;
    }
  };

  const formatTime = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

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
          Real-time event stream from transaction-events & fraud-alerts Kafka topics
        </p>
      </div>

      {/* Controls */}
      <div className="surface-panel" style={{ marginBottom: 24, padding: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          {(['ALL', 'TRANSACTION', 'FRAUD_ALERT'] as const).map(f => (
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
              {f === 'ALL' ? '📡 All' : f === 'TRANSACTION' ? '💸 Transactions' : '🚨 Fraud Alerts'}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
            {eventCount} events processed
          </span>
          <button
            className="btn-primary"
            onClick={startStreaming}
            disabled={isStreaming}
            style={{ padding: '8px 18px', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}
          >
            {isStreaming ? (
              <><RefreshCw size={14} className="spin" /> Streaming...</>
            ) : (
              <><Zap size={14} /> Start Stream</>
            )}
          </button>
        </div>
      </div>

      {/* Topic Badges */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '8px 16px',
          borderRadius: 12, background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border)'
        }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: isStreaming ? '#10b981' : '#6b7280', animation: isStreaming ? 'pulse-dot 1.5s infinite' : 'none' }} />
          <span style={{ fontSize: 12, fontWeight: 600 }}>transaction-events</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', background: 'var(--color-bg-primary)', padding: '2px 6px', borderRadius: 4 }}>3 partitions</span>
        </div>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '8px 16px',
          borderRadius: 12, background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border)'
        }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: isStreaming ? '#f59e0b' : '#6b7280', animation: isStreaming ? 'pulse-dot 1.5s infinite' : 'none' }} />
          <span style={{ fontSize: 12, fontWeight: 600 }}>fraud-alerts</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', background: 'var(--color-bg-primary)', padding: '2px 6px', borderRadius: 4 }}>3 partitions</span>
        </div>
      </div>

      {/* Event Stream */}
      <div className="surface-panel" style={{ padding: 0, overflow: 'hidden' }}>
        {filteredEvents.length === 0 ? (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--color-text-secondary)' }}>
            <Radio size={48} style={{ opacity: 0.3, marginBottom: 12 }} />
            <p style={{ margin: 0 }}>No events yet. Click "Start Stream" to simulate Kafka events.</p>
          </div>
        ) : (
          filteredEvents.map((event, i) => (
            <div
              key={event.id}
              style={{
                padding: '14px 20px', borderBottom: '1px solid var(--color-border)',
                display: 'flex', alignItems: 'center', gap: 14,
                animation: i === 0 ? 'slideIn 0.4s ease' : 'none',
                background: i === 0 ? 'var(--color-bg-secondary)' : 'transparent'
              }}
            >
              {/* Event Type Badge */}
              <div style={{
                padding: '4px 10px', borderRadius: 8, fontSize: 10, fontWeight: 700, minWidth: 90, textAlign: 'center',
                background: event.type === 'TRANSACTION' ? '#3b82f620' : '#ef444420',
                color: event.type === 'TRANSACTION' ? '#3b82f6' : '#ef4444'
              }}>
                {event.type === 'TRANSACTION' ? '💸 TXN' : '🚨 ALERT'}
              </div>

              {/* Event Details */}
              <div style={{ flex: 1 }}>
                <p style={{ margin: 0, fontSize: 13, fontWeight: 500 }}>{event.details}</p>
                <p style={{ margin: 0, fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  <Clock size={10} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                  {formatTime(event.timestamp)} • ID: {event.transactionId}
                </p>
              </div>

              {/* Risk Score / Action */}
              {event.type === 'FRAUD_ALERT' && event.action && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{
                    padding: '4px 10px', borderRadius: 8, fontSize: 11, fontWeight: 700,
                    background: getActionColor(event.action) + '20', color: getActionColor(event.action),
                    display: 'flex', alignItems: 'center', gap: 4
                  }}>
                    {getActionIcon(event.action)} {event.action}
                  </span>
                  {event.riskScore !== undefined && (
                    <span style={{
                      fontSize: 13, fontWeight: 800, color: getActionColor(event.action)
                    }}>
                      {event.riskScore}%
                    </span>
                  )}
                </div>
              )}

              {event.type === 'TRANSACTION' && event.amount && (
                <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--color-primary)' }}>
                  ₹{Number(event.amount).toLocaleString('en-IN')}
                </span>
              )}
            </div>
          ))
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
        .spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
