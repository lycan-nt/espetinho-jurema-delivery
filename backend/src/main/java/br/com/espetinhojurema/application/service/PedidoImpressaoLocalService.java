package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ImprimirLocalResult;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class PedidoImpressaoLocalService {

    private final PedidosPersistencePort pedidosPersistencePort;
    private final ComprovanteTextoService comprovanteTextoService;
    private final ComandaCozinhaTextoService comandaCozinhaTextoService;
    private final ImpressaoConfigOperacaoService impressaoConfigOperacaoService;
    private final ImpressaoCupsService impressaoCupsService;

    public PedidoImpressaoLocalService(
            PedidosPersistencePort pedidosPersistencePort,
            ComprovanteTextoService comprovanteTextoService,
            ComandaCozinhaTextoService comandaCozinhaTextoService,
            ImpressaoConfigOperacaoService impressaoConfigOperacaoService,
            ImpressaoCupsService impressaoCupsService) {
        this.pedidosPersistencePort = pedidosPersistencePort;
        this.comprovanteTextoService = comprovanteTextoService;
        this.comandaCozinhaTextoService = comandaCozinhaTextoService;
        this.impressaoConfigOperacaoService = impressaoConfigOperacaoService;
        this.impressaoCupsService = impressaoCupsService;
    }

    /** Envia cupom/fechamento para a térmica via {@code lp}, se configurado. */
    public ImprimirLocalResult imprimirComprovanteLocal(Long pedidoId, boolean fiscal, boolean fechamento) {
        var pedido = pedidosPersistencePort
                .buscarDetalhe(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        String texto =
                fechamento ? comprovanteTextoService.gerarFechamento(pedido, fiscal) : comprovanteTextoService.gerar(pedido, fiscal);
        var nomeOpt = impressaoConfigOperacaoService.nomeImpressoraLpOuVazio();
        if (nomeOpt.isEmpty()) {
            return new ImprimirLocalResult(false);
        }
        boolean ok = impressaoCupsService.imprimirTextoUtf8(texto, nomeOpt.get());
        return new ImprimirLocalResult(ok);
    }

    /**
     * Texto da comanda de cozinha com todos os itens ativos (sem corte incremental) — reimpressão manual.
     */
    public String textoComandaCozinhaCompleta(Long pedidoId) {
        var pedido = pedidosPersistencePort
                .buscarDetalhe(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        return comandaCozinhaTextoService.gerar(pedido, false, null);
    }

    /** Envia comanda completa para a térmica via {@code lp}, se configurado. */
    public ImprimirLocalResult imprimirComandaCozinhaLocal(Long pedidoId) {
        String texto = textoComandaCozinhaCompleta(pedidoId);
        var nomeOpt = impressaoConfigOperacaoService.nomeImpressoraLpOuVazio();
        if (nomeOpt.isEmpty()) {
            return new ImprimirLocalResult(false);
        }
        boolean ok = impressaoCupsService.imprimirTextoUtf8(texto, nomeOpt.get());
        return new ImprimirLocalResult(ok);
    }
}
