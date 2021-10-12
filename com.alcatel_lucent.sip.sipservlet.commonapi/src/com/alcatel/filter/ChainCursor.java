// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.filter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

public interface ChainCursor {
        byte nextCursor();
	void atFinalCursor(ServletRequest r,ServletResponse s) throws ServletException,IOException;
	void onCursorError(Throwable t);
}
