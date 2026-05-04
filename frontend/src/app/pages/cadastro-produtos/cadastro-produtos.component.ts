import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiBackendService } from '../../core/api-backend.service';
import { CategoriaCardapio, Produto } from '../../models/api.models';

@Component({
  selector: 'app-cadastro-produtos',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './cadastro-produtos.component.html',
  styleUrl: './cadastro-produtos.component.scss',
})
export class CadastroProdutosComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  categorias: CategoriaCardapio[] = [];
  lista: Produto[] = [];

  carregando = false;
  erro: string | null = null;

  novoNome = '';
  novaDesc = '';
  novoPreco: number | null = null;
  novoCatId: number | null = null;
  novoCod = '';
  novoAtivo = true;
  salvandoNovo = false;

  editandoId: number | null = null;
  editNome = '';
  editDesc = '';
  editPreco: number | null = null;
  editCatId: number | null = null;
  editCod = '';
  editAtivo = true;
  salvandoEdit = false;

  ngOnInit(): void {
    this.carregarTudo();
  }

  carregarTudo(): void {
    this.carregando = true;
    this.erro = null;
    this.api.getCategorias().subscribe({
      next: (cats) => {
        this.categorias = cats;
        this.api.getAdminProdutos().subscribe({
          next: (p) => {
            this.lista = p;
            this.carregando = false;
            if (this.novoCatId == null && cats.length > 0) {
              this.novoCatId = cats[0].id;
            }
          },
          error: (e) => {
            this.carregando = false;
            this.erro = e?.error?.erro ?? 'Não foi possível carregar produtos.';
          },
        });
      },
      error: (e) => {
        this.carregando = false;
        this.erro = e?.error?.erro ?? 'Não foi possível carregar categorias.';
      },
    });
  }

  criar(): void {
    if (!this.novoNome.trim() || this.novoPreco == null || this.novoCatId == null) {
      this.erro = 'Preencha nome, preço e categoria.';
      return;
    }
    if (this.novoPreco < 0) {
      this.erro = 'Preço inválido.';
      return;
    }
    this.salvandoNovo = true;
    this.erro = null;
    this.api
      .postAdminProduto({
        nome: this.novoNome.trim(),
        descricao: this.novaDesc.trim() || null,
        preco: this.novoPreco,
        categoriaId: this.novoCatId,
        codigoImpressao: this.novoCod.trim() || null,
        ativo: this.novoAtivo,
      })
      .subscribe({
        next: () => {
          this.salvandoNovo = false;
          this.novoNome = '';
          this.novaDesc = '';
          this.novoPreco = null;
          this.novoCod = '';
          this.novoAtivo = true;
          this.carregarTudo();
        },
        error: (e) => {
          this.salvandoNovo = false;
          this.erro = e?.error?.erro ?? 'Não foi possível criar o produto.';
        },
      });
  }

  iniciarEdicao(p: Produto): void {
    this.editandoId = p.id;
    this.editNome = p.nome;
    this.editDesc = p.descricao ?? '';
    this.editPreco = p.preco;
    this.editCatId = p.categoriaId;
    this.editCod = p.codigoImpressao ?? '';
    this.editAtivo = p.ativo;
    this.erro = null;
  }

  cancelarEdicao(): void {
    this.editandoId = null;
  }

  salvarEdicao(): void {
    if (this.editandoId == null || !this.editNome.trim() || this.editPreco == null || this.editCatId == null) {
      return;
    }
    if (this.editPreco < 0) {
      this.erro = 'Preço inválido.';
      return;
    }
    this.salvandoEdit = true;
    this.erro = null;
    this.api
      .putAdminProduto(this.editandoId, {
        nome: this.editNome.trim(),
        descricao: this.editDesc.trim() || null,
        preco: this.editPreco,
        categoriaId: this.editCatId,
        codigoImpressao: this.editCod.trim() || null,
        ativo: this.editAtivo,
      })
      .subscribe({
        next: () => {
          this.salvandoEdit = false;
          this.cancelarEdicao();
          this.carregarTudo();
        },
        error: (e) => {
          this.salvandoEdit = false;
          this.erro = e?.error?.erro ?? 'Não foi possível salvar.';
        },
      });
  }
}
