package br.com.espetinhojurema.domain.model;

/** Ponto de cocção aplicável a espetinhos de carne (e similares). */
public enum PontoCarne {
    MAL_PASSADA("Mal passada"),
    AO_PONTO("Ao ponto"),
    BEM_PASSADA("Bem passada");

    private final String rotulo;

    PontoCarne(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Rótulo curto para cupom/comanda/cozinha. */
    public String rotulo() {
        return rotulo;
    }
}
