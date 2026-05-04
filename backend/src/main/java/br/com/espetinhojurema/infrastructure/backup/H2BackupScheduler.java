package br.com.espetinhojurema.infrastructure.backup;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.backup.enabled", havingValue = "true", matchIfMissing = true)
public class H2BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(H2BackupScheduler.class);

    private final DataSource dataSource;

    @Value("${app.backup.directory:./data/backups}")
    private String backupDirectory;

    @Value("${app.backup.retention-days:14}")
    private int retentionDays;

    public H2BackupScheduler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(cron = "${app.backup.cron:0 0 3 * * *}")
    public void executarBackup() {
        try {
            Path dir = Path.of(backupDirectory).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String nome = "espetinho-backup-" + Instant.now().toString().replace(':', '-').replace('.', '-') + ".zip";
            Path arquivo = dir.resolve(nome);
            String url = arquivo.toString().replace('\\', '/');
            try (var con = dataSource.getConnection(); var st = con.createStatement()) {
                st.execute("BACKUP TO '" + url + "'");
            }
            log.info("Backup H2 concluído: {}", arquivo);
            aplicarRetencao(dir);
        } catch (Exception e) {
            log.error("Falha ao executar backup agendado do H2", e);
        }
    }

    private void aplicarRetencao(Path dir) throws IOException {
        Instant limite = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("espetinho-backup-") && p.toString().endsWith(".zip"))
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
