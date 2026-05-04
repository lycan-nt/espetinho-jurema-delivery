package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        @NotBlank @Size(max = 120) String nomeExibicao,
        @NotNull PerfilUsuario perfil,
        @NotNull Boolean ativo,
        String senha) {}
