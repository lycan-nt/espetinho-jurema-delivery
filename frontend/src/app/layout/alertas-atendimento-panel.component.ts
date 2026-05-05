import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { imprimirTextoTerminalBrowser } from '../core/impressao-browser.util';
import { AlertasAtendimentoService } from '../core/alertas-atendimento.service';
import { ApiBackendService } from '../core/api-backend.service';
import { AuthService } from '../core/auth.service';

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

  /** Status de impressão por alertaId: imprimindo | impresso | erro */
  readonly statusMap = signal<Record<string, StatusImpressao>>({});

  private sub?: Subscription;

  visivel(): boolean {
    return this.auth.usuario()?.perfil === 'ATENDIMENTO';
  }

  ngOnInit(): void {
    // Ao chegar novo alerta, dispara impressão automaticamente
    this.sub = this.alertas.novoAlerta$.subscribe((a) => {
      this.imprimir(a.alertaId);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  statusDe(alertaId: string): StatusImpressao {
    return this.statusMap()[alertaId] ?? 'imprimindo';
  }

  /** Fecha/descarta o card manualmente. */
  fechar(alertaId: string): void {
    this.alertas.remover(alertaId);
    this.statusMap.update((m) => {
      const next = { ...m };
      delete next[alertaId];
      return next;
    });
  }

  /** Reimprime manualmente em caso de erro. */
  reimprimir(alertaId: string): void {
    this.imprimir(alertaId);
  }

  private imprimir(alertaId: string): void {
    this.setStatus(alertaId, 'imprimindo');
    this.api.reconhecerAlertaAtendimento(alertaId).subscribe({
      next: (r) => {
        if (!r.impressoServidor) {
          imprimirTextoTerminalBrowser(r.textoComanda, 'Comanda cozinha');
        }
        this.setStatus(alertaId, 'impresso');
        // Remove o card automaticamente após 5s
        setTimeout(() => this.fechar(alertaId), 5_000);
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
