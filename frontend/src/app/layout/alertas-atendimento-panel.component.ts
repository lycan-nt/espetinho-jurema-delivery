import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { imprimirTextoTerminalBrowser } from '../core/impressao-browser.util';
import { AlertasAtendimentoService } from '../core/alertas-atendimento.service';
import { ApiBackendService } from '../core/api-backend.service';
import { AuthService } from '../core/auth.service';
import { AlertaAtendimentoWsPayload } from '../models/api.models';

type StatusImpressao = 'imprimindo' | 'impresso' | 'aguardando-retry' | 'erro';

const MAX_TENTATIVAS = 3;
const DELAY_RETRY_S = 5;

interface EstadoAlerta {
  status: StatusImpressao;
  tentativas: number;
  contagem: number | null;
  intervalo?: ReturnType<typeof setInterval>;
}

@Component({
  selector: 'app-alertas-atendimento-panel',
  imports: [],
  templateUrl: './alertas-atendimento-panel.component.html',
  styleUrl: './alertas-atendimento-panel.component.scss',
})
export class AlertasAtendimentoPanelComponent implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  readonly alertas = inject(AlertasAtendimentoService);
  private readonly api = inject(ApiBackendService);

  /** Estado de impressão por alertaId (status, tentativas, contagem regressiva). */
  readonly estadoMap = signal<Record<string, EstadoAlerta>>({});

  /** Timers de contagem regressiva — mantidos fora do signal para não serializar handles. */
  private readonly timers = new Map<string, ReturnType<typeof setInterval>>();

  private sub?: Subscription;

  visivel(): boolean {
    return this.auth.usuario()?.perfil === 'ATENDIMENTO';
  }

  ehSolicitaFechamento(a: AlertaAtendimentoWsPayload): boolean {
    return a.tipo === 'SOLICITACAO_FECHAMENTO_COMANDA';
  }

  mensagemTituloAlerta(a: AlertaAtendimentoWsPayload): string {
    return this.ehSolicitaFechamento(a)
      ? 'Solicitação: fechar comanda'
      : 'Comanda para a cozinha';
  }

  mensagemAjudaAlerta(a: AlertaAtendimentoWsPayload): string {
    return this.ehSolicitaFechamento(a)
      ? 'Pedido pra fechar a comanda (churrasqueiro). Ao confirmar, imprime comanda igual ao fluxo habitual.'
      : '';
  }

  statusDe(alertaId: string): StatusImpressao {
    return this.estadoMap()[alertaId]?.status ?? 'imprimindo';
  }

  contagemDe(alertaId: string): number | null {
    return this.estadoMap()[alertaId]?.contagem ?? null;
  }

  ngOnInit(): void {
    this.sub = this.alertas.novoAlerta$.subscribe((a) => {
      if (!this.ehSolicitaFechamento(a)) {
        this.imprimirAuto(a.alertaId);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.limparTodosTimers();
  }

  /** Confirmação manual — usado apenas para SOLICITACAO_FECHAMENTO_COMANDA. */
  ok(alertaId: string): void {
    this.alertas.inicioProcessar(alertaId);
    this.api.reconhecerAlertaAtendimento(alertaId).subscribe({
      next: (r) => {
        const texto = r.textoComanda?.trim() ?? '';
        if (texto && !r.impressoServidor) {
          imprimirTextoTerminalBrowser(texto, 'Comanda cozinha');
        }
        this.alertas.inicioProcessar(null);
        this.alertas.ignorar(alertaId);
      },
      error: () => {
        this.alertas.inicioProcessar(null);
      },
    });
  }

  /** Fecha o card e cancela timers pendentes. */
  fechar(alertaId: string): void {
    this.cancelarTimer(alertaId);
    this.alertas.remover(alertaId);
    this.estadoMap.update((m) => {
      const next = { ...m };
      delete next[alertaId];
      return next;
    });
  }

  /** Retry manual — reseta tentativas e começa do zero. */
  reimprimir(alertaId: string): void {
    this.cancelarTimer(alertaId);
    this.setEstado(alertaId, { status: 'imprimindo', tentativas: 0, contagem: null });
    this.imprimirAuto(alertaId);
  }

  ignorar(alertaId: string): void {
    this.cancelarTimer(alertaId);
    this.alertas.ignorar(alertaId);
  }

  private imprimirAuto(alertaId: string): void {
    const tentativaAtual = (this.estadoMap()[alertaId]?.tentativas ?? 0) + 1;
    this.setEstado(alertaId, { status: 'imprimindo', tentativas: tentativaAtual, contagem: null });

    this.api.reconhecerAlertaAtendimento(alertaId).subscribe({
      next: (r) => {
        const texto = r.textoComanda?.trim() ?? '';
        if (texto && !r.impressoServidor) {
          imprimirTextoTerminalBrowser(texto, 'Comanda cozinha');
        }
        this.setEstado(alertaId, { status: 'impresso', tentativas: tentativaAtual, contagem: null });
      },
      error: () => {
        if (tentativaAtual < MAX_TENTATIVAS) {
          this.iniciarContagemRegressiva(alertaId, tentativaAtual);
        } else {
          this.setEstado(alertaId, { status: 'erro', tentativas: tentativaAtual, contagem: null });
        }
      },
    });
  }

  private iniciarContagemRegressiva(alertaId: string, tentativasFeitas: number): void {
    this.cancelarTimer(alertaId);
    this.setEstado(alertaId, { status: 'aguardando-retry', tentativas: tentativasFeitas, contagem: DELAY_RETRY_S });

    const intervalo = setInterval(() => {
      const estado = this.estadoMap()[alertaId];
      if (!estado || estado.status !== 'aguardando-retry') {
        this.cancelarTimer(alertaId);
        return;
      }
      const novaContagem = (estado.contagem ?? 1) - 1;
      if (novaContagem <= 0) {
        this.cancelarTimer(alertaId);
        this.imprimirAuto(alertaId);
      } else {
        this.setEstado(alertaId, { ...estado, contagem: novaContagem });
      }
    }, 1000);

    this.timers.set(alertaId, intervalo);
  }

  private cancelarTimer(alertaId: string): void {
    const t = this.timers.get(alertaId);
    if (t != null) {
      clearInterval(t);
      this.timers.delete(alertaId);
    }
  }

  private limparTodosTimers(): void {
    this.timers.forEach((t) => clearInterval(t));
    this.timers.clear();
  }

  private setEstado(alertaId: string, estado: EstadoAlerta): void {
    this.estadoMap.update((m) => ({ ...m, [alertaId]: estado }));
  }
}
