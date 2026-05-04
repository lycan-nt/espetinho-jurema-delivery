package br.com.espetinhojurema.application.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Impressão local no mesmo host do backend.
 *
 * <p>Windows: envia bytes em modo <b>RAW</b> via API {@code WritePrinter} do spool (WinSpool.drv),
 * evitando que o GDI do Windows escalone o texto e produza fonte pequenininha.
 * Fallback: {@code Out-Printer} e, por último, Java Print Service.
 *
 * <p>macOS/Linux: CUPS ({@code lp} / {@code lpstat}).
 */
@Service
public class ImpressaoCupsService {

    private static final Logger log = LoggerFactory.getLogger(ImpressaoCupsService.class);

    private static final Pattern FILA_VALIDA = Pattern.compile("^[\\p{L}\\p{N}\\s._\\-]+$");

    private static final DocFlavor BYTE_TEXT_UTF8  = new DocFlavor.BYTE_ARRAY("text/plain; charset=UTF-8");
    private static final DocFlavor STREAM_TEXT_UTF8 = new DocFlavor.INPUT_STREAM("text/plain; charset=UTF-8");

    /**
     * PowerShell script que chama WritePrinter com datatype=RAW.
     * Bytes chegam direto ao firmware da impressora — sem GDI, sem escalonamento de fonte.
     */
    private static final String PS_RAW_PRINT = String.join("\n",
        "$prn=$env:EJ_PRINT_PRN",
        "$dat=$env:EJ_PRINT_TMP",
        "$bytes=[System.IO.File]::ReadAllBytes($dat)",
        "Add-Type -TypeDefinition @'",
        "using System;",
        "using System.Runtime.InteropServices;",
        "public class WinSpool {",
        "  [DllImport(\"winspool.drv\",SetLastError=true,CharSet=CharSet.Auto)]",
        "  public static extern bool OpenPrinter(string n,out IntPtr h,IntPtr d);",
        "  [DllImport(\"winspool.drv\",SetLastError=true)]",
        "  public static extern bool ClosePrinter(IntPtr h);",
        "  [DllImport(\"winspool.drv\",SetLastError=true,CharSet=CharSet.Auto)]",
        "  public static extern int StartDocPrinter(IntPtr h,int lv,IntPtr di);",
        "  [DllImport(\"winspool.drv\",SetLastError=true)]",
        "  public static extern bool EndDocPrinter(IntPtr h);",
        "  [DllImport(\"winspool.drv\",SetLastError=true)]",
        "  public static extern bool StartPagePrinter(IntPtr h);",
        "  [DllImport(\"winspool.drv\",SetLastError=true)]",
        "  public static extern bool EndPagePrinter(IntPtr h);",
        "  [DllImport(\"winspool.drv\",SetLastError=true)]",
        "  public static extern bool WritePrinter(IntPtr h,IntPtr p,int n,out int w);",
        "}",
        "'@",
        "$h=[IntPtr]::Zero",
        "if(-not [WinSpool]::OpenPrinter($prn,[ref]$h,[IntPtr]::Zero)){Write-Error 'OpenPrinter falhou';exit 1}",
        "try{",
        "  $pn=[System.Runtime.InteropServices.Marshal]::StringToHGlobalAuto('EJ Comanda')",
        "  $pt=[System.Runtime.InteropServices.Marshal]::StringToHGlobalAuto('RAW')",
        "  $sz=3*[IntPtr]::Size",
        "  $di=[System.Runtime.InteropServices.Marshal]::AllocHGlobal($sz)",
        "  [System.Runtime.InteropServices.Marshal]::WriteIntPtr($di,0,$pn)",
        "  [System.Runtime.InteropServices.Marshal]::WriteIntPtr($di,[IntPtr]::Size,[IntPtr]::Zero)",
        "  [System.Runtime.InteropServices.Marshal]::WriteIntPtr($di,2*[IntPtr]::Size,$pt)",
        "  $job=[WinSpool]::StartDocPrinter($h,1,$di)",
        "  [System.Runtime.InteropServices.Marshal]::FreeHGlobal($di)",
        "  [System.Runtime.InteropServices.Marshal]::FreeHGlobal($pn)",
        "  [System.Runtime.InteropServices.Marshal]::FreeHGlobal($pt)",
        "  if($job -eq 0){Write-Error 'StartDocPrinter falhou';exit 1}",
        "  [WinSpool]::StartPagePrinter($h)|Out-Null",
        "  $pb=[System.Runtime.InteropServices.Marshal]::AllocHGlobal($bytes.Length)",
        "  [System.Runtime.InteropServices.Marshal]::Copy($bytes,0,$pb,$bytes.Length)",
        "  $w=0",
        "  [WinSpool]::WritePrinter($h,$pb,$bytes.Length,[ref]$w)|Out-Null",
        "  [System.Runtime.InteropServices.Marshal]::FreeHGlobal($pb)",
        "  [WinSpool]::EndPagePrinter($h)|Out-Null",
        "  [WinSpool]::EndDocPrinter($h)|Out-Null",
        "  Write-Output \"RAW OK: $w bytes\"",
        "}finally{[WinSpool]::ClosePrinter($h)|Out-Null}"
    );

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /** Lista impressoras locais (Windows: spooler; Unix: lpstat). */
    public List<String> listarFilasImpressoras() {
        if (osWindows()) {
            try {
                PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                return Arrays.stream(services)
                        .map(PrintService::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
            } catch (Exception e) {
                log.warn("Não foi possível listar impressoras (Java Print Service): {}", e.getMessage());
                return List.of();
            }
        }
        Set<String> nomes = new LinkedHashSet<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("lpstat", "-p");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            proc.waitFor();
            for (String line : out.split("\r?\n")) {
                if (line.startsWith("printer ")) {
                    int end = line.indexOf(" is ");
                    if (end > 8) nomes.add(line.substring(8, end).trim());
                }
            }
        } catch (Exception e) {
            log.warn("lpstat -p não disponível: {}", e.getMessage());
        }
        return nomes.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    /** Envia texto UTF-8 para a impressora indicada. */
    public boolean imprimirTextoUtf8(String texto, String nomeFila) {
        if (!nomeFilaValido(nomeFila)) {
            log.warn("Nome de impressora inválido ou vazio.");
            return false;
        }
        String nome = nomeFila.trim();
        if (osWindows()) {
            return imprimirWindows(texto, nome);
        }
        return imprimirUnix(texto, nome);
    }

    public boolean nomeFilaValido(String nome) {
        if (nome == null || nome.isBlank()) return false;
        String t = nome.trim();
        if (t.length() > 200) return false;
        return FILA_VALIDA.matcher(t).matches();
    }

    // -------------------------------------------------------------------------
    // Windows
    // -------------------------------------------------------------------------

    private boolean imprimirWindows(String texto, String nomeDesejado) {
        PrintService ps = encontrarServicoImpressora(nomeDesejado);
        if (ps == null) {
            log.warn("Impressora \"{}\" não encontrada no Windows.", nomeDesejado);
            return false;
        }
        String nomeReal = ps.getName();
        String conteudo = texto != null ? texto : "";

        // 1. RAW via WritePrinter (preserva fonte padrão da impressora, sem escalonamento GDI)
        if (rawPrint(conteudo, nomeReal)) return true;

        // 2. Out-Printer (GDI — pode sair fonte menor dependendo do papel configurado)
        log.warn("RAW falhou para \"{}\"; tentando Out-Printer.", nomeReal);
        if (outPrinter(conteudo, nomeReal)) return true;

        // 3. Java Print Service (vários flavors)
        log.warn("Out-Printer falhou para \"{}\"; tentando Java Print Service.", nomeReal);
        return tentarJavaPrint(ps, conteudo);
    }

    /** Envia bytes em modo RAW via WinSpool WritePrinter — fonte do firmware, não do GDI. */
    private boolean rawPrint(String texto, String nomeImpressora) {
        Path tmpDat = null;
        Path tmpPs  = null;
        try {
            // Normaliza para ASCII puro: remove acentos/diacríticos para evitar "prefer|ncia" etc.
            byte[] bytes = normalizarTextoTermica(texto).getBytes(StandardCharsets.US_ASCII);
            tmpDat = Files.createTempFile("ej-dat-", ".bin");
            Files.write(tmpDat, bytes);

            tmpPs = Files.createTempFile("ej-ps-", ".ps1");
            Files.writeString(tmpPs, PS_RAW_PRINT, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-File", tmpPs.toAbsolutePath().toString());
            pb.environment().put("EJ_PRINT_TMP", tmpDat.toAbsolutePath().toString());
            pb.environment().put("EJ_PRINT_PRN", nomeImpressora);
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            String saida = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int code = proc.waitFor();
            if (code != 0) {
                log.warn("RAW WritePrinter falhou (código {}): {}", code, saida);
                return false;
            }
            log.info("Impressão RAW enviada para \"{}\": {}", nomeImpressora, saida);
            return true;
        } catch (Exception e) {
            log.warn("RAW WritePrinter exceção para \"{}\"", nomeImpressora, e);
            return false;
        } finally {
            deletar(tmpDat);
            deletar(tmpPs);
        }
    }

    private boolean outPrinter(String texto, String nomeImpressora) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("ej-print-", ".txt");
            Files.writeString(tmp, normalizarTextoTermica(texto), StandardCharsets.US_ASCII);
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-Command",
                    "& { $ErrorActionPreference='Stop'; "
                    + "Get-Content -LiteralPath $env:EJ_PRINT_TMP -Raw -Encoding UTF8 "
                    + "| Out-Printer -Name $env:EJ_PRINT_PRN }");
            pb.environment().put("EJ_PRINT_TMP", tmp.toAbsolutePath().toString());
            pb.environment().put("EJ_PRINT_PRN", nomeImpressora);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String saida = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int code = proc.waitFor();
            if (code != 0) {
                log.warn("Out-Printer falhou (código {}): {}", code, saida);
                return false;
            }
            log.info("Out-Printer enviado para \"{}\"", nomeImpressora);
            return true;
        } catch (Exception e) {
            log.warn("Out-Printer exceção para \"{}\"", nomeImpressora, e);
            return false;
        } finally {
            deletar(tmp);
        }
    }

    private boolean tentarJavaPrint(PrintService ps, String conteudo) {
        byte[] utf8 = conteudo.getBytes(StandardCharsets.UTF_8);
        if (javaPrint(ps, new SimpleDoc(utf8, BYTE_TEXT_UTF8, null))) return true;
        try {
            if (javaPrint(ps, new SimpleDoc(new ByteArrayInputStream(utf8), STREAM_TEXT_UTF8, null))) return true;
        } catch (Exception e) {
            log.debug("INPUT_STREAM falhou: {}", e.getMessage());
        }
        if (javaPrint(ps, new SimpleDoc(conteudo, DocFlavor.STRING.TEXT_PLAIN, null))) return true;
        return javaPrint(ps, new SimpleDoc(utf8, DocFlavor.BYTE_ARRAY.AUTOSENSE, null));
    }

    private boolean javaPrint(PrintService ps, SimpleDoc doc) {
        DocFlavor f = doc.getDocFlavor();
        try {
            DocPrintJob job = ps.createPrintJob();
            job.print(doc, null);
            log.info("Java Print ({}) para \"{}\"", f.getMimeType(), ps.getName());
            return true;
        } catch (PrintException | RuntimeException e) {
            log.debug("Java Print falhou ({}): {}", f.getMimeType(), e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Unix/macOS
    // -------------------------------------------------------------------------

    private boolean imprimirUnix(String texto, String nome) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("ej-print-", ".txt");
            Files.writeString(tmp, texto != null ? texto : "", StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder("lp", "-d", nome, tmp.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String saida = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = proc.waitFor();
            if (code != 0) { log.warn("lp falhou (código {}): {}", code, saida.trim()); return false; }
            return true;
        } catch (Exception e) {
            log.warn("Erro ao executar lp", e);
            return false;
        } finally {
            deletar(tmp);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------

    private PrintService encontrarServicoImpressora(String nomeDesejado) {
        PrintService[] todos = PrintServiceLookup.lookupPrintServices(null, null);
        String want = nomeDesejado.trim();
        for (PrintService ps : todos) if (ps.getName().equalsIgnoreCase(want)) return ps;
        String ww = want.toLowerCase(Locale.ROOT);
        for (PrintService ps : todos) if (ps.getName().toLowerCase(Locale.ROOT).contains(ww)) return ps;
        return null;
    }

    private static boolean osWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void deletar(Path p) {
        if (p == null) return;
        try { Files.deleteIfExists(p); } catch (Exception ignored) { /* ignorado */ }
    }

    /**
     * Remove acentos e diacríticos do texto para evitar problemas de code page na térmica.
     * Ex.: "preferência" → "preferencia", "Débito" → "Debito", "não" → "nao".
     */
    static String normalizarTextoTermica(String texto) {
        if (texto == null) return "";
        String nfd = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }
}
