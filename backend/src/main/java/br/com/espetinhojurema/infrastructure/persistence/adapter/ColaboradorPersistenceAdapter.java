package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.model.ColaboradorView;
import br.com.espetinhojurema.application.port.out.ColaboradorPersistencePort;
import br.com.espetinhojurema.infrastructure.persistence.repository.ColaboradorJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ColaboradorPersistenceAdapter implements ColaboradorPersistencePort {

    private final ColaboradorJpaRepository colaboradorJpaRepository;

    public ColaboradorPersistenceAdapter(ColaboradorJpaRepository colaboradorJpaRepository) {
        this.colaboradorJpaRepository = colaboradorJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColaboradorView> listarAtivos() {
        return colaboradorJpaRepository.findAllAtivos().stream()
                .map(c -> new ColaboradorView(c.getId(), c.getNome()))
                .toList();
    }
}
