package br.com.delivere.acai.caixa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class FecharCaixaRequest {

    @NotNull(message = "Informe o valor de fechamento do caixa")
    @DecimalMin(value = "0", message = "Valor não pode ser negativo")
    private BigDecimal valorFechamento;

    /** Valor de retirada de caixa (sangria), opcional. */
    @DecimalMin(value = "0", message = "Valor de retirada não pode ser negativo")
    private BigDecimal valorRetirada;

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }

    public BigDecimal getValorRetirada() {
        return valorRetirada;
    }

    public void setValorRetirada(BigDecimal valorRetirada) {
        this.valorRetirada = valorRetirada;
    }
}
