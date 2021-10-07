package com.alcatel.as.util.sctp;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/*
 * #define _K_SS_MAXSIZE	128	// Implementation specific max size
 * #define _K_SS_ALIGNSIZE	(__alignof__ (struct sockaddr *)) // Implementation specific desired alignment
 *
 * typedef unsigned short __kernel_sa_family_t;
 *
 * struct __kernel_sockaddr_storage {
 *	__kernel_sa_family_t	ss_family;		// address family
 *	
 * // Following field(s) are implementation specific
 *	char		__data[_K_SS_MAXSIZE - sizeof(unsigned short)]; // space to achieve desired size,
 *	// _SS_MAXSIZE value minus size of ss_family
 * } __attribute__ ((aligned(_K_SS_ALIGNSIZE)));	// force desired alignment
 */

class sockaddr_storage {
	
	static final short AF_INET = 2;
	static final short AF_INET6 = 10;
	
	static Pointer toJNA(InetSocketAddress addr, Pointer p, long offset) {
		if(addr == null) {
			p.setShort(offset, AF_INET);
			return p;
		}
		
		InetAddress iaddr = addr.getAddress();
		if (iaddr instanceof Inet4Address) { //ipv4
			/* struct sockaddr_in {
	         *      sa_family_t    sin_family; // address family: AF_INET
	         *      in_port_t      sin_port;   // port in network byte order
	         *      struct in_addr sin_addr;   // internet address 
	         *  };
             *
	         *  // Internet address.
	         *  struct in_addr {
	         *      uint32_t       s_addr;     // address in network byte order
	         *  }
	         */
			
			p.setShort(offset, AF_INET);
			p.setShort(offset + 2, htonl((short) addr.getPort()));
			p.setInt(offset + 4, htons(iaddr.getAddress()));
			return p;
			
		} else { //ipv6
			/* struct sockaddr_in6 {
	         *      sa_family_t     sin6_family;   // AF_INET6
	         *      in_port_t       sin6_port;     // port number
	         *      uint32_t        sin6_flowinfo; // IPv6 flow information
	         *      struct in6_addr sin6_addr;     // IPv6 address
	         *      uint32_t        sin6_scope_id; // Scope ID
	         *  };
             *
	         *  struct in6_addr {
	         *      unsigned char   s6_addr[16];   // IPv6 address
	         *  };
			 */
			Inet6Address i6addr = (Inet6Address) iaddr;
			p.setShort(offset, AF_INET6);
			p.setShort(offset + 2, htonl((short) addr.getPort()));
			p.setInt(offset + 4, 0); //not important
			Memory addrMemory = new Memory(16); //16 bytes for ipv6 address
			byte[] orderedAddr = ByteBuffer.allocate(16).put(i6addr.getAddress()).order(ByteOrder.nativeOrder()).array();
			for(int i = 0; i < 16; i++) {
				addrMemory.setByte(i, orderedAddr[i]);
			}
			p.setPointer(offset + 8, addrMemory);
			p.setInt(offset + 24, i6addr.getScopeId());
			return p;
		}
	}
	
	static InetSocketAddress fromJNA(Pointer p, long offset) throws UnknownHostException {
		int addrType = p.getShort(offset);
		if(addrType == AF_INET) { //ipv4
			int port = nltoh(p.getShort(offset + 2));
			byte[] addr = nstoh(p.getInt(offset + 4));
			return new InetSocketAddress(InetAddress.getByAddress(addr), port);
		} else { //ipv6
			int port = nltoh(p.getShort(offset + 2));
			byte[] addr = p.getByteArray(offset + 8, 16);
			return new InetSocketAddress(Inet6Address.getByAddress(addr), port);
		}
	}
	
	private static short htonl(short port) {
		return ByteBuffer.allocate(2).putShort(port)
			    .order(ByteOrder.nativeOrder()).getShort(0);
	}
	
	private static short nltoh(short port) {
		return ByteBuffer.allocate(2).putShort(port)
			    .order(ByteOrder.LITTLE_ENDIAN).getShort(0);
	}
	
	private static int htons(byte[] addr) {
		return ByteBuffer.allocate(4).put(addr)
				.order(ByteOrder.nativeOrder()).getInt(0);
	}
	
	private static byte[] nstoh(int addr) {
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(addr).array();
	}
	
	static int jnaSize() {
		return 128;
	}

}
