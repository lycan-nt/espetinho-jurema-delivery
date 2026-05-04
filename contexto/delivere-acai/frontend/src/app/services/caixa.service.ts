import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Caixa, CaixaStatus } from '../models/caixa.model';

const API = 'http://localhost:8080/api/caixa';

@Injectable({
  providedIn: 'root',
})
export class CaixaService {
  /** Último status do caixa; atualizado por refreshStatus(). Para o header e componentes sincronizarem. */
  private readonly statusSubject = new BehaviorSubject<CaixaStatus | null>(null);
  readonly caixaStatus$ = this.statusSubject.asObservable();

  constructor(private http: HttpClient) {}

  getStatus(): Observable<CaixaStatus> {
    return this.http.get<CaixaStatus>(`${API}/status`);
  }

  /** Atualiza o status e notifica assinantes (ex.: header). Chamar após abrir/fechar caixa. */
  refreshStatus(): void {
    this.getStatus().subscribe({
      next: (s) => this.statusSubject.next(s),
      error: () => this.statusSubject.next(null),
    });
  }

  abrir(valorAbertura: number): Observable<Caixa> {
    return this.http.post<Caixa>(`${API}/abrir`, { valorAbertura });
  }

  fechar(valorFechamento: number, valorRetirada?: number): Observable<Caixa> {
    return this.http.post<Caixa>(`${API}/fechar`, { valorFechamento, valorRetirada: valorRetirada ?? 0 });
  }
}
