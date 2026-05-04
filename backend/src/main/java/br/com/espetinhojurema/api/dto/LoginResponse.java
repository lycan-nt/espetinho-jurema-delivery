package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PerfilUsuario;

public record LoginResponse(
        String token,
        String tipoToken,
        long expiraEmSegundos,
        String nome,
        PerfilUsuario perfil,
        String login) {}
