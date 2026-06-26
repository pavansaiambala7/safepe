import { useState } from 'react';
import { Receipt, UserPlus, IndianRupee, Trash2, User } from 'lucide-react';

interface Person {
  id: number;
  name: string;
  phone: string;
}

export default function SplitBills() {
  const [amount, setAmount] = useState('1000');
  const [persons, setPersons] = useState<Person[]>([
    { id: 1, name: 'Myself', phone: '' }
  ]);

  const addPerson = () => {
    setPersons([...persons, { id: Date.now(), name: '', phone: '' }]);
  };

  const removePerson = (id: number) => {
    if (persons.length > 1) {
      setPersons(persons.filter(p => p.id !== id));
    }
  };

  const updatePerson = (id: number, field: 'name' | 'phone', value: string) => {
    setPersons(persons.map(p => p.id === id ? { ...p, [field]: value } : p));
  };

  const splitAmount = parseFloat(amount || '0') / persons.length;

  return (
    <div className="animate-fade-up">
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
          <Receipt size={32} color="white" />
        </div>
        <div>
          <h1 style={{ fontSize: '28px' }}>Split Bills</h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>Divide expenses seamlessly.</p>
        </div>
      </div>

      <div className="surface-panel">
        <h3 style={{ marginBottom: '16px' }}>Total Amount</h3>
        <div style={{ position: 'relative', marginBottom: '32px' }}>
          <IndianRupee size={20} color="var(--color-text-secondary)" style={{ position: 'absolute', left: '16px', top: '18px' }} />
          <input 
            type="number" 
            className="input-field" 
            style={{ paddingLeft: '48px', fontSize: '24px', fontWeight: '600' }} 
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ margin: 0 }}>People ({persons.length})</h3>
          <button 
            onClick={addPerson}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'transparent', border: 'none', color: 'var(--color-primary)', fontWeight: '600', cursor: 'pointer', fontSize: '14px' }}
          >
            <UserPlus size={18} /> Add Person
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '32px' }}>
          {persons.map((person, index) => (
            <div key={person.id} style={{ display: 'flex', alignItems: 'center', gap: '12px', background: '#f8fafc', padding: '12px', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '10px', borderRadius: '50%' }}>
                <User size={20} color="var(--color-primary)" />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flexGrow: 1 }}>
                <input 
                  type="text" 
                  placeholder="Name" 
                  value={person.name}
                  onChange={(e) => updatePerson(person.id, 'name', e.target.value)}
                  style={{ border: 'none', background: 'transparent', fontSize: '15px', fontWeight: '500', outline: 'none', color: 'var(--color-text-primary)' }}
                  autoFocus={index > 0 && person.name === ''}
                />
                <input 
                  type="tel" 
                  placeholder="Phone Number" 
                  value={person.phone}
                  onChange={(e) => updatePerson(person.id, 'phone', e.target.value)}
                  style={{ border: 'none', background: 'transparent', fontSize: '13px', outline: 'none', color: 'var(--color-text-secondary)' }}
                />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
                <span style={{ fontWeight: '600', color: 'var(--color-accent)' }}>₹{splitAmount.toFixed(2)}</span>
                {persons.length > 1 && (
                  <button onClick={() => removePerson(person.id)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: '4px' }}>
                    <Trash2 size={16} color="var(--color-danger)" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>

        <button className="btn-primary" style={{ width: '100%' }}>
          Send Requests
        </button>
      </div>
    </div>
  );
}
