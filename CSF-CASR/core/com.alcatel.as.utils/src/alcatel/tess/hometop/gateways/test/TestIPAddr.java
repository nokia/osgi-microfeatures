package alcatel.tess.hometop.gateways.test;

import alcatel.tess.hometop.gateways.utils.IPAddr;
import alcatel.tess.hometop.gateways.utils.Utils;

public class TestIPAddr {
  public static void main(String ... args) throws Exception {
    System.out.println("isIPAddress(" + args[0] + ")=" + IPAddr.isIPAddress(args[0]));
    System.out.println("isIPAddress(" + args[1] + ")=" + IPAddr.isIPAddress(args[1]));
    
    IPAddr addr1 = new IPAddr(args[0]);
    IPAddr addr2 = new IPAddr(args[1]);
    
    dump("addr1 ->", addr1);
    dump("\naddr2 ->", addr2);
    System.out.println("addr1.equals(addr2)=" + addr1.equals(addr2));
  }
  
  private static void dump(String msg, IPAddr addr) {
    System.out.println(msg);
    System.out.println("toString()=" + addr.toString());
    byte[] binary = addr.toByteArray();
    if (binary != null) {
      System.out.println("toByteArray()=" + Utils.toString(binary));
    }
    System.out.println("isIPv4()=" + addr.isIPv4());
    System.out.println("isIPv6()=" + addr.isIPv6());
    System.out.println("hashCode()=" + addr.hashCode());
  }
}
