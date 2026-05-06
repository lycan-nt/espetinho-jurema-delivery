package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.AlertaAtendimentoRegistroView;
import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import java.time.Instant;
import java.util.Optional;

public interface AlertasAtendimentoPersistencePort {

    /**
     * Persiste alerta pendente e devolve o id (UUID).
     *
     * @param itemIdMax snapshot do maior item_id do pedido neste momento; {@code null} quando não aplicável.
     */
    String criarPendente(Long pedidoId, Integer mesaNumero, TipoAlertaAtendimento tipoAlerta, Long itemIdMax);

    Optional<AlertaAtendimentoRegistroView> buscarPorId(String alertaId);

    /**
     * Último alerta pendente do tipo para o pedido (ex.: repetir notificação WebSocket para o mesmo
     * solicitante sem duplicar entradas na fila visual).
     */
    Optional<String> encontrarAlertaIdPendente(Long pedidoId, TipoAlertaAtendimento tipoAlerta);

    void marcarReconhecido(String alertaId, String loginUsuario);

    /**
     * Retorna o {@code itemIdMax} do alerta COMANDA_ENVIADA mais recente do pedido criado
     * <em>antes</em> de {@code anteriorA}. Usado para determinar quais itens são novos na próxima comanda.
     */
    Optional<Long> buscarItemIdMaxDaComandaAnterior(Long pedidoId, Instant anteriorA);
}
