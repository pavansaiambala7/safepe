import { useState, useEffect } from 'react';
import { CreditCard, CheckCircle, ShieldAlert, ArrowRight, Loader2 } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';
import { useNotifications } from '../context/NotificationContext';

export default function Checkout() {
  const [upiId, setUpiId] = useState('');
  const [amount, setAmount] = useState('500');
  const [status, setStatus] = useState<'idle' | 'paying' | 'success'>('idle');
  const [razorpayLoaded, setRazorpayLoaded] = useState(false);
  const [validationError, setValidationError] = useState('');
  const { getToken } = useAuth();
  const { addNotification } = useNotifications();

  // Load Razorpay Script dynamically
  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => setRazorpayLoaded(true);
    document.body.appendChild(script);
  }, []);

  // Validate UPI ID format (must contain @)
  const isValidUpiId = (id: string) => /^[a-zA-Z0-9._-]+@[a-zA-Z]{2,}$/.test(id);

  const handlePay = async () => {
    setValidationError('');
    
    // Input validation
    if (!upiId.trim()) { setValidationError('Please enter a UPI ID'); return; }
    if (!isValidUpiId(upiId.trim())) { setValidationError('Invalid UPI ID format (e.g., name@oksbi)'); return; }
    if (!amount || parseFloat(amount) <= 0) { setValidationError('Amount must be greater than ₹0'); return; }
    if (parseFloat(amount) > 100000) { setValidationError('Maximum payment limit is ₹1,00,000'); return; }
    if (!razorpayLoaded) { setValidationError('Payment gateway is loading. Please try again.'); return; }

    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      setStatus('paying');
      const orderRes = await api.post('/payments/create', { upiId, amount }, { headers });
      const orderId = orderRes.data.orderId;
      const keyId = orderRes.data.keyId;

      // Open the Razorpay Checkout
      const options = {
        key: keyId || import.meta.env.VITE_RAZORPAY_KEY_ID || 'rzp_test_T5AtiMDfqh5J2N',
        amount: parseFloat(amount) * 100,
        currency: 'INR',
        name: 'SafePe',
        description: `Payment to ${upiId}`,
        order_id: orderId,
        handler: async function (response: any) {
          console.log("Payment response received", response);
          try {
            await api.post('/payments/webhook', {
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
            }, { headers });
            
            // Add real-time notification to the Bell Notification Center
            addNotification({
              type: 'SUCCESS',
              title: '✅ Transaction Successful',
              message: `Payment of ₹${parseFloat(amount).toLocaleString('en-IN')} to ${upiId} was completed successfully.`,
              amount: parseFloat(amount),
              upiId: upiId,
              referenceId: response.razorpay_payment_id || orderId
            });

            setStatus('success');
          } catch (verifyError) {
            console.error("Payment verification failed", verifyError);
            setStatus('success');
          }
        },
        modal: {
          ondismiss: function () {
            setStatus('idle');
          }
        },
        prefill: {
          method: 'upi',
          vpa: upiId
        },
        theme: {
          color: '#10b981'
        }
      };

      // @ts-ignore
      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response: any) {
        console.error("Payment failed:", response.error);
        setStatus('idle');
        setValidationError(response.error?.description || 'Payment failed. Please try again.');
      });
      rzp.open();

    } catch (error: any) {
      console.error("Payment flow failed", error);
      setStatus('idle');
      setValidationError(error?.response?.data?.message || 'Payment service is currently unavailable. Please try again.');
    }
  };

  return (
    <div className="surface-panel" style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div style={{ textAlign: 'center', marginBottom: '32px' }}>
        <CreditCard size={48} color="#10b981" style={{ margin: '0 auto 16px auto' }} />
        <h2>Send Money</h2>
        <p style={{ color: 'var(--color-text-secondary)' }}>Pay to any UPI ID instantly via Razorpay</p>
      </div>

      {status === 'success' ? (
        <div style={{ textAlign: 'center', padding: '40px 20px' }}>
          <CheckCircle size={64} className="status-safe" style={{ margin: '0 auto 24px auto' }} />
          <h2 style={{ color: 'var(--color-accent)' }}>Payment Successful!</h2>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '8px' }}>
            ₹{amount} has been sent to <strong>{upiId}</strong>
          </p>
          <button className="btn-primary" style={{ marginTop: '24px' }} onClick={() => setStatus('idle')}>
            Make Another Payment
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>UPI ID</label>
            <input 
              type="text" 
              className="input-field" 
              placeholder="e.g., name@oksbi, merchant@paytm" 
              value={upiId}
              onChange={(e) => { setUpiId(e.target.value); setValidationError(''); }}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--color-text-secondary)' }}>Amount (₹)</label>
            <input 
              type="number" 
              className="input-field" 
              placeholder="500" 
              value={amount}
              onChange={(e) => { setAmount(e.target.value); setValidationError(''); }}
            />
          </div>

          {validationError && (
            <div style={{ color: '#ef4444', fontSize: '14px', padding: '8px 12px', background: 'rgba(239,68,68,0.1)', borderRadius: '8px', border: '1px solid rgba(239,68,68,0.2)' }}>
              {validationError}
            </div>
          )}

          <button 
            className="btn-primary" 
            style={{ width: '100%', marginTop: '16px', height: '54px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}
            onClick={handlePay}
            disabled={status === 'paying' || !upiId || !amount}
          >
            {status === 'paying' ? (
              <>
                <Loader2 size={20} className="spin" />
                Opening Razorpay...
              </>
            ) : (
              <>
                Pay ₹{amount || '0'}
                <ArrowRight size={20} />
              </>
            )}
          </button>

          <p style={{ textAlign: 'center', fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
            Payments processed securely by Razorpay
          </p>
        </div>
      )}

      <style>{`
        .spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
