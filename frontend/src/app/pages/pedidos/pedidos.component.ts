import { DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ApiBackendService } from '../../core/api-backend.service';
import { RealtimeService } from '../../core/realtime.service';
import { PedidoLista, PedidoStatus, PedidoTipo } from '../../models/api.models';

@Component({
  selector: 'app-pedidos',
  imports: [RouterLink, FormsModule, DecimalPipe],
  templateUrl: './pedidos.component.html',
  styleUrl: './pedidos.component.scss',
})
export class PedidosComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiBackendService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroy$ = new Subject<void>();

  lista: PedidoLista[] = [];
  filtroStatus = '';
  /** Foco operação salão: mesas por padrão. */
  filtroTipo = 'MESA';

  ngOnInit(): void {
    this.carregar();
    this.realtime.pedidoEventos.pipe(takeUntil(this.destroy$)).subscribe(() => this.carregar());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregar(): void {
    const st = this.filtroStatus ? (this.filtroStatus as PedidoStatus) : null;
    const tp = this.filtroTipo ? (this.filtroTipo as PedidoTipo) : null;
    this.api.getPedidos(st, tp).subscribe((l) => (this.lista = l));
  }

  aplicarFiltro(): void {
    this.carregar();
  }
}
