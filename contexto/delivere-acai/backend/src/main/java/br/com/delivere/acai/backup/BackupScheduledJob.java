package br.com.delivere.acai.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executa backup do banco H2 uma vez ao dia (cron).
 * O arquivo é salvo no diretório definido em app.backup.path com o nome acaidb-backup-AAAA-MM-DD.zip.
 * Se app.backup.test-delay-ms estiver definido (ex.: 600000 = 10 min), roda um backup de teste após esse delay.
 */
@Component
public class BackupScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduledJob.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${app.backup.path:./backup}")
    private String backupPathConfig;

    @Value("${app.backup.test-delay-ms:0}")
    private long testDelayMs;

    private final DataSource dataSource;

    public BackupScheduledJob(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void agendarTeste() {
        if (testDelayMs > 0) {
            Executors.newSingleThreadScheduledExecutor()
                    .schedule(this::executarBackup, testDelayMs, TimeUnit.MILLISECONDS);
            log.info("Backup de teste agendado para daqui {} min", testDelayMs / 60_000);
        }
    }

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * ?}")
    public void executarBackup() {
        Path dir = Paths.get(backupPathConfig).toAbsolutePath();
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String nomeArquivo = "acaidb-backup-" + LocalDate.now().format(DATE_FMT) + ".zip";
            Path arquivo = dir.resolve(nomeArquivo);
            String pathParaH2 = arquivo.toString().replace("\\", "/");

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("BACKUP TO '" + pathParaH2 + "'");
            }
            log.info("Backup do banco realizado: {}", arquivo);
        } catch (Exception e) {
            log.error("Erro ao realizar backup do banco: {}", e.getMessage());
        }
    }
}
