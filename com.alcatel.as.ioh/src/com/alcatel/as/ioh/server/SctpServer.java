// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.server;

import java.net.InetSocketAddress;

import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface SctpServer extends Server {

    public static final String SCTP_NODELAY = "sctp.nodelay";
	
    public static final String PROP_STREAM_IN = "stream.in";
    public static final String PROP_STREAM_OUT = "stream.out";
    public static final String PROP_SERVER_IP_SECONDARY = "server.ip.secondary";

    public static final String PROP_SCTP_SECURE_DELAYED = "sctp.secure.delayed";
    public static final String PROP_SCTP_SECURE_CIPHER = "sctp.secure.cipher";
    public static final String PROP_SCTP_SECURE_CIPHER_SUITES_ORDER = "sctp.secure.cipher.suites.order";
    public static final String PROP_SCTP_SECURE_PROTOCOL = "sctp.secure.protocol";
    public static final String PROP_SCTP_SECURE_KEYSTORE_FILE = "sctp.secure.keystore.file";
    public static final String PROP_SCTP_SECURE_KEYSTORE_PWD = "sctp.secure.keystore.pwd";
    public static final String PROP_SCTP_SECURE_KEYSTORE_TYPE = "sctp.secure.keystore.type";
    public static final String PROP_SCTP_SECURE_KEYSTORE_ALGO = "sctp.secure.keystore.algo";
    public static final String PROP_SCTP_SECURE_ENDPOINT_IDENTITY_ALGO = "sctp.secure.endpoint.identity.algo";
    public static final String PROP_SCTP_SECURE_KEYSTORE_WATCH = "sctp.secure.keystore.watch";
    public static final String PROP_SCTP_SECURE_CLIENT_AUTHENTICATE = "sctp.secure.client.authenticate";
    public static final String PROP_SCTP_SECURE_ALPN_PROTOCOL = "sctp.secure.alpn.protocol";
    public static final String PROP_SCTP_SECURE_SNI = "sctp.secure.sni";

    public static final String PROP_SCTP_SOCK_OPT_RTO_INIT = "sctp.socket.rto.init";
    public static final String PROP_SCTP_SOCK_OPT_RTO_MIN = "sctp.socket.rto.min";
    public static final String PROP_SCTP_SOCK_OPT_RTO_MAX = "sctp.socket.rto.max";
    public static final String PROP_SCTP_SOCK_OPT_MAX_BURST = "sctp.socket.max.burst";
    public static final String PROP_SCTP_SOCK_OPT_COOKIE_LIFE = "sctp.socket.cookie.life";
    public static final String PROP_SCTP_SOCK_OPT_ASSOC_MAX_RXT = "sctp.socket.assoc.max.rxt";
    public static final String PROP_SCTP_SOCK_OPT_PATH_MAX_RXT = "sctp.socket.path.max.rxt";
    public static final String PROP_SCTP_SOCK_OPT_MAX_INIT_RXT = "sctp.socket.max.init.rxt";
    public static final String PROP_SCTP_SOCK_OPT_HB_INTERVAL = "sctp.socket.hb.interval";
    public static final String PROP_SCTP_SOCK_OPT_DISABLE_FRAGMENTS = "sctp.socket.disable.fragments";
    public static final String PROP_SCTP_SOCK_OPT_PATH_MTU = "sctp.socket.path.mtu"; 
    public static final String PROP_SCTP_SOCK_OPT_HB_ENABLE = "sctp.socket.hb.enable";
    public static final String PROP_SCTP_SOCK_OPT_PMTUD_ENABLE = "sctp.socket.pmtud.enable";
    public static final String PROP_SCTP_SOCK_OPT_SACK_DELAY_ENABLE = "sctp.socket.sack.delay.enable";
    public static final String PROP_SCTP_SOCK_OPT_SACK_DELAY = "sctp.socket.sack.delay";
    public static final String PROP_SCTP_SOCK_OPT_SACK_FREQ = "sctp.socket.sack.freq";
    public static final String PROP_SCTP_SOCK_OPT_MAX_INIT_TIMEOUT = "sctp.socket.max.init.timeout";
    public static final String PROP_SCTP_SOCK_OPT_MAX_SEG = "sctp.socket.max.seg";
    public static final String PROP_SCTP_SOCK_OPT_FRAGMENT_INTERLEAVE = "sctp.socket.fragment.interleave";
    public static final String PROP_SCTP_SOCK_OPT_PARTIAL_DELIVERY_POINT = "sctp.socket.partial.delivery.point";
    public static final String PROP_SCTP_SOCK_OPT_REUSEADDR = "sctp.socket.reuseaddr";
    
    public InetSocketAddress getAddress ();

    public SctpServerChannel getServerChannel ();
}
