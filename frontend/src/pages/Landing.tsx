import { 
  ShieldCheck, Lock, Zap, GitFork, Server, Cpu, Radio, 
  Database, Bell, CheckCircle2, ArrowRight, Activity, Volume2 
} from 'lucide-react';
import { SignInButton } from '@clerk/react';
import { Link } from 'react-router-dom';

export default function Landing() {
  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', paddingTop: '40px', paddingBottom: '80px' }}>
      
      {/* Hero Section */}
      <div className="animate-fade-up" style={{ textAlign: 'center' }}>
        
        {/* Architecture Pill */}
        <div style={{ 
          display: 'inline-flex', 
          alignItems: 'center', 
          gap: '8px', 
          padding: '6px 18px', 
          borderRadius: '30px', 
          background: 'rgba(16, 185, 129, 0.1)', 
          border: '1px solid rgba(16, 185, 129, 0.3)',
          marginBottom: '24px'
        }}>
          <ShieldCheck size={18} color="var(--color-primary)" />
          <span style={{ fontSize: '13px', fontWeight: '700', color: 'var(--color-accent)' }}>
            5-Service Distributed Microservices Platform • 99.5% Uptime
          </span>
        </div>
        
        <h1 style={{ 
          fontSize: '52px', 
          lineHeight: '1.15', 
          marginBottom: '20px',
          color: 'var(--color-text-primary)',
          fontWeight: '800',
          letterSpacing: '-0.03em'
        }}>
          SafePe — Next-Gen Financial <br/>
          <span style={{ 
            background: 'linear-gradient(135deg, #10b981, #059669)', 
            WebkitBackgroundClip: 'text', 
            WebkitTextFillColor: 'transparent' 
          }}>
            Safety & Payments Engine
          </span>
        </h1>
        
        <p style={{ 
          color: 'var(--color-text-secondary)', 
          fontSize: '18px', 
          maxWidth: '750px', 
          margin: '0 auto 36px auto',
          lineHeight: '1.6'
        }}>
          Engineered as a <strong>5-service distributed platform</strong> with database-per-service architecture supporting <strong>550+ merchants</strong>. Powered by <strong>LangGraph Agentic AI</strong>, pgvector RAG semantic search, and real-time Kafka event streaming.
        </p>
        
        {/* CTA Buttons */}
        <div style={{ display: 'flex', gap: '16px', justifyContent: 'center', alignItems: 'center', flexWrap: 'wrap', marginBottom: '48px' }}>
          <SignInButton mode="modal">
            <button className="btn-primary" style={{ fontSize: '16px', padding: '14px 32px', borderRadius: '30px' }}>
              Launch SafePe Dashboard <ArrowRight size={18} />
            </button>
          </SignInButton>

          <a 
            href="https://github.com/pavansaiambala7/safepe" 
            target="_blank" 
            rel="noopener noreferrer"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '10px',
              padding: '14px 26px',
              borderRadius: '30px',
              border: '1px solid var(--color-border)',
              background: 'white',
              color: 'var(--color-text-primary)',
              textDecoration: 'none',
              fontSize: '15px',
              fontWeight: '600',
              boxShadow: '0 4px 12px rgba(0,0,0,0.04)',
              transition: 'all 0.3s ease'
            }}
          >
            <GitFork size={18} color="#1e293b" />
            GitHub Repository
          </a>
        </div>

        {/* Live Architecture Benchmark Cards */}
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
          gap: '16px', 
          textAlign: 'left',
          marginBottom: '64px'
        }}>
          <div className="surface-panel" style={{ padding: '20px', borderTop: '4px solid #10b981' }}>
            <div style={{ fontSize: '28px', fontWeight: '800', color: '#10b981', marginBottom: '4px' }}>92%</div>
            <div style={{ fontSize: '13px', fontWeight: '700', color: 'var(--color-text-primary)' }}>AI Fraud Accuracy</div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '2px' }}>on 3,000+ Transactions (LangGraph)</div>
          </div>

          <div className="surface-panel" style={{ padding: '20px', borderTop: '4px solid #8b5cf6' }}>
            <div style={{ fontSize: '28px', fontWeight: '800', color: '#8b5cf6', marginBottom: '4px' }}>480ms</div>
            <div style={{ fontSize: '13px', fontWeight: '700', color: 'var(--color-text-primary)' }}>Redis Vector Latency</div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '2px' }}>Cut from 800ms via Redis Caching</div>
          </div>

          <div className="surface-panel" style={{ padding: '20px', borderTop: '4px solid #3b82f6' }}>
            <div style={{ fontSize: '28px', fontWeight: '800', color: '#3b82f6', marginBottom: '4px' }}>550+</div>
            <div style={{ fontSize: '13px', fontWeight: '700', color: 'var(--color-text-primary)' }}>Supported Merchants</div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '2px' }}>99.5% Platform SLA Uptime</div>
          </div>

          <div className="surface-panel" style={{ padding: '20px', borderTop: '4px solid #f59e0b' }}>
            <div style={{ fontSize: '28px', fontWeight: '800', color: '#f59e0b', marginBottom: '4px' }}>1,000+</div>
            <div style={{ fontSize: '13px', fontWeight: '700', color: 'var(--color-text-primary)' }}>Secured Accounts</div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginTop: '2px' }}>JWT + AES-256 Cryptographic Vault</div>
          </div>
        </div>
      </div>

      {/* 5-Service Distributed Microservices Platform Section */}
      <div className="animate-fade-up" style={{ marginBottom: '64px' }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h2 style={{ fontSize: '32px', fontWeight: '800', color: 'var(--color-text-primary)' }}>
            🏗️ 5-Service Distributed Architecture
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '15px', marginTop: '6px' }}>
            Strict database-per-service isolation for high reliability and zero single point of failure
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
          {/* Service 1 */}
          <div className="surface-panel" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
              <div style={{ background: '#10b98120', padding: '10px', borderRadius: '12px' }}>
                <ShieldCheck size={24} color="#10b981" />
              </div>
              <div>
                <span style={{ fontSize: '11px', fontWeight: '800', color: '#10b981', textTransform: 'uppercase' }}>Service 1: Client</span>
                <h3 style={{ fontSize: '16px', margin: 0 }}>React 18 + Vite Frontend</h3>
              </div>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
              Zero-trust client interface with PhonePe-style emerald green sound notification bell, QR camera scanning, and NPCI UPI PIN workflows.
            </p>
          </div>

          {/* Service 2 */}
          <div className="surface-panel" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
              <div style={{ background: '#3b82f620', padding: '10px', borderRadius: '12px' }}>
                <Server size={24} color="#3b82f6" />
              </div>
              <div>
                <span style={{ fontSize: '11px', fontWeight: '800', color: '#3b82f6', textTransform: 'uppercase' }}>Service 2: Gateway</span>
                <h3 style={{ fontSize: '16px', margin: 0 }}>Spring Boot 3.2 Core API</h3>
              </div>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
              Handles 200+ Razorpay webhook events, IP token-bucket rate limiting, 550+ merchant trust directories, and Kafka publishing.
            </p>
          </div>

          {/* Service 3 */}
          <div className="surface-panel" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
              <div style={{ background: '#8b5cf620', padding: '10px', borderRadius: '12px' }}>
                <Cpu size={24} color="#8b5cf6" />
              </div>
              <div>
                <span style={{ fontSize: '11px', fontWeight: '800', color: '#8b5cf6', textTransform: 'uppercase' }}>Service 3: AI Engine</span>
                <h3 style={{ fontSize: '16px', margin: 0 }}>LangGraph Agentic Fraud Engine</h3>
              </div>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
              Multi-step reasoning pipeline with 3 specialized agents executing pattern classification, RAG vector search, and automated escrow freeze.
            </p>
          </div>

          {/* Service 4 */}
          <div className="surface-panel" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
              <div style={{ background: '#06b6d420', padding: '10px', borderRadius: '12px' }}>
                <Radio size={24} color="#06b6d4" />
              </div>
              <div>
                <span style={{ fontSize: '11px', fontWeight: '800', color: '#06b6d4', textTransform: 'uppercase' }}>Service 4: Event Bus</span>
                <h3 style={{ fontSize: '16px', margin: 0 }}>Apache Kafka (KRaft Mode)</h3>
              </div>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
              High-throughput KRaft streaming bus with <code>transaction-events</code> and <code>fraud-alerts</code> topics for non-blocking asynchronous processing.
            </p>
          </div>

          {/* Service 5 */}
          <div className="surface-panel" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
              <div style={{ background: '#f59e0b20', padding: '10px', borderRadius: '12px' }}>
                <Database size={24} color="#f59e0b" />
              </div>
              <div>
                <span style={{ fontSize: '11px', fontWeight: '800', color: '#f59e0b', textTransform: 'uppercase' }}>Service 5: Storage</span>
                <h3 style={{ fontSize: '16px', margin: 0 }}>pgvector + Redis + AES Vault</h3>
              </div>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
              HNSW vector index for 1,000+ fraud patterns, Redis cache cutting latency from 800ms → 480ms, and AES-256 GCM encrypted field vault.
            </p>
          </div>

          {/* Bell Notifications */}
          <div className="surface-panel" style={{ padding: '24px', border: '1px solid #10b98140', background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.05), rgba(255, 255, 255, 0.95))' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '14px' }}>
              <div style={{ background: '#10b981', padding: '10px', borderRadius: '12px' }}>
                <Bell size={24} color="white" />
              </div>
              <div>
                <span style={{ fontSize: '11px', fontWeight: '800', color: '#059669', textTransform: 'uppercase' }}>Audio & Visual Alerting</span>
                <h3 style={{ fontSize: '16px', margin: 0 }}>Real-Time Sound Bell Center</h3>
              </div>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
              Web Audio API synthesized chimes for instant transaction feedback, high-risk fraud alerts, and automated escrow refund confirmations.
            </p>
          </div>
        </div>
      </div>

      {/* RAG & Redis Latency Optimization Highlight */}
      <div className="surface-panel animate-fade-up" style={{ padding: '36px', marginBottom: '64px', background: 'linear-gradient(135deg, #ffffff, #f0fdf4)' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '32px', alignItems: 'center' }}>
          <div>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: '800', color: '#8b5cf6', textTransform: 'uppercase', marginBottom: '8px' }}>
              <Zap size={15} color="#8b5cf6" /> Semantic AI Performance
            </div>
            <h3 style={{ fontSize: '26px', fontWeight: '800', color: 'var(--color-text-primary)', margin: '0 0 12px 0' }}>
              RAG Pipeline with Gemini Embeddings + pgvector
            </h3>
            <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', margin: '0 0 20px 0' }}>
              Semantic similarity search over 1,000+ fraud patterns using 768-dimensional Gemini embeddings. Redis vector caching slashes decision latency from <strong>800ms down to 480ms</strong>.
            </p>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              <div style={{ padding: '8px 14px', borderRadius: '10px', background: 'white', border: '1px solid var(--color-border)', fontSize: '12px', fontWeight: '600' }}>
                ⚡ 40% Latency Reduction
              </div>
              <div style={{ padding: '8px 14px', borderRadius: '10px', background: 'white', border: '1px solid var(--color-border)', fontSize: '12px', fontWeight: '600' }}>
                🎯 92% Detection Accuracy
              </div>
            </div>
          </div>

          {/* Latency Comparison Card */}
          <div style={{ background: 'white', padding: '24px', borderRadius: '16px', border: '1px solid var(--color-border)', boxShadow: '0 8px 30px rgba(0,0,0,0.04)' }}>
            <h4 style={{ fontSize: '14px', fontWeight: '700', marginBottom: '16px', color: 'var(--color-text-primary)' }}>
              Live Vector Query Latency Comparison
            </h4>
            
            <div style={{ marginBottom: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', fontWeight: '600', marginBottom: '6px' }}>
                <span style={{ color: '#64748b' }}>Standard pgvector Query</span>
                <span style={{ color: '#ef4444', fontWeight: '700' }}>800 ms</span>
              </div>
              <div style={{ height: '10px', background: '#f1f5f9', borderRadius: '5px', overflow: 'hidden' }}>
                <div style={{ width: '100%', height: '100%', background: '#ef4444', borderRadius: '5px' }} />
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', fontWeight: '700', marginBottom: '6px' }}>
                <span style={{ color: '#059669' }}>Redis 7 In-Memory Vector Cache</span>
                <span style={{ color: '#10b981', fontWeight: '800' }}>480 ms</span>
              </div>
              <div style={{ height: '10px', background: '#f1f5f9', borderRadius: '5px', overflow: 'hidden' }}>
                <div style={{ width: '60%', height: '100%', background: 'linear-gradient(90deg, #10b981, #059669)', borderRadius: '5px' }} />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Footer Attribution */}
      <div style={{ textAlign: 'center', borderTop: '1px solid var(--color-border)', paddingTop: '32px' }}>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px', margin: 0 }}>
          Designed and Architected by <a href="https://github.com/pavansaiambala7" target="_blank" rel="noopener noreferrer" style={{ color: 'var(--color-primary)', fontWeight: '700', textDecoration: 'none' }}>Pavan Sai (@pavansaiambala7)</a>
        </p>
        <p style={{ color: '#94a3b8', fontSize: '12px', marginTop: '6px' }}>
          SafePe Financial Safety Platform • Spring Boot 3.2 • React 18 • Kafka KRaft • pgvector • Redis 7 • AES-256
        </p>
      </div>
      
    </div>
  );
}
