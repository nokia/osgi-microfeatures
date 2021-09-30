package com.alcatel.as.http2.hpack;

import java.util.ArrayList;
public final class StaticTableEntries {

   public static StaticEntry get(int position) {
      return entries[position-1];
   }

   public static final StaticEntry[] entries = new StaticEntry[] {
      new StaticEntry(1,":authority",""),
      new StaticEntry(2,":method","GET"),
      new StaticEntry(3,":method","POST"),
      new StaticEntry(4,":path","/"),
      new StaticEntry(5,":path","/index.html"),
      new StaticEntry(6,":scheme","http"),
      new StaticEntry(7,":scheme","https"),
      new StaticEntry(8,":status","200"),
      new StaticEntry(9,":status","204"),
      new StaticEntry(10,":status","206"),
      new StaticEntry(11,":status","304"),
      new StaticEntry(12,":status","400"),
      new StaticEntry(13,":status","404"),
      new StaticEntry(14,":status","500"),
      new StaticEntry(15,"accept-charset",""),
      new StaticEntry(16,"accept-encoding","gzip, deflate"),
      new StaticEntry(17,"accept-language",""),
      new StaticEntry(18,"accept-ranges",""),
      new StaticEntry(19,"accept",""),
      new StaticEntry(20,"access-control-allow-origin",""),
      new StaticEntry(21,"age",""),
      new StaticEntry(22,"allow",""),
      new StaticEntry(23,"authorization",""),
      new StaticEntry(24,"cache-control",""),
      new StaticEntry(25,"content-disposition",""),
      new StaticEntry(26,"content-encoding",""),
      new StaticEntry(27,"content-language",""),
      new StaticEntry(28,"content-length",""),
      new StaticEntry(29,"content-location",""),
      new StaticEntry(30,"content-range",""),
      new StaticEntry(31,"content-type",""),
      new StaticEntry(32,"cookie",""),
      new StaticEntry(33,"date",""),
      new StaticEntry(34,"etag",""),
      new StaticEntry(35,"expect",""),
      new StaticEntry(36,"expires",""),
      new StaticEntry(37,"from",""),
      new StaticEntry(38,"host",""),
      new StaticEntry(39,"if-match",""),
      new StaticEntry(40,"if-modified-since",""),
      new StaticEntry(41,"if-none-match",""),
      new StaticEntry(42,"if-range",""),
      new StaticEntry(43,"if-unmodified-since",""),
      new StaticEntry(44,"last-modified",""),
      new StaticEntry(45,"link",""),
      new StaticEntry(46,"location",""),
      new StaticEntry(47,"max-forwards",""),
      new StaticEntry(48,"proxy-authenticate",""),
      new StaticEntry(49,"proxy-authorization",""),
      new StaticEntry(50,"range",""),
      new StaticEntry(51,"referer",""),
      new StaticEntry(52,"refresh",""),
      new StaticEntry(53,"retry-after",""),
      new StaticEntry(54,"server",""),
      new StaticEntry(55,"set-cookie",""),
      new StaticEntry(56,"strict-transport-security",""),
      new StaticEntry(57,"transfer-encoding",""),
      new StaticEntry(58,"user-agent",""),
      new StaticEntry(59,"vary",""),
      new StaticEntry(60,"via",""),
      new StaticEntry(61,"www-authenticate","")

   };
}
