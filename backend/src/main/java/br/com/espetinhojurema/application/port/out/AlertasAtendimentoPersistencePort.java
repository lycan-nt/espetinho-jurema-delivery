package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.AlertaAtendimentoRegistroView;
import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import java.util.Optional;

public interface AlertasAtendimentoPersistencePort {

    /** Persiste alerta pendente com tipo informado e devolve o id (UUID). */
    String criarPendente(Long pedidoId, Integer mesaNumero, TipoAlertaAtendimento tipoAlerta);

    Optional<AlertaAtendimentoRegistroView> buscarPorId(String alertaId);

    /**
     * Último alerta pendente do tipo para o pedido (ex.: repetir notificação WebSocket para o mesmo
     * solicitante sem duplicar entradas na fila visual).
     */
    Optional<String> encontrarAlertaIdPendente(Long pedidoId, TipoAlertaAtendimento tipoAlerta);

    void marcarReconhecido(String alertaId, String loginUsuario);
}
