import { DecimalPipe } from '@angular/common';
import { afterNextRender, Component, ElementRef, Injector, OnDestroy, OnInit, inject, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Chart } from 'chart.js/auto';
import { ApiBackendService } from '../../core/api-backend.service';
import { periodoDiaLocal } from '../../core/periodo.util';
import { FaturamentoResumo, FormaPagamento } from '../../models/api.models';

const ROTULO_FORMA: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'Pix',
  DEBITO: 'Débito',
  CREDITO: 'Crédito',
  OUTRO: 'Outro',
};

const CORES_GRAFICO = [
  '#e63900',
  '#ff922b',
  '#fcc419',
  '#69db7c',
  '#38d9a9',
  '#4dabf7',
  '#748ffc',
  '#da77f2',
  '#ff8787',
  '#20c997',
  '#ffa8a8',
  '#a5d8ff',
];

@Component({
  selector: 'app-relatorio-faturamento',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './relatorio-faturamento.component.html',
  styleUrl: './relatorio-faturamento.component.scss',
})
export class RelatorioFaturamentoComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiBackendService);
  private readonly injector = inject(Injector);
  private readonly canvasForma = viewChild<ElementRef<HTMLCanvasElement>>('canvasForma');
  private readonly canvasProduto = viewChild<ElementRef<HTMLCanvasElement>>('canvasProduto');

  private charts: Chart[] = [];

  readonly rotuloForma = ROTULO_FORMA;

  dataInicio = '';
  dataFim = '';
  resumo: FaturamentoResumo | null = null;
  carregando = false;
  erro: string | null = null;

  ngOnInit(): void {
    const hoje = new Date();
    const s = this.toInputDate(hoje);
    this.dataInicio = s;
    this.dataFim = s;
    this.buscar();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  temDadosForma(): boolean {
    return !!this.resumo?.porForma?.some((x) => x.total > 0);
  }

  temDadosProduto(): boolean {
    return !!this.resumo?.porProduto?.some((x) => x.total > 0);
  }

  produtosComVenda() {
    return (this.resumo?.porProduto ?? []).filter((x) => x.total > 0);
  }

  private toInputDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private parseInputDate(s: string): Date {
    const [y, m, d] = s.split('-').map(Number);
    return new Date(y, (m ?? 1) - 1, d ?? 1);
  }

  private fmtBrl(n: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(n);
  }

  buscar(): void {
    if (!this.dataInicio || !this.dataFim) {
      return;
    }
    const di = this.parseInputDate(this.dataInicio);
    const df = this.parseInputDate(this.dataFim);
    if (df < di) {
      this.erro = 'A data final deve ser igual ou posterior à inicial.';
      return;
    }
    const { inicio } = periodoDiaLocal(di);
    const { fim } = periodoDiaLocal(df);
    this.carregando = true;
    this.erro = null;
    this.api.getFaturamentoResumo(inicio, fim).subscribe({
      next: (r) => {
        this.resumo = {
          ...r,
          porProduto: r.porProduto ?? [],
        };
        this.carregando = false;
        this.scheduleCharts();
      },
      error: (e) => {
        this.carregando = false;
        this.erro = e?.error?.erro ?? 'Não foi possível carregar o relatório.';
      },
    });
  }

  private scheduleCharts(): void {
    afterNextRender(
      () => {
        this.destroyCharts();
        const r = this.resumo;
        if (!r) {
          return;
        }
        const elF = this.canvasForma()?.nativeElement;
        const elP = this.canvasProduto()?.nativeElement;
        if (elF && this.temDadosForma()) {
          const rows = r.porForma.filter((x) => x.total > 0);
          const c = this.pieChart(
            elF,
            rows.map((x) => this.rotuloForma[x.forma]),
            rows.map((x) => x.total),
          );
          this.charts.push(c);
        }
        if (elP && this.temDadosProduto()) {
          const { labels, values } = this.agruparProdutosParaGrafico(r);
          const c = this.pieChart(elP, labels, values);
          this.charts.push(c);
        }
      },
      { injector: this.injector },
    );
  }

  /** Top 10 produtos; demais agregados em "Outros" para o gráfico continuar legível. */
  private agruparProdutosParaGrafico(r: FaturamentoResumo): { labels: string[]; values: number[] } {
    const rows = [...(r.porProduto ?? [])]
      .filter((x) => x.total > 0)
      .sort((a, b) => b.total - a.total);
    const top = rows.slice(0, 10);
    const rest = rows.slice(10);
    const labels = top.map((x) => x.produtoNome);
    const values = top.map((x) => x.total);
    const outros = rest.reduce((s, x) => s + x.total, 0);
    if (outros > 0) {
      labels.push('Outros');
      values.push(outros);
    }
    return { labels, values };
  }

  private pieChart(canvas: HTMLCanvasElement, labels: string[], values: number[]): Chart {
    const colors = labels.map((_, i) => CORES_GRAFICO[i % CORES_GRAFICO.length]);
    const sum = values.reduce((a, b) => a + b, 0);
    return new Chart(canvas, {
      type: 'pie',
      data: {
        labels,
        datasets: [
          {
            data: values,
            backgroundColor: colors,
            borderColor: '#1a1d21',
            borderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        aspectRatio: 1.1,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: '#9aa3ad',
              boxWidth: 14,
              padding: 10,
              font: { size: 11 },
              generateLabels: (chart) => {
                const ds = chart.data.datasets[0];
                const dataArr = ds.data as number[];
                const lbls = chart.data.labels as string[];
                const bg = ds.backgroundColor as string[];
                return dataArr.map((v, i) => {
                  const pct = sum > 0 ? ((v / sum) * 100).toFixed(1) : '0.0';
                  return {
                    text: `${lbls[i]} — ${pct}% · ${this.fmtBrl(v)}`,
                    fillStyle: bg[i],
                    strokeStyle: bg[i],
                    fontColor: '#9aa3ad',
                    hidden: false,
                    index: i,
                  };
                });
              },
            },
          },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const v = typeof ctx.raw === 'number' ? ctx.raw : 0;
                const pct = sum > 0 ? ((v / sum) * 100).toFixed(1) : '0.0';
                return `${pct}% · ${this.fmtBrl(v)}`;
              },
            },
          },
        },
      },
    });
  }

  private destroyCharts(): void {
    for (const c of this.charts) {
      c.destroy();
    }
    this.charts = [];
  }

  /** Soma do faturamento por produto (pedidos pagos no período); para % na tabela. */
  totalPorProduto(): number {
    const rows = this.resumo?.porProduto ?? [];
    return rows.reduce((s, x) => s + (x.total > 0 ? x.total : 0), 0);
  }

  pctProduto(valor: number): string {
    const t = this.totalPorProduto();
    if (t <= 0) {
      return '0,0';
    }
    return ((valor / t) * 100).toFixed(1).replace('.', ',');
  }

  imprimir(): void {
    window.print();
  }
}
