package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only TLS trust for Keycloak's self-signed (mkcert) certificate.
 *
 * <p>Keycloak runs over HTTPS with a certificate signed by a local development CA
 * ({@code docker/certs/ca.crt}). The JVM does not trust that CA by default, so every Spring
 * HTTP call to Keycloak (JWKS retrieval for JWT validation, the BFF authorization-code token
 * exchange, the OIDC userinfo request, and ID-token signature validation) fails with
 * {@code PKIX path building failed}.
 *
 * <p>This installs a composite trust manager that trusts the JVM's default CAs <b>plus</b> the
 * Keycloak dev CA, then sets it as the JVM default. Verification (including hostname checks)
 * stays fully enabled — this only <i>adds</i> trust for one extra CA. In production, point the
 * app at a Keycloak served with a publicly trusted certificate and this CA file simply will not
 * be present (the config logs and skips).
 */
@Slf4j
@Configuration
public class KeycloakTlsTrustConfig {

    private final ResourceLoader resourceLoader;

    @Value("${keycloak.tls.ca-cert:file:docker/certs/ca.crt}")
    private String caCertLocation;

    public KeycloakTlsTrustConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void trustKeycloakCa() {
        Resource caResource = resourceLoader.getResource(caCertLocation);
        if (!caResource.exists()) {
            log.info("Keycloak dev CA not found at {} — skipping dev TLS trust (expected in prod).",
                    caCertLocation);
            return;
        }

        try {
            X509TrustManager composite = buildCompositeTrustManager(caResource);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{composite}, null);
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            log.info("Installed dev TLS trust for the Keycloak CA from {}.", caCertLocation);
        } catch (Exception e) {
            log.error("Failed to install dev TLS trust for the Keycloak CA from {}: {}",
                    caCertLocation, e.getMessage());
        }
    }

    private X509TrustManager buildCompositeTrustManager(Resource caResource) throws Exception {
        X509TrustManager defaultTm = loadDefaultTrustManager();
        X509TrustManager caTm = loadCaTrustManager(caResource);

        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                defaultTm.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    defaultTm.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    caTm.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                List<X509Certificate> issuers = new ArrayList<>();
                issuers.addAll(List.of(defaultTm.getAcceptedIssuers()));
                issuers.addAll(List.of(caTm.getAcceptedIssuers()));
                return issuers.toArray(new X509Certificate[0]);
            }
        };
    }

    private X509TrustManager loadDefaultTrustManager() throws Exception {
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        return firstX509(tmf);
    }

    private X509TrustManager loadCaTrustManager(Resource caResource) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate ca;
        try (InputStream is = caResource.getInputStream()) {
            ca = (X509Certificate) cf.generateCertificate(is);
        }

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("keycloak-dev-ca", ca);

        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return firstX509(tmf);
    }

    private X509TrustManager firstX509(TrustManagerFactory tmf) {
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("No X509TrustManager found");
    }
}
