/**
 * SafePe Audio Alert Engine
 * =========================
 * Synthesizes crisp, high-fidelity acoustic notification chimes
 * using the browser's native Web Audio API (Zero external mp3/wav files needed).
 *
 * Sounds:
 *   SUCCESS          → Bright two-tone major interval bell chime (D5 -> A5)
 *   FRAUD_ALERT      → Urgent dual-tone descending warning siren
 *   ESCROW_REFUND    → Reassuring ascending 3-note chord (C5 -> E5 -> G5)
 *   REFUND_INITIATED → Gentle processing ping (two soft tones)
 *   SECURITY         → Single clean high-pitched ding
 *
 * + Realistic metallic bell overtones on the Success chime for
 *   a real bell sound rather than a plain sine wave.
 */

class AudioAlertEngine {
  private audioCtx: AudioContext | null = null;
  private isSoundEnabled: boolean = true;

  constructor() {
    const saved = localStorage.getItem('safepe_sound_enabled');
    this.isSoundEnabled = saved !== null ? saved === 'true' : true;
  }

  public isEnabled(): boolean {
    return this.isSoundEnabled;
  }

  public toggleSound(): boolean {
    this.isSoundEnabled = !this.isSoundEnabled;
    localStorage.setItem('safepe_sound_enabled', String(this.isSoundEnabled));
    return this.isSoundEnabled;
  }

  private getContext(): AudioContext {
    if (!this.audioCtx) {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      this.audioCtx = new AudioContextClass();
    }
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  /**
   * Helper: Creates a bell-like tone with harmonics for a realistic metallic sound.
   * Real bells produce a fundamental + several inharmonic partials that decay at
   * different rates — this simulates that with 4 overtone layers.
   */
  private playBellTone(frequency: number, startTime: number, duration: number, volume: number) {
    const ctx = this.getContext();

    // Bell partials: fundamental, minor third, perfect fifth, octave
    // Real bells have slightly detuned partials — these ratios approximate that
    const partials = [
      { ratio: 1.0,   gain: volume,          type: 'sine' as OscillatorType },
      { ratio: 2.0,   gain: volume * 0.55,   type: 'sine' as OscillatorType },
      { ratio: 3.0,   gain: volume * 0.25,   type: 'sine' as OscillatorType },
      { ratio: 4.12,  gain: volume * 0.15,   type: 'sine' as OscillatorType },  // Slightly detuned
      { ratio: 5.43,  gain: volume * 0.08,   type: 'triangle' as OscillatorType }, // High shimmer
    ];

    partials.forEach(partial => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = partial.type;
      osc.frequency.setValueAtTime(frequency * partial.ratio, startTime);

      gain.gain.setValueAtTime(partial.gain, startTime);
      // Higher partials decay faster — like a real bell
      const decayTime = duration / (1 + partial.ratio * 0.3);
      gain.gain.exponentialRampToValueAtTime(0.001, startTime + decayTime);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(startTime);
      osc.stop(startTime + decayTime + 0.05);
    });
  }

  /**
   * Payment Success — Realistic Bell Chime
   * Two-strike bell: D5 then A5 with metallic overtones
   */
  public playSuccess() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      // Strike 1: D5 (587.33 Hz) — warm fundamental
      this.playBellTone(587.33, now, 0.6, 0.14);

      // Strike 2: A5 (880 Hz) — bright resolution
      this.playBellTone(880.00, now + 0.15, 0.8, 0.16);
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * Fraud Alert — Urgent Warning Siren
   * Descending dual triangle-wave pulses that demand attention
   */
  public playFraudAlert() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      // Pulse 1: High descending sweep
      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.type = 'triangle';
      osc1.frequency.setValueAtTime(880, now);
      osc1.frequency.exponentialRampToValueAtTime(440, now + 0.18);
      gain1.gain.setValueAtTime(0.28, now);
      gain1.gain.exponentialRampToValueAtTime(0.01, now + 0.18);
      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.start(now);
      osc1.stop(now + 0.2);

      // Pulse 2: Second sharp alert burst
      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'triangle';
      osc2.frequency.setValueAtTime(987.77, now + 0.22);
      osc2.frequency.exponentialRampToValueAtTime(523.25, now + 0.42);
      gain2.gain.setValueAtTime(0.3, now + 0.22);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.start(now + 0.22);
      osc2.stop(now + 0.47);
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * Escrow Refund Completed — Reassuring Ascending Chord
   * C5 -> E5 -> G5 major triad resolving upward
   */
  public playRefundChime() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      const notes = [523.25, 659.25, 783.99]; // C5, E5, G5
      notes.forEach((freq, idx) => {
        const startTime = now + idx * 0.12;
        this.playBellTone(freq, startTime, 0.5, 0.12);
      });
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * Refund Initiated — Gentle Processing Ping
   * Two soft tones indicating something is in progress
   */
  public playRefundInitiated() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      // Tone 1: Soft mid-range ping
      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.type = 'sine';
      osc1.frequency.setValueAtTime(698.46, now); // F5
      gain1.gain.setValueAtTime(0.15, now);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.start(now);
      osc1.stop(now + 0.27);

      // Tone 2: Slightly higher confirmation ping
      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(783.99, now + 0.18); // G5
      gain2.gain.setValueAtTime(0.12, now + 0.18);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.42);
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.start(now + 0.18);
      osc2.stop(now + 0.44);
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * Security / Vault — Clean High Ding
   */
  public playSecurityChime() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      this.playBellTone(1046.5, now, 0.4, 0.1); // C6 with bell overtones
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * Play appropriate audio chime based on notification type
   */
  public playSoundForType(type: 'SUCCESS' | 'FRAUD_ALERT' | 'ESCROW_REFUND' | 'REFUND_INITIATED' | 'SECURITY') {
    switch (type) {
      case 'SUCCESS':
        this.playSuccess();
        break;
      case 'FRAUD_ALERT':
        this.playFraudAlert();
        break;
      case 'ESCROW_REFUND':
        this.playRefundChime();
        break;
      case 'REFUND_INITIATED':
        this.playRefundInitiated();
        break;
      case 'SECURITY':
        this.playSecurityChime();
        break;
    }
  }
}

export const audioAlerts = new AudioAlertEngine();
