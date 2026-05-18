package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.BackupConfigView;
import br.com.espetinhojurema.infrastructure.backup.H2BackupRunner;
import org.springframework.stereotype.Service;

@Service
public class BackupSobDemandaOperacaoService {

    private final H2BackupRunner h2BackupRunner;
    private final BackupConfigOperacaoService backupConfigOperacaoService;

    public BackupSobDemandaOperacaoService(
            H2BackupRunner h2BackupRunner, BackupConfigOperacaoService backupConfigOperacaoService) {
        this.h2BackupRunner = h2BackupRunner;
        this.backupConfigOperacaoService = backupConfigOperacaoService;
    }

    public BackupConfigView executarAgora() {
        h2BackupRunner.executarBackupSobDemanda();
        return backupConfigOperacaoService.obter();
    }
}
