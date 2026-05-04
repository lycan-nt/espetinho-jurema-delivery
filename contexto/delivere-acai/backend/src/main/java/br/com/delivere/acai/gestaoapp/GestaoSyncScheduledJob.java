package br.com.delivere.acai.gestaoapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sincroniza os dados de gestão para o MongoDB Atlas periodicamente.
 * A sincronização no fechamento de comanda foi removida para não bloquear a venda com rede lenta;
 * o app reflete as vendas no intervalo configurado (padrão 5 min).
 */
@Component
@ConditionalOnBean(GestaoSyncService.class)
public class GestaoSyncScheduledJob {

    private final GestaoSyncService gestaoSyncService;

    public GestaoSyncScheduledJob(GestaoSyncService gestaoSyncService) {
        this.gestaoSyncService = gestaoSyncService;
    }

    /** A cada 5 minutos atualiza o resumo de gestão no MongoDB. */
    @Scheduled(fixedRateString = "${app.mongodb.sync-interval-ms:300000}")
    public void sync() {
        gestaoSyncService.sync();
    }
}
