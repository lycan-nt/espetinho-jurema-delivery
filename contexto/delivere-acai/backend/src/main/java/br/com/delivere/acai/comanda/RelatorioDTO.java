package br.com.delivere.acai.comanda;

import br.com.delivere.acai.caixa.Caixa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class RelatorioDTO {

    private List<Comanda> comandas;
    private BigDecimal totalVendas;
    private Map<FormaPagamento, BigDecimal> totalPorFormaPagamento;
    private List<Caixa> caixas;

    public RelatorioDTO(List<Comanda> comandas, BigDecimal totalVendas,
                        Map<FormaPagamento, BigDecimal> totalPorFormaPagamento,
                        List<Caixa> caixas) {
        this.comandas = comandas;
        this.totalVendas = totalVendas;
        this.totalPorFormaPagamento = totalPorFormaPagamento;
        this.caixas = caixas != null ? caixas : List.of();
    }

    public List<Comanda> getComandas() {
        return comandas;
    }

    public BigDecimal getTotalVendas() {
        return totalVendas;
    }

    public Map<FormaPagamento, BigDecimal> getTotalPorFormaPagamento() {
        return totalPorFormaPagamento;
    }

    public List<Caixa> getCaixas() {
        return caixas;
    }
}
