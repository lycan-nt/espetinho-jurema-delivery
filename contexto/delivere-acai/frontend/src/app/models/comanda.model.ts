export type TipoComanda = 'CLIENTE' | 'MESA' | 'COMANDA';

/** Por peso (açaí/sorvete) ou preço fixo (padrão). */
export type TipoProduto = 'POR_PESO' | 'PRECO_FIXO';

export type FormaPagamento = 'PIX' | 'DINHEIRO' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO';

export interface Comanda {
  id?: number;
  tipo: TipoComanda;
  identificador: string;
  tipoProduto?: TipoProduto;
  pesoKg: number;
  precoPorKilo: number;
  valorTotal: number;
  dataHora?: string;
  dataFechamento?: string;
  status?: string;
  formaPagamento?: FormaPagamento;
  /** Chave da NFC-e (44 dígitos), quando emitida. */
  chaveNfce?: string;
  /** Protocolo de autorização SEFAZ. */
  protocoloNfce?: string;
  /** Usuário que abriu a comanda. */
  openedByUsername?: string;
  /** Usuário que fechou a comanda. */
  closedByUsername?: string;
  /** Quantidade (apenas para PRECO_FIXO ao criar; enviado no request). */
  quantidade?: number;
}

/** Item lançado em uma comanda (detalhamento para relatório). */
export interface ComandaItem {
  id: number;
  comandaId: number;
  tipoProduto: TipoProduto;
  pesoKg: number;
  precoUnitario: number;
  quantidade: number;
  valorTotal: number;
}

export interface Relatorio {
  comandas: Comanda[];
  totalVendas: number;
  totalPorFormaPagamento: Record<FormaPagamento, number>;
  /** Aberturas e fechamentos de caixa no período (pode haver mais de um por reabertura). */
  caixas?: import('./caixa.model').Caixa[];
}

export interface ComandaForm {
  tipo: TipoComanda;
  identificador: string;
  pesoKg: number;
  precoPorKilo: number;
}
