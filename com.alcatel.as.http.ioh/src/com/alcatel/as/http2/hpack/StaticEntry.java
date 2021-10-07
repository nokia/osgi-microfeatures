package com.alcatel.as.http2.hpack;

public final class StaticEntry {
    public StaticEntry(int position, String header, String value) {
	this.position = position;
	this.header   = header;
	this.value    = value;
    }
    public final String header;
    public final String value;
    public final int    position;
}
