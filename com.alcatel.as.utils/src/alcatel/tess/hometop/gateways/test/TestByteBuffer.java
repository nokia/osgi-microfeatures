// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;

public class TestByteBuffer {
  
  private static int SERV_PORT = 8765;
  
  public static void main(String args[]) throws Exception {
    Server server = new Server();
    server.start();
    
    Thread.sleep(1000);
    Socket s = new Socket("localhost", SERV_PORT);
    InputStream in = new BufferedInputStream(s.getInputStream());
    
    ByteBuffer buf = new ByteBuffer();
    
    // read all bytes from the socket until eof
    buf.append(in);
    
    System.out.println(buf.toString());
    in.close();
    s.close();
  }
  
  private static class Server extends Thread {
    
    public Server() {
    }
    
    public void run() {
      try {
        ServerSocket ss = new ServerSocket(SERV_PORT);
        Socket s = ss.accept();
        OutputStream out = s.getOutputStream();
        out.write("11111111111111111111111\n".getBytes());
        out.write("22222222222222222222221\n".getBytes());
        out.write("33333333333333333333331\n".getBytes());
        out.write("44444444444444444444441\n".getBytes());
        s.close();
        ss.close();
      }
      
      catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }
}
