package br.com.espetinhojurema.application.model;

/** Campos do cadastro da empresa que podem aparecer no cabeçalho da comanda de cozinha. */
public record ComandaCabecalhoCampos(
        boolean cnpj,
        boolean nomeEmpresa,
        boolean endereco,
        boolean telefone,
        boolean email,
        boolean instagram) {

    public static ComandaCabecalhoCampos todosVisiveis() {
        return new ComandaCabecalhoCampos(true, true, true, true, true, true);
    }
}
