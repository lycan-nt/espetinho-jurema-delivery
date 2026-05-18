package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.api.dto.BackupConfigUpdateRequest;
import br.com.espetinhojurema.application.model.BackupConfigView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupConfigOperacaoService {

    private static final Set<String> DIAS_API_VALIDOS =
            Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;

    private final String defaultBackupDirectory;
    private final boolean backupEnabled;
    private final int retentionDays;
    private final String scheduleZoneId;

    public BackupConfigOperacaoService(
            ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository,
            @Value("${app.backup.directory:./data/backups}") String defaultBackupDirectory,
            @Value("${app.backup.enabled:true}") boolean backupEnabled,
            @Value("${app.backup.retention-days:14}") int retentionDays,
            @Value("${app.backup.schedule-zone:America/Sao_Paulo}") String scheduleZoneId) {
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
        this.defaultBackupDirectory = defaultBackupDirectory;
        this.backupEnabled = backupEnabled;
        this.retentionDays = retentionDays;
        this.scheduleZoneId = scheduleZoneId;
    }

    @Transactional(readOnly = true)
    public BackupConfigView obter() {
        ConfiguracaoSistemaEntity e = carregarOuCriarConfig();
        return montarView(e);
    }

    @Transactional
    public BackupConfigView atualizar(BackupConfigUpdateRequest body) {
        boolean criar = body.criarDiretorioSeNaoExistir() == null || body.criarDiretorioSeNaoExistir();
        ConfiguracaoSistemaEntity e = carregarOuCriarConfig();
        if (body.diretorio() != null) {
            aplicarDiretorio(e, body.diretorio(), criar);
        }
        if (body.backupHora1() != null) {
            validarEaplicarAgendamento(e, body);
        }
        configuracaoSistemaJpaRepository.save(e);
        return montarView(e);
    }

    /** Mantido para chamadas que só atualizam pasta (testes / legado). */
    @Transactional
    public BackupConfigView atualizar(String diretorioInformado, boolean criarDiretorioSeNaoExistir) {
        return atualizar(new BackupConfigUpdateRequest(
                diretorioInformado, criarDiretorioSeNaoExistir, null, null, null, null, null));
    }

    /**
     * Usado pelo agendador: {@code true} se o instante (já no fuso do estabelecimento) bate com um dos horários
     * configurados e o dia da semana está habilitado.
     */
    @Transactional(readOnly = true)
    public boolean deveDispararBackupNesteMinuto(ZonedDateTime agoraNoFusoEstabelecimento) {
        if (!backupEnabled) {
            return false;
        }
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .map(e -> correspondeAEsteMinuto(e, agoraNoFusoEstabelecimento))
                .orElse(false);
    }

    public Path resolverDiretorioEfetivo() {
        ConfiguracaoSistemaEntity e = carregarOuCriarConfig();
        String gravado = e.getBackupDiretorio();
        if (gravado == null || gravado.isBlank()) {
            return Path.of(defaultBackupDirectory).toAbsolutePath().normalize();
        }
        return Path.of(gravado.trim()).toAbsolutePath().normalize();
    }

    @Transactional
    public void registrarSucesso(Instant quando) {
        ConfiguracaoSistemaEntity e = carregarOuCriarConfig();
        e.setBackupUltimoSucesso(quando);
        e.setBackupUltimoErroEm(null);
        e.setBackupUltimoErroMsg(null);
        configuracaoSistemaJpaRepository.save(e);
    }

    @Transactional
    public void registrarErro(Instant quando, String mensagem) {
        ConfiguracaoSistemaEntity e = carregarOuCriarConfig();
        e.setBackupUltimoErroEm(quando);
        String m = mensagem == null ? "Erro desconhecido" : mensagem;
        if (m.length() > 1900) {
            m = m.substring(0, 1900) + "…";
        }
        e.setBackupUltimoErroMsg(m);
        configuracaoSistemaJpaRepository.save(e);
    }

    public boolean isBackupAgendamentoHabilitado() {
        return backupEnabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getScheduleZoneId() {
        return scheduleZoneId;
    }

    public ZoneId getScheduleZone() {
        return ZoneId.of(scheduleZoneId);
    }

    public String getDefaultBackupDirectory() {
        return Path.of(defaultBackupDirectory).toAbsolutePath().normalize().toString();
    }

    private boolean correspondeAEsteMinuto(ConfiguracaoSistemaEntity e, ZonedDateTime zdt) {
        Set<DayOfWeek> dias = parseDiasParaConjunto(e.getBackupAgendDiasSemana());
        if (!dias.contains(zdt.getDayOfWeek())) {
            return false;
        }
        int alvo = zdt.getHour() * 60 + zdt.getMinute();
        int t1 = valorHora1(e) * 60 + valorMinuto1(e);
        int t2 = valorHora2(e) * 60 + valorMinuto2(e);
        return alvo == t1 || alvo == t2;
    }

    private void aplicarDiretorio(ConfiguracaoSistemaEntity e, String diretorioInformado, boolean criarDiretorioSeNaoExistir) {
        if (diretorioInformado == null || diretorioInformado.isBlank()) {
            e.setBackupDiretorio(null);
            return;
        }
        String t = diretorioInformado.trim();
        if (t.contains("..")) {
            throw new BusinessException("Informe o caminho absoluto completo da pasta no servidor, sem '..'.");
        }
        Path alvo = Path.of(t).toAbsolutePath().normalize();
        if (Files.exists(alvo)) {
            if (!Files.isDirectory(alvo)) {
                throw new BusinessException("O caminho informado existe mas não é uma pasta: " + alvo);
            }
        } else if (criarDiretorioSeNaoExistir) {
            try {
                Files.createDirectories(alvo);
            } catch (IOException ex) {
                throw new BusinessException(
                        "Não foi possível criar o diretório: " + alvo + " (" + ex.getMessage() + ")");
            }
        } else {
            throw new BusinessException(
                    "Diretório não existe. Marque criar pasta ou crie manualmente: " + alvo);
        }
        e.setBackupDiretorio(alvo.toString());
    }

    private void validarEaplicarAgendamento(ConfiguracaoSistemaEntity e, BackupConfigUpdateRequest body) {
        if (body.backupHora1() == null
                || body.backupMinuto1() == null
                || body.backupHora2() == null
                || body.backupMinuto2() == null) {
            throw new BusinessException(
                    "Para alterar o agendamento, informe os dois horários (hora e minuto de cada um).");
        }
        if (body.backupDiasSemana() == null) {
            throw new BusinessException(
                    "Informe a lista de dias (pode ser vazia para não disparar backup automático nestes horários).");
        }
        int h1 = body.backupHora1();
        int m1 = body.backupMinuto1();
        int h2 = body.backupHora2();
        int m2 = body.backupMinuto2();
        if (h1 < 0 || h1 > 23 || m1 < 0 || m1 > 59 || h2 < 0 || h2 > 23 || m2 < 0 || m2 > 59) {
            throw new BusinessException("Horário inválido. Use horas 0–23 e minutos 0–59.");
        }
        List<String> diasNormalizados = new ArrayList<>();
        for (String d : body.backupDiasSemana()) {
            if (d == null || d.isBlank()) {
                continue;
            }
            String u = d.trim().toUpperCase(Locale.ROOT);
            if (!DIAS_API_VALIDOS.contains(u)) {
                throw new BusinessException("Dia da semana inválido: " + d + ". Use MON, TUE, WED, THU, FRI, SAT, SUN.");
            }
            if (!diasNormalizados.contains(u)) {
                diasNormalizados.add(u);
            }
        }
        diasNormalizados.sort(Comparator.comparingInt(BackupConfigOperacaoService::ordemDiaApi));
        e.setBackupAgendHora1(h1);
        e.setBackupAgendMinuto1(m1);
        e.setBackupAgendHora2(h2);
        e.setBackupAgendMinuto2(m2);
        e.setBackupAgendDiasSemana(diasNormalizados.isEmpty() ? "" : String.join(",", diasNormalizados));
    }

    private static int ordemDiaApi(String codigo) {
        return switch (codigo) {
            case "MON" -> 1;
            case "TUE" -> 2;
            case "WED" -> 3;
            case "THU" -> 4;
            case "FRI" -> 5;
            case "SAT" -> 6;
            case "SUN" -> 7;
            default -> 9;
        };
    }

    private BackupConfigView montarView(ConfiguracaoSistemaEntity e) {
        Path efetivo = resolverDiretorioAPartirDaEntidade(e);
        String status = montarStatusRotina(e);
        String gravado = e.getBackupDiretorio();
        if (gravado != null && gravado.isBlank()) {
            gravado = null;
        }
        List<String> dias = diasOrdenadosApi(e.getBackupAgendDiasSemana());
        return new BackupConfigView(
                gravado,
                efetivo.toString(),
                e.getBackupUltimoSucesso(),
                e.getBackupUltimoErroEm(),
                e.getBackupUltimoErroMsg(),
                backupEnabled,
                valorHora1(e),
                valorMinuto1(e),
                valorHora2(e),
                valorMinuto2(e),
                dias,
                montarResumoAgendamento(e, dias),
                status,
                retentionDays,
                scheduleZoneId);
    }

    private String montarResumoAgendamento(ConfiguracaoSistemaEntity e, List<String> dias) {
        int h1 = valorHora1(e);
        int m1 = valorMinuto1(e);
        int h2 = valorHora2(e);
        int m2 = valorMinuto2(e);
        if (dias == null || dias.isEmpty()) {
            return String.format(
                    Locale.ROOT,
                    "Nenhum dia selecionado — sem execução automática nestes horários. "
                            + "Horários gravados: %02d:%02d e %02d:%02d (fuso %s).",
                    h1,
                    m1,
                    h2,
                    m2,
                    scheduleZoneId);
        }
        String diasPt = formatarDiasPt(dias);
        return String.format(
                Locale.ROOT,
                "%s, às %02d:%02d e %02d:%02d (fuso %s).",
                diasPt,
                h1,
                m1,
                h2,
                m2,
                scheduleZoneId);
    }

    private static String formatarDiasPt(List<String> dias) {
        if (dias == null || dias.isEmpty()) {
            return "nenhum dia";
        }
        return dias.stream().map(BackupConfigOperacaoService::nomeDiaPt).collect(Collectors.joining(", "));
    }

    private static String nomeDiaPt(String api) {
        if (api == null) {
            return "";
        }
        return switch (api) {
            case "MON" -> "seg";
            case "TUE" -> "ter";
            case "WED" -> "qua";
            case "THU" -> "qui";
            case "FRI" -> "sex";
            case "SAT" -> "sáb";
            case "SUN" -> "dom";
            default -> api;
        };
    }

    private Path resolverDiretorioAPartirDaEntidade(ConfiguracaoSistemaEntity e) {
        String g = e.getBackupDiretorio();
        if (g == null || g.isBlank()) {
            return Path.of(defaultBackupDirectory).toAbsolutePath().normalize();
        }
        return Path.of(g.trim()).toAbsolutePath().normalize();
    }

    private String montarStatusRotina(ConfiguracaoSistemaEntity e) {
        if (!backupEnabled) {
            return "Desativada (app.backup.enabled=false).";
        }
        if (agendamentoSemDiasMarcados(e)) {
            return "Sem dias marcados — o backup automático não será disparado até incluir dias e salvar.";
        }
        Instant ok = e.getBackupUltimoSucesso();
        Instant erro = e.getBackupUltimoErroEm();
        if (erro != null && (ok == null || erro.isAfter(ok))) {
            return "Última execução automática falhou; ver mensagem de erro abaixo.";
        }
        if (ok != null) {
            return "Ativa — último backup automático concluído com sucesso.";
        }
        return "Ativa — aguardando primeira execução no horário agendado.";
    }

    /** {@code true} quando o cliente salvou lista vazia de dias (string vazia no banco). {@code null} = legado. */
    private boolean agendamentoSemDiasMarcados(ConfiguracaoSistemaEntity e) {
        String s = e.getBackupAgendDiasSemana();
        return s != null && s.isBlank();
    }

    private ConfiguracaoSistemaEntity carregarOuCriarConfig() {
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .orElseGet(() -> {
                    var c = new ConfiguracaoSistemaEntity();
                    c.setId(ConfiguracaoSistemaEntity.ID_UNICO);
                    return configuracaoSistemaJpaRepository.save(c);
                });
    }

    private int valorHora1(ConfiguracaoSistemaEntity e) {
        return e.getBackupAgendHora1() != null ? e.getBackupAgendHora1() : 19;
    }

    private int valorMinuto1(ConfiguracaoSistemaEntity e) {
        return e.getBackupAgendMinuto1() != null ? e.getBackupAgendMinuto1() : 0;
    }

    private int valorHora2(ConfiguracaoSistemaEntity e) {
        return e.getBackupAgendHora2() != null ? e.getBackupAgendHora2() : 21;
    }

    private int valorMinuto2(ConfiguracaoSistemaEntity e) {
        return e.getBackupAgendMinuto2() != null ? e.getBackupAgendMinuto2() : 0;
    }

    private List<String> diasOrdenadosApi(String csv) {
        if (csv == null) {
            return List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
        }
        if (csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.comparingInt(BackupConfigOperacaoService::ordemDiaApi))
                .toList();
    }

    private Set<DayOfWeek> parseDiasParaConjunto(String csv) {
        if (csv == null) {
            return EnumSet.allOf(DayOfWeek.class);
        }
        if (csv.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        EnumSet<DayOfWeek> out = EnumSet.noneOf(DayOfWeek.class);
        for (String raw : csv.split(",")) {
            String t = raw.trim().toUpperCase(Locale.ROOT);
            if (t.isEmpty()) {
                continue;
            }
            out.add(
                    switch (t) {
                        case "MON" -> DayOfWeek.MONDAY;
                        case "TUE" -> DayOfWeek.TUESDAY;
                        case "WED" -> DayOfWeek.WEDNESDAY;
                        case "THU" -> DayOfWeek.THURSDAY;
                        case "FRI" -> DayOfWeek.FRIDAY;
                        case "SAT" -> DayOfWeek.SATURDAY;
                        case "SUN" -> DayOfWeek.SUNDAY;
                        default -> throw new IllegalStateException("Dia inválido: " + t);
                    });
        }
        return out;
    }
}
