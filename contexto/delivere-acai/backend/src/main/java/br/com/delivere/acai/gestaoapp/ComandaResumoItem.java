package br.com.delivere.acai.gestaoapp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resumo de uma comanda fechada para o documento de gestão no MongoDB. */
public class ComandaResumoItem {

    private Long id;
    private String tipo;
    private String identificador;
    private BigDecimal valorTotal;
    private LocalDateTime dataFechamento;
    private String formaPagamento;

    /** POR_PESO ou PRECO_FIXO (igual à comanda no banco). */
    private String tipoProduto;

    public ComandaResumoItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getTipoProduto() {
        return tipoProduto;
    }

    public void setTipoProduto(String tipoProduto) {
        this.tipoProduto = tipoProduto;
    }
}
