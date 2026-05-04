import { DecimalPipe } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, forkJoin, map, Subject, takeUntil } from 'rxjs';
import { ApiBackendService } from '../../core/api-backend.service';
import { AuthService } from '../../core/auth.service';
import { RealtimeService } from '../../core/realtime.service';
import {
  FormaPagamento,
  ItemPedido,
  MesaComOcupacao,
  MesaTransferencia,
  PedidoDetalhe,
  PedidoStatus,
  Produto,
} from '../../models/api.models';

const ROTULO_FORMA: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'Pix',
  DEBITO: 'Débito',
  CREDITO: 'Crédito',
  OUTRO: 'Outro',
};

@Component({
  selector: 'app-pedido-detalhe',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './pedido-detalhe.component.html',
  styleUrl: './pedido-detalhe.component.scss',
})
export class PedidoDetalheComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ApiBackendService);
  readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroy$ = new Subject<void>();

  pedidoId = 0;
  pedido: PedidoDetalhe | null = null;
  produtos: Produto[] = [];
  produtoId: number | null = null;
  quantidade = 1;
  observacao = '';
  carregandoItem = false;
  erroItem: string | null = null;
  erroPagamento: string | null = null;

  formas: FormaPagamento[] = ['DINHEIRO', 'PIX', 'DEBITO', 'CREDITO', 'OUTRO'];
  formaPagamento: FormaPagamento = 'PIX';
  valorPagamento = 0;
  valorRecebidoDinheiro: number | null = null;
  carregandoPagamento = false;
  carregandoEnviarComanda = false;

  mesasParaTransferir: MesaComOcupacao[] = [];
  mesaDestinoTransferirId: number | null = null;
  transferenciasMesa: MesaTransferencia[] = [];
  carregandoTransferirMesa = false;
  erroTransferirMesa: string | null = null;

  /** Item em processo de cancelamento (evita cliques duplos). */
  cancelandoItemId: number | null = null;

  /** Painel lateral com ações (cozinha, conta, impressão, transferência). */
  panelAcoesAberto = false;

  readonly rotuloForma = ROTULO_FORMA;

  private readonly statusPermiteTransferirMesa: PedidoStatus[] = ['RASCUNHO', 'ABERTO', 'EM_PREPARO', 'PRONTO'];

  ngOnInit(): void {
    this.api.getProdutos().subscribe((p) => {
      this.produtos = p;
      if (p.length) {
        this.produtoId = p[0].id;
      }
    });

    this.route.paramMap
      .pipe(
        takeUntil(this.destroy$),
        map((pm) => Number(pm.get('id'))),
      )
      .subscribe((id) => {
        this.pedidoId = id;
        this.carregarPedido();
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

  podeCancelarItemPedido(it: ItemPedido): boolean {
    const p = this.pedido;
    if (!p || p.status === 'PAGO' || p.status === 'CANCELADO') {
      return false;
    }
    return this.itemPedidoAtivo(it);
  }

  cancelarItemPedido(it: ItemPedido): void {
    if (!this.pedido) {
      return;
    }
    const msg =
      'Cancelar este item? Ele deixa de entrar no total, o estoque é devolvido e o lançamento fica registrado como cancelado.';
    if (typeof globalThis.confirm === 'function' && !globalThis.confirm(msg)) {
      return;
    }
    this.cancelandoItemId = it.id;
    this.erroItem = null;
    this.api.cancelarItemPedido(this.pedido.id, it.id).subscribe({
      next: (p) => {
        this.pedido = p;
        this.cancelandoItemId = null;
      },
      error: (e) => {
        this.cancelandoItemId = null;
        this.erroItem = e?.error?.erro ?? 'Não foi possível cancelar o item.';
      },
    });
  }

  adicionarItem(): void {
    if (!this.pedido || !this.produtoId) {
      return;
    }
    this.carregandoItem = true;
    this.erroItem = null;
    this.api.adicionarItem(this.pedido.id, this.produtoId, this.quantidade, this.observacao || null).subscribe({
      next: (p) => {
        this.pedido = p;
        this.carregandoItem = false;
        this.observacao = '';
        this.quantidade = 1;
      },
      error: (e) => {
        this.carregandoItem = false;
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
        if (p.status === 'PAGO' && this.podeVerPagamentoCaixa()) {
          this.api.imprimirComprovante(p.id, { fechamento: true });
        }
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
        const r = this.restanteConta(p);
        this.valorPagamento = r > 0 ? Math.round(r * 100) / 100 : 0;
        if (this.formaPagamento === 'DINHEIRO') {
          this.valorRecebidoDinheiro = this.valorPagamento || null;
        }
        if (p.status === 'PAGO' && this.podeVerPagamentoCaixa()) {
          this.api.imprimirComprovante(p.id, { fechamento: true });
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
