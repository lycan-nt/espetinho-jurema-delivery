package br.com.espetinhojurema.api.dto;

import java.util.List;

/**
 * @param diretorio Caminho absoluto no servidor; {@code null} ou vazio remove valor gravado.
 * @param criarDiretorioSeNaoExistir Criar pasta ao salvar diretório, quando {@code true}.
 * @param backupHora1 Se informado (não {@code null}), atualiza agendamento completo (todos os campos de backup* abaixo
 *     devem vir juntos).
 */
public record BackupConfigUpdateRequest(
        String diretorio,
        Boolean criarDiretorioSeNaoExistir,
        Integer backupHora1,
        Integer backupMinuto1,
        Integer backupHora2,
        Integer backupMinuto2,
        List<String> backupDiasSemana) {}
