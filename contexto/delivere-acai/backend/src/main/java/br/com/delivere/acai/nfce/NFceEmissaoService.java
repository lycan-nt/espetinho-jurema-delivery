package br.com.delivere.acai.nfce;

import br.com.delivere.acai.comanda.Comanda;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Emissão de NFC-e em ambiente de homologação.
 * Monta o XML infNFe 4.0, assina com certificado A1 e envia ao Web Service da SEFAZ (SVRS homologação para Bahia).
 */
@Service
public class NFceEmissaoService {

    private static final Logger log = LoggerFactory.getLogger(NFceEmissaoService.class);
    private static final String NS_NFE = "http://www.portalfiscal.inf.br/nfe";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HF = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Random RND = new Random();

    private final NFceProperties properties;
    private final CertificadoNFceLoader certificadoLoader;
    private final RestTemplate restTemplate = new RestTemplate();

    public NFceEmissaoService(NFceProperties properties, CertificadoNFceLoader certificadoLoader) {
        this.properties = properties;
        this.certificadoLoader = certificadoLoader;
    }

    /**
     * Emite NFC-e para a comanda fechada e retorna chave e protocolo (ou lança exceção).
     */
    public ResultadoEmissao emitir(Comanda comanda) throws Exception {
        if (!properties.isHabilitado()) {
            throw new IllegalStateException("Emissão de NFC-e está desabilitada. Configure app.nfce.habilitado=true e demais propriedades.");
        }
        if (!"FECHADA".equals(comanda.getStatus())) {
            throw new IllegalArgumentException("Comanda deve estar fechada para emitir NFC-e.");
        }

        String cnpj = properties.getEmitenteCnpj().replaceAll("\\D", "");
        if (cnpj.length() != 14) {
            throw new IllegalStateException("CNPJ do emitente deve ter 14 dígitos. Configure app.nfce.emitente-cnpj.");
        }

        int numero = properties.getProximoNumero();
        String chave = NFceChaveUtil.gerarChave(cnpj, properties.getSerie(), numero);
        String idInfNFe = "NFe" + chave;
        String cNF = chave.length() >= 39 ? chave.substring(31, 39) : String.format("%08d", RND.nextInt(100_000_000));

        String infNFeXml = montarInfNFe(comanda, chave, idInfNFe, numero, cNF);
        String nfeComAssinatura = assinarNFe(infNFeXml, idInfNFe);
        String enviNFe = montarEnviNFe(nfeComAssinatura);
        String resposta = enviarSoap(enviNFe);

        // Atualizar próximo número (em memória; em produção use DB ou arquivo)
        properties.setProximoNumero(numero + 1);

        String protocolo = extrairProtocolo(resposta);
        log.info("NFC-e emitida: chave={}, protocolo={}", chave, protocolo);
        return new ResultadoEmissao(chave, protocolo, numero);
    }

    private String montarInfNFe(Comanda comanda, String chave, String idInfNFe, int numero, String cNF) {
        LocalDateTime now = LocalDateTime.now();
        String dEmi = now.format(DF);
        String hEmi = now.format(HF);
        String dhEmi = now.format(DTF);
        int tpAmb = "HOMOLOGACAO".equalsIgnoreCase(properties.getAmbiente()) ? 2 : 1;

        BigDecimal vProd = comanda.getValorTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal qCom = comanda.getPesoKg().setScale(4, RoundingMode.HALF_UP);
        BigDecimal vUnCom = comanda.getPrecoPorKilo().setScale(4, RoundingMode.HALF_UP);

        String cnpj = escape(properties.getEmitenteCnpj().replaceAll("\\D", ""));
        String xNome = escape(properties.getEmitenteRazaoSocial());
        String xFant = escape(properties.getEmitenteFantasia());
        String xMun = escape(properties.getEmitenteMunicipio());
        String uf = escape(properties.getEmitenteUf());

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<infNFe xmlns=\"").append(NS_NFE).append("\" Id=\"").append(idInfNFe).append("\" versao=\"4.00\">");
        sb.append("<ide>");
        sb.append("<cUF>29</cUF>");
        sb.append("<cNF>").append(cNF).append("</cNF>");
        sb.append("<natOp>Venda de mercadoria</natOp>");
        sb.append("<mod>65</mod>");
        sb.append("<serie>").append(properties.getSerie()).append("</serie>");
        sb.append("<nNF>").append(numero).append("</nNF>");
        sb.append("<dhEmi>").append(dhEmi).append("</dhEmi>");
        sb.append("<tpNF>1</tpNF>");
        sb.append("<idDest>1</idDest>");
        sb.append("<cMunFG>").append(properties.getEmitenteCodigoMunicipio()).append("</cMunFG>");
        sb.append("<tpImp>4</tpImp>");
        sb.append("<tpEmis>1</tpEmis>");
        sb.append("<cDV>").append(chave.substring(chave.length() - 1)).append("</cDV>");
        sb.append("<tpAmb>").append(tpAmb).append("</tpAmb>");
        sb.append("<finNFe>1</finNFe>");
        sb.append("<indFinal>1</indFinal>");
        sb.append("<indPres>1</indPres>");
        sb.append("<procEmi>0</procEmi>");
        sb.append("<verProc>1.0</verProc>");
        sb.append("</ide>");

        sb.append("<emit>");
        sb.append("<CNPJ>").append(cnpj).append("</CNPJ>");
        sb.append("<xNome>").append(xNome).append("</xNome>");
        sb.append("<xFant>").append(xFant).append("</xFant>");
        sb.append("<enderEmit>");
        sb.append("<xLgr>Rua Exemplo</xLgr>");
        sb.append("<nro>0</nro>");
        sb.append("<xBairro>Centro</xBairro>");
        sb.append("<cMun>").append(properties.getEmitenteCodigoMunicipio()).append("</cMun>");
        sb.append("<xMun>").append(xMun).append("</xMun>");
        sb.append("<UF>").append(uf).append("</UF>");
        sb.append("<CEP>40000000</CEP>");
        sb.append("<cPais>1058</cPais>");
        sb.append("<xPais>Brasil</xPais>");
        sb.append("</enderEmit>");
        if (properties.getEmitenteIe() != null && !properties.getEmitenteIe().isBlank()) {
            sb.append("<IE>").append(escape(properties.getEmitenteIe())).append("</IE>");
        }
        sb.append("<CRT>").append(properties.getEmitenteCrt()).append("</CRT>");
        sb.append("</emit>");

        sb.append("<dest>");
        sb.append("<CNPJ>00000000000000</CNPJ>");
        sb.append("<xNome>Consumidor Final</xNome>");
        sb.append("<indIEDest>9</indIEDest>");
        sb.append("</dest>");

        sb.append("<det nItem=\"1\">");
        sb.append("<prod>");
        sb.append("<cEAN>0000000000000</cEAN>");
        sb.append("<cEANTrib>0000000000000</cEANTrib>");
        sb.append("<NCM>22021000</NCM>");
        sb.append("<CFOP>5102</CFOP>");
        sb.append("<xProd>").append(escape("Acai por kg")).append("</xProd>");
        sb.append("<uCom>KG</uCom>");
        sb.append("<qCom>").append(qCom.toPlainString()).append("</qCom>");
        sb.append("<vUnCom>").append(vUnCom.toPlainString()).append("</vUnCom>");
        sb.append("<vProd>").append(vProd.toPlainString()).append("</vProd>");
        sb.append("<cEAN>0000000000000</cEAN>");
        sb.append("<cEANTrib>0000000000000</cEANTrib>");
        sb.append("<uTrib>KG</uTrib>");
        sb.append("<qTrib>").append(qCom.toPlainString()).append("</qTrib>");
        sb.append("<indTot>1</indTot>");
        sb.append("</prod>");
        sb.append("<imposto>");
        sb.append("<vTotTrib>0.00</vTotTrib>");
        sb.append("<ICMS>");
        sb.append("<ICMS00>");
        sb.append("<orig>0</orig>");
        sb.append("<CST>00</CST>");
        sb.append("<modBC>0</modBC>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<pICMS>0.00</pICMS>");
        sb.append("<vICMS>0.00</vICMS>");
        sb.append("</ICMS00>");
        sb.append("</ICMS>");
        sb.append("<PIS>");
        sb.append("<PISAliq>");
        sb.append("<CST>01</CST>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<pPIS>0.00</pPIS>");
        sb.append("<vPIS>0.00</vPIS>");
        sb.append("</PISAliq>");
        sb.append("</PIS>");
        sb.append("<COFINS>");
        sb.append("<COFINSAliq>");
        sb.append("<CST>01</CST>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<pCOFINS>0.00</pCOFINS>");
        sb.append("<vCOFINS>0.00</vCOFINS>");
        sb.append("</COFINSAliq>");
        sb.append("</COFINS>");
        sb.append("</imposto>");
        sb.append("</det>");

        sb.append("<total>");
        sb.append("<ICMSTot>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<vICMS>0.00</vICMS>");
        sb.append("<vICMSDeson>0.00</vICMSDeson>");
        sb.append("<vFCP>0.00</vFCP>");
        sb.append("<vBCST>0.00</vBCST>");
        sb.append("<vST>0.00</vST>");
        sb.append("<vFCPST>0.00</vFCPST>");
        sb.append("<vFCPSTRet>0.00</vFCPSTRet>");
        sb.append("<vProd>").append(vProd.toPlainString()).append("</vProd>");
        sb.append("<vFrete>0.00</vFrete>");
        sb.append("<vSeg>0.00</vSeg>");
        sb.append("<vDesc>0.00</vDesc>");
        sb.append("<vII>0.00</vII>");
        sb.append("<vIPI>0.00</vIPI>");
        sb.append("<vIPIDevol>0.00</vIPIDevol>");
        sb.append("<vPIS>0.00</vPIS>");
        sb.append("<vCOFINS>0.00</vCOFINS>");
        sb.append("<vOutro>0.00</vOutro>");
        sb.append("<vNF>").append(vProd.toPlainString()).append("</vNF>");
        sb.append("</ICMSTot>");
        sb.append("</total>");

        sb.append("<transp><modFrete>9</modFrete></transp>");
        sb.append("</infNFe>");
        return sb.toString();
    }

    private String assinarNFe(String infNFeXml, String idInfNFe) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(infNFeXml)));
        Element infNFe = doc.getDocumentElement();

        // NFe deve conter infNFe + Signature; coloca infNFe dentro de NFe
        Element nfe = doc.createElementNS(NS_NFE, "NFe");
        nfe.setAttribute("xmlns", NS_NFE);
        doc.removeChild(infNFe);
        doc.appendChild(nfe);
        nfe.appendChild(infNFe);

        PrivateKey privateKey = certificadoLoader.getPrivateKey();
        X509Certificate cert = certificadoLoader.getCertificate();

        org.apache.xml.security.Init.init();
        org.apache.xml.security.signature.XMLSignature sig = new org.apache.xml.security.signature.XMLSignature(
                doc, "", org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        org.apache.xml.security.transforms.Transforms transforms = new org.apache.xml.security.transforms.Transforms(doc);
        transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        sig.addDocument("#" + idInfNFe, transforms, org.apache.xml.security.algorithms.MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
        sig.addKeyInfo(cert);
        sig.sign(privateKey);

        nfe.appendChild(sig.getElement());

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter w = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(w));
        return w.toString();
    }

    private String montarEnviNFe(String nfeXml) {
        String lote = String.valueOf(System.currentTimeMillis() % 999999999);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<enviNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<idLote>" + lote + "</idLote>"
                + "<indSinc>1</indSinc>"
                + "<NFe>" + nfeXml + "</NFe>"
                + "</enviNFe>";
    }

    private String enviarSoap(String enviNFe) {
        String soapBody = "<nfeDadosMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4\">"
                + escapeXmlCdata(enviNFe)
                + "</nfeDadosMsg>";
        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:nfe=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4\">"
                + "<soap:Body>"
                + "<nfe:nfeDadosMsg>"
                + "<![CDATA[" + enviNFe + "]]>"
                + "</nfe:nfeDadosMsg>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/soap+xml; charset=utf-8"));
        headers.set("SOAPAction", "http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4/nfeAutorizacaoLote");
        HttpEntity<String> entity = new HttpEntity<>(soap, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                properties.getUrlAutorizacao(),
                HttpMethod.POST,
                entity,
                String.class);
        return response.getBody() != null ? response.getBody() : "";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String escapeXmlCdata(String s) {
        return "<![CDATA[" + s.replace("]]>", "]]]]><![CDATA[>") + "]]>";
    }

    private String extrairProtocolo(String respostaSoap) {
        if (respostaSoap == null) return "";
        int i = respostaSoap.indexOf("<nProt>");
        if (i < 0) return "";
        int j = respostaSoap.indexOf("</nProt>", i);
        if (j < 0) return "";
        return respostaSoap.substring(i + 7, j).trim();
    }

    public static class ResultadoEmissao {
        private final String chave;
        private final String protocolo;
        private final int numero;

        public ResultadoEmissao(String chave, String protocolo, int numero) {
            this.chave = chave;
            this.protocolo = protocolo;
            this.numero = numero;
        }

        public String getChave() { return chave; }
        public String getProtocolo() { return protocolo; }
        public int getNumero() { return numero; }
    }
}
