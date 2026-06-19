import { DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, finalize, takeUntil } from 'rxjs';
import { ApiBackendService } from '../../core/api-backend.service';
import { AuthService } from '../../core/auth.service';
import { RealtimeService } from '../../core/realtime.service';
import { FormaPagamento, MesaComOcupacao, MesaResumo, PedidoDetalhe } from '../../models/api.models';

type FiltroMesa = 'todas' | 'ocupadas' | 'encerrando' | 'livres';

const ROTULO_FORMA: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'Pix',
  DEBITO: 'Débito',
  CREDITO: 'Crédito',
  OUTRO: 'Outro',
};

@Component({
  selector: 'app-mesas',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './mesas.component.html',
  styleUrl: './mesas.component.scss',
})
export class MesasComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiBackendService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroy$ = new Subject<void>();

  readonly rotuloForma = ROTULO_FORMA;

  mesas: MesaComOcupacao[] = [];
  resumo: MesaResumo | null = null;
  filtro: FiltroMesa = 'todas';

  drawerAberto = false;
  mesaSelecionada: MesaComOcupacao | null = null;

  colaboradorId: number | null = null;
  clienteNome = '';
  descricao = '';
  pessoas: number | null = 1;
  documentoFiscal = false;
  carregando = false;
  erro: string | null = null;
  /** Feedback após solicitar fechamento (churrasqueiro → balcão). */
  feedbackMesas: string | null = null;
  solicitacaoFechamentoCarregando = false;
  private feedbackMesasTimer: ReturnType<typeof setTimeout> | null = null;

  /** Modal de pagamento (caixa) a partir do drawer da mesa ocupada. */
  modalQuitarConta = false;
  pedidoModal: PedidoDetalhe | null = null;
  carregandoPedidoModal = false;
  formas: FormaPagamento[] = ['DINHEIRO', 'PIX', 'DEBITO', 'CREDITO', 'OUTRO'];
  formaPagamento: FormaPagamento = 'PIX';
  valorPagamento = 0;
  valorRecebidoDinheiro: number | null = null;
  carregandoPagamentoModal = false;
  erroPagamentoModal: string | null = null;
  readonly atalhosValor = [2, 5, 10, 20, 50, 100];
  carregandoImprimirComanda = false;

  ngOnInit(): void {
    this.carregar();
    /** Colaborador padrão (API ainda exige id na abertura; não aparece na tela). */
    this.api.getColaboradores().subscribe((c) => {
      if (c.length && this.colaboradorId == null) {
        this.colaboradorId = c[0].id;
      }
    });
    this.realtime.pedidoEventos.pipe(takeUntil(this.destroy$)).subscribe((e) => {
      this.carregar();
      if (this.modalQuitarConta && this.pedidoModal && e.pedidoId === this.pedidoModal.id) {
        this.recarregarPedidoModal();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.feedbackMesasTimer != null) {
      clearTimeout(this.feedbackMesasTimer);
      this.feedbackMesasTimer = null;
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregar(): void {
    this.api.getMesas().subscribe((m) => {
      this.mesas = m;
      const sel = this.mesaSelecionada;
      if (sel) {
        const atualizada = m.find((x) => x.id === sel.id);
        if (atualizada) {
          this.mesaSelecionada = atualizada;
        }
      }
    });
    this.api.getMesasResumo().subscribe((r) => (this.resumo = r));
  }

  mesasVisiveis(): MesaComOcupacao[] {
    return this.mesas.filter((m) => {
      switch (this.filtro) {
        case 'ocupadas':
          return m.ocupada;
        case 'livres':
          return !m.ocupada;
        case 'encerrando':
          return m.status === 'ENCERRANDO_SERVICO';
        default:
          return true;
      }
    });
  }

  selecionarMesa(m: MesaComOcupacao): void {
    this.mesaSelecionada = m;
    this.drawerAberto = true;
    this.erro = null;
    this.feedbackMesas = null;
    if (this.feedbackMesasTimer != null) {
      clearTimeout(this.feedbackMesasTimer);
      this.feedbackMesasTimer = null;
    }
    this.clienteNome = '';
    if (!m.ocupada) {
      this.pessoas = 1;
    }
  }

  fecharDrawer(): void {
    this.modalQuitarConta = false;
    this.pedidoModal = null;
    this.erroPagamentoModal = null;
    this.drawerAberto = false;
    this.mesaSelecionada = null;
  }

  iniciarMesa(): void {
    if (!this.mesaSelecionada) {
      return;
    }
    this.carregando = true;
    this.erro = null;
    this.api.getColaboradores().subscribe({
      next: (c) => {
        if (c.length) {
          this.colaboradorId = c[0].id;
        }
        if (this.colaboradorId == null) {
          this.carregando = false;
          this.erro = 'Cadastre ao menos um colaborador para abrir mesas.';
          return;
        }
        this.api
          .abrirMesa(this.mesaSelecionada!.id, {
            colaboradorId: this.colaboradorId,
            clienteNome: this.clienteNome.trim() || undefined,
            descricao: this.descricao || undefined,
            pessoas: this.pessoas ?? undefined,
            documentoFiscal: this.documentoFiscal,
          })
          .subscribe({
            next: (p) => {
              this.carregando = false;
              this.fecharDrawer();
              void this.router.navigate(['/pedidos', p.id]);
            },
            error: (e) => {
              this.carregando = false;
              this.erro = e?.error?.erro ?? 'Não foi possível abrir a mesa.';
            },
          });
      },
      error: (e) => {
        this.carregando = false;
        this.erro = e?.error?.erro ?? 'Não foi possível carregar colaboradores. Tente de novo.';
      },
    });
  }

  irPedido(m: MesaComOcupacao): void {
    if (m.pedidoAbertoId) {
      void this.router.navigate(['/pedidos', m.pedidoAbertoId]);
    }
  }

  podeQuitarContaNoCaixa(): boolean {
    return this.auth.usuario()?.perfil === 'ATENDIMENTO';
  }

  /** Mesma comanda completa da cozinha que no detalhe do pedido (balcão). */
  podeImprimirComandaCozinhaDrawer(): boolean {
    return this.podeQuitarContaNoCaixa() && !!this.mesaSelecionada?.pedidoAbertoId;
  }

  imprimirComandaCozinhaDrawer(): void {
    const id = this.mesaSelecionada?.pedidoAbertoId;
    if (!id || !this.podeImprimirComandaCozinhaDrawer() || this.carregandoImprimirComanda) {
      return;
    }
    this.carregandoImprimirComanda = true;
    this.api
      .imprimirComandaCozinha(id)
      .pipe(finalize(() => (this.carregandoImprimirComanda = false)))
      .subscribe();
  }

  podeSolicitarFechamentoComandaChurrasqueiro(): boolean {
    return this.auth.usuario()?.perfil === 'CHURRASQUEIRO';
  }

  solicitarFechamentoComandaParaBalcao(): void {
    const m = this.mesaSelecionada;
    if (!m?.pedidoAbertoId) {
      return;
    }
    this.erro = null;
    this.feedbackMesas = null;
    if (this.solicitacaoFechamentoCarregando) {
      return;
    }
    this.solicitacaoFechamentoCarregando = true;
    this.api.solicitarFechamentoComandaMesa(m.id).subscribe({
      next: () => {
        this.solicitacaoFechamentoCarregando = false;
        this.carregar();
        this.feedbackMesas = 'Solicitação enviada ao balcão.';
        if (this.feedbackMesasTimer != null) {
          clearTimeout(this.feedbackMesasTimer);
        }
        this.feedbackMesasTimer = setTimeout(() => {
          this.feedbackMesas = null;
          this.feedbackMesasTimer = null;
        }, 4500);
      },
      error: (e) => {
        this.solicitacaoFechamentoCarregando = false;
        this.erro = e?.error?.erro ?? 'Não foi possível enviar a solicitação.';
      },
    });
  }

  abrirModalQuitarConta(): void {
    if (!this.mesaSelecionada?.pedidoAbertoId || !this.podeQuitarContaNoCaixa()) {
      return;
    }
    this.modalQuitarConta = true;
    this.erroPagamentoModal = null;
    this.carregandoPedidoModal = true;
    this.pedidoModal = null;
    this.api.getPedido(this.mesaSelecionada.pedidoAbertoId).subscribe({
      next: (p) => {
        this.pedidoModal = p;
        this.carregandoPedidoModal = false;
        this.sincronizarFormPagamentoModal();
      },
      error: () => {
        this.carregandoPedidoModal = false;
        this.erroPagamentoModal = 'Não foi possível carregar o pedido.';
      },
    });
  }

  fecharModalQuitarConta(): void {
    this.modalQuitarConta = false;
    this.pedidoModal = null;
    this.erroPagamentoModal = null;
    this.carregandoPagamentoModal = false;
  }

  private recarregarPedidoModal(): void {
    const id = this.pedidoModal?.id;
    if (!id) {
      return;
    }
    this.api.getPedido(id).subscribe({
      next: (p) => {
        this.pedidoModal = p;
        this.sincronizarFormPagamentoModal();
      },
    });
  }

  private sincronizarFormPagamentoModal(): void {
    const p = this.pedidoModal;
    if (!p) {
      return;
    }
    const r = this.restanteConta(p);
    this.valorPagamento = r > 0 ? Math.round(r * 100) / 100 : 0;
    if (this.formaPagamento === 'DINHEIRO') {
      this.valorRecebidoDinheiro = this.valorPagamento || null;
    } else {
      this.valorRecebidoDinheiro = null;
    }
  }

  aplicarAtalhoValor(nota: number): void {
    if (!this.pedidoModal) {
      return;
    }
    const r = this.restanteConta(this.pedidoModal);
    if (r <= 0) {
      return;
    }
    this.valorPagamento = Math.round(Math.min(r, nota) * 100) / 100;
    if (this.formaPagamento === 'DINHEIRO') {
      this.valorRecebidoDinheiro = this.valorPagamento;
    }
  }

  selecionarFormaModal(f: FormaPagamento): void {
    this.formaPagamento = f;
    this.onFormaModalChange();
  }

  onFormaModalChange(): void {
    if (this.formaPagamento === 'DINHEIRO' && this.pedidoModal) {
      const r = this.restanteConta(this.pedidoModal);
      this.valorRecebidoDinheiro = r > 0 ? Math.round(r * 100) / 100 : null;
    } else {
      this.valorRecebidoDinheiro = null;
    }
  }

  restanteConta(p: PedidoDetalhe): number {
    if (p.restante != null) {
      return p.restante;
    }
    return Math.max(0, p.total - (p.totalPago ?? 0));
  }

  registrarPagamentoModal(): void {
    const p = this.pedidoModal;
    if (!p) {
      return;
    }
    const v = Number(this.valorPagamento);
    if (!Number.isFinite(v) || v <= 0) {
      this.erroPagamentoModal = 'Informe um valor válido.';
      return;
    }
    this.carregandoPagamentoModal = true;
    this.erroPagamentoModal = null;
    const body: {
      forma: FormaPagamento;
      valor: number;
      valorRecebidoDinheiro?: number | null;
    } = { forma: this.formaPagamento, valor: v };
    if (this.formaPagamento === 'DINHEIRO' && this.valorRecebidoDinheiro != null && this.valorRecebidoDinheiro > 0) {
      body.valorRecebidoDinheiro = this.valorRecebidoDinheiro;
    }
    this.api.registrarPagamento(p.id, body).subscribe({
      next: (atual) => {
        this.pedidoModal = atual;
        this.carregandoPagamentoModal = false;
        this.sincronizarFormPagamentoModal();
        this.carregar();
        if (atual.status === 'PAGO') {
          this.fecharModalQuitarConta();
          this.fecharDrawer();
        }
      },
      error: (e) => {
        this.carregandoPagamentoModal = false;
        this.erroPagamentoModal = e?.error?.erro ?? 'Erro ao registrar pagamento.';
      },
    });
  }

  encerrarSemValorModal(): void {
    const p = this.pedidoModal;
    if (!p || p.total > 0) {
      return;
    }
    this.carregandoPagamentoModal = true;
    this.erroPagamentoModal = null;
    this.api.patchPedidoStatus(p.id, 'PAGO').subscribe({
      next: (pedido) => {
        this.carregandoPagamentoModal = false;
        this.carregar();
        this.fecharModalQuitarConta();
        this.fecharDrawer();
      },
      error: (e) => {
        this.carregandoPagamentoModal = false;
        this.erroPagamentoModal = e?.error?.erro ?? 'Não foi possível encerrar.';
      },
    });
  }
}
