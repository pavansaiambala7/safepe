import { useUser, useAuth } from '@clerk/react';
import { 
  Send, QrCode, Landmark, CreditCard,
  Smartphone, Lightbulb, Tv, Wifi,
  Wallet, History, ArrowRightLeft
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import api from '../api';

interface Transaction {
  id: string;
  type: string;
  amount: number;
  currency: string;
  status: string;
  createdAt: string;
  merchant?: {
    name: string;
  };
}

export default function Dashboard() {
  const { user } = useUser();
  const { getToken } = useAuth();
  
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        const token = await getToken();
        const res = await api.get('/history/transactions', {
          headers: { Authorization: `Bearer ${token}` }
        });
        setTransactions(res.data);
      } catch (err) {
        console.error('Failed to fetch transactions', err);
      } finally {
        setLoading(false);
      }
    };
    fetchHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Compute contacts from transactions
  const contacts = transactions
    .filter(t => t.merchant && t.merchant.name)
    .map(t => ({
      name: t.merchant?.name || 'Unknown',
      initial: t.merchant?.name?.charAt(0) || 'U',
      amount: `₹${t.amount}`,
      time: new Date(t.createdAt).toLocaleDateString(),
      color: '#10b981'
    }))
    // unique by name
    .filter((c, index, self) => index === self.findIndex((t) => t.name === c.name))
    .slice(0, 5);

  return (
    <div className="animate-fade-up" style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>

      {/* Part 1: Quick Payments */}
      <div className="surface-panel" style={{ padding: '24px 16px' }}>
        <h3 style={{ marginBottom: '16px', fontSize: '16px', color: 'var(--color-text-secondary)' }}>Transfer Money</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', textAlign: 'center' }}>
          
          <Link to="/pay" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Send size={28} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Pay<br/>UPI ID</span>
            </div>
          </Link>

          <Link to="/scan-qr" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <QrCode size={28} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Scan<br/>QR</span>
            </div>
          </Link>

          <Link to="/qr" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <QrCode size={28} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Receive<br/>QR</span>
            </div>
          </Link>

          <Link to="/bank-transfer" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <ArrowRightLeft size={28} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Bank<br/>Transfer</span>
            </div>
          </Link>

        </div>
      </div>

      {/* Part 2: Utilities (DTH, Electricity, Postpaid, Mobile) */}
      <div className="surface-panel" style={{ padding: '24px 16px' }}>
        <h3 style={{ marginBottom: '16px', fontSize: '16px', color: 'var(--color-text-secondary)' }}>Recharge & Pay Bills</h3>
        
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', textAlign: 'center' }}>
          
          <Link to="/utilities/mobile" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Smartphone size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Mobile<br/>Recharge</span>
            </div>
          </Link>

          <Link to="/utilities/electricity" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Lightbulb size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Electricity</span>
            </div>
          </Link>

          <Link to="/utilities/dth" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Tv size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>DTH</span>
            </div>
          </Link>

          <Link to="/utilities/postpaid" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Wifi size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Postpaid</span>
            </div>
          </Link>

        </div>
      </div>

      {/* Part 2b: Banking Quick Actions */}
      <div className="surface-panel" style={{ padding: '24px 16px' }}>
        <h3 style={{ marginBottom: '16px', fontSize: '16px', color: 'var(--color-text-secondary)' }}>Banking</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', textAlign: 'center' }}>
          
          <Link to="/balance" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Wallet size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Check<br/>Balance</span>
            </div>
          </Link>

          <Link to="/balance" state={{ openAddBank: true }} style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Landmark size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Bank<br/>Accounts</span>
            </div>
          </Link>

          <Link to="/cards" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CreditCard size={24} color="var(--color-primary)" />
              </div>
              <span style={{ fontSize: '13px', fontWeight: '500' }}>Cards</span>
            </div>
          </Link>

        </div>
      </div>

      {/* Part 3: History & Tools */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '16px' }}>
        
        <Link to="/history" style={{ textDecoration: 'none', color: 'inherit' }}>
          <div className="surface-panel" style={{ padding: '20px', display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ background: '#1e1e1e', padding: '12px', borderRadius: '12px' }}>
              <History size={24} color="#a3a3a3" />
            </div>
            <div style={{ flex: 1 }}>
              <h4 style={{ fontSize: '16px', fontWeight: '600' }}>Recent Transactions</h4>
              <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)' }}>View your complete history</p>
            </div>
            <div style={{ color: '#10b981', fontWeight: 'bold' }}>
              {transactions.length > 0 ? `${transactions.length} records` : 'No history yet'}
            </div>
          </div>
        </Link>

      </div>

      {/* Part 4: Contacts & Recent Transactions */}
      <div>
        <h3 style={{ marginBottom: '16px', fontSize: '16px', color: 'var(--color-text-secondary)', paddingLeft: '8px' }}>Recent Contacts</h3>
        <div style={{ display: 'flex', gap: '16px', overflowX: 'auto', paddingBottom: '16px', paddingLeft: '8px' }}>
          
          {contacts.length === 0 ? (
            <div style={{ padding: '16px', color: '#737373', fontSize: '14px', fontStyle: 'italic' }}>
              No recent contacts. Send money to see them here.
            </div>
          ) : (
            contacts.map((contact, idx) => (
              <div key={idx} className="surface-panel" style={{ minWidth: '140px', padding: '16px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: contact.color, color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px', fontWeight: '600', marginBottom: '12px' }}>
                  {contact.initial}
                </div>
                <h4 style={{ fontSize: '14px', marginBottom: '8px' }}>{contact.name}</h4>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '14px', fontWeight: '600', color: 'var(--color-text-primary)' }}>
                  {contact.amount}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                  {contact.time}
                </div>
              </div>
            ))
          )}

        </div>
      </div>

    </div>
  );
}
