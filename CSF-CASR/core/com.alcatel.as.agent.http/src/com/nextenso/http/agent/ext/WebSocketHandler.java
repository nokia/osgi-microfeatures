package com.nextenso.http.agent.ext;

import java.nio.ByteBuffer;

import com.nextenso.proxylet.http.HttpRequest;

public interface WebSocketHandler
{

  void onFrame(HttpRequest request, ByteBuffer ... buffers);

}
