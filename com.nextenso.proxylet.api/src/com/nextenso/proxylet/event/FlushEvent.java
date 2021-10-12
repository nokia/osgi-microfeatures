// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.event;

import com.nextenso.proxylet.ProxyletData;

/**
 * An event sent when a message is flushed out.
 */
public class FlushEvent extends ProxyletEvent {

    public FlushEvent (Object src, ProxyletData data){
	super (src, data);
    }

}
