package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PerfilUsuario;

public record UsuarioAdminView(Long id, String login, String nomeExibicao, PerfilUsuario perfil, boolean ativo) {}
