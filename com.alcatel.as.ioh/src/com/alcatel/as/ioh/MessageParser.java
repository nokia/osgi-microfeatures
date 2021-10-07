package com.alcatel.as.ioh;

import java.util.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import alcatel.tess.hometop.gateways.reactor.*;

public interface MessageParser<T> {

    public T parseMessage (ByteBuffer buffer);

}