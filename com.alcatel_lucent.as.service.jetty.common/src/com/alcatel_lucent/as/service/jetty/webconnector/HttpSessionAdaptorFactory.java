package com.alcatel_lucent.as.service.jetty.webconnector;

import javax.servlet.http.HttpSession;

public interface HttpSessionAdaptorFactory {

	public HttpSession createHttpSession(WebSession ws);
}
