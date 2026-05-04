package br.com.delivere.acai.gestaoapp;

import br.com.delivere.acai.caixa.Caixa;
import br.com.delivere.acai.comanda.Comanda;
import br.com.delivere.acai.comanda.ComandaService;
import br.com.delivere.acai.comanda.FormaPagamento;
import br.com.delivere.acai.comanda.RelatorioDTO;
import br.com.delivere.acai.loja.LojaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sincroniza o resumo de gestão do dia para o MongoDB.
 * Um documento por dia por loja (id = data_idLoja), com data, idLoja, nome, responsável e totais.
 */
@Service
@ConditionalOnBean(MongoTemplate.class)
public class GestaoSyncService {

    private static final Logger log = LoggerFactory.getLogger(GestaoSyncService.class);

    private final ComandaService comandaService;
    private final MongoTemplate mongoTemplate;
    private final String idLojaAtual;
    private final LojaRepository lojaRepository;

    public GestaoSyncService(ComandaService comandaService, MongoTemplate mongoTemplate,
                             @Value("${app.loja.id:}") String idLojaAtual,
                             LojaRepository lojaRepository) {
        this.comandaService = comandaService;
        this.mongoTemplate = mongoTemplate;
        this.idLojaAtual = idLojaAtual != null ? idLojaAtual.trim() : "";
        this.lojaRepository = lojaRepository;
    }

    /**
     * Grava/atualiza o documento do dia da loja atual no MongoDB.
     * Id do documento = data_idLoja (ex.: 2025-02-12_01). Histórico: um doc por dia por loja.
     */
    public void sync() {
        if (idLojaAtual.isEmpty()) {
            log.debug("app.loja.id não configurado; sync para MongoDB ignorado.");
            return;
        }
        try {
            LocalDate hoje = LocalDate.now();
            RelatorioDTO relatorio = comandaService.relatorio(hoje, hoje);

            ResumoGestaoDocument doc = new ResumoGestaoDocument();
            doc.setId(ResumoGestaoDocument.buildId(hoje, idLojaAtual));
            doc.setData(hoje);
            doc.setIdLoja(idLojaAtual);
            lojaRepository.findById(idLojaAtual).ifPresent(loja -> {
                doc.setNomeLoja(loja.getNome());
                doc.setResponsavelLoja(loja.getResponsavel());
            });
            doc.setTotalVendas(relatorio.getTotalVendas() != null ? relatorio.getTotalVendas() : BigDecimal.ZERO);
            doc.setTotalPorFormaPagamento(mapFormaPagamento(relatorio.getTotalPorFormaPagamento()));
            doc.setComandas(relatorio.getComandas().stream().map(this::toComandaResumo).collect(Collectors.toList()));
            doc.setCaixas(relatorio.getCaixas().stream().map(this::toCaixaResumo).collect(Collectors.toList()));
            doc.setUpdatedAt(Instant.now());

            mongoTemplate.save(doc);
            log.debug("Gestão sincronizada para MongoDB: id={}, totalVendas={}", doc.getId(), doc.getTotalVendas());
        } catch (Exception e) {
            log.warn("Falha ao sincronizar gestão para MongoDB: {}", e.getMessage());
        }
    }

    private Map<String, BigDecimal> mapFormaPagamento(Map<FormaPagamento, BigDecimal> totalPorForma) {
        if (totalPorForma == null) return Map.of();
        return totalPorForma.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
    }

    private ComandaResumoItem toComandaResumo(Comanda c) {
        ComandaResumoItem item = new ComandaResumoItem();
        item.setId(c.getId());
        item.setTipo(c.getTipo() != null ? c.getTipo().name() : null);
        item.setIdentificador(c.getIdentificador());
        item.setValorTotal(c.getValorTotal());
        item.setDataFechamento(c.getDataFechamento());
        item.setFormaPagamento(c.getFormaPagamento() != null ? c.getFormaPagamento().name() : null);
        item.setTipoProduto(c.getTipoProduto() != null ? c.getTipoProduto().name() : "POR_PESO");
        return item;
    }

    private CaixaResumoItem toCaixaResumo(Caixa c) {
        CaixaResumoItem item = new CaixaResumoItem();
        item.setId(c.getId());
        item.setData(c.getData());
        item.setValorAbertura(c.getValorAbertura());
        item.setValorFechamento(c.getValorFechamento());
        item.setStatus(c.getStatus());
        item.setDataHoraAbertura(c.getDataHoraAbertura());
        item.setDataHoraFechamento(c.getDataHoraFechamento());
        return item;
    }
}
