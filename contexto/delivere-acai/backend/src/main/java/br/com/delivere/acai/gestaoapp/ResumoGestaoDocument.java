package br.com.delivere.acai.gestaoapp;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Documento no MongoDB com resumo de gestão de uma loja em uma data.
 * Id do documento = data_idLoja (ex.: 2025-02-12_01) para ter histórico (um doc por dia por loja).
 */
@Document(collection = "resumo_gestao")
public class ResumoGestaoDocument {

    @Id
    private String id;

    /** Data do relatório. */
    private LocalDate data;
    /** Identificador da loja (vem do cadastro da loja no banco). */
    private String idLoja;
    /** Nome e responsável da loja para exibição no app. */
    private String nomeLoja;
    private String responsavelLoja;

    private BigDecimal totalVendas;
    private Map<String, BigDecimal> totalPorFormaPagamento;
    private List<ComandaResumoItem> comandas;
    private List<CaixaResumoItem> caixas;
    private Instant updatedAt;

    public ResumoGestaoDocument() {
    }

    /** Monta o id do documento: data_idLoja (ex.: 2025-02-12_01). */
    public static String buildId(LocalDate data, String idLoja) {
        return data.toString() + "_" + idLoja;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getIdLoja() {
        return idLoja;
    }

    public void setIdLoja(String idLoja) {
        this.idLoja = idLoja;
    }

    public String getNomeLoja() {
        return nomeLoja;
    }

    public void setNomeLoja(String nomeLoja) {
        this.nomeLoja = nomeLoja;
    }

    public String getResponsavelLoja() {
        return responsavelLoja;
    }

    public void setResponsavelLoja(String responsavelLoja) {
        this.responsavelLoja = responsavelLoja;
    }

    public BigDecimal getTotalVendas() {
        return totalVendas;
    }

    public void setTotalVendas(BigDecimal totalVendas) {
        this.totalVendas = totalVendas;
    }

    public Map<String, BigDecimal> getTotalPorFormaPagamento() {
        return totalPorFormaPagamento;
    }

    public void setTotalPorFormaPagamento(Map<String, BigDecimal> totalPorFormaPagamento) {
        this.totalPorFormaPagamento = totalPorFormaPagamento;
    }

    public List<ComandaResumoItem> getComandas() {
        return comandas;
    }

    public void setComandas(List<ComandaResumoItem> comandas) {
        this.comandas = comandas;
    }

    public List<CaixaResumoItem> getCaixas() {
        return caixas;
    }

    public void setCaixas(List<CaixaResumoItem> caixas) {
        this.caixas = caixas;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
