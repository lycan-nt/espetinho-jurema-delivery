package br.com.delivere.acai.comanda;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RemoverItemRequest {

    @NotNull(message = "Peso do item é obrigatório")
    @DecimalMin(value = "0", message = "Peso não pode ser negativo")
    private BigDecimal pesoKg;

    @NotNull(message = "Valor total do item é obrigatório")
    @DecimalMin(value = "0", message = "Valor não pode ser negativo")
    private BigDecimal valorTotal;

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
}
