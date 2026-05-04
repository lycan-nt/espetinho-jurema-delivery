import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = 'http://localhost:8080/api/produtos';

export interface PrecoKgDTO {
  precoPorKilo: number;
}

export interface ConfigBalancaDTO {
  baudRate: number;
  serialConfig: '8n1' | '8e2';
  enviarEnq: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  constructor(private http: HttpClient) {}

  getPrecoKg(): Observable<PrecoKgDTO> {
    return this.http.get<PrecoKgDTO>(`${API}/preco-kg`);
  }

  setPrecoKg(precoPorKilo: number): Observable<PrecoKgDTO> {
    return this.http.put<PrecoKgDTO>(`${API}/preco-kg`, { precoPorKilo });
  }

  getConfigBalanca(): Observable<ConfigBalancaDTO> {
    return this.http.get<ConfigBalancaDTO>(`${API}/config-balanca`);
  }

  setConfigBalanca(config: ConfigBalancaDTO): Observable<ConfigBalancaDTO> {
    return this.http.put<ConfigBalancaDTO>(`${API}/config-balanca`, config);
  }
}
