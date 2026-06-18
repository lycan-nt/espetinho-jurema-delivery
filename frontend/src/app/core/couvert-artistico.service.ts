import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { CouvertArtisticoConfig } from '../models/api.models';
import { ApiBackendService } from './api-backend.service';

/** Estado global do couvert artístico do dia (leitura em shell, edição no Início). */
@Injectable({ providedIn: 'root' })
export class CouvertArtisticoService {
  private readonly api = inject(ApiBackendService);

  readonly config = signal<CouvertArtisticoConfig | null>(null);

  recarregar(): Observable<CouvertArtisticoConfig> {
    return this.api.getCouvertArtisticoConfig().pipe(tap((c) => this.config.set(c)));
  }

  salvar(ativo: boolean, valorPorPessoa: number): Observable<CouvertArtisticoConfig> {
    return this.api.patchCouvertArtisticoConfig(ativo, valorPorPessoa).pipe(
      tap((c) => this.config.set(c)),
    );
  }
}
