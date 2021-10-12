// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.webconnector;

import javax.servlet.http.HttpSession;

public interface HttpSessionAdaptorFactory {

	public HttpSession createHttpSession(WebSession ws);
}
