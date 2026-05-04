package br.com.delivere.acai.caixa;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CaixaScheduledJob {

    private final CaixaService caixaService;

    public CaixaScheduledJob(CaixaService caixaService) {
        this.caixaService = caixaService;
    }

    /**
     * A cada X minutos (app.caixa.intervalo-envio-planilha-ms, padrão 5 min), envia o relatório do dia
     * para a planilha do caixa aberto, se houver.
     */
    @Scheduled(fixedRateString = "${app.caixa.intervalo-envio-planilha-ms:300000}")
    public void enviarRelatorioDiarioParaPlanilha() {
        caixaService.enviarRelatorioDiarioParaPlanilha();
    }
}
