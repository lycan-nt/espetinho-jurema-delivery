package br.com.delivere.acai.sheets;

import br.com.delivere.acai.caixa.Caixa;
import br.com.delivere.acai.comanda.Comanda;
import br.com.delivere.acai.comanda.FormaPagamento;
import br.com.delivere.acai.comanda.RelatorioDTO;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Envia os dados do relatório de vendas para uma planilha no Google Sheets.
 * Requer Service Account (JSON) e a planilha compartilhada com o e-mail da conta de serviço.
 */
@Service
public class GoogleSheetsService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final DateTimeFormatter DATE_FMT_TITLE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    /** Nome da primeira aba na planilha diária: resumo geral de todas as vendas. */
    private static final String RESUMO_SHEET_NAME = "Resumo";
    private static final String[] LABELS_FORMA = {
            "PIX", "Dinheiro", "Cartão de crédito", "Cartão de débito"
    };

    @Value("${app.google.sheets.spreadsheet-id:}")
    private String spreadsheetId;

    /** Planilha fixa onde são criadas as abas do caixa (uma aba por dia). Se vazio, usa spreadsheet-id. */
    @Value("${app.google.sheets.spreadsheet-id-planilha-diaria:}")
    private String spreadsheetIdPlanilhaDiaria;

    @Value("${app.google.sheets.credentials-path:}")
    private String credentialsPath;

    @Value("${app.google.sheets.sheet-name:Relatório}")
    private String sheetName;

    @Value("${app.google.sheets.uma-planilha-por-dia:false}")
    private boolean umaPlanilhaPorDia;

    @Value("${app.google.sheets.titulo-planilha-dia:Relatório Mix Açaí}")
    private String tituloPlanilhaDia;

    private final PlanilhaDiaRepository planilhaDiaRepository;

    public GoogleSheetsService(PlanilhaDiaRepository planilhaDiaRepository) {
        this.planilhaDiaRepository = planilhaDiaRepository;
    }

    public void enviarRelatorio(RelatorioDTO relatorio) throws Exception {
        String targetId = spreadsheetId != null && !spreadsheetId.isBlank() ? spreadsheetId : null;
        if (targetId == null) {
            throw new IllegalStateException("Google Sheets não configurado: defina app.google.sheets.spreadsheet-id no application.properties (ou use app.google.sheets.uma-planilha-por-dia=true).");
        }
        Sheets sheets = buildSheetsClient();
        writeToSpreadsheet(sheets, targetId, relatorio);
    }

    /**
     * Envia o relatório de um único dia para a planilha daquele dia. Se não existir planilha para a data,
     * cria uma nova no Google Drive (da conta de serviço) e grava o ID no banco para reutilizar ao longo do dia.
     */
    public void enviarRelatorioParaDia(LocalDate data, RelatorioDTO relatorio) throws Exception {
        Sheets sheets = buildSheetsClient();
        String targetId = getOrCreateSpreadsheetForDay(sheets, data);
        writeToSpreadsheet(sheets, targetId, relatorio);
    }

    /**
     * Garante que exista uma aba com o nome dado na planilha. Se a aba já existir (ex.: planilha
     * foi mantida e o banco foi limpo), usa a existente e faz update; senão cria uma nova.
     * Usado na abertura do caixa: uma planilha fixa, uma aba por dia.
     */
    public String criarAbaNoSpreadsheet(String spreadsheetIdTarget, String sheetTitle) throws Exception {
        if (spreadsheetIdTarget == null || spreadsheetIdTarget.isBlank()) {
            throw new IllegalArgumentException("spreadsheetId é obrigatório.");
        }
        String title = sheetTitle != null && !sheetTitle.isBlank() ? sheetTitle : "Dia";
        Sheets sheets = buildSheetsClient();
        // Se a aba já existe, usar a existente (update) em vez de criar e dar erro 400
        if (getSheetId(sheets, spreadsheetIdTarget, title) != null) {
            return title;
        }
        // index 0 = inserir à esquerda (aba mais recente fica sempre na primeira posição)
        SheetProperties props = new SheetProperties().setTitle(title).setIndex(0);
        AddSheetRequest addSheet = new AddSheetRequest().setProperties(props);
        BatchUpdateSpreadsheetRequest batch = new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(new Request().setAddSheet(addSheet)));
        sheets.spreadsheets().batchUpdate(spreadsheetIdTarget, batch).execute();
        return title;
    }

    /**
     * Envia o relatório para uma planilha já existente (por ID). Usado pela planilha diária do caixa (job e fechamento).
     * Se sheetNameForRange for informado, escreve nessa aba; senão usa a primeira/configured.
     */
    public void enviarRelatorioParaPlanilha(String spreadsheetIdTarget, RelatorioDTO relatorio) throws Exception {
        enviarRelatorioParaPlanilha(spreadsheetIdTarget, null, relatorio);
    }

    /**
     * Envia o relatório para uma aba específica da planilha (caixa: uma aba por dia).
     */
    public void enviarRelatorioParaPlanilha(String spreadsheetIdTarget, String sheetNameForRange, RelatorioDTO relatorio) throws Exception {
        if (spreadsheetIdTarget == null || spreadsheetIdTarget.isBlank()) {
            throw new IllegalArgumentException("spreadsheetId é obrigatório.");
        }
        Sheets sheets = buildSheetsClient();
        writeToSpreadsheet(sheets, spreadsheetIdTarget, sheetNameForRange, relatorio);
    }

    public boolean isUmaPlanilhaPorDia() {
        return umaPlanilhaPorDia;
    }

    /** ID da planilha onde são criadas as abas do caixa (uma aba por dia). Se não configurado, usa spreadsheet-id. */
    public String getSpreadsheetIdPlanilhaDiaria() {
        if (spreadsheetIdPlanilhaDiaria != null && !spreadsheetIdPlanilhaDiaria.isBlank()) {
            return spreadsheetIdPlanilhaDiaria;
        }
        return spreadsheetId;
    }

    /**
     * Atualiza a primeira aba "Resumo" da planilha diária com totais gerais (todas as vendas, todas as datas)
     * e por forma de pagamento. Cria a aba se não existir e garante que ela fique como primeira aba.
     */
    public void atualizarResumoGeral(String spreadsheetIdTarget, RelatorioDTO relatorioResumo) throws Exception {
        if (spreadsheetIdTarget == null || spreadsheetIdTarget.isBlank()) {
            return;
        }
        Sheets sheets = buildSheetsClient();
        criarOuObterAbaResumo(sheets, spreadsheetIdTarget);
        List<List<Object>> rows = buildResumoRows(relatorioResumo);
        String clearRange = toA1Range(RESUMO_SHEET_NAME, "A:Z");
        sheets.spreadsheets().values().clear(spreadsheetIdTarget, clearRange, new ClearValuesRequest()).execute();
        ValueRange body = new ValueRange().setValues(rows);
        sheets.spreadsheets().values()
                .update(spreadsheetIdTarget, toA1Range(RESUMO_SHEET_NAME, "A1"), body)
                .setValueInputOption("USER_ENTERED")
                .execute();
        aplicarFormatacaoResumo(sheets, spreadsheetIdTarget, rows.size());
        moverAbaParaIndice(sheets, spreadsheetIdTarget, RESUMO_SHEET_NAME, 0);
    }

    private void criarOuObterAbaResumo(Sheets sheets, String spreadsheetIdTarget) throws java.io.IOException {
        if (getSheetId(sheets, spreadsheetIdTarget, RESUMO_SHEET_NAME) != null) {
            return;
        }
        SheetProperties props = new SheetProperties().setTitle(RESUMO_SHEET_NAME).setIndex(0);
        AddSheetRequest addSheet = new AddSheetRequest().setProperties(props);
        BatchUpdateSpreadsheetRequest batch = new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(new Request().setAddSheet(addSheet)));
        sheets.spreadsheets().batchUpdate(spreadsheetIdTarget, batch).execute();
    }

    private void moverAbaParaIndice(Sheets sheets, String spreadsheetIdTarget, String sheetTitle, int index) throws java.io.IOException {
        Integer sheetId = getSheetId(sheets, spreadsheetIdTarget, sheetTitle);
        if (sheetId == null) return;
        UpdateSheetPropertiesRequest updateProps = new UpdateSheetPropertiesRequest()
                .setProperties(new SheetProperties().setSheetId(sheetId).setIndex(index))
                .setFields("index");
        sheets.spreadsheets().batchUpdate(spreadsheetIdTarget,
                new BatchUpdateSpreadsheetRequest().setRequests(List.of(new Request().setUpdateSheetProperties(updateProps)))).execute();
    }

    private List<List<Object>> buildResumoRows(RelatorioDTO relatorio) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Resumo geral – Todas as vendas"));
        rows.add(List.of(""));
        rows.add(List.of("Total de vendas (R$)", relatorio.getTotalVendas() != null ? relatorio.getTotalVendas() : BigDecimal.ZERO));
        rows.add(List.of(""));
        rows.add(List.of("Forma de pagamento", "Total (R$)"));
        Map<FormaPagamento, BigDecimal> totalPorForma = relatorio.getTotalPorFormaPagamento();
        if (totalPorForma != null) {
            for (FormaPagamento forma : FormaPagamento.values()) {
                BigDecimal total = totalPorForma.get(forma);
                if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                    rows.add(List.of(labelForma(forma), total));
                }
            }
        }
        rows.add(List.of(""));
        rows.add(List.of("Atualizado em", java.time.LocalDateTime.now().format(DATE_TIME_FMT)));
        return rows;
    }

    private void aplicarFormatacaoResumo(Sheets sheets, String spreadsheetIdTarget, int totalRows) throws java.io.IOException {
        Integer sheetId = getSheetId(sheets, spreadsheetIdTarget, RESUMO_SHEET_NAME);
        if (sheetId == null || totalRows <= 0) return;
        Color headerBg = new Color().setRed(0.27f).setGreen(0.16f).setBlue(0.29f);
        CellFormat headerFormat = new CellFormat()
                .setBackgroundColor(headerBg)
                .setTextFormat(new TextFormat().setBold(true).setForegroundColor(new Color().setRed(1f).setGreen(1f).setBlue(1f)).setFontSize(14));
        Color formaHeaderBg = new Color().setRed(0.91f).setGreen(0.84f).setBlue(0.94f);
        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(0).setEndColumnIndex(2))
                .setCell(new CellData().setUserEnteredFormat(headerFormat))
                .setFields("userEnteredFormat(backgroundColor,textFormat)")));
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(2).setEndRowIndex(3).setStartColumnIndex(0).setEndColumnIndex(2))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                        .setTextFormat(new TextFormat().setBold(true))
                        .setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                .setFields("userEnteredFormat(textFormat,numberFormat)")));
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(4).setEndRowIndex(5).setStartColumnIndex(0).setEndColumnIndex(2))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                        .setBackgroundColor(formaHeaderBg)
                        .setTextFormat(new TextFormat().setBold(true))))
                .setFields("userEnteredFormat(backgroundColor,textFormat)")));
        if (totalRows > 6) {
            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(5).setEndRowIndex(totalRows - 2).setStartColumnIndex(1).setEndColumnIndex(2))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                    .setFields("userEnteredFormat(numberFormat)")));
        }
        for (int c = 0; c < 2; c++) {
            int width = c == 0 ? 180 : 120;
            requests.add(new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                    .setRange(new DimensionRange().setSheetId(sheetId).setDimension("COLUMNS").setStartIndex(c).setEndIndex(c + 1))
                    .setProperties(new DimensionProperties().setPixelSize(width))
                    .setFields("pixelSize")));
        }
        if (!requests.isEmpty()) {
            sheets.spreadsheets().batchUpdate(spreadsheetIdTarget, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();
        }
    }

    private Sheets buildSheetsClient() throws Exception {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException("Google Sheets não configurado: defina app.google.sheets.credentials-path com o caminho do JSON da Service Account.");
        }
        InputStream credentialsStream;
        if (credentialsPath.startsWith("classpath:")) {
            String resourcePath = credentialsPath.substring("classpath:".length()).replaceFirst("^/", "");
            credentialsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (credentialsStream == null) {
                throw new IllegalStateException("Arquivo de credenciais não encontrado no classpath: " + resourcePath);
            }
        } else {
            Path path = Path.of(credentialsPath);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Arquivo de credenciais não encontrado: " + credentialsPath);
            }
            credentialsStream = new FileInputStream(path.toFile());
        }
        GoogleCredentials credentials;
        try (InputStream is = credentialsStream) {
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(List.of(
                            "https://www.googleapis.com/auth/spreadsheets",
                            "https://www.googleapis.com/auth/drive.file"  // necessário para criar nova planilha (POST spreadsheets)
                    ));
        }
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Delivere Acai")
                .build();
    }

    private Drive buildDriveClient() throws Exception {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException("Google Sheets não configurado: defina app.google.sheets.credentials-path.");
        }
        InputStream credentialsStream;
        if (credentialsPath.startsWith("classpath:")) {
            String resourcePath = credentialsPath.substring("classpath:".length()).replaceFirst("^/", "");
            credentialsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (credentialsStream == null) {
                throw new IllegalStateException("Arquivo de credenciais não encontrado no classpath: " + resourcePath);
            }
        } else {
            Path path = Path.of(credentialsPath);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Arquivo de credenciais não encontrado: " + credentialsPath);
            }
            credentialsStream = new FileInputStream(path.toFile());
        }
        GoogleCredentials credentials;
        try (InputStream is = credentialsStream) {
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(List.of(
                            "https://www.googleapis.com/auth/spreadsheets",
                            DriveScopes.DRIVE_FILE
                    ));
        }
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Delivere Acai")
                .build();
    }

    /**
     * Cria uma nova planilha via Drive API (evita 403 do Sheets API create com Service Account).
     * Retorna o ID da planilha para uso com a Sheets API (update/append).
     */
    private String createSpreadsheetViaDrive(String title) throws Exception {
        Drive drive = buildDriveClient();
        File fileMetadata = new File();
        fileMetadata.setName(title);
        fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        File file = drive.files().create(fileMetadata)
                .setFields("id")
                .execute();
        return file.getId();
    }

    private String getOrCreateSpreadsheetForDay(Sheets sheets, LocalDate data) throws java.io.IOException {
        return planilhaDiaRepository.findByData(data)
                .map(PlanilhaDia::getSpreadsheetId)
                .orElseGet(() -> {
                    try {
                        String title = tituloPlanilhaDia + " " + data.format(DATE_FMT_TITLE);
                        String id = createSpreadsheetViaDrive(title);
                        planilhaDiaRepository.save(new PlanilhaDia(data, id));
                        return id;
                    } catch (Exception e) {
                        throw new RuntimeException("Erro ao criar planilha do dia: " + e.getMessage(), e);
                    }
                });
    }

    private void writeToSpreadsheet(Sheets sheets, String targetSpreadsheetId, RelatorioDTO relatorio) throws java.io.IOException {
        writeToSpreadsheet(sheets, targetSpreadsheetId, null, relatorio);
    }

    private void writeToSpreadsheet(Sheets sheets, String targetSpreadsheetId, String sheetNameForRange, RelatorioDTO relatorio) throws java.io.IOException {
        String resolvedSheetName = sheetNameForRange != null && !sheetNameForRange.isBlank()
                ? sheetNameForRange
                : resolveSheetName(sheets, targetSpreadsheetId);
        if (sheetNameForRange != null && !sheetNameForRange.isBlank()) {
            String clearRange = toA1Range(resolvedSheetName, "A:Z");
            sheets.spreadsheets().values().clear(targetSpreadsheetId, clearRange, new ClearValuesRequest()).execute();
        }
        List<List<Object>> rows = buildRows(relatorio);
        String range = toA1Range(resolvedSheetName, "A1");
        ValueRange body = new ValueRange().setValues(rows);
        sheets.spreadsheets().values()
                .update(targetSpreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
        aplicarFormatacao(sheets, targetSpreadsheetId, resolvedSheetName, relatorio, rows.size());
    }

    /**
     * Aplica formatação à planilha: cabeçalho em destaque, bordas, moeda nos valores, largura das colunas.
     */
    private void aplicarFormatacao(Sheets sheets, String spreadsheetId, String sheetName, RelatorioDTO relatorio, int totalRows) throws java.io.IOException {
        Integer sheetId = getSheetId(sheets, spreadsheetId, sheetName);
        if (sheetId == null) return;

        int comandasCount = relatorio.getComandas() != null ? relatorio.getComandas().size() : 0;
        int colCount = 9; // #, Tipo, Identificador, Total (R$), Forma, Data abe, Data fech, Abriu, Fechou

        List<Request> requests = new ArrayList<>();

        // 1) Cabeçalho da tabela de comandas (linha 0): fundo escuro, texto branco, negrito
        Color headerBg = new Color().setRed(0.27f).setGreen(0.16f).setBlue(0.29f); // tom açaí
        CellFormat headerFormat = new CellFormat()
                .setBackgroundColor(headerBg)
                .setTextFormat(new TextFormat().setBold(true).setForegroundColor(new Color().setRed(1f).setGreen(1f).setBlue(1f)).setFontSize(11));
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(0).setEndColumnIndex(colCount))
                .setCell(new CellData().setUserEnteredFormat(headerFormat))
                .setFields("userEnteredFormat(backgroundColor,textFormat)")));

        // 2) Área de dados das comandas: zebra (linhas alternadas) e bordas
        if (comandasCount > 0) {
            for (int r = 0; r < comandasCount; r++) {
                Color rowBg = (r % 2 == 0) ? new Color().setRed(1f).setGreen(1f).setBlue(1f)
                        : new Color().setRed(0.98f).setGreen(0.96f).setBlue(0.99f);
                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(1 + r).setEndRowIndex(2 + r).setStartColumnIndex(0).setEndColumnIndex(colCount))
                        .setCell(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(rowBg)))
                        .setFields("userEnteredFormat(backgroundColor)")));
            }
        }

        // 3) Coluna Total (R$) nas comandas: formato moeda
        if (comandasCount > 0) {
            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(1).setEndRowIndex(1 + comandasCount).setStartColumnIndex(3).setEndColumnIndex(4))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                    .setFields("userEnteredFormat(numberFormat)")));
        }

        // 4) Linha "Total de vendas (R$)": negrito no rótulo, formato moeda no valor
        int totalVendasRow = comandasCount + 2; // 0=header, 1..N=data, N+1=vazia, N+2=total
        if (totalVendasRow < totalRows) {
            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(totalVendasRow).setEndRowIndex(totalVendasRow + 1).setStartColumnIndex(0).setEndColumnIndex(1))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setBold(true))))
                    .setFields("userEnteredFormat(textFormat)")));
            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(totalVendasRow).setEndRowIndex(totalVendasRow + 1).setStartColumnIndex(1).setEndColumnIndex(2))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                            .setTextFormat(new TextFormat().setBold(true))
                            .setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                    .setFields("userEnteredFormat(textFormat,numberFormat)")));
        }

        // 5) Cabeçalho "Forma de pagamento" / "Total (R$)": fundo claro, negrito
        int formaHeaderRow = comandasCount + 4; // N+3=vazia, N+4=header formas
        if (formaHeaderRow < totalRows) {
            Color formaHeaderBg = new Color().setRed(0.91f).setGreen(0.84f).setBlue(0.94f);
            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(formaHeaderRow).setEndRowIndex(formaHeaderRow + 1).setStartColumnIndex(0).setEndColumnIndex(2))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                            .setBackgroundColor(formaHeaderBg)
                            .setTextFormat(new TextFormat().setBold(true))))
                    .setFields("userEnteredFormat(backgroundColor,textFormat)")));
        }

        // 6) Coluna Total (R$) na seção por forma de pagamento: formato moeda
        int formaDataStart = formaHeaderRow + 1;
        if (formaDataStart < totalRows) {
            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(formaDataStart).setEndRowIndex(totalRows).setStartColumnIndex(1).setEndColumnIndex(2))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                    .setFields("userEnteredFormat(numberFormat)")));
        }

        // 7) Largura das colunas (comandas)
        for (int c = 0; c < colCount; c++) {
            int width = switch (c) {
                case 0 -> 50;   // #
                case 1 -> 80;   // Tipo
                case 2 -> 100;  // Identificador
                case 3 -> 100;  // Total
                case 4 -> 120;  // Forma
                case 5, 6 -> 130; // Datas
                case 7, 8 -> 100; // Abriu/Fechou
                default -> 100;
            };
            requests.add(new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                    .setRange(new DimensionRange().setSheetId(sheetId).setDimension("COLUMNS").setStartIndex(c).setEndIndex(c + 1))
                    .setProperties(new DimensionProperties().setPixelSize(width))
                    .setFields("pixelSize")));
        }

        // 8) Seção Caixa: cabeçalho e formato moeda nos valores
        int caixasCount = relatorio.getCaixas() != null ? relatorio.getCaixas().size() : 0;
        if (caixasCount > 0) {
            int caixaHeaderRow = totalRows - caixasCount - 2; // linha do cabeçalho "Data", "Valor abertura"...
            if (caixaHeaderRow >= 0) {
                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(caixaHeaderRow).setEndRowIndex(caixaHeaderRow + 1).setStartColumnIndex(0).setEndColumnIndex(9))
                        .setCell(new CellData().setUserEnteredFormat(headerFormat))
                        .setFields("userEnteredFormat(backgroundColor,textFormat)")));
                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(caixaHeaderRow + 1).setEndRowIndex(totalRows).setStartColumnIndex(1).setEndColumnIndex(2))
                        .setCell(new CellData().setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                        .setFields("userEnteredFormat(numberFormat)")));
                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(caixaHeaderRow + 1).setEndRowIndex(totalRows).setStartColumnIndex(3).setEndColumnIndex(5))
                        .setCell(new CellData().setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("R$ #,##0.00"))))
                        .setFields("userEnteredFormat(numberFormat)")));
            }
        }

        if (!requests.isEmpty()) {
            sheets.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();
        }
    }

    private Integer getSheetId(Sheets sheets, String spreadsheetId, String sheetTitle) throws java.io.IOException {
        Spreadsheet spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute();
        if (spreadsheet.getSheets() == null) return null;
        for (var sheet : spreadsheet.getSheets()) {
            String title = sheet.getProperties() != null && sheet.getProperties().getTitle() != null ? sheet.getProperties().getTitle() : "";
            if (sheetTitle.equals(title)) {
                return sheet.getProperties().getSheetId();
            }
        }
        return null;
    }

    /**
     * Retorna o nome da aba a usar: se app.google.sheets.sheet-name existir na planilha, usa esse;
     * senão usa o nome da primeira aba (ex.: "Planilha1", "Sheet1"). Assim não precisa criar aba com nome fixo.
     */
    private String resolveSheetName(Sheets sheets, String spreadsheetId) throws java.io.IOException {
        Spreadsheet spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute();
        var sheetList = spreadsheet.getSheets();
        if (sheetList == null || sheetList.isEmpty()) {
            throw new IllegalStateException("A planilha não possui abas. Crie pelo menos uma aba no Google Sheets.");
        }
        String configured = sheetName != null ? sheetName.trim() : "";
        for (var sheet : sheetList) {
            String title = sheet.getProperties() != null && sheet.getProperties().getTitle() != null
                    ? sheet.getProperties().getTitle()
                    : "";
            if (configured.equalsIgnoreCase(title)) {
                return title;
            }
        }
        return sheetList.get(0).getProperties().getTitle();
    }

    private static String toA1Range(String sheetTitle, String cellRange) {
        if (sheetTitle == null) sheetTitle = "";
        // Só colocar aspas quando há espaço ou aspa (hífen etc. a API aceita sem aspas)
        boolean needsQuotes = sheetTitle.contains(" ") || sheetTitle.contains("'");
        if (needsQuotes) {
            String escaped = sheetTitle.replace("'", "''");
            return "'" + escaped + "'!" + cellRange;
        }
        return sheetTitle + "!" + cellRange;
    }

    private List<List<Object>> buildRows(RelatorioDTO relatorio) {
        List<List<Object>> rows = new ArrayList<>();

        // Cabeçalho comandas
        rows.add(List.of(
                "#", "Tipo", "Identificador", "Total (R$)", "Forma pagamento",
                "Data abertura", "Data fechamento", "Abriu", "Fechou"));

        List<Comanda> comandas = relatorio.getComandas() != null ? relatorio.getComandas() : List.of();
        for (Comanda c : comandas) {
            String tipo = c.getTipo() != null ? labelTipo(c.getTipo().name()) : "";
            String forma = c.getFormaPagamento() != null ? labelForma(c.getFormaPagamento()) : "";
            String dataAbertura = c.getDataHora() != null ? c.getDataHora().format(DATE_TIME_FMT) : "";
            String dataFechamento = c.getDataFechamento() != null ? c.getDataFechamento().format(DATE_TIME_FMT) : "";
            rows.add(List.of(
                    c.getId() != null ? c.getId() : "",
                    tipo,
                    c.getIdentificador() != null ? c.getIdentificador() : "",
                    c.getValorTotal() != null ? c.getValorTotal() : BigDecimal.ZERO,
                    forma,
                    dataAbertura,
                    dataFechamento,
                    c.getOpenedByUsername() != null ? c.getOpenedByUsername() : "",
                    c.getClosedByUsername() != null ? c.getClosedByUsername() : ""
            ));
        }

        rows.add(List.of(""));
        rows.add(List.of("Total de vendas (R$)", relatorio.getTotalVendas() != null ? relatorio.getTotalVendas() : BigDecimal.ZERO));
        rows.add(List.of(""));

        Map<FormaPagamento, BigDecimal> totalPorForma = relatorio.getTotalPorFormaPagamento();
        if (totalPorForma != null && !totalPorForma.isEmpty()) {
            rows.add(List.of("Forma de pagamento", "Total (R$)"));
            for (FormaPagamento forma : FormaPagamento.values()) {
                BigDecimal total = totalPorForma.get(forma);
                if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                    rows.add(List.of(labelForma(forma), total));
                }
            }
        }

        // Seção Caixa (aberturas e fechamentos do período)
        List<Caixa> caixas = relatorio.getCaixas() != null ? relatorio.getCaixas() : List.of();
        if (!caixas.isEmpty()) {
            rows.add(List.of(""));
            rows.add(List.of("Caixa (aberturas e fechamentos)", "", "", "", "", "", "", "", ""));
            rows.add(List.of("Data", "Valor abertura (R$)", "Data/hora abertura", "Valor fechamento (R$)", "Valor retirada (R$)", "Data/hora fechamento", "Reabertura", "Abriu", "Fechou"));
            for (Caixa cx : caixas) {
                String dataStr = cx.getData() != null ? cx.getData().format(DATE_FMT_TITLE) : "";
                String dataHoraAbertura = cx.getDataHoraAbertura() != null ? cx.getDataHoraAbertura().format(DATE_TIME_FMT) : "";
                String dataHoraFechamento = cx.getDataHoraFechamento() != null ? cx.getDataHoraFechamento().format(DATE_TIME_FMT) : "";
                Object valorAbertura = cx.getValorAbertura() != null ? cx.getValorAbertura() : BigDecimal.ZERO;
                Object valorFechamento = cx.getValorFechamento() != null ? cx.getValorFechamento() : "";
                Object valorRetirada = cx.getValorRetirada() != null ? cx.getValorRetirada() : BigDecimal.ZERO;
                String reabertura = cx.isReabertura() ? "Sim" : "";
                rows.add(List.of(
                        dataStr,
                        valorAbertura,
                        dataHoraAbertura,
                        valorFechamento,
                        valorRetirada,
                        dataHoraFechamento,
                        reabertura,
                        cx.getOpenedByUsername() != null ? cx.getOpenedByUsername() : "",
                        cx.getClosedByUsername() != null ? cx.getClosedByUsername() : ""));
            }
        }

        return rows;
    }

    private static String labelTipo(String tipo) {
        return switch (tipo != null ? tipo : "") {
            case "CLIENTE" -> "Cliente";
            case "MESA" -> "Mesa";
            case "COMANDA" -> "Comanda";
            default -> tipo;
        };
    }

    private static String labelForma(FormaPagamento forma) {
        if (forma == null) return "";
        int i = forma.ordinal();
        return i >= 0 && i < LABELS_FORMA.length ? LABELS_FORMA[i] : forma.name();
    }
}
