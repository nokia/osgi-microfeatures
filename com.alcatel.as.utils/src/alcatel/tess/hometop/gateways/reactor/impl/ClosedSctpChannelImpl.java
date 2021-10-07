package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.Executor;

import com.alcatel.as.util.sctp.SctpSocketOption;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.SctpAssociation;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;

/**
 * A Closed sctp channel, used when notifying a listener about a failed connection request.
 */
public class ClosedSctpChannelImpl implements SctpChannel {
  
  private final int _pri;
  private final Object _attachment;
  private final Reactor _reactor;
  
  public ClosedSctpChannelImpl(Reactor r, int pri, Object attachment) {
    _pri = pri;
    _attachment = attachment;
    _reactor = r;
  }
  
  @Override
  public int getPriority() {
    return _pri;
  }
  
  @Override
  public void setPriority(int priority) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public InetSocketAddress getLocalAddress() {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void setSoTimeout(long soTimeout) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void setSoTimeout(long soTimeout, boolean readOnly) {
    throw new IllegalStateException("channel closed");
  }

  @Override
  public boolean isClosed() {
    return true;
  }
  
  @Override
  public void disableReading() {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void enableReading() {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void setInputExecutor(Executor executor) {
    throw new IllegalStateException("channel closed");
  }
    
  @Override
  public Executor getInputExecutor() {
    throw new IllegalStateException("channel closed");
  }

  @Override
  public void send(ByteBuffer msg, boolean copy) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(ByteBuffer[] msg, boolean copy) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(DataBuffer msg) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(ByteBuffer msg) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(ByteBuffer[] msg) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(byte[] msg) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(byte[] msg, boolean copy) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(byte[] msg, int off, int len) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void send(byte[] msg, int off, int len, boolean copy) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public int getSendBufferSize() {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public void close() {
  }
  
  @Override
  public void shutdown() {
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public <T> T attachment() {
    return (T) _attachment;
  }
  
  @Override
  public void attach(Object attached) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public Reactor getReactor() {
    return _reactor;
  }
  
  @Override
  public Set<SocketAddress> getLocalAddresses() throws IOException {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public Set<SocketAddress> getRemoteAddresses() throws IOException {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public int getRemotePort() {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public SctpAssociation getAssociation() throws IOException {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public SctpChannel unbindAddress(InetAddress address) throws IOException {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public SctpChannel send(boolean copy, SocketAddress addr, int streamNumber, ByteBuffer ... data) {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public SctpChannel send(boolean copy, SocketAddress addr, boolean complete, int ploadPID, int streamNumber,
                          long timeToLive, boolean unordered, ByteBuffer ... data) {
    throw new IllegalStateException("channel closed");
  }

  @Override
  public boolean isSecure() {
	  return false;
  }
  
  @Override
  public Object getSocketOption(SctpSocketOption option, Object extra) throws IOException {
    throw new IllegalStateException("channel closed");
  }
  
  @Override
  public SctpChannel setSocketOption(SctpSocketOption option, Object param) throws IOException {
	  throw new IllegalStateException("channel closed");
  }

  @Override
  public void setSoLinger(long linger) {
  }
}
