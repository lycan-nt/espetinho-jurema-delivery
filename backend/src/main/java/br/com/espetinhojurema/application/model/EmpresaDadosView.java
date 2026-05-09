package br.com.espetinhojurema.application.model;

/** Dados cadastrais da empresa exibidos e editados no painel de atendimento. */
public record EmpresaDadosView(
        String cnpj,
        String nomeEmpresa,
        String endereco,
        String telefone,
        String email,
        String instagram,
        ComandaCabecalhoCampos comandaCabecalho) {}
