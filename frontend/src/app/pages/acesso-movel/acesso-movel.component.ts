import { Component, OnInit, inject } from '@angular/core';
import { QRCodeComponent } from 'angularx-qrcode';
import { ApiBackendService } from '../../core/api-backend.service';

interface EntradaAcesso {
  ip: string;
  url: string;
}

@Component({
  selector: 'app-acesso-movel',
  imports: [QRCodeComponent],
  templateUrl: './acesso-movel.component.html',
  styleUrl: './acesso-movel.component.scss',
})
export class AcessoMovelComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  carregando = true;
  erro: string | null = null;
  entradas: EntradaAcesso[] = [];
  ipSelecionado: string | null = null;

  get urlSelecionada(): string {
    return this.entradas.find((e) => e.ip === this.ipSelecionado)?.url ?? '';
  }

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.api.getSistemaAcessoLocal().subscribe({
      next: ({ ips, porta }) => {
        this.entradas = ips.map((ip) => ({
          ip,
          url: `http://${ip}:${porta}`,
        }));
        this.ipSelecionado = this.entradas[0]?.ip ?? null;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível obter os IPs da máquina. Verifique se o servidor está em execução.';
        this.carregando = false;
      },
    });
  }

  selecionar(ip: string): void {
    this.ipSelecionado = ip;
  }
}
