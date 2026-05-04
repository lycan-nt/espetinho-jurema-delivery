package br.com.espetinhojurema.application.model;

import java.math.BigDecimal;

public record ProdutoVendaTotalView(long produtoId, String produtoNome, BigDecimal total) {}
