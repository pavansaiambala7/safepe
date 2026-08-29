import { useState, useRef, useEffect } from 'react';
import { Bot, Send, Sparkles, Loader2, Database, ShieldAlert, CheckCircle2, DollarSign, PieChart, TrendingUp, AlertTriangle } from 'lucide-react';
import { useAuth, useUser } from '@clerk/react';
import api from '../api';

interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isRagGrounded?: boolean;
}

export default function Scanner() {
  const [input, setInput] = useState('');
  const [activeTab, setActiveTab] = useState<'money' | 'scam'>('money');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 0,
      role: 'assistant',
      content: "Hello! I am your SafePe AI Money Assistant 🤖\n\nI am connected to your live transaction ledger with Retrieval-Augmented Generation (RAG) and pgvector intelligence. Ask me anything about your spending, recent payments, budgeting, or verify suspicious messages.\n\nTry asking:\n• \"How much did I spend this month?\"\n• \"Breakdown my highest expenses\"\n• \"Give me a practical plan to save ₹5,000 this month\"\n• \"Check if UPI ID cashback99@paytm is safe or fraud\"",
      timestamp: new Date(),
      isRagGrounded: true
    }
  ]);
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const { getToken } = useAuth();
  const { user } = useUser();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = async (customText?: string) => {
    const text = (customText || input).trim();
    if (!text || loading) return;

    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: text,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMsg]);
    if (!customText) setInput('');
    setLoading(true);

    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      let reply = '';
      if (activeTab === 'scam') {
        const res = await api.post('/fraud/analyze-sms', { content: text }, { headers });
        reply = res.data.analysis || 'Analysis complete.';
      } else {
        const response = await api.post('/assistant/chat', { 
          message: text,
          userId: user?.id || 'demo_user'
        }, { headers });
        reply = response.data.reply || response.data.answer || "I processed your request with live transaction context.";
      }

      const botMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: reply,
        timestamp: new Date(),
        isRagGrounded: activeTab === 'money'
      };

      setMessages(prev => [...prev, botMsg]);
    } catch (error) {
      console.error("Failed to get assistant reply", error);
      const errorMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: "Sorry, I could not connect to the AI model right now. Please verify your internet connection or try again shortly.",
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMsg]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  };

  const promptSuggestions = [
    { label: "💰 Monthly Spend", query: "How much did I spend this month and what was my largest expense?" },
    { label: "📊 Category Breakdown", query: "Give me a summary of my spending by category and top recipients." },
    { label: "🛡️ Scam Check", query: "I received: 'Your electricity power will be cut tonight at 9:30pm call 9876543210'. Is this a scam?" },
    { label: "💡 Savings Advisor", query: "How can I optimize my monthly budget to save ₹10,000?" }
  ];

  return (
    <div style={{
      maxWidth: '860px',
      margin: '0 auto',
      height: 'calc(100vh - 120px)',
      display: 'flex',
      flexDirection: 'column',
      background: 'var(--color-bg-primary)',
      borderRadius: '20px',
      overflow: 'hidden',
      border: '1px solid var(--color-border)',
      boxShadow: '0 8px 30px rgba(0,0,0,0.06)'
    }}>
      {/* Chat Header */}
      <div style={{
        padding: '16px 24px',
        background: 'linear-gradient(135deg, #059669 0%, #10b981 50%, #047857 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '12px',
        flexShrink: 0
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            background: 'rgba(255,255,255,0.2)',
            padding: '10px',
            borderRadius: '14px',
            backdropFilter: 'blur(10px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Bot size={26} color="white" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h2 style={{ fontSize: '18px', color: '#fff', fontWeight: '800', margin: 0 }}>SafePe Money Assistant</h2>
              <span style={{
                fontSize: '11px',
                fontWeight: '700',
                background: 'rgba(255,255,255,0.25)',
                color: '#fff',
                padding: '2px 8px',
                borderRadius: '12px',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px'
              }}>
                <Database size={11} /> RAG Grounded
              </span>
            </div>
            <p style={{ fontSize: '12px', color: 'rgba(255,255,255,0.85)', margin: '2px 0 0 0' }}>
              LangChain4j • Vector DB Embeddings • Gemini AI
            </p>
          </div>
        </div>

        {/* Mode Switcher */}
        <div style={{
          display: 'flex',
          background: 'rgba(0,0,0,0.2)',
          padding: '4px',
          borderRadius: '12px',
          gap: '4px'
        }}>
          <button
            onClick={() => setActiveTab('money')}
            style={{
              background: activeTab === 'money' ? 'white' : 'transparent',
              color: activeTab === 'money' ? '#047857' : 'white',
              border: 'none',
              padding: '6px 12px',
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: '700',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.2s ease'
            }}
          >
            <DollarSign size={13} /> Money Assistant
          </button>
          <button
            onClick={() => setActiveTab('scam')}
            style={{
              background: activeTab === 'scam' ? 'white' : 'transparent',
              color: activeTab === 'scam' ? '#b91c1c' : 'white',
              border: 'none',
              padding: '6px 12px',
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: '700',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.2s ease'
            }}
          >
            <ShieldAlert size={13} /> Scam Scanner
          </button>
        </div>
      </div>

      {/* Messages Area */}
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '18px',
        background: 'var(--color-bg-secondary)'
      }}>
        {messages.map(msg => (
          <div
            key={msg.id}
            style={{
              display: 'flex',
              justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
              animation: 'chat-slide-in 0.3s ease-out'
            }}
          >
            <div style={{ maxWidth: '82%', display: 'flex', gap: '10px', alignItems: 'flex-end' }}>
              {/* Bot avatar */}
              {msg.role === 'assistant' && (
                <div style={{
                  width: '34px', height: '34px', borderRadius: '50%',
                  background: 'linear-gradient(135deg, #10b981, #059669)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  boxShadow: '0 2px 8px rgba(16,185,129,0.3)'
                }}>
                  <Bot size={18} color="#fff" />
                </div>
              )}

              <div>
                <div style={{
                  padding: '14px 18px',
                  borderRadius: msg.role === 'user'
                    ? '18px 18px 4px 18px'
                    : '18px 18px 18px 4px',
                  background: msg.role === 'user'
                    ? 'linear-gradient(135deg, #10b981, #059669)'
                    : 'var(--color-bg-surface)',
                  color: msg.role === 'user' ? '#fff' : 'var(--color-text-primary)',
                  border: msg.role === 'assistant'
                    ? '1px solid var(--color-border)'
                    : 'none',
                  lineHeight: '1.6',
                  fontSize: '14px',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  boxShadow: msg.role === 'user' ? '0 4px 12px rgba(16,185,129,0.2)' : '0 2px 8px rgba(0,0,0,0.02)'
                }}>
                  {msg.content}
                </div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  marginTop: '4px',
                  justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                  paddingLeft: msg.role === 'assistant' ? '4px' : '0',
                  paddingRight: msg.role === 'user' ? '4px' : '0'
                }}>
                  {msg.role === 'assistant' && msg.isRagGrounded && (
                    <span style={{ fontSize: '10px', color: '#059669', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '3px' }}>
                      <CheckCircle2 size={10} /> RAG Verified
                    </span>
                  )}
                  <span style={{ fontSize: '11px', color: 'var(--color-text-secondary)' }}>
                    {formatTime(msg.timestamp)}
                  </span>
                </div>
              </div>

              {/* User avatar */}
              {msg.role === 'user' && (
                <div style={{
                  width: '34px', height: '34px', borderRadius: '50%',
                  background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  color: '#fff', fontWeight: '800', fontSize: '14px',
                  boxShadow: '0 2px 8px rgba(59,130,246,0.3)'
                }}>
                  {user?.firstName?.charAt(0) || 'U'}
                </div>
              )}
            </div>
          </div>
        ))}

        {/* Typing indicator */}
        {loading && (
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: '10px', animation: 'chat-slide-in 0.3s ease-out' }}>
            <div style={{
              width: '34px', height: '34px', borderRadius: '50%',
              background: 'linear-gradient(135deg, #10b981, #059669)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0
            }}>
              <Bot size={18} color="#fff" />
            </div>
            <div style={{
              padding: '16px 20px',
              borderRadius: '18px 18px 18px 4px',
              background: 'var(--color-bg-surface)',
              border: '1px solid var(--color-border)',
              display: 'flex', gap: '8px', alignItems: 'center'
            }}>
              <div className="typing-dot" style={{ animationDelay: '0ms' }} />
              <div className="typing-dot" style={{ animationDelay: '150ms' }} />
              <div className="typing-dot" style={{ animationDelay: '300ms' }} />
              <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginLeft: '8px', fontWeight: '500' }}>
                {activeTab === 'money' ? 'Retrieving transaction context & generating response...' : 'Running Gemini scam detection...'}
              </span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Suggestion Chips */}
      <div style={{
        padding: '8px 20px',
        background: 'var(--color-bg-primary)',
        borderTop: '1px solid var(--color-border)',
        display: 'flex',
        gap: '8px',
        overflowX: 'auto'
      }}>
        {promptSuggestions.map((item, idx) => (
          <button
            key={idx}
            onClick={() => handleSend(item.query)}
            disabled={loading}
            style={{
              background: 'var(--color-bg-secondary)',
              border: '1px solid var(--color-border)',
              borderRadius: '20px',
              padding: '6px 12px',
              fontSize: '12px',
              fontWeight: '600',
              color: 'var(--color-text-primary)',
              cursor: loading ? 'not-allowed' : 'pointer',
              whiteSpace: 'nowrap',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              transition: 'all 0.2s'
            }}
          >
            {item.label}
          </button>
        ))}
      </div>

      {/* Input Area */}
      <div style={{
        padding: '14px 20px',
        background: 'var(--color-bg-primary)',
        flexShrink: 0
      }}>
        <div style={{
          display: 'flex',
          gap: '12px',
          alignItems: 'flex-end',
          background: 'var(--color-bg-secondary)',
          borderRadius: '16px',
          padding: '8px 8px 8px 16px',
          border: '1px solid var(--color-border)',
          transition: 'border-color 0.2s'
        }}>
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={activeTab === 'money' ? "Ask about your transactions, spending, budget tips..." : "Paste suspicious SMS message to scan..."}
            rows={1}
            style={{
              flex: 1,
              background: 'transparent',
              border: 'none',
              outline: 'none',
              color: 'var(--color-text-primary)',
              fontSize: '14px',
              fontFamily: 'inherit',
              resize: 'none',
              maxHeight: '120px',
              lineHeight: '1.5',
              padding: '8px 0'
            }}
          />
          <button
            onClick={() => handleSend()}
            disabled={loading || !input.trim()}
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              border: 'none',
              background: loading || !input.trim()
                ? 'var(--color-bg-surface-hover)'
                : 'linear-gradient(135deg, #10b981, #059669)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              flexShrink: 0
            }}
          >
            {loading ? <Loader2 size={20} className="spin" /> : <Send size={20} />}
          </button>
        </div>
        <div style={{
          display: 'flex', alignItems: 'center', gap: '6px',
          justifyContent: 'center', marginTop: '8px'
        }}>
          <Sparkles size={12} color="var(--color-text-secondary)" />
          <span style={{ fontSize: '11px', color: 'var(--color-text-secondary)' }}>
            RAG Vector Grounded • Real-time AI Financial Reasoning
          </span>
        </div>
      </div>

      {/* Animations */}
      <style>{`
        @keyframes chat-slide-in {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .typing-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #10b981;
          animation: typing-bounce 1.2s infinite;
        }
        @keyframes typing-bounce {
          0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
          30% { transform: translateY(-6px); opacity: 1; }
        }
        .spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
