import { Component, inject } from '@angular/core';
import { imprimirTextoTerminalBrowser } from '../core/impressao-browser.util';
import { AlertasAtendimentoService } from '../core/alertas-atendimento.service';
import { ApiBackendService } from '../core/api-backend.service';
import { AuthService } from '../core/auth.service';
import { AlertaAtendimentoWsPayload } from '../models/api.models';

@Component({
  selector: 'app-alertas-atendimento-panel',
  imports: [],
  templateUrl: './alertas-atendimento-panel.component.html',
  styleUrl: './alertas-atendimento-panel.component.scss',
})
export class AlertasAtendimentoPanelComponent {
  readonly auth = inject(AuthService);
  readonly alertas = inject(AlertasAtendimentoService);
  private readonly api = inject(ApiBackendService);

  visivel(): boolean {
    return this.auth.usuario()?.perfil === 'ATENDIMENTO';
  }

  ehSolicitaFechamento(a: AlertaAtendimentoWsPayload): boolean {
    return a.tipo === 'SOLICITACAO_FECHAMENTO_COMANDA';
  }

  rotuloPrimarioAlerta(): string {
    return 'Imprimir comanda';
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

  ignorar(alertaId: string): void {
    this.alertas.ignorar(alertaId);
  }
}
