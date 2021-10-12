// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.sctp_assoc_value;
import com.alcatel.as.util.sctp.sctp_assocparams;
import com.alcatel.as.util.sctp.sctp_authchunk;
import com.alcatel.as.util.sctp.sctp_authchunks;
import com.alcatel.as.util.sctp.sctp_authkey;
import com.alcatel.as.util.sctp.sctp_authkeyid;
import com.alcatel.as.util.sctp.sctp_boolean;
import com.alcatel.as.util.sctp.sctp_event_subscribe;
import com.alcatel.as.util.sctp.sctp_hmacalgo;
import com.alcatel.as.util.sctp.sctp_initmsg;
import com.alcatel.as.util.sctp.sctp_paddrinfo;
import com.alcatel.as.util.sctp.sctp_paddrparams;
import com.alcatel.as.util.sctp.sctp_rtoinfo;
import com.alcatel.as.util.sctp.sctp_sack_info;
import com.alcatel.as.util.sctp.sctp_setadaptation;
import com.alcatel.as.util.sctp.sctp_sndrcvinfo;
import com.alcatel.as.util.sctp.sctp_status;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpStandardSocketOptions;

public class SctpSocketOptionHelper {

	private static int SOL_SOCKET = 1;
	private static int SO_REUSEADDR = 2;
	
	private static int IPPROTO_SCTP = 132;
	private static int SCTP_RTOINFO = 0;
	private static int SCTP_ASSOCINFO = 1;
	private static int SCTP_INITMSG = 2;
	private static int SCTP_ADAPTATION_LAYER = 7;
	private static int SCTP_PEER_ADDR_PARAMS = 9;
	private static int SCTP_DEFAULT_SEND_PARAM = 10;
	private static int SCTP_EVENTS = 11;
	private static int SCTP_I_WANT_MAPPED_V4_ADDR = 12;
	private static int SCTP_MAXSEG = 13;
	private static int SCTP_STATUS = 14;
	private static int SCTP_GET_PEER_ADDR_INFO = 15;
	private static int SCTP_DELAYED_SACK = 16;
	private static int SCTP_CONTEXT = 17;
	private static int SCTP_FRAGMENT_INTERLEAVE = 18;
	private static int SCTP_PARTIAL_DELIVERY_POINT = 19;
	private static int SCTP_MAX_BURST = 20;
	private static int SCTP_AUTH_CHUNK = 21;
	private static int SCTP_HMAC_IDENT = 22;
	private static int SCTP_AUTH_KEY = 23;
	private static int SCTP_AUTH_ACTIVE_KEY = 24;
	private static int SCTP_AUTH_DELETE_KEY = 25;
	private static int SCTP_PEER_AUTH_CHUNKS = 26;
	private static int SCTP_LOCAL_AUTH_CHUNKS = 27;

	private interface SocketOption extends Library {
		public void getsockopt(int fd, int level, int option, Pointer value, Pointer len);
		public void setsockopt(int fd, int level, int option, Pointer value, int len);
	}

	private SocketOption helper = Native.loadLibrary("c", SocketOption.class);
	private final Logger logger = Logger.getLogger(SctpSocketOptionHelper.class);

	public Object getOption(SctpChannel chan, SctpSocketOption option, Object extra) throws IOException {
		try {
			int fd = getFileDescriptor(chan);
			int assocID = getAssocId(chan);

			switch(option) {
			case SCTP_DISABLEFRAGMENTS:
				return new sctp_boolean(chan.getOption(SctpStandardSocketOptions.SCTP_DISABLE_FRAGMENTS));
			case SCTP_NODELAY:
				return new sctp_boolean(chan.getOption(SctpStandardSocketOptions.SCTP_NODELAY));
			case SCTP_PRIMARY_ADDR:
				return chan.getOption(SctpStandardSocketOptions.SCTP_PRIMARY_ADDR);
			default:
				return getOption(fd, assocID, option, extra);
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	public Object getOption(SctpServerChannel chan, SctpSocketOption option, Object extra) throws IOException {
		try {
			int fd = getFileDescriptor(chan);
			int assocID = 0;
			switch(option) {
			case SCTP_DISABLEFRAGMENTS:
				return new sctp_boolean(chan.getOption(SctpStandardSocketOptions.SCTP_DISABLE_FRAGMENTS));
			case SCTP_NODELAY:
				return new sctp_boolean(chan.getOption(SctpStandardSocketOptions.SCTP_NODELAY));
			case SCTP_PRIMARY_ADDR:
				return chan.getOption(SctpStandardSocketOptions.SCTP_PRIMARY_ADDR);
			default:
				return getOption(fd, assocID, option, extra);
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	public Object getOption(int fd, int assocID, SctpSocketOption option, Object extra) throws IOException {
		switch(option) {
		case SCTP_ADAPTATION_LAYER:
			return getSCTP_ADAPTATION_LAYER(fd);
		case SCTP_ASSOCINFO:
			return getSCTP_ASSOCINFO(fd, assocID);
		case SCTP_AUTH_ACTIVE_KEY:
			return getSCTP_AUTH_ACTIVE_KEY(fd, assocID);
		case SCTP_CONTEXT:
			return getSCTP_CONTEXT(fd, assocID);
		case SCTP_DEFAULT_SEND_PARAM:
			return getSCTP_DEFAULT_SEND_PARAM(fd, assocID);
		case SCTP_DELAYED_SACK:
			return getSCTP_DELAYED_SACK(fd, assocID);
		case SCTP_EVENTS:
			return getSCTP_EVENTS(fd);
		case SCTP_FRAGMENT_INTERLEAVE:
			return new sctp_boolean(getSCTP_FRAGMENT_INTERLEAVE(fd));
		case SCTP_GET_PEER_ADDR_INFO:
			return getSCTP_PEER_ADDR_INFO(fd, assocID, (InetSocketAddress) extra); 
		case SCTP_HMAC_IDENT:
			return getSCTP_HMAC_IDENT(fd);
		case SCTP_INITMSG:
			return getSCTP_INITMSG(fd);
		case SCTP_I_WANT_MAPPED_V4_ADDR:
			return new sctp_boolean(getSCTP_I_WANT_MAPPED_V4_ADDR(fd));
		case SCTP_LOCAL_AUTH_CHUNKS:
			return getSCTP_LOCAL_AUTH_CHUNKS(fd, assocID);
		case SCTP_MAXSEG:
			return getSCTP_MAXSEG(fd, assocID);
		case SCTP_MAX_BURST:
			return getSCTP_MAX_BURST(fd, assocID);
		case SCTP_PARTIAL_DELIVERY_POINT:
			return new sctp_assoc_value(0, getSCTP_PARTIAL_DELIVERY_POINT(fd));
		case SCTP_PEER_ADDR_PARAMS:
			return getSCTP_PEER_ADDR_PARAMS(fd, assocID);
		case SCTP_PEER_AUTH_CHUNKS:
			return getSCTP_PEER_AUTH_CHUNKS(fd, assocID);
		case SCTP_RTOINFO:
			return getSCTP_RTOINFO(fd, assocID);
		case SCTP_STATUS:
			return getSCTP_STATUS(fd, assocID);
		case SCTP_SO_REUSEADDR:
			return getSO_REUSEADDR(fd);
		default:
			throw new IOException("Operation not supported");
		}
	}

	public void setOption(SctpChannel chan, SctpSocketOption option, Object extra) throws IOException {
		try {
			int fd = getFileDescriptor(chan);
			int assocID = getAssocId(chan);

			switch(option) {
			case SCTP_DISABLEFRAGMENTS:
				chan.setOption(SctpStandardSocketOptions.SCTP_DISABLE_FRAGMENTS, ((sctp_boolean) extra).value);
				break;
			case SCTP_PRIMARY_ADDR:
				chan.setOption(SctpStandardSocketOptions.SCTP_PRIMARY_ADDR, (SocketAddress) extra);
				break;
			case SCTP_NODELAY:
				chan.setOption(SctpStandardSocketOptions.SCTP_NODELAY, ((sctp_boolean) extra).value);
				break;
			default:
				setOption(fd, assocID, option, extra);
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	public void setOption(SctpServerChannel chan, SctpSocketOption option, Object extra) throws IOException {
		try {
			int fd = getFileDescriptor(chan);
			int assocID = 0;
			switch(option) {
			case SCTP_DISABLEFRAGMENTS:
				chan.setOption(SctpStandardSocketOptions.SCTP_DISABLE_FRAGMENTS, ((sctp_boolean) extra).value);
				break;
			case SCTP_PRIMARY_ADDR:
				chan.setOption(SctpStandardSocketOptions.SCTP_PRIMARY_ADDR, (SocketAddress) extra);
				break;
			case SCTP_NODELAY:
				chan.setOption(SctpStandardSocketOptions.SCTP_NODELAY, ((sctp_boolean) extra).value);
				break;
			default:
				setOption(fd, assocID, option, extra);
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	public void setOption(int fd, int assocID, SctpSocketOption option, Object param) throws IOException {
		switch(option) {
		case SCTP_ADAPTATION_LAYER:
			setSCTP_ADAPTATION_LAYER(fd, (sctp_setadaptation) param);
			break;
		case SCTP_ASSOCINFO:
			setSCTP_ASSOCINFO(fd, (sctp_assocparams) param); 
			break;
		case SCTP_AUTH_ACTIVE_KEY:
			setSCTP_AUTH_ACTIVE_KEY(fd, (sctp_authkeyid) param);
			break;
		case SCTP_AUTH_CHUNK:
			setSCTP_AUTH_CHUNK(fd, (sctp_authchunk) param);
			break;
		case SCTP_AUTH_DELETE_KEY:
			setSCTP_AUTH_DELETE_KEY(fd, (sctp_authkeyid) param);
			break;
		case SCTP_AUTH_KEY:
			setSCTP_AUTH_KEY(fd, (sctp_authkey) param);
			break;
		case SCTP_CONTEXT:
			setSCTP_CONTEXT(fd, (sctp_assoc_value) param); 
			break;
		case SCTP_DEFAULT_SEND_PARAM:
			setSCTP_DEFAULT_SEND_PARAM(fd, (sctp_sndrcvinfo) param);
			break;
		case SCTP_DELAYED_SACK:
			setSCTP_DELAYED_SACK(fd, (sctp_sack_info) param);
			break;
		case SCTP_EVENTS:
			setSCTP_EVENTS(fd, (sctp_event_subscribe) param);
			break;
		case SCTP_FRAGMENT_INTERLEAVE:
			setSCTP_FRAGMENT_INTERLEAVE(fd, ((sctp_boolean) param).value);
			break;
		case SCTP_HMAC_IDENT:
			setSCTP_HMAC_IDENT(fd, (sctp_hmacalgo) param);
			break;
		case SCTP_INITMSG:
			setSCTP_INITMSG(fd, (sctp_initmsg) param);
			break;
		case SCTP_I_WANT_MAPPED_V4_ADDR:
			setSCTP_I_WANT_MAPPED_V4_ADDR(fd, ((sctp_boolean) param).value);
			break;
		case SCTP_MAXSEG:
			setSCTP_MAXSEG(fd, (sctp_assoc_value) param);
			break;
		case SCTP_MAX_BURST:
			setSCTP_MAX_BURST(fd, (sctp_assoc_value) param);
			break;
		case SCTP_PARTIAL_DELIVERY_POINT:
			setSCTP_PARTIAL_DELIVERY_POINT(fd, ((sctp_assoc_value) param).assoc_value); 
			break;
		case SCTP_PEER_ADDR_PARAMS:
			setSCTP_PEER_ADDR_PARAMS(fd, (sctp_paddrparams) param);
			break;
		case SCTP_RTOINFO:
			setSCTP_RTOINFO(fd, (sctp_rtoinfo) param);
			break;
		case SCTP_SO_REUSEADDR:
			setSO_REUSEADDR(fd, ((sctp_boolean) param).value);
			break;
		default:
			throw new IOException("Operation not supported");

		}
	}

	private sctp_rtoinfo getSCTP_RTOINFO(int fd, int assocID) {
		sctp_rtoinfo rtoinfo = new sctp_rtoinfo(assocID);
		Memory m = new Memory(rtoinfo.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_RTOINFO, rtoinfo.toJNA(m), new IntByReference(rtoinfo.jnaSize()).getPointer());
		return rtoinfo.fromJNA(m);
	}
	
	private void setSCTP_RTOINFO(int fd, sctp_rtoinfo rtoinfo) {
		Memory m = new Memory(rtoinfo.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_RTOINFO, rtoinfo.toJNA(m), rtoinfo.jnaSize());
	}

	private sctp_assocparams getSCTP_ASSOCINFO(int fd, int assocID) {
		sctp_assocparams associnfo = new sctp_assocparams(assocID);
		Memory m = new Memory(associnfo.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_ASSOCINFO, associnfo.toJNA(m), new IntByReference(associnfo.jnaSize()).getPointer());
		return associnfo.fromJNA(m);
	}
	
	private void setSCTP_ASSOCINFO(int fd, sctp_assocparams associnfo) {
		Memory m = new Memory(associnfo.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_ASSOCINFO, associnfo.toJNA(m), associnfo.jnaSize());
	}

	private sctp_initmsg getSCTP_INITMSG(int fd) {
		sctp_initmsg initmsg = new sctp_initmsg();
		Memory m = new Memory(initmsg.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_INITMSG, initmsg.toJNA(m), new IntByReference(initmsg.jnaSize()).getPointer());
		return initmsg.fromJNA(m);
	}	
	private void setSCTP_INITMSG(int fd, sctp_initmsg initmsg) {
		Memory m = new Memory(initmsg.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_INITMSG, initmsg.toJNA(m), initmsg.jnaSize());
	}

	private sctp_setadaptation getSCTP_ADAPTATION_LAYER(int fd) {
		sctp_setadaptation layer = new sctp_setadaptation();
		Memory m = new Memory(layer.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_ADAPTATION_LAYER, layer.toJNA(m), new IntByReference(layer.jnaSize()).getPointer());
		return layer.fromJNA(m);
	}
	
	private void setSCTP_ADAPTATION_LAYER(int fd, sctp_setadaptation layer) {
		Memory m = new Memory(layer.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_ADAPTATION_LAYER, layer.toJNA(m), layer.jnaSize());
	}

	private sctp_paddrparams getSCTP_PEER_ADDR_PARAMS(int fd, int assocID) throws IOException {
		sctp_paddrparams paddr = new sctp_paddrparams(assocID);
		Memory m = new Memory(paddr.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_PEER_ADDR_PARAMS, paddr.toJNA(m), new IntByReference(paddr.jnaSize()).getPointer());
		try {
			return paddr.fromJNA(m);	
		} catch (UnknownHostException e) {
			throw new IOException(e);
		}
	};
	
	private void setSCTP_PEER_ADDR_PARAMS(int fd, sctp_paddrparams paddr) {
		Memory m = new Memory(paddr.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_PEER_ADDR_PARAMS, paddr.toJNA(m), paddr.jnaSize());
	}
	
	private sctp_sndrcvinfo getSCTP_DEFAULT_SEND_PARAM(int fd, int associd) {
		sctp_sndrcvinfo sndrcvinfo = new sctp_sndrcvinfo(associd);
		Memory m = new Memory(sndrcvinfo.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_DEFAULT_SEND_PARAM, sndrcvinfo.toJNA(m), new IntByReference(sndrcvinfo.jnaSize()).getPointer());
		return sndrcvinfo.fromJNA(m);
	}
	
	private void setSCTP_DEFAULT_SEND_PARAM(int fd, sctp_sndrcvinfo sndrcvinfo) {
		Memory m = new Memory(sndrcvinfo.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_DEFAULT_SEND_PARAM, sndrcvinfo.toJNA(m), sndrcvinfo.jnaSize());
	}
	
	private sctp_event_subscribe getSCTP_EVENTS(int fd) {
		sctp_event_subscribe events = new sctp_event_subscribe();
		Memory m = new Memory(events.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_EVENTS, events.toJNA(m), new IntByReference(events.jnaSize()).getPointer());
		return events.fromJNA(m);
	}
	
	private void setSCTP_EVENTS(int fd, sctp_event_subscribe events) {
		Memory m = new Memory(events.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_EVENTS, events.toJNA(m), events.jnaSize());
	}
	
	private boolean getSCTP_I_WANT_MAPPED_V4_ADDR(int fd) {
		Memory m = new Memory(4); //1 boolean
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_I_WANT_MAPPED_V4_ADDR, m, new IntByReference(4).getPointer());
		return (m.getInt(0) != 0);
	}
	
	private void setSCTP_I_WANT_MAPPED_V4_ADDR(int fd, boolean iwantmappedv4addr) {
		Memory m = new Memory(4); //1 boolean
		m.setInt(0, iwantmappedv4addr ? 1 : 0);
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_I_WANT_MAPPED_V4_ADDR, m, 4);
	}
	
	private sctp_assoc_value getSCTP_MAXSEG(int fd, int associd) {
		sctp_assoc_value maxseg = new sctp_assoc_value(0);
		Memory m = new Memory(maxseg.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_MAXSEG, maxseg.toJNA(m), new IntByReference(maxseg.jnaSize()).getPointer());
		return maxseg.fromJNA(m);
	}
	
	private void setSCTP_MAXSEG(int fd, sctp_assoc_value maxseg) {
		Memory m = new Memory(maxseg.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_MAXSEG, maxseg.toJNA(m), maxseg.jnaSize());
	}

	private sctp_status getSCTP_STATUS(int fd, int assocID) throws IOException {
		sctp_status status = new sctp_status(assocID);
		Memory m = new Memory(status.jnaSize());
		m.clear();
		try {
			helper.getsockopt(fd, IPPROTO_SCTP, SCTP_STATUS, status.toJNA(m), new IntByReference(status.jnaSize()).getPointer());
			return status.fromJNA(m);
		} catch(UnknownHostException e) {
			throw new IOException(e);
		}
	}

	private sctp_paddrinfo getSCTP_PEER_ADDR_INFO(int fd, int associd, InetSocketAddress peer) throws IOException {
		sctp_paddrinfo paddr = new sctp_paddrinfo(associd, peer);
		Memory m = new Memory(paddr.jnaSize());
		m.clear();
		try {
			helper.getsockopt(fd, IPPROTO_SCTP, SCTP_GET_PEER_ADDR_INFO, paddr.toJNA(m, 0), new IntByReference(paddr.jnaSize()).getPointer());
			return paddr.fromJNA(m, 0);
		} catch(UnknownHostException e) {
			throw new IOException(e);
		}
	};

	private sctp_sack_info getSCTP_DELAYED_SACK(int fd, int associd) {
		sctp_sack_info sackinfo = new sctp_sack_info(associd);
		Memory m = new Memory(sackinfo.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_DELAYED_SACK, sackinfo.toJNA(m), new IntByReference(sackinfo.jnaSize()).getPointer());
		return sackinfo.fromJNA(m);
	}
	
	private void setSCTP_DELAYED_SACK(int fd, sctp_sack_info sackinfo) {
		Memory m = new Memory(sackinfo.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_DELAYED_SACK, sackinfo.toJNA(m), sackinfo.jnaSize());
	}

	private sctp_assoc_value getSCTP_MAX_BURST(int fd, int associd) {
		sctp_assoc_value burst = new sctp_assoc_value(0);
		Memory m = new Memory(burst.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_MAX_BURST, burst.toJNA(m), new IntByReference(burst.jnaSize()).getPointer());
		return burst.fromJNA(m);
	}
	
	private void setSCTP_MAX_BURST(int fd, sctp_assoc_value burst) {
		Memory m = new Memory(burst.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_MAX_BURST, burst.toJNA(m), burst.jnaSize());
	}
	
	private boolean getSCTP_FRAGMENT_INTERLEAVE(int fd) {
		Memory m = new Memory(4); //1 boolean
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_FRAGMENT_INTERLEAVE, m, new IntByReference(4).getPointer());
		return (m.getInt(0) != 0);
	}
	
	private void setSCTP_FRAGMENT_INTERLEAVE(int fd, boolean fragmentinterleave) {
		Memory m = new Memory(4); //1 boolean
		m.setInt(0, fragmentinterleave ? 1 : 0);
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_FRAGMENT_INTERLEAVE, m, 4);
	}
	
	private boolean getSO_REUSEADDR(int fd) {
		Memory m = new Memory(4); //1 boolean
		m.clear();
		helper.getsockopt(fd, SOL_SOCKET, SO_REUSEADDR, m, new IntByReference(4).getPointer());
		return (m.getInt(0) != 0);
	}
	
	private void setSO_REUSEADDR(int fd, boolean reuse) {
		Memory m = new Memory(4); //1 boolean
		m.setInt(0, reuse ? 1 : 0);
		helper.setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, m, 4);
	}
	
	private long getSCTP_PARTIAL_DELIVERY_POINT(int fd) {
		Memory m = new Memory(4); //1 int
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_PARTIAL_DELIVERY_POINT, m, new IntByReference(4).getPointer());
		return m.getInt(0);
	}
	
	private void setSCTP_PARTIAL_DELIVERY_POINT(int fd, long partialdeliverypoint) {
		Memory m = new Memory(4); //1 int
		m.setInt(0, (int) partialdeliverypoint);
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_PARTIAL_DELIVERY_POINT, m, 4);
	}

	private sctp_assoc_value getSCTP_CONTEXT(int fd, int associd) {
		sctp_assoc_value context = new sctp_assoc_value(0);
		Memory m = new Memory(context.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_CONTEXT, context.toJNA(m), new IntByReference(context.jnaSize()).getPointer());
		return context.fromJNA(m);
	}
	
	private void setSCTP_CONTEXT(int fd, sctp_assoc_value context) {
		Memory m = new Memory(context.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_CONTEXT, context.toJNA(m), context.jnaSize());
	}

	private void setSCTP_AUTH_CHUNK(int fd, sctp_authchunk authchunk) {
		Memory m = new Memory(authchunk.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_CHUNK, authchunk.toJNA(m), authchunk.jnaSize());
	}

	private sctp_hmacalgo getSCTP_HMAC_IDENT(int fd) {
		sctp_hmacalgo hmacalgo = new sctp_hmacalgo(0, new sctp_hmacalgo.idents[0]);
		Memory m = new Memory(hmacalgo.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_HMAC_IDENT, hmacalgo.toJNA(m), new IntByReference(hmacalgo.jnaSize()).getPointer());
		return hmacalgo.fromJNA(m);
	}
	
	private void setSCTP_HMAC_IDENT(int fd, sctp_hmacalgo hmacalgo) {
		Memory m = new Memory(hmacalgo.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_HMAC_IDENT, hmacalgo.toJNA(m), hmacalgo.jnaSize());
	}

	private void setSCTP_AUTH_KEY(int fd, sctp_authkey authkey) {
		Memory m = new Memory(authkey.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_KEY, authkey.toJNA(m), authkey.jnaSize());
	}

	private sctp_authkeyid getSCTP_AUTH_ACTIVE_KEY(int fd, int associd) {
		sctp_authkeyid authkeyid = new sctp_authkeyid(associd, 0);
		Memory m = new Memory(authkeyid.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_ACTIVE_KEY, authkeyid.toJNA(m), new IntByReference(authkeyid.jnaSize()).getPointer());
		return authkeyid.fromJNA(m);
	}
	
	private void setSCTP_AUTH_ACTIVE_KEY(int fd, sctp_authkeyid authkeyid) {
		Memory m = new Memory(authkeyid.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_ACTIVE_KEY, authkeyid.toJNA(m), authkeyid.jnaSize());
	}

	private void setSCTP_AUTH_DELETE_KEY(int fd, sctp_authkeyid authkeyid) {
		Memory m = new Memory(authkeyid.jnaSize());
		helper.setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_DELETE_KEY, authkeyid.toJNA(m), authkeyid.jnaSize());
	}

	private sctp_authchunks getSCTP_PEER_AUTH_CHUNKS(int fd, int associd) {
		sctp_authchunks chunks = new sctp_authchunks(associd);
		Memory m = new Memory(chunks.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_PEER_AUTH_CHUNKS, chunks.toJNA(m), new IntByReference(chunks.jnaSize()).getPointer());
		return chunks.fromJNA(m);
	}

	private sctp_authchunks getSCTP_LOCAL_AUTH_CHUNKS(int fd, int associd) {
		sctp_authchunks chunks = new sctp_authchunks(associd);
		Memory m = new Memory(chunks.jnaSize());
		m.clear();
		helper.getsockopt(fd, IPPROTO_SCTP, SCTP_LOCAL_AUTH_CHUNKS, chunks.toJNA(m), new IntByReference(chunks.jnaSize()).getPointer());
		return chunks.fromJNA(m);
	}

	private int getFileDescriptor(SctpChannel chan) throws Exception {
		Field f = chan.getClass().getDeclaredField("fdVal");
		f.setAccessible(true);
		return (Integer) f.get(chan);		
	}

	private int getAssocId(SctpChannel chan) throws Exception {
		Association assoc = chan.association();
		return assoc == null ? 0 : assoc.associationID();
	}

	private int getFileDescriptor(SctpServerChannel chan) throws Exception {
		Field f = chan.getClass().getDeclaredField("fdVal");
		f.setAccessible(true);
		return (Integer) f.get(chan);		
	}
}
