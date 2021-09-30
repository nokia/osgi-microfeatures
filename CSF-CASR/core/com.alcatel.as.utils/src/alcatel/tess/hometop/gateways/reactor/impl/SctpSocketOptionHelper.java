package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.log4j.Logger;

import com.alcatel.as.util.sctp.sctp_boolean;
import com.alcatel.as.util.sctp.sctp_assoc_value;
import com.alcatel.as.util.sctp.sctp_assocparams;
import com.alcatel.as.util.sctp.sctp_authchunk;
import com.alcatel.as.util.sctp.sctp_authchunks;
import com.alcatel.as.util.sctp.sctp_authkey;
import com.alcatel.as.util.sctp.sctp_authkeyid;
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
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpStandardSocketOptions;

import com.alcatel.as.util.sctp.SctpSocketOption;

public class SctpSocketOptionHelper {
	
	static {
		try {
			System.loadLibrary("asrsctp");
			Logger.getLogger("alcatel.tess.hometop.gateways.reactor.impl.SctpSocketOptionHelper").debug("Loaded asrsctp native library successfully");
		} catch(Throwable e) {
			Logger.getLogger("alcatel.tess.hometop.gateways.reactor.impl.SctpSocketOptionHelper").warn("Could not load asrsctp native library", e);
		}
	}
	
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
			return n_getSCTP_ADAPTATION_LAYER(fd);
		case SCTP_ASSOCINFO:
			return n_getSCTP_ASSOCINFO(fd, assocID);
		case SCTP_AUTH_ACTIVE_KEY:
			return n_getSCTP_AUTH_ACTIVE_KEY(fd, assocID);
//		case SCTP_AUTOCLOSE:
//			return getSCTP_AUTOCLOSE(chan);
		case SCTP_CONTEXT:
			return n_getSCTP_CONTEXT(fd, assocID);
		case SCTP_DEFAULT_SEND_PARAM:
			return n_getSCTP_DEFAULT_SEND_PARAM(fd, assocID);
		case SCTP_DELAYED_SACK:
			return n_getSCTP_DELAYED_SACK(fd, assocID);
		case SCTP_EVENTS:
			return n_getSCTP_EVENTS(fd);
		case SCTP_FRAGMENT_INTERLEAVE:
			return new sctp_boolean(n_getSCTP_FRAGMENT_INTERLEAVE(fd));
//		case SCTP_GET_ASSOC_NUMBER:
//			return getSCTP_GET_ASSOC_NUMBER(chan);
		case SCTP_GET_PEER_ADDR_INFO:
			return n_getSCTP_PEER_ADDR_INFO(fd, assocID, (InetSocketAddress) extra); 
		case SCTP_HMAC_IDENT:
			return n_getSCTP_HMAC_IDENT(fd);
		case SCTP_INITMSG:
			return n_getSCTP_INITMSG(fd);
		case SCTP_I_WANT_MAPPED_V4_ADDR:
			return new sctp_boolean(n_getSCTP_I_WANT_MAPPED_V4_ADDR(fd));
		case SCTP_LOCAL_AUTH_CHUNKS:
			return n_getSCTP_LOCAL_AUTH_CHUNKS(fd, assocID);
		case SCTP_MAXSEG:
			return n_getSCTP_MAXSEG(fd, assocID);
		case SCTP_MAX_BURST:
			return n_getSCTP_MAX_BURST(fd, assocID);
		case SCTP_PARTIAL_DELIVERY_POINT:
			return new sctp_assoc_value(n_getSCTP_PARTIAL_DELIVERY_POINT(fd));
		case SCTP_PEER_ADDR_PARAMS:
			return n_getSCTP_PEER_ADDR_PARAMS(fd, assocID);
		case SCTP_PEER_AUTH_CHUNKS:
			return n_getSCTP_PEER_AUTH_CHUNKS(fd, assocID);
		case SCTP_RTOINFO:
			return n_getSCTP_RTOINFO(fd, assocID);
		case SCTP_STATUS:
			return n_getSCTP_STATUS(fd, assocID);
		case SCTP_SO_REUSEADDR:
			return n_getSCTP_REUSE_ADDR(fd);
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
			n_setSCTP_ADAPTATION_LAYER(fd, (sctp_setadaptation) param);
			break;
		case SCTP_ASSOCINFO:
			n_setSCTP_ASSOCINFO(fd, (sctp_assocparams) param); 
			break;
		case SCTP_AUTH_ACTIVE_KEY:
			n_setSCTP_AUTH_ACTIVE_KEY(fd, (sctp_authkeyid) param);
			break;
		case SCTP_AUTH_CHUNK:
			n_setSCTP_AUTH_CHUNK(fd, (sctp_authchunk) param);
			break;
		case SCTP_AUTH_DELETE_KEY:
			n_setSCTP_AUTH_DELETE_KEY(fd, (sctp_authkeyid) param);
			break;
		case SCTP_AUTH_KEY:
			n_setSCTP_AUTH_KEY(fd, (sctp_authkey) param);
			break;
//		case SCTP_AUTOCLOSE:
//			setSCTP_AUTOCLOSE(chan, (Long) param); 
//			break;
		case SCTP_CONTEXT:
			n_setSCTP_CONTEXT(fd, (sctp_assoc_value) param); 
			break;
		case SCTP_DEFAULT_SEND_PARAM:
			n_setSCTP_DEFAULT_SEND_PARAM(fd, (sctp_sndrcvinfo) param);
			break;
		case SCTP_DELAYED_SACK:
			n_setSCTP_DELAYED_SACK(fd, (sctp_sack_info) param);
			break;
		case SCTP_EVENTS:
			n_setSCTP_EVENTS(fd, (sctp_event_subscribe) param);
			break;
		case SCTP_FRAGMENT_INTERLEAVE:
			n_setSCTP_FRAGMENT_INTERLEAVE(fd, ((sctp_boolean) param).value);
			break;
		case SCTP_HMAC_IDENT:
			n_setSCTP_HMAC_IDENT(fd, (sctp_hmacalgo) param);
			break;
		case SCTP_INITMSG:
			n_setSCTP_INITMSG(fd, (sctp_initmsg) param);
			break;
		case SCTP_I_WANT_MAPPED_V4_ADDR:
			n_setSCTP_I_WANT_MAPPED_V4_ADDR(fd, ((sctp_boolean) param).value);
			break;
		case SCTP_MAXSEG:
			n_setSCTP_MAXSEG(fd, (sctp_assoc_value) param);
			break;
		case SCTP_MAX_BURST:
			n_setSCTP_MAX_BURST(fd, (sctp_assoc_value) param);
			break;
		case SCTP_PARTIAL_DELIVERY_POINT:
			n_setSCTP_PARTIAL_DELIVERY_POINT(fd, ((sctp_assoc_value) param).assoc_value); 
			break;
		case SCTP_PEER_ADDR_PARAMS:
			n_setSCTP_PEER_ADDR_PARAMS(fd, (sctp_paddrparams) param);
			break;
		case SCTP_RTOINFO:
			n_setSCTP_RTOINFO(fd, (sctp_rtoinfo) param);
			break;
//		case SCTP_SET_PEER_PRIMARY_ADDR:
//			setSCTP_SET_PEER_PRIMARY_ADDR(chan, (SocketAddress) param);
//			break;
		case SCTP_SO_REUSEADDR:
			n_setSCTP_REUSE_ADDR(fd, ((sctp_boolean) param).value);
			break;
		default:
			throw new IOException("Operation not supported");
		
		}
	}
	
	private native sctp_rtoinfo n_getSCTP_RTOINFO(int fd, int associd);
	private native void n_setSCTP_RTOINFO(int fd, sctp_rtoinfo rtoinfo) throws IOException;

	private native sctp_assocparams n_getSCTP_ASSOCINFO(int fd, int assocID);
	private native void n_setSCTP_ASSOCINFO(int fd, sctp_assocparams associnfo) throws IOException;
	
	private native sctp_initmsg n_getSCTP_INITMSG(int fd);
	private native void n_setSCTP_INITMSG(int fd, sctp_initmsg initmsg) throws IOException;
	
//	/* SCTP_AUTOCLOSE */
//	private long getSCTP_AUTOCLOSE(SctpChannel chan) throws IOException {
//		try {
//			return n_getSCTP_AUTOCLOSE(getFileDescriptor(chan));
//		} catch(Exception e) {
//			throw new IOException(e.getMessage());
//		}
//	}
//	
//	private void setSCTP_AUTOCLOSE(SctpChannel chan, long seconds) throws IOException {
//		try {
//			n_setSCTP_AUTOCLOSE(getFileDescriptor(chan), seconds);
//		} catch(Exception e) {
//			throw new IOException(e.getMessage());
//		} 
//	}
	
//	private native long n_getSCTP_AUTOCLOSE(int fd);
//	private native void n_setSCTP_AUTOCLOSE(int fd, long seconds) throws IOException;
	
//	/* SCTP_SET_PRIMARY_PEER_ADDR */
//	private void setSCTP_SET_PEER_PRIMARY_ADDR(SctpChannel chan, SocketAddress peerprimaryaddr) throws IOException {
//		chan.setOption(SctpStandardSocketOptions.SCTP_SET_PEER_PRIMARY_ADDR, peerprimaryaddr);
//	}
	
	private native sctp_setadaptation n_getSCTP_ADAPTATION_LAYER(int fd);
	private native void n_setSCTP_ADAPTATION_LAYER(int fd, sctp_setadaptation layer) throws IOException;
	
	private native sctp_paddrparams n_getSCTP_PEER_ADDR_PARAMS(int fd, int assocID);
	private native void n_setSCTP_PEER_ADDR_PARAMS(int fd, sctp_paddrparams peeraddrparams) throws IOException;
	
	private native sctp_sndrcvinfo n_getSCTP_DEFAULT_SEND_PARAM(int fd, int associd);
	private native void n_setSCTP_DEFAULT_SEND_PARAM(int fd, sctp_sndrcvinfo sndrcvinfo) throws IOException;
	
	private native sctp_event_subscribe n_getSCTP_EVENTS(int fd);
	private native void n_setSCTP_EVENTS(int fd, sctp_event_subscribe events) throws IOException;
	
	private native boolean n_getSCTP_I_WANT_MAPPED_V4_ADDR(int fd);
	private native void n_setSCTP_I_WANT_MAPPED_V4_ADDR(int fd, boolean iwantmappedv4addr) throws IOException;
	
	private native sctp_assoc_value n_getSCTP_MAXSEG(int fd, int associd);
	private native void n_setSCTP_MAXSEG(int fd, sctp_assoc_value maxseg) throws IOException;
	
	private native sctp_status n_getSCTP_STATUS(int fd, int assocID);
	
	private native sctp_paddrinfo n_getSCTP_PEER_ADDR_INFO(int fd, int associd, InetSocketAddress peer);

	private native sctp_sack_info n_getSCTP_DELAYED_SACK(int fd, int associd);
	private native void n_setSCTP_DELAYED_SACK(int fd, sctp_sack_info sackinfo) throws IOException;
	
	private native sctp_assoc_value n_getSCTP_CONTEXT(int fd, int associd);
	private native void n_setSCTP_CONTEXT(int fd, sctp_assoc_value assocvalue) throws IOException;
	
	private native boolean n_getSCTP_FRAGMENT_INTERLEAVE(int fd);
	private native void n_setSCTP_FRAGMENT_INTERLEAVE(int fd, boolean fragmentinterleave) throws IOException;
	
	private native boolean n_getSCTP_REUSE_ADDR(int fd);
	private native void n_setSCTP_REUSE_ADDR(int fd, boolean reuseaddr) throws IOException;
	
	private native long n_getSCTP_PARTIAL_DELIVERY_POINT(int fd);
	private native void n_setSCTP_PARTIAL_DELIVERY_POINT(int fd, long partialdeliverypoint) throws IOException;
	
	private native sctp_assoc_value n_getSCTP_MAX_BURST(int fd, int associd);
	private native void n_setSCTP_MAX_BURST(int fd, sctp_assoc_value assocvalue) throws IOException;
	
	private native void n_setSCTP_AUTH_CHUNK(int fd, sctp_authchunk authchunk) throws IOException;
	
	private native sctp_hmacalgo n_getSCTP_HMAC_IDENT(int fd);
	private native void n_setSCTP_HMAC_IDENT(int fd, sctp_hmacalgo hmacalgo) throws IOException;
	
	private native void n_setSCTP_AUTH_KEY(int fd, sctp_authkey authkey) throws IOException;
	
	private native sctp_authkeyid n_getSCTP_AUTH_ACTIVE_KEY(int fd, int associd);
	private native void n_setSCTP_AUTH_ACTIVE_KEY(int fd, sctp_authkeyid authkeyid) throws IOException;
	
	private native void n_setSCTP_AUTH_DELETE_KEY(int fd, sctp_authkeyid authkeyid) throws IOException;
	
	private native sctp_authchunks n_getSCTP_PEER_AUTH_CHUNKS(int fd, int assocID) throws IOException;
	
	private native sctp_authchunks n_getSCTP_LOCAL_AUTH_CHUNKS(int fd, int assocID) throws IOException;
	
//	/* SCTP_GET_ASSOC_NUMBER */
//	private long getSCTP_GET_ASSOC_NUMBER(SctpChannel chan) throws IOException {
//		try {
//			return n_getSCTP_GET_ASSOC_NUMBER(getFileDescriptor(chan));
//		} catch (Exception e) {
//			throw new IOException(e.getMessage());
//		}
//	}
//
//	private native long n_getSCTP_GET_ASSOC_NUMBER(int fd) throws IOException;
		
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
