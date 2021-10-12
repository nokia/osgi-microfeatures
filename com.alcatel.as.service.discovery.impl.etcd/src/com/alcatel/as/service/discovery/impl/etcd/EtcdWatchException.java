// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.discovery.impl.etcd;

public class EtcdWatchException extends Exception{
	private static final long serialVersionUID = 1L;

	public EtcdWatchException(Throwable e) {
		super(e);
	}
}
