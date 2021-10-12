// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;

public class ClosedTcpChannelImpl implements TcpChannel {
  private final int _pri;
  private final Reactor _reactor;
  private final Object _attachment;
  private final boolean _secure;
  private final InetSocketAddress _localAddr;
  private final InetSocketAddress _remoteAddr;

  ClosedTcpChannelImpl(Reactor r, int pri, Object attachment, boolean secure, InetSocketAddress localAddr, InetSocketAddress remoteAddr) {
    _reactor = r;
    _pri = pri;
    _attachment = attachment;
    _secure = secure;
    _localAddr = localAddr;
    _remoteAddr = remoteAddr;
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
    return _localAddr;
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
    throw new IllegalStateException("channel closed");
  }

  @Override
  public void shutdown() {
    throw new IllegalStateException("channel closed");
  }

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
  public InetSocketAddress getRemoteAddress() {
    return _remoteAddr;    
  }

  @Override
  public void flush() {
    throw new IllegalStateException("channel closed");
  }

  @Override
  public boolean isSecure() {
    return _secure;
  }
  
  @Override
  public void upgradeToSecure() {
    throw new IllegalStateException("channel closed");
  }

  @Override
  public Security getSecurity() {
    return null;
  }

  @Override
  public SSLEngine getSSLEngine() {
    return null;
  }
  
  @Override
  public void setSoLinger(long linger) {
  }

  @Override
  public Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length) {
      return Collections.emptyMap();
  }

  @Override
  public List<SNIHostName> getClientRequestedServerNames() {
	  return Collections.emptyList();
  }
  
}
