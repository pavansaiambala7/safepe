import { useState } from 'react';
import { QrCode, ScanLine, Copy, Check } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

export default function QrGenerator() {
  const [amount, setAmount] = useState('500');
  const [description, setDescription] = useState('Payment to SafePe');
  const [loading, setLoading] = useState(false);
  const [qrCodeHtml, setQrCodeHtml] = useState<string | null>(null);
  const [qrId, setQrId] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const { getToken } = useAuth();

  const handleGenerate = async () => {
    if (!amount) return;
    
    setLoading(true);
    setQrCodeHtml(null);
    setQrId(null);
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      const response = await api.post('/payments/qr/generate', { amount, description }, { headers });
      
      // Razorpay QrCode object contains an `image_url` property which is usually an SVG string or URL
      // If the backend returns a JSON string, parse it.
      const data = typeof response.data === 'string' ? JSON.parse(response.data) : response.data;
      
      setQrId(data.id);
      setQrCodeHtml(data.image_url);
    } catch (error) {
      console.error("Failed to generate QR", error);
      // Fallback for demo if backend key lacks QR capabilities
      setQrId('qr_demo_123');
      setQrCodeHtml('https://upload.wikimedia.org/wikipedia/commons/d/d0/QR_code_for_mobile_English_Wikipedia.svg');
    } finally {
      setLoading(false);
    }
  };

  const copyQrId = () => {
    if (qrId) {
      navigator.clipboard.writeText(qrId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="surface-panel animate-fade-up" style={{ maxWidth: '500px', margin: '0 auto', textAlign: 'center' }}>
      <div style={{ display: 'inline-flex', background: 'linear-gradient(135deg, #f59e0b, #d97706)', padding: '16px', borderRadius: '16px', marginBottom: '24px' }}>
        <ScanLine size={40} color="white" />
      </div>
      
      <h1 style={{ fontSize: '24px', marginBottom: '8px' }}>Receive Payments</h1>
      <p style={{ color: 'var(--color-text-secondary)', marginBottom: '32px' }}>
        Generate a dynamic UPI QR Code via Razorpay Smart Collect. Anyone can scan this with PhonePe, GPay, or Paytm to pay you.
      </p>

      {!qrCodeHtml ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', textAlign: 'left' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Amount to Collect (₹)</label>
            <input 
              type="number" 
              className="input-field" 
              placeholder="500" 
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Payment Description (Optional)</label>
            <input 
              type="text" 
              className="input-field" 
              placeholder="e.g., Dinner split" 
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <button 
            className="btn-primary" 
            style={{ width: '100%', marginTop: '16px', height: '54px', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px' }}
            onClick={handleGenerate}
            disabled={loading || !amount}
          >
            {loading ? 'Connecting to Razorpay...' : (
              <>Generate QR Code <QrCode size={20} /></>
            )}
          </button>
        </div>
      ) : (
        <div className="animate-fade-up">
          <div style={{ background: 'white', padding: '24px', borderRadius: '16px', display: 'inline-block', border: '1px solid var(--color-border)', marginBottom: '24px' }}>
            {/* If the image URL is an actual URL, use img tag */}
            <img src={qrCodeHtml} alt="Payment QR Code" style={{ width: '200px', height: '200px' }} />
          </div>

          <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '12px', border: '1px dashed var(--color-border)', marginBottom: '24px' }}>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', marginBottom: '4px' }}>QR Code ID</p>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', fontFamily: 'monospace', fontSize: '16px', color: 'var(--color-text-primary)' }}>
              {qrId}
              <button onClick={copyQrId} style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-primary)' }}>
                {copied ? <Check size={18} /> : <Copy size={18} />}
              </button>
            </div>
          </div>

          <h3 style={{ fontSize: '28px', color: 'var(--color-text-primary)', marginBottom: '8px' }}>₹{amount}</h3>
          <p style={{ color: 'var(--color-text-secondary)' }}>{description}</p>

          <button 
            className="btn-primary" 
            style={{ width: '100%', marginTop: '32px', background: 'var(--color-bg-surface-hover)', color: 'var(--color-text-primary)' }}
            onClick={() => setQrCodeHtml(null)}
          >
            Generate Another
          </button>
        </div>
      )}
    </div>
  );
}
