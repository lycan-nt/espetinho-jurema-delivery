package br.com.espetinhojurema.application.model;

/** Resultado da tentativa de enviar texto para a fila CUPS configurada no servidor. */
public record ImprimirLocalResult(boolean impressoServidor) {}
