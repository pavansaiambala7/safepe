import { useState, useRef, useEffect } from 'react';
import { Bot, Send, Sparkles, Loader2, ShieldAlert, ShieldCheck, CheckCircle2, DollarSign, Wallet, AlertTriangle, MessageSquare, ArrowRight, RefreshCw, Zap } from 'lucide-react';
import { useAuth, useUser } from '@clerk/react';
import api from '../api';

interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isRagGrounded?: boolean;
  category?: 'money' | 'scam';
}

export default function Scanner() {
  const [input, setInput] = useState('');
  const [activeTab, setActiveTab] = useState<'money' | 'scam'>('money');
  
  // Separate initial state for Money Assistant
  const [moneyMessages, setMoneyMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      role: 'assistant',
      content: "👋 Hi! I'm your **SafePe Money Assistant**.\n\nI can analyze your spending, look up past transactions, and give you smart budgeting advice. \n\n**Try asking:**\n• \"How much money did I spend on Food & Dining this month?\"\n• \"What was my largest transaction recently?\"\n• \"Give me a plan to save ₹5,000 this month.\"",
      timestamp: new Date(),
      isRagGrounded: true,
      category: 'money'
    }
  ]);

  // Separate initial state for Scam Scanner
  const [scamMessages, setScamMessages] = useState<ChatMessage[]>([
    {
      id: 2,
      role: 'assistant',
      content: "🛡️ **SafePe Scam & Fraud Shield**\n\nPaste any suspicious SMS, payment link, or UPI ID below. I will analyze it against known phishing patterns and give you an instant Risk & Trust verdict.",
      timestamp: new Date(),
      isRagGrounded: false,
      category: 'scam'
    }
  ]);

  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const { getToken } = useAuth();
  const { user } = useUser();

  const currentMessages = activeTab === 'money' ? moneyMessages : scamMessages;

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [moneyMessages, scamMessages, activeTab]);

  const handleSend = async (customText?: string) => {
    const text = (customText || input).trim();
    if (!text || loading) return;

    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: text,
      timestamp: new Date(),
      category: activeTab
    };

    if (activeTab === 'money') {
      setMoneyMessages(prev => [...prev, userMsg]);
    } else {
      setScamMessages(prev => [...prev, userMsg]);
    }

    if (!customText) setInput('');
    setLoading(true);

    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      let reply = '';
      if (activeTab === 'scam') {
        const res = await api.post('/fraud/analyze-sms', { content: text }, { headers });
        reply = res.data.analysis || 'Analysis complete. No severe threat detected.';
      } else {
        const response = await api.post('/assistant/chat', { 
          message: text,
          userId: user?.id || 'demo_user'
        }, { headers });
        reply = response.data.reply || response.data.answer || "I checked your transactions and processed your request.";
      }

      const botMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: reply,
        timestamp: new Date(),
        isRagGrounded: activeTab === 'money',
        category: activeTab
      };

      if (activeTab === 'money') {
        setMoneyMessages(prev => [...prev, botMsg]);
      } else {
        setScamMessages(prev => [...prev, botMsg]);
      }
    } catch (error) {
      console.error("Assistant request failed", error);
      const errorMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: "Sorry, I couldn't reach the AI service right now. Please check your connection and try again.",
        timestamp: new Date(),
        category: activeTab
      };
      if (activeTab === 'money') {
        setMoneyMessages(prev => [...prev, errorMsg]);
      } else {
        setScamMessages(prev => [...prev, errorMsg]);
      }
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

  // Money Assistant Example Prompts
  const moneyPrompts = [
    { title: "🍔 Food & Dining Spend", query: "How much money did I spend on Food & Dining this month?" },
    { title: "💸 Largest Expense", query: "What was my largest transaction recently and who was it to?" },
    { title: "📊 Spend Breakdown", query: "Give me a quick breakdown of all my expenses by category." },
    { title: "💡 Saving Advice", query: "How can I save ₹5,000 this month based on my spending habits?" }
  ];

  // Scam Scanner Example Prompts
  const scamPrompts = [
    { title: "⚡ Electricity Disconnection", query: "Dear customer, your electricity power will be disconnected at 9:30 PM tonight due to unpaid bill. Call officer at 9876543210 immediately." },
    { title: "🎁 Lottery Prize Alert", query: "Congratulations! You have won ₹25,00,000 in SafePe Lucky Draw. Click bit.ly/claim-prize to pay ₹500 fee and claim cash." },
    { title: "🏦 KYC Expired Warning", query: "Your SBI Bank KYC has expired. Your account will be blocked within 24 hours. Update KYC now at http://sbi-kyc-update.xyz" },
    { title: "💳 Part-time Job Offer", query: "Earn ₹3,000 daily working from home just by liking YouTube videos. Deposit ₹1,000 security to start now." }
  ];

  const currentPrompts = activeTab === 'money' ? moneyPrompts : scamPrompts;

  return (
    <div className="animate-fade-up" style={{
      maxWidth: '920px',
      margin: '0 auto',
      height: 'calc(100vh - 110px)',
      display: 'flex',
      flexDirection: 'column',
      background: 'var(--color-bg-primary)',
      borderRadius: '24px',
      overflow: 'hidden',
      border: '1px solid var(--color-border)',
      boxShadow: '0 12px 36px rgba(0,0,0,0.06)'
    }}>
      
      {/* Top Header Bar */}
      <div style={{
        padding: '18px 24px',
        background: activeTab === 'money'
          ? 'linear-gradient(135deg, #059669 0%, #10b981 60%, #0d9488 100%)'
          : 'linear-gradient(135deg, #b91c1c 0%, #dc2626 50%, #991b1b 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '14px',
        flexShrink: 0,
        transition: 'background 0.3s ease'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            background: 'rgba(255,255,255,0.2)',
            padding: '10px',
            borderRadius: '14px',
            backdropFilter: 'blur(10px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
          }}>
            {activeTab === 'money' ? <Bot size={28} color="white" /> : <ShieldAlert size={28} color="white" />}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h2 style={{ fontSize: '19px', color: '#fff', fontWeight: '800', margin: 0, letterSpacing: '-0.3px' }}>
                {activeTab === 'money' ? 'SafePe Money Assistant' : 'Scam & Phishing Scanner'}
              </h2>
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
                <Zap size={11} /> AI Copilot
              </span>
            </div>
            <p style={{ fontSize: '12px', color: 'rgba(255,255,255,0.88)', margin: '3px 0 0 0', fontWeight: '500' }}>
              {activeTab === 'money' 
                ? 'Smart Spending Analytics • Budget Copilot • Gemini AI' 
                : 'Phishing Detector • URL & SMS Risk Evaluator • Gemini AI'}
            </p>
          </div>
        </div>

        {/* Distinct Segmented Mode Switcher */}
        <div style={{
          display: 'flex',
          background: 'rgba(0,0,0,0.25)',
          padding: '4px',
          borderRadius: '14px',
          backdropFilter: 'blur(8px)',
          gap: '4px'
        }}>
          <button
            onClick={() => setActiveTab('money')}
            style={{
              background: activeTab === 'money' ? '#ffffff' : 'transparent',
              color: activeTab === 'money' ? '#047857' : '#ffffff',
              border: 'none',
              padding: '7px 14px',
              borderRadius: '10px',
              fontSize: '13px',
              fontWeight: '700',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: activeTab === 'money' ? '0 2px 8px rgba(0,0,0,0.15)' : 'none',
              transition: 'all 0.2s ease'
            }}
          >
            <DollarSign size={15} /> Money Assistant
          </button>
          <button
            onClick={() => setActiveTab('scam')}
            style={{
              background: activeTab === 'scam' ? '#ffffff' : 'transparent',
              color: activeTab === 'scam' ? '#b91c1c' : '#ffffff',
              border: 'none',
              padding: '7px 14px',
              borderRadius: '10px',
              fontSize: '13px',
              fontWeight: '700',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: activeTab === 'scam' ? '0 2px 8px rgba(0,0,0,0.15)' : 'none',
              transition: 'all 0.2s ease'
            }}
          >
            <ShieldAlert size={15} /> Scam Scanner
          </button>
        </div>
      </div>

      {/* Messages Container */}
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '24px 20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '18px',
        background: 'var(--color-bg-secondary)'
      }}>
        {currentMessages.map(msg => (
          <div
            key={msg.id}
            style={{
              display: 'flex',
              justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
              animation: 'chat-slide-in 0.25s ease-out'
            }}
          >
            <div style={{ maxWidth: '84%', display: 'flex', gap: '10px', alignItems: 'flex-end' }}>
              
              {/* Bot avatar */}
              {msg.role === 'assistant' && (
                <div style={{
                  width: '36px', height: '36px', borderRadius: '50%',
                  background: activeTab === 'money' 
                    ? 'linear-gradient(135deg, #10b981, #059669)' 
                    : 'linear-gradient(135deg, #ef4444, #b91c1c)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  boxShadow: '0 2px 10px rgba(0,0,0,0.1)'
                }}>
                  {activeTab === 'money' ? <Bot size={18} color="#fff" /> : <ShieldAlert size={18} color="#fff" />}
                </div>
              )}

              <div>
                <div style={{
                  padding: '14px 18px',
                  borderRadius: msg.role === 'user'
                    ? '18px 18px 4px 18px'
                    : '18px 18px 18px 4px',
                  background: msg.role === 'user'
                    ? activeTab === 'money'
                      ? 'linear-gradient(135deg, #10b981, #059669)'
                      : 'linear-gradient(135deg, #ef4444, #dc2626)'
                    : 'var(--color-bg-surface)',
                  color: msg.role === 'user' ? '#fff' : 'var(--color-text-primary)',
                  border: msg.role === 'assistant'
                    ? '1px solid var(--color-border)'
                    : 'none',
                  lineHeight: '1.65',
                  fontSize: '14px',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  boxShadow: msg.role === 'user' 
                    ? '0 4px 14px rgba(0,0,0,0.12)' 
                    : '0 2px 8px rgba(0,0,0,0.03)'
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
                  {msg.role === 'assistant' && (
                    <span style={{ 
                      fontSize: '10px', 
                      color: activeTab === 'money' ? '#059669' : '#dc2626', 
                      fontWeight: '700', 
                      display: 'flex', 
                      alignItems: 'center', 
                      gap: '3px' 
                    }}>
                      <CheckCircle2 size={11} /> {activeTab === 'money' ? 'AI Grounded' : 'Scam Analyzer Verified'}
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
                  width: '36px', height: '36px', borderRadius: '50%',
                  background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  color: '#fff', fontWeight: '800', fontSize: '14px',
                  boxShadow: '0 2px 10px rgba(59,130,246,0.25)'
                }}>
                  {user?.firstName?.charAt(0) || 'U'}
                </div>
              )}
            </div>
          </div>
        ))}

        {/* Loading / Typing Animation */}
        {loading && (
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: '10px', animation: 'chat-slide-in 0.25s ease-out' }}>
            <div style={{
              width: '36px', height: '36px', borderRadius: '50%',
              background: activeTab === 'money' ? '#10b981' : '#ef4444',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0
            }}>
              {activeTab === 'money' ? <Bot size={18} color="#fff" /> : <ShieldAlert size={18} color="#fff" />}
            </div>
            <div style={{
              padding: '14px 18px',
              borderRadius: '18px 18px 18px 4px',
              background: 'var(--color-bg-surface)',
              border: '1px solid var(--color-border)',
              display: 'flex', gap: '6px', alignItems: 'center'
            }}>
              <div className="typing-dot" style={{ background: activeTab === 'money' ? '#10b981' : '#ef4444', animationDelay: '0ms' }} />
              <div className="typing-dot" style={{ background: activeTab === 'money' ? '#10b981' : '#ef4444', animationDelay: '150ms' }} />
              <div className="typing-dot" style={{ background: activeTab === 'money' ? '#10b981' : '#ef4444', animationDelay: '300ms' }} />
              <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginLeft: '8px', fontWeight: '600' }}>
                {activeTab === 'money' ? 'Calculating transactions & spending insights...' : 'Scanning message for scam & phishing patterns...'}
              </span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Interactive Quick-Prompt Cards Section */}
      <div style={{
        padding: '10px 20px',
        background: 'var(--color-bg-primary)',
        borderTop: '1px solid var(--color-border)',
        display: 'flex',
        flexDirection: 'column',
        gap: '6px'
      }}>
        <div style={{ fontSize: '11px', fontWeight: '700', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          {activeTab === 'money' ? '💡 Try Asking Money Assistant:' : '⚠️ Try Scanning Sample Scam Messages:'}
        </div>
        <div style={{
          display: 'flex',
          gap: '8px',
          overflowX: 'auto',
          paddingBottom: '4px'
        }}>
          {currentPrompts.map((item, idx) => (
            <button
              key={idx}
              onClick={() => handleSend(item.query)}
              disabled={loading}
              style={{
                background: 'var(--color-bg-secondary)',
                border: '1px solid var(--color-border)',
                borderRadius: '12px',
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: '600',
                color: 'var(--color-text-primary)',
                cursor: loading ? 'not-allowed' : 'pointer',
                whiteSpace: 'nowrap',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'all 0.15s ease',
                boxShadow: '0 1px 4px rgba(0,0,0,0.02)'
              }}
            >
              <span>{item.title}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Input Box */}
      <div style={{
        padding: '12px 20px 16px 20px',
        background: 'var(--color-bg-primary)',
        flexShrink: 0
      }}>
        <div style={{
          display: 'flex',
          gap: '12px',
          alignItems: 'flex-end',
          background: 'var(--color-bg-secondary)',
          borderRadius: '18px',
          padding: '8px 8px 8px 16px',
          border: '1px solid var(--color-border)',
          transition: 'border-color 0.2s'
        }}>
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={activeTab === 'money' 
              ? "Ask how much money you spent, budget advice, or transaction details..." 
              : "Paste any suspicious SMS message or UPI ID to detect scams..."}
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
              borderRadius: '14px',
              border: 'none',
              background: loading || !input.trim()
                ? 'var(--color-bg-surface-hover)'
                : activeTab === 'money'
                  ? 'linear-gradient(135deg, #10b981, #059669)'
                  : 'linear-gradient(135deg, #ef4444, #dc2626)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              flexShrink: 0,
              boxShadow: input.trim() ? '0 4px 12px rgba(0,0,0,0.15)' : 'none'
            }}
          >
            {loading ? <Loader2 size={20} className="spin" /> : <Send size={19} />}
          </button>
        </div>

        <div style={{
          display: 'flex', alignItems: 'center', gap: '6px',
          justifyContent: 'center', marginTop: '8px'
        }}>
          <Sparkles size={12} color="var(--color-text-secondary)" />
          <span style={{ fontSize: '11px', color: 'var(--color-text-secondary)', fontWeight: '500' }}>
            Powered by Gemini AI • Real-time Financial & Security Reasoning
          </span>
        </div>
      </div>

      {/* Styles & Animations */}
      <style>{`
        @keyframes chat-slide-in {
          from { opacity: 0; transform: translateY(8px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .typing-dot {
          width: 7px;
          height: 7px;
          border-radius: 50%;
          animation: typing-bounce 1.2s infinite;
        }
        @keyframes typing-bounce {
          0%, 60%, 100% { transform: translateY(0); opacity: 0.35; }
          30% { transform: translateY(-5px); opacity: 1; }
        }
        .spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
