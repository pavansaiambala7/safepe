import { useState } from 'react';
import { Landmark, ArrowRight, CheckCircle, AlertTriangle } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

export default function BankTransfer() {
  const [formData, setFormData] = useState({
    beneficiaryName: '',
    accountNumber: '',
    ifscCode: '',
    amount: '',
    purpose: 'transfer'
  });
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const { getToken } = useAuth();

  const handleTransfer = async () => {
    if (!formData.beneficiaryName || !formData.accountNumber || !formData.ifscCode || !formData.amount) return;
    
    setLoading(true);
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      await api.post('/payments/bank/transfer', formData, { headers });
      
      setStatus('success');
    } catch (error) {
      console.error("Bank transfer failed", error);
      setStatus('error');
    } finally {
      setLoading(false);
    }
  };

  if (status === 'success') {
    return (
      <div className="surface-panel" style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center', padding: '40px 20px' }}>
        <CheckCircle size={64} className="status-safe" style={{ margin: '0 auto 24px auto' }} />
        <h2 style={{ color: 'var(--color-accent)' }}>Transfer Initiated!</h2>
        <p style={{ color: 'var(--color-text-secondary)', marginTop: '8px' }}>
          ₹{formData.amount} is being transferred to {formData.beneficiaryName} via IMPS.
        </p>
        <button className="btn-primary" style={{ marginTop: '32px' }} onClick={() => setStatus('idle')}>
          Make Another Transfer
        </button>
      </div>
    );
  }

  return (
    <div className="surface-panel animate-fade-up" style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <div style={{ background: 'linear-gradient(135deg, #3b82f6, #2563eb)', padding: '12px', borderRadius: '12px' }}>
          <Landmark size={32} color="white" />
        </div>
        <div>
          <h1 style={{ fontSize: '24px' }}>Bank Transfer</h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>Send money directly to any bank account via RazorpayX.</p>
        </div>
      </div>

      {status === 'error' && (
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-danger)', padding: '16px', borderRadius: '12px', marginBottom: '24px', border: '1px solid var(--color-danger)' }}>
          <AlertTriangle size={24} />
          <span style={{ fontWeight: '500' }}>Failed to initiate transfer. Ensure your RazorpayX account has sufficient funds.</span>
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Beneficiary Name</label>
          <input 
            type="text" 
            className="input-field" 
            placeholder="e.g., John Doe" 
            value={formData.beneficiaryName}
            onChange={(e) => setFormData({...formData, beneficiaryName: e.target.value})}
          />
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Account Number</label>
          <input 
            type="password" 
            className="input-field" 
            placeholder="e.g., 1234567890" 
            value={formData.accountNumber}
            onChange={(e) => setFormData({...formData, accountNumber: e.target.value})}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>IFSC Code</label>
            <input 
              type="text" 
              className="input-field" 
              placeholder="e.g., HDFC0001234" 
              style={{ textTransform: 'uppercase' }}
              value={formData.ifscCode}
              onChange={(e) => setFormData({...formData, ifscCode: e.target.value.toUpperCase()})}
            />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Amount (₹)</label>
            <input 
              type="number" 
              className="input-field" 
              placeholder="1000" 
              value={formData.amount}
              onChange={(e) => setFormData({...formData, amount: e.target.value})}
            />
          </div>
        </div>

        <button 
          className="btn-primary" 
          style={{ width: '100%', marginTop: '16px', height: '54px', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px' }}
          onClick={handleTransfer}
          disabled={loading || !formData.amount || !formData.accountNumber || !formData.ifscCode}
        >
          {loading ? 'Processing Transfer...' : (
            <>Transfer ₹{formData.amount || '0'} <ArrowRight size={20} /></>
          )}
        </button>
      </div>
    </div>
  );
}
