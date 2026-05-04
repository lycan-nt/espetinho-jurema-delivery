package br.com.delivere.acai.comanda;

import br.com.delivere.acai.caixa.Caixa;
import br.com.delivere.acai.caixa.CaixaRepository;
import br.com.delivere.acai.nfce.NFceEmissaoService;
import br.com.delivere.acai.nfce.NFceEmissaoService.ResultadoEmissao;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ComandaService {

    private final ComandaRepository repository;
    private final ComandaItemRepository itemRepository;
    private final CaixaRepository caixaRepository;
    private final NFceEmissaoService nfceEmissaoService;

    public ComandaService(ComandaRepository repository, ComandaItemRepository itemRepository,
                          CaixaRepository caixaRepository, NFceEmissaoService nfceEmissaoService) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.caixaRepository = caixaRepository;
        this.nfceEmissaoService = nfceEmissaoService;
    }

    @Transactional
    public Comanda criar(Comanda comanda) {
        TipoProduto tipoProduto = comanda.getTipoProduto() != null ? comanda.getTipoProduto() : TipoProduto.POR_PESO;
        Optional<Comanda> existenteOpt = repository.findFirstByTipoAndIdentificadorAndStatus(
                comanda.getTipo(), comanda.getIdentificador(), "ABERTA");

        if (existenteOpt.isPresent()) {
            Comanda existente = existenteOpt.get();
            if (tipoProduto == TipoProduto.PRECO_FIXO) {
                BigDecimal valorAdicionar = comanda.getValorTotal() != null ? comanda.getValorTotal() : BigDecimal.ZERO;
                existente.setValorTotal(existente.getValorTotal().add(valorAdicionar));
                existente = repository.save(existente);
                salvarItem(existente.getId(), comanda, tipoProduto);
                return existente;
            }
            BigDecimal novoPeso = existente.getPesoKg().add(comanda.getPesoKg() != null ? comanda.getPesoKg() : BigDecimal.ZERO);
            BigDecimal valorDesteLancamento;
            if (comanda.getValorTotal() != null && comanda.getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
                valorDesteLancamento = comanda.getValorTotal().setScale(2, RoundingMode.HALF_UP);
            } else {
                valorDesteLancamento = (comanda.getPesoKg() != null && comanda.getPrecoPorKilo() != null)
                        ? comanda.getPesoKg().multiply(comanda.getPrecoPorKilo()).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            }
            BigDecimal novoTotal = existente.getValorTotal().add(valorDesteLancamento);
            existente.setPesoKg(novoPeso);
            existente.setPrecoPorKilo(comanda.getPrecoPorKilo() != null ? comanda.getPrecoPorKilo() : existente.getPrecoPorKilo());
            existente.setValorTotal(novoTotal);
            existente = repository.save(existente);
            salvarItem(existente.getId(), comanda, tipoProduto);
            return existente;
        }

        if (tipoProduto == TipoProduto.PRECO_FIXO) {
            BigDecimal valor = comanda.getValorTotal() != null ? comanda.getValorTotal() : BigDecimal.ZERO;
            comanda.setValorTotal(valor);
            comanda.setPesoKg(BigDecimal.ZERO);
            comanda.setPrecoPorKilo(BigDecimal.ZERO);
        } else {
            BigDecimal peso = comanda.getPesoKg() != null ? comanda.getPesoKg() : BigDecimal.ZERO;
            BigDecimal preco = comanda.getPrecoPorKilo() != null ? comanda.getPrecoPorKilo() : BigDecimal.ZERO;
            if (comanda.getValorTotal() != null && comanda.getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
                comanda.setValorTotal(comanda.getValorTotal().setScale(2, RoundingMode.HALF_UP));
            } else {
                comanda.setValorTotal(peso.multiply(preco).setScale(2, RoundingMode.HALF_UP));
            }
        }
        comanda.setTipoProduto(tipoProduto);
        comanda.setOpenedByUsername(getCurrentUsername());
        Comanda salva = repository.save(comanda);
        salvarItem(salva.getId(), comanda, tipoProduto);
        return salva;
    }

    private void salvarItem(Long comandaId, Comanda comanda, TipoProduto tipoProduto) {
        ComandaItem item = new ComandaItem();
        item.setComandaId(comandaId);
        item.setTipoProduto(tipoProduto);
        if (tipoProduto == TipoProduto.PRECO_FIXO) {
            item.setPesoKg(BigDecimal.ZERO);
            item.setPrecoUnitario(comanda.getPrecoPorKilo() != null ? comanda.getPrecoPorKilo() : BigDecimal.ZERO);
            int qtd = comanda.getQuantidade() != null && comanda.getQuantidade() > 0 ? comanda.getQuantidade() : 1;
            item.setQuantidade(qtd);
            item.setValorTotal(comanda.getValorTotal() != null ? comanda.getValorTotal() : BigDecimal.ZERO);
        } else {
            item.setPesoKg(comanda.getPesoKg() != null ? comanda.getPesoKg() : BigDecimal.ZERO);
            item.setPrecoUnitario(comanda.getPrecoPorKilo() != null ? comanda.getPrecoPorKilo() : BigDecimal.ZERO);
            item.setQuantidade(1);
            if (comanda.getValorTotal() != null && comanda.getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
                item.setValorTotal(comanda.getValorTotal().setScale(2, RoundingMode.HALF_UP));
            } else {
                item.setValorTotal(item.getPesoKg().multiply(item.getPrecoUnitario()).setScale(2, RoundingMode.HALF_UP));
            }
        }
        itemRepository.save(item);
    }

    public List<Comanda> listarTodas() {
        return repository.findAll();
    }

    public List<Comanda> listarAbertas() {
        return repository.findByStatusOrderByDataHoraDesc("ABERTA");
    }

    public Comanda buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comanda não encontrada: " + id));
    }

    /**
     * Retorna o próximo identificador sequencial para COMANDA (ex.: "001", "002", ...).
     * Usa o maior valor numérico já presente em qualquer registro {@link TipoComanda#COMANDA},
     * não o último ID do banco — assim trocar tipo para CLIENTE na linha atual não “esconde”
     * os números já usados nem faz a sequência voltar para 001 indevidamente.
     */
    public String proximoIdentificador(TipoComanda tipo) {
        if (tipo == null || tipo != TipoComanda.COMANDA) {
            return "001";
        }
        int max = 0;
        for (Comanda c : repository.findByTipo(TipoComanda.COMANDA)) {
            String idAtual = c.getIdentificador();
            if (idAtual == null || idAtual.isBlank()) {
                continue;
            }
            String numeros = idAtual.replaceAll("\\D", "");
            if (numeros.isEmpty()) {
                continue;
            }
            try {
                int n = Integer.parseInt(numeros);
                if (n > max) {
                    max = n;
                }
            } catch (NumberFormatException ignored) {
                // ignora identificadores não numéricos
            }
        }
        return String.format("%03d", max + 1);
    }

    @Transactional
    public Comanda atualizar(Long id, Comanda comandaAtualizada) {
        Comanda existente = buscarPorId(id);
        existente.setPesoKg(comandaAtualizada.getPesoKg());
        existente.setPrecoPorKilo(comandaAtualizada.getPrecoPorKilo());
        BigDecimal valorTotal = existente.getPesoKg()
                .multiply(existente.getPrecoPorKilo())
                .setScale(2, RoundingMode.HALF_UP);
        existente.setValorTotal(valorTotal);
        return repository.save(existente);
    }

    /**
     * Altera tipo (cliente / mesa / comanda) e identificador enquanto a comanda estiver aberta.
     * Não altera itens nem totais. Evita duplicar outra comanda aberta com o mesmo par tipo+identificador.
     */
    @Transactional
    public Comanda alterarCabecalho(Long id, TipoComanda novoTipo, String novoIdentificador) {
        if (novoTipo == null) {
            throw new IllegalArgumentException("Tipo é obrigatório.");
        }
        String idNorm = novoIdentificador == null ? "" : novoIdentificador.trim();
        if (idNorm.isEmpty()) {
            throw new IllegalArgumentException("Identificador é obrigatório.");
        }
        Comanda c = buscarPorId(id);
        if (!"ABERTA".equals(c.getStatus())) {
            throw new IllegalStateException("Só é possível alterar tipo e identificador em comanda aberta.");
        }
        repository.findFirstByTipoAndIdentificadorAndStatus(novoTipo, idNorm, "ABERTA")
                .ifPresent(outra -> {
                    if (!outra.getId().equals(id)) {
                        throw new IllegalStateException(
                                "Já existe outra comanda aberta com este tipo e identificador.");
                    }
                });
        c.setTipo(novoTipo);
        c.setIdentificador(idNorm);
        return repository.save(c);
    }

    /**
     * Remove um item da comanda (subtrai peso e valor). Remove também o registro em comanda_itens quando existir.
     */
    @Transactional
    public Comanda removerItem(Long id, BigDecimal pesoKgRemover, BigDecimal valorTotalRemover) {
        Comanda comanda = buscarPorId(id);
        if (!"ABERTA".equals(comanda.getStatus())) {
            throw new IllegalStateException("Só é possível remover itens de comanda aberta.");
        }
        Optional<ComandaItem> itemOpt = itemRepository.findFirstByComandaIdAndPesoKgAndValorTotalOrderByIdDesc(
                id, pesoKgRemover != null ? pesoKgRemover : BigDecimal.ZERO,
                valorTotalRemover != null ? valorTotalRemover : BigDecimal.ZERO);
        itemOpt.ifPresent(itemRepository::delete);
        BigDecimal novoPeso = comanda.getPesoKg().subtract(pesoKgRemover != null ? pesoKgRemover : BigDecimal.ZERO).max(BigDecimal.ZERO);
        BigDecimal novoTotal = comanda.getValorTotal().subtract(valorTotalRemover != null ? valorTotalRemover : BigDecimal.ZERO).max(BigDecimal.ZERO);
        comanda.setPesoKg(novoPeso);
        comanda.setValorTotal(novoTotal);
        if (novoPeso.compareTo(BigDecimal.ZERO) > 0) {
            comanda.setPrecoPorKilo(novoTotal.divide(novoPeso, 2, RoundingMode.HALF_UP));
        } else {
            comanda.setPrecoPorKilo(BigDecimal.ZERO);
        }
        return repository.save(comanda);
    }

    public List<ComandaItem> listarItens(Long comandaId) {
        return itemRepository.findByComandaIdOrderByIdAsc(comandaId);
    }

    @Transactional
    public Comanda fechar(Long id, FormaPagamento formaPagamento) {
        Comanda comanda = buscarPorId(id);
        comanda.setStatus("FECHADA");
        comanda.setFormaPagamento(formaPagamento);
        comanda.setDataFechamento(LocalDateTime.now());
        comanda.setClosedByUsername(getCurrentUsername());
        return repository.save(comanda);
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                ? auth.getName()
                : null;
    }

    public RelatorioDTO relatorio(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now();
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
        if (fim.isBefore(inicio)) {
            fim = inicio;
        }
        LocalDateTime from = inicio.atStartOfDay();
        LocalDateTime to = fim.atTime(LocalTime.MAX);

        List<Comanda> comandas = repository.findByStatusAndDataFechamentoBetweenOrderByDataFechamentoDesc(
                "FECHADA", from, to);
        BigDecimal totalVendas = comandas.stream()
                .map(Comanda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<FormaPagamento, BigDecimal> totalPorForma = new EnumMap<>(FormaPagamento.class);
        for (FormaPagamento forma : FormaPagamento.values()) {
            totalPorForma.put(forma, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        for (Comanda c : comandas) {
            if (c.getFormaPagamento() != null) {
                totalPorForma.merge(c.getFormaPagamento(), c.getValorTotal(), BigDecimal::add);
            }
        }
        List<Caixa> caixas = caixaRepository.findByDataBetweenOrderByDataAscDataHoraAberturaAsc(inicio, fim);
        return new RelatorioDTO(comandas, totalVendas, totalPorForma, caixas);
    }

    /**
     * Dados para o dashboard (tela inicial). Não inclui estado do caixa (evitar dependência circular).
     */
    public DashboardDTO dashboard() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime startOfToday = hoje.atStartOfDay();
        long comandasAbertasHoje = repository.countByStatusAndDataHoraGreaterThanEqual("ABERTA", startOfToday);
        long comandasAbertasMaisDeUmDia = repository.countByStatusAndDataHoraLessThan("ABERTA", startOfToday);
        long pedidosPendentesNfce = repository.countFechadasSemNfce();
        RelatorioDTO relatorioHoje = relatorio(hoje, hoje);
        BigDecimal totalVendasHoje = relatorioHoje.getTotalVendas() != null ? relatorioHoje.getTotalVendas() : BigDecimal.ZERO;
        return new DashboardDTO(comandasAbertasHoje, comandasAbertasMaisDeUmDia, pedidosPendentesNfce, false, totalVendasHoje);
    }

    /**
     * Emite NFC-e em homologação para a comanda fechada e grava chave/protocolo na comanda.
     */
    @Transactional
    public Comanda emitirNfce(Long id) throws Exception {
        Comanda comanda = buscarPorId(id);
        ResultadoEmissao resultado = nfceEmissaoService.emitir(comanda);
        comanda.setChaveNfce(resultado.getChave());
        comanda.setProtocoloNfce(resultado.getProtocolo());
        return repository.save(comanda);
    }
}
