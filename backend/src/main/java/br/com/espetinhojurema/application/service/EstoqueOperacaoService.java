package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.TipoMovimentoEstoque;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.ItemPedidoEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.MovimentoEstoqueEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.PedidoEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.ProdutoEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.MovimentoEstoqueJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ProdutoJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueOperacaoService {

    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;
    private final MovimentoEstoqueJpaRepository movimentoEstoqueJpaRepository;
    private final ProdutoJpaRepository produtoJpaRepository;

    public EstoqueOperacaoService(
            ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository,
            MovimentoEstoqueJpaRepository movimentoEstoqueJpaRepository,
            ProdutoJpaRepository produtoJpaRepository) {
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
        this.movimentoEstoqueJpaRepository = movimentoEstoqueJpaRepository;
        this.produtoJpaRepository = produtoJpaRepository;
    }

    /** Consome estoque ao lançar item no pedido (saída automática). */
    @Transactional
    public void aplicarConsumoVenda(ProdutoEntity produto, int quantidade) {
        if (quantidade < 1) {
            return;
        }
        boolean obrig = isEstoqueObrigatorio();
        int saldo = produto.getSaldoEstoque() != null ? produto.getSaldoEstoque() : 0;
        if (obrig && saldo < quantidade) {
            throw new BusinessException(
                    "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: " + saldo + ".");
        }
        produto.setSaldoEstoque(saldo - quantidade);
        produtoJpaRepository.save(produto);
    }

    /** Devolve ao estoque ao cancelar pedido com itens já lançados (ignora itens já cancelados antes). */
    @Transactional
    public void restaurarItensPedido(PedidoEntity pedido) {
        for (ItemPedidoEntity item : pedido.getItens()) {
            if (item.isCancelado()) {
                continue;
            }
            restaurarQuantidadeProduto(item.getProduto(), item.getQuantidade());
        }
    }

    /** Devolve quantidade ao estoque (ex.: item cancelado no pedido aberto). */
    @Transactional
    public void restaurarQuantidadeProduto(ProdutoEntity produto, int quantidade) {
        if (quantidade < 1) {
            return;
        }
        int saldo = produto.getSaldoEstoque() != null ? produto.getSaldoEstoque() : 0;
        produto.setSaldoEstoque(saldo + quantidade);
        produtoJpaRepository.save(produto);
    }

    @Transactional
    public void registrarEntrada(Long produtoId, int quantidade, String referencia) {
        if (quantidade < 1) {
            throw new BusinessException("Quantidade de entrada inválida.");
        }
        ProdutoEntity produto =
                produtoJpaRepository.findById(produtoId).orElseThrow(() -> new BusinessException("Produto não encontrado"));
        int saldo = produto.getSaldoEstoque() != null ? produto.getSaldoEstoque() : 0;
        produto.setSaldoEstoque(saldo + quantidade);
        produtoJpaRepository.save(produto);

        var mov = new MovimentoEstoqueEntity();
        mov.setProduto(produto);
        mov.setTipo(TipoMovimentoEstoque.ENTRADA);
        mov.setDelta(quantidade);
        mov.setReferencia(referencia != null && !referencia.isBlank() ? referencia.trim() : null);
        movimentoEstoqueJpaRepository.save(mov);
    }

    @Transactional
    public void ajustarSaldo(Long produtoId, int novoSaldo) {
        ProdutoEntity produto =
                produtoJpaRepository.findById(produtoId).orElseThrow(() -> new BusinessException("Produto não encontrado"));
        int anterior = produto.getSaldoEstoque() != null ? produto.getSaldoEstoque() : 0;
        int delta = novoSaldo - anterior;
        produto.setSaldoEstoque(novoSaldo);
        produtoJpaRepository.save(produto);

        var mov = new MovimentoEstoqueEntity();
        mov.setProduto(produto);
        mov.setTipo(TipoMovimentoEstoque.AJUSTE);
        mov.setDelta(delta);
        mov.setReferencia("Ajuste manual");
        movimentoEstoqueJpaRepository.save(mov);
    }

    public boolean isEstoqueObrigatorio() {
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .map(ConfiguracaoSistemaEntity::isEstoqueObrigatorio)
                .orElse(false);
    }

    @Transactional
    public void definirEstoqueObrigatorio(boolean valor) {
        ConfiguracaoSistemaEntity cfg = configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .orElseGet(() -> {
                    var c = new ConfiguracaoSistemaEntity();
                    c.setId(ConfiguracaoSistemaEntity.ID_UNICO);
                    return c;
                });
        cfg.setEstoqueObrigatorio(valor);
        configuracaoSistemaJpaRepository.save(cfg);
    }
}
