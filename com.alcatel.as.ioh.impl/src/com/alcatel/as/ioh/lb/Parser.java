// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;


public interface Parser {

    public Chunk parse (java.nio.ByteBuffer buffer);
    
}
