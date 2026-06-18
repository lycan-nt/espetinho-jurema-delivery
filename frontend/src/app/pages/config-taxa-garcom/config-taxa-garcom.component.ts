import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiBackendService } from '../../core/api-backend.service';

@Component({
  selector: 'app-config-taxa-garcom',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './config-taxa-garcom.component.html',
  styleUrl: './config-taxa-garcom.component.scss',
})
export class ConfigTaxaGarcomComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  carregando = true;
  salvando = false;
  erro: string | null = null;
  ok: string | null = null;

  habilitada = false;
  percentual = 10;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.api.getTaxaGarcomConfig().subscribe({
      next: (c) => {
        this.habilitada = c.habilitada;
        this.percentual = c.percentual;
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar a configuração.';
      },
    });
  }

  salvar(): void {
    this.erro = null;
    this.ok = null;
    const pct = Number(this.percentual);
    if (this.habilitada && (!Number.isFinite(pct) || pct <= 0 || pct > 100)) {
      this.erro = 'Informe um percentual entre 0,01 e 100 para habilitar.';
      return;
    }
    this.salvando = true;
    this.api.patchTaxaGarcomConfig(this.habilitada, this.habilitada ? pct : Math.max(0, pct)).subscribe({
      next: (c) => {
        this.habilitada = c.habilitada;
        this.percentual = c.percentual;
        this.ok = c.habilitada
          ? `Taxa do garçom ativa: ${c.percentual}% sobre os itens em mesas.`
          : 'Taxa do garçom desabilitada.';
        this.salvando = false;
      },
      error: (e) => {
        this.erro = e?.error?.message ?? e?.error?.erro ?? 'Erro ao salvar.';
        this.salvando = false;
      },
    });
  }
}
