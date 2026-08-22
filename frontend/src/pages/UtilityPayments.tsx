import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, HelpCircle, Search, ChevronRight, CheckCircle, AlertTriangle, Zap, Star } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

declare global {
  interface Window {
    Razorpay: any;
  }
}

interface Biller {
  id: string;
  name: string;
  domain: string;
  category: string;
}

interface Plan {
  label: string;
  amount: number;
  data: string;
  validity: string;
  tag: string;
}

export default function UtilityPayments() {
  const { type } = useParams<{ type: string }>();
  const navigate = useNavigate();
  const { getToken } = useAuth();

  const [step, setStep] = useState<'list' | 'plans' | 'amount' | 'success'>('list');
  const [billers, setBillers] = useState<Biller[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [selectedBiller, setSelectedBiller] = useState<Biller | null>(null);
  const [customerIdentifier, setCustomerIdentifier] = useState('');
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingBillers, setLoadingBillers] = useState(true);
  const [txResult, setTxResult] = useState<any>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Dark theme on mount
  useEffect(() => {
    document.body.style.backgroundColor = '#121212';
    document.body.style.color = '#ffffff';
    const appLayout = document.querySelector('.app-layout') as HTMLElement;
    if (appLayout) appLayout.style.background = '#121212';
    return () => {
      document.body.style.backgroundColor = '';
      document.body.style.color = '';
      if (appLayout) appLayout.style.background = '';
    };
  }, []);

  // Fetch billers from backend on mount
  useEffect(() => {
    const fetchBillers = async () => {
      setLoadingBillers(true);
      try {
        const token = await getToken();
        const category = type === 'postpaid' ? 'postpaid' : type;
        const res = await api.get(`/utilities/billers?category=${category}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setBillers(res.data.billers || []);
      } catch (err) {
        console.error('Failed to fetch billers', err);
        setError('Failed to load providers. Please try again.');
      } finally {
        setLoadingBillers(false);
      }
    };
    fetchBillers();
  }, [type, getToken]);

  const handleSelectBiller = async (biller: Biller) => {
    setSelectedBiller(biller);
    setError(null);

    // Fetch plans for ALL categories (mobile, dth, postpaid, electricity)
    try {
      const token = await getToken();
      const res = await api.get(`/utilities/plans?operator=${biller.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setPlans(res.data.plans || []);
      setStep('plans');
    } catch (err) {
      console.error('Failed to fetch plans', err);
      setStep('amount');
    }
  };

  const handleSelectPlan = (plan: Plan) => {
    setAmount(plan.amount.toString());
    setStep('amount');
  };

  const handlePay = async () => {
    if (!amount || !selectedBiller) return;
    setLoading(true);
    setError(null);

    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      // Step 1: Create a REAL Razorpay Order via our backend
      const orderRes = await api.post('/utilities/order/create', {
        biller_id: selectedBiller.id,
        customer_identifier: customerIdentifier || 'N/A',
        amount: parseFloat(amount),
        category: type
      }, { headers });

      const orderData = orderRes.data;

      // Step 2: Open Razorpay Checkout with the real order
      const options = {
        key: orderData.razorpay_key_id,
        amount: orderData.amount_paise,
        currency: orderData.currency,
        name: 'SafePe',
        description: `${getCategoryTitle()} - ${selectedBiller.name}`,
        order_id: orderData.order_id,
        handler: async function (response: any) {
          // Step 3: Verify payment + fulfill recharge via backend
          try {
            const verifyRes = await api.post('/utilities/order/verify', {
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
              biller_id: selectedBiller.id,
              customer_identifier: customerIdentifier || 'N/A',
              amount: amount,
              category: type
            }, { headers });

            setTxResult(verifyRes.data);
            setStep('success');
          } catch (verifyErr) {
            console.error('Verification failed', verifyErr);
            setError('Payment was received but verification failed. Contact support.');
          }
          setLoading(false);
        },
        prefill: {
          name: 'SafePe User',
          contact: customerIdentifier || '9999999999'
        },
        theme: { color: '#10b981' },
        modal: {
          ondismiss: function () {
            setLoading(false);
          }
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response: any) {
        setError('Payment failed: ' + response.error.description);
        setLoading(false);
      });
      rzp.open();

    } catch (err: any) {
      console.error('Order creation failed', err);
      setError(err.response?.data?.message || 'Failed to create payment order. Please try again.');
      setLoading(false);
    }
  };

  const getCategoryTitle = () => {
    switch (type) {
      case 'mobile': return 'Mobile Recharge';
      case 'electricity': return 'Electricity Bill';
      case 'postpaid': return 'Postpaid Bill';
      case 'dth': return 'DTH Recharge';
      default: return 'Utility Payment';
    }
  };

  const getSearchPlaceholder = () => {
    switch (type) {
      case 'mobile': return 'Search by operator name';
      case 'electricity': return 'Search by biller name';
      case 'postpaid': return 'Search by operator name';
      case 'dth': return 'Search by DTH provider';
      default: return 'Search...';
    }
  };

  const filteredBillers = billers.filter(b =>
    b.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // ─── Header ───────────────────────────────────────────────────────
  const renderHeader = (title: string) => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', background: '#121212', position: 'sticky', top: 0, zIndex: 10 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <ArrowLeft size={24} onClick={() => {
          if (step === 'plans') setStep('list');
          else if (step === 'amount') setStep('plans');
          else navigate('/');
        }} style={{ cursor: 'pointer' }} />
        <h2 style={{ fontSize: '18px', fontWeight: '500' }}>{title}</h2>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <div style={{ fontSize: '14px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <span style={{ color: '#ff6600' }}>Bharat</span>Connect
        </div>
        <HelpCircle size={24} color="#a3a3a3" />
      </div>
    </div>
  );

  // ─── Ad Banner ────────────────────────────────────────────────────
  const renderAdBanner = () => {
    if (type === 'mobile') {
      return (
        <div style={{ margin: '0 16px 16px', background: 'linear-gradient(90deg, #450a0a, #7f1d1d)', borderRadius: '12px', padding: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '4px', color: '#fff' }}>Get flat ₹30 cashback</h3>
            <p style={{ fontSize: '12px', color: '#fca5a5', marginBottom: '12px' }}>Recharge your mobile with Mastercard Cards on SafePe</p>
            <button style={{ background: '#fff', color: '#000', padding: '6px 12px', borderRadius: '16px', fontSize: '12px', fontWeight: '600', border: 'none' }}>Pay now {'>'}</button>
          </div>
          <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#ea580c', opacity: 0.8 }}></div>
        </div>
      );
    }
    if (type === 'electricity') {
      return (
        <div style={{ margin: '0 16px 16px', background: 'linear-gradient(90deg, #450a0a, #7f1d1d)', borderRadius: '12px', padding: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '4px', color: '#fff' }}>Get flat ₹30 cashback</h3>
            <p style={{ fontSize: '12px', color: '#fca5a5', marginBottom: '12px' }}>On electricity bill payments using Mastercard Cards via SafePe</p>
            <button style={{ background: '#fff', color: '#000', padding: '6px 12px', borderRadius: '16px', fontSize: '12px', fontWeight: '600', border: 'none' }}>Pay now {'>'}</button>
          </div>
          <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#ea580c', opacity: 0.8 }}></div>
        </div>
      );
    }
    if (type === 'postpaid') {
      return (
        <div style={{ margin: '0 16px 16px', background: 'linear-gradient(90deg, #581c87, #7e22ce)', borderRadius: '12px', padding: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#fff', marginBottom: '8px' }}>Set up AutoPay for Postpaid Bill & make timely payments</h3>
          <button style={{ background: '#fff', color: '#581c87', padding: '6px 12px', borderRadius: '16px', fontSize: '12px', fontWeight: '600', border: 'none' }}>Know More {'>'}</button>
        </div>
      );
    }
    if (type === 'dth') {
      return (
        <div style={{ margin: '0 16px 16px', background: 'linear-gradient(90deg, #0f172a, #1e293b)', borderRadius: '12px', padding: '16px', border: '1px solid #334155' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 'bold', color: '#fff' }}>TATA PLAY</h3>
          <p style={{ fontSize: '14px', color: '#94a3b8', margin: '4px 0 12px' }}>Never miss a single goal! Recharge Tata Play for ₹2,999. Get flat ₹300 cashback!</p>
          <button style={{ background: '#fff', color: '#000', padding: '6px 12px', borderRadius: '16px', fontSize: '12px', fontWeight: '600', border: 'none' }}>Recharge Now {'>'}</button>
        </div>
      );
    }
    return null;
  };

  // ═══════════════════════════════════════════════════════════════════
  // SUCCESS SCREEN
  // ═══════════════════════════════════════════════════════════════════
  if (step === 'success' && txResult) {
    return (
      <div style={{ minHeight: '100vh', background: '#121212', color: '#fff', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
        <div style={{ background: 'rgba(16, 185, 129, 0.15)', borderRadius: '50%', padding: '20px', marginBottom: '24px' }}>
          <CheckCircle size={64} color="#10b981" />
        </div>
        <h2 style={{ fontSize: '24px', marginBottom: '8px' }}>Payment Successful!</h2>
        <p style={{ color: '#a3a3a3', marginBottom: '24px', textAlign: 'center', maxWidth: '350px' }}>{txResult.message}</p>

        <div style={{ background: '#1e1e1e', padding: '20px', borderRadius: '16px', width: '100%', maxWidth: '400px', border: '1px solid #333' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #262626' }}>
            <span style={{ color: '#a3a3a3' }}>Amount</span>
            <span style={{ fontWeight: '600', color: '#10b981' }}>₹{txResult.amount}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #262626' }}>
            <span style={{ color: '#a3a3a3' }}>Payment ID</span>
            <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{txResult.razorpay_payment_id}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #262626' }}>
            <span style={{ color: '#a3a3a3' }}>BBPS Ref</span>
            <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{txResult.bbps_reference}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0' }}>
            <span style={{ color: '#a3a3a3' }}>Status</span>
            <span style={{ color: '#10b981', fontWeight: '600' }}>{txResult.status}</span>
          </div>
        </div>

        <button
          onClick={() => navigate('/')}
          style={{ marginTop: '32px', background: '#10b981', color: '#fff', border: 'none', padding: '16px 32px', borderRadius: '24px', fontSize: '16px', fontWeight: '600', cursor: 'pointer', width: '100%', maxWidth: '400px' }}
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  // ═══════════════════════════════════════════════════════════════════
  // AMOUNT ENTRY + PAY SCREEN
  // ═══════════════════════════════════════════════════════════════════
  if (step === 'amount') {
    return (
      <div style={{ minHeight: '100vh', background: '#121212', color: '#fff' }}>
        {renderHeader('Enter Details')}

        <div style={{ padding: '24px 16px' }}>
          {/* Selected Biller Info */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: '#1e1e1e', borderRadius: '16px', marginBottom: '24px', border: '1px solid #333' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#10b981', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', fontWeight: 'bold' }}>
              {selectedBiller?.name.charAt(0)}
            </div>
            <div>
              <h3 style={{ fontSize: '18px' }}>{selectedBiller?.name}</h3>
              <p style={{ color: '#a3a3a3', fontSize: '14px' }}>{getCategoryTitle()}</p>
            </div>
          </div>

          {/* Customer Identifier Input */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', color: '#a3a3a3', marginBottom: '8px', fontSize: '14px' }}>
              {type === 'electricity' ? 'Consumer Number' : type === 'dth' ? 'Subscriber ID' : 'Mobile Number'}
            </label>
            <input
              type="text"
              value={customerIdentifier}
              onChange={e => setCustomerIdentifier(e.target.value)}
              placeholder={type === 'electricity' ? 'Enter consumer number' : type === 'dth' ? 'Enter subscriber ID' : 'Enter mobile number'}
              style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', borderRadius: '12px', padding: '16px', color: '#fff', fontSize: '16px' }}
            />
          </div>

          {/* Amount Input */}
          <div style={{ marginBottom: '32px' }}>
            <label style={{ display: 'block', color: '#a3a3a3', marginBottom: '8px', fontSize: '14px' }}>Amount (₹)</label>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: '16px', top: '16px', fontSize: '20px', color: '#a3a3a3' }}>₹</span>
              <input
                type="number"
                value={amount}
                onChange={e => setAmount(e.target.value)}
                placeholder="0"
                style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', borderRadius: '12px', padding: '16px 16px 16px 40px', color: '#fff', fontSize: '24px', fontWeight: '600' }}
              />
            </div>
          </div>

          {error && (
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center', background: 'rgba(239, 68, 68, 0.15)', padding: '16px', borderRadius: '12px', marginBottom: '24px', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
              <AlertTriangle size={20} color="#ef4444" />
              <span style={{ color: '#fca5a5', fontSize: '14px' }}>{error}</span>
            </div>
          )}

          {/* Pay Button */}
          <button
            onClick={handlePay}
            disabled={!amount || !customerIdentifier || loading}
            style={{
              width: '100%',
              background: (amount && customerIdentifier) ? '#10b981' : '#333',
              color: (amount && customerIdentifier) ? '#fff' : '#737373',
              padding: '18px',
              borderRadius: '24px',
              fontSize: '16px',
              fontWeight: 'bold',
              border: 'none',
              cursor: (amount && customerIdentifier) ? 'pointer' : 'not-allowed',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px'
            }}
          >
            {loading ? (
              <><Zap size={20} className="floating" /> Opening Razorpay...</>
            ) : (
              <>Pay ₹{amount || '0'} via Razorpay</>
            )}
          </button>

          <p style={{ textAlign: 'center', color: '#525252', fontSize: '12px', marginTop: '16px' }}>
            Secured by Razorpay BBPS • Bharat Bill Payment System
          </p>
        </div>
      </div>
    );
  }

  // ═══════════════════════════════════════════════════════════════════
  // PLANS SCREEN (All Categories)
  // ═══════════════════════════════════════════════════════════════════
  if (step === 'plans') {
    return (
      <div style={{ minHeight: '100vh', background: '#121212', color: '#fff' }}>
        {renderHeader(`${selectedBiller?.name} Plans`)}

        <div style={{ padding: '0 16px' }}>
          {/* Biller header */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '16px 0', borderBottom: '1px solid #262626', marginBottom: '16px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#10b981', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold' }}>
              {selectedBiller?.name.charAt(0)}
            </div>
            <div>
              <h3 style={{ fontSize: '16px' }}>{selectedBiller?.name}</h3>
              <p style={{ color: '#a3a3a3', fontSize: '13px' }}>Select a {type === 'dth' ? 'channel package' : type === 'electricity' ? 'payment amount' : type === 'postpaid' ? 'postpaid plan' : 'recharge plan'}</p>
            </div>
          </div>

          {/* Enter custom number */}
          <div style={{ marginBottom: '24px' }}>
            <input
              type="text"
              value={customerIdentifier}
              onChange={e => setCustomerIdentifier(e.target.value)}
              placeholder={type === 'dth' ? 'Enter subscriber ID / smart card number' : type === 'electricity' ? 'Enter consumer number' : type === 'postpaid' ? 'Enter mobile number' : 'Enter mobile number'}
              style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', borderRadius: '12px', padding: '14px 16px', color: '#fff', fontSize: '15px' }}
            />
          </div>

          <h4 style={{ fontSize: '14px', color: '#a3a3a3', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '1px' }}>{type === 'dth' ? 'Channel Packages' : type === 'electricity' ? 'Quick Pay Options' : type === 'postpaid' ? 'Postpaid Plans' : 'Popular Plans'}</h4>

          {plans.map((plan, idx) => (
            <div
              key={idx}
              onClick={() => handleSelectPlan(plan)}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '16px', borderBottom: '1px solid #262626', cursor: 'pointer',
                transition: 'background 0.2s'
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#1e1e1e')}
              onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
            >
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  <h4 style={{ fontSize: '18px', fontWeight: '600', color: '#fff' }}>₹{plan.amount}</h4>
                  {plan.tag && (
                    <span style={{
                      background: plan.tag === 'Best Seller' || plan.tag === 'Popular' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(59, 130, 246, 0.2)',
                      color: plan.tag === 'Best Seller' || plan.tag === 'Popular' ? '#10b981' : '#3b82f6',
                      padding: '2px 8px', borderRadius: '8px', fontSize: '11px', fontWeight: '600'
                    }}>
                      {plan.tag === 'Best Seller' && <Star size={10} style={{ marginRight: '4px', verticalAlign: 'middle' }} />}
                      {plan.tag}
                    </span>
                  )}
                </div>
                <p style={{ color: '#a3a3a3', fontSize: '13px' }}>{plan.data}</p>
                <p style={{ color: '#525252', fontSize: '12px' }}>Validity: {plan.validity}</p>
              </div>
              <ChevronRight size={20} color="#525252" />
            </div>
          ))}

          {/* Custom amount option */}
          <div
            onClick={() => setStep('amount')}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', marginTop: '8px', background: '#1e1e1e', borderRadius: '12px', cursor: 'pointer', border: '1px solid #333' }}
          >
            <div>
              <h4 style={{ fontSize: '16px', color: '#fff' }}>Enter custom amount</h4>
              <p style={{ color: '#a3a3a3', fontSize: '13px' }}>Recharge with any amount</p>
            </div>
            <ChevronRight size={20} color="#525252" />
          </div>
        </div>
      </div>
    );
  }

  // ═══════════════════════════════════════════════════════════════════
  // BILLER LIST SCREEN (Default)
  // ═══════════════════════════════════════════════════════════════════
  return (
    <div style={{ minHeight: '100vh', background: '#121212', color: '#fff', paddingBottom: '80px' }}>
      {renderHeader(getCategoryTitle())}
      {renderAdBanner()}

      {/* Search Bar */}
      <div style={{ margin: '0 16px 24px', position: 'relative' }}>
        <Search size={20} color="#a3a3a3" style={{ position: 'absolute', left: '16px', top: '14px' }} />
        <input
          type="text"
          placeholder={getSearchPlaceholder()}
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', borderRadius: '24px', padding: '14px 14px 14px 48px', color: '#fff', fontSize: '15px' }}
        />
      </div>

      {error && (
        <div style={{ margin: '0 16px 16px', display: 'flex', gap: '12px', alignItems: 'center', background: 'rgba(239, 68, 68, 0.15)', padding: '16px', borderRadius: '12px', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
          <AlertTriangle size={20} color="#ef4444" />
          <span style={{ color: '#fca5a5', fontSize: '14px' }}>{error}</span>
        </div>
      )}

      <div style={{ padding: '0 16px' }}>
        {loadingBillers ? (
          <div style={{ textAlign: 'center', padding: '48px 0' }}>
            <Zap size={36} color="#10b981" className="floating" style={{ margin: '0 auto 16px auto' }} />
            <p style={{ color: '#a3a3a3' }}>Loading providers...</p>
          </div>
        ) : (
          <>
            <h4 style={{ fontSize: '14px', fontWeight: '600', color: '#a3a3a3', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '1px' }}>
              {type === 'electricity' ? 'All Billers' : 'Select Provider'}
            </h4>

            {filteredBillers.map(biller => (
              <div
                key={biller.id}
                onClick={() => handleSelectBiller(biller)}
                style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '16px', borderBottom: '1px solid #262626', cursor: 'pointer',
                  transition: 'background 0.2s'
                }}
                onMouseOver={e => (e.currentTarget.style.background = '#1e1e1e')}
                onMouseOut={e => (e.currentTarget.style.background = 'transparent')}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '4px' }}>
                    <img
                      src={`https://icon.horse/icon/${biller.domain}`}
                      alt={biller.name}
                      style={{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'contain' }}
                      onError={(e) => {
                        (e.currentTarget as HTMLImageElement).style.display = 'none';
                      }}
                    />
                  </div>
                  <div>
                    <h4 style={{ fontSize: '16px', fontWeight: '500', color: '#fff' }}>{biller.name}</h4>
                  </div>
                </div>
                <ChevronRight size={20} color="#525252" />
              </div>
            ))}

            {filteredBillers.length === 0 && (
              <p style={{ textAlign: 'center', color: '#525252', padding: '32px 0' }}>No providers found matching "{searchQuery}"</p>
            )}
          </>
        )}
      </div>

      {/* Footer */}
      <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, padding: '16px', background: 'rgba(18,18,18,0.95)', borderTop: '1px solid #262626', fontSize: '12px', color: '#525252', textAlign: 'center' }}>
        Powered by SafePe BBPS • Bharat Bill Payment System
      </div>
    </div>
  );
}
