export interface Caixa {
  id: number;
  data: string;
  valorAbertura: number;
  dataHoraAbertura: string;
  dataHoraFechamento?: string;
  valorFechamento?: number;
  valorRetirada?: number;
  openedByUsername?: string;
  closedByUsername?: string;
  status: string;
  spreadsheetId?: string;
  sheetName?: string;
  reabertura?: boolean;
}

export interface CaixaStatus {
  needsAbertura: boolean;
  caixaAberto: boolean;
  /** True quando o caixa já foi fechado hoje (reabertura). */
  caixaFechadoHoje: boolean;
  caixa: Caixa | null;
}

export interface FecharCaixaRequest {
  valorFechamento: number;
  valorRetirada?: number;
}
