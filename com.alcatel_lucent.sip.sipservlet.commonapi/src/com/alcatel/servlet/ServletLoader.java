// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

public interface ServletLoader {
	Servlet loadServlet(boolean force) throws ServletException;
    Servlet initServlet(boolean force) throws ServletException;
}
