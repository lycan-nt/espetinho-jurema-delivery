package br.com.espetinhojurema.infrastructure.backup;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Abre diálogo nativo de escolha de pasta no SO onde o processo JVM está rodando (não no navegador).
 */
@Component
public class OsNativeFolderPicker {

    private static final Logger log = LoggerFactory.getLogger(OsNativeFolderPicker.class);

    private final int timeoutSeconds;

    public OsNativeFolderPicker(@Value("${app.backup.desktop-folder-picker-timeout-seconds:120}") int timeoutSeconds) {
        this.timeoutSeconds = Math.max(30, timeoutSeconds);
    }

    public PickOutcome pickFolder() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                return pickWindows();
            }
            if (os.contains("mac")) {
                return pickMacOs();
            }
            return pickLinuxZenity();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrompido ao executar seletor de pasta nativo", e);
            return new PickOutcome.Failed("Operação interrompida.");
        } catch (IOException e) {
            log.warn("Falha ao executar seletor de pasta nativo", e);
            return new PickOutcome.Failed(e.getMessage());
        }
    }

    private PickOutcome pickWindows() throws IOException, InterruptedException {
        if (!GraphicsEnvironment.isHeadless()) {
            PickOutcome swing = pickFolderSwingOnWorkerThread("Selecione a pasta para salvar os backups (H2)");
            if (swing instanceof PickOutcome.Success || swing instanceof PickOutcome.Cancelled) {
                return swing;
            }
            log.info("Swing não concluiu ({}). Tentando PowerShell.", ((PickOutcome.Failed) swing).reason());
        } else {
            log.debug("Ambiente headless: usando PowerShell para escolher pasta no Windows.");
        }
        return pickWindowsPowerShell();
    }

    /**
     * Swing não pode usar {@code invokeAndWait} na thread do servlet (Tomcat) — trava a requisição sem abrir o
     * diálogo de forma confiável. Rodamos o EDT numa thread dedicada.
     */
    private PickOutcome pickFolderSwingOnWorkerThread(String title) throws InterruptedException {
        AtomicReference<PickOutcome> outcome = new AtomicReference<>(new PickOutcome.Cancelled());
        CountDownLatch done = new CountDownLatch(1);
        Thread picker = new Thread(
                () -> {
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            JFrame owner = new JFrame();
                            owner.setTitle("Espetinho Jurema — Backup");
                            owner.setAlwaysOnTop(true);
                            owner.setLocationByPlatform(true);
                            try {
                                try {
                                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                                } catch (Exception ignored) {
                                }
                                JFileChooser chooser = new JFileChooser();
                                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                                chooser.setDialogTitle(title);
                                chooser.setApproveButtonText("Selecionar pasta");
                                int r = chooser.showOpenDialog(owner);
                                if (r == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                                    String p = chooser.getSelectedFile().getAbsolutePath();
                                    if (p != null && !p.isBlank()) {
                                        outcome.set(new PickOutcome.Success(p.trim()));
                                        return;
                                    }
                                }
                                outcome.set(new PickOutcome.Cancelled());
                            } finally {
                                owner.dispose();
                            }
                        });
                    } catch (InvocationTargetException e) {
                        Throwable c = e.getCause();
                        String msg = c != null ? c.getMessage() : e.getMessage();
                        log.warn("Erro no seletor Swing", e);
                        outcome.set(new PickOutcome.Failed(
                                msg != null && !msg.isBlank() ? msg : "Erro ao abrir o seletor de pasta (Swing)."));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        outcome.set(new PickOutcome.Failed("Interrompido."));
                    } finally {
                        done.countDown();
                    }
                },
                "espetinho-backup-folder-picker");

        picker.setDaemon(false);
        log.info("Abrindo seletor de pasta (Swing em thread dedicada) …");
        picker.start();
        if (!done.await(timeoutSeconds, TimeUnit.SECONDS)) {
            log.warn("Timeout do seletor Swing ({} s)", timeoutSeconds);
            picker.interrupt();
            return new PickOutcome.Failed(
                    "Tempo limite ao abrir a janela. Confira se ela não ficou atrás de outras (Alt+Tab). "
                            + "Se a API roda só como serviço Windows sem área de trabalho, digite o caminho no campo.");
        }
        return outcome.get();
    }

    private PickOutcome pickWindowsPowerShell() throws IOException, InterruptedException {
        String script =
                """
                Add-Type -AssemblyName System.Windows.Forms
                $b = New-Object System.Windows.Forms.FolderBrowserDialog
                $b.Description = 'Pasta para salvar backups do Espetinho Jurema (H2)'
                $b.ShowNewFolderButton = $true
                $r = $b.ShowDialog()
                if ($r -eq [System.Windows.Forms.DialogResult]::OK -and $b.SelectedPath -ne '') {
                    [Console]::Out.Write($b.SelectedPath)
                    exit 0
                }
                exit 1
                """;
        Path tmp = Files.createTempFile("espetinho-folder-", ".ps1");
        try {
            Files.writeString(tmp, script, StandardCharsets.UTF_8);
            String ps = resolveWindowsPowerShellExecutable();
            ProcessBuilder pb = new ProcessBuilder(
                    ps,
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Sta",
                    "-File",
                    tmp.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            log.info("Abrindo seletor de pasta (PowerShell) …");
            return runProcess(pb, "PowerShell");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
        }
    }

    private static String resolveWindowsPowerShellExecutable() {
        String sysroot = System.getenv("SystemRoot");
        if (sysroot != null && !sysroot.isEmpty()) {
            Path ps = Path.of(sysroot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
            if (Files.isRegularFile(ps)) {
                return ps.toString();
            }
        }
        return "powershell.exe";
    }

    private PickOutcome pickMacOs() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/osascript",
                "-e",
                "POSIX path of (choose folder with prompt \"Pasta para backups do Espetinho Jurema\")");
        pb.redirectErrorStream(true);
        return runProcess(pb, "osascript");
    }

    private PickOutcome pickLinuxZenity() throws IOException, InterruptedException {
        ProcessBuilder which = new ProcessBuilder("which", "zenity");
        which.redirectErrorStream(true);
        Process w = which.start();
        boolean finished = w.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            w.destroyForcibly();
            return new PickOutcome.Failed("Timeout ao verificar zenity.");
        }
        if (w.exitValue() != 0) {
            if (!GraphicsEnvironment.isHeadless()) {
                try {
                    return pickFolderSwingOnWorkerThread("Pasta para backups Espetinho Jurema");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
            return new PickOutcome.Failed(
                    "Comando zenity não encontrado. Instale (ex.: apt install zenity) ou digite o caminho manualmente.");
        }
        ProcessBuilder pb =
                new ProcessBuilder("zenity", "--file-selection", "--directory", "--title=Pasta para backups Espetinho Jurema");
        pb.redirectErrorStream(true);
        return runProcess(pb, "zenity");
    }

    private PickOutcome runProcess(ProcessBuilder pb, String label) throws IOException, InterruptedException {
        Process p = pb.start();
        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        byte[] out = p.getInputStream().readAllBytes();
        if (!finished) {
            p.destroyForcibly();
            return new PickOutcome.Failed(
                    "Tempo esgotado ao aguardar o seletor de pasta ("
                            + label
                            + "). Se a API roda como serviço Windows sem sessão de usuário, use o campo de texto ou execute a API com usuário logado.");
        }
        int code = p.exitValue();
        String text = new String(out, StandardCharsets.UTF_8).trim();
        if (code != 0 && !text.isEmpty()) {
            log.warn("{} saiu com código {}: {}", label, code, text);
            return new PickOutcome.Failed(truncate(text, 900));
        }
        if (code != 0 || text.isEmpty()) {
            return new PickOutcome.Cancelled();
        }
        return new PickOutcome.Success(text);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }

    public sealed interface PickOutcome permits PickOutcome.Success, PickOutcome.Cancelled, PickOutcome.Failed {

        record Success(String absolutePath) implements PickOutcome {}

        record Cancelled() implements PickOutcome {}

        record Failed(String reason) implements PickOutcome {}
    }
}
