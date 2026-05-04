package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.domain.model.Mesa;
import br.com.espetinhojurema.domain.model.MesaStatus;
import java.util.List;
import java.util.Optional;

public interface MesasPersistencePort {

    List<Mesa> listarTodasOrdenadas();

    Optional<Mesa> buscarPorId(Long id);

    Mesa salvar(Mesa mesa);

    Mesa atualizarStatus(Long mesaId, MesaStatus novoStatus);
}
