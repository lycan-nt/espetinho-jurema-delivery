package br.com.espetinhojurema.infrastructure.backup;

import br.com.espetinhojurema.application.service.BackupConfigOperacaoService;
import br.com.espetinhojurema.domain.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Execução do comando nativo H2 {@code BACKUP TO} e retenção de arquivos. Agendamento em
 * {@link H2BackupScheduler}.
 */
@Component
public class H2BackupRunner {

    private static final Logger log = LoggerFactory.getLogger(H2BackupRunner.class);

    private final DataSource dataSource;
    private final BackupConfigOperacaoService backupConfigOperacaoService;

    public H2BackupRunner(DataSource dataSource, BackupConfigOperacaoService backupConfigOperacaoService) {
        this.dataSource = dataSource;
        this.backupConfigOperacaoService = backupConfigOperacaoService;
    }

    public void executarBackupAgendado() {
        if (!backupConfigOperacaoService.isBackupAgendamentoHabilitado()) {
            return;
        }
        executarInterno(false);
    }

    /**
     * Backup imediato pela UI. Ignora horários e dias da semana; usa a mesma pasta efetiva e respeita
     * {@code app.backup.enabled}.
     */
    public void executarBackupSobDemanda() {
        if (!backupConfigOperacaoService.isBackupAgendamentoHabilitado()) {
            throw new BusinessException(
                    "Backup desabilitado na configuração do servidor (app.backup.enabled=false).");
        }
        executarInterno(true);
    }

    private void executarInterno(boolean propagarErroApi) {
        Path dir;
        try {
            dir = backupConfigOperacaoService.resolverDiretorioEfetivo();
        } catch (Exception e) {
            log.error("Backup: falha ao resolver diretório configurado", e);
            backupConfigOperacaoService.registrarErro(Instant.now(), e.getMessage());
            if (propagarErroApi) {
                throw new BusinessException("Não foi possível definir a pasta do backup: " + e.getMessage());
            }
            return;
        }
        try {
            Files.createDirectories(dir);
            String nome = "espetinho-backup-"
                    + Instant.now().toString().replace(':', '-').replace('.', '-')
                    + ".zip";
            Path arquivo = dir.resolve(nome);
            String url = arquivo.toString().replace('\\', '/');
            String sql = "BACKUP TO '" + escapeSqlString(url) + "'";
            try (var con = dataSource.getConnection();
                    var st = con.createStatement()) {
                st.execute(sql);
            }
            log.info("Backup H2 concluído: {}", arquivo);
            aplicarRetencao(dir);
            backupConfigOperacaoService.registrarSucesso(Instant.now());
        } catch (Exception e) {
            log.error(
                    propagarErroApi ? "Falha ao executar backup sob demanda do H2"
                            : "Falha ao executar backup agendado do H2",
                    e);
            backupConfigOperacaoService.registrarErro(Instant.now(), e.getMessage());
            if (propagarErroApi) {
                throw new BusinessException("Falha ao gerar o arquivo de backup: " + e.getMessage());
            }
        }
    }

    private static String escapeSqlString(String pathUrl) {
        return pathUrl.replace("'", "''");
    }

    private void aplicarRetencao(Path dir) throws IOException {
        int retentionDays = backupConfigOperacaoService.getRetentionDays();
        Instant limite = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("espetinho-backup-")
                            && p.toString().endsWith(".zip"))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant().isBefore(limite);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                            log.info("Backup antigo removido: {}", p.getFileName());
                        } catch (IOException ex) {
                            log.warn("Não foi possível remover backup {}", p, ex);
                        }
                    });
        }
    }
}
