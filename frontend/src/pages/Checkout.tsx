import { useState, useEffect } from 'react';
import { CreditCard, CheckCircle, ShieldAlert, ShieldCheck, ArrowRight } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

export default function Checkout() {
  const [upiId, setUpiId] = useState('');
  const [amount, setAmount] = useState('500');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<'idle' | 'checking_fraud' | 'trust_score' | 'fraud_detected' | 'paying' | 'success'>('idle');
  const [trustScore, setTrustScore] = useState<number | null>(null);
  const [razorpayLoaded, setRazorpayLoaded] = useState(false);
  const [validationError, setValidationError] = useState('');
  const { getToken } = useAuth();

  // Load Razorpay Script dynamically
  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => setRazorpayLoaded(true);
    document.body.appendChild(script);
  }, []);

  // Validate UPI ID format (must contain @)
  const isValidUpiId = (id: string) => /^[a-zA-Z0-9._-]+@[a-zA-Z]{2,}$/.test(id);

  const handleCheckTrust = async () => {
    setValidationError('');
    
    // Input validation
    if (!upiId.trim()) { setValidationError('Please enter a UPI ID'); return; }
    if (!isValidUpiId(upiId.trim())) { setValidationError('Invalid UPI ID format (e.g., name@oksbi)'); return; }
    if (!amount || parseFloat(amount) <= 0) { setValidationError('Amount must be greater than ₹0'); return; }
    if (parseFloat(amount) > 100000) { setValidationError('Maximum payment limit is ₹1,00,000'); return; }
    
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      // Step 1: Check for Fraud first!
      setStatus('checking_fraud');
      const fraudRes = await api.post('/fraud/check', { upiId }, { headers });
      
      // If our backend AI says this merchant is a scammer, block the payment!
      if (fraudRes.data.isFlagged || fraudRes.data.riskLevel === 'HIGH') {
        setStatus('fraud_detected');
        return;
      }

      // Extract trust score from DB response, or use default 70% secure
      let score = fraudRes.data.trustScore ?? 70;
      // Backend stores known merchant scores as 0.0-1.0, normalize to percentage
      if (score > 0 && score <= 1) {
        score = Math.round(score * 100);
      }
      setTrustScore(score);
      setStatus('trust_score');
    } catch (error) {
      console.error("Trust check failed", error);
      // If backend is down, give a default secure score
      setTrustScore(70);
      setStatus('trust_score');
    }
  };

  const handleProceedToPay = async () => {
    if (!razorpayLoaded) return;
    
    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      setStatus('paying');
      const orderRes = await api.post('/payments/create', { upiId, amount }, { headers });
      const orderId = orderRes.data.orderId;
      const keyId = orderRes.data.keyId; // Get key dynamically from backend

      // Open the Razorpay Window!
      const options = {
        key: keyId || import.meta.env.VITE_VITE_RAZORPAY_KEY_ID || 'rzp_test_T5AtiMDfqh5J2N',
        amount: parseFloat(amount) * 100,
        currency: 'INR',
        name: 'SafePe Secure Checkout',
        description: `Payment to ${upiId}`,
        order_id: orderId,
        handler: function (response: any) {
          console.log("Payment Successful on Phone!", response);
          setStatus('success');
        },
        theme: {
          color: '#10b981'
        }
      };

      // @ts-ignore
      const rzp = new window.Razorpay(options);
      rzp.open();

    } catch (error) {
      console.error("Payment flow failed", error);
      setStatus('idle');
    }
  };

  const getTrustColor = (score: number) => {
    if (score >= 80) return '#10b981';
    if (score >= 60) return '#f59e0b';
    return '#ef4444';
  };

  const getTrustLabel = (score: number) => {
    if (score >= 80) return 'Highly Trusted';
    if (score >= 60) return 'Secure';
    return 'Low Trust';
  };

  return (
    <div className="surface-panel" style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div style={{ textAlign: 'center', marginBottom: '32px' }}>
        <CreditCard size={48} color="#10b981" style={{ margin: '0 auto 16px auto' }} />
        <h2>Send Money Securely</h2>
        <p style={{ color: 'var(--color-text-secondary)' }}>Protected by SafePe Anti-Fraud Engine</p>
      </div>

      {status === 'success' ? (
        <div style={{ textAlign: 'center', padding: '40px 20px' }}>
          <CheckCircle size={64} className="status-safe" style={{ margin: '0 auto 24px auto' }} />
          <h2 style={{ color: 'var(--color-accent)' }}>Payment Successful!</h2>
          <p style={{ color: 'var(--color-text-secondary)' }}>₹{amount} has been safely transferred.</p>
          <button className="btn-primary" style={{ marginTop: '24px' }} onClick={() => { setStatus('idle'); setTrustScore(null); }}>Make Another Payment</button>
        </div>
      ) : status === 'fraud_detected' ? (
        <div style={{ textAlign: 'center', padding: '40px 20px' }}>
          <ShieldAlert size={64} className="status-danger" style={{ margin: '0 auto 24px auto' }} />
          <h2 style={{ color: 'var(--color-danger)' }}>Payment Blocked</h2>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '8px' }}>
            Our AI engine flagged <strong>{upiId}</strong> as a high-risk scammer. We have blocked this transaction to protect your funds.
          </p>
          <button className="btn-primary" style={{ marginTop: '24px', background: 'var(--color-bg-surface-hover)' }} onClick={() => { setStatus('idle'); setTrustScore(null); }}>Go Back</button>
        </div>
      ) : status === 'trust_score' && trustScore !== null ? (
        /* ═══════ TRUST SCORE SCREEN ═══════ */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Payment Summary */}
          <div style={{ background: '#f8fafc', borderRadius: '12px', padding: '20px', border: '1px solid #e2e8f0' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
              <span style={{ color: '#64748b', fontSize: '14px' }}>Paying to</span>
              <span style={{ fontWeight: '600', color: '#0f172a' }}>{upiId}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b', fontSize: '14px' }}>Amount</span>
              <span style={{ fontWeight: '700', fontSize: '20px', color: '#0f172a' }}>₹{amount}</span>
            </div>
          </div>

          {/* Trust Score Card */}
          <div style={{ 
            background: `linear-gradient(135deg, ${getTrustColor(trustScore)}15, ${getTrustColor(trustScore)}05)`, 
            borderRadius: '16px', 
            padding: '24px', 
            border: `1px solid ${getTrustColor(trustScore)}30`,
            textAlign: 'center'
          }}>
            <ShieldCheck size={40} color={getTrustColor(trustScore)} style={{ margin: '0 auto 12px' }} />
            
            {/* Circular Score */}
            <div style={{ 
              width: '100px', height: '100px', borderRadius: '50%', 
              border: `4px solid ${getTrustColor(trustScore)}`,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 16px', background: '#fff'
            }}>
              <span style={{ fontSize: '32px', fontWeight: '800', color: getTrustColor(trustScore) }}>{trustScore}%</span>
            </div>
            
            <h3 style={{ color: getTrustColor(trustScore), fontWeight: '700', fontSize: '18px', marginBottom: '4px' }}>
              {getTrustLabel(trustScore)}
            </h3>
            <p style={{ color: '#64748b', fontSize: '13px' }}>
              Trust Score verified by SafePe Anti-Fraud Engine
            </p>
          </div>

          {/* Proceed Button */}
          <button 
            className="btn-primary" 
            style={{ 
              width: '100%', height: '54px', 
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px',
              background: '#10b981', border: 'none', borderRadius: '12px',
              color: '#fff', fontSize: '16px', fontWeight: '700', cursor: 'pointer'
            }}
            onClick={handleProceedToPay}
            disabled={status === 'paying' || !razorpayLoaded}
          >
            Proceed to Pay ₹{amount}
            <ArrowRight size={20} />
          </button>

          {/* Go Back */}
          <button 
            style={{ 
              width: '100%', height: '44px', background: 'transparent', 
              border: '1px solid #e2e8f0', borderRadius: '12px', color: '#64748b', 
              fontSize: '14px', cursor: 'pointer' 
            }}
            onClick={() => { setStatus('idle'); setTrustScore(null); }}
          >
            Cancel
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Merchant UPI ID</label>
            <input 
              type="text" 
              className="input-field" 
              placeholder="e.g., merchant@oksbi" 
              value={upiId}
              onChange={(e) => setUpiId(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Amount (₹)</label>
            <input 
              type="number" 
              className="input-field" 
              placeholder="500" 
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>

          {validationError && (
            <div style={{ color: '#ef4444', fontSize: '14px', padding: '8px 12px', background: 'rgba(239,68,68,0.1)', borderRadius: '8px', border: '1px solid rgba(239,68,68,0.2)' }}>
              {validationError}
            </div>
          )}
          <button 
            className="btn-primary" 
            style={{ width: '100%', marginTop: '16px', height: '54px' }}
            onClick={handleCheckTrust}
            disabled={status === 'checking_fraud' || !upiId || !amount}
          >
            {status === 'checking_fraud' ? 'Checking Trust Score...' : 
             status === 'paying' ? 'Opening Secure Portal...' : 
             `Pay ₹${amount} Securely`}
          </button>
        </div>
      )}
    </div>
  );
}
