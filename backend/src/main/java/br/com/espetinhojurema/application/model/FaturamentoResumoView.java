package br.com.espetinhojurema.application.model;

import java.math.BigDecimal;
import java.util.List;

public record FaturamentoResumoView(
        BigDecimal receitaTotal,
        List<FormaPagamentoTotalView> porForma,
        long pedidosEncerradosNoPeriodo,
        List<ProdutoVendaTotalView> porProduto) {}
