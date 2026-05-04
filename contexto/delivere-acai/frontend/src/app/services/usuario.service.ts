import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = 'http://localhost:8080/api/usuarios';

export interface Usuario {
  id: number;
  username: string;
  setor: string;
}

export interface UsuarioCreate {
  username: string;
  password: string;
  setor: 'VENDAS' | 'GESTAO';
}

export interface UsuarioUpdate {
  username: string;
  password?: string;
  setor: 'VENDAS' | 'GESTAO';
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  constructor(private http: HttpClient) {}

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(API);
  }

  criar(dto: UsuarioCreate): Observable<Usuario> {
    return this.http.post<Usuario>(API, dto);
  }

  atualizar(id: number, dto: UsuarioUpdate): Observable<Usuario> {
    return this.http.put<Usuario>(`${API}/${id}`, dto);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }
}
