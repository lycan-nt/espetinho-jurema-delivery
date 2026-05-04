package br.com.espetinhojurema.application.model;

import java.time.Instant;

public record MesaTransferenciaView(
        Long id,
        Long pedidoId,
        int mesaOrigemNumero,
        int mesaDestinoNumero,
        Instant criadoEm,
        String usuarioLogin) {}
