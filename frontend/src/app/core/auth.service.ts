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
      login: sessionStorage.getItem(KEY_LOGIN) ?? '',
      nome: sessionStorage.getItem(KEY_NOME) ?? '',
      perfil: (sessionStorage.getItem(KEY_PERFIL) as PerfilUsuario) ?? 'GARCOM',
    };
  });

  loginRequest(login: string, senha: string): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>(`${this.base}/auth/login`, { login, senha }).pipe(
      tap((r) => {
        sessionStorage.setItem(KEY_TOKEN, r.token);
        sessionStorage.setItem(KEY_PERFIL, r.perfil);
        sessionStorage.setItem(KEY_NOME, r.nome);
        sessionStorage.setItem(KEY_LOGIN, r.login);
        this.tokenSig.set(r.token);
      }),
    );
  }

  logout(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  clearSession(): void {
    sessionStorage.removeItem(KEY_TOKEN);
    sessionStorage.removeItem(KEY_PERFIL);
    sessionStorage.removeItem(KEY_NOME);
    sessionStorage.removeItem(KEY_LOGIN);
    this.tokenSig.set(null);
  }

  getToken(): string | null {
    return this.tokenSig();
  }

  refreshMe(): Observable<UsuarioMeDto> {
    return this.http.get<UsuarioMeDto>(`${this.base}/auth/me`).pipe(
      tap((u) => {
        sessionStorage.setItem(KEY_PERFIL, u.perfil);
        sessionStorage.setItem(KEY_NOME, u.nome);
        sessionStorage.setItem(KEY_LOGIN, u.login);
      }),
    );
  }

  private readStoredToken(): string | null {
    return sessionStorage.getItem(KEY_TOKEN);
  }
}
