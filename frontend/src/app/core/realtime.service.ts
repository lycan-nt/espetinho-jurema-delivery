import { Injectable, OnDestroy, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { AlertaAtendimentoWsPayload, PedidoWsPayload } from '../models/api.models';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private readonly auth = inject(AuthService);
  private client: Client | null = null;
  readonly pedidoEventos = new Subject<PedidoWsPayload>();
  /** Abertura de mesa (Fase D) — perfil atendimento consome na fila de alertas. */
  readonly alertasAtendimento = new Subject<AlertaAtendimentoWsPayload>();

  conectar(): void {
    if (this.client?.active) {
      return;
    }
    const token = this.auth.getToken();
    const wsUrl =
      token && environment.wsUrl
        ? `${environment.wsUrl}${environment.wsUrl.includes('?') ? '&' : '?'}access_token=${encodeURIComponent(token)}`
        : environment.wsUrl;
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl) as unknown as WebSocket,
      reconnectDelay: 4000,
      debug: () => {},
    });
    this.client.onConnect = () => {
      this.client?.subscribe('/topic/pedidos', (msg: IMessage) => {
        try {
          const body = JSON.parse(msg.body) as PedidoWsPayload;
          this.pedidoEventos.next(body);
        } catch {
          /* ignore */
        }
      });
      this.client?.subscribe('/topic/atendimento/alertas', (msg: IMessage) => {
        try {
          const body = JSON.parse(msg.body) as AlertaAtendimentoWsPayload;
          this.alertasAtendimento.next(body);
        } catch {
          /* ignore */
        }
      });
    };
    this.client.activate();
  }

  ngOnDestroy(): void {
    void this.client?.deactivate();
  }
}
