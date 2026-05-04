import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protege a rota do módulo de Gestão. Redireciona para a home com mensagem se o usuário não tiver permissão.
 */
export const gestaoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  const gestao = auth.gestaoPermission();
  if (gestao === true) {
    return true;
  }

  if (gestao === false) {
    router.navigate(['/'], { queryParams: { semPermissao: 'gestao' } });
    return false;
  }

  router.navigate(['/']);
  return false;
};
