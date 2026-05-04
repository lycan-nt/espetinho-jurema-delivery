import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProdutoService } from '../../services/produto.service';

@Component({
  selector: 'app-produtos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './produtos.component.html',
  styleUrl: './produtos.component.scss',
})
export class ProdutosComponent implements OnInit {
  precoPorKilo = 0;
  loading = true;
  salvando = false;
  error: string | null = null;
  sucesso: string | null = null;
  errorPermissao = false;

  constructor(private produtoService: ProdutoService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading = true;
    this.error = null;
    this.errorPermissao = false;
    this.produtoService.getPrecoKg().subscribe({
      next: (dto) => {
        this.precoPorKilo = dto.precoPorKilo ?? 0;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorPermissao = err?.status === 403;
        this.error = this.errorPermissao ? '' : (err?.error?.message || 'Erro ao carregar.');
      },
    });
  }

  salvar(): void {
    this.error = null;
    this.sucesso = null;
    this.salvando = true;
    this.produtoService.setPrecoKg(this.precoPorKilo).subscribe({
      next: (dto) => {
        this.precoPorKilo = dto.precoPorKilo ?? 0;
        this.salvando = false;
        this.sucesso = 'Preço do kilo salvo.';
        setTimeout(() => (this.sucesso = null), 3000);
      },
      error: (err) => {
        this.salvando = false;
        this.errorPermissao = err?.status === 403;
        this.error = this.errorPermissao ? '' : (err?.error?.message || 'Erro ao salvar.');
      },
    });
  }
}
