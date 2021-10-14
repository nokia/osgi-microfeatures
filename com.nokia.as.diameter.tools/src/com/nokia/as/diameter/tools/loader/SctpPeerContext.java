package com.nokia.as.diameter.tools.loader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.concurrent.Executor;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class SctpPeerContext extends PeerContext {
  SctpPeerContext(int bulkSize, byte[] acrUserName, int fillbackSize, Executor executor) {
    super(bulkSize, acrUserName, fillbackSize, executor);
  }
  
  @Override
  public boolean flush(SelectionKey sk) throws Exception {
    SctpChannel socket = (SctpChannel) sk.channel();
    
    // flush pending requests
    LinkedList<ByteBuffer> queue = _queue;
    while (queue.size() > 0) {
      ByteBuffer buf = queue.peek();
      long sent = 0;
      try {
        MessageInfo info = MessageInfo.createOutgoing(null, 0);
        info.complete(true);
        sent = socket.send(buf, info);
      } catch (IOException e) {
        _logger.error("could not write", e);
        System.exit(2);
      }
      if (sent < 0) {
        _logger.error("could not write");
        System.exit(2);
      } else if (buf.hasRemaining()) {
        // socket full, we'll be called back once we can write again
        return false;
      }
      queue.poll();
    }
    return true;
  }
}
