package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.CouvertArtisticoModo;
import java.math.BigDecimal;

public record CouvertArtisticoConfigView(
        boolean ativo, CouvertArtisticoModo modo, BigDecimal valorPorPessoa) {}
