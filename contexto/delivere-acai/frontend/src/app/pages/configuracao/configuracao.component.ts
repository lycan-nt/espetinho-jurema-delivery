import { Component, OnInit, OnDestroy, NgZone, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DecimalPipe } from '@angular/common';
import { ProdutoService, ConfigBalancaDTO } from '../../services/produto.service';
import {
  BalancaService,
  BalancaStatus,
  BalancaModo,
  BALANCA_BAUD_RATES,
  BalancaInfoSerial,
} from '../../services/balanca.service';

@Component({
  selector: 'app-configuracao',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DecimalPipe],
  templateUrl: './configuracao.component.html',
  styleUrl: './configuracao.component.scss',
})
export class ConfiguracaoComponent implements OnInit, OnDestroy {
  /** Preço do kilo (açaí) */
  precoPorKilo = 0;
  loadingPreco = true;
  salvandoPreco = false;
  errorPreco: string | null = null;
  sucessoPreco: string | null = null;

  /** Config balança (carregada do backend) */
  loadingBalanca = true;
  salvandoBalanca = false;
  sucessoBalanca: string | null = null;
  errorBalanca: string | null = null;
  errorPermissao = false;

  /** Form balança (usado ao conectar e ao salvar) */
  readonly balancaBaudRates = BALANCA_BAUD_RATES;
  balancaBaudRate = 4800;
  balancaSerialConfig: '8n1' | '8e2' = '8n1';
  balancaEnviarEnq = true;

  balancaStatus = signal<BalancaStatus>('desconectada');
  balancaModo = signal<BalancaModo>('manual');
  balancaInfoSerial = signal<BalancaInfoSerial | null>(null);
  balancaConectando = signal(false);
  balancaErro = signal<string | null>(null);

  private readonly destroy$ = new Subject<void>();

  constructor(
    private produtoService: ProdutoService,
    public balancaService: BalancaService,
    private ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    this.carregarPreco();
    this.carregarConfigBalanca();
    this.balancaService.status$.pipe(takeUntil(this.destroy$)).subscribe((s) => this.ngZone.run(() => this.balancaStatus.set(s)));
    this.balancaService.modo$.pipe(takeUntil(this.destroy$)).subscribe((m) => this.ngZone.run(() => this.balancaModo.set(m)));
    this.balancaService.infoSerial$.pipe(takeUntil(this.destroy$)).subscribe((info) => this.ngZone.run(() => this.balancaInfoSerial.set(info)));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregarPreco(): void {
    this.loadingPreco = true;
    this.errorPreco = null;
    this.produtoService.getPrecoKg().subscribe({
      next: (dto) => {
        this.precoPorKilo = dto.precoPorKilo ?? 0;
        this.loadingPreco = false;
      },
      error: (err) => {
        this.loadingPreco = false;
        this.errorPreco = err?.error?.message || 'Erro ao carregar preço.';
      },
    });
  }

  carregarConfigBalanca(): void {
    this.loadingBalanca = true;
    this.errorBalanca = null;
    this.produtoService.getConfigBalanca().subscribe({
      next: (dto) => {
        this.balancaBaudRate = dto.baudRate ?? 4800;
        this.balancaSerialConfig = (dto.serialConfig === '8e2' ? '8e2' : '8n1') as '8n1' | '8e2';
        this.balancaEnviarEnq = dto.enviarEnq !== false;
        this.loadingBalanca = false;
      },
      error: (err) => {
        this.loadingBalanca = false;
        this.errorPermissao = err?.status === 403;
        this.errorBalanca = this.errorPermissao ? '' : (err?.error?.message || 'Erro ao carregar configuração.');
      },
    });
  }

  salvarPreco(): void {
    this.errorPreco = null;
    this.sucessoPreco = null;
    this.salvandoPreco = true;
    this.produtoService.setPrecoKg(this.precoPorKilo).subscribe({
      next: (dto) => {
        this.precoPorKilo = dto.precoPorKilo ?? 0;
        this.salvandoPreco = false;
        this.sucessoPreco = 'Preço do kilo salvo.';
        setTimeout(() => (this.sucessoPreco = null), 3000);
      },
      error: (err) => {
        this.salvandoPreco = false;
        this.errorPreco = err?.error?.message || 'Erro ao salvar.';
      },
    });
  }

  salvarConfigBalanca(): void {
    this.errorBalanca = null;
    this.sucessoBalanca = null;
    this.salvandoBalanca = true;
    const config: ConfigBalancaDTO = {
      baudRate: this.balancaBaudRate,
      serialConfig: this.balancaSerialConfig,
      enviarEnq: this.balancaEnviarEnq,
    };
    this.produtoService.setConfigBalanca(config).subscribe({
      next: () => {
        this.salvandoBalanca = false;
        this.sucessoBalanca = 'Configuração da balança salva.';
        setTimeout(() => (this.sucessoBalanca = null), 3000);
      },
      error: (err) => {
        this.salvandoBalanca = false;
        this.errorPermissao = err?.status === 403;
        this.errorBalanca = this.errorPermissao ? '' : (err?.error?.message || 'Erro ao salvar.');
      },
    });
  }

  async conectarBalanca(): Promise<void> {
    this.balancaErro.set(null);
    this.balancaConectando.set(true);
    try {
      const dataBits = this.balancaSerialConfig === '8e2' ? 8 : 8;
      const stopBits = this.balancaSerialConfig === '8e2' ? 2 : 1;
      const parity = this.balancaSerialConfig === '8e2' ? 'even' as const : 'none' as const;
      await this.balancaService.conectarSerial({
        baudRate: this.balancaBaudRate,
        dataBits,
        stopBits,
        parity,
        enviarEnq: this.balancaEnviarEnq,
      });
      this.salvarConfigBalanca(); // persistir para próxima vez
    } catch (err) {
      this.ngZone.run(() => {
        this.balancaConectando.set(false);
        this.balancaErro.set(err instanceof Error ? err.message : 'Erro ao conectar.');
      });
    }
    this.balancaConectando.set(false);
  }

  async desconectarBalanca(): Promise<void> {
    await this.balancaService.desconectar();
  }
}
