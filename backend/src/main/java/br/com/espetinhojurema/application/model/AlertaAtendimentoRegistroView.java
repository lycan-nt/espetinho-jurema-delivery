package br.com.espetinhojurema.application.model;

import java.time.Instant;

public record AlertaAtendimentoRegistroView(
        String id,
        Long pedidoId,
        Integer mesaNumero,
        Instant criadoEm,
        Instant reconhecidoEm,
        String reconhecidoPor
) {
    public boolean pendente() {
        return reconhecidoEm == null;
    }
}
