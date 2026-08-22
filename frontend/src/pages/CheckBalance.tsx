import { useState, useEffect } from 'react';
import { Wallet, Landmark, PlusCircle, ShieldCheck, X, Lock } from 'lucide-react';
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
  const [balanceResult, setBalanceResult] = useState<{ balance: number; currency: string; bankName: string; accountLastFour: string } | null>(null);
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

  const handlePinPress = (num: number) => {
    if (pin.length < 4) setPin(prev => prev + num);
  };

  const handleBackspace = () => {
    setPin(prev => prev.slice(0, -1));
  };

  const resetModal = () => {
    setSelectedAccount(null);
    setBalanceResult(null);
    setPin('');
    setPinError('');
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

      {/* ═══════════════════════════════════════════════════════════════ */}
      {/* NPCI-Standard UPI PIN Modal                                   */}
      {/* ═══════════════════════════════════════════════════════════════ */}
      {selectedAccount && (
        <div style={{ 
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
          background: 'rgba(0,0,0,0.7)', 
          display: 'flex', alignItems: 'flex-end', justifyContent: 'center', 
          zIndex: 1000,
          animation: 'npci-overlay-in 0.3s ease-out'
        }}>
          <div style={{ 
            background: '#ffffff', 
            borderRadius: '24px 24px 0 0', 
            width: '100%', 
            maxWidth: '420px', 
            overflow: 'hidden',
            animation: 'npci-slide-up 0.4s cubic-bezier(0.16, 1, 0.3, 1)'
          }}>
            
            {/* ── NPCI Header Bar ──────────────────────────────────── */}
            <div style={{ 
              background: 'linear-gradient(135deg, #1a237e, #283593)', 
              padding: '20px 20px 16px 20px',
              color: '#fff'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <div style={{ 
                    width: '36px', height: '36px', borderRadius: '8px', 
                    background: 'rgba(255,255,255,0.15)', 
                    display: 'flex', alignItems: 'center', justifyContent: 'center' 
                  }}>
                    <img 
                      src={getBankLogo(selectedAccount.bankName)} 
                      alt="Bank" 
                      style={{ width: '24px', height: '24px', borderRadius: '4px', objectFit: 'contain' }} 
                    />
                  </div>
                  <div>
                    <p style={{ fontSize: '15px', fontWeight: '600', margin: 0 }}>{selectedAccount.bankName}</p>
                    <p style={{ fontSize: '12px', opacity: 0.8, margin: 0, fontFamily: 'monospace' }}>
                      A/C {maskAccount(selectedAccount.accountLastFour)}
                    </p>
                  </div>
                </div>
                <button
                  onClick={resetModal}
                  style={{ 
                    background: 'rgba(255,255,255,0.15)', border: 'none', 
                    width: '32px', height: '32px', borderRadius: '50%', 
                    display: 'flex', alignItems: 'center', justifyContent: 'center', 
                    cursor: 'pointer' 
                  }}
                >
                  <X size={18} color="#fff" />
                </button>
              </div>

              {/* Transaction Info */}
              <div style={{ 
                background: 'rgba(255,255,255,0.1)', 
                borderRadius: '12px', 
                padding: '12px 16px',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between'
              }}>
                <div>
                  <p style={{ fontSize: '11px', opacity: 0.7, margin: '0 0 2px 0', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Transaction</p>
                  <p style={{ fontSize: '14px', fontWeight: '600', margin: 0 }}>Balance Enquiry</p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p style={{ fontSize: '11px', opacity: 0.7, margin: '0 0 2px 0', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Via</p>
                  <p style={{ fontSize: '14px', fontWeight: '600', margin: 0 }}>UPI</p>
                </div>
              </div>
            </div>

            {/* ── Body Content ─────────────────────────────────────── */}
            <div style={{ padding: '24px 20px', background: '#f8f9fa' }}>

              {balanceResult ? (
                /* ── Balance Result Screen ──────────────────────── */
                <div style={{ textAlign: 'center', animation: 'npci-fade-in 0.4s ease-out' }}>
                  <div style={{ 
                    width: '64px', height: '64px', borderRadius: '50%', 
                    background: '#e8f5e9', 
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    margin: '0 auto 16px auto'
                  }}>
                    <ShieldCheck size={32} color="#2e7d32" />
                  </div>
                  
                  <p style={{ fontSize: '13px', color: '#78909c', textTransform: 'uppercase', letterSpacing: '1.5px', marginBottom: '4px' }}>
                    Available Balance
                  </p>
                  <h2 style={{ fontSize: '36px', color: '#1a237e', fontWeight: '800', margin: '0 0 8px 0' }}>
                    ₹{balanceResult.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </h2>
                  
                  <div style={{ 
                    display: 'inline-flex', alignItems: 'center', gap: '6px', 
                    background: '#e8f5e9', color: '#2e7d32', 
                    padding: '6px 16px', borderRadius: '20px', 
                    fontSize: '12px', fontWeight: '600' 
                  }}>
                    <ShieldCheck size={14} />
                    Verified via NPCI
                  </div>

                  <div style={{ 
                    marginTop: '24px', padding: '12px', 
                    background: '#fff', borderRadius: '12px', 
                    border: '1px solid #e0e0e0',
                    fontSize: '12px', color: '#78909c', textAlign: 'left'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <span>Bank</span>
                      <span style={{ color: '#37474f', fontWeight: '500' }}>{balanceResult.bankName}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <span>Account</span>
                      <span style={{ color: '#37474f', fontWeight: '500', fontFamily: 'monospace' }}>XXXX{balanceResult.accountLastFour}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span>Time</span>
                      <span style={{ color: '#37474f', fontWeight: '500' }}>{new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                  </div>

                  <button
                    onClick={resetModal}
                    style={{ 
                      width: '100%', marginTop: '20px', padding: '14px', 
                      background: '#1a237e', color: '#fff', border: 'none', 
                      borderRadius: '12px', fontSize: '15px', fontWeight: '600', 
                      cursor: 'pointer' 
                    }}
                  >
                    Done
                  </button>
                </div>
              ) : (
                /* ── UPI PIN Entry Screen ──────────────────────── */
                <>
                  <div style={{ textAlign: 'center', marginBottom: '20px' }}>
                    <Lock size={24} color="#1a237e" style={{ marginBottom: '8px' }} />
                    <p style={{ fontSize: '15px', fontWeight: '600', color: '#263238', margin: '0 0 4px 0' }}>
                      Enter UPI PIN
                    </p>
                    <p style={{ fontSize: '12px', color: '#78909c', margin: 0 }}>
                      Enter your 4-digit UPI PIN to check balance
                    </p>
                  </div>

                  {/* PIN Dots */}
                  <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', marginBottom: '8px' }}>
                    {[0, 1, 2, 3].map(i => (
                      <div key={i} style={{ 
                        width: '48px', height: '48px', 
                        borderRadius: '12px',
                        border: `2px solid ${pin.length === i ? '#1a237e' : pin.length > i ? '#1a237e' : '#cfd8dc'}`,
                        background: pin.length > i ? '#1a237e' : '#fff',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        transition: 'all 0.15s ease',
                        boxShadow: pin.length === i ? '0 0 0 3px rgba(26, 35, 126, 0.15)' : 'none'
                      }}>
                        {pin.length > i && (
                          <div style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#fff' }} />
                        )}
                      </div>
                    ))}
                  </div>
                  
                  {pinError && (
                    <p style={{ color: '#d32f2f', fontSize: '13px', textAlign: 'center', margin: '8px 0 0 0', fontWeight: '500' }}>
                      {pinError}
                    </p>
                  )}

                  {/* ── NPCI Numpad ─────────────────────────────── */}
                  <div style={{ 
                    display: 'grid', 
                    gridTemplateColumns: 'repeat(3, 1fr)', 
                    gap: '8px', 
                    maxWidth: '300px', 
                    margin: '20px auto 0 auto' 
                  }}>
                    {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(num => (
                      <button 
                        key={num} 
                        onClick={() => handlePinPress(num)}
                        style={{ 
                          padding: '16px', 
                          fontSize: '22px', 
                          background: '#fff', 
                          border: '1px solid #e0e0e0', 
                          borderRadius: '12px', 
                          fontWeight: '600', 
                          color: '#263238', 
                          cursor: 'pointer',
                          transition: 'all 0.1s ease',
                          userSelect: 'none',
                          WebkitTapHighlightColor: 'transparent'
                        }}
                        onMouseDown={(e) => {
                          e.currentTarget.style.background = '#e8eaf6';
                          e.currentTarget.style.transform = 'scale(0.95)';
                        }}
                        onMouseUp={(e) => {
                          e.currentTarget.style.background = '#fff';
                          e.currentTarget.style.transform = 'scale(1)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = '#fff';
                          e.currentTarget.style.transform = 'scale(1)';
                        }}
                      >
                        {num}
                      </button>
                    ))}
                    
                    {/* Backspace */}
                    <button 
                      onClick={handleBackspace}
                      style={{ 
                        padding: '16px', fontSize: '20px', 
                        background: 'transparent', border: '1px solid #e0e0e0', 
                        borderRadius: '12px', fontWeight: '500', 
                        color: '#d32f2f', cursor: 'pointer',
                        transition: 'all 0.1s ease'
                      }}
                    >
                      ⌫
                    </button>
                    
                    {/* Zero */}
                    <button 
                      onClick={() => handlePinPress(0)}
                      style={{ 
                        padding: '16px', fontSize: '22px', 
                        background: '#fff', border: '1px solid #e0e0e0', 
                        borderRadius: '12px', fontWeight: '600', 
                        color: '#263238', cursor: 'pointer',
                        transition: 'all 0.1s ease'
                      }}
                      onMouseDown={(e) => {
                        e.currentTarget.style.background = '#e8eaf6';
                        e.currentTarget.style.transform = 'scale(0.95)';
                      }}
                      onMouseUp={(e) => {
                        e.currentTarget.style.background = '#fff';
                        e.currentTarget.style.transform = 'scale(1)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = '#fff';
                        e.currentTarget.style.transform = 'scale(1)';
                      }}
                    >
                      0
                    </button>
                    
                    {/* Submit */}
                    <button 
                      onClick={handleCheckBalance} 
                      disabled={checking || pin.length !== 4}
                      style={{ 
                        padding: '16px', fontSize: '18px', 
                        background: pin.length === 4 ? '#1a237e' : '#cfd8dc', 
                        color: '#fff', border: 'none', borderRadius: '12px', 
                        fontWeight: 'bold', 
                        cursor: pin.length === 4 ? 'pointer' : 'not-allowed', 
                        transition: 'all 0.2s ease' 
                      }}
                    >
                      {checking ? '...' : '✓'}
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* ── NPCI Footer ──────────────────────────────────────── */}
            <div style={{ 
              padding: '12px 20px 20px 20px', 
              background: '#f8f9fa',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px',
              borderTop: '1px solid #eeeeee'
            }}>
              <span style={{ fontSize: '11px', color: '#90a4ae', fontWeight: '500' }}>Powered by</span>
              <div style={{ 
                display: 'flex', alignItems: 'center', gap: '4px',
                background: '#fff', padding: '4px 12px', borderRadius: '6px',
                border: '1px solid #e0e0e0'
              }}>
                <span style={{ fontSize: '12px', fontWeight: '700', color: '#1a237e' }}>NPCI</span>
              </div>
              <div style={{ width: '1px', height: '16px', background: '#e0e0e0' }} />
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Lock size={10} color="#78909c" />
                <span style={{ fontSize: '11px', color: '#90a4ae', fontWeight: '500' }}>Secured by SafePe</span>
              </div>
            </div>
          </div>

          {/* ── Animations ─────────────────────────────────────────── */}
          <style>{`
            @keyframes npci-overlay-in {
              from { opacity: 0; }
              to { opacity: 1; }
            }
            @keyframes npci-slide-up {
              from { transform: translateY(100%); }
              to { transform: translateY(0); }
            }
            @keyframes npci-fade-in {
              from { opacity: 0; transform: scale(0.95); }
              to { opacity: 1; transform: scale(1); }
            }
          `}</style>
        </div>
      )}
    </div>
  );
}
