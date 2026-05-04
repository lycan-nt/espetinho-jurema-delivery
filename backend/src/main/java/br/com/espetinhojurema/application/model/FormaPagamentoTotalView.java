package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.FormaPagamento;
import java.math.BigDecimal;

public record FormaPagamentoTotalView(FormaPagamento forma, BigDecimal total) {}
