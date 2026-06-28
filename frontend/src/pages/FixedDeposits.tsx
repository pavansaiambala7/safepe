import { useState, useEffect } from 'react';
import { TrendingUp, Bot, RefreshCw, Percent } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

interface FDRate {
  bank: string;
  domain: string;
  normal: string;
  senior: string;
}

export default function FixedDeposits() {
  const [loading, setLoading] = useState(true);
  const [fdRates, setFdRates] = useState<FDRate[]>([]);
  const [error, setError] = useState<string | null>(null);
  const { getToken } = useAuth();

  const fetchFDRates = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      // Backend now instructs Gemini to return JSON
      const response = await api.post('/fraud/fd-rates', { content: "Get top 10 banks" }, { headers });
      
      const rawJson = response.data.analysis;
      // In case the AI still wraps it in markdown backticks, strip them
      const cleanJson = rawJson.replace(/```json\n?|```/g, '').trim();
      
      const parsedData = JSON.parse(cleanJson);
      setFdRates(parsedData);
    } catch (err) {
      console.error("Failed to fetch FD rates", err);
      setError("Error contacting AI Financial Advisor. Please try again later.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFDRates();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const getBankLogo = (bankName: string) => {
    const lower = bankName.toLowerCase();
    if (lower.includes('hdfc')) return 'https://icon.horse/icon/hdfcbank.com';
    if (lower.includes('sbi') || lower.includes('state bank')) return 'https://icon.horse/icon/sbi.co.in';
    if (lower.includes('icici')) return 'https://icon.horse/icon/icicibank.com';
    if (lower.includes('canara')) return 'https://icon.horse/icon/canarabank.com';
    if (lower.includes('kotak')) return 'https://icon.horse/icon/kotak.com';
    if (lower.includes('yes')) return 'https://icon.horse/icon/yesbank.in';
    if (lower.includes('axis')) return 'https://icon.horse/icon/axisbank.com';
    if (lower.includes('airtel')) return 'https://icon.horse/icon/airtel.in';
    if (lower.includes('union')) return 'https://icon.horse/icon/unionbankofindia.co.in';
    if (lower.includes('federal')) return 'https://icon.horse/icon/federalbank.co.in';
    if (lower.includes('idbi')) return 'https://icon.horse/icon/idbibank.in';
    if (lower.includes('bob') || lower.includes('baroda')) return 'https://icon.horse/icon/bankofbaroda.in';
    if (lower.includes('pnb') || lower.includes('punjab national')) return 'https://icon.horse/icon/pnbindia.in';
    if (lower.includes('indusind')) return 'https://icon.horse/icon/indusind.com';
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(bankName)}&background=10b981&color=fff&size=64&bold=true`;
  };

  return (
    <div className="animate-fade-up">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
            <TrendingUp size={32} color="white" />
          </div>
          <div>
            <h1 style={{ fontSize: '28px' }}>FD Rates</h1>
            <p style={{ color: 'var(--color-text-secondary)' }}>Top 10 Banks AI Analysis</p>
          </div>
        </div>
        
        <button 
          onClick={fetchFDRates} 
          disabled={loading}
          style={{ background: 'transparent', border: '1px solid var(--color-border)', padding: '8px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          <RefreshCw size={20} color="var(--color-primary)" className={loading ? "floating" : ""} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
        </button>
      </div>

      <style>
        {`
          @keyframes spin { 100% { transform: rotate(360deg); } }
        `}
      </style>

      {loading ? (
        <div className="surface-panel" style={{ overflow: 'hidden', padding: '0', border: '1px solid var(--color-border)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '2px solid var(--color-border)' }}>
                <th style={{ padding: '16px' }}><div className="skeleton skeleton-text" style={{ width: '80px' }}></div></th>
                <th style={{ padding: '16px' }}><div className="skeleton skeleton-text" style={{ width: '120px' }}></div></th>
                <th style={{ padding: '16px' }}><div className="skeleton skeleton-text" style={{ width: '120px' }}></div></th>
              </tr>
            </thead>
            <tbody>
              {[1, 2, 3, 4, 5].map((i) => (
                <tr key={i} style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <td style={{ padding: '16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div className="skeleton" style={{ width: '32px', height: '32px', borderRadius: '50%' }}></div>
                      <div className="skeleton skeleton-text" style={{ width: '100px' }}></div>
                    </div>
                  </td>
                  <td style={{ padding: '16px' }}><div className="skeleton skeleton-text" style={{ width: '60px' }}></div></td>
                  <td style={{ padding: '16px' }}><div className="skeleton skeleton-text" style={{ width: '60px' }}></div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : error ? (
        <div className="surface-panel status-danger" style={{ textAlign: 'center', padding: '24px' }}>
          <p>{error}</p>
        </div>
      ) : (
        <div className="surface-panel" style={{ overflow: 'hidden', padding: '0', border: '1px solid var(--color-highlight)' }}>
          <div style={{ background: 'var(--color-bg-surface-hover)', padding: '16px', borderBottom: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', gap: '8px' }}>
             <Bot size={20} color="var(--color-highlight)" />
             <span style={{ fontWeight: '500', color: 'var(--color-highlight)' }}>Live Data Generated by AI (Cached at Startup)</span>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '2px solid var(--color-border)' }}>
                <th style={{ padding: '16px', fontWeight: '600', color: 'var(--color-text-secondary)' }}>Bank Name</th>
                <th style={{ padding: '16px', fontWeight: '600', color: 'var(--color-text-secondary)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Percent size={18} /> General Citizen
                  </div>
                </th>
                <th style={{ padding: '16px', fontWeight: '600', color: 'var(--color-text-secondary)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <TrendingUp size={18} /> Senior Citizen
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {fdRates.map((rate, idx) => (
                <tr 
                  key={idx} 
                  style={{ 
                    borderBottom: idx === fdRates.length - 1 ? 'none' : '1px solid var(--color-border)',
                    transition: 'background 0.2s ease',
                    cursor: 'pointer'
                  }}
                  onMouseOver={(e) => e.currentTarget.style.background = 'var(--color-bg-surface-hover)'}
                  onMouseOut={(e) => e.currentTarget.style.background = 'transparent'}
                >
                  <td style={{ padding: '16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <img 
                        src={getBankLogo(rate.bank)} 
                        alt={rate.bank} 
                        style={{ width: '32px', height: '32px', borderRadius: '50%', objectFit: 'contain', background: '#fff', border: '1px solid var(--color-border)' }}
                        onError={(e) => {
                          e.currentTarget.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(rate.bank)}&background=10b981&color=fff&size=64&bold=true`;
                        }}
                      />
                      <span style={{ fontWeight: '500', fontSize: '15px' }}>{rate.bank}</span>
                    </div>
                  </td>
                  <td style={{ padding: '16px', fontSize: '15px', color: 'var(--color-text-primary)' }}>
                    {rate.normal}
                  </td>
                  <td style={{ padding: '16px', fontSize: '15px', fontWeight: '600', color: 'var(--color-primary)' }}>
                    {rate.senior}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      
      <p style={{ marginTop: '16px', fontSize: '12px', color: 'var(--color-text-secondary)', textAlign: 'center' }}>
        * Rates are dynamically generated by Gemini AI and are indicative.
      </p>
    </div>
  );
}
