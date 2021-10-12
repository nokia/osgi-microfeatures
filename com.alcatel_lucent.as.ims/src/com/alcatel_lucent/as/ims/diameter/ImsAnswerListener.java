// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import java.io.IOException;

/**
 * The Ims Answer Listener.
 */
public interface  ImsAnswerListener<R extends ImsRequest, A extends ImsAnswer> {
	/**
	 * Called when en error occurs when sending a request.
	 * 
 * @param request The request.
	 * @param ioe The exception.
	 */
	public void handleException(R request, IOException ioe);

	/**
	 * Called when an answer is received from the HSS.
	 * 
	 * @param request The request.
	 * @param answer The answer.
	 */
	public void handleAnswer(R request, A answer);

}
