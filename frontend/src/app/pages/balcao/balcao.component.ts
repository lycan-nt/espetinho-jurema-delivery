import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiBackendService } from '../../core/api-backend.service';
import { Colaborador, Cliente } from '../../models/api.models';

@Component({
  selector: 'app-balcao',
  imports: [FormsModule],
  templateUrl: '../delivery/delivery.component.html',
  styleUrl: '../delivery/delivery.component.scss',
})
export class BalcaoComponent implements OnInit {
  private readonly api = inject(ApiBackendService);
  private readonly router = inject(Router);

  colaboradores: Colaborador[] = [];
  clientes: Cliente[] = [];
  colaboradorId: number | null = null;
  clienteId: number | null = null;
  descricao = '';
  pessoas: number | null = 1;
  documentoFiscal = false;
  carregando = false;
  erro: string | null = null;
  readonly titulo = 'Balcão';
  readonly tipo = 'BALCAO' as const;

  ngOnInit(): void {
    this.api.getColaboradores().subscribe((c) => {
      this.colaboradores = c;
      if (c.length) {
        this.colaboradorId = c[0].id;
      }
    });
    this.api.getClientes().subscribe((c) => (this.clientes = c));
  }

  salvar(): void {
    if (!this.colaboradorId) {
      this.erro = 'Selecione o responsável.';
      return;
    }
    this.carregando = true;
    this.erro = null;
    this.api
      .criarPedidoAvulso({
        tipo: this.tipo,
        colaboradorId: this.colaboradorId,
        clienteId: this.clienteId ?? undefined,
        descricao: this.descricao || undefined,
        pessoas: this.pessoas ?? undefined,
        documentoFiscal: this.documentoFiscal,
      })
      .subscribe({
        next: (p) => {
          this.carregando = false;
          void this.router.navigate(['/pedidos', p.id]);
        },
        error: (e) => {
          this.carregando = false;
          this.erro = e?.error?.erro ?? 'Erro ao criar pedido.';
        },
      });
  }
}
