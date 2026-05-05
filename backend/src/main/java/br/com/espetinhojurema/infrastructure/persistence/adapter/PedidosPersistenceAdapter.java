package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.command.CriarPedidoCommand;
import br.com.espetinhojurema.application.model.ItemPedidoView;
import br.com.espetinhojurema.application.model.PagamentoPedidoView;
import br.com.espetinhojurema.application.model.MesaTransferenciaView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.model.PedidoListaView;
import br.com.espetinhojurema.application.port.out.PedidoEventPublisherPort;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.FormaPagamento;
import br.com.espetinhojurema.domain.model.MesaStatus;
import br.com.espetinhojurema.domain.model.PontoCarne;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import br.com.espetinhojurema.infrastructure.persistence.entity.ItemPedidoEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.PagamentoPedidoEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.MesaEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.MesaTransferenciaEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.PedidoEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.ProdutoEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ClienteJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ColaboradorJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.MesaJpaRepository;
import br.com.espetinhojurema.application.service.EstoqueOperacaoService;
import br.com.espetinhojurema.infrastructure.persistence.repository.ItemPedidoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.MesaTransferenciaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.PedidoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ProdutoJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PedidosPersistenceAdapter implements PedidosPersistencePort {

    /** Tolerância para considerar conta quitada (centavos). */
    private static final BigDecimal EPS = new BigDecimal("0.02");

    /** Igual ao seed / cadastro inicial; define produtos nos quais o ponto da carne é obrigatório. */
    private static final String NOME_CATEGORIA_ESPETINHOS = "Espetinhos";

    private static final EnumSet<PedidoStatus> STATUS_PEDIDO_ATIVO_MESA = EnumSet.of(
            PedidoStatus.RASCUNHO,
            PedidoStatus.ABERTO,
            PedidoStatus.EM_PREPARO,
            PedidoStatus.PRONTO);

    private final PedidoJpaRepository pedidoJpaRepository;
    private final MesaJpaRepository mesaJpaRepository;
    private final ColaboradorJpaRepository colaboradorJpaRepository;
    private final ClienteJpaRepository clienteJpaRepository;
    private final ProdutoJpaRepository produtoJpaRepository;
    private final EstoqueOperacaoService estoqueOperacaoService;
    private final PedidoEventPublisherPort pedidoEventPublisherPort;
    private final MesaTransferenciaJpaRepository mesaTransferenciaJpaRepository;
    private final ItemPedidoJpaRepository itemPedidoJpaRepository;

    public PedidosPersistenceAdapter(
            PedidoJpaRepository pedidoJpaRepository,
            MesaJpaRepository mesaJpaRepository,
            ColaboradorJpaRepository colaboradorJpaRepository,
            ClienteJpaRepository clienteJpaRepository,
            ProdutoJpaRepository produtoJpaRepository,
            EstoqueOperacaoService estoqueOperacaoService,
            PedidoEventPublisherPort pedidoEventPublisherPort,
            MesaTransferenciaJpaRepository mesaTransferenciaJpaRepository,
            ItemPedidoJpaRepository itemPedidoJpaRepository) {
        this.pedidoJpaRepository = pedidoJpaRepository;
        this.mesaJpaRepository = mesaJpaRepository;
        this.colaboradorJpaRepository = colaboradorJpaRepository;
        this.clienteJpaRepository = clienteJpaRepository;
        this.produtoJpaRepository = produtoJpaRepository;
        this.estoqueOperacaoService = estoqueOperacaoService;
        this.pedidoEventPublisherPort = pedidoEventPublisherPort;
        this.mesaTransferenciaJpaRepository = mesaTransferenciaJpaRepository;
        this.itemPedidoJpaRepository = itemPedidoJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PedidoDetalheView> buscarDetalhe(Long id) {
        return pedidoJpaRepository.findDetalhePorId(id).map(this::mapearDetalhe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoListaView> filtrar(PedidoStatus status, PedidoTipo tipo) {
        return pedidoJpaRepository.filtrar(status, tipo).stream()
                .map(this::mapearLista)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> obterIdPedidoAbertoNaMesa(Long mesaId) {
        return pedidoJpaRepository
                .findTopByMesa_IdAndStatusInOrderByCriadoEmDesc(mesaId, STATUS_PEDIDO_ATIVO_MESA)
                .map(PedidoEntity::getId);
    }

    @Override
    @Transactional
    public PedidoDetalheView criarPedido(CriarPedidoCommand command) {
        if (command.mesaId() != null) {
            obterIdPedidoAbertoNaMesa(command.mesaId()).ifPresent(id -> {
                throw new BusinessException("A mesa já possui pedido aberto: " + id);
            });
        }

        var pedido = new PedidoEntity();
        pedido.setTipo(command.tipo());
        pedido.setStatus(PedidoStatus.ABERTO);
        pedido.setDescricaoMesa(command.descricaoMesa());
        pedido.setPessoas(command.pessoas());
        pedido.setDocumentoFiscal(command.documentoFiscal());
        pedido.setCriadoEm(Instant.now());
        pedido.setAtualizadoEm(Instant.now());

        if (command.mesaId() != null) {
            MesaEntity mesa = mesaJpaRepository.findById(command.mesaId())
                    .orElseThrow(() -> new BusinessException("Mesa não encontrada"));
            pedido.setMesa(mesa);
            mesa.setStatus(MesaStatus.OCUPADA);
            mesaJpaRepository.save(mesa);
        }

        var colab = colaboradorJpaRepository.findById(command.colaboradorId())
                .orElseThrow(() -> new BusinessException("Colaborador não encontrado"));
        pedido.setColaborador(colab);

        if (command.clienteId() != null) {
            pedido.setCliente(clienteJpaRepository.findById(command.clienteId())
                    .orElseThrow(() -> new BusinessException("Cliente não encontrado")));
        } else if (command.clienteNome() != null && !command.clienteNome().isBlank()) {
            pedido.setNomeClienteLivre(command.clienteNome().trim());
        }

        PedidoEntity salvo = pedidoJpaRepository.save(pedido);
        PedidoDetalheView view = pedidoJpaRepository.findDetalhePorId(salvo.getId())
                .map(this::mapearDetalhe)
                .orElseThrow();
        pedidoEventPublisherPort.notificarMudancaPedido(view.id());
        return view;
    }

    @Override
    @Transactional
    public PedidoDetalheView adicionarItem(
            Long pedidoId, Long produtoId, int quantidade, String observacao, PontoCarne pontoCarne) {
        if (quantidade < 1) {
            throw new BusinessException("Quantidade inválida");
        }
        PedidoEntity pedido = pedidoJpaRepository.findById(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        if (pedido.getStatus() == PedidoStatus.PAGO || pedido.getStatus() == PedidoStatus.CANCELADO) {
            throw new BusinessException("Pedido encerrado; não é possível adicionar itens");
        }

        ProdutoEntity produto = produtoJpaRepository.findById(produtoId)
                .orElseThrow(() -> new BusinessException("Produto não encontrado"));
        if (!produto.isAtivo()) {
            throw new BusinessException("Produto inativo");
        }

        boolean espetinho = produtoEhEspetinho(produto);
        PontoCarne ponto = null;
        if (espetinho) {
            if (pontoCarne == null) {
                throw new BusinessException("Escolha o ponto da carne.");
            }
            ponto = pontoCarne;
        }

        estoqueOperacaoService.aplicarConsumoVenda(produto, quantidade);

        var item = new ItemPedidoEntity();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(produto.getPreco());
        item.setObservacao(observacao);
        item.setPontoCarne(ponto);
        pedido.addItem(item);
        pedido.setAtualizadoEm(Instant.now());

        if (pedido.getStatus() == PedidoStatus.RASCUNHO) {
            pedido.setStatus(PedidoStatus.ABERTO);
        }

        pedidoJpaRepository.save(pedido);
        PedidoDetalheView detalhe = pedidoJpaRepository.findDetalhePorId(pedidoId)
                .map(this::mapearDetalhe)
                .orElseThrow();
        pedidoEventPublisherPort.notificarMudancaPedido(pedidoId);
        return detalhe;
    }

    @Override
    @Transactional
    public PedidoDetalheView cancelarItemPedido(
            Long pedidoId, Long itemPedidoId, String usuarioLogin, Integer quantidadeCancelar) {
        ItemPedidoEntity item = itemPedidoJpaRepository
                .findById(itemPedidoId)
                .orElseThrow(() -> new BusinessException("Item não encontrado"));
        PedidoEntity pedido = item.getPedido();
        if (!pedido.getId().equals(pedidoId)) {
            throw new BusinessException("Item não pertence a este pedido");
        }
        if (pedido.getStatus() == PedidoStatus.PAGO || pedido.getStatus() == PedidoStatus.CANCELADO) {
            throw new BusinessException("Pedido encerrado; não é possível cancelar itens");
        }
        if (item.isCancelado()) {
            throw new BusinessException("Item já cancelado");
        }

        int qtdAtual = item.getQuantidade();
        int qCancel = quantidadeCancelar != null ? quantidadeCancelar : qtdAtual;
        if (qCancel <= 0) {
            throw new BusinessException("Informe uma quantidade válida para cancelar.");
        }
        if (qCancel > qtdAtual) {
            throw new BusinessException("Quantidade maior que a lançada neste item.");
        }

        BigDecimal valorRemover = item.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(qCancel))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal novoTotal = totalItens(pedido).subtract(valorRemover).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pago = somarPagamentos(pedido);
        if (pago.subtract(novoTotal).compareTo(EPS) > 0) {
            throw new BusinessException(
                    "Não é possível cancelar: o valor já pago excede o total da conta após remover esta quantidade.");
        }

        estoqueOperacaoService.restaurarQuantidadeProduto(item.getProduto(), qCancel);

        if (qCancel >= qtdAtual) {
            item.setCancelado(true);
            item.setCanceladoEm(Instant.now());
            item.setCanceladoPorLogin(usuarioLogin != null && !usuarioLogin.isBlank() ? usuarioLogin.trim() : null);
        } else {
            item.setQuantidade(qtdAtual - qCancel);
        }

        pedido.setAtualizadoEm(Instant.now());
        itemPedidoJpaRepository.save(item);
        pedidoJpaRepository.save(pedido);

        PedidoDetalheView detalhe = pedidoJpaRepository.findDetalhePorId(pedidoId)
                .map(this::mapearDetalhe)
                .orElseThrow();
        pedidoEventPublisherPort.notificarMudancaPedido(pedidoId);
        return detalhe;
    }

    @Override
    @Transactional
    public PedidoDetalheView atualizarStatus(Long pedidoId, PedidoStatus novoStatus) {
        PedidoEntity pedido = pedidoJpaRepository.findById(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        if (novoStatus == PedidoStatus.CANCELADO
                && pedido.getStatus() != PedidoStatus.CANCELADO
                && pedido.getStatus() != PedidoStatus.PAGO) {
            estoqueOperacaoService.restaurarItensPedido(pedido);
        }
        if (novoStatus == PedidoStatus.PAGO) {
            BigDecimal total = totalItens(pedido);
            BigDecimal pago = somarPagamentos(pedido);
            if (total.compareTo(BigDecimal.ZERO) > 0 && pago.add(EPS).compareTo(total) < 0) {
                throw new BusinessException(
                        "Registre pagamentos que cubram o total (ou deixe o pedido sem itens) antes de marcar como pago.");
            }
        }
        pedido.setStatus(novoStatus);
        pedido.setAtualizadoEm(Instant.now());

        if ((novoStatus == PedidoStatus.PAGO || novoStatus == PedidoStatus.CANCELADO)
                && pedido.getMesa() != null) {
            liberarMesaSeSemOutrosPedidos(pedido);
        }

        pedidoJpaRepository.save(pedido);
        PedidoDetalheView detalhe = pedidoJpaRepository.findDetalhePorId(pedidoId)
                .map(this::mapearDetalhe)
                .orElseThrow();
        pedidoEventPublisherPort.notificarMudancaPedido(pedidoId);
        return detalhe;
    }

    @Override
    @Transactional
    public PedidoDetalheView registrarPagamento(
            Long pedidoId, FormaPagamento forma, BigDecimal valor, BigDecimal valorRecebidoDinheiro) {
        PedidoEntity pedido = pedidoJpaRepository
                .findById(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        if (pedido.getStatus() == PedidoStatus.PAGO || pedido.getStatus() == PedidoStatus.CANCELADO) {
            throw new BusinessException("Pedido encerrado");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor inválido");
        }
        BigDecimal valorArred = valor.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = totalItens(pedido);
        BigDecimal jaPago = somarPagamentos(pedido);
        BigDecimal restante = total.subtract(jaPago).setScale(2, RoundingMode.HALF_UP);

        if (forma == FormaPagamento.DINHEIRO) {
            if (valorRecebidoDinheiro != null) {
                if (valorRecebidoDinheiro.compareTo(valorArred) < 0) {
                    throw new BusinessException("Valor recebido deve ser maior ou igual ao valor aplicado.");
                }
            }
            if (valorArred.compareTo(restante) > 0) {
                throw new BusinessException("Valor excede o restante da conta.");
            }
        } else {
            if (valorRecebidoDinheiro != null) {
                throw new BusinessException("Valor recebido só se aplica a pagamentos em dinheiro.");
            }
            if (valorArred.compareTo(restante) > 0) {
                throw new BusinessException("Valor excede o restante da conta.");
            }
        }

        var pg = new PagamentoPedidoEntity();
        pg.setForma(forma);
        pg.setValor(valorArred);
        if (forma == FormaPagamento.DINHEIRO && valorRecebidoDinheiro != null) {
            pg.setValorRecebidoDinheiro(valorRecebidoDinheiro.setScale(2, RoundingMode.HALF_UP));
        }
        pedido.addPagamento(pg);
        pedido.setAtualizadoEm(Instant.now());

        jaPago = somarPagamentos(pedido);
        boolean quitado =
                total.compareTo(BigDecimal.ZERO) == 0 || jaPago.add(EPS).compareTo(total) >= 0;
        if (quitado) {
            pedido.setStatus(PedidoStatus.PAGO);
            if (pedido.getMesa() != null) {
                liberarMesaSeSemOutrosPedidos(pedido);
            }
        }

        pedidoJpaRepository.save(pedido);
        PedidoDetalheView detalhe = pedidoJpaRepository.findDetalhePorId(pedidoId)
                .map(this::mapearDetalhe)
                .orElseThrow();
        pedidoEventPublisherPort.notificarMudancaPedido(pedidoId);
        return detalhe;
    }

    private void liberarMesaSeSemOutrosPedidos(PedidoEntity pedidoAtual) {
        MesaEntity mesa = pedidoAtual.getMesa();
        if (mesa == null) {
            return;
        }
        boolean existeOutro = pedidoJpaRepository.existsByMesa_IdAndIdNotAndStatusIn(
                mesa.getId(),
                pedidoAtual.getId(),
                STATUS_PEDIDO_ATIVO_MESA);
        if (!existeOutro) {
            mesa.setStatus(MesaStatus.LIVRE);
            mesaJpaRepository.save(mesa);
        }
    }

    @Override
    @Transactional
    public PedidoDetalheView transferirPedidoParaMesa(Long pedidoId, Long mesaDestinoId, String usuarioLogin) {
        PedidoEntity pedido = pedidoJpaRepository
                .findById(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        if (pedido.getTipo() != PedidoTipo.MESA) {
            throw new BusinessException("Só é possível transferir pedidos de mesa.");
        }
        if (!STATUS_PEDIDO_ATIVO_MESA.contains(pedido.getStatus())) {
            throw new BusinessException("Pedido encerrado não pode ser transferido.");
        }
        MesaEntity origem = pedido.getMesa();
        if (origem == null) {
            throw new BusinessException("Pedido sem mesa associada.");
        }
        if (origem.getId().equals(mesaDestinoId)) {
            throw new BusinessException("Escolha uma mesa diferente da atual.");
        }
        MesaEntity destino = mesaJpaRepository
                .findById(mesaDestinoId)
                .orElseThrow(() -> new BusinessException("Mesa destino não encontrada."));
        if (destino.getStatus() != MesaStatus.LIVRE) {
            throw new BusinessException("Mesa destino não está livre.");
        }
        if (obterIdPedidoAbertoNaMesa(mesaDestinoId).isPresent()) {
            throw new BusinessException("Mesa destino já possui pedido em andamento.");
        }

        Long origemId = origem.getId();
        int origemNum = origem.getNumero();
        int destinoNum = destino.getNumero();

        pedido.setMesa(destino);
        destino.setStatus(MesaStatus.OCUPADA);
        mesaJpaRepository.save(destino);

        boolean outroNaOrigem = pedidoJpaRepository.existsByMesa_IdAndIdNotAndStatusIn(
                origemId, pedidoId, STATUS_PEDIDO_ATIVO_MESA);
        if (!outroNaOrigem) {
            origem.setStatus(MesaStatus.LIVRE);
            mesaJpaRepository.save(origem);
        }

        pedido.setAtualizadoEm(Instant.now());
        pedidoJpaRepository.save(pedido);

        var log = new MesaTransferenciaEntity();
        log.setPedido(pedido);
        log.setMesaOrigemId(origemId);
        log.setMesaDestinoId(destino.getId());
        log.setMesaOrigemNumero(origemNum);
        log.setMesaDestinoNumero(destinoNum);
        log.setCriadoEm(Instant.now());
        log.setUsuarioLogin(usuarioLogin != null && !usuarioLogin.isBlank() ? usuarioLogin.trim() : null);
        mesaTransferenciaJpaRepository.save(log);

        PedidoDetalheView detalhe = pedidoJpaRepository
                .findDetalhePorId(pedidoId)
                .map(this::mapearDetalhe)
                .orElseThrow();
        pedidoEventPublisherPort.notificarMudancaPedido(pedidoId);
        return detalhe;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MesaTransferenciaView> listarTransferenciasMesa(Long pedidoId) {
        if (!pedidoJpaRepository.existsById(pedidoId)) {
            throw new BusinessException("Pedido não encontrado");
        }
        return mesaTransferenciaJpaRepository.findByPedido_IdOrderByCriadoEmAsc(pedidoId).stream()
                .map(t -> new MesaTransferenciaView(
                        t.getId(),
                        pedidoId,
                        t.getMesaOrigemNumero(),
                        t.getMesaDestinoNumero(),
                        t.getCriadoEm(),
                        t.getUsuarioLogin() != null ? t.getUsuarioLogin() : ""))
                .toList();
    }

    private PedidoListaView mapearLista(PedidoEntity p) {
        BigDecimal total = p.getItens().stream()
                .filter(i -> !i.isCancelado())
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer mesaNum = p.getMesa() != null ? p.getMesa().getNumero() : null;
        Long mesaId = p.getMesa() != null ? p.getMesa().getId() : null;
        String colab = p.getColaborador() != null ? p.getColaborador().getNome() : null;
        return new PedidoListaView(
                p.getId(),
                p.getTipo(),
                p.getStatus(),
                mesaId,
                mesaNum,
                colab,
                p.getCriadoEm(),
                total);
    }

    private PedidoDetalheView mapearDetalhe(PedidoEntity p) {
        List<ItemPedidoView> itens = p.getItens().stream()
                .map(i -> new ItemPedidoView(
                        i.getId(),
                        i.getProduto().getId(),
                        i.getProduto().getNome(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getObservacao(),
                        i.getPontoCarne(),
                        i.isCancelado(),
                        i.getCanceladoEm(),
                        i.getCanceladoPorLogin() != null ? i.getCanceladoPorLogin() : ""))
                .toList();

        BigDecimal total = itens.stream()
                .filter(i -> !i.cancelado())
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long mesaId = p.getMesa() != null ? p.getMesa().getId() : null;
        Integer mesaNum = p.getMesa() != null ? p.getMesa().getNumero() : null;
        Long clienteId = p.getCliente() != null ? p.getCliente().getId() : null;
        String clienteNome =
                p.getCliente() != null ? p.getCliente().getNome() : p.getNomeClienteLivre();
        Long colabId = p.getColaborador() != null ? p.getColaborador().getId() : null;
        String colabNome = p.getColaborador() != null ? p.getColaborador().getNome() : null;

        List<PagamentoPedidoView> pagamentos =
                p.getPagamentos().stream().map(this::mapearPagamento).toList();
        BigDecimal totalPago = somarPagamentos(p);
        BigDecimal restante = total.subtract(totalPago);
        if (restante.compareTo(BigDecimal.ZERO) < 0) {
            restante = BigDecimal.ZERO;
        }
        restante = restante.setScale(2, RoundingMode.HALF_UP);
        totalPago = totalPago.setScale(2, RoundingMode.HALF_UP);

        return new PedidoDetalheView(
                p.getId(),
                p.getTipo(),
                p.getStatus(),
                mesaId,
                mesaNum,
                clienteId,
                clienteNome,
                colabId,
                colabNome,
                p.getDescricaoMesa(),
                p.getPessoas(),
                p.isDocumentoFiscal(),
                p.getCriadoEm(),
                itens,
                total,
                pagamentos,
                totalPago,
                restante);
    }

    private PagamentoPedidoView mapearPagamento(PagamentoPedidoEntity pg) {
        BigDecimal troco = null;
        if (pg.getValorRecebidoDinheiro() != null
                && pg.getValorRecebidoDinheiro().compareTo(pg.getValor()) > 0) {
            troco = pg.getValorRecebidoDinheiro()
                    .subtract(pg.getValor())
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new PagamentoPedidoView(
                pg.getId(), pg.getForma(), pg.getValor(), pg.getValorRecebidoDinheiro(), troco);
    }

    private static BigDecimal totalItens(PedidoEntity p) {
        return p.getItens().stream()
                .filter(i -> !i.isCancelado())
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somarPagamentos(PedidoEntity p) {
        return p.getPagamentos().stream()
                .map(PagamentoPedidoEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean produtoEhEspetinho(ProdutoEntity produto) {
        return produto.getCategoria() != null
                && NOME_CATEGORIA_ESPETINHOS.equalsIgnoreCase(produto.getCategoria().getNome().strip());
    }
}
