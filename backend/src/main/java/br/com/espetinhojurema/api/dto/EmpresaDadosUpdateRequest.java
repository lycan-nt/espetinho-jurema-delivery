package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.application.model.ComandaCabecalhoCampos;

public record EmpresaDadosUpdateRequest(
        String cnpj,
        String nomeEmpresa,
        String endereco,
        String telefone,
        String email,
        String instagram,
        ComandaCabecalhoCampos comandaCabecalho) {}
