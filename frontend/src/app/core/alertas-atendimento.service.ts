import { Injectable, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { RealtimeService } from './realtime.service';
import { AlertaAtendimentoWsPayload } from '../models/api.models';

const ALARME_DURACAO_MS = 30_000;
/** Entre um “ping” e outro — estilo notificação suave (tipo Teams), sem martelar a cada segundo. */
const ALARME_INTERVALO_MS = 2_450;

@Injectable({ providedIn: 'root' })
export class AlertasAtendimentoService {
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthService);

  private audioCtx: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private alarmeIntervalId: ReturnType<typeof setInterval> | null = null;
  private alarmeTimeoutId: ReturnType<typeof setTimeout> | null = null;

  /** Fila local (por aba) — mobile → PC via WebSocket/STOMP. */
  readonly fila = signal<AlertaAtendimentoWsPayload[]>([]);
  readonly processandoId = signal<string | null>(null);
  /** Enquanto o ciclo de som (~30s) está ativo — para piscar a caixinha da mesa na tela. */
  readonly alarmeAtivo = signal(false);

  /** Emite cada novo alerta assim que chega — usado pelo panel para auto-imprimir. */
  readonly novoAlerta$ = new Subject<AlertaAtendimentoWsPayload>();

  constructor() {
    // Singleton de app: sem teardown; o stream segue a sessão WebSocket.
    this.realtime.alertasAtendimento.subscribe((a) => {
      if (this.auth.usuario()?.perfil !== 'ATENDIMENTO') {
        return;
      }
      if ((a.tipo !== 'COMANDA_ENVIADA' && a.tipo !== 'MESA_ABERTA') || !a.alertaId) {
        return;
      }
      if (this.fila().some((x) => x.alertaId === a.alertaId)) {
        return;
      }
      this.fila.update((q) => [...q, a]);
      this.novoAlerta$.next(a);
      void this.tocarSomAlerta();
    });
  }

  private pararAlarme(): void {
    if (this.alarmeIntervalId != null) {
      clearInterval(this.alarmeIntervalId);
      this.alarmeIntervalId = null;
    }
    if (this.alarmeTimeoutId != null) {
      clearTimeout(this.alarmeTimeoutId);
      this.alarmeTimeoutId = null;
    }
    this.alarmeAtivo.set(false);
  }

  /**
   * Som estilo “notificação de mensagem” (dois tons suaves em senoide, inspiração no ping do Teams —
   * não é o arquivo original da Microsoft). Repete por ~30s com intervalo largo para não irritar.
   */
  private async tocarSomAlerta(): Promise<void> {
    if (typeof window === 'undefined') {
      return;
    }
    try {
      if (!this.audioCtx) {
        const Ctor =
          window.AudioContext ||
          (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
        if (!Ctor) {
          return;
        }
        this.audioCtx = new Ctor();
        this.masterGain = this.audioCtx.createGain();
        /** Volume de saída — ambiente com música ambiente precisa cortar mais. */
        this.masterGain.gain.value = 1;
        this.masterGain.connect(this.audioCtx.destination);
      }
      const ctx = this.audioCtx;
      const out = this.masterGain ?? ctx.destination;
      if (this.masterGain) {
        this.masterGain.gain.value = 1;
      }
      await ctx.resume();

      this.pararAlarme();

      /** Dois tons curtos (terça maior), som “de mensagem”; senoide + decay suave. */
      this.alarmeAtivo.set(true);
      const sequenciaChimeTeamsLike = () => {
        const t0 = ctx.currentTime;
        const nivel = 0.55;
        const nota = (freq: number, offsetSec: number, durSec: number) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, t0 + offsetSec);
          g.gain.setValueAtTime(0, t0 + offsetSec);
          g.gain.linearRampToValueAtTime(nivel, t0 + offsetSec + 0.006);
          g.gain.exponentialRampToValueAtTime(0.001, t0 + offsetSec + durSec);
          osc.connect(g);
          g.connect(out);
          osc.start(t0 + offsetSec);
          osc.stop(t0 + offsetSec + durSec + 0.02);
        };
        // C5 → E5 (aprox. ping duplo de UI)
        nota(523.25, 0, 0.14);
        nota(659.25, 0.1, 0.18);
      };

      sequenciaChimeTeamsLike();
      this.alarmeIntervalId = setInterval(sequenciaChimeTeamsLike, ALARME_INTERVALO_MS);
      this.alarmeTimeoutId = setTimeout(() => this.pararAlarme(), ALARME_DURACAO_MS);
    } catch {
      this.alarmeAtivo.set(false);
      /* áudio bloqueado ou indisponível */
    }
  }

  ignorar(alertaId: string): void {
    this.fila.update((q) => {
      const next = q.filter((x) => x.alertaId !== alertaId);
      if (next.length === 0) {
        this.pararAlarme();
      }
      return next;
    });
  }

  remover(alertaId: string): void {
    this.ignorar(alertaId);
  }

  inicioProcessar(alertaId: string | null): void {
    this.processandoId.set(alertaId);
  }
}
