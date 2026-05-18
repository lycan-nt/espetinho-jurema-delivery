package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.BackupConfigUpdateRequest;
import br.com.espetinhojurema.application.model.BackupConfigView;
import br.com.espetinhojurema.application.model.BackupFolderPickView;
import br.com.espetinhojurema.application.service.BackupConfigOperacaoService;
import br.com.espetinhojurema.application.service.BackupPastaSistemaService;
import br.com.espetinhojurema.application.service.BackupSobDemandaOperacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config/backup")
@PreAuthorize("hasRole('ATENDIMENTO')")
public class BackupRestController {

    private static final Logger log = LoggerFactory.getLogger(BackupRestController.class);

    private final BackupConfigOperacaoService backupConfigOperacaoService;
    private final BackupPastaSistemaService backupPastaSistemaService;
    private final BackupSobDemandaOperacaoService backupSobDemandaOperacaoService;

    public BackupRestController(
            BackupConfigOperacaoService backupConfigOperacaoService,
            BackupPastaSistemaService backupPastaSistemaService,
            BackupSobDemandaOperacaoService backupSobDemandaOperacaoService) {
        this.backupConfigOperacaoService = backupConfigOperacaoService;
        this.backupPastaSistemaService = backupPastaSistemaService;
        this.backupSobDemandaOperacaoService = backupSobDemandaOperacaoService;
    }

    @GetMapping
    public BackupConfigView obter() {
        return backupConfigOperacaoService.obter();
    }

    @PatchMapping
    public BackupConfigView atualizar(@RequestBody BackupConfigUpdateRequest body) {
        return backupConfigOperacaoService.atualizar(body);
    }

    /** Dispara um backup imediato (mesma pasta efetiva; não depende do agendamento). */
    @PostMapping("/executar-agora")
    public BackupConfigView executarAgora() {
        log.info("POST /api/v1/config/backup/executar-agora");
        return backupSobDemandaOperacaoService.executarAgora();
    }

    /**
     * Abre o seletor de pastas do sistema operacional <strong>no computador onde a API está em execução</strong>
     * (não no dispositivo do navegador).
     */
    @PostMapping("/selecionar-pasta")
    public BackupFolderPickView selecionarPastaNoServidor() {
        log.info("POST /api/v1/config/backup/selecionar-pasta — abrindo seletor no servidor");
        BackupFolderPickView r = backupPastaSistemaService.selecionarPastaLocal();
        log.info("Seletor concluído: cancelado={}, path={}", r.cancelado(), r.path());
        return r;
    }
}
