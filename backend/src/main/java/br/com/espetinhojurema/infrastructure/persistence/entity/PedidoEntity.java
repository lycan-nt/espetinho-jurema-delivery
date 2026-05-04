package br.com.espetinhojurema.infrastructure.persistence.entity;

import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PedidoTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PedidoStatus status = PedidoStatus.ABERTO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id")
    private MesaEntity mesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    /** Nome informado na abertura, sem cadastro na tabela clientes. */
    @Column(name = "nome_cliente_livre", length = 200)
    private String nomeClienteLivre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id")
    private ColaboradorEntity colaborador;

    @Column(length = 500)
    private String descricaoMesa;

    private Integer pessoas;

    @Column(nullable = false)
    private boolean documentoFiscal = false;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    private Instant atualizadoEm = Instant.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedidoEntity> itens = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PagamentoPedidoEntity> pagamentos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PedidoTipo getTipo() {
        return tipo;
    }

    public void setTipo(PedidoTipo tipo) {
        this.tipo = tipo;
    }

    public PedidoStatus getStatus() {
        return status;
    }

    public void setStatus(PedidoStatus status) {
        this.status = status;
    }

    public MesaEntity getMesa() {
        return mesa;
    }

    public void setMesa(MesaEntity mesa) {
        this.mesa = mesa;
    }

    public ClienteEntity getCliente() {
        return cliente;
    }

    public void setCliente(ClienteEntity cliente) {
        this.cliente = cliente;
    }

    public String getNomeClienteLivre() {
        return nomeClienteLivre;
    }

    public void setNomeClienteLivre(String nomeClienteLivre) {
        this.nomeClienteLivre = nomeClienteLivre;
    }

    public ColaboradorEntity getColaborador() {
        return colaborador;
    }

    public void setColaborador(ColaboradorEntity colaborador) {
        this.colaborador = colaborador;
    }

    public String getDescricaoMesa() {
        return descricaoMesa;
    }

    public void setDescricaoMesa(String descricaoMesa) {
        this.descricaoMesa = descricaoMesa;
    }

    public Integer getPessoas() {
        return pessoas;
    }

    public void setPessoas(Integer pessoas) {
        this.pessoas = pessoas;
    }

    public boolean isDocumentoFiscal() {
        return documentoFiscal;
    }

    public void setDocumentoFiscal(boolean documentoFiscal) {
        this.documentoFiscal = documentoFiscal;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public List<ItemPedidoEntity> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedidoEntity> itens) {
        this.itens = itens;
    }

    public void addItem(ItemPedidoEntity item) {
        itens.add(item);
        item.setPedido(this);
    }

    public List<PagamentoPedidoEntity> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<PagamentoPedidoEntity> pagamentos) {
        this.pagamentos = pagamentos;
    }

    public void addPagamento(PagamentoPedidoEntity pagamento) {
        pagamentos.add(pagamento);
        pagamento.setPedido(this);
    }
}
