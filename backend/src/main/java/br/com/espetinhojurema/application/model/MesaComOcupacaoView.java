package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.MesaStatus;

public record MesaComOcupacaoView(
        Long id,
        int numero,
        MesaStatus status,
        Long pedidoAbertoId,
        boolean ocupada
) {
}
