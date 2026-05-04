import { Component, signal, OnInit, OnDestroy, HostListener, ViewChild, ElementRef, AfterViewInit, NgZone } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { ComandaService } from '../../services/comanda.service';
import { BalancaService, BalancaStatus, BalancaModo } from '../../services/balanca.service';
import { Comanda, FormaPagamento, TipoComanda, TipoProduto } from '../../models/comanda.model';
import { CaixaService } from '../../services/caixa.service';
import { ProdutoService } from '../../services/produto.service';

export interface ItemLancado {
  tipoProduto: TipoProduto;
  /** Peso em kg (POR_PESO) ou 0 (PRECO_FIXO). */
  pesoKg: number;
  /** Preço por kg (POR_PESO) ou preço unitário (PRECO_FIXO). */
  precoPorKilo: number;
  valorTotal: number;
  /** Quantidade (PRECO_FIXO); 1 ou indefinido para POR_PESO. */
  quantidade?: number;
}

const FORMAS_PAGAMENTO: { value: FormaPagamento; label: string }[] = [
  { value: 'PIX', label: 'PIX' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
];

@Component({
  selector: 'app-comanda',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './comanda.component.html',
  styleUrl: './comanda.component.scss',
})
export class ComandaComponent implements OnInit, OnDestroy, AfterViewInit {
  tipo: TipoComanda = 'COMANDA';
  identificador = '';
  /** Tipo de produto: por peso (padrão) ou preço fixo. */
  tipoProduto: TipoProduto = 'POR_PESO';
  /** Valor total (R$) a lançar para açaí/sorvete quando usa só preço (sem balança). */
  valorPorPeso = 0;
  pesoKg = 0;
  precoPorKilo = 0;
  /** Quantidade quando tipo é preço fixo. */
  quantidadePrecoFixo = 1;
  /** Preço unitário (R$) quando tipo é preço fixo. */
  valorUnitarioPrecoFixo = 0;

  /** Comanda em que estamos adicionando lançamentos (vinda do backend). */
  comandaAtual = signal<Comanda | null>(null);
  /** Lista de itens lançados nesta sessão para exibir no painel. */
  itensAtuais = signal<ItemLancado[]>([]);

  loading = signal(false);
  /** Persistência de tipo/identificador em comanda já aberta no backend. */
  salvandoCabecalho = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  showModalFechar = signal(false);
  formaPagamentoSelecionada: FormaPagamento = 'PIX';
  fechando = signal(false);
  readonly formasPagamento = FORMAS_PAGAMENTO;

  /** Modal: aviso quando clica em Editar com outro item já em edição. */
  showModalItemEmEdicao = signal(false);
  /** True quando um item foi removido para edição e ainda não foi lançado de novo. */
  itemEmEdicao = signal(false);

  /** Modal: confirmação de cancelar item. */
  showModalCancelarItem = signal(false);
  /** Item e índice pendentes de cancelamento (preenchido ao abrir o modal). */
  itemParaCancelar = signal<{ index: number; item: ItemLancado } | null>(null);

  /** Balança: estado para a UI (hint na comanda). */
  balancaStatus = signal<BalancaStatus>('desconectada');
  balancaModo = signal<BalancaModo>('manual');
  pesoBalanca = signal<number | null>(null);
  balancaConectando = signal(false);
  balancaErro = signal<string | null>(null);
  /** Evita múltiplas tentativas de reconexão automática na mesma sessão. */
  private tentouReconectarAuto = false;

  /** Caixa: exibir painel de abrir/reabrir quando o usuário tenta lançar e o caixa está fechado. */
  caixaStatusLoading = true;
  caixaNeedsAbertura = false;
  caixaFechadoHoje = false;
  showFormAbertura = false;
  valorAbertura = 0;
  caixaAberturaLoading = false;
  caixaAberturaError: string | null = null;

  private readonly destroy$ = new Subject<void>();

  /**
   * Número da comanda desta comanda em aberto (ex.: "012") para reutilizar ao voltar de Cliente/Mesa
   * sem avançar a sequência nem chamar o servidor várias vezes só por trocar o tipo.
   */
  private identificadorComandaDaSessao: string | null = null;

  @ViewChild('pesoInput') pesoInputRef: ElementRef<HTMLInputElement> | null = null;
  @ViewChild('precoFixoInput') precoFixoInputRef: ElementRef<HTMLInputElement> | null = null;
  @ViewChild('identificadorInput') identificadorInputRef: ElementRef<HTMLInputElement> | null = null;

  /** Total calculado ao vivo do próximo lançamento. */
  get valorTotal(): number {
    if (this.tipoProduto === 'PRECO_FIXO') {
      const q = Math.max(1, Math.floor(Number(this.quantidadePrecoFixo) || 0));
      const u = Number(this.valorUnitarioPrecoFixo) || 0;
      return Math.round(q * u * 100) / 100;
    }
    const p = Number(this.pesoKg) || 0;
    const k = Number(this.precoPorKilo) || 0;
    const custom = Number(this.valorPorPeso) || 0;
    if (custom > 0) return Math.round(custom * 100) / 100;
    return Math.round(p * k * 100) / 100;
  }

  /** Total da comanda em aberto (backend ou soma dos itens). */
  get totalComandaAtual(): number {
    const c = this.comandaAtual();
    if (c != null && c.valorTotal != null) return Number(c.valorTotal);
    return this.itensAtuais().reduce((s, i) => s + i.valorTotal, 0);
  }

  readonly tipos: { value: TipoComanda; label: string }[] = [
    { value: 'CLIENTE', label: 'Cliente' },
    { value: 'MESA', label: 'Mesa' },
    { value: 'COMANDA', label: 'Comanda' },
  ];

  readonly tiposProduto: { value: TipoProduto; label: string }[] = [
    { value: 'POR_PESO', label: 'Por peso (açaí/sorvete)' },
    { value: 'PRECO_FIXO', label: 'Preço fixo (padrão)' },
  ];

  /** Atalhos: Ctrl+Enter = Lançar; Alt+F = Fechar comanda (com itens); no modal: 1–4 = forma, Enter = confirmar, Esc = cancelar */
  @HostListener('document:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (this.showModalCancelarItem()) {
      if (e.key === 'Escape') {
        e.preventDefault();
        this.fecharModalCancelarItem();
        return;
      }
      if (e.key === 'Enter' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        this.confirmarCancelarItem();
        return;
      }
      return;
    }
    if (this.showModalItemEmEdicao()) {
      if (e.key === 'Escape') {
        e.preventDefault();
        this.fecharModalItemEmEdicao();
        return;
      }
      return;
    }
    if (this.showModalFechar()) {
      if (e.key === 'Escape') {
        e.preventDefault();
        this.cancelarFechar();
        return;
      }
      if (e.key === 'Enter' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        if (!this.fechando()) this.confirmarFechar();
        return;
      }
      if (['1', '2', '3', '4'].includes(e.key) && !e.ctrlKey && !e.metaKey && !e.altKey) {
        const formas: FormaPagamento[] = ['PIX', 'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO'];
        const idx = parseInt(e.key, 10) - 1;
        this.formaPagamentoSelecionada = formas[idx];
        this.onFormaPagamentoChange();
        e.preventDefault();
      }
      return;
    }
    if (e.ctrlKey && e.key === 'Enter') {
      e.preventDefault();
      if (!this.loading() && !this.salvandoCabecalho()) this.lancar();
      return;
    }
    if (e.altKey && (e.key === 'f' || e.key === 'F') && this.temComandaAberta() && this.comandaAtual()?.id) {
      e.preventDefault();
      this.abrirModalFechar();
    }
  }

  constructor(
    private comandaService: ComandaService,
    public balancaService: BalancaService,
    private caixaService: CaixaService,
    private produtoService: ProdutoService,
    private route: ActivatedRoute,
    private router: Router,
    private ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    // Restaura última opção (por peso / preço fixo) quando não está abrindo uma comanda por id
    if (!this.route.snapshot.queryParams['id']) {
      this.tipoProduto = this.comandaService.getLastTipoProduto();
    }
    this.produtoService.getPrecoKg().pipe(takeUntil(this.destroy$)).subscribe({
      next: (dto) => {
        this.precoPorKilo = dto.precoPorKilo ?? 0;
      },
    });
    this.caixaService.getStatus().pipe(takeUntil(this.destroy$)).subscribe({
      next: (s) => {
        this.caixaNeedsAbertura = s.needsAbertura;
        this.caixaFechadoHoje = s.caixaFechadoHoje ?? false;
        this.caixaStatusLoading = false;
        if (!s.needsAbertura && this.tipoProduto === 'POR_PESO') {
          this.tentarReconectarAuto();
        }
      },
      error: () => {
        this.caixaNeedsAbertura = false;
        this.caixaFechadoHoje = false;
        this.caixaStatusLoading = false;
      },
    });

    this.balancaService.status$.pipe(takeUntil(this.destroy$)).subscribe((s) => this.balancaStatus.set(s));
    this.balancaService.modo$.pipe(takeUntil(this.destroy$)).subscribe((m) => this.balancaModo.set(m));
    this.balancaService.pesoKg$.pipe(
      takeUntil(this.destroy$),
      distinctUntilChanged(),
    ).subscribe((kg) => {
      this.ngZone.run(() => {
        this.pesoBalanca.set(kg);
        if (kg != null) this.pesoKg = kg;
      });
    });

    this.route.queryParams
      .pipe(
        takeUntil(this.destroy$),
        filter((params) => params['id'] != null && params['id'] !== ''),
      )
      .subscribe((params) => {
        const id = Number(params['id']);
        if (!Number.isFinite(id)) return;
        this.comandaService.buscar(id).subscribe({
          next: (c) => {
            if (c.status === 'ABERTA') {
              this.comandaAtual.set(c);
              this.tipo = c.tipo;
              this.identificador = c.identificador ?? '';
              this.registrarIdentificadorComandaDaSessaoDe(c);
              this.tipoProduto = c.tipoProduto ?? 'POR_PESO';
              this.comandaService.setLastTipoProduto(this.tipoProduto);
              this.comandaService.listarItens(c.id!).subscribe({
                next: (itens) => {
                  const lista: ItemLancado[] = itens.map((item) => ({
                    tipoProduto: item.tipoProduto,
                    pesoKg: item.pesoKg ?? 0,
                    precoPorKilo: item.precoUnitario ?? 0,
                    valorTotal: item.valorTotal ?? 0,
                    quantidade: item.tipoProduto === 'PRECO_FIXO' ? (item.quantidade ?? 1) : undefined,
                  }));
                  this.itensAtuais.set(lista);
                },
                error: () => this.itensAtuais.set([]),
              });
            }
          },
          error: () => {
            this.error.set('Comanda não encontrada.');
          },
        });
      });

    if (!this.route.snapshot.queryParams['id'] && this.tipo === 'COMANDA') {
      this.preencherProximoIdentificador();
    }
  }

  ngAfterViewInit(): void {
    // Foca no campo de peso ou preço conforme o tipo de produto (quando o formulário está visível).
    setTimeout(() => this.focarCampoPorTipo(), 150);
  }

  /** Ao mudar tipo de produto, foca no campo correto e persiste a opção para a próxima comanda. */
  onTipoProdutoChange(): void {
    this.comandaService.setLastTipoProduto(this.tipoProduto);
    setTimeout(() => this.focarCampoPorTipo(), 50);
  }

  private focarCampoPorTipo(): void {
    if (this.tipoProduto === 'POR_PESO' && this.pesoInputRef?.nativeElement) {
      this.pesoInputRef.nativeElement.focus();
    } else if (this.tipoProduto === 'PRECO_FIXO' && this.precoFixoInputRef?.nativeElement) {
      this.precoFixoInputRef.nativeElement.focus();
    }
  }

  /** No foco, se o valor for zero, seleciona tudo para a digitação substituir (evita "010" em vez de "10"). */
  selecionarSeZero(event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = input.value?.trim();
    if (val === '' || val === '0' || Number(val) === 0) {
      input.select();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get placeholderIdentificador(): string {
    switch (this.tipo) {
      case 'CLIENTE': return 'Ex: Nome do cliente';
      case 'MESA': return 'Ex: 1, 2, 3...';
      case 'COMANDA': return 'Ex: 001, 002...';
      default: return 'Identificador';
    }
  }

  labelTipo(t: TipoComanda): string {
    return { CLIENTE: 'Cliente', MESA: 'Mesa', COMANDA: 'Comanda' }[t] || t;
  }

  /**
   * Ao mudar o tipo: com comanda já salva no backend, persiste tipo + identificador.
   * Comanda nova: Mesa/Cliente limpa identificador; Comanda sugere próximo número.
   */
  onTipoChange(novoTipo: TipoComanda): void {
    if (this.comandaAtual()?.id) {
      this.error.set(null);
      this.aplicarCabecalhoParaComandaExistente(novoTipo);
      return;
    }
    if (novoTipo === 'MESA' || novoTipo === 'CLIENTE') {
      this.identificador = '';
    } else if (novoTipo === 'COMANDA') {
      this.preencherProximoIdentificador();
    }
  }

  /** Ao sair do campo identificador, sincroniza com o servidor se a comanda já existir. */
  onIdentificadorBlur(): void {
    const id = this.comandaAtual()?.id;
    if (!id) return;
    const trimmed = this.identificador?.trim() ?? '';
    const c = this.comandaAtual()!;
    if (!trimmed) {
      if (
        this.cabecalhoDivergeDoServidor() &&
        (this.tipo === 'CLIENTE' || this.tipo === 'MESA')
      ) {
        this.error.set('Preencha o identificador e saia do campo novamente para registrar a alteração.');
        setTimeout(() => this.error.set(null), 5000);
        return;
      }
      this.identificador = c.identificador ?? '';
      this.error.set('Identificador não pode ficar vazio.');
      setTimeout(() => this.error.set(null), 4000);
      return;
    }
    if (!this.cabecalhoDivergeDoServidor()) return;
    this.persistirCabecalhoComanda();
  }

  private aplicarCabecalhoParaComandaExistente(tipoEscolhido: TipoComanda): void {
    if (tipoEscolhido === 'COMANDA') {
      const reutilizar =
        this.identificadorComandaDaSessao &&
        this.ehIdentificadorNumericoComanda(this.identificadorComandaDaSessao);
      if (reutilizar) {
        this.identificador = this.identificadorComandaDaSessao!;
        this.persistirCabecalhoComanda();
        return;
      }
      this.comandaService.proximoIdentificador('COMANDA').subscribe({
        next: (res) => {
          this.identificador = res.identificador ?? '';
          this.persistirCabecalhoComanda();
        },
        error: () => {
          this.error.set('Não foi possível obter o próximo número de comanda.');
          void this.resyncCabecalhoDaComanda();
        },
      });
      return;
    }
    if (tipoEscolhido === 'CLIENTE' || tipoEscolhido === 'MESA') {
      const snap = this.comandaAtual();
      if (snap?.tipo === 'COMANDA' && this.ehIdentificadorNumericoComanda(snap.identificador)) {
        this.identificadorComandaDaSessao = snap.identificador!.trim();
      }
      this.identificador = '';
      this.success.set(
        tipoEscolhido === 'CLIENTE'
          ? 'Informe o nome do cliente e saia do campo para salvar.'
          : 'Informe o número da mesa e saia do campo para salvar.',
      );
      setTimeout(() => this.success.set(null), 4500);
      setTimeout(() => this.identificadorInputRef?.nativeElement?.focus(), 0);
      return;
    }
  }

  /** True quando o formulário difere do que está gravado no servidor (comanda já existente). */
  private cabecalhoDivergeDoServidor(): boolean {
    const c = this.comandaAtual();
    if (!c?.id) return false;
    const ident = (this.identificador ?? '').trim();
    const serverIdent = (c.identificador ?? '').trim();
    return this.tipo !== c.tipo || ident !== serverIdent;
  }

  private persistirCabecalhoComanda(): void {
    const id = this.comandaAtual()?.id;
    if (!id) return;
    const ident = this.identificador?.trim() ?? '';
    if (!ident) return;
    this.salvandoCabecalho.set(true);
    this.comandaService.alterarCabecalho(id, { tipo: this.tipo, identificador: ident }).subscribe({
      next: (c) => {
        this.salvandoCabecalho.set(false);
        this.comandaAtual.set(c);
        this.tipo = c.tipo;
        this.identificador = c.identificador ?? '';
        this.registrarIdentificadorComandaDaSessaoDe(c);
        this.success.set('Tipo e identificador atualizados.');
        setTimeout(() => this.success.set(null), 2000);
      },
      error: (err) => {
        this.salvandoCabecalho.set(false);
        const body = err?.error;
        const msg =
          typeof body?.message === 'string'
            ? body.message
            : typeof body === 'string'
              ? body
              : 'Não foi possível atualizar tipo/identificador.';
        this.error.set(msg);
        void this.resyncCabecalhoDaComanda();
      },
    });
  }

  private resyncCabecalhoDaComanda(): void {
    const id = this.comandaAtual()?.id;
    if (!id) return;
    this.comandaService.buscar(id).subscribe({
      next: (c) => {
        if (c.status === 'ABERTA') {
          this.comandaAtual.set(c);
          this.tipo = c.tipo;
          this.identificador = c.identificador ?? '';
          this.registrarIdentificadorComandaDaSessaoDe(c);
        }
      },
      error: () => {},
    });
  }

  /** Identificador usado como número de comanda (apenas dígitos, ex.: 001 ou 12). */
  private ehIdentificadorNumericoComanda(raw: string | null | undefined): boolean {
    if (!raw) return false;
    return /^\d{1,6}$/.test(raw.trim());
  }

  private registrarIdentificadorComandaDaSessaoDe(c: Comanda | null | undefined): void {
    if (c?.tipo === 'COMANDA' && this.ehIdentificadorNumericoComanda(c.identificador)) {
      this.identificadorComandaDaSessao = c.identificador!.trim();
    }
  }

  /** Busca o próximo identificador sequencial para tipo COMANDA e preenche o campo. */
  preencherProximoIdentificador(): void {
    if (this.tipo !== 'COMANDA') return;
    this.comandaService.proximoIdentificador('COMANDA').subscribe({
      next: (res) => {
        this.identificador = res.identificador ?? '';
      },
    });
  }

  lancar(): void {
    this.error.set(null);
    this.success.set(null);

    const isPrecoFixo = this.tipoProduto === 'PRECO_FIXO';
    const qtdFixo = Math.max(1, Math.floor(Number(this.quantidadePrecoFixo) || 0));
    const unitFixo = Number(this.valorUnitarioPrecoFixo) || 0;

    if (!this.identificador?.trim()) {
      this.error.set('Informe o identificador.');
      return;
    }
    if (isPrecoFixo) {
      if (!unitFixo || unitFixo <= 0) {
        this.error.set('Informe o preço (R$) válido.');
        return;
      }
    } else {
      const peso = Number(this.pesoKg) || 0;
      const preco = Number(this.precoPorKilo) || 0;
      if (!peso || peso <= 0) {
        this.error.set('Informe o peso (kg) válido.');
        return;
      }
      if (!preco || preco <= 0) {
        this.error.set('Informe o preço por kilo válido.');
        return;
      }
    }

    const valorItem = isPrecoFixo
      ? Math.round(qtdFixo * unitFixo * 100) / 100
      : (() => {
          const custom = Number(this.valorPorPeso) || 0;
          if (custom > 0) return Math.round(custom * 100) / 100;
          return Math.round((Number(this.pesoKg) || 0) * (Number(this.precoPorKilo) || 0) * 100) / 100;
        })();

    const payload = {
      tipo: this.tipo,
      identificador: this.identificador.trim(),
      tipoProduto: this.tipoProduto,
      pesoKg: isPrecoFixo ? 0 : (Number(this.pesoKg) || 0),
      precoPorKilo: isPrecoFixo ? unitFixo : (Number(this.precoPorKilo) || 0),
      valorTotal: valorItem,
      ...(isPrecoFixo && { quantidade: qtdFixo }),
    };

    const idExistente = this.comandaAtual()?.id;
    const precisaSincronizarCabecalho = !!idExistente && this.cabecalhoDivergeDoServidor();

    this.loading.set(true);

    const aposCriar = (c: Comanda) => {
      this.loading.set(false);
      this.itemEmEdicao.set(false);
      this.comandaAtual.set(c);
      this.registrarIdentificadorComandaDaSessaoDe(c);
      this.itensAtuais.update((list) => [
        ...list,
        {
          tipoProduto: this.tipoProduto,
          pesoKg: isPrecoFixo ? 0 : (Number(this.pesoKg) || 0),
          precoPorKilo: isPrecoFixo ? unitFixo : (Number(this.precoPorKilo) || 0),
          valorTotal: valorItem,
          quantidade: isPrecoFixo ? qtdFixo : undefined,
        },
      ]);
      this.success.set('Lançado!');
      setTimeout(() => this.success.set(null), 2000);
      this.limparApenasPesoPreco();
      setTimeout(() => this.focarCampoPorTipo(), 50);
    };

    const onErroCriar = (err: unknown) => {
      this.loading.set(false);
      this.error.set(
        (err as { error?: { message?: string } })?.error?.message ||
          'Erro ao lançar. Verifique se o backend está rodando.',
      );
    };

    if (precisaSincronizarCabecalho) {
      this.comandaService
        .alterarCabecalho(idExistente!, { tipo: this.tipo, identificador: payload.identificador })
        .pipe(
          switchMap((cAtualizada) => {
            this.comandaAtual.set(cAtualizada);
            return this.comandaService.criar(payload);
          }),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: aposCriar,
          error: (err) => {
            this.loading.set(false);
            const body = (err as { error?: { message?: string } })?.error;
            this.error.set(
              typeof body?.message === 'string'
                ? body.message
                : 'Não foi possível atualizar tipo/identificador antes de lançar.',
            );
            void this.resyncCabecalhoDaComanda();
          },
        });
      return;
    }

    this.comandaService
      .criar(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: aposCriar,
        error: onErroCriar,
      });
  }

  private limparApenasPesoPreco(): void {
    this.pesoKg = 0;
    this.valorPorPeso = 0;
    this.quantidadePrecoFixo = 1;
    this.valorUnitarioPrecoFixo = 0;
  }

  novaComanda(): void {
    this.itemEmEdicao.set(false);
    this.identificadorComandaDaSessao = null;
    this.comandaAtual.set(null);
    this.itensAtuais.set([]);
    this.tipo = 'COMANDA';
    this.identificador = '';
    // Mantém tipoProduto (por peso / preço fixo) como estava; precoPorKilo vem do cadastro (Produtos).
    this.valorPorPeso = 0;
    this.pesoKg = 0;
    this.produtoService.getPrecoKg().pipe(takeUntil(this.destroy$)).subscribe({
      next: (dto) => { this.precoPorKilo = dto.precoPorKilo ?? 0; },
    });
    this.quantidadePrecoFixo = 1;
    this.valorUnitarioPrecoFixo = 0;
    this.error.set(null);
    this.success.set(null);
    this.showModalFechar.set(false);
    this.preencherProximoIdentificador();
    setTimeout(() => this.focarCampoPorTipo(), 100);
  }

  abrirModalFechar(): void {
    if (!this.comandaAtual()?.id) return;
    this.showModalFechar.set(true);
    this.formaPagamentoSelecionada = 'PIX';
  }

  cancelarFechar(): void {
    this.showModalFechar.set(false);
  }

  fecharModalItemEmEdicao(): void {
    this.showModalItemEmEdicao.set(false);
  }

  onFormaPagamentoChange(): void {}

  confirmarFechar(): void {
    const c = this.comandaAtual();
    if (!c?.id) return;
    this.fechando.set(true);
    this.comandaService.fechar(c.id, this.formaPagamentoSelecionada).subscribe({
      next: () => {
        this.fechando.set(false);
        this.showModalFechar.set(false);
        this.novaComanda();
      },
      error: (err) => {
        this.fechando.set(false);
        this.error.set(err?.error?.message || 'Erro ao fechar comanda.');
      },
    });
  }

  /** Remove o item da comanda no backend e da lista local. Abre modal de confirmação. */
  cancelarItem(index: number, item: ItemLancado): void {
    this.itemParaCancelar.set({ index, item });
    this.showModalCancelarItem.set(true);
  }

  fecharModalCancelarItem(): void {
    this.showModalCancelarItem.set(false);
    this.itemParaCancelar.set(null);
  }

  /** Confirma o cancelamento do item (chamado pelo modal). */
  confirmarCancelarItem(): void {
    const pending = this.itemParaCancelar();
    if (!pending) return;
    const { index, item } = pending;
    const c = this.comandaAtual();
    if (!c?.id) return;
    this.fecharModalCancelarItem();
    this.error.set(null);
    this.comandaService.removerItem(c.id, item.pesoKg, item.valorTotal).subscribe({
      next: (atualizada) => {
        this.comandaAtual.set(atualizada);
        this.itensAtuais.update((list) => list.filter((_, i) => i !== index));
        this.success.set('Item cancelado.');
        setTimeout(() => this.success.set(null), 2000);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erro ao cancelar item.');
      },
    });
  }

  /** Remove o item da comanda e preenche o formulário para o usuário editar e lançar de novo. */
  editarItem(index: number, item: ItemLancado): void {
    const c = this.comandaAtual();
    if (!c?.id) return;
    if (this.itemEmEdicao()) {
      this.showModalItemEmEdicao.set(true);
      return;
    }
    this.error.set(null);
    this.comandaService.removerItem(c.id, item.pesoKg, item.valorTotal).subscribe({
      next: (atualizada) => {
        this.comandaAtual.set(atualizada);
        this.itensAtuais.update((list) => list.filter((_, i) => i !== index));
        this.itemEmEdicao.set(true);
        this.tipoProduto = item.tipoProduto ?? 'POR_PESO';
        this.pesoKg = item.pesoKg ?? 0;
        this.precoPorKilo = item.precoPorKilo ?? 0;
        this.valorPorPeso = item.tipoProduto === 'POR_PESO' ? (item.valorTotal ?? 0) : 0;
        this.quantidadePrecoFixo = item.tipoProduto === 'PRECO_FIXO' ? (item.quantidade ?? 1) : 1;
        this.valorUnitarioPrecoFixo = item.tipoProduto === 'PRECO_FIXO' ? (item.precoPorKilo ?? 0) : 0;
        this.success.set('Edite os valores e clique em Lançar para registrar novamente.');
        setTimeout(() => this.success.set(null), 3000);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erro ao editar item.');
      },
    });
  }

  temComandaAberta(): boolean {
    return this.comandaAtual() != null || this.itensAtuais().length > 0;
  }

  voltarParaInicio(): void {
    this.router.navigate(['/']);
  }

  abrirCaixaComanda(): void {
    this.caixaAberturaError = null;
    this.caixaAberturaLoading = true;
    const valor = Number(this.valorAbertura);
    if (isNaN(valor) || valor < 0) {
      this.caixaAberturaError = 'Informe um valor válido.';
      this.caixaAberturaLoading = false;
      return;
    }
    this.caixaService.abrir(valor).subscribe({
      next: () => {
        this.caixaAberturaLoading = false;
        this.caixaService.refreshStatus();
        this.caixaNeedsAbertura = false;
        this.showFormAbertura = false;
        if (this.tipoProduto === 'POR_PESO') this.tentarReconectarAuto();
      },
      error: (err) => {
        this.caixaAberturaLoading = false;
        this.caixaAberturaError = err?.error?.message || err?.error || 'Erro ao abrir caixa.';
      },
    });
  }

  /** Conecta à balança usando a configuração salva (usuário escolhe a porta no diálogo). */
  async conectarBalancaNaComanda(): Promise<void> {
    this.balancaErro.set(null);
    this.balancaConectando.set(true);
    this.produtoService.getConfigBalanca().subscribe({
      next: async (config) => {
        try {
          const dataBits = config.serialConfig === '8e2' ? 8 : 8;
          const stopBits = config.serialConfig === '8e2' ? 2 : 1;
          const parity = config.serialConfig === '8e2' ? 'even' as const : 'none' as const;
          await this.balancaService.conectarSerial({
            baudRate: config.baudRate ?? 4800,
            dataBits,
            stopBits,
            parity,
            enviarEnq: config.enviarEnq !== false,
          });
        } catch (err) {
          this.ngZone.run(() => {
            this.balancaErro.set(err instanceof Error ? err.message : 'Erro ao conectar.');
          });
        }
        this.ngZone.run(() => this.balancaConectando.set(false));
      },
      error: () => this.ngZone.run(() => { this.balancaConectando.set(false); this.balancaErro.set('Erro ao carregar configuração.'); }),
    });
  }

  /** Desconecta a balança (serial). */
  async desconectarBalancaNaComanda(): Promise<void> {
    await this.balancaService.desconectar();
    this.pesoBalanca.set(null);
  }

  /**
   * Tenta reconectar à balança automaticamente (porta já autorizada).
   * Chamado ao abrir o caixa ou ao entrar na comanda com caixa já aberto.
   */
  tentarReconectarAuto(): void {
    if (this.tentouReconectarAuto) return;
    if (this.balancaStatus() === 'conectada' && this.balancaModo() === 'serial') return;
    if (!this.balancaService.serialDisponivel) return;
    this.tentouReconectarAuto = true;
    this.produtoService.getConfigBalanca().subscribe({
      next: (config) => {
        const dataBits = config.serialConfig === '8e2' ? 8 : 8;
        const stopBits = config.serialConfig === '8e2' ? 2 : 1;
        const parity = config.serialConfig === '8e2' ? 'even' as const : 'none' as const;
        this.balancaService.tentarReconectar({
          baudRate: config.baudRate ?? 4800,
          dataBits,
          stopBits,
          parity,
          enviarEnq: config.enviarEnq !== false,
        }).then((ok) => {
          if (!ok) this.tentouReconectarAuto = false;
        });
      },
      error: () => { this.tentouReconectarAuto = false; },
    });
  }
}
