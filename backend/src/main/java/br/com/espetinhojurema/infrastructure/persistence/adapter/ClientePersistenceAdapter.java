package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.model.ClienteView;
import br.com.espetinhojurema.application.port.out.ClientePersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.persistence.entity.ClienteEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ClienteJpaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClientePersistenceAdapter implements ClientePersistencePort {

    private final ClienteJpaRepository clienteJpaRepository;

    public ClientePersistenceAdapter(ClienteJpaRepository clienteJpaRepository) {
        this.clienteJpaRepository = clienteJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteView> listar() {
        return clienteJpaRepository.findAll(Sort.by("nome")).stream()
                .map(c -> new ClienteView(c.getId(), c.getNome(), c.getTelefone(), c.getEndereco()))
                .toList();
    }

    @Override
    @Transactional
    public ClienteView criar(String nome, String telefone, String endereco) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome do cliente é obrigatório");
        }
        var e = new ClienteEntity();
        e.setNome(nome.strip());
        e.setTelefone(telefone);
        e.setEndereco(endereco);
        ClienteEntity salvo = clienteJpaRepository.save(e);
        return new ClienteView(salvo.getId(), salvo.getNome(), salvo.getTelefone(), salvo.getEndereco());
    }
}
