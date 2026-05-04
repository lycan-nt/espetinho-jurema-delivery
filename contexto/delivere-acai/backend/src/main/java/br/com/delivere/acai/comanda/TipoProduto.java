package br.com.delivere.acai.comanda;

/**
 * Tipo do produto no lançamento: por peso (açaí/sorvete) ou preço fixo (padrão).
 */
public enum TipoProduto {
    /** Produto vendido por peso (kg × preço/kg). */
    POR_PESO,
    /** Produto com preço fixo (apenas valor). */
    PRECO_FIXO
}
