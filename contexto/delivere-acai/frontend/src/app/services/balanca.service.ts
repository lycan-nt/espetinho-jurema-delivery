import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

export type BalancaModo = 'manual' | 'serial' | 'simulador';
export type BalancaStatus = 'desconectada' | 'conectando' | 'conectada' | 'erro';

/** Info para debug na tela: ver se a balança está enviando dados. */
export interface BalancaInfoSerial {
  bytesRecebidos: number;
  leiturasComPeso: number;
  ultimosBytesHex: string;
  ultimaLeituraKg: number | null;
  /** Segundos desde que conectou (mostra que o app está aguardando). */
  segundosAguardando: number;
}

/** Configuração da porta serial. Toledo Prix 3 Fit: 4800 ou 2400 baud; outras podem usar 9600. */
export const BALANCA_SERIAL_PADRAO = { baudRate: 9600 as const };

/** Baud rates suportados (Toledo Prix 3 Fit costuma usar 4800 ou 2400). */
export const BALANCA_BAUD_RATES = [2400, 4800, 9600] as const;

/** ENQ (0x05): solicita peso em balanças Toledo (modo request/response). */
const ENQ = new Uint8Array([0x05]);
/** STX/ETX: delimitadores do protocolo Toledo (payload entre 0x02 e 0x03). */
const STX = 0x02;
const ETX = 0x03;

/** Opções de abertura da porta (algumas Toledo usam 7E2 ou 8E2). */
export interface SerialOptions {
  baudRate: number;
  dataBits?: 7 | 8;
  stopBits?: 1 | 2;
  parity?: 'none' | 'even' | 'odd';
  /** Se false, não envia ENQ (só lê). Use se a balança envia sozinha ou se ENQ atrapalha. */
  enviarEnq?: boolean;
}

/** Tipo mínimo para Web Serial API (Chrome/Edge). */
interface SerialPortLike {
  open(options: SerialOptions): Promise<void>;
  readable: ReadableStream<Uint8Array> | null;
  writable?: WritableStream<Uint8Array> | null;
  close(): Promise<void>;
}

declare global {
  interface Navigator {
    serial?: {
      requestPort(): Promise<SerialPortLike>;
      getPorts(): Promise<unknown[]>;
    };
  }
}

/**
 * Serviço de integração com balança de pesar.
 * - Modo serial: Web Serial API (Chrome/Edge) para balança real via USB/RS-232.
 * - Modo simulador: envia pesos fictícios para testar sem hardware.
 * O peso é emitido em pesoKg$; o componente pode preencher o campo automaticamente.
 */
@Injectable({
  providedIn: 'root',
})
export class BalancaService {
  private readonly pesoKgSubject = new BehaviorSubject<number | null>(null);
  private readonly statusSubject = new BehaviorSubject<BalancaStatus>('desconectada');
  private readonly modoSubject = new BehaviorSubject<BalancaModo>('manual');

  private reader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  private port: SerialPortLike | null = null;
  private fechado = false;
  private enqIntervalId: ReturnType<typeof setInterval> | null = null;

  private bytesRecebidos = 0;
  private leiturasComPeso = 0;
  private ultimosBytes: number[] = [];
  private conexaoDesde = 0;
  private heartbeatIntervalId: ReturnType<typeof setInterval> | null = null;
  private readonly maxUltimosBytes = 40;
  private readonly infoSerialSubject = new BehaviorSubject<BalancaInfoSerial>({
    bytesRecebidos: 0,
    leiturasComPeso: 0,
    ultimosBytesHex: '',
    ultimaLeituraKg: null,
    segundosAguardando: 0,
  });

  /** Último peso recebido (kg) ou null. Atualizado em tempo real na leitura serial ou pelo simulador. */
  readonly pesoKg$: Observable<number | null> = this.pesoKgSubject.asObservable();
  /** Status da conexão (serial ou simulador). */
  readonly status$: Observable<BalancaStatus> = this.statusSubject.asObservable();
  /** Modo atual: manual, serial, simulador. */
  readonly modo$: Observable<BalancaModo> = this.modoSubject.asObservable();
  /** Info para exibir na tela e testar se a balança envia dados (serial). */
  readonly infoSerial$: Observable<BalancaInfoSerial> = this.infoSerialSubject.asObservable();

  /** Web Serial API está disponível (Chrome/Edge, contexto seguro). */
  get serialDisponivel(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.serial;
  }

  get status(): BalancaStatus {
    return this.statusSubject.value;
  }

  get modo(): BalancaModo {
    return this.modoSubject.value;
  }

  /**
   * Conecta à balança via porta serial (Web Serial).
   * O usuário escolhe a porta no diálogo do navegador.
   * Opção serialOptions: algumas Toledo usam 7E2 ou 8E2 em vez de 8N1.
   */
  private emitInfoSerial(): void {
    const segundos =
      this.conexaoDesde > 0 ? Math.floor((Date.now() - this.conexaoDesde) / 1000) : 0;
    this.infoSerialSubject.next({
      bytesRecebidos: this.bytesRecebidos,
      leiturasComPeso: this.leiturasComPeso,
      ultimosBytesHex: this.ultimosBytes.length
        ? this.ultimosBytes.map((b) => b.toString(16).padStart(2, '0')).join(' ')
        : '',
      ultimaLeituraKg: this.pesoKgSubject.value,
      segundosAguardando: segundos,
    });
  }

  async conectarSerial(options: Partial<SerialOptions> = {}): Promise<void> {
    if (!navigator.serial) {
      this.statusSubject.next('erro');
      throw new Error('Seu navegador não suporta conexão com balança. Use Chrome ou Edge em HTTPS.');
    }
    this.modoSubject.next('serial');
    this.statusSubject.next('conectando');
    this.pesoKgSubject.next(null);
    this.fechado = false;
    this.bytesRecebidos = 0;
    this.leiturasComPeso = 0;
    this.ultimosBytes = [];
    this.conexaoDesde = Date.now();
    this.emitInfoSerial();
    this.iniciarHeartbeat();

    const baud = options.baudRate ?? BALANCA_SERIAL_PADRAO.baudRate;
    const openOpts: SerialOptions = { baudRate: baud };
    if (options && 'dataBits' in options && options.dataBits != null) openOpts.dataBits = options.dataBits;
    if (options && 'stopBits' in options && options.stopBits != null) openOpts.stopBits = options.stopBits;
    if (options && 'parity' in options && options.parity != null) openOpts.parity = options.parity;
    try {
      const serialPort = await navigator.serial!.requestPort() as SerialPortLike;
      await serialPort.open(openOpts);
      if (!serialPort.readable) throw new Error('Porta sem leitura');
      this.port = serialPort;
      this.reader = serialPort.readable.getReader();
      this.statusSubject.next('conectada');
      this.lerSerial(options.enviarEnq !== false);
    } catch (err) {
      this.statusSubject.next('erro');
      this.modoSubject.next('manual');
      throw err;
    }
  }

  /**
   * Tenta reconectar a uma porta já autorizada (getPorts), sem pedir ao usuário.
   * Útil quando a balança desconectou (ex.: reinício do PC). Retorna true se conectou.
   */
  async tentarReconectar(options: Partial<SerialOptions> = {}): Promise<boolean> {
    if (!navigator.serial?.getPorts) return false;
    const ports = await navigator.serial.getPorts() as SerialPortLike[];
    if (ports.length === 0) return false;

    const baud = options.baudRate ?? BALANCA_SERIAL_PADRAO.baudRate;
    const openOpts: SerialOptions = { baudRate: baud };
    if (options?.dataBits != null) openOpts.dataBits = options.dataBits;
    if (options?.stopBits != null) openOpts.stopBits = options.stopBits;
    if (options?.parity != null) openOpts.parity = options.parity;
    const enviarEnq = options.enviarEnq !== false;

    this.modoSubject.next('serial');
    this.statusSubject.next('conectando');
    this.pesoKgSubject.next(null);
    this.fechado = false;
    this.bytesRecebidos = 0;
    this.leiturasComPeso = 0;
    this.ultimosBytes = [];
    this.conexaoDesde = Date.now();
    this.emitInfoSerial();
    this.iniciarHeartbeat();

    for (const p of ports) {
      try {
        await p.open(openOpts);
        if (!p.readable) continue;
        this.port = p;
        this.reader = p.readable.getReader();
        this.statusSubject.next('conectada');
        this.lerSerial(enviarEnq);
        return true;
      } catch {
        continue;
      }
    }
    this.statusSubject.next('desconectada');
    this.modoSubject.next('manual');
    this.pararHeartbeat();
    return false;
  }

  private async lerSerial(enviarEnq: boolean): Promise<void> {
    if (!this.reader) return;
    const decoder = new TextDecoder('utf-8', { fatal: false });
    let textBuffer = '';
    let byteBuffer: number[] = [];
    if (enviarEnq) this.iniciarEnqPolling();
    try {
      while (!this.fechado) {
        const { value, done } = await this.reader.read();
        if (done) break;
        this.bytesRecebidos += value.length;
        for (let i = 0; i < value.length; i++) {
          const b = value[i];
          this.ultimosBytes.push(b);
          if (this.ultimosBytes.length > this.maxUltimosBytes)
            this.ultimosBytes = this.ultimosBytes.slice(-this.maxUltimosBytes);
          byteBuffer.push(b);
          // Protocolo Toledo STX/ETX: frame = STX + payload ASCII + ETX
          if (b === ETX) {
            const stxIdx = byteBuffer.indexOf(STX);
            if (stxIdx >= 0) {
              const payload = byteBuffer.slice(stxIdx + 1, byteBuffer.length - 1);
              const str = decoder.decode(new Uint8Array(payload)).trim();
              const peso = this.extrairPeso(str);
              if (peso != null) {
                this.leiturasComPeso++;
                this.pesoKgSubject.next(peso);
              }
            }
            byteBuffer = [];
            continue;
          }
        }
        // Limitar tamanho do buffer binário (evitar lixo antes de STX)
        if (byteBuffer.length > 128) byteBuffer = byteBuffer.slice(-64);
        // Parsing em texto (balanças que enviam linhas ASCII ou fluxo contínuo)
        const texto = decoder.decode(value, { stream: true });
        textBuffer += texto;
        const linhas = textBuffer.split(/\r\n|\r|\n/);
        textBuffer = linhas.pop() ?? '';
        for (const linha of linhas) {
          const peso = this.extrairPeso(linha);
          if (peso != null) {
            this.leiturasComPeso++;
            this.pesoKgSubject.next(peso);
          }
        }
        const rest = textBuffer.trim();
        if (rest.length > 0) {
          const peso = this.extrairPeso(textBuffer);
          if (peso != null) {
            this.leiturasComPeso++;
            this.pesoKgSubject.next(peso);
            textBuffer = '';
          }
        }
        this.emitInfoSerial();
      }
    } catch {
      if (!this.fechado) this.statusSubject.next('erro');
    } finally {
      this.pararEnqPolling();
      this.pararHeartbeat();
      this.reader = null;
    }
  }

  private iniciarHeartbeat(): void {
    this.heartbeatIntervalId = setInterval(() => this.emitInfoSerial(), 1000);
  }

  private pararHeartbeat(): void {
    if (this.heartbeatIntervalId != null) {
      clearInterval(this.heartbeatIntervalId);
      this.heartbeatIntervalId = null;
    }
  }

  /** Envia ENQ (0x05) periodicamente para balanças Toledo que só respondem sob solicitação. */
  private iniciarEnqPolling(): void {
    const port = this.port;
    if (!port?.writable) return;
    this.enqIntervalId = setInterval(() => {
      if (this.fechado) return;
      const w = port.writable!.getWriter();
      w.write(ENQ)
        .catch(() => {})
        .finally(() => w.releaseLock());
    }, 400);
  }

  private pararEnqPolling(): void {
    if (this.enqIntervalId != null) {
      clearInterval(this.enqIntervalId);
      this.enqIntervalId = null;
    }
  }

  /**
   * Extrai valor de peso (kg) de uma linha enviada pela balança.
   * Aceita: "1.234", "1,234", "  0.5 kg", "350" (gramas → 0.35 kg).
   * Toledo Prix 3 Fit: payload entre STX/ETX pode ser em kg (0.350) ou gramas (350).
   */
  private extrairPeso(linha: string): number | null {
    const s = linha.trim();
    if (!s.length) return null;
    const match = s.match(/(\d+[.,]?\d*)/);
    if (!match) return null;
    const num = match[1].replace(',', '.');
    const v = parseFloat(num);
    if (!Number.isFinite(v) || v < 0) return null;
    // Número inteiro: provável gramas (Toledo e outras), ex: 350 → 0.35 kg, 1234 → 1.234 kg
    if (Number.isInteger(v) && v > 0 && v <= 999999) return Math.round(v) / 1000;
    return v;
  }

  /** Desconecta a balança (serial) e volta ao modo manual. */
  async desconectar(): Promise<void> {
    this.fechado = true;
    this.pararEnqPolling();
    if (this.reader) {
      try {
        await this.reader.cancel();
      } catch {
        // ignore
      }
      this.reader = null;
    }
    if (this.port) {
      try {
        await this.port.close();
      } catch {
        // ignore
      }
      this.port = null;
    }
    this.modoSubject.next('manual');
    this.statusSubject.next('desconectada');
    this.pesoKgSubject.next(null);
    this.infoSerialSubject.next({
      bytesRecebidos: 0,
      leiturasComPeso: 0,
      ultimosBytesHex: '',
      ultimaLeituraKg: null,
      segundosAguardando: 0,
    });
    this.pararHeartbeat();
  }

  /**
   * Modo simulador: envia um peso como se viesse da balança.
   * Use para testar sem hardware.
   */
  simularPeso(kg: number): void {
    this.modoSubject.next('simulador');
    this.statusSubject.next('conectada');
    this.pesoKgSubject.next(kg);
  }

  /** Para o simulador e limpa o último peso. */
  pararSimulador(): void {
    if (this.modo === 'simulador') {
      this.modoSubject.next('manual');
      this.statusSubject.next('desconectada');
    }
    this.pesoKgSubject.next(null);
  }

  /** Retorna o último peso recebido (serial ou simulador) ou null. */
  get ultimoPeso(): number | null {
    return this.pesoKgSubject.value;
  }
}
