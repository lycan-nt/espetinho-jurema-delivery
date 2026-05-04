import { Injectable } from '@angular/core';
import * as QRCode from 'qrcode';
import { PIX_CONFIG } from '../config/pix.config';

/**
 * Gera o payload PIX (BR Code) estático para chave telefone e o QR Code em data URL.
 * Formato EMV conforme documentação do Banco Central.
 */
@Injectable({
  providedIn: 'root',
})
export class PixQrService {

  /**
   * Gera o payload PIX estático (Copia e Cola) no formato BR Code.
   * @param valor Valor da transação em R$ (opcional). Se informado, o app PIX já abre com o valor preenchido.
   */
  gerarPayloadPix(valor?: number): string {
    let chave = PIX_CONFIG.chave.replace(/\D/g, '');
    // E.164 Brasil: +55 + DDD + número. O "+" no payload ajuda Nubank/outros a identificarem como telefone.
    if (chave.length >= 10 && chave.length <= 11 && !chave.startsWith('55')) {
      chave = '+55' + chave;
    } else if (chave.startsWith('55') && chave.length >= 12) {
      chave = '+' + chave;
    }
    const nome = this.normalizar(PIX_CONFIG.nomeRecebedor).slice(0, 25);
    const cidade = this.normalizar(PIX_CONFIG.cidade).slice(0, 15);

    // Subcampo 00: GUI BR.GOV.BCB.PIX (14 chars). Subcampo 01: chave PIX (telefone E.164 com +).
    const gui = '0014BR.GOV.BCB.PIX';
    const keyPart = '01' + String(chave.length).padStart(2, '0') + chave;
    const field26 = gui + keyPart;

    // Campo 54 = Transaction Amount (opcional). Formato: 1-13 chars, ex: "123.45" (BCB).
    const valorStr = valor != null && valor >= 0
      ? Number(valor).toFixed(2)
      : '';
    const field54 = valorStr ? '54' + String(valorStr.length).padStart(2, '0') + valorStr : '';

    const payload =
      '000201' +
      '26' + String(field26.length).padStart(2, '0') + field26 +
      '52040000' +
      '5303986' +
      field54 +
      '5802BR' +
      '59' + String(nome.length).padStart(2, '0') + nome +
      '60' + String(cidade.length).padStart(2, '0') + cidade +
      '62070503***' +
      '6304';

    const crc = this.crc16CcittFfff(payload);
    return payload + crc;
  }

  /**
   * Retorna uma Promise com o data URL do QR Code (para usar em <img [src]="...">).
   * @param valor Valor em R$ para pré-preencher no app PIX (opcional).
   */
  async gerarQrDataUrl(valor?: number): Promise<string> {
    const payload = this.gerarPayloadPix(valor);
    return QRCode.toDataURL(payload, { width: 260, margin: 2, color: { dark: '#000000', light: '#ffffff' } });
  }

  private normalizar(s: string): string {
    return s
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^A-Z0-9\s]/g, '');
  }

  /**
   * CRC16-CCITT-FFFF conforme BCB (polinômio 0x1021, valor inicial 0xFFFF).
   * Usa mesma lógica da lib pix.js (referência) para compatibilidade com leitores PIX.
   */
  private crc16CcittFfff(str: string): string {
    const bytes = new TextEncoder().encode(str);
    const table = this.getCrc16CcittTable();
    let crc = 0xffff;
    for (let i = 0; i < bytes.length; i++) {
      const c = bytes[i];
      const j = (c ^ (crc >> 8)) & 0xff;
      crc = (table[j] ^ (crc << 8)) & 0xffff;
    }
    return (crc & 0xffff).toString(16).toUpperCase().padStart(4, '0');
  }

  /** Tabela CRC-16-CCITT (polinômio 0x1021) usada por implementações de referência do PIX. */
  private getCrc16CcittTable(): Uint16Array {
    return new Uint16Array([
      0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
      0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
      0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
      0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
      0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
      0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
      0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
      0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70, 0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
      0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
      0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
      0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
      0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
      0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
      0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
      0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
      0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0,
    ]);
  }
}
