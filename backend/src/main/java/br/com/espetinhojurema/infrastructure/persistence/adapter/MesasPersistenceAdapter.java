package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.port.out.MesasPersistencePort;
import br.com.espetinhojurema.domain.model.Mesa;
import br.com.espetinhojurema.domain.model.MesaStatus;
import br.com.espetinhojurema.infrastructure.persistence.entity.MesaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.MesaJpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class MesasPersistenceAdapter implements MesasPersistencePort {

    private final MesaJpaRepository repository;

    public MesasPersistenceAdapter(MesaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Mesa> listarTodasOrdenadas() {
        return repository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getNumero(), b.getNumero()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Mesa> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Mesa salvar(Mesa mesa) {
        MesaEntity entity = repository.findById(mesa.id())
                .orElseThrow();
        entity.setNumero(mesa.numero());
        entity.setStatus(mesa.status());
        return toDomain(repository.save(entity));
    }

    @Override
    public Mesa atualizarStatus(Long mesaId, MesaStatus novoStatus) {
        MesaEntity entity = repository.findById(mesaId)
                .orElseThrow();
        entity.setStatus(novoStatus);
        return toDomain(repository.save(entity));
    }

    private Mesa toDomain(MesaEntity e) {
        return new Mesa(e.getId(), e.getNumero(), e.getStatus());
    }
}
