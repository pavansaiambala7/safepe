import { ShieldCheck, Lock, Zap } from 'lucide-react';
import { SignInButton } from '@clerk/react';

export default function Landing() {
  return (
    <div style={{ textAlign: 'center', paddingTop: '60px' }}>
      
      {/* ReactBits-style animated hero section */}
      <div className="animate-fade-up" style={{ animationDelay: '0.1s' }}>
        <ShieldCheck size={80} color="var(--color-primary)" className="floating" style={{ margin: '0 auto 32px auto', display: 'block' }} />
        
        <h1 style={{ 
          fontSize: '56px', 
          lineHeight: '1.2', 
          marginBottom: '24px',
          color: 'var(--color-text-primary)',
          fontWeight: '800'
        }}>
          The World's Most <br/> Secure Payments App
        </h1>
        
        <p style={{ 
          color: 'var(--color-text-secondary)', 
          fontSize: '20px', 
          maxWidth: '600px', 
          margin: '0 auto 48px auto',
          lineHeight: '1.6'
        }}>
          Bank-grade AES-256 encryption, AI-powered fraud detection, and zero-trust architecture tailored exclusively for SafePe.
        </p>
        
        <SignInButton mode="modal">
          <button className="btn-primary" style={{ fontSize: '20px', padding: '18px 40px', borderRadius: '30px' }}>
            Get Started Securely
          </button>
        </SignInButton>
      </div>

      {/* Feature Grid */}
      <div className="animate-fade-up" style={{ animationDelay: '0.4s', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '24px', marginTop: '80px' }}>
        <div className="surface-panel">
          <Lock size={32} color="var(--color-highlight)" style={{ marginBottom: '16px' }} />
          <h3 style={{ marginBottom: '8px' }}>Pay Secure</h3>
          <p style={{ color: 'var(--color-text-secondary)' }}>Every transaction is verified through multi-layered cryptographic checks.</p>
        </div>
        <div className="surface-panel">
          <Zap size={32} color="var(--color-highlight)" style={{ marginBottom: '16px' }} />
          <h3 style={{ marginBottom: '8px' }}>Lightning Fast</h3>
          <p style={{ color: 'var(--color-text-secondary)' }}>Instant settlements directly to your bank account with zero delay.</p>
        </div>
      </div>
      
    </div>
  );
}
