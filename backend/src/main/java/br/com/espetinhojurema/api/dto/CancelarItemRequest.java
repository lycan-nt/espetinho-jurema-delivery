package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.Min;

/**
 * Body opcional em POST cancelar item. {@code quantidade} ausente ou {@code null} cancela a linha inteira.
 */
public record CancelarItemRequest(@Min(1) Integer quantidade) {}
