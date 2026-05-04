package br.com.delivere.acai.nfce;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração para emissão de NFC-e em homologação (Bahia).
 * Certificado: use o certificado de homologação da SEFAZ-BA ou PFX de teste.
 */
@Component
@ConfigurationProperties(prefix = "app.nfce")
public class NFceProperties {

    /** Ativar emissão (homologação). */
    private boolean habilitado = false;

    /** Caminho do certificado A1 PFX (ex: classpath:certificado.pfx ou file:./cert.pfx). */
    private String certificadoPath = "";

    /** Senha do certificado PFX. */
    private String certificadoSenha = "";

    /** Ambiente: HOMOLOGACAO ou PRODUCAO. */
    private String ambiente = "HOMOLOGACAO";

    /**
     * URL do Web Service de autorização.
     * Homologação SVRS (Bahia pode usar SVRS): https://nfce-homologacao.svrs.rs.gov.br/ws/NfeAutorizacao/NFeAutorizacao4.asmx
     */
    private String urlAutorizacao = "https://nfce-homologacao.svrs.rs.gov.br/ws/NfeAutorizacao/NFeAutorizacao4.asmx";

    /** CNPJ do emitente (14 dígitos, só números). */
    private String emitenteCnpj = "";

    /** Razão social do emitente. */
    private String emitenteRazaoSocial = "MIX ACAI LTDA";

    /** Nome fantasia. */
    private String emitenteFantasia = "MIX ACAI";

    /** Código do município (IBGE 7 dígitos). Ex: Salvador = 2927408. */
    private String emitenteCodigoMunicipio = "2927408";

    /** Nome do município. */
    private String emitenteMunicipio = "SALVADOR";

    /** UF do emitente (BA). */
    private String emitenteUf = "BA";

    /** Inscrição estadual (vazio se MEI/simples sem IE). */
    private String emitenteIe = "";

    /** CRT: 1=Simples Nacional, 2=Simples excesso, 3=Normal. */
    private int emitenteCrt = 1;

    /** Série da NFC-e (1 a 999, geralmente 1). */
    private int serie = 1;

    /** Próximo número da NFC-e (controle local; em homologação pode ser fixo). */
    private int proximoNumero = 1;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getCertificadoPath() {
        return certificadoPath;
    }

    public void setCertificadoPath(String certificadoPath) {
        this.certificadoPath = certificadoPath;
    }

    public String getCertificadoSenha() {
        return certificadoSenha;
    }

    public void setCertificadoSenha(String certificadoSenha) {
        this.certificadoSenha = certificadoSenha;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getUrlAutorizacao() {
        return urlAutorizacao;
    }

    public void setUrlAutorizacao(String urlAutorizacao) {
        this.urlAutorizacao = urlAutorizacao;
    }

    public String getEmitenteCnpj() {
        return emitenteCnpj;
    }

    public void setEmitenteCnpj(String emitenteCnpj) {
        this.emitenteCnpj = emitenteCnpj;
    }

    public String getEmitenteRazaoSocial() {
        return emitenteRazaoSocial;
    }

    public void setEmitenteRazaoSocial(String emitenteRazaoSocial) {
        this.emitenteRazaoSocial = emitenteRazaoSocial;
    }

    public String getEmitenteFantasia() {
        return emitenteFantasia;
    }

    public void setEmitenteFantasia(String emitenteFantasia) {
        this.emitenteFantasia = emitenteFantasia;
    }

    public String getEmitenteCodigoMunicipio() {
        return emitenteCodigoMunicipio;
    }

    public void setEmitenteCodigoMunicipio(String emitenteCodigoMunicipio) {
        this.emitenteCodigoMunicipio = emitenteCodigoMunicipio;
    }

    public String getEmitenteMunicipio() {
        return emitenteMunicipio;
    }

    public void setEmitenteMunicipio(String emitenteMunicipio) {
        this.emitenteMunicipio = emitenteMunicipio;
    }

    public String getEmitenteUf() {
        return emitenteUf;
    }

    public void setEmitenteUf(String emitenteUf) {
        this.emitenteUf = emitenteUf;
    }

    public String getEmitenteIe() {
        return emitenteIe;
    }

    public void setEmitenteIe(String emitenteIe) {
        this.emitenteIe = emitenteIe;
    }

    public int getEmitenteCrt() {
        return emitenteCrt;
    }

    public void setEmitenteCrt(int emitenteCrt) {
        this.emitenteCrt = emitenteCrt;
    }

    public int getSerie() {
        return serie;
    }

    public void setSerie(int serie) {
        this.serie = serie;
    }

    public int getProximoNumero() {
        return proximoNumero;
    }

    public void setProximoNumero(int proximoNumero) {
        this.proximoNumero = proximoNumero;
    }
}
