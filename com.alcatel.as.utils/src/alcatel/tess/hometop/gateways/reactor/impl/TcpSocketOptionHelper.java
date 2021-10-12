// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.lang.reflect.Field;
import java.nio.channels.SocketChannel;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import alcatel.tess.hometop.gateways.utils.Log;

public class TcpSocketOptionHelper {
    
    private static int IP_TRANSPARENT = 19;
    private static int IPV6_TRANSPARENT = 75;
    
    private interface SocketOption extends Library {
        public void setsockopt(int fd, int level, int option, Pointer value, int len);
    }
    
    private SocketOption helper = Native.loadLibrary("c", SocketOption.class);
    private final Log logger;
    
    public TcpSocketOptionHelper(Log logger) {
        this.logger = logger;
    }
    
    public void setIpTransparent(SocketChannel chan, boolean ipv4) {
        try {
            int fd = getFileDescriptor(chan);
            IntByReference one = new IntByReference(1);
            helper.setsockopt(fd, 0, ipv4 ? IP_TRANSPARENT : IPV6_TRANSPARENT, one.getPointer(), 4);
        } catch(Exception e) {
            logger.warn("Could not set IP_TRANSPARENT option", e);
        }
    }
    
    private int getFileDescriptor(SocketChannel chan) throws NoSuchFieldException, IllegalAccessException {
        Field f = chan.getClass().getDeclaredField("fdVal");
        f.setAccessible(true);
        return f.getInt(chan);       
    }
}
