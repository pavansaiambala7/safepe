import { Link } from 'react-router-dom';
import { 
  Receipt, 
  PieChart, 
  Send, 
  TrendingUp, 
  X,
  Home,
  Brain,
  Radio,
  GitFork,
  Bot
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  return (
    <>
      {/* Overlay */}
      <div 
        className={`sidebar-overlay ${isOpen ? 'open' : ''}`} 
        onClick={onClose}
      />

      {/* Drawer */}
      <div className={`sidebar ${isOpen ? 'open' : ''}`}>
        <button 
          onClick={onClose} 
          style={{ position: 'absolute', top: '24px', right: '24px', background: 'transparent', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer' }}
        >
          <X size={32} />
        </button>

        <h2 style={{ color: 'var(--color-highlight)', marginBottom: '32px', fontFamily: 'var(--font-heading)', paddingLeft: '16px' }}>
          SafePe Menu
        </h2>

        <nav style={{ display: 'flex', flexDirection: 'column' }}>
          <Link to="/" className="sidebar-link" onClick={onClose}>
            <Home size={24} color="#10b981" /> 
            Home
          </Link>

          <Link to="/split" className="sidebar-link" onClick={onClose}>
            <Receipt size={24} color="#10b981" />
            Split Bills
          </Link>
          
          <Link to="/analysis" className="sidebar-link" onClick={onClose}>
            <PieChart size={24} color="#10b981" />
            Spend Analysis
          </Link>
          
          <Link to="/fd-rates" className="sidebar-link" onClick={onClose}>
            <TrendingUp size={24} color="#10b981" />
            Fixed Deposits Rates
          </Link>
          
          <Link to="/chatbot" className="sidebar-link" onClick={onClose}>
            <Bot size={24} color="#10b981" />
            AI Money Assistant & Scam Shield
          </Link>

          <Link to="/events" className="sidebar-link" onClick={onClose}>
            <Radio size={24} color="#06b6d4" />
            Kafka Event Monitor
          </Link>

          {/* GitHub Author Profile */}
          <a 
            href="https://github.com/pavansaiambala7" 
            target="_blank" 
            rel="noopener noreferrer" 
            className="sidebar-link"
            style={{ marginTop: 'auto', borderTop: '1px solid var(--color-border)', paddingTop: '16px' }}
          >
            <GitFork size={24} color="#1e293b" />
            Author: @pavansaiambala7
          </a>
        </nav>
      </div>
    </>
  );
}
