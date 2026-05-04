package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CriarProdutoRequest(
        @NotBlank @Size(max = 200) String nome,
        @Size(max = 500) String descricao,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal preco,
        @NotNull Long categoriaId,
        @Size(max = 32) String codigoImpressao,
        @NotNull Boolean ativo) {}
