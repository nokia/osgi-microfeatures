// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

/**
 * The different options to that we can get/set </br>
 * Each options returns/takes a different class: </br>
	SCTP_STATUS (only get):							sctp_status </br>
	SCTP_DISABLEFRAGMENTS:							sctp_boolean</br>
	SCTP_EVENTS:									sctp_event_subscribe </br>
	SCTP_PEER_ADDR_PARAMS:							sctp_paddrparams </br>
	SCTP_DELAYED_SACK:								sctp_sack_info </br>
	SCTP_INITMSG: 									sctp_initmsg </br>
	SCTP_DEFAULT_SEND_PARAM:						sctp_sndrcvinfo </br>
	SCTP_PRIMARY_ADDR: 								SocketAddress </br>
	SCTP_NODELAY: 									sctp_boolean</br>
	SCTP_RTOINFO: 									sctp_rtoinfo </br>
	SCTP_ASSOCINFO:			 						sctp_assocparams </br>
	SCTP_I_WANT_MAPPED_V4_ADDR:						sctp_boolean</br>
	SCTP_MAXSEG:									sctp_assoc_value</br>
	SCTP_GET_PEER_ADDR_INFO (only get):				sctp_paddrinfo</br>
	SCTP_ADAPTATION_LAYER:							sctp_setadaptation</br>
	SCTP_CONTEXT:									sctp_assoc_value</br>
	SCTP_FRAGMENT_INTERLEAVE:						sctp_boolean</br>
	SCTP_PARTIAL_DELIVERY_POINT:					Long</br>
	SCTP_MAX_BURST:									sctp_assoc_value</br>
	SCTP_HMAC_IDENT:								sctp_hmacalgo</br>
	SCTP_AUTH_ACTIVE_KEY:							sctp_authkeyid</br>
	SCTP_PEER_AUTH_CHUNKS (only get):				sctp_authchunks</br>
	SCTP_LOCAL_AUTH_CHUNKS (only get):				sctp_authchunks</br>
	SCTP_AUTH_KEY (only set):						sctp_authkey</br>
	SCTP_AUTH_DELETE_KEY (only set):				sctp_authkeyid</br>
	SCTP_AUTH_CHUNK (only set):						sctp_authchunk</br>
	SCTP_SO_REUSEADDR:								sctp_boolean</br>
 */
public enum SctpSocketOption {
	SCTP_STATUS,
	SCTP_DISABLEFRAGMENTS,
	SCTP_EVENTS,
	//SCTP_AUTOCLOSE,
	SCTP_PEER_ADDR_PARAMS,
	SCTP_DELAYED_SACK,
	SCTP_INITMSG,
	SCTP_DEFAULT_SEND_PARAM,
	SCTP_PRIMARY_ADDR,
	SCTP_NODELAY,
	SCTP_RTOINFO,
	SCTP_ASSOCINFO,
	SCTP_I_WANT_MAPPED_V4_ADDR,
	SCTP_MAXSEG,
	SCTP_GET_PEER_ADDR_INFO,
	SCTP_ADAPTATION_LAYER,
	SCTP_CONTEXT,
	SCTP_FRAGMENT_INTERLEAVE,
	SCTP_PARTIAL_DELIVERY_POINT,
	SCTP_MAX_BURST,
	SCTP_HMAC_IDENT,
	SCTP_AUTH_ACTIVE_KEY,
	SCTP_PEER_AUTH_CHUNKS,
	SCTP_LOCAL_AUTH_CHUNKS,
	//SCTP_GET_ASSOC_NUMBER,
	//SCTP_SET_PEER_PRIMARY_ADDR,
	SCTP_AUTH_KEY,
	SCTP_AUTH_DELETE_KEY,
	SCTP_AUTH_CHUNK,
	SCTP_SO_REUSEADDR
}
