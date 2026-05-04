package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank @Size(max = 64) String login,
        @NotBlank @Size(max = 120) String nomeExibicao,
        @NotNull PerfilUsuario perfil,
        @NotBlank @Size(min = 6, max = 72) String senha) {}
