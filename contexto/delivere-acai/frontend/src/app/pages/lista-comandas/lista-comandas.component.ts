import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ComandaService } from '../../services/comanda.service';
import { Comanda, TipoComanda, FormaPagamento } from '../../models/comanda.model';

const FORMAS_PAGAMENTO: { value: FormaPagamento; label: string }[] = [
  { value: 'PIX', label: 'PIX' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
];

@Component({
  selector: 'app-lista-comandas',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink, FormsModule],
  templateUrl: './lista-comandas.component.html',
  styleUrl: './lista-comandas.component.scss',
})
export class ListaComandasComponent implements OnInit {
  comandas: Comanda[] = [];
  apenasAbertas = true;
  loading = true;
  error: string | null = null;
  comandaParaFechar: Comanda | null = null;
  formaPagamentoSelecionada: FormaPagamento = 'PIX';
  fechando = false;
  readonly formasPagamento = FORMAS_PAGAMENTO;
  /** ID da comanda cuja NFC-e está sendo emitida (loading). */
  emittingNfceId: number | null = null;
  /** Mensagem de erro ao emitir NFC-e. */
  nfceError: string | null = null;

  /** Atalhos no modal fechar: 1–4 = forma, Enter = confirmar, Esc = cancelar */
  @HostListener('document:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (!this.comandaParaFechar) return;
    if (e.key === 'Escape') {
      e.preventDefault();
      this.cancelarFechar();
      return;
    }
    if (e.key === 'Enter' && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      if (!this.fechando) this.confirmarFechar();
      return;
    }
    if (['1', '2', '3', '4'].includes(e.key) && !e.ctrlKey && !e.metaKey && !e.altKey) {
      const formas: FormaPagamento[] = ['PIX', 'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO'];
      const idx = parseInt(e.key, 10) - 1;
      this.formaPagamentoSelecionada = formas[idx];
      e.preventDefault();
    }
  }

  constructor(
    private comandaService: ComandaService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading = true;
    this.error = null;
    this.comandaService.listar(this.apenasAbertas).subscribe({
      next: (list) => {
        this.comandas = list;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erro ao carregar comandas.';
        this.loading = false;
      },
    });
  }

  toggleFiltro(): void {
    this.apenasAbertas = !this.apenasAbertas;
    this.carregar();
  }

  /** Navega para a tela de lançamento para continuar adicionando itens à comanda em aberto. */
  continuarComanda(c: Comanda): void {
    if (c.status !== 'ABERTA' || c.id == null) return;
    this.router.navigate(['/comanda'], { queryParams: { id: c.id } });
  }

  abrirModalFechar(c: Comanda): void {
    if (!c.id || c.status === 'FECHADA') return;
    this.comandaParaFechar = c;
    this.formaPagamentoSelecionada = 'PIX';
  }

  cancelarFechar(): void {
    this.comandaParaFechar = null;
  }

  onFormaPagamentoChange(): void {}

  confirmarFechar(): void {
    if (!this.comandaParaFechar?.id) return;
    this.fechando = true;
    this.comandaService.fechar(this.comandaParaFechar.id, this.formaPagamentoSelecionada).subscribe({
      next: () => {
        this.comandaParaFechar = null;
        this.fechando = false;
        this.carregar();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erro ao fechar comanda.';
        this.fechando = false;
      },
    });
  }

  labelTipo(tipo: TipoComanda): string {
    return { CLIENTE: 'Cliente', MESA: 'Mesa', COMANDA: 'Comanda' }[tipo] || tipo;
  }

  formatarData(s?: string): string {
    if (!s) return '-';
    const d = new Date(s);
    return d.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  labelFormaPagamento(forma: FormaPagamento): string {
    return this.formasPagamento.find(f => f.value === forma)?.label || forma;
  }

  emitirNfce(c: Comanda): void {
    if (!c.id || c.status !== 'FECHADA') return;
    this.nfceError = null;
    this.emittingNfceId = c.id;
    this.comandaService.emitirNfce(c.id).subscribe({
      next: (atualizada) => {
        this.emittingNfceId = null;
        this.comandas = this.comandas.map(x => (x.id === atualizada.id ? atualizada : x));
      },
      error: (err) => {
        this.emittingNfceId = null;
        this.nfceError = err?.error?.message || err?.error || 'Erro ao emitir NFC-e.';
      },
    });
  }
}
