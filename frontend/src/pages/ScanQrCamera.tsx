import { useEffect, useState } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { ShieldCheck, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function ScanQrCamera() {
  const [scanResult, setScanResult] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    // Only initialize scanner if we haven't got a result
    if (!scanResult) {
      const scanner = new Html5QrcodeScanner(
        "reader",
        { fps: 10, qrbox: { width: 250, height: 250 } },
        /* verbose= */ false
      );

      scanner.render(
        (decodedText) => {
          setScanResult(decodedText);
          scanner.clear();
        },
        (error) => {
          // parse errors are normal (e.g. no qr code currently in frame)
        }
      );

      return () => {
        scanner.clear().catch(e => console.error(e));
      };
    }
  }, [scanResult]);

  return (
    <div className="surface-panel animate-fade-up" style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '24px' }}>
        <div style={{ background: 'linear-gradient(135deg, #10b981, #059669)', padding: '12px', borderRadius: '12px' }}>
          <ShieldCheck size={32} color="white" />
        </div>
        <div>
          <h1 style={{ fontSize: '24px' }}>Scan Any QR</h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>Scan SafePe, PhonePe, or GPay QR codes</p>
        </div>
      </div>

      {!scanResult ? (
        <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '16px', border: '1px solid var(--color-border)' }}>
          <div id="reader" style={{ width: '100%' }}></div>
          <p style={{ marginTop: '16px', color: 'var(--color-text-secondary)' }}>Point your camera at a QR code to pay securely.</p>
        </div>
      ) : (
        <div style={{ padding: '40px 20px' }}>
          <CheckCircle size={64} className="status-safe" style={{ margin: '0 auto 24px auto' }} />
          <h2 style={{ color: 'var(--color-accent)', marginBottom: '16px' }}>QR Code Scanned!</h2>
          <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '12px', border: '1px solid var(--color-border)', wordBreak: 'break-all', marginBottom: '24px' }}>
            {scanResult}
          </div>
          
          {/* Usually we would parse the UPI intent and auto-fill checkout, but for demo: */}
          <button 
            className="btn-primary" 
            style={{ width: '100%' }}
            onClick={() => navigate('/pay', { state: { scannedUpiId: scanResult } })}
          >
            Proceed to Payment
          </button>
          
          <button 
            className="btn-primary" 
            style={{ width: '100%', marginTop: '16px', background: 'var(--color-bg-surface-hover)', color: 'var(--color-text-primary)' }}
            onClick={() => setScanResult(null)}
          >
            Scan Another Code
          </button>
        </div>
      )}
    </div>
  );
}
