// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.scenarii.impl;

import com.alcatel.as.http.parser.HttpMessageImpl;

public class HttpResponse {

	private HttpMessageImpl message;
	private long duration;

	public HttpResponse(HttpMessageImpl message, long duration) {
		super();
		this.message = message;
		this.duration = duration;
	}

	public HttpMessageImpl getMessage() {
		return message;
	}

	public long getDuration() {
		return duration;
	}

}
