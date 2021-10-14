package com.nokia.as.diameter.tools.loader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executor;

public class TcpPeerContext extends PeerContext {
  TcpPeerContext(int bulkSize, byte[] acrUserName, int fillbackSize, Executor executor) {
    super(bulkSize, acrUserName, fillbackSize, executor);
  }
  
  @Override
  public boolean flush(SelectionKey sk) throws Exception {
    SocketChannel socket = (SocketChannel) sk.channel();
    PeerContext ctx = (PeerContext) sk.attachment();
    
    // flush pending acr requests
    // flush pending requests
    LinkedList<ByteBuffer> queue = ctx._queue;
    while (queue.size() > 0) {
      ByteBuffer[] bufs = _preallocBuffers;
      int len = queue.size();
      if (len <= bufs.length) {
        bufs = queue.toArray(bufs);
      } else {
        len = 0;
        Iterator<ByteBuffer> it = queue.iterator();
        while (it.hasNext() && len < bufs.length) {
          bufs[len++] = it.next();
        }
      }
      
      long sent = socket.write(bufs, 0, len);
      if (sent < 0) {
        _logger.error("socket write error");
        System.exit(2);
      } else if (sent == 0) {
        // socket full, we'll be called back once we can write again
        return false;
      }
      
      // Check if we have sent all our buffers
      for (int i = 0; i < len; i++) {
        ByteBuffer buf = bufs[i];
        if (!buf.hasRemaining()) {
          queue.removeFirst();
        } else {
          // socket full, we'll be called back once we can write again
          return false;
        }
      }
    }
    
    return true;
  }
}
