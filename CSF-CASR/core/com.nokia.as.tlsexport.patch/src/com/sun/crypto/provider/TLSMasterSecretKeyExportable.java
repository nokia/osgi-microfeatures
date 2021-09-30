package com.sun.crypto.provider;

import static com.sun.crypto.provider.TlsPrfGenerator.*;

import sun.security.internal.interfaces.TlsMasterSecret;
import sun.security.internal.spec.TlsMasterSecretParameterSpec;

import java.security.DigestException;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("deprecation")
public class TLSMasterSecretKeyExportable implements TlsMasterSecret {

    private static final long serialVersionUID = 0x2be97d32d9e2fef0L;

    private final byte[] key;
    private final int    majorVersion, minorVersion;
    @SuppressWarnings("deprecation")
    private final TlsMasterSecretParameterSpec spec;
    private final boolean rfc7627_5_4;

    @SuppressWarnings("deprecation")
    TLSMasterSecretKeyExportable(byte[] key, int majorVersion, int minorVersion, TlsMasterSecretParameterSpec spec) {
        this(key,majorVersion,minorVersion,spec,true);
    }

    @SuppressWarnings("deprecation")
    TLSMasterSecretKeyExportable(byte[] key, int majorVersion, int minorVersion, TlsMasterSecretParameterSpec spec, boolean rfc7627_5_4) {
        this.rfc7627_5_4=rfc7627_5_4;
        this.key = key;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.spec = spec;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public String getAlgorithm() {
        return "TlsMasterSecret";
    }

    public String getFormat() {
        return "RAW";
    }

    public byte[] getEncoded() {
        return key.clone();
    }

    public static void writeUint16(int i, byte[] buf, int offset)
    {
        buf[offset] = (byte)(i >>> 8);
        buf[offset + 1] = (byte)i;
    }

    byte[] export(String label, byte[] context_value, int length) {
        byte[] clientRandom = spec.getClientRandom();
        byte[] serverRandom = spec.getServerRandom();

        int seedLength = clientRandom.length + serverRandom.length;
        if (context_value != null)
        {
            seedLength += (2 + context_value.length);
        }

        byte[] seed = new byte[seedLength];
        int seedPos = 0;

        System.arraycopy(clientRandom, 0, seed, seedPos, clientRandom.length);
        seedPos += clientRandom.length;
        System.arraycopy(serverRandom, 0, seed, seedPos, serverRandom.length);
        seedPos += serverRandom.length;
        if (context_value != null)
        {
            writeUint16(context_value.length, seed, seedPos);
            seedPos += 2;
            System.arraycopy(context_value, 0, seed, seedPos, context_value.length);
            seedPos += context_value.length;
        }
        if (seedPos != seedLength)
        {
            throw new IllegalStateException("incorrect seed length");
        }

        try {
            return doTLS12PRF(key, label.getBytes(), seed, length,
                    spec.getPRFHashAlg(), spec.getPRFHashLength(),
                    spec.getPRFBlockSize());
        } catch(DigestException de) {
            throw new IllegalStateException("digest exception");
        } catch(NoSuchAlgorithmException nsae) {
            throw new IllegalStateException("incorrect algorithm specification");
        }
        //return TlsUtils.PRF(this, sp.getMasterSecret(), label, seed, length);



    }

}
