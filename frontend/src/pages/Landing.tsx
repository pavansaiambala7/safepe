import { useEffect, useRef, useState } from "react";
import {
  ShieldCheck, Lock, Zap, GitFork, Server,
  Radio, Database, Bell, ArrowRight, Activity,
  ChevronRight, Shield, Cpu, CreditCard, Search, BarChart3
} from "lucide-react";
import { SignInButton } from "@clerk/react";

function Counter({ target, suffix }: { target: number; suffix?: string }) {
  const [value, setValue] = useState(0);
  const ref = useRef<HTMLSpanElement>(null);
  useEffect(() => {
    const obs = new IntersectionObserver(([e]) => {
      if (!e.isIntersecting) return;
      obs.disconnect();
      let start = 0;
      const step = target / 60;
      const tick = () => {
        start += step;
        if (start < target) { setValue(Math.floor(start)); requestAnimationFrame(tick); }
        else setValue(target);
      };
      requestAnimationFrame(tick);
    }, { threshold: 0.3 });
    if (ref.current) obs.observe(ref.current);
    return () => obs.disconnect();
  }, [target]);
  return <span ref={ref}>{value.toLocaleString()}{suffix || ""}</span>;
}

function ParticleCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    let W = (canvas.width = window.innerWidth);
    const H = (canvas.height = 700);
    const pts = Array.from({ length: 55 }, () => ({
      x: Math.random() * W, y: Math.random() * H,
      vx: (Math.random() - 0.5) * 0.3, vy: (Math.random() - 0.5) * 0.3,
      r: Math.random() * 2 + 1,
    }));
    let raf = 0;
    const draw = () => {
      ctx.clearRect(0, 0, W, H);
      pts.forEach((p) => {
        p.x += p.vx; p.y += p.vy;
        if (p.x < 0 || p.x > W) p.vx *= -1;
        if (p.y < 0 || p.y > H) p.vy *= -1;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = "rgba(16,185,129,0.5)";
        ctx.fill();
      });
      for (let i = 0; i < pts.length; i++) {
        for (let j = i + 1; j < pts.length; j++) {
          const d = Math.hypot(pts[i].x - pts[j].x, pts[i].y - pts[j].y);
          if (d < 120) {
            ctx.beginPath();
            ctx.moveTo(pts[i].x, pts[i].y);
            ctx.lineTo(pts[j].x, pts[j].y);
            ctx.strokeStyle = "rgba(16,185,129," + (0.12 * (1 - d / 120)) + ")";
            ctx.lineWidth = 0.8;
            ctx.stroke();
          }
        }
      }
      raf = requestAnimationFrame(draw);
    };
    draw();
    const onResize = () => { W = canvas.width = window.innerWidth; };
    window.addEventListener("resize", onResize);
    return () => { cancelAnimationFrame(raf); window.removeEventListener("resize", onResize); };
  }, []);
  return (
    <canvas ref={canvasRef} style={{
      position: "absolute", top: 0, left: 0, width: "100%", height: "700px",
      pointerEvents: "none", opacity: 0.7,
    }} />
  );
}

interface FeatureCardProps {
  icon: React.ReactNode;
  color: string;
  label: string;
  title: string;
  desc: string;
  delay: string;
}

function FeatureCard({ icon, color, label, title, desc, delay }: FeatureCardProps) {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered ? "rgba(255,255,255,0.06)" : "rgba(255,255,255,0.03)",
        border: hovered ? "1px solid " + color + "50" : "1px solid rgba(255,255,255,0.08)",
        borderRadius: "20px", padding: "28px",
        backdropFilter: "blur(12px)",
        transition: "all 0.4s cubic-bezier(0.16,1,0.3,1)",
        animation: "sfadeUp 0.8s cubic-bezier(0.16,1,0.3,1) " + delay + " both",
        cursor: "default",
        transform: hovered ? "translateY(-6px)" : "translateY(0)",
        boxShadow: hovered ? "0 20px 40px " + color + "15" : "none",
      }}
    >
      <div style={{
        width: "52px", height: "52px", borderRadius: "14px",
        background: color + "20", display: "flex", alignItems: "center",
        justifyContent: "center", marginBottom: "18px",
        border: "1px solid " + color + "30",
      }}>{icon}</div>
      <div style={{ fontSize: "10px", fontWeight: "800", color, textTransform: "uppercase", letterSpacing: "1.5px", marginBottom: "8px" }}>{label}</div>
      <h3 style={{ fontSize: "17px", fontWeight: "700", color: "#f1f5f9", marginBottom: "10px", letterSpacing: "-0.02em" }}>{title}</h3>
      <p style={{ fontSize: "13px", color: "#94a3b8", lineHeight: "1.7", margin: 0 }}>{desc}</p>
    </div>
  );
}

function StatBadge({ value, suffix, label, color }: { value: number; suffix?: string; label: string; color: string }) {
  return (
    <div style={{
      textAlign: "center", padding: "28px 20px",
      background: "rgba(255,255,255,0.03)",
      border: "1px solid " + color + "25", borderRadius: "20px",
      backdropFilter: "blur(12px)",
    }}>
      <div style={{ fontSize: "42px", fontWeight: "900", color, letterSpacing: "-0.04em", lineHeight: 1 }}>
        <Counter target={value} suffix={suffix} />
      </div>
      <div style={{ fontSize: "12px", color: "#94a3b8", marginTop: "8px", fontWeight: "600" }}>{label}</div>
    </div>
  );
}

export default function Landing() {
  const services = [
    { dot: "#10b981", label: "Eureka Registry", sub: ":8761" },
    { dot: "#3b82f6", label: "API Gateway", sub: ":8080" },
    { dot: "#8b5cf6", label: "AI Fraud Engine", sub: ":8082" },
    { dot: "#f59e0b", label: "Kafka KRaft", sub: ":9092" },
  ];

  const securityItems = [
    { icon: <ShieldCheck size={20} color="#10b981" />, title: "Clerk JWKS JWT", desc: "Stateless cryptographic signature validation at Gateway" },
    { icon: <Lock size={20} color="#8b5cf6" />, title: "AES-256 GCM Vault", desc: "Card numbers and UPI IDs encrypted at rest with IV" },
    { icon: <Zap size={20} color="#f59e0b" />, title: "Bucket4j Rate Limit", desc: "100 req/min per IP, blocks automated attacks" },
    { icon: <Activity size={20} color="#3b82f6" />, title: "Vector Trust Score", desc: "Cosine similarity 60pct+ triggers auto BLOCK with audit" },
  ];

  return (
    <div style={{ background: "#030d1a", minHeight: "100vh", color: "#e2e8f0", fontFamily: "'Outfit', sans-serif", overflowX: "hidden" }}>

      <section style={{ position: "relative", minHeight: "700px", display: "flex", alignItems: "center", justifyContent: "center", textAlign: "center", overflow: "hidden" }}>
        <div style={{ position: "absolute", top: "50%", left: "50%", transform: "translate(-50%, -50%)", width: "800px", height: "500px", background: "radial-gradient(ellipse at center, rgba(16,185,129,0.12) 0%, transparent 70%)", pointerEvents: "none" }} />
        <ParticleCanvas />
        <div style={{ position: "relative", zIndex: 2, maxWidth: "900px", padding: "0 24px", paddingTop: "40px" }}>
          <div style={{ display: "inline-flex", alignItems: "center", gap: "8px", padding: "6px 18px 6px 8px", borderRadius: "50px", background: "rgba(16,185,129,0.1)", border: "1px solid rgba(16,185,129,0.25)", marginBottom: "32px" }}>
            <div style={{ background: "#10b981", borderRadius: "50px", padding: "4px 10px", fontSize: "11px", fontWeight: "800", color: "white" }}>LIVE</div>
            <span style={{ fontSize: "13px", color: "#34d399", fontWeight: "600" }}>5-Service Distributed Platform on AWS EC2</span>
          </div>

          <h1 style={{ fontSize: "clamp(42px, 7vw, 78px)", fontWeight: "900", lineHeight: 1.05, letterSpacing: "-0.04em", marginBottom: "24px" }}>
            <span style={{ color: "#f1f5f9" }}>The Future of</span><br />
            <span style={{ background: "linear-gradient(135deg, #10b981 0%, #34d399 50%, #6ee7b7 100%)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>AI-Powered Payments</span>
          </h1>

          <p style={{ fontSize: "18px", color: "#94a3b8", maxWidth: "650px", margin: "0 auto 40px", lineHeight: "1.7" }}>
            Enterprise UPI platform with <strong style={{ color: "#e2e8f0" }}>Gemini AI Money Assistant</strong>,{" "}
            <strong style={{ color: "#e2e8f0" }}>pgvector Scam Scanner</strong>, real-time Kafka SSE,
            and AES-256 token vault on AWS.
          </p>

          <div style={{ display: "flex", gap: "16px", justifyContent: "center", flexWrap: "wrap" }}>
            <SignInButton mode="modal">
              <button style={{ display: "inline-flex", alignItems: "center", gap: "10px", background: "linear-gradient(135deg, #10b981, #059669)", color: "white", border: "none", borderRadius: "50px", padding: "16px 36px", fontSize: "16px", fontWeight: "700", cursor: "pointer", fontFamily: "'Outfit', sans-serif", boxShadow: "0 0 30px rgba(16,185,129,0.4)", transition: "all 0.3s ease" }}>
                Launch Dashboard <ArrowRight size={18} />
              </button>
            </SignInButton>
            <a href="https://github.com/pavansaiambala7/safepe" target="_blank" rel="noopener noreferrer"
              style={{ display: "inline-flex", alignItems: "center", gap: "10px", border: "1px solid rgba(255,255,255,0.12)", borderRadius: "50px", padding: "16px 30px", fontSize: "15px", fontWeight: "600", color: "#cbd5e1", textDecoration: "none", background: "rgba(255,255,255,0.04)", transition: "all 0.3s ease" }}>
              <GitFork size={18} /> GitHub
            </a>
          </div>

          <div style={{ display: "flex", justifyContent: "center", gap: "24px", flexWrap: "wrap", marginTop: "56px" }}>
            {services.map((s) => (
              <div key={s.label} style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "50%", background: s.dot, boxShadow: "0 0 8px " + s.dot }} />
                <span style={{ fontSize: "12px", color: "#64748b", fontWeight: "600" }}>{s.label}</span>
                <span style={{ fontSize: "11px", color: "#334155", fontFamily: "monospace" }}>{s.sub}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section style={{ maxWidth: "1100px", margin: "0 auto", padding: "80px 24px" }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "20px" }}>
          <StatBadge value={92} suffix="%" label="AI Fraud Detection Accuracy" color="#10b981" />
          <StatBadge value={480} suffix="ms" label="Redis Vector Cache Latency" color="#8b5cf6" />
          <StatBadge value={1000} suffix="+" label="Encrypted Vault Accounts" color="#3b82f6" />
          <StatBadge value={5} label="Distributed Microservices" color="#f59e0b" />
          <StatBadge value={3} label="LangChain4j AI Agents" color="#ec4899" />
        </div>
      </section>

      <div style={{ height: "1px", background: "linear-gradient(90deg, transparent, rgba(16,185,129,0.3), transparent)", margin: "0 24px" }} />

      <section style={{ maxWidth: "1100px", margin: "0 auto", padding: "80px 24px" }}>
        <div style={{ textAlign: "center", marginBottom: "56px" }}>
          <div style={{ fontSize: "11px", fontWeight: "800", color: "#10b981", letterSpacing: "2px", textTransform: "uppercase", marginBottom: "14px" }}>Enterprise Capabilities</div>
          <h2 style={{ fontSize: "clamp(28px, 4vw, 44px)", fontWeight: "900", color: "#f1f5f9", letterSpacing: "-0.03em", marginBottom: "14px" }}>Built for Production. Designed for Scale.</h2>
          <p style={{ color: "#64748b", fontSize: "16px", maxWidth: "560px", margin: "0 auto" }}>Every component is engineered with mathematical precision using industry-standard distributed systems patterns.</p>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: "20px" }}>
          <FeatureCard icon={<Cpu size={26} color="#10b981" />} color="#10b981" label="AI Engine" title="RAG Money Assistant" desc="Gemini-powered chatbot grounded in your real transaction history. Uses LangChain4j retrieval to answer spending questions with pinpoint accuracy." delay="0s" />
          <FeatureCard icon={<Search size={26} color="#8b5cf6" />} color="#8b5cf6" label="Fraud Detection" title="pgvector Scam Scanner" desc="Cosine similarity search over 1,000+ fraud embeddings. Patterns 60pct+ match triggers Risk 75pct+ auto BLOCK with trust score reasoning." delay="0.1s" />
          <FeatureCard icon={<Zap size={26} color="#f59e0b" />} color="#f59e0b" label="Payments" title="Razorpay UPI + QR Vault" desc="Full Razorpay order lifecycle, dynamic UPI QR generation, HMAC signature verification, and AES-256 GCM encrypted card tokenization." delay="0.2s" />
          <FeatureCard icon={<Bell size={26} color="#ec4899" />} color="#ec4899" label="Real-time" title="Kafka SSE Notification Hub" desc="@Scheduled bill sweeper publishes BillReminderEvents to Kafka. Streams them via SSE with Web Audio API chimes directly to the UI bell." delay="0.3s" />
          <FeatureCard icon={<Shield size={26} color="#3b82f6" />} color="#3b82f6" label="Gateway" title="Clerk JWT + Rate Limiter" desc="Spring Cloud Gateway validates Clerk JWKS signatures stateless. Bucket4j token-bucket enforces 100 req/min per IP with global CORS." delay="0.4s" />
          <FeatureCard icon={<Database size={26} color="#06b6d4" />} color="#06b6d4" label="Data Layer" title="pgvector + Redis + Eureka" desc="Database-per-service isolation. Redis cuts vector query latency 40pct from 800ms to 480ms. Eureka provides dynamic service discovery." delay="0.5s" />
        </div>
      </section>

      <div style={{ height: "1px", background: "linear-gradient(90deg, transparent, rgba(139,92,246,0.3), transparent)", margin: "0 24px" }} />

      <section style={{ maxWidth: "1100px", margin: "0 auto", padding: "80px 24px" }}>
        <div style={{ textAlign: "center", marginBottom: "48px" }}>
          <div style={{ fontSize: "11px", fontWeight: "800", color: "#8b5cf6", letterSpacing: "2px", textTransform: "uppercase", marginBottom: "14px" }}>Event-Driven Architecture</div>
          <h2 style={{ fontSize: "clamp(26px, 4vw, 40px)", fontWeight: "900", color: "#f1f5f9", letterSpacing: "-0.03em" }}>CQRS Payment Flow End-to-End</h2>
        </div>
        <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: "24px", padding: "40px 32px", backdropFilter: "blur(10px)" }}>
          <div style={{ display: "flex", alignItems: "center", flexWrap: "wrap", justifyContent: "center", gap: "8px" }}>
            {[
              { icon: <CreditCard size={24} color="#10b981" />, label: "React UI", sub: "Checkout", color: "#10b981" },
              { icon: <Server size={24} color="#3b82f6" />, label: "API Gateway", sub: ":8080 JWT", color: "#3b82f6" },
              { icon: <Zap size={24} color="#f59e0b" />, label: "Payment Svc", sub: "Razorpay", color: "#f59e0b" },
              { icon: <Radio size={24} color="#ec4899" />, label: "Kafka KRaft", sub: "tx-events", color: "#ec4899" },
            ].map((step, i) => (
              <div key={step.label} style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "8px" }}>
                  <div style={{ width: "56px", height: "56px", borderRadius: "14px", background: step.color + "15", border: "1.5px solid " + step.color + "40", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "0 0 20px " + step.color + "20" }}>{step.icon}</div>
                  <div style={{ textAlign: "center" }}>
                    <div style={{ fontSize: "12px", fontWeight: "700", color: "#e2e8f0" }}>{step.label}</div>
                    <div style={{ fontSize: "10px", color: "#64748b" }}>{step.sub}</div>
                  </div>
                </div>
                {i < 3 && <ChevronRight size={18} color="#334155" />}
              </div>
            ))}
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <ChevronRight size={18} color="#334155" />
              <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "6px" }}>
                  <div style={{ width: "52px", height: "52px", borderRadius: "14px", background: "#8b5cf615", border: "1.5px solid #8b5cf640", display: "flex", alignItems: "center", justifyContent: "center" }}><Cpu size={22} color="#8b5cf6" /></div>
                  <div style={{ fontSize: "11px", fontWeight: "700", color: "#e2e8f0", textAlign: "center" }}>AI Service<br /><span style={{ fontSize: "10px", color: "#64748b", fontWeight: "400" }}>RAG Model</span></div>
                </div>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "6px" }}>
                  <div style={{ width: "52px", height: "52px", borderRadius: "14px", background: "#10b98115", border: "1.5px solid #10b98140", display: "flex", alignItems: "center", justifyContent: "center" }}><Bell size={22} color="#10b981" /></div>
                  <div style={{ fontSize: "11px", fontWeight: "700", color: "#e2e8f0", textAlign: "center" }}>Notif Svc<br /><span style={{ fontSize: "10px", color: "#64748b", fontWeight: "400" }}>SSE Push</span></div>
                </div>
              </div>
            </div>
          </div>

          <div style={{ marginTop: "40px", borderTop: "1px solid rgba(255,255,255,0.06)", paddingTop: "32px" }}>
            <div style={{ fontSize: "12px", color: "#64748b", fontWeight: "700", marginBottom: "16px", textTransform: "uppercase", letterSpacing: "1px" }}>Vector Query Latency - Redis Cache Impact</div>
            <div style={{ display: "grid", gap: "12px" }}>
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: "12px", marginBottom: "6px" }}>
                  <span style={{ color: "#64748b" }}>Standard pgvector Query</span>
                  <span style={{ color: "#ef4444", fontWeight: "800" }}>800 ms</span>
                </div>
                <div style={{ height: "8px", background: "rgba(255,255,255,0.05)", borderRadius: "4px", overflow: "hidden" }}>
                  <div style={{ width: "100%", height: "100%", background: "linear-gradient(90deg, #ef4444, #dc2626)", borderRadius: "4px" }} />
                </div>
              </div>
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: "12px", marginBottom: "6px" }}>
                  <span style={{ color: "#10b981", fontWeight: "700" }}>Redis 7 In-Memory Cache</span>
                  <span style={{ color: "#10b981", fontWeight: "800" }}>480 ms - 40% faster</span>
                </div>
                <div style={{ height: "8px", background: "rgba(255,255,255,0.05)", borderRadius: "4px", overflow: "hidden" }}>
                  <div style={{ width: "60%", height: "100%", background: "linear-gradient(90deg, #10b981, #059669)", borderRadius: "4px" }} />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section style={{ maxWidth: "1100px", margin: "0 auto", padding: "0 24px 80px" }}>
        <div style={{ background: "linear-gradient(135deg, rgba(16,185,129,0.08) 0%, rgba(16,185,129,0.02) 100%)", border: "1px solid rgba(16,185,129,0.2)", borderRadius: "24px", padding: "40px", display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "32px", alignItems: "center" }}>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
              <Lock size={20} color="#10b981" />
              <span style={{ fontSize: "11px", fontWeight: "800", color: "#10b981", textTransform: "uppercase", letterSpacing: "1.5px" }}>Security Architecture</span>
            </div>
            <h3 style={{ fontSize: "24px", fontWeight: "800", color: "#f1f5f9", letterSpacing: "-0.02em", marginBottom: "10px" }}>Zero-Trust. Encrypted End-to-End.</h3>
            <p style={{ fontSize: "14px", color: "#64748b", lineHeight: "1.7" }}>Every transaction is protected by multiple independent security layers.</p>
          </div>
          {securityItems.map((item) => (
            <div key={item.title} style={{ display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <div style={{ flexShrink: 0, marginTop: "2px" }}>{item.icon}</div>
              <div>
                <div style={{ fontSize: "14px", fontWeight: "700", color: "#e2e8f0", marginBottom: "4px" }}>{item.title}</div>
                <div style={{ fontSize: "12px", color: "#64748b", lineHeight: "1.5" }}>{item.desc}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section style={{ maxWidth: "900px", margin: "0 auto", padding: "0 24px 100px" }}>
        <div style={{ background: "linear-gradient(135deg, #0d2818 0%, #0a1628 100%)", border: "1px solid rgba(16,185,129,0.25)", borderRadius: "28px", padding: "64px 40px", textAlign: "center", boxShadow: "0 0 80px rgba(16,185,129,0.1)" }}>
          <div style={{ fontSize: "52px", marginBottom: "16px" }}>🛡️</div>
          <h2 style={{ fontSize: "clamp(26px, 4vw, 42px)", fontWeight: "900", color: "#f1f5f9", letterSpacing: "-0.03em", marginBottom: "16px" }}>Ready to Explore SafePe?</h2>
          <p style={{ color: "#64748b", fontSize: "16px", maxWidth: "480px", margin: "0 auto 36px", lineHeight: "1.7" }}>Sign in to access the full dashboard with real-time fraud scanner, AI chatbot, payment flows, and notification center.</p>
          <div style={{ display: "flex", gap: "16px", justifyContent: "center", flexWrap: "wrap" }}>
            <SignInButton mode="modal">
              <button style={{ display: "inline-flex", alignItems: "center", gap: "10px", background: "linear-gradient(135deg, #10b981, #059669)", color: "white", border: "none", borderRadius: "50px", padding: "16px 40px", fontSize: "16px", fontWeight: "700", cursor: "pointer", fontFamily: "'Outfit', sans-serif", boxShadow: "0 0 40px rgba(16,185,129,0.5)", transition: "all 0.3s ease" }}>
                Get Started Free <ArrowRight size={18} />
              </button>
            </SignInButton>
            <a href="http://13.60.235.28:8761" target="_blank" rel="noopener noreferrer"
              style={{ display: "inline-flex", alignItems: "center", gap: "8px", border: "1px solid rgba(255,255,255,0.1)", borderRadius: "50px", padding: "16px 28px", fontSize: "14px", fontWeight: "600", color: "#94a3b8", textDecoration: "none", transition: "all 0.3s ease" }}>
              <Activity size={16} /> Eureka Registry
            </a>
          </div>
        </div>
      </section>

      <footer style={{ borderTop: "1px solid rgba(255,255,255,0.06)", padding: "32px 24px", textAlign: "center" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "10px", marginBottom: "10px" }}>
          <div style={{ width: "32px", height: "32px", background: "linear-gradient(135deg,#10b981,#059669)", borderRadius: "8px", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Shield size={18} color="white" />
          </div>
          <span style={{ fontSize: "18px", fontWeight: "800", background: "linear-gradient(135deg,#10b981,#34d399)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>SafePe</span>
        </div>
        <p style={{ color: "#334155", fontSize: "13px", marginBottom: "6px" }}>
          Designed by{" "}
          <a href="https://github.com/pavansaiambala7" target="_blank" rel="noopener noreferrer" style={{ color: "#10b981", fontWeight: "700", textDecoration: "none" }}>Pavan Sai Ambala</a>
        </p>
        <p style={{ color: "#1e293b", fontSize: "11px" }}>Spring Boot 3.2 - React 19 - Kafka KRaft - pgvector - Redis 7 - AES-256 - AWS EC2</p>
      </footer>

      <style>{`
        @keyframes sfadeUp {
          from { opacity: 0; transform: translateY(28px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}
