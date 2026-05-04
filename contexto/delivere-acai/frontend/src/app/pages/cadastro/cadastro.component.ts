import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UsuarioService, Usuario, UsuarioCreate, UsuarioUpdate } from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cadastro.component.html',
  styleUrl: './cadastro.component.scss',
})
export class CadastroComponent implements OnInit {
  usuarios: Usuario[] = [];
  loading = true;
  error: string | null = null;
  errorPermissao = false;

  nome = '';
  senha = '';
  setor: 'VENDAS' | 'GESTAO' = 'VENDAS';
  salvando = false;
  sucesso: string | null = null;
  /** Id do usuário em edição; null = modo novo */
  editandoId: number | null = null;
  excluindoId: number | null = null;

  readonly setores = [
    { value: 'VENDAS' as const, label: 'Vendas' },
    { value: 'GESTAO' as const, label: 'Gestão' },
  ];

  constructor(
    private usuarioService: UsuarioService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading = true;
    this.error = null;
    this.errorPermissao = false;
    this.usuarioService.listar().subscribe({
      next: (list) => {
        this.usuarios = list;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorPermissao = err?.status === 403;
        this.error = this.errorPermissao ? '' : (err?.error?.message || 'Erro ao carregar usuários.');
      },
    });
  }

  editar(u: Usuario): void {
    this.editandoId = u.id;
    this.nome = u.username;
    this.senha = '';
    this.setor = (u.setor === 'GESTAO' ? 'GESTAO' : 'VENDAS');
    this.error = null;
    this.sucesso = null;
  }

  cancelarEdicao(): void {
    this.editandoId = null;
    this.nome = '';
    this.senha = '';
    this.setor = 'VENDAS';
    this.error = null;
  }

  enviar(): void {
    this.error = null;
    this.sucesso = null;
    const username = this.nome.trim();
    if (!username) {
      this.error = 'Informe o nome de usuário.';
      return;
    }
    if (this.editandoId == null) {
      if (!this.senha || this.senha.length < 4) {
        this.error = 'A senha deve ter no mínimo 4 caracteres.';
        return;
      }
    } else if (this.senha && this.senha.length > 0 && this.senha.length < 4) {
      this.error = 'A senha deve ter no mínimo 4 caracteres (ou deixe em branco para não alterar).';
      return;
    }

    this.salvando = true;
    if (this.editandoId != null) {
      const dto: UsuarioUpdate = {
        username: username.toLowerCase(),
        setor: this.setor,
      };
      if (this.senha && this.senha.length >= 4) {
        dto.password = this.senha;
      }
      this.usuarioService.atualizar(this.editandoId, dto).subscribe({
        next: () => {
          this.salvando = false;
          this.editandoId = null;
          this.nome = '';
          this.senha = '';
          this.sucesso = 'Usuário atualizado com sucesso.';
          this.carregar();
          setTimeout(() => (this.sucesso = null), 4000);
        },
        error: (err) => {
          this.salvando = false;
          this.error = err?.error?.message || 'Erro ao atualizar.';
        },
      });
    } else {
      const dto: UsuarioCreate = {
        username: username.toLowerCase(),
        password: this.senha,
        setor: this.setor,
      };
      this.usuarioService.criar(dto).subscribe({
        next: () => {
          this.salvando = false;
          this.nome = '';
          this.senha = '';
          this.sucesso = 'Usuário cadastrado com sucesso.';
          this.carregar();
          setTimeout(() => (this.sucesso = null), 4000);
        },
        error: (err) => {
          this.salvando = false;
          this.error = err?.error?.message || 'Erro ao cadastrar.';
        },
      });
    }
  }

  excluir(u: Usuario): void {
    if (u.username === this.authService.username()) {
      this.error = 'Você não pode excluir seu próprio usuário.';
      return;
    }
    if (!confirm(`Excluir o usuário "${u.username}"? Esta ação não pode ser desfeita.`)) {
      return;
    }
    this.excluindoId = u.id;
    this.error = null;
    this.usuarioService.excluir(u.id).subscribe({
      next: () => {
        this.excluindoId = null;
        this.sucesso = 'Usuário excluído.';
        this.carregar();
        setTimeout(() => (this.sucesso = null), 4000);
      },
      error: (err) => {
        this.excluindoId = null;
        this.error = err?.error?.message || 'Erro ao excluir.';
      },
    });
  }

  ehUsuarioLogado(username: string): boolean {
    return (this.authService.username() || '').toLowerCase() === username.toLowerCase();
  }

  labelSetor(setor: string | null): string {
    if (!setor) return '—';
    return setor === 'GESTAO' ? 'Gestão' : 'Vendas';
  }
}
