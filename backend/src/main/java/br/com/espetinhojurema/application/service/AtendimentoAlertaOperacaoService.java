package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ReconhecerAlertaResult;
import br.com.espetinhojurema.application.port.out.AlertasAtendimentoPersistencePort;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoAlertaOperacaoService {

    private final AlertasAtendimentoPersistencePort alertasAtendimentoPersistencePort;
    private final PedidosPersistencePort pedidosPersistencePort;
    private final ComandaCozinhaTextoService comandaCozinhaTextoService;
    private final ImpressaoConfigOperacaoService impressaoConfigOperacaoService;
    private final ImpressaoCupsService impressaoCupsService;

    public AtendimentoAlertaOperacaoService(
            AlertasAtendimentoPersistencePort alertasAtendimentoPersistencePort,
            PedidosPersistencePort pedidosPersistencePort,
            ComandaCozinhaTextoService comandaCozinhaTextoService,
            ImpressaoConfigOperacaoService impressaoConfigOperacaoService,
            ImpressaoCupsService impressaoCupsService) {
        this.alertasAtendimentoPersistencePort = alertasAtendimentoPersistencePort;
        this.pedidosPersistencePort = pedidosPersistencePort;
        this.comandaCozinhaTextoService = comandaCozinhaTextoService;
        this.impressaoConfigOperacaoService = impressaoConfigOperacaoService;
        this.impressaoCupsService = impressaoCupsService;
    }

    @Transactional
    public ReconhecerAlertaResult reconhecerAlerta(String alertaId, String loginUsuario) {
        var registro = alertasAtendimentoPersistencePort
                .buscarPorId(alertaId)
                .orElseThrow(() -> new BusinessException("Alerta não encontrado"));
        var pedido = pedidosPersistencePort
                .buscarDetalhe(registro.pedidoId())
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));

        boolean ehFechamento = registro.tipo() == TipoAlertaAtendimento.SOLICITACAO_FECHAMENTO_COMANDA;

        // Para fechamento: sem corte (imprime tudo + total). Para comanda normal: corte = itemIdMax da comanda anterior.
        Long itemIdCorte = null;
        if (!ehFechamento) {
            itemIdCorte = alertasAtendimentoPersistencePort
                    .buscarItemIdMaxDaComandaAnterior(registro.pedidoId(), registro.criadoEm())
                    .orElse(null);
        }

        String texto = comandaCozinhaTextoService.gerar(pedido, ehFechamento, itemIdCorte);
        if (!registro.pendente()) {
            boolean impressoServidor = tentarImprimirServidor(texto);
            return new ReconhecerAlertaResult(texto, true, impressoServidor);
        }
        alertasAtendimentoPersistencePort.marcarReconhecido(alertaId, loginUsuario);
        boolean impressoServidor = tentarImprimirServidor(texto);
        return new ReconhecerAlertaResult(texto, false, impressoServidor);
    }

    private boolean tentarImprimirServidor(String texto) {
        return impressaoConfigOperacaoService
                .nomeImpressoraLpOuVazio()
                .map(n -> impressaoCupsService.imprimirTextoUtf8(texto, n))
                .orElse(false);
    }
}
