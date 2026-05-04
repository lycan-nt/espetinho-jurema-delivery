import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiBackendService } from '../../core/api-backend.service';
import { PerfilUsuario, UsuarioAdmin } from '../../models/api.models';

const ROTULO_PERFIL: Record<PerfilUsuario, string> = {
  ATENDIMENTO: 'Atendimento (caixa)',
  GARCOM: 'Garçom',
  CHURRASQUEIRO: 'Churrasqueiro',
};

@Component({
  selector: 'app-cadastro-usuarios',
  imports: [FormsModule],
  templateUrl: './cadastro-usuarios.component.html',
  styleUrl: './cadastro-usuarios.component.scss',
})
export class CadastroUsuariosComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  readonly rotuloPerfil = ROTULO_PERFIL;
  readonly perfis: PerfilUsuario[] = ['ATENDIMENTO', 'GARCOM', 'CHURRASQUEIRO'];

  lista: UsuarioAdmin[] = [];
  carregando = false;
  erro: string | null = null;

  novoLogin = '';
  novoNome = '';
  novoPerfil: PerfilUsuario = 'GARCOM';
  novaSenha = '';
  salvandoNovo = false;

  editandoId: number | null = null;
  editLogin = '';
  editNome = '';
  editPerfil: PerfilUsuario = 'GARCOM';
  editAtivo = true;
  editSenha = '';
  salvandoEdit = false;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.api.getAdminUsuarios().subscribe({
      next: (r) => {
        this.lista = r;
        this.carregando = false;
      },
      error: (e) => {
        this.carregando = false;
        this.erro = e?.error?.erro ?? 'Não foi possível carregar usuários.';
      },
    });
  }

  criar(): void {
    const login = this.novoLogin.trim();
    if (!login || !this.novoNome.trim() || !this.novaSenha || this.novaSenha.length < 6) {
      this.erro = 'Preencha login, nome e senha (mín. 6 caracteres).';
      return;
    }
    this.salvandoNovo = true;
    this.erro = null;
    this.api
      .postAdminUsuario({
        login,
        nomeExibicao: this.novoNome.trim(),
        perfil: this.novoPerfil,
        senha: this.novaSenha,
      })
      .subscribe({
        next: () => {
          this.salvandoNovo = false;
          this.novoLogin = '';
          this.novoNome = '';
          this.novaSenha = '';
          this.novoPerfil = 'GARCOM';
          this.carregar();
        },
        error: (e) => {
          this.salvandoNovo = false;
          this.erro = e?.error?.erro ?? 'Não foi possível criar o usuário.';
        },
      });
  }

  iniciarEdicao(u: UsuarioAdmin): void {
    this.editandoId = u.id;
    this.editLogin = u.login;
    this.editNome = u.nomeExibicao;
    this.editPerfil = u.perfil;
    this.editAtivo = u.ativo;
    this.editSenha = '';
    this.erro = null;
  }

  cancelarEdicao(): void {
    this.editandoId = null;
    this.editSenha = '';
  }

  salvarEdicao(): void {
    if (this.editandoId == null || !this.editNome.trim()) {
      return;
    }
    if (this.editSenha && this.editSenha.length < 6) {
      this.erro = 'Senha nova deve ter no mínimo 6 caracteres ou deixe em branco.';
      return;
    }
    this.salvandoEdit = true;
    this.erro = null;
    const body: { nomeExibicao: string; perfil: PerfilUsuario; ativo: boolean; senha?: string } = {
      nomeExibicao: this.editNome.trim(),
      perfil: this.editPerfil,
      ativo: this.editAtivo,
    };
    if (this.editSenha.trim()) {
      body.senha = this.editSenha.trim();
    }
    this.api.putAdminUsuario(this.editandoId, body).subscribe({
      next: () => {
        this.salvandoEdit = false;
        this.cancelarEdicao();
        this.carregar();
      },
      error: (e) => {
        this.salvandoEdit = false;
        this.erro = e?.error?.erro ?? 'Não foi possível salvar.';
      },
    });
  }
}
