package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PerfilUsuario;

public record UsuarioLogadoResponse(String login, String nome, PerfilUsuario perfil) {}
