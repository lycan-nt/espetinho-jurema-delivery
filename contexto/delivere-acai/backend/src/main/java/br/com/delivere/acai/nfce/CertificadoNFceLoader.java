package br.com.delivere.acai.nfce;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Carrega certificado A1 (PFX) para assinar a NFC-e.
 */
@Component
public class CertificadoNFceLoader {

    private final NFceProperties properties;
    private final ResourceLoader resourceLoader;

    public CertificadoNFceLoader(NFceProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public KeyStore loadKeyStore() throws Exception {
        String path = properties.getCertificadoPath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("app.nfce.certificado-path não configurado.");
        }
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Certificado não encontrado: " + path);
        }
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            ks.load(is, properties.getCertificadoSenha().toCharArray());
        }
        return ks;
    }

    public PrivateKey getPrivateKey() throws Exception {
        KeyStore ks = loadKeyStore();
        String alias = ks.aliases().nextElement();
        return (PrivateKey) ks.getKey(alias, properties.getCertificadoSenha().toCharArray());
    }

    public X509Certificate getCertificate() throws Exception {
        KeyStore ks = loadKeyStore();
        String alias = ks.aliases().nextElement();
        return (X509Certificate) ks.getCertificate(alias);
    }
}
