import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NgClass } from '@angular/common';
import { filter } from 'rxjs/operators';
import { RealtimeService } from '../core/realtime.service';
import { ApiBackendService } from '../core/api-backend.service';
import { AuthService } from '../core/auth.service';
import { APP_VERSION_LABEL } from '../core/app-version';
import { AlertasAtendimentoPanelComponent } from './alertas-atendimento-panel.component';

const CONFIG_ROUTES = [
  '/config/impressao',
  '/config/backup',
  '/config/empresa',
  '/cadastro/produtos',
  '/cadastro/usuarios',
  '/config/acesso-movel',
];

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgClass, AlertasAtendimentoPanelComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit, OnDestroy {
  readonly appVersionLabel = APP_VERSION_LABEL;
  readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly api = inject(ApiBackendService);
  private readonly router = inject(Router);

  relogio = '';
  relogioHora = '';
  relogioData = '';
  caixaAberto = false;
  navAberta = false;
  configAberto = false;
  private tick?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.relogioAtualizar();
    this.tick = setInterval(() => this.relogioAtualizar(), 30_000);
    this.realtime.conectar();
    this.api.getCaixaStatus().subscribe((s) => (this.caixaAberto = s.aberto));

    const atualizarConfig = (url: string) => {
      if (CONFIG_ROUTES.some((r) => url.startsWith(r))) {
        this.configAberto = true;
      }
    };
    atualizarConfig(this.router.url);
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe((e) => atualizarConfig((e as NavigationEnd).urlAfterRedirects));
  }

  toggleConfig(event: Event): void {
    event.stopPropagation();
    this.configAberto = !this.configAberto;
  }

  emRotaConfig(): boolean {
    return CONFIG_ROUTES.some((r) => this.router.url.startsWith(r));
  }

  ngOnDestroy(): void {
    if (this.tick) {
      clearInterval(this.tick);
    }
  }

  toggleNav(): void {
    this.navAberta = !this.navAberta;
  }

  fecharNavMobile(): void {
    this.navAberta = false;
  }

  private relogioAtualizar(): void {
    const agora = new Date();

    this.relogio = new Intl.DateTimeFormat('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(agora);

    this.relogioHora = new Intl.DateTimeFormat('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(agora);

    this.relogioData = new Intl.DateTimeFormat('pt-BR', {
      weekday: 'short',
      day: '2-digit',
      month: '2-digit',
    }).format(agora);
  }
}
