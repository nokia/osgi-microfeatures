// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
