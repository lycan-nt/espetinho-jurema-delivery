package br.com.espetinhojurema.infrastructure.backup;

import br.com.espetinhojurema.application.service.BackupConfigOperacaoService;
import java.time.ZonedDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Disparo dos backups conforme horários e dias armazenados em {@code configuracao_sistema} (editável na tela de
 * backup). Avaliação a cada minuto no fuso {@code app.backup.schedule-zone}.
 */
@Component
@ConditionalOnProperty(name = "app.backup.enabled", havingValue = "true", matchIfMissing = true)
public class H2BackupScheduler {

    private final H2BackupRunner h2BackupRunner;
    private final BackupConfigOperacaoService backupConfigOperacaoService;

    public H2BackupScheduler(H2BackupRunner h2BackupRunner, BackupConfigOperacaoService backupConfigOperacaoService) {
        this.h2BackupRunner = h2BackupRunner;
        this.backupConfigOperacaoService = backupConfigOperacaoService;
    }

    @Scheduled(cron = "${app.backup.scheduler-tick-cron:0 * * * * *}", zone = "${app.backup.schedule-zone:America/Sao_Paulo}")
    public void aCadaMinutoVerificarAgendamento() {
        if (!backupConfigOperacaoService.isBackupAgendamentoHabilitado()) {
            return;
        }
        ZonedDateTime agora = ZonedDateTime.now(backupConfigOperacaoService.getScheduleZone());
        if (backupConfigOperacaoService.deveDispararBackupNesteMinuto(agora)) {
            h2BackupRunner.executarBackupAgendado();
        }
    }
}
