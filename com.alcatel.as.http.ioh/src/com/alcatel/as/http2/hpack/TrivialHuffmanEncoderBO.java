// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

public final class TrivialHuffmanEncoderBO extends TrivialHuffmanEncoderAbstract<ByteOutput> {

    @Override
    final protected void write_code(ByteOutput bb, int len, int first) {
        if (index == 0) {
            index = len;
            current = first;
            if (len == 8) {
                index = 0;
                bb.put((byte) current);
                current = 0;
            }
        } else if (index + len < 8) {
            current = current | (first >> index);
            index = index + len;
        } else {
            byte x = (byte) (current | (first >> index));
            current = (first << (8 - index)) & 0xff;
            bb.put(x);
            index = index + len - 8;
        }
    }


}

