// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class UnsignedInteger {

    public static final long UNDERFLOW = 0xDEF0DEF0DEF0DEF0L;
    public static final long ERROR     = 0x7077370773707733L;

    public static void encode(int n, long value, byte mask, ByteBuffer bb) {
        long talon = (1 << n) - 1;

        if (value < talon) {
            bb.put((byte) (mask | (value & talon)));
        } else {
            bb.put((byte) (mask | talon));
            value = value - talon;
            while (value >= 128) {
                bb.put((byte) ((0x7F & value) | 0x80));
                value = value >> 7;
            }
            bb.put((byte) value);
        }
    }

    public static void encode(int n, long value, byte mask, ByteOutput byte_output) {
        long talon = (1 << n) - 1;

        if (value < talon) {
            byte_output.put((byte) (mask | (value & talon)));
        } else {
            byte_output.put((byte) (mask | talon));
            value = value - talon;
            while (value >= 128) {
                byte_output.put((byte) ((0x7F & value) | 0x80));
                value = value >> 7;
            }
            byte_output.put((byte) value);
        }
    }

    public static long decode(int n, ByteBuffer bb) {
        if (bb.hasRemaining())
            return decode(n, bb.get(), bb);
        else
            return UNDERFLOW;
    }

    public static long decode(int n, byte first, ByteBuffer bb) {
        final long talon;

        switch (n) {
            case 4:
                talon = 0xFL;
                break;
            case 5:
                talon = 0x1FL;
                break;
            case 6:
                talon = 0x3FL;
                break;
            case 7:
                talon = 0x7FL;
                break;
            default:
                // N is hard-coded in the parser, so this exception is more intended for a developer
                throw new IllegalArgumentException("n=" + n + "which does not belong to the RFC.");
        }

        long value = (first & talon);
        if (value == talon) {
            if (!bb.hasRemaining())
                return UNDERFLOW;
            byte b = bb.get();
            value = (b & 0x7F);
            int m = 7;
            while ((b & 0x80) == 0x80) {
                if (!bb.hasRemaining())
                    return UNDERFLOW;
                b = bb.get();
                value = value | ((b & 0x7F) << m);
                m = m + 7;
                if (m>=35)
                    return ERROR;
            }
            return value + talon;
        } else {
            return value;
        }
    }

}
