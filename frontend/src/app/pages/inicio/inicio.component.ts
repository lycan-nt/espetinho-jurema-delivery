import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiBackendService } from '../../core/api-backend.service';
import { AuthService } from '../../core/auth.service';
import { CouvertArtisticoService } from '../../core/couvert-artistico.service';
import { periodoMesLocal } from '../../core/periodo.util';
import { FaturamentoResumo, MesaResumo } from '../../models/api.models';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, DecimalPipe, FormsModule],
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.scss',
})
export class InicioComponent implements OnInit {
  private readonly api = inject(ApiBackendService);
  readonly auth = inject(AuthService);
  readonly couvert = inject(CouvertArtisticoService);

  resumo: MesaResumo | null = null;
  faturamentoMes: FaturamentoResumo | null = null;
  erroFat: string | null = null;

  couvertAtivo = false;
  couvertValor = 0;
  salvandoCouvert = false;
  erroCouvert: string | null = null;
  couvertOk: string | null = null;

  ngOnInit(): void {
    this.api.getMesasResumo().subscribe((r) => (this.resumo = r));
    if (this.auth.usuario()?.perfil === 'ATENDIMENTO') {
      const { inicio, fim } = periodoMesLocal(new Date());
      this.api.getFaturamentoResumo(inicio, fim).subscribe({
        next: (f) => (this.faturamentoMes = f),
        error: () => (this.erroFat = 'Não foi possível carregar o faturamento do mês.'),
      });
      this.couvert.recarregar().subscribe({
        next: (c) => this.syncCouvertForm(c),
        error: () => {},
      });
    } else {
      this.couvert.recarregar().subscribe({ error: () => {} });
    }
  }

  private syncCouvertForm(cfg: { ativo: boolean; valorPorPessoa: number }): void {
    this.couvertAtivo = cfg.ativo;
    this.couvertValor = cfg.valorPorPessoa;
  }

  onCouvertToggle(ativo: boolean): void {
    this.couvertAtivo = ativo;
    this.erroCouvert = null;
    this.couvertOk = null;
    if (!ativo) {
      this.salvarCouvert();
    }
  }

  salvarCouvert(): void {
    if (this.auth.usuario()?.perfil !== 'ATENDIMENTO') return;
    this.erroCouvert = null;
    this.couvertOk = null;
    const valor = Number(this.couvertValor);
    if (this.couvertAtivo && (!Number.isFinite(valor) || valor <= 0)) {
      this.erroCouvert = 'Informe um valor por pessoa maior que zero para ativar.';
      return;
    }
    this.salvandoCouvert = true;
    this.couvert.salvar(this.couvertAtivo, this.couvertAtivo ? valor : Math.max(0, valor)).subscribe({
      next: (c) => {
        this.syncCouvertForm(c);
        this.couvertOk = c.ativo
          ? `Couvert ativo: R$ ${c.valorPorPessoa.toFixed(2)} por pessoa em mesas (até desativar).`
          : 'Couvert desativado.';
        this.salvandoCouvert = false;
      },
      error: (e) => {
        this.erroCouvert = e?.error?.message ?? 'Não foi possível salvar o couvert.';
        this.salvandoCouvert = false;
      },
    });
  }
}
