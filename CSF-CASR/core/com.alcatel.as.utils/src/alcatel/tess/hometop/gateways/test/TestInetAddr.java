package alcatel.tess.hometop.gateways.test;

import alcatel.tess.hometop.gateways.utils.InetAddr;
import alcatel.tess.hometop.gateways.utils.Utils;

public class TestInetAddr {
  public static void main(String ... args) throws Exception {
    InetAddr addr1 = new InetAddr(args[0]);
    InetAddr addr2 = new InetAddr(args[1]);
    dump("addr1 ->", addr1);
    dump("\naddr2 ->", addr2);
    System.out.println("addr1.equals(addr2)=" + addr1.equals(addr2));
  }
  
  private static void dump(String msg, InetAddr addr) {
    System.out.println(msg);
    System.out.println("toString()=" + addr.toString());
    byte[] binary = addr.toByteArray();
    if (binary != null) {
      System.out.println("toByteArray()=" + Utils.toString(binary));
    }
    System.out.println("isIPAddress()=" + addr.isIPAddress());
    System.out.println("isIPv4()=" + addr.isIPv4());
    System.out.println("isIPv6()=" + addr.isIPv6());
    System.out.println("isIPAddress()=" + addr.isIPAddress());
    System.out.println("hashCode()=" + addr.hashCode());
  }
}
