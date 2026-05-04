/**
 * Configuração da chave PIX para exibição do QR Code ao fechar comanda com PIX.
 * Altere aqui para usar a chave e o nome da sua loja.
 */
export const PIX_CONFIG = {
  /** Chave PIX por telefone: só dígitos, DDD + número (ex: 77981283199). O código 55 (Brasil) é acrescentado automaticamente no QR. */
  chave: '77981283199',
  /** Nome do recebedor (até 25 caracteres, sem acento - ex: MIX ACAI) */
  nomeRecebedor: 'MIX ACAI',
  /** Cidade do recebedor (até 15 caracteres, sem acento) */
  cidade: 'BRASIL',
} as const;
