import { DecimalPipe } from '@angular/common';
import { Component, HostListener, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiBackendService } from '../../core/api-backend.service';
import { CategoriaCardapio, Produto } from '../../models/api.models';

type DrawerModo = 'novo' | 'editar';

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

  // ── Drawer ──────────────────────────────────────────────────────────────
  drawerAberto = false;
  drawerModo: DrawerModo = 'novo';
  drawerProdutoId: number | null = null;

  formNome = '';
  formDesc = '';
  formPreco: number | null = null;
  formCatId: number | null = null;
  formCod = '';
  formAtivo = true;
  salvando = false;
  erroForm: string | null = null;

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

  abrirNovo(): void {
    this.drawerModo = 'novo';
    this.drawerProdutoId = null;
    this.formNome = '';
    this.formDesc = '';
    this.formPreco = null;
    this.formCatId = this.categorias.length > 0 ? this.categorias[0].id : null;
    this.formCod = '';
    this.formAtivo = true;
    this.erroForm = null;
    this.salvando = false;
    this.drawerAberto = true;
  }

  abrirEditar(p: Produto): void {
    this.drawerModo = 'editar';
    this.drawerProdutoId = p.id;
    this.formNome = p.nome;
    this.formDesc = p.descricao ?? '';
    this.formPreco = p.preco;
    this.formCatId = p.categoriaId;
    this.formCod = p.codigoImpressao ?? '';
    this.formAtivo = p.ativo;
    this.erroForm = null;
    this.salvando = false;
    this.drawerAberto = true;
  }

  fecharDrawer(): void {
    if (this.salvando) return;
    this.drawerAberto = false;
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.fecharDrawer();
  }

  salvar(): void {
    if (!this.formNome.trim() || this.formPreco == null || this.formCatId == null) {
      this.erroForm = 'Preencha nome, preço e categoria.';
      return;
    }
    if (this.formPreco < 0) {
      this.erroForm = 'Preço inválido.';
      return;
    }

    const body = {
      nome: this.formNome.trim(),
      descricao: this.formDesc.trim() || null,
      preco: this.formPreco,
      categoriaId: this.formCatId,
      codigoImpressao: this.formCod.trim() || null,
      ativo: this.formAtivo,
    };

    this.salvando = true;
    this.erroForm = null;

    const req =
      this.drawerModo === 'editar' && this.drawerProdutoId != null
        ? this.api.putAdminProduto(this.drawerProdutoId, body)
        : this.api.postAdminProduto(body);

    req.subscribe({
      next: () => {
        this.salvando = false;
        this.drawerAberto = false;
        this.carregarTudo();
      },
      error: (e) => {
        this.salvando = false;
        this.erroForm = e?.error?.erro ?? 'Não foi possível salvar o produto.';
      },
    });
  }

  tituloDrawer(): string {
    return this.drawerModo === 'editar'
      ? `Editar produto #${this.drawerProdutoId}`
      : 'Novo produto';
  }
}
