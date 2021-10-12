// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.config;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.Scope;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.alcatel_lucent.as.management.annotation.config.Visibility;

@Config(rootSnmpName = "alcatel.srd.a5350.SystemConfiguration", rootOid = { 637, 71, 6, 10 })
public interface HeaderProperties {
  @StringProperty(title="Http Header name for the clid", 
      help="This property defines the name of the custom http header that willeventually hold the client id (clid) of the http client.", 
      oid=629, 
      snmpName="HttpHeaderNameForTheClid", 
      required=true, 
      dynamic=true,
      section="Http Header Names", 
      defval="X-Nx-Clid",
      scope=Scope.GROUP)
  public static final String HEADER_CLID = "system.clidHeaderName";
  
  @StringProperty(title="Http Header name for the apn", 
      help="This property defines the name of the custom http header that willeventually hold the Called Id (gsm) or the APN (gprs).", 
      oid=630, 
      snmpName="HttpHeaderNameForTheApn", 
      required=true, 
      dynamic=true,
      section="Http Header Names", 
      defval="x-Nx-Apn",
      scope=Scope.GROUP)
  public static final String HEADER_APN = "system.apnHeaderName";
  
  @StringProperty(title="Http Header name for the clip", 
      help="This property defines the name of the custom http header that willeventually hold the client ip address of the http client", 
      oid=631,
      snmpName="HttpHeaderNameForTheClip", 
      required=true, 
      dynamic=true,
      section="Http Header Names", 
      defval="X-Nx-Clip",
      scope=Scope.GROUP)
  public static final String HEADER_CLIP = "system.clipHeaderName";
  
  @StringProperty(title="Http Header name for the ssl subject",
      help="This property defines the name of the custom http header that willeventually hold the name of the subject of the server ssl certificate.", 
      oid=632, 
      snmpName="HttpHeaderNameForTheSslSubject",
      required=true, 
      dynamic=true, 
      section="Http Header Names",
      defval="X-Nx-Ssl-Subject",
      scope=Scope.GROUP)
  public static final String HEADER_SSL = "system.sslHeaderName";
  
  @StringProperty(title="Http Header name for the clip port",
      help="his property defines the name of the custom http header for the port number of the http client.", 
      required=false, 
      dynamic=true, 
      section="Http Header Names",
      defval="X-Nx-ClPort",
      visibility=Visibility.HIDDEN)
  public static final String HEADER_CLIP_PORT = "system.clipPortHeaderName";
}
