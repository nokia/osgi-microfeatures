// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;


public interface ConnectionListener {

    public default void opened (Connection connection){}

    public default void failed (Connection connection){}

    public default void updated (Connection connection){}

    public default void closed (Connection connection){}
    
}
