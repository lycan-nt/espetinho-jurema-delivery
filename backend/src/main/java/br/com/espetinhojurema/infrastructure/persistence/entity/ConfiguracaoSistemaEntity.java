package br.com.espetinhojurema.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistemaEntity {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(nullable = false)
    private boolean estoqueObrigatorio = false;

    /** Nome da fila CUPS (`lpstat -p`), ex.: térmica USB no Mac. Se vazio, comanda/cupom só no navegador. */
    @Column(name = "nome_impressora_lp", length = 200)
    private String nomeImpressoraLp;

    /**
     * Última versão do cardápio oficial aplicada pelo {@code DataInitializer}. {@code null} em bases
     * antigas é tratado como 0 (dispara reseed quando {@code app.catalogo.versao-seed-oficial} for maior).
     */
    @Column(name = "versao_catalogo_seed")
    private Integer versaoCatalogoSeed;

    @Column(name = "empresa_cnpj", length = 20)
    private String empresaCnpj;

    @Column(name = "empresa_nome", length = 200)
    private String empresaNome;

    @Column(name = "empresa_endereco", length = 500)
    private String empresaEndereco;

    @Column(name = "empresa_telefone", length = 40)
    private String empresaTelefone;

    @Column(name = "empresa_email", length = 120)
    private String empresaEmail;

    @Column(name = "empresa_instagram", length = 120)
    private String empresaInstagram;

    @Column(name = "comanda_cab_exibir_cnpj", nullable = false)
    @ColumnDefault("true")
    private boolean comandaCabecalhoExibirCnpj = true;

    @Column(name = "comanda_cab_exibir_nome", nullable = false)
    @ColumnDefault("true")
    private boolean comandaCabecalhoExibirNome = true;

    @Column(name = "comanda_cab_exibir_endereco", nullable = false)
    @ColumnDefault("true")
    private boolean comandaCabecalhoExibirEndereco = true;

    @Column(name = "comanda_cab_exibir_telefone", nullable = false)
    @ColumnDefault("true")
    private boolean comandaCabecalhoExibirTelefone = true;

    @Column(name = "comanda_cab_exibir_email", nullable = false)
    @ColumnDefault("true")
    private boolean comandaCabecalhoExibirEmail = true;

    @Column(name = "comanda_cab_exibir_instagram", nullable = false)
    @ColumnDefault("true")
    private boolean comandaCabecalhoExibirInstagram = true;

    /** Caminho gravado pelo usuário; vazio/null ⇒ usar padrão do sistema (`app.backup.directory`). */
    @Column(name = "backup_diretorio", length = 2000)
    private String backupDiretorio;

    @Column(name = "backup_ultimo_sucesso")
    private Instant backupUltimoSucesso;

    @Column(name = "backup_ultimo_erro_em")
    private Instant backupUltimoErroEm;

    @Column(name = "backup_ultimo_erro_msg", length = 2000)
    private String backupUltimoErroMsg;

    /** Horários locais (fuso {@code app.backup.schedule-zone}) dos dois backups automáticos por dia. */
    @Column(name = "backup_agend_h1")
    private Integer backupAgendHora1 = 19;

    @Column(name = "backup_agend_m1")
    private Integer backupAgendMinuto1 = 0;

    @Column(name = "backup_agend_h2")
    private Integer backupAgendHora2 = 21;

    @Column(name = "backup_agend_m2")
    private Integer backupAgendMinuto2 = 0;

    /** Dias no formato API: MON,TUE,WED,… (separados por vírgula). Vazio = cliente desmarcou todos (sem backup automático). */
    @Column(name = "backup_agend_dias", length = 64)
    private String backupAgendDiasSemana = "MON,TUE,WED,THU,FRI,SAT,SUN";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEstoqueObrigatorio() {
        return estoqueObrigatorio;
    }

    public void setEstoqueObrigatorio(boolean estoqueObrigatorio) {
        this.estoqueObrigatorio = estoqueObrigatorio;
    }

    public String getNomeImpressoraLp() {
        return nomeImpressoraLp;
    }

    public void setNomeImpressoraLp(String nomeImpressoraLp) {
        this.nomeImpressoraLp = nomeImpressoraLp;
    }

    public Integer getVersaoCatalogoSeed() {
        return versaoCatalogoSeed;
    }

    public void setVersaoCatalogoSeed(Integer versaoCatalogoSeed) {
        this.versaoCatalogoSeed = versaoCatalogoSeed;
    }

    public String getEmpresaCnpj() {
        return empresaCnpj;
    }

    public void setEmpresaCnpj(String empresaCnpj) {
        this.empresaCnpj = empresaCnpj;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public void setEmpresaNome(String empresaNome) {
        this.empresaNome = empresaNome;
    }

    public String getEmpresaEndereco() {
        return empresaEndereco;
    }

    public void setEmpresaEndereco(String empresaEndereco) {
        this.empresaEndereco = empresaEndereco;
    }

    public String getEmpresaTelefone() {
        return empresaTelefone;
    }

    public void setEmpresaTelefone(String empresaTelefone) {
        this.empresaTelefone = empresaTelefone;
    }

    public String getEmpresaEmail() {
        return empresaEmail;
    }

    public void setEmpresaEmail(String empresaEmail) {
        this.empresaEmail = empresaEmail;
    }

    public String getEmpresaInstagram() {
        return empresaInstagram;
    }

    public void setEmpresaInstagram(String empresaInstagram) {
        this.empresaInstagram = empresaInstagram;
    }

    public boolean isComandaCabecalhoExibirCnpj() {
        return comandaCabecalhoExibirCnpj;
    }

    public void setComandaCabecalhoExibirCnpj(boolean comandaCabecalhoExibirCnpj) {
        this.comandaCabecalhoExibirCnpj = comandaCabecalhoExibirCnpj;
    }

    public boolean isComandaCabecalhoExibirNome() {
        return comandaCabecalhoExibirNome;
    }

    public void setComandaCabecalhoExibirNome(boolean comandaCabecalhoExibirNome) {
        this.comandaCabecalhoExibirNome = comandaCabecalhoExibirNome;
    }

    public boolean isComandaCabecalhoExibirEndereco() {
        return comandaCabecalhoExibirEndereco;
    }

    public void setComandaCabecalhoExibirEndereco(boolean comandaCabecalhoExibirEndereco) {
        this.comandaCabecalhoExibirEndereco = comandaCabecalhoExibirEndereco;
    }

    public boolean isComandaCabecalhoExibirTelefone() {
        return comandaCabecalhoExibirTelefone;
    }

    public void setComandaCabecalhoExibirTelefone(boolean comandaCabecalhoExibirTelefone) {
        this.comandaCabecalhoExibirTelefone = comandaCabecalhoExibirTelefone;
    }

    public boolean isComandaCabecalhoExibirEmail() {
        return comandaCabecalhoExibirEmail;
    }

    public void setComandaCabecalhoExibirEmail(boolean comandaCabecalhoExibirEmail) {
        this.comandaCabecalhoExibirEmail = comandaCabecalhoExibirEmail;
    }

    public boolean isComandaCabecalhoExibirInstagram() {
        return comandaCabecalhoExibirInstagram;
    }

    public void setComandaCabecalhoExibirInstagram(boolean comandaCabecalhoExibirInstagram) {
        this.comandaCabecalhoExibirInstagram = comandaCabecalhoExibirInstagram;
    }

    public String getBackupDiretorio() {
        return backupDiretorio;
    }

    public void setBackupDiretorio(String backupDiretorio) {
        this.backupDiretorio = backupDiretorio;
    }

    public Instant getBackupUltimoSucesso() {
        return backupUltimoSucesso;
    }

    public void setBackupUltimoSucesso(Instant backupUltimoSucesso) {
        this.backupUltimoSucesso = backupUltimoSucesso;
    }

    public Instant getBackupUltimoErroEm() {
        return backupUltimoErroEm;
    }

    public void setBackupUltimoErroEm(Instant backupUltimoErroEm) {
        this.backupUltimoErroEm = backupUltimoErroEm;
    }

    public String getBackupUltimoErroMsg() {
        return backupUltimoErroMsg;
    }

    public void setBackupUltimoErroMsg(String backupUltimoErroMsg) {
        this.backupUltimoErroMsg = backupUltimoErroMsg;
    }

    public Integer getBackupAgendHora1() {
        return backupAgendHora1;
    }

    public void setBackupAgendHora1(Integer backupAgendHora1) {
        this.backupAgendHora1 = backupAgendHora1;
    }

    public Integer getBackupAgendMinuto1() {
        return backupAgendMinuto1;
    }

    public void setBackupAgendMinuto1(Integer backupAgendMinuto1) {
        this.backupAgendMinuto1 = backupAgendMinuto1;
    }

    public Integer getBackupAgendHora2() {
        return backupAgendHora2;
    }

    public void setBackupAgendHora2(Integer backupAgendHora2) {
        this.backupAgendHora2 = backupAgendHora2;
    }

    public Integer getBackupAgendMinuto2() {
        return backupAgendMinuto2;
    }

    public void setBackupAgendMinuto2(Integer backupAgendMinuto2) {
        this.backupAgendMinuto2 = backupAgendMinuto2;
    }

    public String getBackupAgendDiasSemana() {
        return backupAgendDiasSemana;
    }

    public void setBackupAgendDiasSemana(String backupAgendDiasSemana) {
        this.backupAgendDiasSemana = backupAgendDiasSemana;
    }
}
