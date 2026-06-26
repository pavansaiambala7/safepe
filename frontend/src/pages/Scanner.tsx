import { useState } from 'react';
import { Bot, AlertTriangle, CheckCircle } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

export default function Scanner() {
  const [smsText, setSmsText] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const { getToken } = useAuth();

  const handleScan = async () => {
    if (!smsText.trim()) return;
    
    setLoading(true);
    setResult(null);
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      // Calls http://localhost:8080/api/v1/fraud/analyze-sms via the Vite proxy!
      const response = await api.post('/fraud/analyze-sms', { content: smsText }, { headers });
      setResult(response.data.analysis);
    } catch (error) {
      console.error("Failed to scan SMS", error);
      setResult("Error contacting AI scanner. Please try again later.");
    } finally {
      setLoading(false);
    }
  };

  const isDangerous = result?.toLowerCase().includes("fraud") || result?.toLowerCase().includes("scam") || result?.toLowerCase().includes("suspicious") || result?.toLowerCase().includes("danger");

  return (
    <div className="surface-panel" style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px' }}>
        <div style={{ background: 'linear-gradient(135deg, #8b5cf6, #3b82f6)', padding: '12px', borderRadius: '12px' }}>
          <Bot size={32} color="white" />
        </div>
        <div>
          <h2 style={{ fontSize: '24px' }}>Gemini AI SMS Scanner</h2>
          <p style={{ color: 'var(--color-text-secondary)' }}>Paste a suspicious text message below to check if it's a scam.</p>
        </div>
      </div>

      <textarea
        className="input-field"
        rows={6}
        placeholder="e.g., Dear Customer, your bank account will be blocked. Click here to verify your KYC: http://fake-link.com"
        value={smsText}
        onChange={(e) => setSmsText(e.target.value)}
        style={{ resize: 'none', marginBottom: '20px' }}
      />

      <button 
        className="btn-primary" 
        style={{ width: '100%', marginBottom: '24px', opacity: loading ? 0.7 : 1 }}
        onClick={handleScan}
        disabled={loading}
      >
        {loading ? 'AI is Analyzing...' : 'Scan Message'}
      </button>

      {result && (
        <div style={{ 
          padding: '20px', 
          borderRadius: '12px', 
          backgroundColor: isDangerous ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
          border: `1px solid ${isDangerous ? 'var(--color-danger)' : 'var(--color-accent)'}`,
          display: 'flex',
          gap: '16px',
          alignItems: 'flex-start'
        }}>
          {isDangerous ? (
            <AlertTriangle size={28} className="status-danger" style={{ flexShrink: 0 }} />
          ) : (
            <CheckCircle size={28} className="status-safe" style={{ flexShrink: 0 }} />
          )}
          <div style={{ lineHeight: '1.6' }}>
            <h4 style={{ color: isDangerous ? 'var(--color-danger)' : 'var(--color-accent)', marginBottom: '8px' }}>
              {isDangerous ? 'High Risk Detected' : 'Looks Safe'}
            </h4>
            <p>{result}</p>
          </div>
        </div>
      )}
    </div>
  );
}
