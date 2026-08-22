import { Link } from 'react-router-dom';
import { 
  Receipt, 
  PieChart, 
  Send, 
  TrendingUp, 
  ShieldAlert,
  X,
  Home,
  Brain,
  Radio,
  Github
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
          Menu
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
            <ShieldAlert size={24} color="#ef4444" />
            Fraud Detector
          </Link>

          <Link to="/agentic-fraud" className="sidebar-link" onClick={onClose}>
            <Brain size={24} color="#8b5cf6" />
            Agentic AI Analyzer
          </Link>

          <Link to="/events" className="sidebar-link" onClick={onClose}>
            <Radio size={24} color="#06b6d4" />
            Event Monitor
          </Link>

          {/* GitHub Author Profile */}
          <a 
            href="https://github.com/pavansaiambala7" 
            target="_blank" 
            rel="noopener noreferrer" 
            className="sidebar-link"
            style={{ marginTop: 'auto', borderTop: '1px solid var(--color-border)', paddingTop: '16px' }}
          >
            <Github size={24} color="#1e293b" />
            GitHub Profile
          </a>
        </nav>
      </div>
    </>
  );
}
