package sun.security.ssl;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import sun.security.internal.spec.TlsMasterSecretParameterSpec;
import sun.security.ssl.CipherSuite.HashAlg;
import static sun.security.ssl.CipherSuite.HashAlg.H_NONE;

enum SSLMasterKeyDerivation implements SSLKeyDerivationGenerator {
    SSL30       ("kdf_ssl30"),
    TLS10       ("kdf_tls10"),
    TLS12       ("kdf_tls12");

    final String name;

    private SSLMasterKeyDerivation(String name) {
        this.name = name;
    }

    static SSLMasterKeyDerivation valueOf(ProtocolVersion protocolVersion) {
        switch (protocolVersion) {
            case SSL30:
                return SSLMasterKeyDerivation.SSL30;
            case TLS10:
            case TLS11:
            case DTLS10:
                return SSLMasterKeyDerivation.TLS10;
            case TLS12:
            case DTLS12:
                return SSLMasterKeyDerivation.TLS12;
            default:
                return null;
        }
    }

    @Override
    public SSLKeyDerivation createKeyDerivation(HandshakeContext context,
            SecretKey secretKey) throws IOException {
        return new LegacyMasterKeyDerivation(context, secretKey);
    }

    // Note, we may use different key derivation implementation in the future.
    private static final
            class LegacyMasterKeyDerivation implements SSLKeyDerivation {

        final HandshakeContext context;
        final SecretKey preMasterSecret;

        LegacyMasterKeyDerivation(
                HandshakeContext context, SecretKey preMasterSecret) {
            this.context = context;
            this.preMasterSecret = preMasterSecret;
        }

        @Override
        @SuppressWarnings("deprecation")
        public SecretKey deriveKey(String algorithm,
                AlgorithmParameterSpec params) throws IOException {

            CipherSuite cipherSuite = context.negotiatedCipherSuite;
            ProtocolVersion protocolVersion = context.negotiatedProtocol;

            // What algs/params do we need to use?
            String masterAlg;
            HashAlg hashAlg;

            byte majorVersion = protocolVersion.major;
            byte minorVersion = protocolVersion.minor;
            if (protocolVersion.isDTLS) {
                // Use TLS version number for DTLS key calculation
                if (protocolVersion.id == ProtocolVersion.DTLS10.id) {
                    majorVersion = ProtocolVersion.TLS11.major;
                    minorVersion = ProtocolVersion.TLS11.minor;

                    masterAlg = "SunTlsMasterSecret";
                    hashAlg = H_NONE;
                } else {    // DTLS 1.2
                    majorVersion = ProtocolVersion.TLS12.major;
                    minorVersion = ProtocolVersion.TLS12.minor;

                    masterAlg = "SunTls12MasterSecret";
                    hashAlg = cipherSuite.hashAlg;
                }
            } else {
                if (protocolVersion.id >= ProtocolVersion.TLS12.id) {
                    masterAlg = "SunTls12MasterSecret";
                    hashAlg = cipherSuite.hashAlg;
                } else {
                    masterAlg = "SunTlsMasterSecret";
                    hashAlg = H_NONE;
                }
            }

            TlsMasterSecretParameterSpec spec;
            if (context.handshakeSession.useExtendedMasterSecret) {
                // reset to use the extended master secret algorithm
                masterAlg = "SunTlsExtendedMasterSecret";

                // For the session hash, use the handshake messages up to and
                // including the ClientKeyExchange message.
                context.handshakeHash.utilize();
                byte[] sessionHash = context.handshakeHash.digest();
                spec = new TlsMasterSecretParameterSpec(
                        preMasterSecret,
                        (majorVersion & 0xFF), (minorVersion & 0xFF),
                        // -- Export
                        context.clientHelloRandom.randomBytes,
                        context.serverHelloRandom.randomBytes,
                        // -- Export
                        sessionHash,
                        hashAlg.name, hashAlg.hashLength, hashAlg.blockSize);
            } else {
                spec = new TlsMasterSecretParameterSpec(
                        preMasterSecret,
                        (majorVersion & 0xFF), (minorVersion & 0xFF),
                        context.clientHelloRandom.randomBytes,
                        context.serverHelloRandom.randomBytes,
                        hashAlg.name, hashAlg.hashLength, hashAlg.blockSize);
            }

            try {
                KeyGenerator kg = JsseJce.getKeyGenerator(masterAlg);
                kg.init(spec);
                return kg.generateKey();
            } catch (InvalidAlgorithmParameterException |
                    NoSuchAlgorithmException iae) {
                // unlikely to happen, otherwise, must be a provider exception
                //
                // For RSA premaster secrets, do not signal a protocol error
                // due to the Bleichenbacher attack. See comments further down.
                if (SSLLogger.isOn && SSLLogger.isOn("handshake")) {
                    SSLLogger.fine("RSA master secret generation error.", iae);
                }
                throw new ProviderException(iae);
            }
        }
    }
}
