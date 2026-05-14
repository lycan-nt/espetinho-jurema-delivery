import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { PerfilUsuario } from '../models/api.models';

const KEY_TOKEN = 'ej_token';
const KEY_PERFIL = 'ej_perfil';
const KEY_NOME = 'ej_nome';
const KEY_LOGIN = 'ej_login';

export interface LoginResponseDto {
  token: string;
  tipoToken: string;
  expiraEmSegundos: number;
  nome: string;
  perfil: PerfilUsuario;
  login: string;
}

export interface UsuarioMeDto {
  login: string;
  nome: string;
  perfil: PerfilUsuario;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly base = environment.apiUrl ? `${environment.apiUrl}/api/v1` : '/api/v1';

  private readonly tokenSig = signal<string | null>(this.readStoredToken());

  readonly token = this.tokenSig.asReadonly();

  readonly usuario = computed(() => {
    const t = this.tokenSig();
    if (!t) {
      return null;
    }
    return {
      login: localStorage.getItem(KEY_LOGIN) ?? '',
      nome: localStorage.getItem(KEY_NOME) ?? '',
      perfil: (localStorage.getItem(KEY_PERFIL) as PerfilUsuario) ?? 'GARCOM',
    };
  });

  loginRequest(login: string, senha: string): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>(`${this.base}/auth/login`, { login, senha }).pipe(
      tap((r) => {
        localStorage.setItem(KEY_TOKEN, r.token);
        localStorage.setItem(KEY_PERFIL, r.perfil);
        localStorage.setItem(KEY_NOME, r.nome);
        localStorage.setItem(KEY_LOGIN, r.login);
        this.tokenSig.set(r.token);
      }),
    );
  }

  logout(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  clearSession(): void {
    localStorage.removeItem(KEY_TOKEN);
    localStorage.removeItem(KEY_PERFIL);
    localStorage.removeItem(KEY_NOME);
    localStorage.removeItem(KEY_LOGIN);
    this.tokenSig.set(null);
  }

  getToken(): string | null {
    return this.tokenSig();
  }

  refreshMe(): Observable<UsuarioMeDto> {
    return this.http.get<UsuarioMeDto>(`${this.base}/auth/me`).pipe(
      tap((u) => {
        localStorage.setItem(KEY_PERFIL, u.perfil);
        localStorage.setItem(KEY_NOME, u.nome);
        localStorage.setItem(KEY_LOGIN, u.login);
      }),
    );
  }

  /**
   * Lê o token do localStorage. Se existir mas estiver expirado, limpa o armazenamento
   * e retorna null — o guard redireciona para login automaticamente.
   */
  private readStoredToken(): string | null {
    const token = localStorage.getItem(KEY_TOKEN);
    if (!token) return null;
    if (this.isTokenExpired(token)) {
      localStorage.removeItem(KEY_TOKEN);
      localStorage.removeItem(KEY_PERFIL);
      localStorage.removeItem(KEY_NOME);
      localStorage.removeItem(KEY_LOGIN);
      return null;
    }
    return token;
  }

  /**
   * Decodifica o payload do JWT e verifica se o campo `exp` já passou.
   * Retorna true se expirado ou se o token for malformado.
   */
  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() >= (payload.exp as number) * 1000;
    } catch {
      return true;
    }
  }
}
