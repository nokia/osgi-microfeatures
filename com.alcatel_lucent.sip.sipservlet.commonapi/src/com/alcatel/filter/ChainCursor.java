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
