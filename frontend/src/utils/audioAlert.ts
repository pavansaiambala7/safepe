/**
 * SafePe Audio Alert Engine
 * =========================
 * Synthesizes crisp, high-fidelity acoustic notification chimes
 * using the browser's native Web Audio API (Zero external mp3/wav files needed).
 */

class AudioAlertEngine {
  private audioCtx: AudioContext | null = null;
  private isSoundEnabled: boolean = true;

  constructor() {
    // Check saved sound preference
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
   * 🟢 PhonePe Style Success Chime
   * Two-tone uplifting major interval (D5 -> A5)
   */
  public playSuccess() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      // Note 1: D5 (587.33 Hz)
      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.type = 'sine';
      osc1.frequency.setValueAtTime(587.33, now);
      gain1.gain.setValueAtTime(0.18, now);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.18);
      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.start(now);
      osc1.stop(now + 0.18);

      // Note 2: A5 (880.00 Hz) - crisp and bright
      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(880.00, now + 0.12);
      gain2.gain.setValueAtTime(0.22, now + 0.12);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.start(now + 0.12);
      osc2.stop(now + 0.45);
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * 🚨 Urgent Fraud Interception Warning Siren
   * Attention-grabbing dual-tone descending buzz (880Hz -> 440Hz -> 880Hz)
   */
  public playFraudAlert() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      // Tone 1: High warning pulse (sawtooth with low-pass)
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
      osc1.stop(now + 0.18);

      // Tone 2: Secondary sharp alert pulse
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
      osc2.stop(now + 0.45);
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * 🛡️ Escrow Refund Completed Chime
   * Reassuring 3-tone ascending chord (C5 -> E5 -> G5)
   */
  public playRefundChime() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;

      const notes = [523.25, 659.25, 783.99]; // C5, E5, G5
      notes.forEach((freq, idx) => {
        const startTime = now + idx * 0.1;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, startTime);
        gain.gain.setValueAtTime(0.18, startTime);
        gain.gain.exponentialRampToValueAtTime(0.001, startTime + 0.35);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(startTime);
        osc.stop(startTime + 0.35);
      });
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * 🔐 Security / Generic Ding
   */
  public playSecurityChime() {
    if (!this.isSoundEnabled) return;
    try {
      const ctx = this.getContext();
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(1046.5, now); // C6
      gain.gain.setValueAtTime(0.15, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.25);
    } catch (e) {
      console.warn('Audio alert unavailable:', e);
    }
  }

  /**
   * Play appropriate audio chime based on notification type
   */
  public playSoundForType(type: 'SUCCESS' | 'FRAUD_ALERT' | 'ESCROW_REFUND' | 'SECURITY') {
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
      case 'SECURITY':
        this.playSecurityChime();
        break;
    }
  }
}

export const audioAlerts = new AudioAlertEngine();
