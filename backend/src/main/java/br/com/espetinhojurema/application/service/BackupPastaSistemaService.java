package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.BackupFolderPickView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.backup.OsNativeFolderPicker;
import br.com.espetinhojurema.infrastructure.backup.OsNativeFolderPicker.PickOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BackupPastaSistemaService {

    private final OsNativeFolderPicker osNativeFolderPicker;
    private final boolean pickerEnabled;

    public BackupPastaSistemaService(
            OsNativeFolderPicker osNativeFolderPicker,
            @Value("${app.backup.desktop-folder-picker-enabled:true}") boolean pickerEnabled) {
        this.osNativeFolderPicker = osNativeFolderPicker;
        this.pickerEnabled = pickerEnabled;
    }

    public BackupFolderPickView selecionarPastaLocal() {
        if (!pickerEnabled) {
            throw new BusinessException(
                    "Seletor de pasta está desativado (app.backup.desktop-folder-picker-enabled=false). Digite o caminho manualmente.");
        }
        PickOutcome outcome = osNativeFolderPicker.pickFolder();
        if (outcome instanceof PickOutcome.Success ok) {
            return new BackupFolderPickView(ok.absolutePath(), false);
        }
        if (outcome instanceof PickOutcome.Cancelled) {
            return new BackupFolderPickView(null, true);
        }
        if (outcome instanceof PickOutcome.Failed fail) {
            throw new BusinessException(
                    fail.reason() == null || fail.reason().isBlank()
                            ? "Não foi possível abrir o seletor de pasta neste ambiente."
                            : fail.reason());
        }
        throw new BusinessException("Resposta inesperada do seletor de pasta.");
    }
}
