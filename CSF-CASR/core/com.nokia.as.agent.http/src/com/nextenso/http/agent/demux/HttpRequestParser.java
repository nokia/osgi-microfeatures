package com.nextenso.http.agent.demux;

import java.io.IOException;
import java.io.InputStream;

import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.http.agent.parser.HttpRequestHandler;

public class HttpRequestParser extends HttpParser {
  
  @Override
  protected int readRequestBody(HttpRequestHandler reqHandler, InputStream in) throws IOException {
    if (this.isChunked) {
      int len = in.available();
      this.chunked.setInputStream(in);
      byte[] tmp = new byte[len];
      chunked.read(tmp, 0, len);
      if (chunked.needsInput()) {
        return (READING_BODY);
      }
    }
    else {      
      if (this.clen != -1) {
        if (clen > 0) {
          int len = Math.min(clen, in.available());
          byte[] tmp = new byte[len];
          in.read(tmp, 0, len);
          clen -= len;
        }
        
        if (clen > 0) {
          return (READING_BODY);
        }
      }
    }
    clear(ST_READY);
    return (PARSED);    
  }

}
