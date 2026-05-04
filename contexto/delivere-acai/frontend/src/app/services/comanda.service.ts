import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comanda, ComandaItem, FormaPagamento, Relatorio, TipoComanda, TipoProduto } from '../models/comanda.model';
import { AuthService } from './auth.service';

const API = 'http://localhost:8080/api/comandas';

@Injectable({
  providedIn: 'root',
})
export class ComandaService {
  /** Última opção selecionada (por peso / preço fixo) para persistir ao fechar e abrir nova comanda ou ao navegar. */
  private lastTipoProduto: TipoProduto = 'POR_PESO';

  constructor(
    private http: HttpClient,
    private auth: AuthService,
  ) {}

  /** Cria ou adiciona à comanda. Para PRECO_FIXO envie valorTotal; para POR_PESO envie pesoKg e precoPorKilo. */
  criar(comanda: Omit<Comanda, 'id' | 'dataHora' | 'status'>): Observable<Comanda> {
    return this.http.post<Comanda>(API, comanda);
  }

  listar(apenasAbertas?: boolean): Observable<Comanda[]> {
    let params = new HttpParams();
    if (apenasAbertas === true) {
      params = params.set('abertas', 'true');
    }
    return this.http.get<Comanda[]>(API, { params });
  }

  /** Obtém o próximo identificador sequencial para o tipo (ex.: COMANDA → "001", "002"). */
  proximoIdentificador(tipo: TipoComanda): Observable<{ identificador: string }> {
    return this.http.get<{ identificador: string }>(`${API}/proximo-identificador`, {
      params: { tipo },
    });
  }

  getLastTipoProduto(): TipoProduto {
    return this.lastTipoProduto;
  }

  setLastTipoProduto(t: TipoProduto): void {
    this.lastTipoProduto = t;
  }

  buscar(id: number): Observable<Comanda> {
    return this.http.get<Comanda>(`${API}/${id}`);
  }

  /** Lista os itens lançados na comanda (para detalhes no relatório). */
  listarItens(comandaId: number): Observable<ComandaItem[]> {
    return this.http.get<ComandaItem[]>(`${API}/${comandaId}/itens`);
  }

  atualizar(id: number, comanda: { pesoKg: number; precoPorKilo: number }): Observable<Comanda> {
    return this.http.put<Comanda>(`${API}/${id}`, comanda);
  }

  /** Atualiza tipo (cliente / mesa / comanda) e identificador em comanda aberta. */
  alterarCabecalho(id: number, body: { tipo: TipoComanda; identificador: string }): Observable<Comanda> {
    return this.http.patch<Comanda>(`${API}/${id}/cabecalho`, body);
  }

  /** Remove um item da comanda (subtrai peso e valor). Para cancelar ou antes de editar. */
  removerItem(id: number, pesoKg: number, valorTotal: number): Observable<Comanda> {
    const token = this.auth.getToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return this.http.post<Comanda>(`${API}/${id}/remover-item`, { pesoKg, valorTotal }, { headers });
  }

  fechar(id: number, formaPagamento: FormaPagamento): Observable<Comanda> {
    return this.http.patch<Comanda>(`${API}/${id}/fechar`, { formaPagamento });
  }

  /** Emite NFC-e em homologação para a comanda fechada. */
  emitirNfce(id: number): Observable<Comanda> {
    return this.http.post<Comanda>(`${API}/${id}/nfce/emitir`, {});
  }

  relatorio(dataInicio?: string, dataFim?: string): Observable<Relatorio> {
    let params = new HttpParams();
    if (dataInicio) params = params.set('dataInicio', dataInicio);
    if (dataFim) params = params.set('dataFim', dataFim);
    return this.http.get<Relatorio>('http://localhost:8080/api/relatorio', { params });
  }

  /** Envia o relatório do período para a planilha configurada no Google Sheets. */
  enviarParaGoogleSheets(dataInicio?: string, dataFim?: string): Observable<{ message: string }> {
    let params = new HttpParams();
    if (dataInicio) params = params.set('dataInicio', dataInicio);
    if (dataFim) params = params.set('dataFim', dataFim);
    return this.http.post<{ message: string }>(
      'http://localhost:8080/api/relatorio/enviar-google-sheets',
      null,
      { params }
    );
  }
}
