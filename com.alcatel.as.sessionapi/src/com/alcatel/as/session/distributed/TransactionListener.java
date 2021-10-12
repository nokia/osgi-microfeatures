// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed;
import java.io.Serializable;

/**
 * Callback for asynchronous transaction
 * @see SessionManager#execute(Transaction, TransactionListener)
 */
public interface TransactionListener {
  
  /**
   * The transaction is completed (the session has been committed, rollbacked or destroyed) 
   * @param tx the transaction
   * @param result the result returned by {@link Session#commit(Serializable)},  
   *  {@link Session#rollback(Serializable)} or {@link Session#destroy(Serializable)}.<br>
   *  result can be null.
   */
  public void transactionCompleted(Transaction tx, Serializable result);
  
  /**
   * The transaction is failed
   * @param tx the transaction
   * @param exc the cause of the failure
   */
  public void transactionFailed(Transaction tx, SessionException exc);
}
