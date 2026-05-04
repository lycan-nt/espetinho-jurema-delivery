import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ComandaService } from '../../services/comanda.service';
import { Comanda, ComandaItem, FormaPagamento, TipoProduto } from '../../models/comanda.model';
import { Caixa } from '../../models/caixa.model';

const LABELS_FORMA: Record<FormaPagamento, string> = {
  PIX: 'PIX',
  DINHEIRO: 'Dinheiro',
  CARTAO_CREDITO: 'Cartão de crédito',
  CARTAO_DEBITO: 'Cartão de débito',
};

function hojeISO(): string {
  const d = new Date();
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}

@Component({
  selector: 'app-relatorio',
  standalone: true,
  imports: [CommonModule, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './relatorio.component.html',
  styleUrl: './relatorio.component.scss',
})
export class RelatorioComponent implements OnInit {
  comandas: Comanda[] = [];
  caixas: Caixa[] = [];
  totalVendas = 0;
  totalPorForma: { forma: string; total: number }[] = [];
  loading = true;
  error: string | null = null;
  /** True quando o backend retorna 403 (sem permissão para Gestão). */
  errorPermissao = false;

  dataInicio = hojeISO();
  dataFim = hojeISO();
  /** Filtro por tipo de produto: todos, só por peso ou só preço fixo. */
  filtroTipoProduto: 'TODOS' | TipoProduto = 'TODOS';
  /** Comanda cuja tela de detalhes está aberta. */
  comandaDetalhe: Comanda | null = null;
  /** Itens da comanda em detalhe (preenchido ao abrir). */
  itensDetalhe: ComandaItem[] = [];
  /** Envio para Google Sheets em andamento. */
  enviandoSheets = false;
  /** Mensagem de sucesso/erro do envio para Google Sheets. */
  mensagemSheets: string | null = null;

  /** Comandas após aplicar filtro de tipo de produto. */
  get comandasFiltradas(): Comanda[] {
    if (this.filtroTipoProduto === 'TODOS') return this.comandas;
    return this.comandas.filter(c => (c.tipoProduto ?? 'POR_PESO') === this.filtroTipoProduto);
  }

  /** Total de vendas exibido (respeitando filtro de tipo). */
  get totalVendasExibido(): number {
    return this.comandasFiltradas.reduce((s, c) => s + (Number(c.valorTotal) || 0), 0);
  }

  /** Total por forma de pagamento (respeitando filtro de tipo). */
  get totalPorFormaExibido(): { forma: string; total: number }[] {
    const map: Record<string, number> = {};
    this.comandasFiltradas.forEach(c => {
      const forma = c.formaPagamento ? LABELS_FORMA[c.formaPagamento] || c.formaPagamento : 'Outros';
      map[forma] = (map[forma] || 0) + (Number(c.valorTotal) || 0);
    });
    return Object.entries(map).map(([forma, total]) => ({ forma, total }));
  }

  constructor(public comandaService: ComandaService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading = true;
    this.error = null;
    this.errorPermissao = false;
    this.comandaService.relatorio(this.dataInicio, this.dataFim).subscribe({
      next: (r) => {
        this.comandas = r.comandas ?? [];
        this.caixas = r.caixas ?? [];
        this.totalVendas = Number(r.totalVendas) || 0;
        const map = r.totalPorFormaPagamento || {};
        this.totalPorForma = (Object.keys(map) as FormaPagamento[])
          .filter(forma => map[forma] != null && Number(map[forma]) > 0)
          .map(forma => ({
            forma: LABELS_FORMA[forma] || forma,
            total: Number(map[forma]),
          }));
        this.loading = false;
      },
      error: (err) => {
        this.errorPermissao = err?.status === 403;
        this.error = this.errorPermissao ? null : (err?.error?.message || 'Erro ao carregar dados.');
        this.loading = false;
      },
    });
  }

  aplicarFiltro(): void {
    if (!this.dataInicio) this.dataInicio = hojeISO();
    if (!this.dataFim) this.dataFim = this.dataInicio;
    if (this.dataFim < this.dataInicio) this.dataFim = this.dataInicio;
    this.carregar();
  }

  filtrarHoje(): void {
    this.dataInicio = hojeISO();
    this.dataFim = hojeISO();
    this.carregar();
  }

  get textoPeriodo(): string {
    const di = this.dataInicio ? this.formatarDataExibicao(this.dataInicio) : '';
    const df = this.dataFim ? this.formatarDataExibicao(this.dataFim) : '';
    if (!di && !df) return 'Período: hoje';
    if (di === df) return 'Período: ' + di;
    return 'Período: ' + di + ' a ' + df;
  }

  private formatarDataExibicao(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  labelTipo(tipo: string): string {
    return { CLIENTE: 'Cliente', MESA: 'Mesa', COMANDA: 'Comanda' }[tipo] || tipo;
  }

  formatarData(s?: string): string {
    if (!s) return '—';
    return new Date(s).toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /** Formata apenas data (yyyy-MM-dd) para exibição. */
  formatarDataCaixa(data?: string): string {
    if (!data) return '—';
    const d = new Date(data + 'T12:00:00');
    if (isNaN(d.getTime())) return data;
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  labelForma(forma?: FormaPagamento): string {
    if (!forma) return '-';
    return LABELS_FORMA[forma] ?? forma;
  }

  labelTipoProduto(tipo?: TipoProduto): string {
    return tipo === 'PRECO_FIXO' ? 'Preço fixo' : 'Por peso';
  }

  abrirDetalhe(c: Comanda): void {
    this.comandaDetalhe = c;
    this.itensDetalhe = [];
    if (c.id) {
      this.comandaService.listarItens(c.id).subscribe({
        next: (itens) => (this.itensDetalhe = itens),
        error: () => (this.itensDetalhe = []),
      });
    }
  }

  fecharDetalhe(): void {
    this.comandaDetalhe = null;
    this.itensDetalhe = [];
  }

  imprimir(): void {
    window.print();
  }

  enviarGoogleSheets(): void {
    this.mensagemSheets = null;
    this.errorPermissao = false;
    this.enviandoSheets = true;
    const di = this.dataInicio || hojeISO();
    const df = this.dataFim || this.dataInicio || di;
    this.comandaService.enviarParaGoogleSheets(di, df).subscribe({
      next: (res) => {
        this.enviandoSheets = false;
        this.mensagemSheets = res?.message || 'Relatório enviado para o Google Sheets.';
        setTimeout(() => (this.mensagemSheets = null), 5000);
      },
      error: (err) => {
        this.enviandoSheets = false;
        this.errorPermissao = err?.status === 403;
        this.mensagemSheets = this.errorPermissao ? null : (err?.error?.message || err?.error || 'Erro ao enviar para Google Sheets.');
      },
    });
  }
}
