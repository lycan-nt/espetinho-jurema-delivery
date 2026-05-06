import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { imprimirTextoTerminalBrowser } from '../core/impressao-browser.util';
import { AlertasAtendimentoService } from '../core/alertas-atendimento.service';
import { ApiBackendService } from '../core/api-backend.service';
import { AuthService } from '../core/auth.service';
import { AlertaAtendimentoWsPayload } from '../models/api.models';

type StatusImpressao = 'imprimindo' | 'impresso' | 'erro';

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

  /** Status de impressão automática por alertaId */
  readonly statusMap = signal<Record<string, StatusImpressao>>({});

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
    return this.statusMap()[alertaId] ?? 'imprimindo';
  }

  ngOnInit(): void {
    // Ao chegar novo alerta de comanda, dispara impressão automaticamente.
    // Solicitação de fechamento fica aguardando confirmação manual do atendente.
    this.sub = this.alertas.novoAlerta$.subscribe((a) => {
      if (!this.ehSolicitaFechamento(a)) {
        this.imprimirAuto(a.alertaId);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
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

  /** Fecha o card de comanda auto-impressa. */
  fechar(alertaId: string): void {
    this.alertas.remover(alertaId);
    this.statusMap.update((m) => {
      const next = { ...m };
      delete next[alertaId];
      return next;
    });
  }

  /** Reimprime em caso de falha. */
  reimprimir(alertaId: string): void {
    this.imprimirAuto(alertaId);
  }

  ignorar(alertaId: string): void {
    this.alertas.ignorar(alertaId);
  }

  private imprimirAuto(alertaId: string): void {
    this.setStatus(alertaId, 'imprimindo');
    this.api.reconhecerAlertaAtendimento(alertaId).subscribe({
      next: (r) => {
        const texto = r.textoComanda?.trim() ?? '';
        if (texto && !r.impressoServidor) {
          imprimirTextoTerminalBrowser(texto, 'Comanda cozinha');
        }
        this.setStatus(alertaId, 'impresso');
      },
      error: () => {
        this.setStatus(alertaId, 'erro');
      },
    });
  }

  private setStatus(alertaId: string, status: StatusImpressao): void {
    this.statusMap.update((m) => ({ ...m, [alertaId]: status }));
  }
}
