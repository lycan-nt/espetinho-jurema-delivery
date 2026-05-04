package br.com.espetinhojurema.api.dto;

/** {@code nomeImpressoraLp} pode ser {@code null} para limpar (usa só impressão pelo navegador). */
public record ImpressaoConfigUpdateRequest(String nomeImpressoraLp) {}
