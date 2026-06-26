import { useState, useEffect } from 'react';
import { CreditCard, PlusCircle, X, Eye, EyeOff, Trash2 } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

interface SavedCard {
  id: string;
  type: 'debit' | 'credit';
  cardNumber: string; // masked
  bankName: string;
  expiryMonth: string;
  expiryYear: string;
}

export default function Cards() {
  const [cards, setCards] = useState<SavedCard[]>([]);
  const [loading, setLoading] = useState(false);
  const { getToken } = useAuth();
  
  // Add Card State
  const [isAdding, setIsAdding] = useState(false);
  const [cardType, setCardType] = useState<'debit' | 'credit'>('debit');
  const [cardNumber, setCardNumber] = useState('');
  const [cardName, setCardName] = useState('');
  const [expiryMonth, setExpiryMonth] = useState('');
  const [expiryYear, setExpiryYear] = useState('');
  const [cvv, setCvv] = useState('');
  const [addingLoading, setAddingLoading] = useState(false);

  // For demo: store cards in local state (localStorage)
  useEffect(() => {
    const saved = localStorage.getItem('safepe_cards');
    if (saved) {
      setCards(JSON.parse(saved));
    }
  }, []);

  const saveCards = (newCards: SavedCard[]) => {
    setCards(newCards);
    localStorage.setItem('safepe_cards', JSON.stringify(newCards));
  };

  const handleAddCard = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddingLoading(true);
    
    try {
      const token = await getToken();
      const res = await api.post('/vault/cards', {
        cardNumber: cardNumber.replace(/\s+/g, ''), // Send without spaces
        expiryDate: `${expiryMonth}/${expiryYear}`,
        cvv,
        razorpayTokenId: `tok_card_${Date.now()}` // Mocking Razorpay.js
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      const newCard: SavedCard = {
        id: res.data.tokenId || Date.now().toString(),
        type: cardType,
        cardNumber: res.data.maskedCard || ('**** ' + cardNumber.slice(-4)),
        bankName: cardName || 'Unknown Bank',
        expiryMonth,
        expiryYear
      };
      
      saveCards([...cards, newCard]);
      setIsAdding(false);
      setCardNumber('');
      setCardName('');
      setExpiryMonth('');
      setExpiryYear('');
      setCvv('');
    } catch (err) {
      console.error('Failed to tokenize card', err);
      alert('Failed to add card securely');
    } finally {
      setAddingLoading(false);
    }
  };

  const handleDeleteCard = (id: string) => {
    saveCards(cards.filter(c => c.id !== id));
  };

  const formatCardInput = (val: string) => {
    const nums = val.replace(/\D/g, '');
    const groups = nums.match(/.{1,4}/g);
    return groups ? groups.join(' ') : '';
  };

  return (
    <div className="animate-fade-up">
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
          <CreditCard size={32} color="white" />
        </div>
        <div>
          <h1 style={{ fontSize: '28px' }}>Cards</h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>Manage your debit & credit cards</p>
        </div>
      </div>

      {!isAdding && (
        <div className="surface-panel" style={{ padding: '24px' }}>
          <h3 style={{ marginBottom: '24px', fontSize: '18px', fontWeight: '500' }}>Saved Cards</h3>
          
          {cards.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 24px', background: '#1a1a1a', borderRadius: '16px', border: '1px dashed #333' }}>
              <CreditCard size={48} color="#525252" style={{ margin: '0 auto 16px auto' }} />
              <h3 style={{ marginBottom: '8px', color: '#a3a3a3' }}>No cards added</h3>
              <p style={{ color: '#737373', fontSize: '14px', marginBottom: '24px' }}>Add your debit or credit card for quick payments</p>
              <button 
                onClick={() => setIsAdding(true)}
                style={{ background: '#10b981', color: '#fff', border: 'none', padding: '12px 24px', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '8px' }}
              >
                <PlusCircle size={20} />
                Add Card
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {cards.map(card => (
                <div 
                  key={card.id}
                  style={{ 
                    background: card.type === 'credit' 
                      ? 'linear-gradient(135deg, #1e293b, #334155)' 
                      : 'linear-gradient(135deg, #064e3b, #065f46)',
                    borderRadius: '16px', padding: '20px', color: '#fff', position: 'relative'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
                    <span style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px', opacity: 0.7 }}>
                      {card.type} Card
                    </span>
                    <button 
                      onClick={() => handleDeleteCard(card.id)}
                      style={{ background: 'rgba(255,255,255,0.15)', border: 'none', padding: '6px', borderRadius: '8px', cursor: 'pointer' }}
                    >
                      <Trash2 size={14} color="#fff" />
                    </button>
                  </div>
                  <div style={{ fontSize: '22px', fontFamily: 'monospace', letterSpacing: '3px', marginBottom: '16px' }}>
                    {card.cardNumber}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '14px', fontWeight: '500' }}>{card.bankName}</span>
                    <span style={{ fontSize: '13px', opacity: 0.8 }}>{card.expiryMonth}/{card.expiryYear}</span>
                  </div>
                </div>
              ))}

              <button 
                onClick={() => setIsAdding(true)}
                style={{ width: '100%', background: 'transparent', color: '#10b981', border: '1px dashed #10b981', padding: '16px', borderRadius: '12px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginTop: '8px' }}
              >
                <PlusCircle size={20} />
                Add Another Card
              </button>
            </div>
          )}
        </div>
      )}

      {isAdding && (
        <div className="surface-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: '500' }}>Add New Card</h3>
            <X size={24} style={{ cursor: 'pointer', color: '#a3a3a3' }} onClick={() => setIsAdding(false)} />
          </div>

          <form onSubmit={handleAddCard} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {/* Card Type Toggle */}
            <div style={{ display: 'flex', gap: '12px' }}>
              <button 
                type="button"
                onClick={() => setCardType('debit')}
                style={{ 
                  flex: 1, padding: '12px', borderRadius: '8px', fontWeight: '600', cursor: 'pointer',
                  background: cardType === 'debit' ? '#10b981' : '#1e1e1e',
                  color: cardType === 'debit' ? '#fff' : '#a3a3a3',
                  border: `1px solid ${cardType === 'debit' ? '#10b981' : '#333'}`
                }}
              >
                Debit Card
              </button>
              <button 
                type="button"
                onClick={() => setCardType('credit')}
                style={{ 
                  flex: 1, padding: '12px', borderRadius: '8px', fontWeight: '600', cursor: 'pointer',
                  background: cardType === 'credit' ? '#10b981' : '#1e1e1e',
                  color: cardType === 'credit' ? '#fff' : '#a3a3a3',
                  border: `1px solid ${cardType === 'credit' ? '#10b981' : '#333'}`
                }}
              >
                Credit Card
              </button>
            </div>

            <div>
              <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>Card Number</label>
              <input 
                type="text" 
                required 
                maxLength={19}
                value={formatCardInput(cardNumber)} 
                onChange={e => setCardNumber(e.target.value.replace(/\D/g, ''))}
                placeholder="1234 5678 9012 3456"
                style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px', letterSpacing: '2px', fontFamily: 'monospace' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>Bank Name / Card Issuer</label>
              <input 
                type="text" 
                required 
                value={cardName} 
                onChange={e => setCardName(e.target.value)}
                placeholder="e.g., HDFC Bank"
                style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px' }}
              />
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>Expiry Month</label>
                <input 
                  type="text" 
                  required 
                  maxLength={2}
                  value={expiryMonth} 
                  onChange={e => setExpiryMonth(e.target.value.replace(/\D/g, ''))}
                  placeholder="MM"
                  style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px', textAlign: 'center' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>Expiry Year</label>
                <input 
                  type="text" 
                  required 
                  maxLength={2}
                  value={expiryYear} 
                  onChange={e => setExpiryYear(e.target.value.replace(/\D/g, ''))}
                  placeholder="YY"
                  style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px', textAlign: 'center' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', color: '#a3a3a3', fontSize: '14px', marginBottom: '8px' }}>CVV</label>
                <input 
                  type="password" 
                  required 
                  maxLength={3}
                  value={cvv} 
                  onChange={e => setCvv(e.target.value.replace(/\D/g, ''))}
                  placeholder="•••"
                  style={{ width: '100%', background: '#1e1e1e', border: '1px solid #333', padding: '12px', borderRadius: '8px', color: '#fff', fontSize: '16px', textAlign: 'center' }}
                />
              </div>
            </div>

            <button 
              type="submit" 
              disabled={addingLoading}
              style={{ background: '#10b981', color: '#fff', border: 'none', padding: '16px', borderRadius: '8px', fontWeight: 'bold', fontSize: '16px', cursor: 'pointer', marginTop: '16px' }}
            >
              {addingLoading ? 'Adding securely...' : 'Add Card'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
