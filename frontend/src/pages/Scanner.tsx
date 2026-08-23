import { useState, useRef, useEffect } from 'react';
import { Bot, Send, Sparkles, Loader2 } from 'lucide-react';
import { useAuth, useUser } from '@clerk/react';
import api from '../api';

interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export default function Scanner() {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 0,
      role: 'assistant',
      content: "Hi! I'm SafePe Assistant 🤖\n\nI'm your AI helper inside SafePe. Ask me anything — I can help with payments, budgeting tips, how to use SafePe features, or just general questions.\n\nTry asking:\n• \"How do I send money to a UPI ID?\"\n• \"Give me 3 quick tips to save money each month.\"\n• \"Explain what a fixed deposit is in simple words.\"",
      timestamp: new Date()
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

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;

    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: text,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const token = await getToken();
      const headers = { Authorization: `Bearer ${token}` };

      const response = await api.post('/assistant/chat', { message: text }, { headers });
      const reply = response.data.reply;

      const botMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: reply,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, botMsg]);
    } catch (error) {
      console.error("Failed to get assistant reply", error);
      const errorMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: "Sorry, I couldn't respond right now. Please try again in a moment.",
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

  return (
    <div style={{
      maxWidth: '800px',
      margin: '0 auto',
      height: 'calc(100vh - 120px)',
      display: 'flex',
      flexDirection: 'column',
      background: 'var(--color-bg-primary)',
      borderRadius: '16px',
      overflow: 'hidden',
      border: '1px solid var(--color-border)'
    }}>
      {/* Chat Header */}
      <div style={{
        padding: '16px 20px',
        background: 'linear-gradient(135deg, #8b5cf6, #3b82f6)',
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        flexShrink: 0
      }}>
        <div style={{
          background: 'rgba(255,255,255,0.2)',
          padding: '8px',
          borderRadius: '12px',
          backdropFilter: 'blur(10px)'
        }}>
          <Bot size={28} color="white" />
        </div>
        <div>
          <h2 style={{ fontSize: '18px', color: '#fff', fontWeight: '700', margin: 0 }}>SafePe Assistant</h2>
          <p style={{ fontSize: '12px', color: 'rgba(255,255,255,0.8)', margin: 0 }}>
            AI Assistant • Powered by Gemini AI
          </p>
        </div>
        <div style={{
          marginLeft: 'auto',
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          background: 'rgba(16, 185, 129, 0.3)',
          padding: '4px 12px',
          borderRadius: '20px'
        }}>
          <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981', animation: 'pulse-dot 2s infinite' }} />
          <span style={{ fontSize: '12px', color: '#fff', fontWeight: '500' }}>Online</span>
        </div>
      </div>

      {/* Messages Area */}
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '16px',
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
            <div style={{ maxWidth: '80%', display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
              {/* Bot avatar */}
              {msg.role === 'assistant' && (
                <div style={{
                  width: '32px', height: '32px', borderRadius: '50%',
                  background: 'linear-gradient(135deg, #8b5cf6, #3b82f6)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0
                }}>
                  <Bot size={16} color="#fff" />
                </div>
              )}

              <div>
                <div style={{
                  padding: '12px 16px',
                  borderRadius: msg.role === 'user'
                    ? '16px 16px 4px 16px'
                    : '16px 16px 16px 4px',
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
                  wordBreak: 'break-word'
                }}>
                  {msg.content}
                </div>
                <div style={{
                  fontSize: '11px',
                  color: 'var(--color-text-secondary)',
                  marginTop: '4px',
                  textAlign: msg.role === 'user' ? 'right' : 'left',
                  paddingLeft: msg.role === 'assistant' ? '4px' : '0',
                  paddingRight: msg.role === 'user' ? '4px' : '0'
                }}>
                  {formatTime(msg.timestamp)}
                </div>
              </div>

              {/* User avatar */}
              {msg.role === 'user' && (
                <div style={{
                  width: '32px', height: '32px', borderRadius: '50%',
                  background: 'linear-gradient(135deg, #10b981, #059669)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  color: '#fff', fontWeight: '700', fontSize: '14px'
                }}>
                  {user?.firstName?.charAt(0) || 'U'}
                </div>
              )}
            </div>
          </div>
        ))}

        {/* Typing indicator */}
        {loading && (
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: '8px', animation: 'chat-slide-in 0.3s ease-out' }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '50%',
              background: 'linear-gradient(135deg, #8b5cf6, #3b82f6)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0
            }}>
              <Bot size={16} color="#fff" />
            </div>
            <div style={{
              padding: '16px 20px',
              borderRadius: '16px 16px 16px 4px',
              background: 'var(--color-bg-surface)',
              border: '1px solid var(--color-border)',
              display: 'flex', gap: '6px', alignItems: 'center'
            }}>
              <div className="typing-dot" style={{ animationDelay: '0ms' }} />
              <div className="typing-dot" style={{ animationDelay: '150ms' }} />
              <div className="typing-dot" style={{ animationDelay: '300ms' }} />
              <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)', marginLeft: '8px' }}>
                Assistant is typing...
              </span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div style={{
        padding: '16px 20px',
        background: 'var(--color-bg-primary)',
        borderTop: '1px solid var(--color-border)',
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
            placeholder="Ask SafePe Assistant anything..."
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
            onClick={handleSend}
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
            Powered by Gemini AI
          </span>
        </div>
      </div>

      {/* Animations */}
      <style>{`
        @keyframes chat-slide-in {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes pulse-dot {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
        .typing-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: var(--color-text-secondary);
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
