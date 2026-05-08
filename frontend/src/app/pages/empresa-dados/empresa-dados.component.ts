import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiBackendService } from '../../core/api-backend.service';
import { ComandaCabecalhoCampos } from '../../models/api.models';

@Component({
  selector: 'app-empresa-dados',
  imports: [FormsModule],
  templateUrl: './empresa-dados.component.html',
  styleUrl: './empresa-dados.component.scss',
})
export class EmpresaDadosComponent implements OnInit {
  private readonly api = inject(ApiBackendService);

  carregando = true;
  salvando = false;
  erro: string | null = null;

  cnpj = '';
  nomeEmpresa = '';
  endereco = '';
  telefone = '';
  email = '';
  instagram = '';

  comandaCabecalho: ComandaCabecalhoCampos = this.cabecalhoPadrao();

  readonly opcoesCabecalhoComanda: { key: keyof ComandaCabecalhoCampos; label: string }[] = [
    { key: 'cnpj', label: 'CNPJ' },
    { key: 'nomeEmpresa', label: 'Nome da empresa' },
    { key: 'endereco', label: 'Endereço' },
    { key: 'telefone', label: 'Telefone' },
    { key: 'email', label: 'E-mail' },
    { key: 'instagram', label: 'Instagram' },
  ];

  ngOnInit(): void {
    this.carregar();
  }

  alternarCabecalho(key: keyof ComandaCabecalhoCampos): void {
    this.comandaCabecalho = { ...this.comandaCabecalho, [key]: !this.comandaCabecalho[key] };
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.api.getEmpresaDados().subscribe({
      next: (d) => {
        this.aplicarResposta(d);
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar os dados da empresa.';
      },
    });
  }

  salvar(): void {
    this.salvando = true;
    this.erro = null;
    this.api
      .patchEmpresaDados({
        cnpj: this.trimOuNull(this.cnpj),
        nomeEmpresa: this.trimOuNull(this.nomeEmpresa),
        endereco: this.trimOuNull(this.endereco),
        telefone: this.trimOuNull(this.telefone),
        email: this.trimOuNull(this.email),
        instagram: this.trimOuNull(this.instagram),
        comandaCabecalho: { ...this.comandaCabecalho },
      })
      .subscribe({
        next: (d) => {
          this.aplicarResposta(d);
          this.salvando = false;
        },
        error: (e) => {
          this.salvando = false;
          this.erro = e?.error?.erro ?? 'Erro ao salvar.';
        },
      });
  }

  private aplicarResposta(d: {
    cnpj: string | null;
    nomeEmpresa: string | null;
    endereco: string | null;
    telefone: string | null;
    email: string | null;
    instagram?: string | null;
    comandaCabecalho?: ComandaCabecalhoCampos;
  }): void {
    this.cnpj = d.cnpj ?? '';
    this.nomeEmpresa = d.nomeEmpresa ?? '';
    this.endereco = d.endereco ?? '';
    this.telefone = d.telefone ?? '';
    this.email = d.email ?? '';
    this.instagram = d.instagram ?? '';
    this.comandaCabecalho = { ...this.cabecalhoPadrao(), ...d.comandaCabecalho };
  }

  private cabecalhoPadrao(): ComandaCabecalhoCampos {
    return {
      cnpj: true,
      nomeEmpresa: true,
      endereco: true,
      telefone: true,
      email: true,
      instagram: true,
    };
  }

  private trimOuNull(s: string): string | null {
    const t = s.trim();
    return t === '' ? null : t;
  }
}
