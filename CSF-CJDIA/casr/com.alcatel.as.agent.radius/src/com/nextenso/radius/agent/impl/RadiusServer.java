package com.nextenso.radius.agent.impl;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.DNSManager;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.radius.AuthenticationRule;
import com.nextenso.proxylet.radius.acct.AcctUtils;
import com.nextenso.proxylet.radius.auth.AuthUtils;

/**
 * The Radius server.
 */
public class RadiusServer {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.server");

	private String _hostAddress, _hostName;
	private int _ip;
	private int _port;
	private AuthenticationRule _secret;

	/**
	 * Used when parsing Next Server Format: <H:>clientIP serverIP<:port> secret
	 */
	private RadiusServer(String def, int defPort) {
		StringTokenizer st = new StringTokenizer(def);
		if (st.countTokens() != 3) {
			throw new IllegalArgumentException("Invalid radius server definition: " + def);
		}
		String clientIP = st.nextToken();
		String server = st.nextToken();
		_secret = new RadiusSecret(clientIP, st.nextToken());
		int index = server.indexOf(':');
		if (index == -1) {
			_hostAddress = server;
			_port = defPort;
		} else {
			_hostAddress = server.substring(0, index);
			_port = Integer.parseInt(server.substring(index + 1));
		}
		_ip = MuxUtils.getIPAsInt(_hostAddress);
		// we re-write the IP Address to avoid 127.0.0.010 != 127.0.0.10
		_hostAddress = MuxUtils.getIPAsString(_ip);
	}

	public RadiusServer(String host, int port, byte[] secret, int clientIP, boolean isAccounting) {
		if (host == null || host.length() == 0) {
			throw new IllegalArgumentException("Invalid server definition: the host cannot be null");
		}
		String theHost = host;
		char c = theHost.charAt(0);
		if (c < '0' || c > '9') {
			// need a DNS
			String ip_s = getByName(theHost);
			if (ip_s == null) {
				throw new RuntimeException("Failed to resolve DNS for: " + theHost);
			}
			theHost = ip_s;
		}

		_ip = MuxUtils.getIPAsInt(theHost);
		// we re-write the IP Address to avoid 127.0.0.010 != 127.0.0.10
		_hostAddress = MuxUtils.getIPAsString(_ip);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("RadiusServer: host=" + host + ", int ip= " + _ip + ", host address=" + _hostAddress);
		}

		byte[] theSecret = secret;
		if (theSecret == null) {
			theSecret = getRadiusSecret(_hostAddress, port, clientIP, isAccounting);
			if (theSecret == null) {
				throw new IllegalArgumentException("Invalid server definition: unknown secret for " + host + ":" + port);
			}
		}

		_port = port;
		_secret = new RadiusSecret(theSecret);
	}

	public String getHostAddress() {
		return _hostAddress;
	}

	public String getName() {
		// TO DO : dns for host name...?
		return _hostName;
	}

	public int getPort() {
		return _port;
	}

	public int getIp() {
		return _ip;
	}

	public byte[] getSecret() {
		return _secret.getPassword();
	}

	private boolean matchIP(int ip) {
		return _secret.match(ip);
	}

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("RadiusServer [host=").append(getHostAddress());
		buff.append(':').append(String.valueOf(getPort()));
		buff.append(", ").append(_secret).append(']');
		return buff.toString();
	}

	private static MuxConnectionManager connectionManager;

	public static void setMuxConnectionManager(MuxConnectionManager manager) {
		connectionManager = manager;
	}

	private static String getByName(String name) {
		MuxConnection connection = connectionManager.getRandomMuxConnection();
		if (connection == null) {
			return null;
		}
		// TODO use DNS API
		String[] ips = DNSManager.getByName(name, connection);
		if (ips.length > 0) {
			return ips[0];
		}
		return null;
	}

	/*********************************************************
	 * The following code is used to handle predefined servers
	 ********************************************************/

	private static List<RadiusServer> NEXT_ACCOUNTING = new ArrayList<RadiusServer>();
	private static List<RadiusServer> NEXT_ACCESS = new ArrayList<RadiusServer>();

	public static void setAccountingServers(String data)
		throws ConfigException {
		init(data, NEXT_ACCOUNTING, AcctUtils.ACCT_PORT);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setAccountingServers: Registered servers=" + NEXT_ACCOUNTING);
		}
	}

	public static void setAccessServers(String data)
		throws ConfigException {
		init(data, NEXT_ACCESS, AuthUtils.AUTH_PORT);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setAccessServers: Registered servers=" + NEXT_ACCESS);
		}
	}

	private static void init(String data, List<RadiusServer> servers, int defaultPort)
		throws ConfigException {
		servers.clear();

		try {
			BufferedReader reader = new BufferedReader(new StringReader(data));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				RadiusServer server = new RadiusServer(line, defaultPort);
				servers.add(server);
			}
			reader.close();
		}
		catch (Throwable t) {
			throw new ConfigException("Invalid Server List", t);
		}

	}

	public static RadiusServer getServer(RadiusMessageFacade message, int remoteIP) {
		RadiusServer server = message.getServer();
		if (server != null) {
			return server;
		}
		List<RadiusServer> servers = (message instanceof AccountingRequestFacade) ? NEXT_ACCOUNTING : NEXT_ACCESS;
		for (int i = 0; i < servers.size(); i++) {
			server = servers.get(i);
			if (server.matchIP(remoteIP)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Redirecting to server #" + i + " for client IP: " + MuxUtils.getIPAsString(remoteIP));
				}
				message.setServer(server);
				return server;
			}
		}
		return null;
	}

	private static byte[] getRadiusSecret(String host, int port, int ip, boolean isAccounting) {
		List<RadiusServer> servers = (isAccounting) ? NEXT_ACCOUNTING : NEXT_ACCESS;
		for (int i = 0; i < servers.size(); i++) {
			RadiusServer server = servers.get(i);
			if (port != server.getPort()) {
				continue;
			}
			if (!host.equals(server.getHostAddress())) {
				continue;
			}
			if (server.matchIP(ip)) {
				return server.getSecret();
			}
		}
		return null;
	}

}
