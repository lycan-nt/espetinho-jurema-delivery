import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { imprimirTextoTerminalBrowser } from './impressao-browser.util';
import {
  CaixaStatus,
  CategoriaCardapio,
  Cliente,
  Colaborador,
  EstoqueConfig,
  FaturamentoResumo,
  FormaPagamento,
  MesaComOcupacao,
  MesaResumo,
  MesaTransferencia,
  PedidoDetalhe,
  PedidoLista,
  PedidoStatus,
  PedidoTipo,
  PerfilUsuario,
  Produto,
  ReconhecerAlertaResponse,
  UsuarioAdmin,
  ImpressaoConfig,
  ImpressaoFilasResponse,
  ImprimirLocalResponse,
} from '../models/api.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiBackendService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl
    ? `${environment.apiUrl}/api/v1`
    : '/api/v1';

  getMesas(): Observable<MesaComOcupacao[]> {
    return this.http.get<MesaComOcupacao[]>(`${this.base}/mesas`);
  }

  getMesasResumo(): Observable<MesaResumo> {
    return this.http.get<MesaResumo>(`${this.base}/mesas/resumo`);
  }

  abrirMesa(
    mesaId: number,
    body: {
      colaboradorId: number;
      clienteId?: number | null;
      /** Nome digitado; usado se não houver clienteId. */
      clienteNome?: string | null;
      descricao?: string | null;
      pessoas?: number | null;
      documentoFiscal: boolean;
    },
  ): Observable<PedidoDetalhe> {
    return this.http.post<PedidoDetalhe>(`${this.base}/mesas/${mesaId}/abrir`, body);
  }

  patchMesaStatus(mesaId: number, status: string): Observable<void> {
    return this.http.patch<void>(`${this.base}/mesas/${mesaId}/status`, { status });
  }

  getColaboradores(): Observable<Colaborador[]> {
    return this.http.get<Colaborador[]>(`${this.base}/colaboradores`);
  }

  getClientes(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${this.base}/clientes`);
  }

  criarCliente(nome: string, telefone?: string | null, endereco?: string | null): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.base}/clientes`, { nome, telefone, endereco });
  }

  getProdutos(): Observable<Produto[]> {
    return this.http.get<Produto[]>(`${this.base}/produtos`);
  }

  getCategorias(): Observable<CategoriaCardapio[]> {
    return this.http.get<CategoriaCardapio[]>(`${this.base}/categorias`);
  }

  getAdminUsuarios(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>(`${this.base}/admin/usuarios`);
  }

  postAdminUsuario(body: {
    login: string;
    nomeExibicao: string;
    perfil: PerfilUsuario;
    senha: string;
  }): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>(`${this.base}/admin/usuarios`, body);
  }

  putAdminUsuario(
    id: number,
    body: { nomeExibicao: string; perfil: PerfilUsuario; ativo: boolean; senha?: string },
  ): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`${this.base}/admin/usuarios/${id}`, body);
  }

  getAdminProdutos(): Observable<Produto[]> {
    return this.http.get<Produto[]>(`${this.base}/admin/produtos`);
  }

  postAdminProduto(body: {
    nome: string;
    descricao?: string | null;
    preco: number;
    categoriaId: number;
    codigoImpressao?: string | null;
    ativo: boolean;
  }): Observable<Produto> {
    return this.http.post<Produto>(`${this.base}/admin/produtos`, body);
  }

  putAdminProduto(
    id: number,
    body: {
      nome: string;
      descricao?: string | null;
      preco: number;
      categoriaId: number;
      codigoImpressao?: string | null;
      ativo: boolean;
    },
  ): Observable<Produto> {
    return this.http.put<Produto>(`${this.base}/admin/produtos/${id}`, body);
  }

  getPedidos(status?: PedidoStatus | null, tipo?: PedidoTipo | null): Observable<PedidoLista[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<PedidoLista[]>(`${this.base}/pedidos`, { params });
  }

  getPedido(id: number): Observable<PedidoDetalhe> {
    return this.http.get<PedidoDetalhe>(`${this.base}/pedidos/${id}`);
  }

  getPedidoTransferenciasMesa(pedidoId: number): Observable<MesaTransferencia[]> {
    return this.http.get<MesaTransferencia[]>(`${this.base}/pedidos/${pedidoId}/mesa/transferencias`);
  }

  transferirPedidoMesa(pedidoId: number, mesaDestinoId: number): Observable<PedidoDetalhe> {
    return this.http.post<PedidoDetalhe>(`${this.base}/pedidos/${pedidoId}/mesa/transferir`, {
      mesaDestinoId,
    });
  }

  criarPedidoAvulso(body: {
    tipo: 'BALCAO' | 'DELIVERY';
    colaboradorId: number;
    clienteId?: number | null;
    descricao?: string | null;
    pessoas?: number | null;
    documentoFiscal: boolean;
  }): Observable<PedidoDetalhe> {
    return this.http.post<PedidoDetalhe>(`${this.base}/pedidos/avulsos`, body);
  }

  adicionarItem(pedidoId: number, produtoId: number, quantidade: number, observacao?: string | null): Observable<PedidoDetalhe> {
    return this.http.post<PedidoDetalhe>(`${this.base}/pedidos/${pedidoId}/itens`, {
      produtoId,
      quantidade,
      observacao,
    });
  }

  cancelarItemPedido(pedidoId: number, itemId: number): Observable<PedidoDetalhe> {
    return this.http.post<PedidoDetalhe>(`${this.base}/pedidos/${pedidoId}/itens/${itemId}/cancelar`, {});
  }

  /** Garçom/churrasqueiro: após lançar itens, notifica o atendimento (alerta + comanda). */
  enviarComandaPedido(pedidoId: number): Observable<PedidoDetalhe> {
    return this.http.post<PedidoDetalhe>(`${this.base}/pedidos/${pedidoId}/comanda/enviar`, {});
  }

  patchPedidoStatus(pedidoId: number, status: PedidoStatus): Observable<PedidoDetalhe> {
    return this.http.patch<PedidoDetalhe>(`${this.base}/pedidos/${pedidoId}/status`, { status });
  }

  registrarPagamento(
    pedidoId: number,
    body: {
      forma: FormaPagamento;
      valor: number;
      valorRecebidoDinheiro?: number | null;
    },
  ): Observable<PedidoDetalhe> {
    const payload: Record<string, unknown> = {
      forma: body.forma,
      valor: body.valor,
    };
    if (body.valorRecebidoDinheiro != null && body.valorRecebidoDinheiro > 0) {
      payload['valorRecebidoDinheiro'] = body.valorRecebidoDinheiro;
    }
    return this.http.post<PedidoDetalhe>(`${this.base}/pedidos/${pedidoId}/pagamentos`, payload);
  }

  /** Texto do comprovante (com Authorization). */
  getComprovanteTexto(
    pedidoId: number,
    opts?: { fiscal?: boolean; fechamento?: boolean },
  ): Observable<string> {
    let params = new HttpParams();
    if (opts?.fiscal) {
      params = params.set('fiscal', 'true');
    }
    if (opts?.fechamento) {
      params = params.set('fechamento', 'true');
    }
    return this.http.get(`${this.base}/pedidos/${pedidoId}/comprovante`, {
      params,
      responseType: 'text',
    });
  }

  /**
   * Tenta impressão na térmica via servidor (CUPS); se não imprimir lá, abre pré-visualização no navegador.
   */
  imprimirComprovante(pedidoId: number, opts?: { fiscal?: boolean; fechamento?: boolean }): void {
    let params = new HttpParams();
    if (opts?.fiscal) {
      params = params.set('fiscal', 'true');
    }
    if (opts?.fechamento) {
      params = params.set('fechamento', 'true');
    }
    this.http
      .post<ImprimirLocalResponse>(
        `${this.base}/pedidos/${pedidoId}/imprimir-local`,
        {},
        { params },
      )
      .subscribe({
        next: (res) => {
          if (res.impressoServidor) {
            return;
          }
          this.abrirPreviaComprovanteNoNavegador(pedidoId, opts);
        },
        error: () => this.abrirPreviaComprovanteNoNavegador(pedidoId, opts),
      });
  }

  private abrirPreviaComprovanteNoNavegador(
    pedidoId: number,
    opts?: { fiscal?: boolean; fechamento?: boolean },
  ): void {
    this.getComprovanteTexto(pedidoId, opts).subscribe({
      next: (texto) => {
        const titulo = opts?.fechamento ? 'Fechamento' : 'Comprovante';
        imprimirTextoTerminalBrowser(texto, titulo);
      },
    });
  }

  getImpressaoConfig(): Observable<ImpressaoConfig> {
    return this.http.get<ImpressaoConfig>(`${this.base}/impressao/config`);
  }

  patchImpressaoConfig(nomeImpressoraLp: string | null): Observable<ImpressaoConfig> {
    return this.http.patch<ImpressaoConfig>(`${this.base}/impressao/config`, {
      nomeImpressoraLp,
    });
  }

  getImpressaoFilas(): Observable<ImpressaoFilasResponse> {
    return this.http.get<ImpressaoFilasResponse>(`${this.base}/impressao/filas`);
  }

  comprovanteUrl(pedidoId: number, fiscal: boolean, fechamento = false): string {
    let q = '';
    if (fiscal && fechamento) {
      q = '?fiscal=true&fechamento=true';
    } else if (fiscal) {
      q = '?fiscal=true';
    } else if (fechamento) {
      q = '?fechamento=true';
    }
    const path = `${this.base}/pedidos/${pedidoId}/comprovante${q}`;
    if (path.startsWith('http')) {
      return path;
    }
    if (typeof globalThis !== 'undefined' && 'location' in globalThis) {
      const loc = (globalThis as unknown as { location: { origin: string } }).location;
      return `${loc.origin}${path}`;
    }
    return path;
  }

  getCaixaStatus(): Observable<CaixaStatus> {
    return this.http.get<CaixaStatus>(`${this.base}/caixa/status`);
  }

  /** Perfil ATENDIMENTO: confirma alerta e devolve texto da comanda para impressão (idempotente). */
  reconhecerAlertaAtendimento(alertaId: string): Observable<ReconhecerAlertaResponse> {
    return this.http.post<ReconhecerAlertaResponse>(
      `${this.base}/atendimento/alertas/${encodeURIComponent(alertaId)}/ok`,
      {},
    );
  }

  getEstoqueConfig(): Observable<EstoqueConfig> {
    return this.http.get<EstoqueConfig>(`${this.base}/estoque/config`);
  }

  patchEstoqueConfig(estoqueObrigatorio: boolean): Observable<EstoqueConfig> {
    return this.http.patch<EstoqueConfig>(`${this.base}/estoque/config`, { estoqueObrigatorio });
  }

  entradaEstoque(produtoId: number, quantidade: number, referencia?: string | null): Observable<void> {
    return this.http.post<void>(`${this.base}/estoque/entradas`, {
      produtoId,
      quantidade,
      referencia: referencia || undefined,
    });
  }

  ajusteEstoque(produtoId: number, novoSaldo: number): Observable<void> {
    return this.http.post<void>(`${this.base}/estoque/ajustes`, { produtoId, novoSaldo });
  }

  getFaturamentoResumo(inicioIso: string, fimIso: string): Observable<FaturamentoResumo> {
    const params = new HttpParams().set('inicio', inicioIso).set('fim', fimIso);
    return this.http.get<FaturamentoResumo>(`${this.base}/relatorios/faturamento`, { params });
  }

  getSistemaAcessoLocal(): Observable<{ ips: string[]; porta: number }> {
    return this.http.get<{ ips: string[]; porta: number }>(`${this.base}/sistema/acesso-local`);
  }
}
