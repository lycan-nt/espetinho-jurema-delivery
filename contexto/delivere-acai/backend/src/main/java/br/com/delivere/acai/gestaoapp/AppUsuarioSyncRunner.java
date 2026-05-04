package br.com.delivere.acai.gestaoapp;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Na subida do backend, sincroniza usuários para o MongoDB para o app poder fazer login via Data API.
 */
@Component
@ConditionalOnBean(AppUsuarioSyncService.class)
public class AppUsuarioSyncRunner implements ApplicationRunner {

    private final AppUsuarioSyncService syncService;

    public AppUsuarioSyncRunner(AppUsuarioSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncService.sync();
    }
}
