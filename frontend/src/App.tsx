import { useState } from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { ClerkProvider, SignInButton, UserButton, useAuth } from '@clerk/react';
import { ShieldCheck, UserCircle, Menu } from 'lucide-react';
import './index.css';

import Dashboard from './pages/Dashboard';
import Checkout from './pages/Checkout';
import ScamScanner from './pages/Scanner';
import Landing from './pages/Landing';
import Sidebar from './components/Sidebar';
import NotificationBell from './components/NotificationBell';
import { NotificationProvider } from './context/NotificationContext';
import SplitBills from './pages/SplitBills';
import SpendAnalysis from './pages/SpendAnalysis';
import FixedDeposits from './pages/FixedDeposits';
import BankTransfer from './pages/BankTransfer';
import QrGenerator from './pages/QrGenerator';
import ScanQrCamera from './pages/ScanQrCamera';
import UtilityPayments from './pages/UtilityPayments';
import SelfTransfer from './pages/SelfTransfer';
import CheckBalance from './pages/CheckBalance';
import Cards from './pages/Cards';
import AgenticFraudAnalyzer from './pages/AgenticFraudAnalyzer';
import KafkaEventMonitor from './pages/KafkaEventMonitor';
import Architecture from './pages/Architecture';

const clerkPubKey = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;

// Custom Wrappers for Clerk v6 compatibility
const SignedIn = ({ children }: { children: React.ReactNode }) => {
  const { isSignedIn } = useAuth();
  return isSignedIn ? <>{children}</> : null;
};

const SignedOut = ({ children }: { children: React.ReactNode }) => {
  const { isSignedIn } = useAuth();
  return !isSignedIn ? <>{children}</> : null;
};

function InnerApp() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <NotificationProvider>
      <BrowserRouter>
        <div className="app-layout">
          
          {/* Top Navigation Bar */}
          <header className="header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <SignedIn>
                {/* Hamburger Menu Icon */}
                <button 
                  className={`hamburger-btn ${sidebarOpen ? 'open' : ''}`} 
                  onClick={() => setSidebarOpen(!sidebarOpen)}
                >
                  <div className="hamburger-line line-1" />
                  <div className="hamburger-line line-2" />
                  <div className="hamburger-line line-3" />
                </button>
              </SignedIn>
              
              <Link to="/" style={{ textDecoration: 'none' }}>
                <div className="logo">
                  <ShieldCheck size={32} color="var(--color-primary)" />
                  SafePe
                </div>
              </Link>
            </div>
            
            <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
              {/* PhonePe Style Emerald Green Notification Bell with Sound Chimes (Always Visible) */}
              <NotificationBell />
              
              <SignedIn>
                <UserButton />
              </SignedIn>

              <SignedOut>
                <SignInButton mode="modal">
                  <button className="btn-primary" style={{ padding: '8px 18px', borderRadius: '24px', fontSize: '14px' }}>
                    <UserCircle size={18} /> Login
                  </button>
                </SignInButton>
              </SignedOut>
            </div>
          </header>

        {/* Hamburger Sidebar Component */}
        <SignedIn>
          <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        </SignedIn>

        {/* Main Content Area */}
        <main className="app-main">
          <SignedIn>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/architecture" element={<Architecture />} />
              <Route path="/pay" element={<Checkout />} />
              <Route path="/sms-scanner" element={<ScamScanner />} />
              <Route path="/chatbot" element={<ScamScanner />} />
              <Route path="/qr" element={<QrGenerator />} />
              <Route path="/bank-transfer" element={<BankTransfer />} />
              <Route path="/split" element={<SplitBills />} />
              <Route path="/analysis" element={<SpendAnalysis />} />
              <Route path="/fd-rates" element={<FixedDeposits />} />
              <Route path="/scan-qr" element={<ScanQrCamera />} />
              <Route path="/utilities/:type" element={<UtilityPayments />} />
              <Route path="/self-transfer" element={<SelfTransfer />} />
              <Route path="/check-balance" element={<CheckBalance />} />
              <Route path="/balance" element={<CheckBalance />} />
              <Route path="/cards" element={<Cards />} />
              <Route path="/agentic-fraud" element={<AgenticFraudAnalyzer />} />
              <Route path="/events" element={<KafkaEventMonitor />} />
            </Routes>
          </SignedIn>

          <SignedOut>
            <Landing />
          </SignedOut>
        </main>

      </div>
    </BrowserRouter>
  </NotificationProvider>
);
}

function App() {
  if (!clerkPubKey) {
    return (
      <div className="app-layout">
        <main className="app-main">
          <div className="surface-panel status-danger">
            <h2>🚨 Missing Clerk Publishable Key</h2>
            <p>Please add VITE_CLERK_PUBLISHABLE_KEY to your frontend/.env file!</p>
          </div>
        </main>
      </div>
    );
  }

  return (
    <ClerkProvider publishableKey={clerkPubKey}>
      <InnerApp />
    </ClerkProvider>
  );
}

export default App;
