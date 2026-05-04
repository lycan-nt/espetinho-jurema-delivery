import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LojaService, Loja } from '../../services/loja.service';

@Component({
  selector: 'app-lojas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './lojas.component.html',
  styleUrl: './lojas.component.scss',
})
export class LojasComponent implements OnInit {
  lojas: Loja[] = [];
  loading = true;
  error: string | null = null;
  errorPermissao = false;

  id = '';
  nome = '';
  endereco = '';
  responsavel = '';
  salvando = false;
  sucesso: string | null = null;
  editandoId: string | null = null;
  excluindoId: string | null = null;

  constructor(private lojaService: LojaService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading = true;
    this.error = null;
    this.errorPermissao = false;
    this.lojaService.listar().subscribe({
      next: (list) => {
        this.lojas = list;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorPermissao = err?.status === 403;
        this.error = this.errorPermissao ? '' : (err?.error?.message || 'Erro ao carregar lojas.');
      },
    });
  }

  editar(l: Loja): void {
    this.editandoId = l.id;
    this.id = l.id;
    this.nome = l.nome || '';
    this.endereco = l.endereco || '';
    this.responsavel = l.responsavel || '';
    this.error = null;
    this.sucesso = null;
  }

  cancelarEdicao(): void {
    this.editandoId = null;
    this.id = '';
    this.nome = '';
    this.endereco = '';
    this.responsavel = '';
    this.error = null;
  }

  enviar(): void {
    this.error = null;
    this.sucesso = null;
    const idTrim = this.id.trim();
    const nomeTrim = this.nome.trim();
    if (!idTrim) {
      this.error = 'O ID da loja é obrigatório (ex.: 01, LOJA-A).';
      return;
    }
    if (!nomeTrim) {
      this.error = 'O nome da loja é obrigatório.';
      return;
    }

    this.salvando = true;
    if (this.editandoId != null) {
      // Backend valida @RequestBody como Loja: campo id é @NotBlank — precisa ir no JSON.
      const payloadAtualizacao = {
        id: this.editandoId,
        nome: nomeTrim,
        endereco: this.endereco.trim() || undefined,
        responsavel: this.responsavel.trim() || undefined,
      };
      this.lojaService.atualizar(this.editandoId, payloadAtualizacao).subscribe({
        next: () => {
          this.salvando = false;
          this.cancelarEdicao();
          this.sucesso = 'Loja atualizada com sucesso.';
          this.carregar();
          setTimeout(() => (this.sucesso = null), 4000);
        },
        error: (err) => {
          this.salvando = false;
          this.error = err?.error?.message || 'Erro ao atualizar.';
        },
      });
    } else {
      const payload = {
        nome: nomeTrim,
        endereco: this.endereco.trim() || undefined,
        responsavel: this.responsavel.trim() || undefined,
      };
      this.lojaService.criar({ id: idTrim, ...payload }).subscribe({
        next: () => {
          this.salvando = false;
          this.cancelarEdicao();
          this.sucesso = 'Loja cadastrada com sucesso.';
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

  excluir(l: Loja): void {
    if (!confirm(`Excluir a loja "${l.nome}" (ID: ${l.id})? Esta ação não pode ser desfeita.`)) {
      return;
    }
    this.excluindoId = l.id;
    this.error = null;
    this.lojaService.excluir(l.id).subscribe({
      next: () => {
        this.excluindoId = null;
        this.sucesso = 'Loja excluída.';
        this.carregar();
        setTimeout(() => (this.sucesso = null), 4000);
      },
      error: (err) => {
        this.excluindoId = null;
        this.error = err?.error?.message || 'Erro ao excluir.';
      },
    });
  }
}
