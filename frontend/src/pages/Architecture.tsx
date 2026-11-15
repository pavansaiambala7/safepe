import { useState } from 'react';
import { Server, Database, Cpu, Radio, ShieldCheck, Zap, CheckCircle2, Layers, Bell, Bot } from 'lucide-react';
import { Link } from 'react-router-dom';
import { audioAlerts } from '../utils/audioAlert';

// Every number on this page is either read from the code/config or measured by
// the RAGAS evaluation in rag-eval/ (results/full_report.md). Keep it that way:
// update the numbers when the code or a new evaluation run changes them.

export default function Architecture() {
  const [selectedService, setSelectedService] = useState<number>(0);

  const services = [
    {
      id: 1,
      name: 'Layer 1: Presentation Layer',
      tech: 'React 18 + Vite + TypeScript',
      icon: <Layers size={28} color="#10b981" />,
      tag: 'CLIENT TIER',
      color: '#10b981',
      description: 'Single-page React app served by nginx. Users sign in with Clerk; every API call carries the Clerk session JWT to the API gateway.',
      responsibilities: [
        'Responsive mobile & web UI for payments, bills and insights',
        'Notification bell with Web Audio API chime',
        'Razorpay Checkout for card / UPI payments',
        'Kafka event monitor and AI reasoning-step visualisation'
      ],
      metrics: {
        'Framework': 'React 18 + Vite',
        'Auth': 'Clerk (JWT, RS256)',
        'Served By': 'nginx (Docker)',
        'Routing': 'Client-side SPA'
      }
    },
    {
      id: 2,
      name: 'Layer 2: API Gateway & Payments',
      tech: 'Spring Boot 3.2 + Spring Cloud Gateway + Java 17',
      icon: <Server size={28} color="#3b82f6" />,
      tag: 'BACKEND CORE',
      color: '#3b82f6',
      description: 'Spring Cloud Gateway verifies the Clerk JWT and forwards the verified user ID; services find each other through Eureka. The payment service creates Razorpay orders and publishes transaction events to Kafka.',
      responsibilities: [
        'JWT verification at the gateway - user identity never taken from the request body',
        'Bucket4j token-bucket rate limiting, with stricter limits on sensitive routes',
        'Razorpay order creation and payment webhooks',
        'Global exception handler returning consistent JSON errors'
      ],
      metrics: {
        'Gateway': 'Spring Cloud Gateway',
        'Discovery': 'Eureka',
        'Rate Limit': '40 burst · 20 req/s',
        'Backend Services': '5 Spring Boot'
      }
    },
    {
      id: 3,
      name: 'Layer 3: Agentic AI Fraud Engine',
      tech: 'LangChain4j + Gemini 2.5 Flash + gemini-embedding-001',
      icon: <Cpu size={28} color="#8b5cf6" />,
      tag: 'AI REASONING',
      color: '#8b5cf6',
      description: 'Three-step RAG pipeline for SMS scam detection, plus a Money Assistant that answers questions from the user\'s own transaction history.',
      responsibilities: [
        'Step 1: Gemini classifies the message into a fraud category',
        'Step 2: Gemini embeddings + cosine similarity against 21 known fraud patterns',
        'Step 3: risk score, ALLOW / FLAG / BLOCK action and a Gemini-written explanation',
        'Money Assistant: answers grounded in the user\'s transactions (SQL retrieval)'
      ],
      metrics: {
        'Scams Caught (RAGAS eval)': '12 / 12',
        'False Alarms (RAGAS eval)': '0 / 6',
        'Assistant Answers Correct': '10 / 10',
        'Match Threshold': '0.75 cosine'
      }
    },
    {
      id: 4,
      name: 'Layer 4: Event-Driven Kafka Bus',
      tech: 'Apache Kafka 3.7 (KRaft Mode)',
      icon: <Radio size={28} color="#06b6d4" />,
      tag: 'MESSAGING',
      color: '#06b6d4',
      description: 'Kafka decouples payments from fraud analysis and notifications: each consumer group receives its own copy of every event.',
      responsibilities: [
        'transaction-events: payment events consumed by fraud and notification services',
        'bill-reminders: scheduled bill reminder events',
        'KRaft mode - no ZooKeeper',
        'Server-Sent Events push notifications to the browser'
      ],
      metrics: {
        'Kafka Mode': 'KRaft (ZooKeeper-free)',
        'Topics': '2',
        'Partitions': '3 per topic',
        'Brokers': '1'
      }
    },
    {
      id: 5,
      name: 'Layer 5: Data & Cache Layer',
      tech: 'PostgreSQL 15 + Redis 7 + Flyway',
      icon: <Database size={28} color="#f59e0b" />,
      tag: 'STORAGE & CACHE',
      color: '#f59e0b',
      description: 'PostgreSQL holds payments, fraud patterns and a fraud-service read model of transactions; Redis caches vector-search results.',
      responsibilities: [
        'Flyway-versioned database schema',
        'fraud_transaction_view: read model built from Kafka events',
        'Redis cache for vector-search results (5-minute TTL)',
        'Database credentials supplied through environment variables'
      ],
      metrics: {
        'Database': 'PostgreSQL 15',
        'Migrations': 'Flyway',
        'Cache': 'Redis 7 · 5-min TTL',
        'Cache Hit (measured)': '8 ms'
      }
    }
  ];

  const statCard = (value: string, title: string, sub: string, color: string) => (
    <div className="surface-panel" style={{ padding: '20px', textAlign: 'center', borderTop: `4px solid ${color}` }}>
      <div style={{ fontSize: 28, fontWeight: 800, color, marginBottom: 4 }}>{value}</div>
      <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-text-primary)' }}>{title}</div>
      <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>{sub}</div>
    </div>
  );

  const evalRow = (label: string, value: string) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px dashed var(--color-border)', padding: '6px 0', fontSize: 13 }}>
      <span style={{ color: 'var(--color-text-secondary)' }}>{label}</span>
      <strong style={{ color: 'var(--color-text-primary)' }}>{value}</strong>
    </div>
  );

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '32px 16px' }} className="animate-fade-up">

      {/* Top Banner */}
      <div style={{ textAlign: 'center', marginBottom: 40 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 10, padding: '6px 16px', borderRadius: 20, background: '#10b98115', border: '1px solid #10b98140', marginBottom: 16 }}>
          <ShieldCheck size={18} color="#10b981" />
          <span style={{ fontSize: 13, fontWeight: 700, color: '#059669', letterSpacing: '0.02em' }}>
            5-SERVICE DISTRIBUTED MICROSERVICES PLATFORM
          </span>
        </div>
        <h1 style={{ fontSize: 36, fontWeight: 800, margin: '0 0 12px 0', color: 'var(--color-text-primary)' }}>
          SafePe Architecture & Evaluation Results
        </h1>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 16, maxWidth: 700, margin: '0 auto', lineHeight: 1.6 }}>
          Event-driven distributed architecture with <strong>3 AI features</strong> — Money Assistant RAG, Spending Insights, and SMS Scam Scanner — powered by LangChain4j and Gemini.
        </p>
      </div>

      {/* Key Metrics Bar */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16, marginBottom: 36 }}>
        {statCard('12 / 12', 'Scams Caught', 'RAGAS eval · 0 of 6 false alarms', '#10b981')}
        {statCard('10 / 10', 'Assistant Answers Correct', 'RAGAS eval · incl. hallucination check', '#8b5cf6')}
        {statCard('3', 'AI Features', 'Money Assistant • Insights • Scam Scanner', '#3b82f6')}
        {statCard('21', 'Fraud Patterns', '8 scam categories · Gemini embeddings', '#f59e0b')}
        {statCard('8 ms', 'Redis Cache Hit', 'Repeat vector searches · 5-min TTL', '#06b6d4')}
      </div>

      {/* Interactive Architecture Navigator */}
      <div className="surface-panel" style={{ padding: 28, marginBottom: 36 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
          <div>
            <h3 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>
              🏗️ 5-Layer Architecture
            </h3>
            <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', margin: '4px 0 0 0' }}>
              Select a layer to see what it does and how it is built
            </p>
          </div>
          <span style={{ fontSize: 12, fontWeight: 700, padding: '4px 12px', borderRadius: 12, background: '#10b98120', color: '#059669' }}>
            Gateway · Eureka · Payment · Fraud · Notification
          </span>
        </div>

        {/* Layer Tab Buttons */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 10, marginBottom: 24 }}>
          {services.map((srv, idx) => (
            <button
              key={srv.id}
              onClick={() => setSelectedService(idx)}
              style={{
                padding: '12px 14px',
                borderRadius: 14,
                border: selectedService === idx ? `2px solid ${srv.color}` : '1px solid var(--color-border)',
                background: selectedService === idx ? `${srv.color}12` : 'white',
                cursor: 'pointer',
                textAlign: 'left',
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                transition: 'all 0.2s ease',
                boxShadow: selectedService === idx ? `0 4px 14px ${srv.color}25` : 'none'
              }}
            >
              <div style={{ background: `${srv.color}20`, padding: 6, borderRadius: 10 }}>
                {srv.icon}
              </div>
              <div style={{ overflow: 'hidden' }}>
                <div style={{ fontSize: 10, fontWeight: 800, color: srv.color, textTransform: 'uppercase' }}>
                  {srv.tag}
                </div>
                <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--color-text-primary)', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
                  {srv.name.split(':')[1]?.trim() || srv.name}
                </div>
              </div>
            </button>
          ))}
        </div>

        {/* Selected Layer Detail Panel */}
        <div style={{ background: 'var(--color-bg-base)', borderRadius: 16, padding: 24, border: `1px solid var(--color-border)` }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16, marginBottom: 16 }}>
            <div>
              <div style={{ display: 'inline-block', fontSize: 11, fontWeight: 800, padding: '3px 8px', borderRadius: 6, background: services[selectedService].color, color: 'white', marginBottom: 6 }}>
                {services[selectedService].tag}
              </div>
              <h3 style={{ fontSize: 22, fontWeight: 800, margin: '0 0 6px 0', color: 'var(--color-text-primary)' }}>
                {services[selectedService].name}
              </h3>
              <div style={{ fontSize: 14, fontWeight: 600, color: services[selectedService].color }}>
                Tech Stack: {services[selectedService].tech}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              {selectedService === 0 && (
                <button onClick={() => audioAlerts.playSoundForType('SUCCESS')} style={{ padding: '6px 12px', borderRadius: 8, background: '#10b981', color: 'white', border: 'none', fontSize: 12, fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Bell size={14} /> Test Bell Sound
                </button>
              )}
              {selectedService === 2 && (
                <Link to="/chatbot" style={{ padding: '6px 14px', borderRadius: 8, background: '#10b981', color: 'white', textDecoration: 'none', fontSize: 12, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Bot size={14} /> Open AI Assistant & Shield
                </Link>
              )}
              {selectedService === 3 && (
                <Link to="/events" style={{ padding: '6px 14px', borderRadius: 8, background: '#06b6d4', color: 'white', textDecoration: 'none', fontSize: 12, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Radio size={14} /> Open Kafka Bus
                </Link>
              )}
            </div>
          </div>

          <p style={{ fontSize: 14, color: 'var(--color-text-secondary)', lineHeight: 1.6, marginBottom: 20 }}>
            {services[selectedService].description}
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
            <div style={{ background: 'white', padding: 16, borderRadius: 12, border: '1px solid var(--color-border)' }}>
              <h4 style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: 'var(--color-text-primary)' }}>
                ⚙️ Core Responsibilities
              </h4>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, color: 'var(--color-text-secondary)', display: 'flex', flexDirection: 'column', gap: 8 }}>
                {services[selectedService].responsibilities.map((resp, i) => (
                  <li key={i}>{resp}</li>
                ))}
              </ul>
            </div>

            <div style={{ background: 'white', padding: 16, borderRadius: 12, border: '1px solid var(--color-border)' }}>
              <h4 style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: 'var(--color-text-primary)' }}>
                📊 Key Facts
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {Object.entries(services[selectedService].metrics).map(([key, val], i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px dashed var(--color-border)', paddingBottom: 6 }}>
                    <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{key}:</span>
                    <strong style={{ fontSize: 13, color: services[selectedService].color }}>{val}</strong>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* RAG Evaluation Results (RAGAS) */}
      <div className="surface-panel" style={{ padding: 28, marginBottom: 36 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 11, fontWeight: 800, color: '#8b5cf6', textTransform: 'uppercase', marginBottom: 4 }}>
          <Zap size={14} color="#8b5cf6" /> RAG Evaluation · RAGAS
        </div>
        <h3 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>
          How Well the AI Performs
        </h3>
        <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', margin: '4px 0 0 0' }}>
          Both RAG pipelines evaluated with RAGAS, using Gemini 3.5 Flash as the judge, plus a manual check of every answer.
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 20, marginTop: 20 }}>
          <div style={{ background: '#10b9810d', padding: 20, borderRadius: 14, border: '1px solid #10b98140' }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: '#059669', marginBottom: 10 }}>
              🛡️ SMS Scam Detection — 18 test messages
            </div>
            {evalRow('Correct verdicts', '18 / 18')}
            {evalRow('Scams caught', '12 / 12')}
            {evalRow('Legitimate messages wrongly flagged', '0 / 6')}
            {evalRow('Best match in the right scam category', '12 / 12')}
          </div>

          <div style={{ background: '#8b5cf60d', padding: 20, borderRadius: 14, border: '1px solid #8b5cf640' }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: '#7c3aed', marginBottom: 10 }}>
              💬 Money Assistant — 10 test questions
            </div>
            {evalRow('Factually correct answers (manual check)', '10 / 10')}
            {evalRow('Invented payments (hallucination check)', 'None')}
            {evalRow('Answer relevancy (RAGAS)', '0.94')}
            {evalRow('Context recall (RAGAS)', '1.00')}
          </div>
        </div>

        <div style={{ marginTop: 16, padding: 12, borderRadius: 10, background: '#f8fafc', border: '1px solid var(--color-border)', fontSize: 12, color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'flex-start', gap: 8 }}>
          <CheckCircle2 size={16} color="#10b981" style={{ flexShrink: 0, marginTop: 1 }} />
          <span>
            Small test set: the 0.75 similarity threshold was tuned on these same messages, so results on new messages may be lower.
            Full report: <code>rag-eval/results/full_report.md</code>.
          </span>
        </div>
      </div>

      {/* Multi-Step Reasoning Pipeline */}
      <div className="surface-panel" style={{ padding: 28, marginBottom: 36 }}>
        <h3 style={{ fontSize: 20, fontWeight: 700, marginBottom: 8 }}>
          🤖 LangChain4j Multi-Step Reasoning Pipeline
        </h3>
        <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 24 }}>
          How every suspicious SMS is analysed in three steps
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 16 }}>
          {[
            { n: 1, color: '#8b5cf6', title: 'Step 1: Pattern Classifier',
              text: 'Gemini 2.5 Flash classifies the message into a fraud category such as Phishing, KYC, UPI, Lottery, Vishing or Impersonation.' },
            { n: 2, color: '#06b6d4', title: 'Step 2: RAG Vector Search',
              text: 'gemini-embedding-001 vectors compared by cosine similarity with 21 known fraud patterns; matches of 0.75 or more count. Results cached in Redis for 5 minutes.' },
            { n: 3, color: '#10b981', title: 'Step 3: Risk Evaluator',
              text: 'Turns the best match into a 0-100 risk score and an ALLOW, FLAG or BLOCK action, and Gemini writes a short explanation for the user.' }
          ].map((s) => (
            <div key={s.n} style={{ background: 'white', padding: 20, borderRadius: 14, border: '1px solid var(--color-border)', borderTop: `4px solid ${s.color}` }}>
              <div style={{ width: 32, height: 32, borderRadius: '50%', background: `${s.color}20`, color: s.color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, marginBottom: 12 }}>
                {s.n}
              </div>
              <h4 style={{ fontSize: 15, fontWeight: 700, margin: '0 0 6px 0' }}>{s.title}</h4>
              <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', margin: 0, lineHeight: 1.5 }}>{s.text}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Footer */}
      <div style={{ textAlign: 'center', paddingTop: 12 }}>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/events" className="btn-primary" style={{ padding: '12px 24px', borderRadius: 24, fontSize: 14, textDecoration: 'none' }}>
            <Radio size={16} /> Open Kafka Monitor
          </Link>
          <Link to="/chatbot" style={{ padding: '12px 24px', borderRadius: 24, fontSize: 14, textDecoration: 'none', background: 'white', border: '1px solid var(--color-border)', color: 'var(--color-text-primary)', fontWeight: 600 }}>
            <Bot size={16} color="#10b981" style={{ verticalAlign: 'middle', marginRight: 6 }} /> Open AI Assistant & Scam Shield
          </Link>
        </div>
      </div>

    </div>
  );
}
