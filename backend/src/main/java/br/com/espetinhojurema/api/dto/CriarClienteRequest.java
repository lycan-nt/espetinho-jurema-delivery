package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarClienteRequest(
        @NotBlank String nome,
        String telefone,
        String endereco
) {
}
