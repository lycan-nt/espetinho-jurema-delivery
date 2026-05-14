import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { APP_VERSION_LABEL } from '../../core/app-version';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly appVersionLabel = APP_VERSION_LABEL;

  login = '';
  senha = '';
  senhaVisivel = false;
  carregando = false;
  erro: string | null = null;

  ngOnInit(): void {
    if (this.auth.getToken()) {
      void this.router.navigate(['/inicio']);
    }
  }

  focarSenha(senhaField: HTMLInputElement | null | undefined): void {
    if (senhaField) {
      senhaField.focus();
      senhaField.select();
    }
  }

  alternarVisibilidadeSenha(): void {
    this.senhaVisivel = !this.senhaVisivel;
  }

  entrar(): void {
    if (!this.login.trim() || !this.senha) {
      this.erro = 'Informe login e senha.';
      return;
    }
    this.carregando = true;
    this.erro = null;
    this.auth.loginRequest(this.login.trim(), this.senha).subscribe({
      next: () => {
        this.carregando = false;
        void this.router.navigate(['/inicio']);
      },
      error: (e) => {
        this.carregando = false;
        if (e instanceof HttpErrorResponse && e.status === 0) {
          this.erro =
            'Sem resposta da API. Confirme o backend na porta 9090, firewall do Mac liberando Java e que você abriu o app pelo IP da rede (ex.: http://192.168.x.x:4200), não só por localhost.';
          return;
        }
        if (e instanceof HttpErrorResponse && typeof e.error === 'object' && e.error !== null && 'erro' in e.error) {
          this.erro = String((e.error as { erro: string }).erro);
          return;
        }
        this.erro = e instanceof HttpErrorResponse ? `Não foi possível entrar (HTTP ${e.status}).` : 'Não foi possível entrar.';
      },
    });
  }
}
