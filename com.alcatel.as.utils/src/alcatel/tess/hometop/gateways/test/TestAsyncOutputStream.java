package alcatel.tess.hometop.gateways.test;

// Jdk
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import alcatel.tess.hometop.gateways.concurrent.AsyncOutputStream;
import alcatel.tess.hometop.gateways.utils.ByteBuffer;

public class TestAsyncOutputStream {
  private static int TESTSIZE = 2048;
  
  public static void main(String args[]) throws Exception {
    TESTSIZE = Integer.parseInt(args[args.length - 2]);
    if (args[args.length - 1].equals("async"))
      testAsync(args);
    else
      testBuffered(args);
  }
  
  private static void testBuffered(String[] args) throws Exception {
    InputStream in = new FileInputStream(args[0]);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(args[1]), 8192);
    
    ByteBuffer bb = new ByteBuffer();
    int n;
    byte[] buf = new byte[TESTSIZE];
    while ((n = in.read(buf)) != -1) {
      bb.append(buf, 0, n);
    }
    
    ByteArrayInputStream bin = new ByteArrayInputStream(bb.toByteArray(true));
    long start = System.currentTimeMillis();
    
    while (true) {
      if ((n = bin.read(buf)) == -1) {
        break;
      }
      out.write(buf, 0, n);
      out.flush();
    }
    
    in.close();
    out.close();
    long end = System.currentTimeMillis();
    System.err.println("BufferedOutputStream time: " + (end - start));
  }
  
  private static void testAsync(String args[]) throws Exception {
    FileInputStream in = new FileInputStream(args[0]);
    AsyncOutputStream aout = new AsyncOutputStream(new FileOutputStream(args[1]), 1024 * 100);
    Thread.sleep(1000);
    
    ByteBuffer bb = new ByteBuffer();
    int n;
    byte[] buf = new byte[TESTSIZE];
    while ((n = in.read(buf)) != -1) {
      bb.append(buf, 0, n);
    }
    
    ByteArrayInputStream bin = new ByteArrayInputStream(bb.toByteArray(true));
    long start = System.currentTimeMillis();
    
    while (true) {
      if ((n = bin.read(buf)) == -1) {
        break;
      }
      aout.write(buf, 0, n);
    }
    
    in.close();
    aout.close();
    long end = System.currentTimeMillis();
    System.err.println("AsyncOutputStream time: " + (end - start));
  }
}
