package br.com.delivere.acai.loja;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LojaService {

    private final LojaRepository repository;

    public LojaService(LojaRepository repository) {
        this.repository = repository;
    }

    public List<Loja> listarTodas() {
        return repository.findAllByOrderByIdAsc();
    }

    public Loja buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loja não encontrada: " + id));
    }

    @Transactional
    public Loja criar(Loja loja) {
        if (repository.existsById(loja.getId())) {
            throw new IllegalArgumentException("Já existe uma loja com o ID informado: " + loja.getId());
        }
        return repository.save(loja);
    }

    @Transactional
    public Loja atualizar(String id, Loja lojaAtualizada) {
        Loja existente = buscarPorId(id);
        existente.setNome(lojaAtualizada.getNome());
        existente.setEndereco(lojaAtualizada.getEndereco());
        existente.setResponsavel(lojaAtualizada.getResponsavel());
        return repository.save(existente);
    }

    @Transactional
    public void excluir(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Loja não encontrada: " + id);
        }
        repository.deleteById(id);
    }
}
