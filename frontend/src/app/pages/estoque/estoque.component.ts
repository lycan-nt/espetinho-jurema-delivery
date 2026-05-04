import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiBackendService } from '../../core/api-backend.service';
import { Produto } from '../../models/api.models';

@Component({
  selector: 'app-estoque',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './estoque.component.html',
  styleUrl: './estoque.component.scss',
})
export class EstoqueComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  produtos: Produto[] = [];
  estoqueObrigatorio = false;
  carregando = true;
  erro: string | null = null;

  produtoEntradaId: number | null = null;
  qtdEntrada = 1;
  refEntrada = '';

  produtoAjusteId: number | null = null;
  novoSaldo = 0;

  salvandoConfig = false;
  salvandoEntrada = false;
  salvandoAjuste = false;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    forkJoin([this.api.getEstoqueConfig(), this.api.getProdutos()]).subscribe({
      next: ([c, p]) => {
        this.estoqueObrigatorio = c.estoqueObrigatorio;
        this.produtos = p;
        this.produtoEntradaId = p[0]?.id ?? null;
        this.produtoAjusteId = p[0]?.id ?? null;
        this.novoSaldo = p[0]?.saldoEstoque ?? 0;
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar dados.';
      },
    });
  }

  onObrigatorioChange(valor: boolean): void {
    this.salvandoConfig = true;
    this.erro = null;
    this.api.patchEstoqueConfig(valor).subscribe({
      next: (c) => {
        this.estoqueObrigatorio = c.estoqueObrigatorio;
        this.salvandoConfig = false;
      },
      error: (e) => {
        this.salvandoConfig = false;
        this.erro = e?.error?.erro ?? 'Erro ao salvar.';
        this.api.getEstoqueConfig().subscribe((c) => (this.estoqueObrigatorio = c.estoqueObrigatorio));
      },
    });
  }

  registrarEntrada(): void {
    if (this.produtoEntradaId == null || this.qtdEntrada < 1) {
      return;
    }
    this.salvandoEntrada = true;
    this.erro = null;
    this.api.entradaEstoque(this.produtoEntradaId, this.qtdEntrada, this.refEntrada || null).subscribe({
      next: () => {
        this.salvandoEntrada = false;
        this.refEntrada = '';
        this.qtdEntrada = 1;
        this.carregar();
      },
      error: (e) => {
        this.salvandoEntrada = false;
        this.erro = e?.error?.erro ?? 'Erro na entrada.';
      },
    });
  }

  aplicarAjuste(): void {
    if (this.produtoAjusteId == null) {
      return;
    }
    this.salvandoAjuste = true;
    this.erro = null;
    this.api.ajusteEstoque(this.produtoAjusteId, this.novoSaldo).subscribe({
      next: () => {
        this.salvandoAjuste = false;
        this.carregar();
      },
      error: (e) => {
        this.salvandoAjuste = false;
        this.erro = e?.error?.erro ?? 'Erro no ajuste.';
      },
    });
  }

  onSelectAjuste(): void {
    const p = this.produtos.find((x) => x.id === this.produtoAjusteId);
    if (p) {
      this.novoSaldo = p.saldoEstoque ?? 0;
    }
  }
}
