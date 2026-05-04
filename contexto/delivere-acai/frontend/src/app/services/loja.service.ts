import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = 'http://localhost:8080/api/lojas';

export interface Loja {
  id: string;
  nome: string;
  endereco?: string;
  responsavel?: string;
}

@Injectable({ providedIn: 'root' })
export class LojaService {
  constructor(private http: HttpClient) {}

  listar(): Observable<Loja[]> {
    return this.http.get<Loja[]>(API);
  }

  buscar(id: string): Observable<Loja> {
    return this.http.get<Loja>(`${API}/${encodeURIComponent(id)}`);
  }

  criar(loja: Loja): Observable<Loja> {
    return this.http.post<Loja>(API, loja);
  }

  atualizar(
    id: string,
    loja: { id: string; nome: string; endereco?: string; responsavel?: string },
  ): Observable<Loja> {
    return this.http.put<Loja>(`${API}/${encodeURIComponent(id)}`, loja);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/${encodeURIComponent(id)}`);
  }
}
