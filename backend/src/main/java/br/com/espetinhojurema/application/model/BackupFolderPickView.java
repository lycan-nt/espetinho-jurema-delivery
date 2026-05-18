package br.com.espetinhojurema.application.model;

/**
 * Resposta ao pedido de abrir o seletor de pasta no servidor ({@code POST .../selecionar-pasta}).
 *
 * @param path Caminho absoluto no SO do servidor ou {@code null} se cancelado.
 * @param cancelado {@code true} se o usuário fechou o diálogo sem confirmar.
 */
public record BackupFolderPickView(String path, boolean cancelado) {}
