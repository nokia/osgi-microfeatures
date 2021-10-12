// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.nio.ByteBuffer;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

public class TcpChannelListenerSupport {
    private ByteBuffer _buf; // We accumulate if the listener requests for more available data
   
    void handleMessage(TcpChannelListener l, TcpChannelImpl channel, ByteBuffer buf /* read mode */) {
        try {
            if (buf.hasRemaining()) {
                handleChunk(l, channel, buf);
            }
        } finally {
            // All data consumed. Now re-enable read interest.
            channel.enableReadingInternal();
        }
    }
    
    private void handleChunk(TcpChannelListener l, TcpChannel channel, ByteBuffer buf) {
        if (_buf == null) {
            // pass chunk to listener
            int miss = l.messageReceived(channel, buf);
            if (miss > 0) { // listener did not consume the chunk, we have to bufferize
                addToBuffer(buf, miss);
            }
        } else { // we previously bufferized: no choice: we have to append the chunk to the old buffer
            addToBuffer(buf, 0);
            _buf.flip();
            int miss = l.messageReceived(channel, _buf);
            if (miss == 0) { // user consumed the whole buffer
                _buf = null;
            } else { // buffer not fully consumed
            	compact();
            }
        }
    }
    
    void compact() {
    	if (_buf.position() + _buf.arrayOffset() > 0) {
	         // compact the remaining (unread) bytes to to begining of the buffer and reset the buffer in write mode.
	         // Doing so, the next read will append the next chunk to the end of the current incomplete buffer.
    		_buf.compact();
    	} else {
    		_buf.position(_buf.remaining());
    		_buf.limit(_buf.capacity());
    	}
    }

    void close() {
        _buf = null;
    }

    private void addToBuffer(ByteBuffer src, int missing) {
        ensurePut(Math.max(src.remaining(), missing));
        _buf.put(src);
    }

    private void ensurePut(int bytesToAdd) {
        if (_buf == null) {
            _buf = ByteBuffer.allocate(bytesToAdd);
        } else if (_buf.remaining() < bytesToAdd) {
            int n = Math.max(_buf.capacity() << 1, _buf.position() + bytesToAdd);
            ByteBuffer newBuf = ByteBuffer.allocate(n);
            _buf.flip();
            newBuf.put(_buf);
            _buf = newBuf;
        }
    }
}
