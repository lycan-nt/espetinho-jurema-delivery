import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs/operators';
import { ApiBackendService } from '../../core/api-backend.service';
import { BackupConfig, DiaBackupApi } from '../../models/api.models';

const ORDEM_DIA: Record<DiaBackupApi, number> = {
  MON: 1,
  TUE: 2,
  WED: 3,
  THU: 4,
  FRI: 5,
  SAT: 6,
  SUN: 7,
};

@Component({
  selector: 'app-config-backup',
  imports: [FormsModule, DatePipe],
  templateUrl: './config-backup.component.html',
  styleUrl: './config-backup.component.scss',
})
export class ConfigBackupComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  carregando = true;
  salvando = false;
  escolhendoPasta = false;
  executandoBackupAgora = false;
  erro: string | null = null;

  diretorio = '';
  criarDiretorioSeNaoExistir = true;

  /** Valores para `input type="time"` (HH:mm). */
  horario1 = '19:00';
  horario2 = '21:00';

  diasSelecionados: DiaBackupApi[] = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

  readonly diasOpcoes: { codigo: DiaBackupApi; label: string }[] = [
    { codigo: 'MON', label: 'Seg' },
    { codigo: 'TUE', label: 'Ter' },
    { codigo: 'WED', label: 'Qua' },
    { codigo: 'THU', label: 'Qui' },
    { codigo: 'FRI', label: 'Sex' },
    { codigo: 'SAT', label: 'Sáb' },
    { codigo: 'SUN', label: 'Dom' },
  ];

  visao: BackupConfig | null = null;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.api.getBackupConfig().subscribe({
      next: (c) => {
        this.aplicarVisao(c);
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar a configuração de backup.';
      },
    });
  }

  private aplicarVisao(c: BackupConfig): void {
    this.visao = c;
    this.diretorio = c.diretorioGravadoNoBanco ?? c.diretorioEfetivo ?? '';
    this.horario1 = this.fmtHorario(c.backupHora1, c.backupMinuto1);
    this.horario2 = this.fmtHorario(c.backupHora2, c.backupMinuto2);
    this.diasSelecionados = [...(c.backupDiasSemana ?? [])].sort((a, b) => ORDEM_DIA[a] - ORDEM_DIA[b]);
  }

  toggleDia(codigo: DiaBackupApi): void {
    if (this.diasSelecionados.includes(codigo)) {
      this.diasSelecionados = this.diasSelecionados.filter((d) => d !== codigo);
    } else {
      this.diasSelecionados = [...this.diasSelecionados, codigo].sort((a, b) => ORDEM_DIA[a] - ORDEM_DIA[b]);
    }
  }

  isDiaMarcado(codigo: DiaBackupApi): boolean {
    return this.diasSelecionados.includes(codigo);
  }

  salvar(): void {
    const { h: h1, m: m1 } = this.lerHorario(this.horario1);
    const { h: h2, m: m2 } = this.lerHorario(this.horario2);

    this.salvando = true;
    this.erro = null;
    const trim = this.diretorio.trim();
    this.api
      .patchBackupConfig({
        diretorio: trim.length === 0 ? '' : trim,
        criarDiretorioSeNaoExistir: this.criarDiretorioSeNaoExistir,
        backupHora1: h1,
        backupMinuto1: m1,
        backupHora2: h2,
        backupMinuto2: m2,
        backupDiasSemana: [...this.diasSelecionados],
      })
      .subscribe({
        next: (c) => {
          this.aplicarVisao(c);
          this.salvando = false;
        },
        error: (e) => {
          this.salvando = false;
          this.erro = e?.error?.erro ?? 'Erro ao salvar.';
        },
      });
  }

  backupAgora(): void {
    this.executandoBackupAgora = true;
    this.erro = null;
    this.api.postBackupExecutarAgora().subscribe({
      next: (c) => {
        this.aplicarVisao(c);
        this.executandoBackupAgora = false;
      },
      error: (e) => {
        this.executandoBackupAgora = false;
        this.erro = e?.error?.erro ?? 'Erro ao gerar o backup.';
      },
    });
  }

  escolherPastaNoServidor(): void {
    this.escolhendoPasta = true;
    this.erro = null;
    this.api
      .postBackupSelecionarPasta()
      .pipe(
        timeout({ first: 130_000 }),
        finalize(() => {
          this.escolhendoPasta = false;
        }),
      )
      .subscribe({
        next: (r) => {
          if (r.cancelado) {
            return;
          }
          if (r.path) {
            this.diretorio = r.path;
          }
        },
        error: (e) => {
          const isTimeout = e?.name === 'TimeoutError';
          this.erro = isTimeout
            ? 'Tempo limite (2 min) sem resposta do servidor. Veja o console da API; se não aparecer janela no PC do servidor, digite o caminho manualmente.'
            : e?.error?.erro ?? 'Não foi possível abrir o seletor de pasta.';
        },
      });
  }

  private fmtHorario(h: number, m: number): string {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }

  private lerHorario(s: string): { h: number; m: number } {
    const part = s.trim().split(':');
    const h = parseInt(part[0] ?? '0', 10);
    const m = parseInt(part[1] ?? '0', 10);
    return {
      h: Number.isFinite(h) ? Math.min(23, Math.max(0, h)) : 0,
      m: Number.isFinite(m) ? Math.min(59, Math.max(0, m)) : 0,
    };
  }
}
