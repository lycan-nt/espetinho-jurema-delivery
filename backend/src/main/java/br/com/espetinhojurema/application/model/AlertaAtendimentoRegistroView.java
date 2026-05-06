package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import java.time.Instant;

public record AlertaAtendimentoRegistroView(
        String id,
        Long pedidoId,
        Integer mesaNumero,
        Instant criadoEm,
        Instant reconhecidoEm,
        String reconhecidoPor,
        TipoAlertaAtendimento tipo,
        Long itemIdMax) {
    public boolean pendente() {
        return reconhecidoEm == null;
    }
}
