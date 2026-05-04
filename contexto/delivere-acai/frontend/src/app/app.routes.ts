import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { gestaoGuard } from './guards/gestao.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: '', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: 'comanda', loadComponent: () => import('./pages/comanda/comanda.component').then(m => m.ComandaComponent), canActivate: [authGuard] },
  { path: 'comandas', loadComponent: () => import('./pages/lista-comandas/lista-comandas.component').then(m => m.ListaComandasComponent), canActivate: [authGuard] },
  { path: 'gestao', loadComponent: () => import('./pages/gestao-menu/gestao-menu.component').then(m => m.GestaoMenuComponent), canActivate: [authGuard, gestaoGuard] },
  { path: 'gestao/vendas', loadComponent: () => import('./pages/relatorio/relatorio.component').then(m => m.RelatorioComponent), canActivate: [authGuard, gestaoGuard] },
  { path: 'gestao/cadastro', loadComponent: () => import('./pages/cadastro/cadastro.component').then(m => m.CadastroComponent), canActivate: [authGuard, gestaoGuard] },
  { path: 'gestao/produtos', loadComponent: () => import('./pages/produtos/produtos.component').then(m => m.ProdutosComponent), canActivate: [authGuard, gestaoGuard] },
  { path: 'gestao/configuracao', loadComponent: () => import('./pages/configuracao/configuracao.component').then(m => m.ConfiguracaoComponent), canActivate: [authGuard, gestaoGuard] },
  { path: 'gestao/lojas', loadComponent: () => import('./pages/lojas/lojas.component').then(m => m.LojasComponent), canActivate: [authGuard, gestaoGuard] },
  { path: '**', redirectTo: '' },
];
