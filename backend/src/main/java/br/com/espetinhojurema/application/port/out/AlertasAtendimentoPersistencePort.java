package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.AlertaAtendimentoRegistroView;
import java.util.Optional;

public interface AlertasAtendimentoPersistencePort {

    /** Persiste alerta pendente e devolve o id (UUID). */
    String criarPendente(Long pedidoId, Integer mesaNumero);

    Optional<AlertaAtendimentoRegistroView> buscarPorId(String alertaId);

    void marcarReconhecido(String alertaId, String loginUsuario);
}
