package br.com.delivere.acai.comanda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AlterarCabecalhoComandaRequest {

    @NotNull(message = "Tipo é obrigatório")
    private TipoComanda tipo;

    @NotBlank(message = "Identificador é obrigatório")
    private String identificador;

    public TipoComanda getTipo() {
        return tipo;
    }

    public void setTipo(TipoComanda tipo) {
        this.tipo = tipo;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }
}
