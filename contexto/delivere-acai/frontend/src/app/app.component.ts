import { Component, OnInit, inject, effect } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { CaixaService } from './services/caixa.service';
import { CaixaStatus } from './models/caixa.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, FormsModule],
  template: `
    @if (authService.isLoggedIn()) {
      @if (caixaStatusLoading) {
        <div class="caixa-loading">
          <div class="spinner"></div>
          <p>Verificando caixa...</p>
        </div>
      } @else {
        <div class="layout">
          <header class="header">
            <a routerLink="/" class="logo" aria-label="Mix Açaí - Início">
              <img src="logo.png" alt="Mix Açaí" class="logo-img" />
              <span class="logo-text">Mix Açaí</span>
            </a>
            <nav class="nav">
              <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Início</a>
              <a routerLink="/comanda" routerLinkActive="active">Nova comanda</a>
              <a routerLink="/comandas" routerLinkActive="active">Comandas</a>
              @if (authService.hasGestao()) {
                <a routerLink="/gestao" routerLinkActive="active">Gestão</a>
              }
              @if (caixaAberto) {
                <button type="button" class="btn-fechar-caixa" (click)="confirmarFecharCaixa()">Fechar caixa</button>
              }
              <span class="nav-user">{{ authService.username() }}</span>
              <button type="button" class="btn-logout" (click)="authService.logout()">Sair</button>
            </nav>
          </header>
          <main class="main">
            <router-outlet />
          </main>
          <span class="app-version">v1.0.0</span>
        </div>
      }
      @if (showFecharCaixaConfirm) {
        <div class="modal-overlay" (click)="showFecharCaixaConfirm = false">
          <div class="modal-fechar-caixa" (click)="$event.stopPropagation()">
            <h3>Fechar caixa</h3>
            <p>Informe o valor de fechamento (conferência do caixa). O relatório final será enviado para a planilha do dia.</p>
            <div class="form-group-fechar">
              <label for="valorFechamento">Valor de fechamento (R$)</label>
              <input
                id="valorFechamento"
                type="number"
                step="0.01"
                min="0"
                [(ngModel)]="valorFechamento"
                name="valorFechamento"
                class="input-valor-fechar"
                placeholder="0,00"
              />
              <label for="valorRetirada">Valor de retirada (R$)</label>
              <input
                id="valorRetirada"
                type="number"
                step="0.01"
                min="0"
                [(ngModel)]="valorRetirada"
                name="valorRetirada"
                class="input-valor-fechar"
                placeholder="0,00"
              />
              <span class="field-hint-fechar">Deixe 0 ou em branco se não houve retirada de caixa.</span>
              @if (fecharCaixaError) {
                <p class="error-msg">{{ fecharCaixaError }}</p>
              }
            </div>
            <div class="modal-buttons">
              <button type="button" class="btn-cancel" (click)="showFecharCaixaConfirm = false">Cancelar</button>
              <button type="button" class="btn-confirm" (click)="fecharCaixa()" [disabled]="fecharCaixaLoading">
                {{ fecharCaixaLoading ? 'Fechando...' : 'Fechar caixa' }}
              </button>
            </div>
          </div>
        </div>
      }
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .layout {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    .header {
      background: var(--acai-800);
      color: white;
      padding: 0.75rem 1.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      box-shadow: 0 2px 12px rgba(45, 27, 46, 0.25);
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      font-weight: 700;
      font-size: 1.2rem;
      color: inherit;
      text-decoration: none;
      transition: opacity 0.2s;
    }
    .logo:hover {
      opacity: 0.95;
    }
    .logo-img {
      height: 40px;
      width: auto;
      max-width: 120px;
      object-fit: contain;
      display: block;
      border-radius: 8px;
    }
    @media (max-width: 480px) {
      .logo-img { height: 36px; }
      .logo-text { font-size: 1.05rem; }
    }
    .logo-text {
      letter-spacing: -0.02em;
    }
    .nav {
      display: flex;
      gap: 1rem;
    }
    .nav a {
      color: var(--acai-200);
      text-decoration: none;
      padding: 0.5rem 0.75rem;
      border-radius: 8px;
      font-weight: 500;
      transition: background 0.2s, color 0.2s;
    }
    .nav a:hover {
      background: rgba(255,255,255,0.1);
      color: white;
    }
    .nav a.active {
      background: var(--acai-500);
      color: white;
    }
    .nav-user {
      font-size: 0.9rem;
      color: var(--acai-200);
      margin-left: 0.5rem;
      padding: 0.5rem 0.25rem 0 0;
    }
    .btn-logout {
      background: rgba(255,255,255,0.15);
      color: white;
      border: 1px solid rgba(255,255,255,0.4);
      padding: 0.45rem 0.85rem;
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-logout:hover {
      background: rgba(255,255,255,0.25);
    }
    .main {
      flex: 1;
      padding: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
      width: 100%;
    }
    .app-version {
      position: fixed;
      bottom: 0.5rem;
      left: 0.75rem;
      font-size: 0.75rem;
      color: var(--acai-400, #9ca3af);
    }
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999;
    }
    .modal-fechar-caixa {
      background: white;
      padding: 1.5rem;
      border-radius: 12px;
      max-width: 400px;
      box-shadow: 0 8px 32px rgba(0,0,0,0.2);
    }
    .modal-fechar-caixa h3 { margin: 0 0 0.75rem 0; font-size: 1.25rem; }
    .modal-fechar-caixa p { margin: 0 0 1rem 0; color: #444; font-size: 0.95rem; }
    .form-group-fechar { margin: 1rem 0 1.25rem 0; }
    .form-group-fechar label { display: block; margin-bottom: 0.35rem; margin-top: 0.75rem; font-weight: 600; color: #333; }
    .form-group-fechar label:first-of-type { margin-top: 0; }
    .input-valor-fechar { width: 100%; padding: 0.6rem 0.75rem; font-size: 1.1rem; border: 2px solid #e5e5e5; border-radius: 8px; box-sizing: border-box; margin-bottom: 0.25rem; }
    .field-hint-fechar { display: block; font-size: 0.8rem; color: #666; margin-top: 0.25rem; }
    .input-valor-fechar:focus { outline: none; border-color: var(--acai-500); }
    .modal-buttons-reabertura { justify-content: center; margin-top: 1rem; gap: 1rem; }
    .modal-buttons-reabertura .btn-abrir { width: auto; padding: 0.6rem 1.5rem; }
    .btn-sair {
      padding: 0.6rem 1.5rem;
      border-radius: 10px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      background: transparent;
      color: var(--acai-700);
      border: 2px solid var(--acai-400);
      transition: background 0.2s, color 0.2s;
    }
    .btn-sair:hover { background: var(--acai-100); color: var(--acai-800); }
    .modal-buttons { display: flex; gap: 0.75rem; justify-content: flex-end; }
    .btn-cancel { padding: 0.5rem 1rem; border: 1px solid #ccc; border-radius: 8px; background: #f5f5f5; cursor: pointer; }
    .btn-confirm { padding: 0.5rem 1rem; border: none; border-radius: 8px; background: var(--acai-600); color: white; cursor: pointer; font-weight: 600; }
    .btn-confirm:disabled { opacity: 0.7; cursor: not-allowed; }
    .btn-fechar-caixa {
      padding: 0.45rem 0.85rem;
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
      background: #dc2626;
      color: white;
      border: none;
      transition: background 0.2s;
    }
    .btn-fechar-caixa:hover { background: #b91c1c; }
    .caixa-loading {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      color: var(--acai-800);
    }
    .caixa-loading .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid var(--acai-200);
      border-top-color: var(--acai-600);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .caixa-abertura-overlay {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, var(--acai-900) 0%, var(--acai-700) 100%);
      padding: 1.5rem;
    }
    .caixa-abertura-modal {
      background: white;
      padding: 2rem;
      border-radius: 16px;
      box-shadow: 0 12px 40px rgba(0,0,0,0.2);
      max-width: 400px;
      width: 100%;
    }
    .caixa-abertura-modal h2 { margin: 0 0 0.5rem 0; font-size: 1.5rem; color: var(--acai-900); }
    .caixa-abertura-desc { margin: 0 0 1.5rem 0; color: #666; font-size: 0.95rem; }
    .caixa-abertura-form label { display: block; margin-bottom: 0.35rem; font-weight: 600; color: #333; }
    .input-valor {
      width: 100%;
      padding: 0.75rem 1rem;
      font-size: 1.25rem;
      border: 2px solid #e5e5e5;
      border-radius: 10px;
      margin-bottom: 1rem;
      box-sizing: border-box;
    }
    .input-valor:focus { outline: none; border-color: var(--acai-500); }
    .error-msg { color: #b91c1c; font-size: 0.9rem; margin: -0.5rem 0 0.75rem 0; }
    .btn-abrir {
      width: 100%;
      padding: 0.85rem;
      font-size: 1.1rem;
      font-weight: 600;
      border: none;
      border-radius: 10px;
      background: var(--acai-600);
      color: white;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-abrir:hover:not(:disabled) { background: var(--acai-500); }
    .btn-abrir:disabled { opacity: 0.7; cursor: not-allowed; }
  `],
})
export class AppComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly caixaService = inject(CaixaService);

  caixaStatusLoading = true;
  caixaNeedsAbertura = false;
  caixaAberto = false;
  caixaFechadoHoje = false;
  showFecharCaixaConfirm = false;
  valorFechamento = 0;
  valorRetirada = 0;
  fecharCaixaError: string | null = null;
  fecharCaixaLoading = false;

  constructor() {
    this.caixaService.caixaStatus$.subscribe((s) => {
      if (s != null) {
        this.caixaNeedsAbertura = s.needsAbertura;
        this.caixaAberto = s.caixaAberto;
        this.caixaFechadoHoje = s.caixaFechadoHoje ?? false;
        this.caixaStatusLoading = false;
      }
    });
    effect(() => {
      if (this.authService.isLoggedIn()) {
        this.caixaStatusLoading = true;
        this.caixaService.refreshStatus();
        if (this.authService.gestaoPermission() === null) {
          this.authService.loadPermissoes();
        }
      }
    });
  }

  ngOnInit(): void {}

  confirmarFecharCaixa(): void {
    this.valorFechamento = 0;
    this.valorRetirada = 0;
    this.fecharCaixaError = null;
    this.showFecharCaixaConfirm = true;
  }

  fecharCaixa(): void {
    this.fecharCaixaError = null;
    const valor = Number(this.valorFechamento);
    if (isNaN(valor) || valor < 0) {
      this.fecharCaixaError = 'Informe o valor de fechamento.';
      return;
    }
    const retirada = Number(this.valorRetirada);
    const valorRetiradaOk = !isNaN(retirada) && retirada >= 0 ? retirada : 0;
    this.fecharCaixaLoading = true;
    this.caixaService.fechar(valor, valorRetiradaOk).subscribe({
      next: () => {
        this.fecharCaixaLoading = false;
        this.showFecharCaixaConfirm = false;
        this.caixaService.refreshStatus();
      },
      error: (err) => {
        this.fecharCaixaLoading = false;
        this.fecharCaixaError = err?.error?.message || err?.error || 'Erro ao fechar caixa.';
      },
    });
  }
}
