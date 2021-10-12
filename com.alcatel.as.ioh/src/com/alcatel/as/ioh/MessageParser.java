// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh;

import java.util.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import alcatel.tess.hometop.gateways.reactor.*;

public interface MessageParser<T> {

    public T parseMessage (ByteBuffer buffer);

}