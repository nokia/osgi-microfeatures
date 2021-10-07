package com.nextenso.mux;

/**
 * This adds constant values used in the mux prorocol. This class is subject to change and may be
 * removed from the API. Please don't use this class directly.
 * 
 * @internal
 */
public interface MuxProtocol
{
    public final static boolean BIG_ENDIAN = false;

    /**
     * The flags are formatted like this:
     *   x     xxx      xxxx
     *  Ack  Protocol  Action
     */

    public static final int KEEP_ALIVE_FLAGS = 0xFF; /* 1 111 1111 */

    public static final int ACK_MASK = 0x80; /* 1 000 0000 */
    public static final int PROTOCOL_MASK = 0x70; /* 0 111 0000 */
    public static final int ACTION_MASK = 0x0F; /* 0 000 1111 */

    /**
     * Mux protocol DNS is used by agent to request a DNS resolution from IO handlers.  
     */
    public static final int PROTOCOL_DNS = 0x60; /* 0 110 0000 */
    public static final int ACTION_GET_BY_NAME = 1;/* 0000 0001 */
    public static final int ACTION_GET_BY_ADDR = 2;/* 0000 0010 */

    /**
     * Mux protocol CTL is used for various control actions.  
     */
    public static final int PROTOCOL_CTL = 0x70; /* 0 111 0000 */
    public static final int ACTION_CONFIG = 1;/* 0000 0001 */
    public static final int ACTION_CONFIG_CONNECTION = 2;/* 0000 0010 */
    public static final int ACTION_STOP = 3;/* 0000 0011 */
    public static final int ACTION_RELEASE = 4;/* 0000 0100 */
    public static final int ACTION_RELEASE_CONFIRM = 5;/* 0000 0101 */
    public static final int ACTION_RELEASE_CANCEL = 6;/* 0000 0110 */
    public static final int ACTION_START = 7;/* 0000 0111 */
    public static final int ACTION_ID = 8;/* 0000 1000 */
    public static final int ACTION_UMEM = 9;/* 0000 1001 */
    public static final int ACTION_PING = 10;/* 0000 1010 */
    public static final int ACTION_ALIVE = 11;/* 0000 1011 */
    public static final int ACTION_OVERLOAD = 12;/* 0000 1100 */

    public static final int ACTION_OVERLOAD_FLAGS = 0x0F; /* 0000 1111 */
    public static final int ACTION_OVERLOAD_NORMAL = 0;
    public static final int ACTION_OVERLOAD_CPU = 1;
    public static final int ACTION_OVERLOAD_MEM = 2;
    public static final int ACTION_OVERLOAD_RTT = 4;

    /**
     * Mux protocol TCP and UDP
     */
    public static final int PROTOCOL_TCP = 0x10; /* 0 001 0000 */
    public static final int PROTOCOL_UDP = 0x30; /* 0 011 0000 */
    public static final int PROTOCOL_SCTP = 0x100 ; /* not used as MASK in MUX, only for decoding */
    public static final int ACTION_DATA = 1;/* 0000 0001 */
    public static final int ACTION_CLOSE = 2;/* 0000 0010 */
    public static final int ACTION_OPEN_V4 = 3;/* 0000 0011 */
    public static final int ACTION_LISTEN = 4;/* 0000 0100 */
    public static final int ACTION_SHARED_SOCKET = 5;/* 0000 0101 */
    public static final int ACTION_REUSE_SOCKET = 6;/* 0000 0110 */
    public static final int ACTION_ABORT = 7;/* 0000 0111 */

    public static final int PROTOCOL_OPAQUE = 0x50; /* 0 101 0000 */

    public static final int PROTOCOL_NONE = 0x00; /* 0 000 0000 */
    public static final int ACTION_DHT_GET = 6;/* 0000 0110 */
    public static final int ACTION_TOPOLOGY_VIEW_REQUEST = 9;/* 0000 1001 */

    /** these are valid for all MUX version */
    public static final int MUX_VERSION_OFFSET = 4;
    public static final int MUX_FLAGS_OFFSET = 5;

    public static final int V0_HEADER_SIZE = 20;
    public static final int V0_VERSION_OFFSET = 4;
    public static final int V0_FLAGS_OFFSET = 5;
    public static final int V0_LEN_OFFSET = 6;
    public static final int V0_SESSIONID_OFFSET = 8;
    public static final int V0_CHANNELID_OFFSET = 16;
    public static final int V0_CONTENT_SIZE_MAX = 0xFFFF;

    public static final int V4_HEADER_SIZE = 20;
    public static final int V4_VERSION_OFFSET = 4;
    public static final int V4_FLAGS_OFFSET = 5;
    public static final int V4_LEN_OFFSET = 6;
    public static final int V4_SESSIONID_OFFSET = 8;
    public static final int V4_CHANNELID_OFFSET = 16;
    public static final int V4_CONTENT_SIZE_MAX = 0xFFFF;

    public static final int MAGIC_HEADER_VAL = 0x5D93ac13;
    public static final int MAGIC_TRAILER_VAL = 0x7E33a1d5;
    public static final int MAGIC_HEADER_LEN = 4;
    public static final int MAGIC_TRAILER_LEN = 4;

    public static final int LISTEN_MSG_LEN = 7;
    public static final int CONNECT_MSG_LEN = 13;
    public static final int BIND_MSG_LEN = 6;

    public static final int V2_HEADER_SIZE_DIFF = 12;
    public static final int V2_REMOTEIP_OFFSET_DIFF = 0;
    public static final int V2_VIRTUALIP_OFFSET_DIFF = 4;
    public static final int V2_REMOTEPORT_OFFSET_DIFF = 8;
    public static final int V2_VIRTUALPORT_OFFSET_DIFF = 10;
    public static final int V2_HEADER_SIZE = MuxProtocol.V0_HEADER_SIZE + MuxProtocol.V2_HEADER_SIZE_DIFF;
    public static final int V2_REMOTEIP_OFFSET = V0_HEADER_SIZE + MuxProtocol.V2_REMOTEIP_OFFSET_DIFF;
    public static final int V2_VIRTUALIP_OFFSET = V0_HEADER_SIZE + MuxProtocol.V2_VIRTUALIP_OFFSET_DIFF;
    public static final int V2_REMOTEPORT_OFFSET = V0_HEADER_SIZE + MuxProtocol.V2_REMOTEPORT_OFFSET_DIFF;
    public static final int V2_VIRTUALPORT_OFFSET = V0_HEADER_SIZE + MuxProtocol.V2_VIRTUALPORT_OFFSET_DIFF;
    public static final int V2_CONTENT_SIZE_MAX = 0xFFFF;

    public static final int V3_HEADER_SIZE = V0_HEADER_SIZE + 4;
    public static final int V3_LEN_OFFSET = V0_HEADER_SIZE;
    public static final int V3_CONTENT_SIZE_MAX = Integer.MAX_VALUE;

    public static final int V5_HEADER_SIZE = 10;
    public static final int V5_LEN_OFFSET = 6;
    public static final int V5_CONTENT_SIZE_MAX = Integer.MAX_VALUE;
  

}
