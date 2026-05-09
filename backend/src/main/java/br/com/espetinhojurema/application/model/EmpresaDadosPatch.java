package br.com.espetinhojurema.application.model;

/** Entrada para atualização dos dados da empresa (painel atendimento). */
public record EmpresaDadosPatch(
        String cnpj,
        String nomeEmpresa,
        String endereco,
        String telefone,
        String email,
        String instagram,
        ComandaCabecalhoCampos comandaCabecalho) {}
