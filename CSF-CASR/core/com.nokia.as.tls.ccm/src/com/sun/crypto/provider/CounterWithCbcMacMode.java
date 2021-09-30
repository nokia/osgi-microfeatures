/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.crypto.provider;

import java.io.*;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import static com.sun.crypto.provider.AESConstants.AES_BLOCK_SIZE;

/**
 * This class represents ciphers in Counter with CBC MAC (CCM) mode.
 *
 * <p>This mode currently should only be used w/ AES cipher.
 * Although no checking is done, caller should only pass AES
 * Cipher to the constructor.
 *
 * <p>NOTE: Unlike other modes, when used for decryption, this class
 * will buffer all processed outputs internally and won't return them
 * until the tag has been successfully verified.
 *
 * @since 1.8
 */
final class CounterWithCbcMacMode extends FeedbackCipher {

    static int DEFAULT_TAG_LEN = AES_BLOCK_SIZE;
    static int DEFAULT_IV_LEN = 12; // in bytes


    private static final int MAX_BUF_SIZE = Integer.MAX_VALUE;

    // buffer for AAD data; if null, meaning update has been called
    private ByteArrayOutputStream aadBuffer = new ByteArrayOutputStream();
    private int sizeOfAAD = 0;
    private byte[] aad = null;

    // buffer for storing input in decryption, not used for encryption
    private ByteArrayOutputStream ibuffer = null;

    // in bytes; need to convert to bits (default value 128) when needed
    private int tagLenBytes = DEFAULT_TAG_LEN;

    // length of total data, i.e. len(C)
    private int processed = 0;

    // additional variables for save/restore calls
    private byte[] aadBufferSave = null;
    private int sizeOfAADSave = 0;
    private byte[] ibufferSave = null;
    private int processedSave = 0;
    private CcmBuildingBlocks ccm;


    private static void checkDataLength(int processed, int len) {
        if (processed > MAX_BUF_SIZE - len) {
            throw new ProviderException("SunJCE provider only supports " +
                "input size up to " + MAX_BUF_SIZE + " bytes");
        }
    }

    CounterWithCbcMacMode(SymmetricCipher embeddedCipher) {
        super(embeddedCipher);
        aadBuffer = new ByteArrayOutputStream();
    }

    /**
     * Gets the name of the feedback mechanism
     *
     * @return the name of the feedback mechanism
     */
    String getFeedback() {
        return "CCM";
    }

    /**
     * Resets the cipher object to its original state.
     * This is used when doFinal is called in the Cipher class, so that the
     * cipher can be reused (with its original key and iv).
     */
    void reset() {
        if (aadBuffer == null) {
            aadBuffer = new ByteArrayOutputStream();
        } else {
            aadBuffer.reset();
        }
        aad=null;
        ccm.stream_init(iv);
//        if (gctrPAndC != null) gctrPAndC.reset();
//        if (ghashAllToS != null) ghashAllToS.reset();
        processed = 0;
        sizeOfAAD = 0;
        if (ibuffer != null) {
            ibuffer.reset();
        }
    }

    /**
     * Save the current content of this cipher.
     */
    void save() {
        processedSave = processed;
        sizeOfAADSave = sizeOfAAD;
        aadBufferSave =
            ((aadBuffer == null || aadBuffer.size() == 0)?
             null : aadBuffer.toByteArray());
//        if (gctrPAndC != null) gctrPAndC.save();
//        if (ghashAllToS != null) ghashAllToS.save();
        if (ibuffer != null) {
            ibufferSave = ibuffer.toByteArray();
        }
        throw new UnsupportedOperationException("not implemented yet!");
    }

    /**
     * Restores the content of this cipher to the previous saved one.
     */
    void restore() {
        processed = processedSave;
        sizeOfAAD = sizeOfAADSave;
        if (aadBuffer != null) {
            aadBuffer.reset();
            if (aadBufferSave != null) {
                aadBuffer.write(aadBufferSave, 0, aadBufferSave.length);
            }
        }
//        if (gctrPAndC != null) gctrPAndC.restore();
//        if (ghashAllToS != null) ghashAllToS.restore();
        if (ibuffer != null) {
            ibuffer.reset();
            ibuffer.write(ibufferSave, 0, ibufferSave.length);
        }
        throw new UnsupportedOperationException("not implemented yet!");
    }

    /**
     * Initializes the cipher in the specified mode with the given key
     * and iv.
     *
     * @param decrypting flag indicating encryption or decryption
     * @param algorithm the algorithm name
     * @param key the key
     * @param iv the iv
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     */
    @Override
    void init(boolean decrypting, String algorithm, byte[] key, byte[] iv)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(decrypting, algorithm, key, iv, DEFAULT_TAG_LEN);
    }

    /**
     * Initializes the cipher in the specified mode with the given key
     * and iv.
     *
     * @param decrypting flag indicating encryption or decryption
     * @param algorithm the algorithm name
     * @param keyValue the key
     * @param ivValue the iv
     * @param tagLenBytes the length of tag in bytes
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     */
    void init(boolean decrypting, String algorithm, byte[] keyValue,
              byte[] ivValue, int tagLenBytes)
              throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (keyValue == null) {
            throw new InvalidKeyException("Internal error");
        }
        if (ivValue == null) {
            throw new InvalidAlgorithmParameterException("Internal error");
        }
        if (ivValue.length != 12) {
            throw new InvalidAlgorithmParameterException("IV must be 12 bytes long");
        }

        // always encrypt mode for embedded cipher
        this.embeddedCipher.init(false, algorithm, keyValue);

        this.iv = ivValue.clone();

        this.tagLenBytes = tagLenBytes;
        if (aadBuffer == null) {
            aadBuffer = new ByteArrayOutputStream();
        } else {
            aadBuffer.reset();
        }
        processed = 0;
        sizeOfAAD = 0;
        if (ibuffer == null) {
            ibuffer = new ByteArrayOutputStream();
        } else {
            ibuffer.reset();
        }

        ccm=new CcmBuildingBlocks(iv);
    }

    /**
     * Continues a multi-part update of the Additional Authentication
     * Data (AAD), using a subset of the provided buffer. If this
     * cipher is operating in either GCM or CCM mode, all AAD must be
     * supplied before beginning operations on the ciphertext (via the
     * {@code update} and {@code doFinal} methods).
     * <p>
     * NOTE: Given most modes do not accept AAD, default impl for this
     * method throws IllegalStateException.
     *
     * @param src the buffer containing the AAD
     * @param offset the offset in {@code src} where the AAD input starts
     * @param len the number of AAD bytes
     *
     * @throws IllegalStateException if this cipher is in a wrong state
     * (e.g., has not been initialized), does not accept AAD, or if
     * operating in either GCM or CCM mode and one of the {@code update}
     * methods has already been called for the active
     * encryption/decryption operation
     * @throws UnsupportedOperationException if this method
     * has not been overridden by an implementation
     *
     * @since 1.8
     */
    void updateAAD(byte[] src, int offset, int len) {
        if (aadBuffer != null) {
            aadBuffer.write(src, offset, len);
        } else {
            // update has already been called
            throw new IllegalStateException
                ("Update has been called; no more AAD data");
        }
    }

    void processAAD() {
        if (aadBuffer != null) {
            if (aadBuffer.size() > 0) {
                aad = aadBuffer.toByteArray();
                sizeOfAAD = aad.length;
            }
            aadBuffer = null;
            ccm.stream_init(iv);
        }
    }

    /**
     * Performs encryption operation.
     *
     * <p>The input plain text <code>in</code>, starting at <code>inOff</code>
     * and ending at <code>(inOff + len - 1)</code>, is encrypted. The result
     * is stored in <code>out</code>, starting at <code>outOfs</code>.
     *
     * @param in the buffer with the input data to be encrypted
     * @param inOfs the offset in <code>in</code>
     * @param len the length of the input data
     * @param out the buffer for the result
     * @param outOfs the offset in <code>out</code>
     * @exception ProviderException if <code>len</code> is not
     * a multiple of the block size
     * @return the number of bytes placed into the <code>out</code> buffer
     */
    int encrypt(byte[] in, int inOfs, int len, byte[] out, int outOfs) {
        if ((len % blockSize) != 0) {
             throw new ProviderException("Internal error in input buffering");
        }

        checkDataLength(processed, len);

        processAAD();
        if (len > 0) {
            ibuffer.write(in, inOfs, len);
            ccm.stream_cipher(in, inOfs, len, out, outOfs);
            processed += len;
        }
        return len;
    }

    /**
     * Performs encryption operation for the last time.
     *
     * @param in the input buffer with the data to be encrypted
     * @param inOfs the offset in <code>in</code>
     * @param len the length of the input data
     * @param out the buffer for the encryption result
     * @param outOfs the offset in <code>out</code>
     * @return the number of bytes placed into the <code>out</code> buffer
     */
    int encryptFinal(byte[] in, int inOfs, int len, byte[] out, int outOfs)
        throws IllegalBlockSizeException, ShortBufferException {
        if (len > MAX_BUF_SIZE - tagLenBytes) {
            throw new ShortBufferException
                ("Can't fit both data and tag into one buffer");
        }
        if (out.length - outOfs < (len + tagLenBytes)) {
            throw new ShortBufferException("Output buffer too small");
        }

        checkDataLength(processed, len);

        processAAD();
        if (len > 0) {
            ibuffer.write(in, inOfs, len);
            ccm.stream_cipher(in, inOfs, len,out, outOfs);
        }

        // refresh 'in' to all buffered-up bytes
        in = ibuffer.toByteArray();
        inOfs = 0;
        len = in.length;
        ibuffer.reset();

        byte[] sOut = new byte[tagLenBytes];
        ccm.format(iv, aad, 0, sizeOfAAD, in, inOfs, len, tagLenBytes, sOut);

        ccm.cypher_decypher_mac(sOut, 0, tagLenBytes, out, (outOfs + len));
        ccm.stream_reset();
        return (len + tagLenBytes);
    }

    /**
     * Performs decryption operation.
     *
     * <p>The input cipher text <code>in</code>, starting at
     * <code>inOfs</code> and ending at <code>(inOfs + len - 1)</code>,
     * is decrypted. The result is stored in <code>out</code>, starting at
     * <code>outOfs</code>.
     *
     * @param in the buffer with the input data to be decrypted
     * @param inOfs the offset in <code>in</code>
     * @param len the length of the input data
     * @param out the buffer for the result
     * @param outOfs the offset in <code>out</code>
     * @exception ProviderException if <code>len</code> is not
     * a multiple of the block size
     * @return the number of bytes placed into the <code>out</code> buffer
     */
    int decrypt(byte[] in, int inOfs, int len, byte[] out, int outOfs) {
        if ((len % blockSize) != 0) {
             throw new ProviderException("Internal error in input buffering");
        }

        checkDataLength(ibuffer.size(), len);

        processAAD();

        if (len > 0) {
            byte [] clear = new byte[len];
            ccm.decipher_block(in,inOfs,len, clear, 0);
            ibuffer.write(clear, 0, len);
        }
        return 0;
    }

    /**
     * Performs decryption operation for the last time.
     *
     * <p>NOTE: For cipher feedback modes which does not perform
     * special handling for the last few blocks, this is essentially
     * the same as <code>encrypt(...)</code>. Given most modes do
     * not do special handling, the default impl for this method is
     * to simply call <code>decrypt(...)</code>.
     *
     * @param in the input buffer with the data to be decrypted
     * @param inOfs the offset in <code>cipher</code>
     * @param len the length of the input data
     * @param out the buffer for the decryption result
     * @param outOfs the offset in <code>plain</code>
     * @return the number of bytes placed into the <code>out</code> buffer
     */
    int decryptFinal(byte[] in, int inOfs, int len,
                     byte[] out, int outOfs)
        throws IllegalBlockSizeException, AEADBadTagException,
        ShortBufferException {
        if (len < tagLenBytes) {
            throw new AEADBadTagException("Input too short - need tag");
        }
        // do this check here can also catch the potential integer overflow
        // scenario for the subsequent output buffer capacity check.
        checkDataLength(ibuffer.size(), (len - tagLenBytes));

        if (out.length - outOfs < ((ibuffer.size() + len) - tagLenBytes)) {
            throw new ShortBufferException("Output buffer too small");
        }

        processAAD();

        if (len > tagLenBytes) {
            byte [] clear = new byte[len];
            ccm.decipher_block(in,inOfs,len-tagLenBytes, clear, 0);
            ibuffer.write(clear, 0, len-tagLenBytes);
        }

        // get the trailing tag bytes from 'in'
        byte[] tag = new byte[tagLenBytes];
//        System.arraycopy(in, inOfs + len - tagLenBytes, tag, 0, tagLenBytes);
        ccm.cypher_decypher_mac(in, inOfs + len - tagLenBytes, tagLenBytes,tag, 0);
//        len -= tagLenBytes;

        // refresh 'in' to all buffered-up bytes
        in = ibuffer.toByteArray();
        inOfs = 0;
        len = in.length;
        ibuffer.reset();

        if (len > 0) {
//            doLastBlock(in, inOfs, len, out, outOfs, false);
            System.arraycopy(in,inOfs,out, outOfs,len);
        }

        byte[] sOut = new byte[tagLenBytes];

        ccm.format(iv, aad, 0, sizeOfAAD, in, inOfs, len, tagLenBytes, sOut);

        aad=null;

        // check entire authentication tag for time-consistency
        int mismatch = 0;
        for (int i = 0; i < tagLenBytes; i++) {
            mismatch |= tag[i] ^ sOut[i];
        }

        if (mismatch != 0) {
            throw new AEADBadTagException("Tag mismatch!");
        }

        return len;
    }

    // return tag length in bytes
    int getTagLen() {
        return this.tagLenBytes;
    }

    @Override
    int getBufferedLength() {
        if (ibuffer == null) {
            return 0;
        } else {
            return ibuffer.size();
        }
    }

    class CcmBuildingBlocks {

        // Specified to work with AES only
        static final int BLOCK_SIZE = 16;

        CcmBuildingBlocks(byte [] n) {
            stream_init(n);
        }

        final class Counter {
            byte [] counter = new byte[BLOCK_SIZE];


            void init() {
                assert(counter[0] == 0x02);
                counter[13] = 0;
                counter[14] = 0;
                counter[15] = 0;
            }
            void init(byte [] n) {
                System.arraycopy(n,0, counter,1, n.length);

                counter[0] = 0x02;
                counter[13] = 0;
                counter[14] = 0;
                counter[15] = 0;
            }

            byte [] tmp_out=new byte [BLOCK_SIZE];

            void cipher_block_byte_aligned(byte [] in, int in_offset, int in_qty, byte [] out, int out_offset) {

                int block=0;
                for(; in_qty-block >= BLOCK_SIZE ; block+=BLOCK_SIZE)
                    cipher(in,in_offset + block, out, out_offset+block);

                if (block < in_qty) {
                    cipher(in,in_offset + block , out, out_offset+block, in_qty-block);
                }
            }

            void cipher(byte [] in, int in_offset,  byte out[], int out_offset ) {
                cipher(in, in_offset, out, out_offset, BLOCK_SIZE);
            }

            void cipher(byte [] in, int in_offset,  byte out[], int out_offset , int length ) {
                aes(counter, tmp_out);
                for(int i=0;i<length;i++)
                    out[i+out_offset]=(byte)( (in[in_offset+i]^tmp_out[i]) & 0xff) ;
                inc();
            }

            private void inc() {
                if (++counter[15] ==0) {
                    if (++counter[14] == 0 )
                        ++counter[13];
                }
            }
        }

        void cipher(
                int q,
                byte [] n,
                byte [] a, int a_offset, int a_qty,
                byte [] p, int p_offset, int p_qty,
                byte [] out, int out_offset
        ) {
            stream_init(n);
            stream_cipher(p,p_offset,p_qty,out,out_offset);
            stream_cipher_last(q,n,a,a_offset,a_qty,p,p_offset,p_qty,out,out_offset);
            stream_reset();
        }

        private Counter stream_counter = null;
        void stream_reset() {
            stream_counter =null;
        }
        void stream_init(
                byte [] n
        ) {
            assert(stream_counter == null);
            stream_counter = new Counter();
            stream_counter.init(n);
            stream_counter.inc();
        }

        void stream_cipher(
                byte [] p, int p_offset, int p_qty,
                byte [] out, int out_offset
        ) {
            assert(stream_counter != null);
            stream_counter.cipher_block_byte_aligned(p,p_offset, p_qty, out, out_offset);
        }

        void stream_cipher_last(
                int q,
                byte [] n,
                byte [] a, int a_offset, int a_qty,
                byte [] p, int p_offset, int p_qty,
                byte [] out, int out_offset
        ) {
            assert(stream_counter != null);
            byte [] mac = new byte[BLOCK_SIZE];
            format(n,a,a_offset,a_qty,p,p_offset,p_qty,q,mac);

            stream_counter.init(n);
            stream_counter.cipher(mac,0,out,out_offset+p_qty, q);
            stream_counter = null;
        }

        void decipher_block(
                byte [] c, int c_offset, int c_qty,
                byte [] out, int out_offset
        ) {
            stream_counter.cipher_block_byte_aligned(c,c_offset, c_qty, out, out_offset);
        }

        void cypher_decypher_mac(
                byte [] c, int c_offset, int c_qty,
                byte [] out, int out_offset
        ) {
            stream_counter.init();
            stream_counter.cipher(c,c_offset, out, out_offset, c_qty);
        }

        void format(
                byte [] n,
                byte [] a, int a_offset, int a_qty,
                byte [] p, int p_offset, int p_qty,
                int q,
                byte [] mac
        ) {
            assert (n != null);
            assert (a != null);
            assert (n.length == 12);
            assert ( ( a!= null && ( a.length >= (a_qty+a_offset)) ) || (a==null && a_qty ==0) );
            assert (p.length >= (p_qty+p_offset) );

            byte [] b0 = new byte[BLOCK_SIZE];

            b0[0]=(byte) ( ((q-2)<<2) | 0x02 | ( a_qty==0 ? 0x00 : 0x40 ));
            System.arraycopy(n,0, b0, 1, n.length);
            b0[13] = (byte)((p_qty >> 16) & 0xff);
            b0[14] = (byte)((p_qty >> 8 ) & 0xff);
            b0[15] = (byte)((p_qty      ) & 0xff);

            cbc_mac(b0);

            if (a_qty>0) {
                if (a_qty <= 14) {
                    Arrays.fill(b0, (byte)0);
                    //b0[0] = 0x00;
                    b0[1] = (byte) a_qty;
                    System.arraycopy(a,a_offset,b0,2,a_qty);
                    cbc_mac(b0);
                } else if ( a_qty < (0x10000-0x100) ) {

                    int offset = 2;
                    b0[0] = (byte) ((a_qty >> 8) & 0xff);
                    b0[1] = (byte) ( a_qty & 0xff);
                    System.arraycopy(a,a_offset,b0,offset,(BLOCK_SIZE-offset));
                    cbc_mac(b0);

                    cbc_mac_pad_zero(a,a_offset+(BLOCK_SIZE-offset), a_qty- (BLOCK_SIZE-offset)) ;
                } else {
                    int offset = 6;
                    b0[0] = (byte) (0xff);
                    b0[1] = (byte) (0xfe);
                    b0[2] = (byte) ((a_qty >> 24) & 0xff);
                    b0[3] = (byte) ((a_qty >> 16) & 0xff);
                    b0[4] = (byte) ((a_qty >> 8)  & 0xff);
                    b0[5] = (byte) ( a_qty        & 0xff);
                    System.arraycopy(a,a_offset,b0,offset,(BLOCK_SIZE-offset));
                    cbc_mac(b0);

                    cbc_mac_pad_zero(a,a_offset+(BLOCK_SIZE-offset), a_qty- (BLOCK_SIZE-offset)) ;
                }
            }

            cbc_mac_pad_zero(p,p_offset,p_qty);

            System.arraycopy(cbc_fbo, 0, mac, 0, q);

        }

        void cbc_mac_pad_zero(byte [] in, int offset, int qty) {

            int block=0;
            for(; qty-block >= BLOCK_SIZE ; block+=BLOCK_SIZE)
                cbc_mac(in,offset + block);

            if (block < qty) {
                byte[] b = new byte[BLOCK_SIZE];
                System.arraycopy(in,block+offset,b,0, qty-(block));
                cbc_mac(b,0);
            }
        }

        void cbc_mac(byte [] in) {
            cbc_mac(in,0 );
        }

        private byte [] cbc_fbi = new byte[BLOCK_SIZE];
        private byte [] cbc_fbo = new byte[BLOCK_SIZE];

        void cbc_mac(byte [] in, int offset ) {
            for(int i=0;i<BLOCK_SIZE;i++)
                cbc_fbi[i]=(byte)( (in[offset+i]^cbc_fbo[i]) & 0xff) ;
            aes(cbc_fbi,cbc_fbo);
        }

        void aes(byte []in, byte [] out) {
            embeddedCipher.encryptBlock(
                    in, 0,
                    out, 0
            );
        }

    }

}
