import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { atendimentoGuard } from './core/atendimento.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio/inicio.component').then((m) => m.InicioComponent),
      },
      {
        path: 'mesas',
        loadComponent: () => import('./pages/mesas/mesas.component').then((m) => m.MesasComponent),
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./pages/pedidos/pedidos.component').then((m) => m.PedidosComponent),
      },
      {
        path: 'pedidos/:id',
        loadComponent: () =>
          import('./pages/pedido-detalhe/pedido-detalhe.component').then((m) => m.PedidoDetalheComponent),
      },
      {
        path: 'config/impressao',
        canActivate: [atendimentoGuard],
        loadComponent: () =>
          import('./pages/config-impressao/config-impressao.component').then((m) => m.ConfigImpressaoComponent),
      },
      {
        path: 'config/empresa',
        canActivate: [atendimentoGuard],
        loadComponent: () =>
          import('./pages/empresa-dados/empresa-dados.component').then((m) => m.EmpresaDadosComponent),
      },
      {
        path: 'estoque',
        canActivate: [atendimentoGuard],
        loadComponent: () => import('./pages/estoque/estoque.component').then((m) => m.EstoqueComponent),
      },
      {
        path: 'relatorio',
        canActivate: [atendimentoGuard],
        loadComponent: () =>
          import('./pages/relatorio-faturamento/relatorio-faturamento.component').then(
            (m) => m.RelatorioFaturamentoComponent,
          ),
      },
      {
        path: 'cadastro/usuarios',
        canActivate: [atendimentoGuard],
        loadComponent: () =>
          import('./pages/cadastro-usuarios/cadastro-usuarios.component').then((m) => m.CadastroUsuariosComponent),
      },
      {
        path: 'cadastro/produtos',
        canActivate: [atendimentoGuard],
        loadComponent: () =>
          import('./pages/cadastro-produtos/cadastro-produtos.component').then((m) => m.CadastroProdutosComponent),
      },
      {
        path: 'config/acesso-movel',
        canActivate: [atendimentoGuard],
        loadComponent: () =>
          import('./pages/acesso-movel/acesso-movel.component').then((m) => m.AcessoMovelComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
