import { DecimalPipe } from '@angular/common';
import { Component, HostBinding, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  catchError,
  concatMap,
  filter,
  finalize,
  forkJoin,
  from,
  last,
  of,
  Subject,
  switchMap,
  takeUntil,
} from 'rxjs';
import { ApiBackendService } from '../../core/api-backend.service';
import { AuthService } from '../../core/auth.service';
import { RealtimeService } from '../../core/realtime.service';
import {
  CategoriaCardapio,
  FormaPagamento,
  ItemPedido,
  MesaComOcupacao,
  MesaTransferencia,
  PedidoDetalhe,
  PedidoStatus,
  PontoCarne,
  Produto,
} from '../../models/api.models';

const ROTULO_PONTO: Record<PontoCarne, string> = {
  MAL_PASSADA: 'Mal passada',
  AO_PONTO: 'Ao ponto',
  BEM_PASSADA: 'Bem passada',
};

/** Igual ao backend; produtos nesta categoria exigem ponto ao lançar. */
const CATEGORIA_ESPETINHOS = 'Espetinhos';

/** Produtos sem categoria ou com id órfão na lista de categorias. */
const CAT_OUTROS_ID = -1;

const OPCOES_PONTO_CARNE: { valor: PontoCarne; rotulo: string }[] = [
  { valor: 'MAL_PASSADA', rotulo: ROTULO_PONTO.MAL_PASSADA },
  { valor: 'AO_PONTO', rotulo: ROTULO_PONTO.AO_PONTO },
  { valor: 'BEM_PASSADA', rotulo: ROTULO_PONTO.BEM_PASSADA },
];

const ROTULO_FORMA: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'Pix',
  DEBITO: 'Débito',
  CREDITO: 'Crédito',
  OUTRO: 'Outro',
};

/** Linha de itens ativos agrupada (mesmo produto + ponto + obs); {@link _itens} = linhas no pedido para cancelar. */
interface ItemAgregado extends ItemPedido {
  _itens: ItemPedido[];
}

/** Linha montada a partir do cardápio para POST sequencial. */
interface LinhaAdicionarPedido {
  produtoId: number;
  quantidade: number;
  observacao: string | null;
  pontoCarne: PontoCarne | null;
}

/** Produto efetivamente marcado para envio (não-espetinho; qtd + obs local). */
interface SelecaoCardapioProduto {
  quantidade: number;
  pontoCarne: null;
}

/** Uma linha de espetinho já com ponto escolhido (vai virar um POST). */
interface LinhaEspetinhoPronta {
  quantidade: number;
  pontoCarne: PontoCarne;
}

/**
 * Espetinho: várias linhas (quantidade + ponto) antes de clicar em Adicionar.
 * {@link rascunho} = linha em edição (falta escolher o ponto).
 */
interface EstadoEspetinhoCardapio {
  prontas: LinhaEspetinhoPronta[];
  rascunho: { quantidade: number } | null;
}

@Component({
  selector: 'app-pedido-detalhe',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './pedido-detalhe.component.html',
  styleUrl: './pedido-detalhe.component.scss',
})
export class PedidoDetalheComponent implements OnInit, OnDestroy {
  /** Espaço inferior no mobile quando a faixa inferior (Adicionar e/ou Enviar comanda) está fixa. */
  @HostBinding('class.pedido-detalhe--mobile-add-bar')
  get hostMobileBarAdicionar(): boolean {
    return this.deveMostrarBarraCtaInferior();
  }

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ApiBackendService);
  readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroy$ = new Subject<void>();

  pedidoId = 0;
  pedido: PedidoDetalhe | null = null;
  produtos: Produto[] = [];
  categorias: CategoriaCardapio[] = [];
  categoriaAtivaId: number | null = null;
  observacao = '';
  /** Produtos comuns marcados (não são da categoria Espetinhos). */
  selecaoCardapioPorProdutoId: Record<number, SelecaoCardapioProduto> = {};
  /** Espetinhos: várias linhas (qtd + ponto) antes de Adicionar. */
  espetinhoEstadoPorProdutoId: Record<number, EstadoEspetinhoCardapio> = {};
  readonly opcoesPontoCarne = OPCOES_PONTO_CARNE;
  readonly rotuloPonto = ROTULO_PONTO;
  carregandoItem = false;
  erroItem: string | null = null;
  erroPagamento: string | null = null;

  formas: FormaPagamento[] = ['DINHEIRO', 'PIX', 'DEBITO', 'CREDITO', 'OUTRO'];
  formaPagamento: FormaPagamento = 'PIX';
  valorPagamento = 0;
  valorRecebidoDinheiro: number | null = null;
  carregandoPagamento = false;
  carregandoEnviarComanda = false;
  carregandoImprimirComanda = false;

  mesasParaTransferir: MesaComOcupacao[] = [];
  mesaDestinoTransferirId: number | null = null;
  transferenciasMesa: MesaTransferencia[] = [];
  carregandoTransferirMesa = false;
  erroTransferirMesa: string | null = null;

  /** Item em processo de cancelamento (evita cliques duplos). */
  cancelandoItemId: number | null = null;

  /** Modal: cancelar só parte da quantidade quando {@link ItemPedido.quantidade} > 1. */
  modalCancelarItem: ItemPedido | null = null;
  qtdParaCancelarItem = 1;

  /** Painel lateral com ações (cozinha, conta, impressão, transferência). */
  panelAcoesAberto = false;

  readonly rotuloForma = ROTULO_FORMA;

  private produtoPorId(id: number | null | undefined): Produto | undefined {
    return id != null ? this.produtos.find((x) => x.id === id) : undefined;
  }

  private idsCategoriasCadastradas(): Set<number> {
    return new Set(this.categorias.map((c) => Number(c.id)));
  }

  private produtoEstaAtivo(pr: Produto): boolean {
    return pr.ativo !== false;
  }

  /** Produtos sem categoria ou categoria inexistente no cadastro. */
  private temLinhaOutros(): boolean {
    const ids = this.idsCategoriasCadastradas();
    return this.produtos.some(
      (p) =>
        this.produtoEstaAtivo(p) &&
        (p.categoriaId == null || !ids.has(Number(p.categoriaId))),
    );
  }

  produtosNaCategoria(catId: number): Produto[] {
    const base = this.produtos.filter((p) => this.produtoEstaAtivo(p));
    if (catId === CAT_OUTROS_ID) {
      const ids = this.idsCategoriasCadastradas();
      return base.filter((p) => p.categoriaId == null || !ids.has(Number(p.categoriaId)));
    }
    return base.filter((p) => Number(p.categoriaId) === Number(catId));
  }

  categoriasParaCardapio(): { id: number; nome: string }[] {
    const out: { id: number; nome: string }[] = [];
    for (const c of [...this.categorias].sort((a, b) => a.ordem - b.ordem || a.id - b.id)) {
      if (this.produtosNaCategoria(c.id).length > 0) {
        out.push({ id: c.id, nome: c.nome });
      }
    }
    if (this.temLinhaOutros()) {
      out.push({ id: CAT_OUTROS_ID, nome: 'Outros' });
    }
    return out;
  }

  selecionarCategoriaCardapio(catId: number): void {
    this.categoriaAtivaId = catId;
  }

  produtoEhEspetinhoPorId(produtoId: number): boolean {
    const p = this.produtoPorId(produtoId);
    const nome = p?.categoriaNome?.trim();
    return nome != null && nome.toLowerCase() === CATEGORIA_ESPETINHOS.toLowerCase();
  }

  produtoMarcadoNoCardapio(produtoId: number): boolean {
    if (this.produtoEhEspetinhoPorId(produtoId)) {
      const e = this.espetinhoEstadoPorProdutoId[produtoId];
      return e != null && e.prontas.length > 0 && e.rascunho == null;
    }
    return this.selecaoCardapioPorProdutoId[produtoId] != null;
  }

  /** Linha aberta: marcado na seleção ou espetinho em montagem (ainda sem ponto). */
  produtoLinhaCardapioAberta(produtoId: number): boolean {
    return (
      this.selecaoCardapioPorProdutoId[produtoId] != null || this.espetinhoEstadoPorProdutoId[produtoId] != null
    );
  }

  quantidadeEspetinhoDraftAbertos(): number {
    return Object.values(this.espetinhoEstadoPorProdutoId).filter((e) => e.rascunho != null).length;
  }

  espetinhoLinhasProntas(produtoId: number): LinhaEspetinhoPronta[] {
    return this.espetinhoEstadoPorProdutoId[produtoId]?.prontas ?? [];
  }

  espetinhoRascunhoAberto(produtoId: number): boolean {
    return this.espetinhoEstadoPorProdutoId[produtoId]?.rascunho != null;
  }

  espetinhoPodeAbrirOutroPonto(produtoId: number): boolean {
    const e = this.espetinhoEstadoPorProdutoId[produtoId];
    return e != null && e.rascunho == null && e.prontas.length > 0;
  }

  rotuloLinhaEspetinhoPronta(l: LinhaEspetinhoPronta): string {
    return `${l.quantidade}x ${ROTULO_PONTO[l.pontoCarne]}`;
  }

  abrirRascunhoOutroPontoEspetinho(produtoId: number): void {
    const cur = this.espetinhoEstadoPorProdutoId[produtoId];
    if (!cur || cur.rascunho != null) {
      return;
    }
    this.espetinhoEstadoPorProdutoId = {
      ...this.espetinhoEstadoPorProdutoId,
      [produtoId]: { ...cur, rascunho: { quantidade: 1 } },
    };
    this.erroItem = null;
  }

  removerLinhaEspetinhoPronta(produtoId: number, index: number): void {
    const cur = this.espetinhoEstadoPorProdutoId[produtoId];
    if (!cur || index < 0 || index >= cur.prontas.length) {
      return;
    }
    const prontas = cur.prontas.filter((_, i) => i !== index);
    const nextEst: EstadoEspetinhoCardapio | undefined =
      prontas.length === 0 && cur.rascunho == null ? undefined : { ...cur, prontas };
    const copy = { ...this.espetinhoEstadoPorProdutoId };
    if (nextEst == null) {
      delete copy[produtoId];
    } else {
      copy[produtoId] = nextEst;
    }
    this.espetinhoEstadoPorProdutoId = copy;
    this.erroItem = null;
  }

  /** Marcado de fato = enviável; espetinho com rascunho aberto ainda não. */
  checkVerdeCardapio(produtoId: number): boolean {
    return this.produtoMarcadoNoCardapio(produtoId);
  }

  alternarMarcacaoProduto(pr: Produto): void {
    const id = Number(pr.id);
    if (this.produtoEhEspetinhoPorId(id)) {
      const nextEsp = { ...this.espetinhoEstadoPorProdutoId };
      if (nextEsp[id]) {
        delete nextEsp[id];
      } else {
        nextEsp[id] = { prontas: [], rascunho: { quantidade: 1 } };
      }
      this.espetinhoEstadoPorProdutoId = nextEsp;
      this.erroItem = null;
      return;
    }
    const next = { ...this.selecaoCardapioPorProdutoId };
    if (next[id]) {
      delete next[id];
    } else {
      next[id] = { quantidade: 1, pontoCarne: null };
    }
    this.selecaoCardapioPorProdutoId = next;
    this.erroItem = null;
  }

  limparSelecaoCardapio(): void {
    this.selecaoCardapioPorProdutoId = {};
    this.espetinhoEstadoPorProdutoId = {};
    this.erroItem = null;
  }

  qtdProdutoCardapio(produtoId: number): number {
    const d = this.espetinhoEstadoPorProdutoId[produtoId]?.rascunho;
    if (d) {
      const q = Math.floor(Number(d.quantidade));
      return !Number.isFinite(q) || q < 1 ? 1 : q;
    }
    const s = this.selecaoCardapioPorProdutoId[produtoId];
    const q = Math.floor(Number(s?.quantidade));
    return !Number.isFinite(q) || q < 1 ? 1 : q;
  }

  ajustarQtdProdutoCardapio(produtoId: number, delta: number): void {
    const est = this.espetinhoEstadoPorProdutoId[produtoId];
    const rascunho = est?.rascunho;
    let q = this.qtdProdutoCardapio(produtoId) + delta;
    if (q < 1) {
      q = 1;
    }
    if (rascunho && est) {
      this.espetinhoEstadoPorProdutoId = {
        ...this.espetinhoEstadoPorProdutoId,
        [produtoId]: { ...est, rascunho: { quantidade: q } },
      };
      return;
    }
    const cur = this.selecaoCardapioPorProdutoId[produtoId];
    if (!cur) {
      return;
    }
    this.selecaoCardapioPorProdutoId = {
      ...this.selecaoCardapioPorProdutoId,
      [produtoId]: { ...cur, quantidade: q },
    };
  }

  definirQtdProdutoCardapio(produtoId: number, raw: unknown): void {
    let q = Math.floor(Number(raw));
    if (!Number.isFinite(q) || q < 1) {
      q = 1;
    }
    const est = this.espetinhoEstadoPorProdutoId[produtoId];
    const rascunho = est?.rascunho;
    if (rascunho && est) {
      this.espetinhoEstadoPorProdutoId = {
        ...this.espetinhoEstadoPorProdutoId,
        [produtoId]: { ...est, rascunho: { quantidade: q } },
      };
      return;
    }
    const cur = this.selecaoCardapioPorProdutoId[produtoId];
    if (!cur) {
      return;
    }
    this.selecaoCardapioPorProdutoId = {
      ...this.selecaoCardapioPorProdutoId,
      [produtoId]: { ...cur, quantidade: q },
    };
  }

  definirPontoProdutoCardapio(produtoId: number, op: PontoCarne): void {
    const est = this.espetinhoEstadoPorProdutoId[produtoId];
    if (!est?.rascunho) {
      return;
    }
    let q = Math.floor(Number(est.rascunho.quantidade));
    if (!Number.isFinite(q) || q < 1) {
      q = 1;
    }
    const prontas = [...est.prontas, { quantidade: q, pontoCarne: op }];
    this.espetinhoEstadoPorProdutoId = {
      ...this.espetinhoEstadoPorProdutoId,
      [produtoId]: { prontas, rascunho: null },
    };
    this.erroItem = null;
  }

  rotuloAriaToggleProduto(pr: Produto): string {
    const id = Number(pr.id);
    if (this.produtoEhEspetinhoPorId(id)) {
      if (this.produtoMarcadoNoCardapio(id)) {
        return `Desmarcar ${pr.nome}`;
      }
      if (this.espetinhoEstadoPorProdutoId[id]) {
        return `Cancelar escolha de ${pr.nome}`;
      }
      return `Escolher quantidade e ponto de ${pr.nome}`;
    }
    return `${this.produtoMarcadoNoCardapio(id) ? 'Desmarcar' : 'Marcar'} ${pr.nome}`;
  }

  quantidadeProdutosMarcadosNoCardapio(): number {
    let n = Object.keys(this.selecaoCardapioPorProdutoId).length;
    for (const [idStr, e] of Object.entries(this.espetinhoEstadoPorProdutoId)) {
      const id = Number(idStr);
      if (this.produtoEhEspetinhoPorId(id) && e.prontas.length > 0 && e.rascunho == null) {
        n += 1;
      }
    }
    return n;
  }

  /** Linhas que a API aceita (espetinho só entra com ponto definido). */
  private montarLinhasDaSelecaoCardapio(): LinhaAdicionarPedido[] {
    const obs = this.observacao?.trim() ? this.observacao.trim() : null;
    const linhas: LinhaAdicionarPedido[] = [];
    for (const [idStr, sel] of Object.entries(this.selecaoCardapioPorProdutoId)) {
      const produtoId = Number(idStr);
      if (this.produtoEhEspetinhoPorId(produtoId)) {
        continue;
      }
      const qtd = Math.floor(Number(sel.quantidade));
      const quantidade = !Number.isFinite(qtd) || qtd < 1 ? 1 : qtd;
      linhas.push({
        produtoId,
        quantidade,
        observacao: obs,
        pontoCarne: null,
      });
    }
    for (const [idStr, e] of Object.entries(this.espetinhoEstadoPorProdutoId)) {
      const produtoId = Number(idStr);
      if (!this.produtoEhEspetinhoPorId(produtoId)) {
        continue;
      }
      for (const L of e.prontas) {
        let q = Math.floor(Number(L.quantidade));
        if (!Number.isFinite(q) || q < 1) {
          q = 1;
        }
        linhas.push({
          produtoId,
          quantidade: q,
          observacao: obs,
          pontoCarne: L.pontoCarne,
        });
      }
    }
    return linhas;
  }

  quantidadeLinhasProntasParaEnviar(): number {
    return this.montarLinhasDaSelecaoCardapio().length;
  }

  botaoAdicionarItemDesabilitado(): boolean {
    if (!this.pedido || this.carregandoItem) {
      return true;
    }
    return this.quantidadeLinhasProntasParaEnviar() === 0;
  }

  rotuloBotaoAdicionarItens(): string {
    if (this.carregandoItem) {
      return 'Adicionando…';
    }
    const n = this.quantidadeLinhasProntasParaEnviar();
    if (n <= 1) {
      return 'Adicionar';
    }
    return `Adicionar ${n} itens`;
  }

  /** Há marcação ou rascunho (ex.: espetinho sem ponto) ainda não enviados ao servidor. */
  temSelecaoPendenteNaoAdicionada(): boolean {
    return (
      this.quantidadeLinhasProntasParaEnviar() > 0 || this.quantidadeEspetinhoDraftAbertos() > 0
    );
  }

  /** Um único CTA forte na faixa inferior: pendência → Adicionar; caso contrário, Enviar comanda quando aplicável. */
  deveMostrarBarraCtaInferior(): boolean {
    const p = this.pedido;
    if (!p || p.status === 'PAGO' || p.status === 'CANCELADO') {
      return false;
    }
    return this.temSelecaoPendenteNaoAdicionada() || this.podeEnviarComanda();
  }

  private readonly statusPermiteTransferirMesa: PedidoStatus[] = ['RASCUNHO', 'ABERTO', 'EM_PREPARO', 'PRONTO'];

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        takeUntil(this.destroy$),
        switchMap((pm) => {
          this.pedidoId = Number(pm.get('id'));
          this.carregarPedido();
          return forkJoin([this.api.getProdutos(), this.api.getCategorias()]).pipe(
            catchError(() => {
              this.erroItem = 'Não foi possível carregar categorias e produtos. Verifique a API e tente de novo.';
              return of<[Produto[], CategoriaCardapio[]]>([[], []]);
            }),
          );
        }),
      )
      .subscribe({
        next: ([produtos, categorias]) => {
          this.produtos = produtos;
          this.categorias = categorias;
          this.selecaoCardapioPorProdutoId = {};
          this.espetinhoEstadoPorProdutoId = {};
          const cats = this.categoriasParaCardapio();
          this.categoriaAtivaId = cats.length > 0 ? cats[0].id : null;
        },
      });

    this.realtime.pedidoEventos
      .pipe(
        takeUntil(this.destroy$),
        filter((e) => e.pedidoId === this.pedidoId),
      )
      .subscribe(() => this.carregarPedido());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('document:keydown.escape')
  onEscapeFecharAcoes(): void {
    if (this.modalCancelarItem) {
      this.fecharModalCancelarItem();
      return;
    }
    if (this.panelAcoesAberto) {
      this.fecharPanelAcoes();
    }
  }

  private carregarPedido(): void {
    if (!this.pedidoId) {
      return;
    }
    this.api.getPedido(this.pedidoId).subscribe((p) => {
      this.pedido = p;
      const r = this.restanteConta(p);
      this.valorPagamento = r > 0 ? Math.round(r * 100) / 100 : 0;
      if (this.formaPagamento === 'DINHEIRO') {
        this.valorRecebidoDinheiro = this.valorPagamento || null;
      }

      if (p.tipo === 'MESA') {
        forkJoin([this.api.getMesas(), this.api.getPedidoTransferenciasMesa(this.pedidoId)])
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: ([mesas, trans]) => {
              this.transferenciasMesa = trans;
              const pode = this.podeTransferirMesa();
              if (pode) {
                this.mesasParaTransferir = mesas
                  .filter((m) => m.status === 'LIVRE' && m.id !== p.mesaId)
                  .sort((a, b) => a.numero - b.numero);
                const aindaValido = this.mesasParaTransferir.some((m) => m.id === this.mesaDestinoTransferirId);
                if (!aindaValido) {
                  this.mesaDestinoTransferirId = this.mesasParaTransferir[0]?.id ?? null;
                }
              } else {
                this.mesasParaTransferir = [];
                this.mesaDestinoTransferirId = null;
              }
            },
          });
      } else {
        this.transferenciasMesa = [];
        this.mesasParaTransferir = [];
        this.mesaDestinoTransferirId = null;
      }
    });
  }

  podeTransferirMesa(): boolean {
    const ped = this.pedido;
    if (!ped || ped.tipo !== 'MESA' || ped.mesaId == null) {
      return false;
    }
    if (ped.status === 'PAGO' || ped.status === 'CANCELADO') {
      return false;
    }
    return this.statusPermiteTransferirMesa.includes(ped.status);
  }

  transferirMesa(): void {
    if (!this.pedido || this.mesaDestinoTransferirId == null) {
      this.erroTransferirMesa = 'Escolha a mesa de destino.';
      return;
    }
    this.carregandoTransferirMesa = true;
    this.erroTransferirMesa = null;
    this.api.transferirPedidoMesa(this.pedido.id, this.mesaDestinoTransferirId).subscribe({
      next: () => {
        this.carregandoTransferirMesa = false;
        this.panelAcoesAberto = false;
        this.carregarPedido();
      },
      error: (e) => {
        this.carregandoTransferirMesa = false;
        this.erroTransferirMesa = e?.error?.erro ?? 'Não foi possível transferir a mesa.';
      },
    });
  }

  formatarDataTransferenciaMesa(iso: string): string {
    const d = new Date(iso);
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('pt-BR');
  }

  itemPedidoAtivo(it: ItemPedido): boolean {
    return it.cancelado !== true;
  }

  itensAtivosNoPedido(p: PedidoDetalhe): ItemPedido[] {
    return (p.itens ?? []).filter((i) => this.itemPedidoAtivo(i));
  }

  /**
   * Agrupa itens ativos por produto + ponto da carne + observação (quantidades somadas).
   * A ordem das linhas segue a primeira ocorrência de cada grupo no pedido (ex.: carne ao ponto,
   * frango, outro carne ao ponto → uma linha “carne” com qtd total, depois frango).
   */
  get itensAgrupados(): ItemAgregado[] {
    if (!this.pedido?.itens) return [];
    const mapa = new Map<string, ItemAgregado>();
    const cancelados: ItemAgregado[] = [];

    for (const item of this.pedido.itens) {
      if (item.cancelado) {
        cancelados.push({ ...item, _itens: [item] });
        continue;
      }
      const pontoKey = item.pontoCarne ?? '';
      const obsKey = (item.observacao ?? '').trim();
      const chave = `${item.produtoId}|${pontoKey}|${obsKey}`;
      let agg = mapa.get(chave);
      if (!agg) {
        agg = {
          ...item,
          quantidade: 0,
          precoUnitario: 0,
          _itens: [],
        };
        mapa.set(chave, agg);
      }
      agg.quantidade += item.quantidade;
      agg._itens.push(item);
    }

    for (const agg of mapa.values()) {
      const subtotal = agg._itens.reduce((s, i) => s + i.precoUnitario * i.quantidade, 0);
      agg.precoUnitario = agg.quantidade > 0 ? subtotal / agg.quantidade : 0;
    }

    return [...mapa.values(), ...cancelados];
  }

  trackIdItemAgregado(agg: ItemAgregado): string {
    return agg._itens.map((i) => i.id).join('-');
  }

  podeCancelarItemAgregado(agg: ItemAgregado): boolean {
    return agg._itens.some((it) => this.podeCancelarItemPedido(it));
  }

  cancelarItemAgregado(agg: ItemAgregado): void {
    const alvo = agg._itens[agg._itens.length - 1];
    this.cancelarItemPedido(alvo);
  }

  cancelandoItemAgregado(agg: ItemAgregado): boolean {
    return agg._itens.some((it) => this.cancelandoItemId === it.id);
  }

  podeCancelarItemPedido(it: ItemPedido): boolean {
    const p = this.pedido;
    if (!p || p.status === 'PAGO' || p.status === 'CANCELADO') {
      return false;
    }
    return this.itemPedidoAtivo(it);
  }

  cancelarItemPedido(it: ItemPedido): void {
    if (!this.pedido || !this.podeCancelarItemPedido(it)) {
      return;
    }
    if (it.quantidade > 1) {
      this.erroItem = null;
      this.modalCancelarItem = it;
      this.qtdParaCancelarItem = 1;
      return;
    }
    const msg =
      'Cancelar este item? Ele deixa de entrar no total, o estoque é devolvido e o lançamento fica registrado como cancelado.';
    if (typeof globalThis.confirm === 'function' && !globalThis.confirm(msg)) {
      return;
    }
    this.executarCancelamentoItem(it);
  }

  fecharModalCancelarItem(): void {
    this.modalCancelarItem = null;
  }

  confirmarCancelamentoQuantidade(): void {
    const it = this.modalCancelarItem;
    if (!this.pedido || !it || this.cancelandoItemId !== null) {
      return;
    }
    let q = Math.floor(Number(this.qtdParaCancelarItem));
    if (!Number.isFinite(q) || q < 1) {
      q = 1;
    }
    if (q > it.quantidade) {
      q = it.quantidade;
    }
    this.executarCancelamentoItem(it, q);
  }

  /** Sem body na API quando {@code quantidade} omissa = cancelar linha inteira. */
  private executarCancelamentoItem(it: ItemPedido, quantidade?: number): void {
    if (!this.pedido) {
      return;
    }
    const enviarQ =
      quantidade != null &&
      Number.isFinite(quantidade) &&
      quantidade > 0 &&
      quantidade < it.quantidade;
    this.cancelandoItemId = it.id;
    this.erroItem = null;
    this.api.cancelarItemPedido(this.pedido.id, it.id, enviarQ ? quantidade : undefined).subscribe({
      next: (p) => {
        this.pedido = p;
        this.cancelandoItemId = null;
        this.fecharModalCancelarItem();
      },
      error: (e) => {
        this.cancelandoItemId = null;
        this.erroItem = e?.error?.erro ?? 'Não foi possível cancelar o item.';
      },
    });
  }

  adicionarItem(): void {
    if (!this.pedido || this.carregandoItem) {
      return;
    }
    const linhas = this.montarLinhasDaSelecaoCardapio();
    if (linhas.length === 0) {
      if (this.quantidadeEspetinhoDraftAbertos() > 0) {
        this.erroItem =
          'Para marcar espetinhos, escolha o ponto da carne em cada um (ou cancele tocando de novo no item).';
      } else {
        this.erroItem = 'Marque um ou mais produtos no cardápio.';
      }
      return;
    }
    const idsAfetados = new Set(linhas.map((L) => L.produtoId));
    this.carregandoItem = true;
    this.erroItem = null;
    from(linhas)
      .pipe(
        concatMap((L) =>
          this.api.adicionarItem(this.pedido!.id, L.produtoId, L.quantidade, L.observacao, L.pontoCarne),
        ),
        last(),
        finalize(() => {
          this.carregandoItem = false;
        }),
      )
      .subscribe({
        next: (p) => {
          this.pedido = p;
          const nextSel = { ...this.selecaoCardapioPorProdutoId };
          const nextEsp = { ...this.espetinhoEstadoPorProdutoId };
          for (const id of idsAfetados) {
            delete nextSel[id];
            delete nextEsp[id];
          }
          this.selecaoCardapioPorProdutoId = nextSel;
          this.espetinhoEstadoPorProdutoId = nextEsp;
          if (Object.keys(nextSel).length === 0 && Object.keys(nextEsp).length === 0) {
            this.observacao = '';
          }
        },
        error: (e) => {
          this.erroItem = e?.error?.erro ?? 'Erro ao adicionar.';
        },
      });
  }

  mudarStatus(status: PedidoStatus): void {
    if (!this.pedido) {
      return;
    }
    this.erroPagamento = null;
    this.api.patchPedidoStatus(this.pedido.id, status).subscribe({
      next: (p) => {
        this.pedido = p;
        this.panelAcoesAberto = false;
      },
      error: (e) => (this.erroPagamento = e?.error?.erro ?? 'Erro ao atualizar status.'),
    });
  }

  registrarPagamento(): void {
    if (!this.pedido) {
      return;
    }
    const v = Number(this.valorPagamento);
    if (!Number.isFinite(v) || v <= 0) {
      this.erroPagamento = 'Informe um valor válido.';
      return;
    }
    this.carregandoPagamento = true;
    this.erroPagamento = null;
    const body: {
      forma: FormaPagamento;
      valor: number;
      valorRecebidoDinheiro?: number | null;
    } = { forma: this.formaPagamento, valor: v };
    if (this.formaPagamento === 'DINHEIRO' && this.valorRecebidoDinheiro != null && this.valorRecebidoDinheiro > 0) {
      body.valorRecebidoDinheiro = this.valorRecebidoDinheiro;
    }
    this.api.registrarPagamento(this.pedido.id, body).subscribe({
      next: (p) => {
        this.pedido = p;
        this.carregandoPagamento = false;
        this.panelAcoesAberto = false;
        const r = this.restanteConta(p);
        this.valorPagamento = r > 0 ? Math.round(r * 100) / 100 : 0;
        if (this.formaPagamento === 'DINHEIRO') {
          this.valorRecebidoDinheiro = this.valorPagamento || null;
        }
      },
      error: (e) => {
        this.carregandoPagamento = false;
        this.erroPagamento = e?.error?.erro ?? 'Erro ao registrar pagamento.';
      },
    });
  }

  /** Pagamentos e fechamento no caixa apenas no perfil atendimento (PC). */
  podeVerPagamentoCaixa(): boolean {
    return this.auth.usuario()?.perfil === 'ATENDIMENTO';
  }

  /** Comanda térmica completa (reimpressão) — atendimento, pedido de mesa. */
  podeImprimirComandaCozinha(): boolean {
    const p = this.pedido;
    return !!p && this.auth.usuario()?.perfil === 'ATENDIMENTO' && p.tipo === 'MESA';
  }

  podeEnviarComanda(): boolean {
    const p = this.pedido;
    const u = this.auth.usuario();
    if (!p || !u) {
      return false;
    }
    if (p.tipo !== 'MESA') {
      return false;
    }
    if (p.status === 'PAGO' || p.status === 'CANCELADO') {
      return false;
    }
    if (!this.itensAtivosNoPedido(p).length) {
      return false;
    }
    return u.perfil === 'GARCOM' || u.perfil === 'CHURRASQUEIRO';
  }

  enviarComanda(): void {
    if (!this.pedido) {
      return;
    }
    this.carregandoEnviarComanda = true;
    this.erroItem = null;
    this.api.enviarComandaPedido(this.pedido.id).subscribe({
      next: (p) => {
        this.pedido = p;
        this.carregandoEnviarComanda = false;
        void this.router.navigate(['/mesas']);
      },
      error: (e) => {
        this.carregandoEnviarComanda = false;
        this.erroItem = e?.error?.erro ?? 'Não foi possível enviar a comanda.';
      },
    });
  }

  onFormaChange(): void {
    if (this.formaPagamento === 'DINHEIRO' && this.pedido) {
      const r = this.restanteConta(this.pedido);
      this.valorRecebidoDinheiro = r > 0 ? Math.round(r * 100) / 100 : null;
    } else {
      this.valorRecebidoDinheiro = null;
    }
  }

  imprimir(fiscal: boolean): void {
    if (!this.pedido) {
      return;
    }
    this.api.imprimirComprovante(this.pedido.id, { fiscal });
    this.panelAcoesAberto = false;
  }

  imprimirComandaCozinha(): void {
    if (!this.pedido || this.carregandoImprimirComanda) {
      return;
    }
    this.carregandoImprimirComanda = true;
    this.api
      .imprimirComandaCozinha(this.pedido.id)
      .pipe(finalize(() => (this.carregandoImprimirComanda = false)))
      .subscribe({
        complete: () => {
          this.panelAcoesAberto = false;
        },
      });
  }

  abrirPanelAcoes(): void {
    this.panelAcoesAberto = true;
    this.erroPagamento = null;
  }

  fecharPanelAcoes(): void {
    this.panelAcoesAberto = false;
  }

  voltar(): void {
    this.panelAcoesAberto = false;
    if (this.pedido?.mesaId) {
      void this.router.navigate(['/mesas']);
    } else {
      void this.router.navigate(['/pedidos']);
    }
  }

  tituloVoltar(): string {
    return this.pedido?.mesaId ? '← Mesas' : '← Pedidos';
  }

  /** Restante para exibir formulário de pagamento (compatível se API omitir campos). */
  restanteConta(p: PedidoDetalhe): number {
    if (p.restante != null) {
      return p.restante;
    }
    return Math.max(0, p.total - (p.totalPago ?? 0));
  }
}
