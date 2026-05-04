import { Component, inject } from '@angular/core';
import { imprimirTextoTerminalBrowser } from '../core/impressao-browser.util';
import { AlertasAtendimentoService } from '../core/alertas-atendimento.service';
import { ApiBackendService } from '../core/api-backend.service';
import { AuthService } from '../core/auth.service';

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

  ok(alertaId: string): void {
    this.alertas.inicioProcessar(alertaId);
    this.api.reconhecerAlertaAtendimento(alertaId).subscribe({
      next: (r) => {
        if (!r.impressoServidor) {
          this.imprimirTexto(r.textoComanda);
        }
        this.alertas.remover(alertaId);
        this.alertas.inicioProcessar(null);
      },
      error: () => {
        this.alertas.inicioProcessar(null);
        window.alert(
          'Não foi possível confirmar o alerta. Confirme que você está logado como atendimento e tente de novo.',
        );
      },
    });
  }

  ignorar(alertaId: string): void {
    this.alertas.ignorar(alertaId);
  }

  private imprimirTexto(texto: string): void {
    imprimirTextoTerminalBrowser(texto, 'Comanda cozinha');
  }
}
