import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiBackendService } from '../../core/api-backend.service';

@Component({
  selector: 'app-config-impressao',
  imports: [FormsModule],
  templateUrl: './config-impressao.component.html',
  styleUrl: './config-impressao.component.scss',
})
export class ConfigImpressaoComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  carregando = true;
  salvando = false;
  erro: string | null = null;

  filas: string[] = [];
  /** Nome da fila CUPS (ex.: da lista ou digitado). */
  nomeImpressoraLp = '';

  /**
   * Nomes de fila mais usados no PDV (Bematech / Elgin). Sempre clicáveis para preencher o campo,
   * mesmo quando o servidor não lista impressoras do SO ({@link filas} vazio).
   */
  readonly presetsBematech = [
    'Bematech MP-4200 TH',
    'Bematech MP-4200 HS',
    'Bematech MP-2800 TH',
  ] as const;

  readonly presetsElgin = ['Elgin i9', 'Elgin FIT Basic', 'Elgin L42 Pro'] as const;

  /** Opções do datalist: presets + filas detectadas no PC (sem duplicar). */
  get opcoesDatalist(): string[] {
    const s = new Set<string>([
      ...this.presetsBematech,
      ...this.presetsElgin,
      ...this.filas,
    ]);
    return [...s].sort((a, b) => a.localeCompare(b, 'pt-BR'));
  }

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    forkJoin([this.api.getImpressaoConfig(), this.api.getImpressaoFilas()]).subscribe({
      next: ([cfg, fil]) => {
        this.nomeImpressoraLp = cfg.nomeImpressoraLp ?? '';
        this.filas = fil.filas ?? [];
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar a configuração ou as filas de impressão.';
      },
    });
  }

  salvar(): void {
    const t = this.nomeImpressoraLp.trim();
    this.salvando = true;
    this.erro = null;
    this.api.patchImpressaoConfig(t === '' ? null : t).subscribe({
      next: (cfg) => {
        this.nomeImpressoraLp = cfg.nomeImpressoraLp ?? '';
        this.salvando = false;
      },
      error: (e) => {
        this.salvando = false;
        this.erro = e?.error?.erro ?? 'Erro ao salvar.';
      },
    });
  }

  usarFila(f: string): void {
    this.nomeImpressoraLp = f;
  }
}
