import { useState, useEffect } from 'react';
import { PieChart, Sparkles, Bot } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '@clerk/react';
import api from '../api';

interface Payee {
  payee: string;
  amount: number;
}

const COLORS = ['#ef4444', '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];

export default function SpendAnalysis() {
  const { getToken } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [narrative, setNarrative] = useState('');
  const [totalSpent, setTotalSpent] = useState(0);
  const [txnCount, setTxnCount] = useState(0);
  const [payees, setPayees] = useState<Payee[]>([]);
  const [active, setActive] = useState(0);

  const radius = 40;
  const circumference = 2 * Math.PI * radius;

  useEffect(() => {
    const fetchInsights = async () => {
      try {
        const token = await getToken();
        const res = await api.get('/assistant/insights', {
          headers: { Authorization: `Bearer ${token}` }
        });
        setNarrative(res.data?.narrative || '');
        setTotalSpent(Number(res.data?.totalSpent) || 0);
        setTxnCount(Number(res.data?.transactionCount) || 0);
        setPayees((res.data?.topPayees || []).map((p: Payee) => ({ payee: p.payee, amount: Number(p.amount) || 0 })));
      } catch (err) {
        console.error('Failed to load spending insights', err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    fetchInsights();
  }, [getToken]);

  const hasSpends = totalSpent > 0 && payees.length > 0;
  const pct = (amount: number) => (totalSpent > 0 ? Math.round((amount / totalSpent) * 100) : 0);
  const inr = (n: number) => n.toLocaleString('en-IN', { maximumFractionDigits: 2 });

  // Donut segments: each top payee's share of total spending.
  let offset = 0;
  const segments = payees.map((p, i) => {
    const dash = (p.amount / (totalSpent || 1)) * circumference;
    const seg = { ...p, dash, offset, color: COLORS[i % COLORS.length] };
    offset += dash;
    return seg;
  });

  const summaryText = loading
    ? 'Analysing your transactions...'
    : error
      ? 'Spending insights are unavailable right now. Please try again later.'
      : narrative || 'Start paying with SafePe and your spending insights will appear here.';

  return (
    <div className="animate-fade-up" style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '14px', borderRadius: '16px', boxShadow: '0 4px 14px rgba(16,185,129,0.3)' }}>
            <PieChart size={30} color="white" />
          </div>
          <div>
            <h1 style={{ fontSize: '26px', fontWeight: '800', margin: 0 }}>AI Spending Insights</h1>
            <p style={{ color: 'var(--color-text-secondary)', margin: '4px 0 0 0', fontSize: '14px' }}>
              Built from your own SafePe transactions.
            </p>
          </div>
        </div>

        <Link to="/chatbot" style={{ textDecoration: 'none' }}>
          <button style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            background: 'linear-gradient(135deg, #10b981, #059669)',
            color: 'white', border: 'none', padding: '10px 18px',
            borderRadius: '12px', cursor: 'pointer', fontWeight: '700', fontSize: '13px',
            boxShadow: '0 4px 12px rgba(16,185,129,0.25)'
          }}>
            <Bot size={16} /> Ask Money Assistant
          </button>
        </Link>
      </div>

      {/* AI Narrative Card */}
      <div className="surface-panel" style={{
        padding: '20px 24px',
        background: 'linear-gradient(135deg, rgba(16,185,129,0.06), rgba(59,130,246,0.04))',
        border: '1px solid rgba(16,185,129,0.25)',
        borderRadius: '16px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
          <Sparkles size={18} color="#10b981" />
          <h3 style={{ fontSize: '15px', fontWeight: '800', margin: 0, color: 'var(--color-text-primary)' }}>
            Gemini AI Financial Summary
          </h3>
        </div>
        <p style={{ fontSize: '14px', lineHeight: '1.6', color: 'var(--color-text-primary)', margin: 0 }}>
          {summaryText}
        </p>
      </div>

      {/* Main Chart Card */}
      <div className="surface-panel" style={{ textAlign: 'center', padding: '36px 24px' }}>
        <h2 style={{ color: 'var(--color-text-secondary)', marginBottom: '8px', fontSize: '15px', fontWeight: '600' }}>
          Total Spent
        </h2>
        <div style={{ fontSize: '48px', fontWeight: '900', color: 'var(--color-text-primary)', marginBottom: '4px' }}>
          ₹{inr(totalSpent)}
        </div>
        <div style={{ fontSize: '13px', color: 'var(--color-text-secondary)', marginBottom: '24px' }}>
          {txnCount} {txnCount === 1 ? 'transaction' : 'transactions'}
        </div>

        {/* SVG Donut Chart */}
        <div style={{ position: 'relative', width: '210px', height: '210px', margin: '0 auto 8px auto' }}>
          <svg viewBox="0 0 100 100" style={{ transform: 'rotate(-90deg)', width: '100%', height: '100%', overflow: 'visible' }}>
            {!hasSpends && (
              <circle cx="50" cy="50" r={radius} fill="transparent" stroke="var(--color-border)" strokeWidth="15" />
            )}
            {hasSpends && segments.map((s, i) => (
              <circle
                key={s.payee}
                cx="50" cy="50" r={radius}
                fill="transparent"
                stroke={s.color}
                strokeWidth={active === i ? '20' : '15'}
                strokeDasharray={`${s.dash} ${circumference}`}
                strokeDashoffset={-s.offset}
                style={{ cursor: 'pointer', transition: 'stroke-width 0.2s' }}
                onClick={() => setActive(i)}
              />
            ))}
          </svg>

          <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', pointerEvents: 'none', padding: '0 40px' }}>
            <span style={{ fontSize: '32px', fontWeight: '800', color: 'var(--color-text-primary)' }}>
              {hasSpends ? `${pct(payees[active]?.amount || 0)}%` : '0%'}
            </span>
            <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)', fontWeight: '600', wordBreak: 'break-all' }}>
              {hasSpends ? payees[active]?.payee : 'No spending yet'}
            </span>
          </div>
        </div>
      </div>

      {/* Breakdown by payee */}
      <div>
        <h3 style={{ marginBottom: '16px', fontSize: '16px', fontWeight: '700', color: 'var(--color-text-primary)' }}>
          Top Payees
        </h3>
        {!hasSpends ? (
          <div className="surface-panel" style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-secondary)', fontSize: '14px' }}>
            {loading ? 'Loading...' : 'No spending yet. Your payments will be broken down here once you start using SafePe.'}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {segments.map((s, i) => (
              <div
                key={s.payee}
                className="surface-panel"
                style={{ padding: '18px 20px', cursor: 'pointer', border: active === i ? `2px solid ${s.color}` : '1px solid var(--color-border)', transition: 'all 0.2s' }}
                onClick={() => setActive(i)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', gap: '12px' }}>
                  <span style={{ fontWeight: '700', fontSize: '14px', wordBreak: 'break-all' }}>{s.payee}</span>
                  <span style={{ fontWeight: '800', fontSize: '14px', whiteSpace: 'nowrap' }}>₹{inr(s.amount)} ({pct(s.amount)}%)</span>
                </div>
                <div style={{ width: '100%', height: '8px', background: 'rgba(0,0,0,0.06)', borderRadius: '6px' }}>
                  <div style={{ width: `${pct(s.amount)}%`, height: '100%', background: s.color, borderRadius: '6px' }} />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
