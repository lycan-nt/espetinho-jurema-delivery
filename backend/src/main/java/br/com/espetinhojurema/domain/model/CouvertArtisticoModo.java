package br.com.espetinhojurema.domain.model;

/** Forma de cobrança do couvert artístico em pedidos de mesa. */
public enum CouvertArtisticoModo {
    /** Valor fixo por mesa, independente do número de pessoas. */
    POR_MESA,
    /** Valor unitário multiplicado pelo número de pessoas na mesa. */
    POR_PESSOA;

    public String rotulo() {
        return switch (this) {
            case POR_MESA -> "por mesa";
            case POR_PESSOA -> "por pessoa";
        };
    }
}
