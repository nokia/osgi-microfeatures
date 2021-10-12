// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet;

/**
 * The generic Exception thrown by a Proxylet to indicate a problem in its
 * execution.
 */
public class ProxyletException
		extends Exception {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Optional status code for the exception.
	 */
	private volatile int status = -1;

	/**
	 * The main Constructor.
	 */
	public ProxyletException() {
		super();
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param rootCause The root cause.
	 */
	public ProxyletException(Throwable rootCause) {
		super(rootCause.getMessage());
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param debugMessage The message
	 */
	public ProxyletException(String debugMessage) {
		super(debugMessage);
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param debugMessage The message
	 * @param rootCause The root cause.
	 */
	public ProxyletException(String debugMessage, Throwable rootCause) {
		super(debugMessage, rootCause);
	}
	
	/**
	 * Optionally provide a generic status code, which identifies the exception.
	 * @param status a generic status code, which identifies the exception.
	 */
	public void setStatus(int status) {
	  this.status = status;
	}
	
  /**
   * Returns the generic status code which identifies the exception.
   * @param status a generic status code which identifies the exception (-1 by default)
   */
	public int getStatus() {
	  return this.status;
	}
}
