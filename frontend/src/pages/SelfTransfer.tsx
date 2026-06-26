import { useState } from 'react';
import { ArrowRightLeft, ArrowDown, CheckCircle, AlertTriangle } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

export default function SelfTransfer() {
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [txId, setTxId] = useState<string | null>(null);
  
  const { getToken } = useAuth();

  const handleTransfer = async () => {
    if (!amount) return;
    
    setLoading(true);
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      // Self transfer is essentially a payout to one's own registered account
      const formData = {
        beneficiaryName: 'Self Account',
        accountNumber: 'SELF-ACCOUNT-123',
        ifscCode: 'SAFE000123',
        amount,
        purpose: 'self_transfer'
      };

      const res = await api.post('/payments/bank/transfer', formData, { headers });
      
      // Assume success if we get here
      const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
      setTxId(data.id || 'txn_self_success');
      setStatus('success');
    } catch (error) {
      console.error("Self transfer failed", error);
      setStatus('error');
    } finally {
      setLoading(false);
    }
  };

  if (status === 'success') {
    return (
      <div className="surface-panel" style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center', padding: '40px 20px' }}>
        <CheckCircle size={64} className="status-safe" style={{ margin: '0 auto 24px auto' }} />
        <h2 style={{ color: 'var(--color-accent)' }}>Transfer Successful!</h2>
        <p style={{ color: 'var(--color-text-secondary)', marginTop: '8px' }}>
          ₹{amount} has been successfully transferred to your HDFC Bank account.
        </p>
        {txId && (
          <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px dashed var(--color-border)', marginTop: '16px', fontFamily: 'monospace' }}>
            Txn ID: {txId}
          </div>
        )}
        <button className="btn-primary" style={{ marginTop: '32px' }} onClick={() => setStatus('idle')}>
          Make Another Transfer
        </button>
      </div>
    );
  }

  return (
    <div className="surface-panel animate-fade-up" style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
          <ArrowRightLeft size={32} color="white" />
        </div>
        <div>
          <h1 style={{ fontSize: '24px' }}>Self Transfer</h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>Transfer money between your own linked bank accounts instantly.</p>
        </div>
      </div>

      {status === 'error' && (
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-danger)', padding: '16px', borderRadius: '12px', marginBottom: '24px', border: '1px solid var(--color-danger)' }}>
          <AlertTriangle size={24} />
          <span style={{ fontWeight: '500' }}>Failed to initiate self transfer. Please try again later.</span>
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Visual From -> To accounts */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f8fafc', border: '1px solid var(--color-border)', padding: '24px', borderRadius: '16px', gap: '12px' }}>
          
          <div style={{ width: '100%', padding: '16px', background: 'white', border: '1px solid var(--color-border)', borderRadius: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <p style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginBottom: '4px' }}>Transfer From</p>
              <h4 style={{ fontSize: '16px' }}>SafePe Wallet</h4>
            </div>
          </div>
          
          <div style={{ background: 'var(--color-bg-surface)', padding: '8px', borderRadius: '50%', border: '1px solid var(--color-border)', zIndex: 1, margin: '-16px 0' }}>
            <ArrowDown size={20} color="var(--color-primary)" />
          </div>

          <div style={{ width: '100%', padding: '16px', background: 'white', border: '1px solid var(--color-border)', borderRadius: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <p style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginBottom: '4px' }}>Transfer To</p>
              <h4 style={{ fontSize: '16px' }}>HDFC Bank - 1234</h4>
            </div>
          </div>
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Amount (₹)</label>
          <input 
            type="number" 
            className="input-field" 
            placeholder="5000" 
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
        </div>

        <button 
          className="btn-primary" 
          style={{ width: '100%', marginTop: '16px', height: '54px' }}
          onClick={handleTransfer}
          disabled={loading || !amount}
        >
          {loading ? 'Processing Transfer...' : `Transfer ₹${amount || '0'}`}
        </button>
      </div>
    </div>
  );
}
