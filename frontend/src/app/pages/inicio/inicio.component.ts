import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiBackendService } from '../../core/api-backend.service';
import { AuthService } from '../../core/auth.service';
import { periodoMesLocal } from '../../core/periodo.util';
import { FaturamentoResumo, MesaResumo } from '../../models/api.models';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.scss',
})
export class InicioComponent implements OnInit {
  private readonly api = inject(ApiBackendService);
  readonly auth = inject(AuthService);
  resumo: MesaResumo | null = null;
  faturamentoMes: FaturamentoResumo | null = null;
  erroFat: string | null = null;

  ngOnInit(): void {
    this.api.getMesasResumo().subscribe((r) => (this.resumo = r));
    if (this.auth.usuario()?.perfil === 'ATENDIMENTO') {
      const { inicio, fim } = periodoMesLocal(new Date());
      this.api.getFaturamentoResumo(inicio, fim).subscribe({
        next: (f) => (this.faturamentoMes = f),
        error: () => (this.erroFat = 'Não foi possível carregar o faturamento do mês.'),
      });
    }
  }
}
