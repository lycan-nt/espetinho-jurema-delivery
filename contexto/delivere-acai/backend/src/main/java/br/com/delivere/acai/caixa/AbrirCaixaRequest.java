package br.com.delivere.acai.caixa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AbrirCaixaRequest {

    @NotNull(message = "Informe o valor em caixa")
    @DecimalMin(value = "0", message = "Valor não pode ser negativo")
    private BigDecimal valorAbertura;

    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    public void setValorAbertura(BigDecimal valorAbertura) {
        this.valorAbertura = valorAbertura;
    }
}
