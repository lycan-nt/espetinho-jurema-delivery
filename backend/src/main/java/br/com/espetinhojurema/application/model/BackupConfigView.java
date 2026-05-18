package br.com.espetinhojurema.application.model;

import java.time.Instant;
import java.util.List;

public record BackupConfigView(
        /** Valor persistido; {@code null} ou vazio ⇒ usa o padrão do sistema. */
        String diretorioGravadoNoBanco,
        String diretorioEfetivo,
        Instant ultimoBackupSucessoEm,
        Instant ultimoErroEm,
        String ultimoErroMensagem,
        boolean agendamentoAtivo,
        int backupHora1,
        int backupMinuto1,
        int backupHora2,
        int backupMinuto2,
        /** Dias em que a rotina roda: MON … SUN (fuso {@link #fusoHorarioAgendamento}). */
        List<String> backupDiasSemana,
        /** Texto descrevendo dias e horários conforme configurado. */
        String agendamentoResumo,
        /** Resumo amigável do estado (ativa, desativada, último erro etc.). */
        String statusRotina,
        int retencaoDias,
        String fusoHorarioAgendamento) {}
