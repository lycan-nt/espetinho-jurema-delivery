import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';

const API_AUTH = 'http://localhost:8080/api/auth';
const API = 'http://localhost:8080/api';
const TOKEN_KEY = 'mix_acai_token';
const USER_KEY = 'mix_acai_user';

export interface LoginResponse {
  token: string;
  username: string;
}

export interface PermissoesResponse {
  gestao: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenSignal = signal<string | null>(this.getStoredToken());
  private readonly usernameSignal = signal<string | null>(this.getStoredUser());
  /** null = ainda não carregou; true/false = permissão de acessar o módulo Gestão */
  private readonly gestaoPermissionSignal = signal<boolean | null>(null);

  readonly token = this.tokenSignal.asReadonly();
  readonly username = this.usernameSignal.asReadonly();
  readonly gestaoPermission = this.gestaoPermissionSignal.asReadonly();
  readonly isLoggedIn = computed(() => !!this.tokenSignal());
  readonly hasGestao = computed(() => this.gestaoPermissionSignal() === true);

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_AUTH}/login`, { username, password }).pipe(
      tap((res) => {
        this.setSession(res.token, res.username);
      }),
    );
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.usernameSignal.set(null);
    this.gestaoPermissionSignal.set(null);
    this.router.navigate(['/login']);
  }

  /** Carrega permissões do usuário (gestão). Chamar após login ou quando o app inicia com token. */
  loadPermissoes(): void {
    if (!this.tokenSignal()) return;
    this.http.get<PermissoesResponse>(`${API}/permissoes`).pipe(
      tap((r) => this.gestaoPermissionSignal.set(r.gestao)),
      catchError(() => {
        this.gestaoPermissionSignal.set(false);
        return of({ gestao: false });
      }),
    ).subscribe();
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  private setSession(token: string, username: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    sessionStorage.setItem(USER_KEY, username);
    this.tokenSignal.set(token);
    this.usernameSignal.set(username);
  }

  private getStoredToken(): string | null {
    if (typeof sessionStorage === 'undefined') return null;
    return sessionStorage.getItem(TOKEN_KEY);
  }

  private getStoredUser(): string | null {
    if (typeof sessionStorage === 'undefined') return null;
    return sessionStorage.getItem(USER_KEY);
  }
}
