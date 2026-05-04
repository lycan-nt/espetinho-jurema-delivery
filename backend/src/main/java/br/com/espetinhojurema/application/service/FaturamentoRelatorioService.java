package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.FaturamentoResumoView;
import br.com.espetinhojurema.application.model.FormaPagamentoTotalView;
import br.com.espetinhojurema.application.model.ProdutoVendaTotalView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.FormaPagamento;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.infrastructure.persistence.repository.ItemPedidoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.PagamentoPedidoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.PedidoJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaturamentoRelatorioService {

    private final PagamentoPedidoJpaRepository pagamentoPedidoJpaRepository;
    private final PedidoJpaRepository pedidoJpaRepository;
    private final ItemPedidoJpaRepository itemPedidoJpaRepository;

    public FaturamentoRelatorioService(
            PagamentoPedidoJpaRepository pagamentoPedidoJpaRepository,
            PedidoJpaRepository pedidoJpaRepository,
            ItemPedidoJpaRepository itemPedidoJpaRepository) {
        this.pagamentoPedidoJpaRepository = pagamentoPedidoJpaRepository;
        this.pedidoJpaRepository = pedidoJpaRepository;
        this.itemPedidoJpaRepository = itemPedidoJpaRepository;
    }

    @Transactional(readOnly = true)
    public FaturamentoResumoView resumo(Instant inicioInclusive, Instant fimExclusive) {
        if (!fimExclusive.isAfter(inicioInclusive)) {
            throw new BusinessException("Período inválido: fim deve ser após início.");
        }
        var linhas = pagamentoPedidoJpaRepository.totaisPorFormaNoPeriodo(inicioInclusive, fimExclusive);
        List<FormaPagamentoTotalView> porForma = new ArrayList<>();
        BigDecimal receita = BigDecimal.ZERO;
        for (Object[] row : linhas) {
            FormaPagamento forma = (FormaPagamento) row[0];
            BigDecimal t = row[1] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            t = t.setScale(2, java.math.RoundingMode.HALF_UP);
            porForma.add(new FormaPagamentoTotalView(forma, t));
            receita = receita.add(t);
        }
        receita = receita.setScale(2, java.math.RoundingMode.HALF_UP);
        long pedidosEncerrados = pedidoJpaRepository.countEncerradosPagosNoPeriodo(PedidoStatus.PAGO, inicioInclusive, fimExclusive);
        List<ProdutoVendaTotalView> porProduto = new ArrayList<>();
        for (Object[] row : itemPedidoJpaRepository.totaisPorProdutoPedidosPagosNoPeriodo(
                PedidoStatus.PAGO, inicioInclusive, fimExclusive)) {
            long produtoId = row[0] instanceof Number n ? n.longValue() : 0L;
            String nome = row[1] != null ? row[1].toString() : "";
            BigDecimal t = row[2] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            t = t.setScale(2, java.math.RoundingMode.HALF_UP);
            porProduto.add(new ProdutoVendaTotalView(produtoId, nome, t));
        }
        return new FaturamentoResumoView(receita, porForma, pedidosEncerrados, porProduto);
    }
}
