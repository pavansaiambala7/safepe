import { useState } from 'react';
import { PieChart, TrendingDown, TrendingUp, Plus } from 'lucide-react';

export default function SpendAnalysis() {
  const [hasSpends, setHasSpends] = useState(false);
  const [activeCategory, setActiveCategory] = useState({ name: 'No Spends Yet', percent: 0 });

  const radius = 40;
  const circumference = 2 * Math.PI * radius; // 251.327

  // If no spends, show a 100% gray circle
  // If spends, show actual data
  const foodPercent = hasSpends ? 60 : 0;
  const shoppingPercent = hasSpends ? 25 : 0;
  const billsPercent = hasSpends ? 15 : 0;

  const foodDash = (foodPercent / 100) * circumference;
  const shoppingDash = (shoppingPercent / 100) * circumference;
  const billsDash = (billsPercent / 100) * circumference;

  const totalSpent = hasSpends ? "14,500" : "0";

  const handleSimulateSpend = () => {
    setHasSpends(true);
    setActiveCategory({ name: 'Food & Dining', percent: 60 });
  };

  return (
    <div className="animate-fade-up">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
            <PieChart size={32} color="white" />
          </div>
          <div>
            <h1 style={{ fontSize: '28px' }}>Spend Analysis</h1>
            <p style={{ color: 'var(--color-text-secondary)' }}>Track your monthly expenses.</p>
          </div>
        </div>
        
        {/* Simulate button for testing */}
        {!hasSpends && (
          <button 
            onClick={handleSimulateSpend}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'var(--color-primary)', color: 'white', border: 'none', padding: '8px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: '600' }}
          >
            <Plus size={18} /> Spend Money
          </button>
        )}
      </div>

      <div className="surface-panel" style={{ textAlign: 'center', padding: '40px 24px', marginBottom: '24px' }}>
        <h2 style={{ color: 'var(--color-text-secondary)', marginBottom: '16px' }}>Total Spent (This Month)</h2>
        <div style={{ fontSize: '56px', fontWeight: '800', color: 'black', marginBottom: '24px' }}>₹{totalSpent}</div>
        
        {/* Interactive SVG Donut Chart */}
        <div style={{ position: 'relative', width: '200px', height: '200px', margin: '0 auto 24px auto' }}>
          <svg viewBox="0 0 100 100" style={{ transform: 'rotate(-90deg)', width: '100%', height: '100%', overflow: 'visible' }}>
            
            {/* Empty State Background Circle */}
            {!hasSpends && (
              <circle
                cx="50" cy="50" r={radius}
                fill="transparent"
                stroke="var(--color-border)"
                strokeWidth="15"
              />
            )}

            {/* Food */}
            {hasSpends && (
              <circle
                cx="50" cy="50" r={radius}
                fill="transparent"
                stroke="#ef4444"
                strokeWidth="15"
                strokeDasharray={`${foodDash} ${circumference}`}
                strokeDashoffset="0"
                style={{ cursor: 'pointer', transition: 'stroke-width 0.2s', strokeWidth: activeCategory.name === 'Food & Dining' ? '20' : '15' }}
                onClick={() => setActiveCategory({ name: 'Food & Dining', percent: 60 })}
              />
            )}

            {/* Shopping */}
            {hasSpends && (
              <circle
                cx="50" cy="50" r={radius}
                fill="transparent"
                stroke="#3b82f6"
                strokeWidth="15"
                strokeDasharray={`${shoppingDash} ${circumference}`}
                strokeDashoffset={-foodDash}
                style={{ cursor: 'pointer', transition: 'stroke-width 0.2s', strokeWidth: activeCategory.name === 'Shopping' ? '20' : '15' }}
                onClick={() => setActiveCategory({ name: 'Shopping', percent: 25 })}
              />
            )}

            {/* Bills */}
            {hasSpends && (
              <circle
                cx="50" cy="50" r={radius}
                fill="transparent"
                stroke="#10b981"
                strokeWidth="15"
                strokeDasharray={`${billsDash} ${circumference}`}
                strokeDashoffset={-(foodDash + shoppingDash)}
                style={{ cursor: 'pointer', transition: 'stroke-width 0.2s', strokeWidth: activeCategory.name === 'Bills & Utilities' ? '20' : '15' }}
                onClick={() => setActiveCategory({ name: 'Bills & Utilities', percent: 15 })}
              />
            )}
          </svg>

          {/* Center Text overlay */}
          <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', pointerEvents: 'none' }}>
            <span style={{ fontSize: '32px', fontWeight: '800', color: 'black', transition: 'all 0.3s' }}>{activeCategory.percent}%</span>
            <span style={{ fontSize: '13px', color: 'var(--color-text-secondary)', fontWeight: '500' }}>{activeCategory.name}</span>
          </div>
        </div>

        {hasSpends ? (
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', color: 'var(--color-accent)', background: 'rgba(16, 185, 129, 0.1)', padding: '8px 16px', borderRadius: '20px' }}>
            <TrendingDown size={18} />
            <span>12% less than last month</span>
          </div>
        ) : (
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', color: 'var(--color-text-secondary)', background: 'var(--color-bg-body)', padding: '8px 16px', borderRadius: '20px' }}>
            <span>No expenses to compare yet</span>
          </div>
        )}
      </div>

      <h3 style={{ marginBottom: '16px' }}>Top Categories</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        
        <div className="surface-panel" style={{ padding: '16px', cursor: hasSpends ? 'pointer' : 'default', border: activeCategory.name === 'Food & Dining' ? '1px solid #ef4444' : '' }} onClick={() => hasSpends && setActiveCategory({ name: 'Food & Dining', percent: 60 })}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontWeight: '500' }}>Food & Dining</span>
            <span style={{ fontWeight: '600' }}>₹{hasSpends ? '8,700' : '0'}</span>
          </div>
          <div style={{ width: '100%', height: '8px', background: 'rgba(0,0,0,0.1)', borderRadius: '4px' }}>
            <div style={{ width: `${foodPercent}%`, height: '100%', background: '#ef4444', borderRadius: '4px', transition: 'width 0.5s ease-out' }} />
          </div>
        </div>

        <div className="surface-panel" style={{ padding: '16px', cursor: hasSpends ? 'pointer' : 'default', border: activeCategory.name === 'Shopping' ? '1px solid #3b82f6' : '' }} onClick={() => hasSpends && setActiveCategory({ name: 'Shopping', percent: 25 })}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontWeight: '500' }}>Shopping</span>
            <span style={{ fontWeight: '600' }}>₹{hasSpends ? '3,625' : '0'}</span>
          </div>
          <div style={{ width: '100%', height: '8px', background: 'rgba(0,0,0,0.1)', borderRadius: '4px' }}>
            <div style={{ width: `${shoppingPercent}%`, height: '100%', background: '#3b82f6', borderRadius: '4px', transition: 'width 0.5s ease-out' }} />
          </div>
        </div>

        <div className="surface-panel" style={{ padding: '16px', cursor: hasSpends ? 'pointer' : 'default', border: activeCategory.name === 'Bills & Utilities' ? '1px solid #10b981' : '' }} onClick={() => hasSpends && setActiveCategory({ name: 'Bills & Utilities', percent: 15 })}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontWeight: '500' }}>Bills & Utilities</span>
            <span style={{ fontWeight: '600' }}>₹{hasSpends ? '2,175' : '0'}</span>
          </div>
          <div style={{ width: '100%', height: '8px', background: 'rgba(0,0,0,0.1)', borderRadius: '4px' }}>
            <div style={{ width: `${billsPercent}%`, height: '100%', background: '#10b981', borderRadius: '4px', transition: 'width 0.5s ease-out' }} />
          </div>
        </div>

      </div>
    </div>
  );
}
