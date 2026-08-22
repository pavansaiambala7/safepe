import { useState } from 'react';
import { Brain, Search, Shield, AlertTriangle, CheckCircle, XCircle, Loader2, Zap, Database, Bot } from 'lucide-react';
import { useAuth } from '@clerk/react';
import api from '../api';

interface ReasoningStep {
  stepNumber: number;
  stepName: string;
  description: string;
  result: string;
}

interface MatchedPattern {
  patternId: string;
  description: string;
  category: string;
  severity: string;
  similarityPercent: number;
}

interface AgenticResult {
  riskScore: number;
  action: string;
  summary: string;
  reasoningSteps: ReasoningStep[];
  matchedPatterns: MatchedPattern[];
  processingTimeMs: number;
}

export default function AgenticFraudAnalyzer() {
  const [message, setMessage] = useState('');
  const [upiId, setUpiId] = useState('');
  const [result, setResult] = useState<AgenticResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const [error, setError] = useState('');
  const { getToken } = useAuth();

  const analyzeMessage = async () => {
    if (!message.trim()) return;
    setLoading(true);
    setError('');
    setResult(null);
    setActiveStep(0);

    // Simulate step-by-step animation
    const stepTimer = setInterval(() => {
      setActiveStep(prev => (prev < 3 ? prev + 1 : prev));
    }, 800);

    try {
      const token = await getToken();
      const res = await api.post('/fraud/agentic-analyze', {
        message: message.trim(),
        upiId: upiId.trim() || null,
        userId: null
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      clearInterval(stepTimer);
      setActiveStep(3);
      setResult(res.data);
    } catch (err: any) {
      clearInterval(stepTimer);
      setError(err.response?.data?.message || 'Failed to analyze. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const getActionColor = (action: string) => {
    switch (action) {
      case 'BLOCK': return '#ef4444';
      case 'FLAG_VERIFICATION': return '#f59e0b';
      case 'ALLOW': return '#10b981';
      default: return '#6b7280';
    }
  };

  const getActionIcon = (action: string) => {
    switch (action) {
      case 'BLOCK': return <XCircle size={24} />;
      case 'FLAG_VERIFICATION': return <AlertTriangle size={24} />;
      case 'ALLOW': return <CheckCircle size={24} />;
      default: return <Shield size={24} />;
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f59e0b';
      case 'MEDIUM': return '#3b82f6';
      case 'LOW': return '#10b981';
      default: return '#6b7280';
    }
  };

  const stepIcons = [
    <Bot size={20} />,
    <Database size={20} />,
    <Shield size={20} />
  ];

  const stepNames = [
    'Pattern Classification',
    'RAG Vector Search',
    'Risk Evaluation'
  ];

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '24px 16px' }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
          <Brain size={36} color="#8b5cf6" />
          <h1 style={{ fontSize: 28, fontWeight: 700, margin: 0, background: 'linear-gradient(135deg, #8b5cf6, #06b6d4)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            Agentic AI Fraud Analyzer
          </h1>
        </div>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 14, margin: 0 }}>
          Multi-step reasoning engine powered by LangChain4j + Gemini AI + pgvector RAG
        </p>
      </div>

      {/* Input Section */}
      <div className="surface-panel" style={{ marginBottom: 24, padding: 24 }}>
        <label style={{ display: 'block', fontSize: 14, fontWeight: 600, marginBottom: 8, color: 'var(--color-text-primary)' }}>
          🔍 Suspicious Message / SMS
        </label>
        <textarea
          value={message}
          onChange={e => setMessage(e.target.value)}
          placeholder="Paste a suspicious SMS, WhatsApp message, or email here..."
          rows={4}
          style={{
            width: '100%', padding: 12, borderRadius: 12, border: '1px solid var(--color-border)',
            background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)',
            fontSize: 14, fontFamily: 'inherit', resize: 'vertical', boxSizing: 'border-box'
          }}
        />

        <div style={{ display: 'flex', gap: 12, marginTop: 12, flexWrap: 'wrap' }}>
          <input
            value={upiId}
            onChange={e => setUpiId(e.target.value)}
            placeholder="UPI ID (optional, e.g. merchant@oksbi)"
            style={{
              flex: 1, minWidth: 200, padding: 10, borderRadius: 10, border: '1px solid var(--color-border)',
              background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)', fontSize: 13
            }}
          />
          <button
            className="btn-primary"
            onClick={analyzeMessage}
            disabled={loading || !message.trim()}
            style={{ padding: '10px 24px', borderRadius: 12, display: 'flex', alignItems: 'center', gap: 8 }}
          >
            {loading ? <Loader2 size={18} className="spin" /> : <Zap size={18} />}
            {loading ? 'Analyzing...' : 'Run Agentic Analysis'}
          </button>
        </div>
      </div>

      {/* Step Progress Animation */}
      {(loading || result) && (
        <div className="surface-panel" style={{ marginBottom: 24, padding: 24 }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Brain size={20} color="#8b5cf6" /> Multi-Step Reasoning Chain
          </h3>
          <div style={{ display: 'flex', gap: 0, position: 'relative' }}>
            {[0, 1, 2].map(i => (
              <div key={i} style={{ flex: 1, position: 'relative' }}>
                {/* Connector line */}
                {i < 2 && (
                  <div style={{
                    position: 'absolute', top: 20, left: '50%', width: '100%', height: 3,
                    background: activeStep > i ? 'linear-gradient(90deg, #8b5cf6, #06b6d4)' : 'var(--color-border)',
                    transition: 'background 0.5s ease', zIndex: 0
                  }} />
                )}
                {/* Step circle */}
                <div style={{
                  width: 40, height: 40, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  margin: '0 auto', position: 'relative', zIndex: 1,
                  background: activeStep >= i + 1 ? 'linear-gradient(135deg, #8b5cf6, #06b6d4)' :
                    activeStep === i ? 'var(--color-primary)' : 'var(--color-bg-secondary)',
                  border: activeStep >= i ? '2px solid #8b5cf6' : '2px solid var(--color-border)',
                  color: activeStep >= i ? '#fff' : 'var(--color-text-secondary)',
                  transition: 'all 0.5s ease',
                  animation: activeStep === i && loading ? 'pulse 1.5s infinite' : 'none'
                }}>
                  {stepIcons[i]}
                </div>
                {/* Step label */}
                <p style={{
                  textAlign: 'center', fontSize: 11, marginTop: 8, fontWeight: activeStep >= i ? 600 : 400,
                  color: activeStep >= i ? 'var(--color-text-primary)' : 'var(--color-text-secondary)'
                }}>
                  Step {i + 1}: {stepNames[i]}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="surface-panel status-danger" style={{ marginBottom: 24, padding: 16 }}>
          <p style={{ margin: 0 }}>❌ {error}</p>
        </div>
      )}

      {/* Results */}
      {result && (
        <>
          {/* Risk Score Card */}
          <div className="surface-panel" style={{
            marginBottom: 24, padding: 24,
            borderLeft: `4px solid ${getActionColor(result.action)}`
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                  <div style={{ color: getActionColor(result.action) }}>{getActionIcon(result.action)}</div>
                  <span style={{
                    padding: '4px 12px', borderRadius: 20, fontSize: 13, fontWeight: 700,
                    background: getActionColor(result.action) + '20', color: getActionColor(result.action)
                  }}>
                    {result.action}
                  </span>
                  <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                    ⚡ {result.processingTimeMs}ms
                  </span>
                </div>
                <p style={{ margin: 0, fontSize: 14, color: 'var(--color-text-secondary)' }}>{result.summary}</p>
              </div>
              <div style={{
                width: 80, height: 80, borderRadius: '50%', display: 'flex', flexDirection: 'column',
                alignItems: 'center', justifyContent: 'center',
                border: `4px solid ${getActionColor(result.action)}`,
                background: getActionColor(result.action) + '10'
              }}>
                <span style={{ fontSize: 24, fontWeight: 800, color: getActionColor(result.action) }}>
                  {result.riskScore}
                </span>
                <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>Risk %</span>
              </div>
            </div>
          </div>

          {/* Reasoning Steps Detail */}
          <div className="surface-panel" style={{ marginBottom: 24, padding: 24 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>📋 Reasoning Steps</h3>
            {result.reasoningSteps.map((step, i) => (
              <div key={i} style={{
                padding: 16, marginBottom: 12, borderRadius: 12,
                background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border)'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  <span style={{
                    width: 24, height: 24, borderRadius: '50%', fontSize: 12, fontWeight: 700,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: 'linear-gradient(135deg, #8b5cf6, #06b6d4)', color: '#fff'
                  }}>
                    {step.stepNumber}
                  </span>
                  <span style={{ fontWeight: 600, fontSize: 14 }}>{step.stepName}</span>
                </div>
                <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', margin: '0 0 8px 32px' }}>
                  {step.description}
                </p>
                <pre style={{
                  fontSize: 12, padding: 12, borderRadius: 8, margin: '0 0 0 32px',
                  background: 'var(--color-bg-primary)', border: '1px solid var(--color-border)',
                  whiteSpace: 'pre-wrap', wordBreak: 'break-word', overflowX: 'auto',
                  color: 'var(--color-text-primary)'
                }}>
                  {step.result}
                </pre>
              </div>
            ))}
          </div>

          {/* Matched Patterns */}
          {result.matchedPatterns.length > 0 && (
            <div className="surface-panel" style={{ padding: 24 }}>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
                <Search size={18} style={{ verticalAlign: 'middle', marginRight: 8 }} />
                RAG Matched Patterns ({result.matchedPatterns.length})
              </h3>
              {result.matchedPatterns.map((p, i) => (
                <div key={i} style={{
                  padding: 14, marginBottom: 10, borderRadius: 10,
                  background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border)',
                  display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12
                }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', gap: 8, marginBottom: 6, flexWrap: 'wrap' }}>
                      <span style={{
                        padding: '2px 8px', borderRadius: 6, fontSize: 11, fontWeight: 600,
                        background: getSeverityColor(p.severity) + '20', color: getSeverityColor(p.severity)
                      }}>
                        {p.severity}
                      </span>
                      <span style={{
                        padding: '2px 8px', borderRadius: 6, fontSize: 11,
                        background: '#8b5cf620', color: '#8b5cf6'
                      }}>
                        {p.category}
                      </span>
                    </div>
                    <p style={{ margin: 0, fontSize: 13, color: 'var(--color-text-secondary)' }}>
                      {p.description}
                    </p>
                  </div>
                  <div style={{
                    minWidth: 60, textAlign: 'center', padding: '8px 12px', borderRadius: 10,
                    background: p.similarityPercent >= 80 ? '#ef444420' : p.similarityPercent >= 60 ? '#f59e0b20' : '#3b82f620'
                  }}>
                    <span style={{
                      fontSize: 18, fontWeight: 800,
                      color: p.similarityPercent >= 80 ? '#ef4444' : p.similarityPercent >= 60 ? '#f59e0b' : '#3b82f6'
                    }}>
                      {p.similarityPercent.toFixed(0)}%
                    </span>
                    <p style={{ fontSize: 9, margin: 0, color: 'var(--color-text-secondary)' }}>match</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      <style>{`
        @keyframes pulse {
          0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.4); }
          50% { transform: scale(1.1); box-shadow: 0 0 0 10px rgba(139, 92, 246, 0); }
        }
        .spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
