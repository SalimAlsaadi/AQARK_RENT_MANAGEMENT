#!/usr/bin/env bash
# Generates a self-signed CA and a Keycloak TLS certificate signed by that CA.
# Outputs to docker/certs/ — this directory is git-ignored.
set -euo pipefail

CERTS_DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
HOSTNAME="${KC_HOSTNAME:-localhost}"
DAYS=825  # Maximum trusted by modern browsers

mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

echo "[INFO] Generating CA private key and certificate..."
openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes \
  -key ca.key \
  -sha256 \
  -days "$DAYS" \
  -out ca.crt \
  -subj "/C=US/ST=State/L=City/O=AQARK/OU=Security/CN=AQARK-Local-CA"

echo "[INFO] Generating Keycloak server private key..."
openssl genrsa -out keycloak.key 4096

echo "[INFO] Creating certificate signing request (CSR)..."
openssl req -new \
  -key keycloak.key \
  -out keycloak.csr \
  -subj "/C=US/ST=State/L=City/O=AQARK/OU=Keycloak/CN=${HOSTNAME}"

echo "[INFO] Creating v3 SAN extension file..."
cat > keycloak.v3.ext <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
extendedKeyUsage=serverAuth
subjectAltName=@alt_names

[alt_names]
DNS.1 = ${HOSTNAME}
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

echo "[INFO] Signing Keycloak certificate with CA..."
openssl x509 -req \
  -in keycloak.csr \
  -CA ca.crt \
  -CAkey ca.key \
  -CAcreateserial \
  -out keycloak.crt \
  -days "$DAYS" \
  -sha256 \
  -extfile keycloak.v3.ext

rm -f keycloak.csr keycloak.v3.ext
chmod 600 keycloak.key ca.key
chmod 644 keycloak.crt ca.crt

echo ""
echo "[SUCCESS] Certificates written to: $CERTS_DIR"
echo ""
echo "  CA certificate:         $CERTS_DIR/ca.crt"
echo "  Keycloak certificate:   $CERTS_DIR/keycloak.crt"
echo "  Keycloak private key:   $CERTS_DIR/keycloak.key"
echo ""
echo "===================================================================="
echo "  TRUST THE CA IN YOUR OS / BROWSER"
echo "===================================================================="
echo ""
echo "  Windows (PowerShell as Administrator):"
echo "    Import-Certificate -FilePath \"$CERTS_DIR/ca.crt\" \\"
echo "      -CertStoreLocation 'Cert:\\LocalMachine\\Root'"
echo ""
echo "  For Java (JVM trust store):"
echo "    keytool -importcert -trustcacerts -alias aqark-local-ca \\"
echo "      -file $CERTS_DIR/ca.crt \\"
echo "      -keystore \"\$JAVA_HOME/lib/security/cacerts\" -storepass changeit"
echo ""
echo "  For Spring Boot app keystore (run after trusting CA):"
echo "    cd $CERTS_DIR"
echo "    openssl pkcs12 -export -in keycloak.crt -inkey keycloak.key \\"
echo "      -out app-keystore.p12 -name spring-app -passout pass:changeit"
echo "    cp app-keystore.p12 ../src/main/resources/keystore/"
echo "===================================================================="
