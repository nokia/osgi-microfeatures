package com.alcatel.as.http2.hpack;

public interface ByteOutput {
    void put(byte b);
    void put(byte [] bytes);
}
