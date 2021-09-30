package com.alcatel_lucent.as.service.dns.impl;

import java.io.File;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;

import com.alcatel_lucent.as.service.dns.impl.parser.HostParser;
import com.alcatel_lucent.as.service.dns.impl.parser.NsSwitchParser;

public class DNSProperties implements Serializable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	public enum NsSwitchOption {
		FILES, DNS;
	}

	private final static Logger LOGGER = Logger
			.getLogger("dns.impl.properties");

	public static final String ETC_HOSTS = "/etc/hosts";
	public static final String RESOLV_CONF = "/etc/resolv.conf";
	public static final String NS_SWITCH_CONF = "/etc/nsswitch.conf";

	private static final String NONE = "none";
	private static final String EMPTY_STRING = "";

	private String _hostsFile = ETC_HOSTS;
	private String _resolvConfFile = RESOLV_CONF;
	private String _nsswitchConfFile = NS_SWITCH_CONF;
	private boolean _isCacheEnabled = true;
	private transient RecordCache _hostCache;
	private final transient RecordCache _cache;
	private final List<NsSwitchOption> _nsOptions = new ArrayList<NsSwitchOption>(
			3);

	private final List<Resolver> _additionalResolvers = new ArrayList<Resolver>();
	private List<Resolver> _resolvers = null;
	private List<Name> _searchPaths = null;

	private long _lastModifiedHostFile = 0L;
	private long _lastCheckedHostFile = 0L;
	private long _lastModifiedResolvConfFile = 0L;
	private long _lastCheckedResolvConfFile = 0L;
	private int _timeout = 5;

	private Meters _meters;

	private int _ipvmode = 10;

	public DNSProperties() {
		_cache = new RecordCache();
	}

	public void setMeters(Meters meters) {
		_meters = meters;
		_cache.setMeters(meters);
	}

	public Meters getMeters() {
		return _meters;
	}

	/**
	 * Gets the hostsFile.
	 * 
	 * @return The hostsFile.
	 */
	public final String getHostsFile() {
		return _hostsFile;
	}

	/**
	 * Sets the hostsFile.
	 * 
	 * @param hostsFile
	 *            The hostsFile.
	 */
	public final void setHostsFile(String hostsFile) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setHostsFile: file=" + hostsFile);
		}
		synchronized (HostParser.class) {
			if (hostsFile == null
					|| hostsFile.equalsIgnoreCase(NONE)
					|| hostsFile.equals(EMPTY_STRING)) {
				_lastModifiedHostFile = 0L;
				_hostsFile = null;
				_hostCache = new RecordCache();
				return;
			}

			File file = new File(hostsFile);
			if (file.exists() && file.isFile()) {
				_hostsFile = hostsFile;
			} else {
				_hostsFile = ETC_HOSTS;
				file = new File(_hostsFile);
				if (file.exists() && file.isFile()) {
					if (LOGGER.isEnabledFor(Level.WARN)) {
						LOGGER.warn(hostsFile
								+ " is not an existing file -> use default file="
								+ _hostsFile);
					}
				} else {
					if (LOGGER.isEnabledFor(Level.WARN)) {
						LOGGER.warn("Default file is not an existing file ("
								+ ETC_HOSTS
								+ ") -> do not use local hosts");
					}
					setHostsFile(null);
					return;
				}
			}

			RecordCache hostCache = new RecordCache();
			_lastModifiedHostFile = file.lastModified();
			_lastCheckedHostFile = System
					.currentTimeMillis();
			HostParser.parse(hostCache, _hostsFile);
			_hostCache = hostCache;
		}
	}

	public RecordCache getHostsCache() {
		return _hostCache;
	}

	/**
	 * Gets the resolvConfFile.
	 * 
	 * @return The resolvConfFile.
	 */
	public final String getResolvConfFile() {
		return _resolvConfFile;
	}

	/**
	 * Sets the resolvConfFile.
	 * 
	 * @param resolvConfFile
	 *            The resolvConfFile.
	 */
	public final void setResolvConfFile(
			String resolvConfFile) {
		/**
		 * IMPORTANT : this method has actually no effect : the file used is
		 * determined in ResolverConfig.getCurrentConfig() Hence this method was
		 * deactivated for now
		 */
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setResolvConfFile: file="
					+ resolvConfFile);
		}
		if (true)
			return;
		synchronized (RESOLV_CONF) {
			if (resolvConfFile != null) {
				if (resolvConfFile.equalsIgnoreCase(NONE)
						|| resolvConfFile
								.equals(EMPTY_STRING)) {
					_resolvConfFile = null;
					return;
				}

				File file = new File(resolvConfFile);
				if (file.exists() && file.isFile()) {
					_resolvConfFile = resolvConfFile;
				} else {
					_resolvConfFile = RESOLV_CONF;
					file = new File(_resolvConfFile);
					if (file.exists() && file.isFile()) {
						if (LOGGER.isEnabledFor(Level.WARN)) {
							LOGGER.warn(resolvConfFile
									+ " is not an existing file -> use default="
									+ _resolvConfFile);
						}
					} else {
						if (LOGGER.isEnabledFor(Level.WARN)) {
							LOGGER.warn("Default file is not an existing file ("
									+ RESOLV_CONF
									+ ") -> do not use resolv.conf");
						}
						_resolvConfFile = null;
					}
				}
			} else {
				_resolvConfFile = null;
			}
		}
	}

	public List<NsSwitchOption> getNsSwitchOptions() {
		return _nsOptions;
	}

	/**
	 * Gets the nsswitchConfFile.
	 * 
	 * @return The nsswitchConfFile.
	 */
	public final String getNsSwitchConfFile() {
		return _nsswitchConfFile;
	}

	/**
	 * Sets the nsswitchConfFile.
	 * 
	 * @param nsswitchConfFile
	 *            The nsswitchConfFile.
	 */
	public final void setNsSwitchConfFile(
			String nsswitchConfFile) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setNsSwitchConfFile: file="
					+ nsswitchConfFile);
		}

		if (nsswitchConfFile != null) {
			File file = new File(nsswitchConfFile);
			if (nsswitchConfFile.equalsIgnoreCase(NONE)
					|| nsswitchConfFile
							.equals(EMPTY_STRING)) {
				_nsOptions.clear();
				_nsswitchConfFile = null;
				return;
			}

			_nsOptions.clear();
			if (file.exists() && file.isFile()) {
				_nsswitchConfFile = nsswitchConfFile;
			} else {
				_nsswitchConfFile = NS_SWITCH_CONF;
				file = new File(_nsswitchConfFile);
				if (file.exists() && file.isFile()) {
					if (LOGGER.isEnabledFor(Level.WARN)) {
						LOGGER.warn(nsswitchConfFile
								+ " is not an existing file -> use default file="
								+ _nsswitchConfFile);
					}
				} else {
					if (LOGGER.isEnabledFor(Level.WARN)) {
						LOGGER.warn("Default file is not an existing file ("
								+ NS_SWITCH_CONF
								+ ") -> use default values: FILES DNS");
					}
					setNsSwitchConfFile(null);
					return;
				}
			}
			NsSwitchParser.parse(_nsswitchConfFile,
					_nsOptions);
			return;
		}

		_nsswitchConfFile = null;
		_nsOptions.clear();
		_nsOptions.add(NsSwitchOption.FILES);
		_nsOptions.add(NsSwitchOption.DNS);
	}

	public void setAdditionalServers(
			String additionalServers) {
		/**
		 * DO NOT USE : not thread safe
		 */
		_additionalResolvers.clear();
		if (additionalServers == null
				|| additionalServers.equalsIgnoreCase(NONE)
				|| additionalServers.equals(EMPTY_STRING)) {
			return;
		}

		String[] servers = additionalServers
				.split("\\s*,\\s*");
		for (String server : servers) {
			String[] elts = server.split("\\s+");
			String address = null;
			if (elts.length >= 1) {
				address = elts[0];
			}
			int port = -1;
			if (elts.length >= 2) {
				String sPort = elts[1];
				try {
					port = Integer.parseInt(sPort);
				} catch (Exception e) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("setAdditionalServers: port is not integer="
								+ sPort);
					}
					continue;
				}
			}

			try {
				DnsResolver resolver = new DnsResolver(
						address);
				if (port >= 0) {
					resolver.setPort(port);
				}
				resolver.setTimeout(_timeout);
				resolver.setAlarmWaterMark(_alarmWaterMark);
				resolver.setInstanceName(_instanceName);
				_additionalResolvers.add(resolver);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("setAdditionalServers: added a server="
							+ address
							+ ", port="
							+ port
							+ ", resolver =" + resolver);
				}
			} catch (UnknownHostException uhe) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("setAdditionalServers: cannot connect the specified host="
							+ address);
				}
			}

		}

	}

	private List<Resolver> getConfResolvers() {
		synchronized (RESOLV_CONF) {
			if (getResolvConfFile() != null) { // always true since
												// setResolvConfFile was
												// deactivated
				long now = System.currentTimeMillis();
				if (now - _lastCheckedResolvConfFile > 5000L) { // check every 5
																// secs only to
																// avoid a
																// lastModified()
																// inside a
																// synchronized
					// verify the last modified date of the host file and parse
					// again if if file is new
					File file = new File(
							getResolvConfFile());
					long lastModified = file.lastModified();
					_lastCheckedResolvConfFile = now;
					if (lastModified > _lastModifiedResolvConfFile) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("getConfResolvers: reloading resolvers from "
									+ getResolvConfFile());
						}
						_lastModifiedResolvConfFile = lastModified;
						List<Resolver> resolvers = new ArrayList<Resolver>();
						/**
						 * ResolverConfig.refresh (); DOES NOT WORK !!!
						 */
						// --> we need to hack !
						try {
							Class clazz = Class
									.forName("org.xbill.DNS.ResolverConfig");
							java.lang.reflect.Method method = clazz
									.getDeclaredMethod("findUnix");
							method.setAccessible(true);
							java.lang.reflect.Field serversF = clazz
									.getDeclaredField("servers");
							serversF.setAccessible(true);
							serversF.set(ResolverConfig
									.getCurrentConfig(),
									null);
							java.lang.reflect.Field searchF = clazz
									.getDeclaredField("searchlist");
							searchF.setAccessible(true);
							searchF.set(ResolverConfig
									.getCurrentConfig(),
									null);
							method.invoke(ResolverConfig
									.getCurrentConfig(),
									new Object[0]);
						} catch (Throwable t) {
							LOGGER.error(
									"Exception while reloading resolv.conf",
									t);
						}
						String[] servers = ResolverConfig
								.getCurrentConfig()
								.servers();
						if (servers != null) {
							for (String server : servers) {
								try {
									DnsResolver resolver = new DnsResolver(
											server);
									resolver.setTimeout(_timeout);
									resolver.setAlarmWaterMark(_alarmWaterMark);
									resolver.setInstanceName(_instanceName);
									resolvers.add(resolver);
									if (LOGGER
											.isDebugEnabled()) {
										LOGGER.debug("getConfResolvers: add a resolver="
												+ resolver);
									}

								} catch (UnknownHostException uhe) {
									if (LOGGER
											.isDebugEnabled()) {
										LOGGER.debug("getConfResolvers: cannot connect the specified host="
												+ server);
									}
								}

							}
						}
						_resolvers = resolvers;
						// we now update the search paths
						List<Name> searchPaths = new ArrayList<Name>();
						searchPaths.add(Name.root);
						Name[] paths = ResolverConfig
								.getCurrentConfig()
								.searchPath();
						if (paths != null) {
							for (Name path : paths) {
								if (LOGGER.isDebugEnabled()) {
									LOGGER.debug("getConfResolvers: add a search path="
											+ path);
								}
								searchPaths.add(path);
							}
						}
						_searchPaths = searchPaths;
					}
				}
			}
			return _resolvers;
		}
	}

	/**
	 * Gets the search paths in the /etc/resolv.conf file.
	 * 
	 * @return The search paths.
	 */
	public List<Name> getSearchPaths() {
		/**
		 * Always called after getResolvers --> we let getResolvers refresh the
		 * list of search paths
		 */
		synchronized (RESOLV_CONF) {
			return _searchPaths;
		}
	}

	public List<Resolver> getResolvers() {
		List<Resolver> res = new ArrayList<Resolver>();
		res.addAll(_additionalResolvers);
		res.addAll(getConfResolvers());
		return res;
	}

	public boolean isCacheEnabled() {
		return _isCacheEnabled;
	}

	public int ipvMode() {
		return _ipvmode;
	}

	public void setIpvMode(int mode) {
		_ipvmode = mode;
	}

	public RecordCache getCache() {
		return _cache;
	}

	public RecordCache getHostCache() {
		synchronized (HostParser.class) {
			if (_hostsFile != null) {
				long now = System.currentTimeMillis();
				if (now - _lastCheckedHostFile > 5000L) { // check every 5 secs
															// only to avoid a
															// lastModified()
															// inside a
															// synchronized
					// verify the last modified date of the host file and parse
					// again if if file is new
					File file = new File(_hostsFile);
					long lastModified = file.lastModified();
					_lastCheckedHostFile = now;
					if (lastModified > _lastModifiedHostFile) {
						_lastModifiedHostFile = lastModified;
						RecordCache hostCache = new RecordCache();
						HostParser.parse(hostCache,
								_hostsFile);
						_hostCache = hostCache;
					}
				}
			}
			return _hostCache;
		}
	}

	public void setCacheEnabled(boolean enabled) {
		_isCacheEnabled = enabled;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setCacheEnabled: cache="
					+ isCacheEnabled());
		}
	}

	public void setTimeout(int timeout) {
		_timeout = timeout;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setTimeout: timeout=" + _timeout);
		}
	}

	private long _ttl = 0;
	private long _alarmWaterMark = 5000L;
	private String _instanceName;

	public long getNotFoundTTL() {
		return _ttl;
	}

	public void setNotFoundTTL(long ttl) {
		_ttl = ttl;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setNotFoundTTL: ttl=" + _ttl);
		}
	}

	public void setAlarmWaterMark(long seconds) {
		_alarmWaterMark = seconds * 1000L;
	}

	public long getAlarmWaterMark() {
		return _alarmWaterMark;
	}

	public void setInstanceName(String instanceName) {
		_instanceName = instanceName;
	}

	public String getInstanceName() {
		return _instanceName;
	}
}
