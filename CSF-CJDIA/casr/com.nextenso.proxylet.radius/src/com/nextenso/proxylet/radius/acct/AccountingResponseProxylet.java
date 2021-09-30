package com.nextenso.proxylet.radius.acct;

import com.nextenso.proxylet.radius.RadiusProxylet;

/**
 * The interface to implement to handle Accounting Responses.
 */
public interface AccountingResponseProxylet extends RadiusProxylet {
    
    /**
     * Called by the Engine to know how the proxylet will handle the Response.
     * <br/>The possible return codes are: ACCEPT, ACCEPT_MAY_BLOCK, IGNORE.
     * <b>NOTE: This method can be called by the Engine several times in a row for the same response.</b>
     * Therefore it should behave accordingly.
     * @param response The response to handle.
     * @return ACCEPT, ACCEPT_MAY_BLOCK or IGNORE.
     */
    public int accept (AccountingResponse response);
    
    /**
     * Processes the response.
     * <br/>Returns one of the predefined codes to specify what the Engine should do next with the response.
     * @param response The response to process.
     * @return SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET or LAST_PROXYLET.
     */
    public int doResponse (AccountingResponse response);

}
