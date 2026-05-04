import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/** Apenas perfil ATENDIMENTO (caixa / gestão). */
export const atendimentoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.usuario()?.perfil !== 'ATENDIMENTO') {
    return router.createUrlTree(['/inicio']);
  }
  return true;
};
