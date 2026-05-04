package br.com.espetinhojurema.application.model;

import java.math.BigDecimal;

public record ProdutoView(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Long categoriaId,
        String categoriaNome,
        String codigoImpressao,
        boolean ativo,
        int saldoEstoque
) {
}
