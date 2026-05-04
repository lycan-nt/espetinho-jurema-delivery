package br.com.delivere.acai.comanda;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comandas")
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TipoComanda tipo;

    @Column(nullable = false)
    private String identificador;

    @Column(nullable = false, precision = 10, scale = 3)
    @DecimalMin(value = "0", message = "Peso não pode ser negativo")
    private BigDecimal pesoKg;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0", message = "Preço por kilo não pode ser negativo")
    private BigDecimal precoPorKilo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false)
    private String status = "ABERTA";

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    private LocalDateTime dataFechamento;

    /** Chave de acesso da NFC-e (44 dígitos), quando emitida. */
    private String chaveNfce;

    /** Número do protocolo de autorização da SEFAZ. */
    private String protocoloNfce;

    /** Usuário que abriu a comanda (username). */
    private String openedByUsername;

    /** Usuário que fechou a comanda (username). */
    private String closedByUsername;

    /** Tipo de produto: por peso (açaí/sorvete) ou preço fixo (padrão). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_produto")
    private TipoProduto tipoProduto = TipoProduto.POR_PESO;

    /** Quantidade (apenas para PRECO_FIXO no request; não persistido). */
    @Transient
    private Integer quantidade;

    public Comanda() {
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public TipoProduto getTipoProduto() {
        return tipoProduto != null ? tipoProduto : TipoProduto.POR_PESO;
    }

    public void setTipoProduto(TipoProduto tipoProduto) {
        this.tipoProduto = tipoProduto != null ? tipoProduto : TipoProduto.POR_PESO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoComanda getTipo() {
        return tipo;
    }

    public void setTipo(TipoComanda tipo) {
        this.tipo = tipo;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getPrecoPorKilo() {
        return precoPorKilo;
    }

    public void setPrecoPorKilo(BigDecimal precoPorKilo) {
        this.precoPorKilo = precoPorKilo;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public String getChaveNfce() {
        return chaveNfce;
    }

    public void setChaveNfce(String chaveNfce) {
        this.chaveNfce = chaveNfce;
    }

    public String getProtocoloNfce() {
        return protocoloNfce;
    }

    public void setProtocoloNfce(String protocoloNfce) {
        this.protocoloNfce = protocoloNfce;
    }

    public String getOpenedByUsername() {
        return openedByUsername;
    }

    public void setOpenedByUsername(String openedByUsername) {
        this.openedByUsername = openedByUsername;
    }

    public String getClosedByUsername() {
        return closedByUsername;
    }

    public void setClosedByUsername(String closedByUsername) {
        this.closedByUsername = closedByUsername;
    }
}
