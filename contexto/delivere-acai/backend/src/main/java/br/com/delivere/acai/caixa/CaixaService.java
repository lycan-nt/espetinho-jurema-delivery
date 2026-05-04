package br.com.delivere.acai.caixa;

import br.com.delivere.acai.comanda.ComandaService;
import br.com.delivere.acai.comanda.RelatorioDTO;
import br.com.delivere.acai.sheets.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CaixaService {

    private final CaixaRepository repository;
    private final GoogleSheetsService googleSheetsService;
    private final ComandaService comandaService;

    @Value("${app.google.sheets.credentials-path:}")
    private String credentialsPath;

    @Value("${app.google.sheets.titulo-planilha-dia:Relatório Mix Açaí}")
    private String tituloPlanilhaDia;

    public CaixaService(CaixaRepository repository, GoogleSheetsService googleSheetsService,
                        ComandaService comandaService) {
        this.repository = repository;
        this.googleSheetsService = googleSheetsService;
        this.comandaService = comandaService;
    }

    /**
     * Retorna o caixa aberto de hoje, se existir.
     */
    public Optional<Caixa> getCaixaAbertoHoje() {
        return repository.findByDataAndStatus(LocalDate.now(), "ABERTO");
    }

    /**
     * True quando não há caixa aberto hoje (primeira abertura do dia ou após fechamento).
     */
    public boolean precisaAberturaHoje() {
        return getCaixaAbertoHoje().isEmpty();
    }

    /**
     * True quando já existe ao menos um caixa fechado hoje (reabertura).
     */
    public boolean caixaFechadoHoje() {
        return repository.existsByDataAndStatus(LocalDate.now(), "FECHADO");
    }

    @Transactional
    public Caixa abrir(BigDecimal valorAbertura) {
        LocalDate hoje = LocalDate.now();
        if (getCaixaAbertoHoje().isPresent()) {
            throw new IllegalStateException("Já existe caixa aberto hoje.");
        }
        Caixa caixa = new Caixa();
        caixa.setReabertura(repository.existsByDataAndStatus(hoje, "FECHADO"));
        caixa.setData(hoje);
        caixa.setValorAbertura(valorAbertura != null ? valorAbertura : BigDecimal.ZERO);
        caixa.setDataHoraAbertura(LocalDateTime.now());
        caixa.setStatus("ABERTO");
        caixa.setOpenedByUsername(getCurrentUsername());

        if (credentialsPath != null && !credentialsPath.isBlank()) {
            String planilhaDiariaId = googleSheetsService.getSpreadsheetIdPlanilhaDiaria();
            if (planilhaDiariaId != null && !planilhaDiariaId.isBlank()) {
                try {
                    String sheetTitle = caixa.isReabertura() ? hoje + "-reabertura" : hoje.toString();
                    String sheetName = googleSheetsService.criarAbaNoSpreadsheet(planilhaDiariaId, sheetTitle);
                    caixa.setSpreadsheetId(planilhaDiariaId);
                    caixa.setSheetName(sheetName);
                    RelatorioDTO relatorioInicial = comandaService.relatorio(hoje, hoje);
                    googleSheetsService.enviarRelatorioParaPlanilha(planilhaDiariaId, sheetName, relatorioInicial);
                    atualizarResumoGeralPlanilhaDiaria(planilhaDiariaId);
                } catch (Exception e) {
                    throw new RuntimeException("Caixa aberto, mas falha ao criar/enviar planilha do dia: " + e.getMessage(), e);
                }
            }
        }

        return repository.save(caixa);
    }

    @Transactional
    public Caixa fechar(BigDecimal valorFechamento, BigDecimal valorRetirada) {
        Caixa caixa = getCaixaAbertoHoje()
                .orElseThrow(() -> new IllegalStateException("Não há caixa aberto hoje para fechar."));
        caixa.setDataHoraFechamento(LocalDateTime.now());
        caixa.setClosedByUsername(getCurrentUsername());
        caixa.setValorFechamento(valorFechamento != null ? valorFechamento : BigDecimal.ZERO);
        caixa.setValorRetirada(valorRetirada != null ? valorRetirada : BigDecimal.ZERO);
        caixa.setStatus("FECHADO");

        if (caixa.getSpreadsheetId() != null && !caixa.getSpreadsheetId().isBlank()) {
            try {
                RelatorioDTO relatorioFinal = comandaService.relatorio(caixa.getData(), caixa.getData());
                googleSheetsService.enviarRelatorioParaPlanilha(caixa.getSpreadsheetId(), caixa.getSheetName(), relatorioFinal);
                atualizarResumoGeralPlanilhaDiaria(caixa.getSpreadsheetId());
            } catch (Exception ignored) {
                // não falha o fechamento se o envio der erro
            }
        }

        return repository.save(caixa);
    }

    /**
     * Envia o relatório do dia para a planilha do caixa aberto (chamado pelo job periódico).
     */
    public void enviarRelatorioDiarioParaPlanilha() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return;
        }
        getCaixaAbertoHoje().ifPresent(caixa -> {
            if (caixa.getSpreadsheetId() == null || caixa.getSpreadsheetId().isBlank()) {
                return;
            }
            try {
                RelatorioDTO relatorio = comandaService.relatorio(caixa.getData(), caixa.getData());
                googleSheetsService.enviarRelatorioParaPlanilha(caixa.getSpreadsheetId(), caixa.getSheetName(), relatorio);
                atualizarResumoGeralPlanilhaDiaria(caixa.getSpreadsheetId());
            } catch (Exception ignored) {
                // job silencioso
            }
        });
    }

    /** Atualiza a primeira aba "Resumo" da planilha diária com totais de todas as vendas (todas as datas). */
    private void atualizarResumoGeralPlanilhaDiaria(String planilhaDiariaId) {
        if (planilhaDiariaId == null || planilhaDiariaId.isBlank()) return;
        try {
            RelatorioDTO relatorioGeral = comandaService.relatorio(LocalDate.of(2000, 1, 1), LocalDate.now());
            googleSheetsService.atualizarResumoGeral(planilhaDiariaId, relatorioGeral);
        } catch (Exception ignored) {
            // não falha a operação principal
        }
    }

    private static String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                ? auth.getName()
                : null;
    }
}
