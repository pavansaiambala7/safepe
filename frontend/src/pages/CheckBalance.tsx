import { useState, useEffect } from 'react';
import { Wallet, Landmark, PlusCircle, ShieldCheck, X } from 'lucide-react';
import { useAuth } from '@clerk/react';
import { useLocation } from 'react-router-dom';
import api from '../api';

interface BankAccount {
  id: string;
  bankName: string;
  accountLastFour: string;
}

export default function CheckBalance() {
  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const { getToken } = useAuth();
  const location = useLocation();
  const openAddBank = (location.state as any)?.openAddBank;
  
  // PIN Pad State
  const [selectedAccount, setSelectedAccount] = useState<BankAccount | null>(null);
  const [pin, setPin] = useState('');
  const [checking, setChecking] = useState(false);
  const [balanceResult, setBalanceResult] = useState<{ balance: number; currency: string } | null>(null);
  const [pinError, setPinError] = useState('');

  // Add Bank State
  const [isAdding, setIsAdding] = useState(!!openAddBank);
  const [newBankName, setNewBankName] = useState('HDFC Bank');
  const [newAccNum, setNewAccNum] = useState('');
  const [addingLoading, setAddingLoading] = useState(false);

  const fetchAccounts = async () => {
    setLoading(true);
    try {
      const token = await getToken();
      const res = await api.get('/bank/accounts', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAccounts(res.data || []);
    } catch (err) {
      console.error('Failed to fetch accounts', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleAddAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddingLoading(true);
    try {
      const token = await getToken();
      await api.post('/bank/accounts', {
        bankName: newBankName,
        razorpayTokenId: `token_bank_${Date.now()}`,
        accountLastFour: newAccNum.length >= 4 ? newAccNum.slice(-4) : '0000'
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setIsAdding(false);
      setNewAccNum('');
      fetchAccounts();
    } catch (err) {
      console.error('Failed to add account', err);
      alert('Failed to add bank account');
    } finally {
      setAddingLoading(false);
    }
  };

  const handleCheckBalance = async () => {
    if (pin.length !== 4) {
      setPinError('UPI PIN must be 4 digits');
      return;
    }
    setChecking(true);
    setPinError('');
    try {
      const token = await getToken();
      const res = await api.post('/bank/balance', {
        accountId: selectedAccount?.id,
        upiPin: pin
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setBalanceResult(res.data);
    } catch (err: any) {
      console.error('Failed to check balance', err);
      if (err.response?.status === 401) {
        setPinError('Wrong UPI PIN');
      } else {
        setPinError('Failed to fetch balance');
      }
    } finally {
      setChecking(false);
      setPin('');
    }
  };

  const getBankLogo = (bankName: string) => {
    const lower = bankName.toLowerCase();
    if (lower.includes('hdfc')) return 'https://icon.horse/icon/hdfcbank.com';
    if (lower.includes('sbi') || lower.includes('state bank')) return 'https://icon.horse/icon/sbi.co.in';
    if (lower.includes('icici')) return 'https://icon.horse/icon/icicibank.com';
    if (lower.includes('canara')) return 'https://icon.horse/icon/canarabank.com';
    if (lower.includes('kotak')) return 'https://icon.horse/icon/kotak.com';
    if (lower.includes('yes')) return 'https://icon.horse/icon/yesbank.in';
    if (lower.includes('axis')) return 'https://icon.horse/icon/axisbank.com';
    if (lower.includes('union')) return 'https://icon.horse/icon/unionbankofindia.co.in';
    if (lower.includes('federal')) return 'https://icon.horse/icon/federalbank.co.in';
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(bankName)}&background=10b981&color=fff&size=64&bold=true`;
  };

  const maskAccount = (accNum: string) => {
    if (!accNum) return '****';
    return `**${accNum}`;
  };

  return (
    <div className="animate-fade-up">
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
          <Wallet size={32} color="white" />
        </div>
        <div>
          <h1 style={{ fontSize: '28px' }}>Check Balance</h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>View balance for added accounts</p>
        </div>
      </div>

      {!isAdding && !selectedAccount && (
        <div className="surface-panel" style={{ padding: '24px' }}>
          <h3 style={{ marginBottom: '24px', fontSize: '18px', fontWeight: '500' }}>Bank Accounts</h3>
          
          {loading ? (
            <div style={{ textAlign: 'center', padding: '32px', color: '#a3a3a3' }}>Loading accounts...</div>
          ) : accounts.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 24px', background: '#1a1a1a', borderRadius: '16px', border: '1px dashed #333' }}>
              <Landmark size={48} color="#525252" style={{ margin: '0 auto 16px auto' }} />
              <h3 style={{ marginBottom: '8px', color: '#a3a3a3' }}>Banks not added</h3>
              <p style={{ color: '#737373', fontSize: '14px', marginBottom: '24px' }}>Link your bank account to check balance securely via UPI</p>
              <button 
                onClick={() => setIsAdding(true)}
                style={{ background: '#10b981', color: '#fff', border: 'none', padding: '12px 24px', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '8px' }}
              >
                <PlusCircle size={20} />
                Add Bank Account
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {accounts.map(acc => (
                <div 
                  key={acc.id}
                  onClick={() => setSelectedAccount(acc)}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', background: '#1e1e1e', borderRadius: '12px', cursor: 'pointer', border: '1px solid #333', transition: 'all 0.2s ease' }}
                  onMouseEnter={(e) => e.currentTarget.style.borderColor = '#10b981'}
                  onMouseLeave={(e) => e.currentTarget.style.borderColor = '#333'}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <img src={getBankLogo(acc.bankName)} alt={acc.bankName} style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#fff', objectFit: 'contain', padding: '4px' }} onError={(e) => e.currentTarget.src = 'https://icon.horse/icon/bank.com'} />
                    <div>
                      <h4 style={{ fontSize: '16px', fontWeight: '500', color: '#fff' }}>{acc.bankName}</h4>
                      <p style={{ color: '#a3a3a3', fontSize: '14px', fontFamily: 'monospace' }}>{maskAccount(acc.accountLastFour)}</p>
                    </div>
                  </div>
                  <div style={{ color: '#10b981', fontWeight: '500', fontSize: '14px' }}>
                    Check Balance
                  </div>
                </div>
              ))}

              <button 
                onClick={() => setIsAdding(true)}
                style={{ width: '100%', background: 'transparent', color: '#10b981', border: '1px dashed #10b981', padding: '16px', borderRadius: '12px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginTop: '8px' }}
              >
                <PlusCircle size={20} />
                Add Another Bank Account
              </button>
            </div>
          )}
        </div>
      )}

      {isAdding && (
        <div className="surface-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: '500' }}>Link Bank Account</h3>
            <X size={24} style={{ cursor: 'pointer', color: '#a3a3a3' }} onClick={() => setIsAdding(false)} />
          </div>

          <form onSubmit={handleAddAccount} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>Select Bank</label>
              <select 
                value={newBankName} 
                onChange={e => setNewBankName(e.target.value)}
                style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px' }}
              >
                <option value="HDFC Bank">HDFC Bank</option>
                <option value="SBI">State Bank of India (SBI)</option>
                <option value="ICICI Bank">ICICI Bank</option>
                <option value="Axis Bank">Axis Bank</option>
                <option value="Kotak Mahindra Bank">Kotak Mahindra Bank</option>
                <option value="Yes Bank">Yes Bank</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>Account Number</label>
              <input 
                type="text" 
                required 
                value={newAccNum} 
                onChange={e => setNewAccNum(e.target.value)}
                placeholder="Enter 10-14 digit account number"
                style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px' }}
              />
            </div>



            <button 
              type="submit" 
              disabled={addingLoading}
              style={{ background: '#10b981', color: '#fff', border: 'none', padding: '16px', borderRadius: '8px', fontWeight: 'bold', fontSize: '16px', cursor: 'pointer', marginTop: '16px' }}
            >
              {addingLoading ? 'Linking securely...' : 'Link Bank Account'}
            </button>
          </form>
        </div>
      )}

      {/* UPI PIN Modal */}
      {selectedAccount && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ background: '#fff', borderRadius: '16px', width: '100%', maxWidth: '400px', overflow: 'hidden', color: '#000' }}>
            
            {/* Header */}
            <div style={{ background: '#f8fafc', padding: '16px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <img src={getBankLogo(selectedAccount.bankName)} alt="Bank" style={{ width: '32px', height: '32px', borderRadius: '50%', objectFit: 'contain' }} />
                <div>
                  <h4 style={{ fontWeight: '600', fontSize: '14px' }}>{selectedAccount.bankName}</h4>
                  <p style={{ fontSize: '12px', color: '#64748b' }}>{maskAccount(selectedAccount.accountLastFour)}</p>
                </div>
              </div>
              <X size={24} style={{ cursor: 'pointer', color: '#64748b' }} onClick={() => { setSelectedAccount(null); setBalanceResult(null); setPin(''); }} />
            </div>

            <div style={{ padding: '32px 24px', textAlign: 'center' }}>
              {balanceResult ? (
                <div className="animate-fade-up">
                  <p style={{ fontSize: '14px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '8px' }}>Available Balance</p>
                  <h2 style={{ fontSize: '42px', color: '#0f172a', fontWeight: 'bold' }}>
                    {balanceResult.currency === 'INR' ? '₹' : balanceResult.currency}{balanceResult.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </h2>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', color: '#10b981', marginTop: '16px', fontWeight: '500' }}>
                    <ShieldCheck size={20} />
                    Fetched securely
                  </div>
                </div>
              ) : (
                <>
                  <p style={{ fontSize: '16px', fontWeight: '500', marginBottom: '24px', color: '#334155' }}>Enter 4-Digit UPI PIN</p>
                  
                  <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginBottom: '32px' }}>
                    {[0,1,2,3].map(i => (
                      <div key={i} style={{ width: '20px', height: '20px', borderRadius: '50%', background: pin.length > i ? '#0f172a' : '#e2e8f0', transition: 'all 0.2s' }} />
                    ))}
                  </div>
                  
                  {pinError && <p style={{ color: '#ef4444', fontSize: '14px', marginBottom: '16px' }}>{pinError}</p>}

                  {/* Custom Numpad */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', maxWidth: '280px', margin: '0 auto' }}>
                    {[1,2,3,4,5,6,7,8,9].map(num => (
                      <button 
                        key={num} 
                        onClick={() => setPin(prev => prev.length < 4 ? prev + num : prev)}
                        style={{ padding: '16px', fontSize: '24px', background: '#f8fafc', border: 'none', borderRadius: '12px', fontWeight: '500', color: '#334155', cursor: 'pointer' }}
                      >
                        {num}
                      </button>
                    ))}
                    <button onClick={() => setPin(prev => prev.slice(0, -1))} style={{ padding: '16px', fontSize: '24px', background: 'transparent', border: 'none', fontWeight: '500', color: '#ef4444', cursor: 'pointer' }}>
                      ⌫
                    </button>
                    <button onClick={() => setPin(prev => prev.length < 4 ? prev + 0 : prev)} style={{ padding: '16px', fontSize: '24px', background: '#f8fafc', border: 'none', borderRadius: '12px', fontWeight: '500', color: '#334155', cursor: 'pointer' }}>
                      0
                    </button>
                    <button 
                      onClick={handleCheckBalance} 
                      disabled={checking || pin.length !== 4}
                      style={{ padding: '16px', fontSize: '24px', background: pin.length === 4 ? '#10b981' : '#e2e8f0', color: '#fff', border: 'none', borderRadius: '12px', fontWeight: 'bold', cursor: pin.length === 4 ? 'pointer' : 'not-allowed', transition: 'background 0.2s' }}
                    >
                      {checking ? '...' : '✓'}
                    </button>
                  </div>
                </>
              )}
            </div>
            
            {!balanceResult && (
              <div style={{ padding: '16px', background: '#f1f5f9', textAlign: 'center', fontSize: '12px', color: '#64748b', borderTop: '1px solid #e2e8f0' }}>
                Powered by NPCI • Secured by SafePe
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
