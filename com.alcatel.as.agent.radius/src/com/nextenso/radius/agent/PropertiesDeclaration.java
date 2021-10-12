// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;

@Config(name = "radiusagent", rootSnmpName = "alcatel.srd.a5350.RadiusAgent", rootOid = { 637, 71, 6, 1030 }, section = "Radius Agent parameters")
public interface PropertiesDeclaration {

	@FileDataProperty(fileData = "secret.radiusAgent", title = "Radius Secrets", help = "This property contains the list of all authorized clients along with the matching shared secrets.<br>The format is : <b>client_ip secret ['nocheck']</b><br>The client_ip can be a mask or a specific host (then it is prefixed with 'H:').<br>The last (optional) field 'nocheck' disables secret checking (but the secret value is still used for responses).<br><br>Examples:<br>&nbsp;H:192.32.31.45     customSecret<br>&nbsp;H:192.32.31.46     otherSecret       nocheck<br>&nbsp;255.255.255.255    secretForWorld<br>The list is ordered: the first matching client_ip is used.", oid = 1600, snmpName = "RadiusSecrets", required = true, dynamic = true)
	public static final String SECRET = "radiusagent.secret";

	@FileDataProperty(fileData = "nextAcct.radiusAgent", title = "Next Radius Accounting Server", help = "Specifies the next Radius Accounting Server in the chain for a given client IP. The format of each line is <b>['H:']client_ip server_ip:server_port secret</b>, or <b>['H:']client_ip server_ip secret</b>.<br>'H:' before the client_ip specifies an exact IP Address. If 'H:' is not present, client_ip specifies an IP Address mask.", oid = 1601, snmpName = "NextRadiusAccountingServer", required = true, dynamic = true)
	public static final String NEXT_ACCT = "radiusagent.next.acct";

	@FileDataProperty(fileData = "nextAuth.radiusAgent", title = "Next Radius Access Server", help = "Specifies the next Radius Access Server in the chain for a given client IP. The format of each line is <b>['H:']client_ip server_ip:server_port secret</b>, or <b>['H:']client_ip server_ip secret</b>.<br>'H:' before the client_ip specifies an exact IP Address. If 'H:' is not present, client_ip specifies an IP Address mask.", oid = 1602, snmpName = "NextRadiusAccessServer", required = true, dynamic = true)
	public static final String NEXT_AUTH = "radiusagent.next.auth";

	@IntProperty(min = 1, max = 3600, title = "Request Timeout", help = "The timeout in seconds after which a request is considered as aborted if not replied to.", oid = 1603, snmpName = "RequestTimeout", required = true, dynamic = true, defval = 3)
	public static final String REQ_TIMEOUT = "radiusagent.req.timeout";

	@IntProperty(min = 1, max = 99, title = "Request Retransmission Attempts", help = "The maximum number of request retransmission attempts.", oid = 1604, snmpName = "RequestRetransmissionAttempts", required = true, dynamic = true, defval = 3)
	public static final String REQ_MAX = "radiusagent.req.max";

	@IntProperty(min = 0, max = 3600, title = "Response Cache", help = "The timeout in seconds during which a response remains in cache to support client retransmission.", oid = 1605, snmpName = "ResponseCache", required = true, dynamic = true, defval = 3)
	public static final String RESP_TIMEOUT = "radiusagent.resp.timeout";

	@StringProperty(title = "Radius Stack instance", help = "The names of the Radius Stack instances that the Radius Agent will connect to (separated by spaces).The special value <b>' * '</b> means that the Radius Agent will connect to all the Radius Stacks no matter the instance.Note that the Radius Agent must be restarted to take into account any change of that property.", oid = 1606, snmpName = "RadiusStackInstance", required = true, dynamic = true, section = "Radius Agent Private Parameters", defval = "*")
	public static final String STACK = "radiusagent.stack";

	@FileDataProperty(title="Radius Proxylets", oid=1607, snmpName="RadiusProxyletContexts", required=true, dynamic=true, blueprintEditor="/bpadminpxleteditor/index.html", fileData="radius-pxlets.xml")
	public static String PROXYLET_CONTEXTS = "radiusagent.proxylets";

	@BooleanProperty(title = "Proxy State Best effort", help = "Anticipate the case where the remote server does not return the ProxyState attribute.", required = false, dynamic = false, defval = false)
	public static final String PROXY_STATE_BEST_EFFORT = "radiusagent.proxystate.besteffort";	 

}
