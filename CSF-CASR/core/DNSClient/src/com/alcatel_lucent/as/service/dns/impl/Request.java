package com.alcatel_lucent.as.service.dns.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Resolver;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel_lucent.as.service.dns.DNSClient;
import com.alcatel_lucent.as.service.dns.DNSListener;
import com.alcatel_lucent.as.service.dns.DNSRequest;
import com.alcatel_lucent.as.service.dns.DNSResponse;
import com.alcatel_lucent.as.service.dns.Record;
import com.alcatel_lucent.as.service.dns.impl.DNSProperties.NsSwitchOption;

public abstract class Request<R extends Record>
		implements DNSRequest<R>, Serializable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	private static char QUOTE = '"';
	private static List NO_RECORD = java.util.Collections.unmodifiableList (new ArrayList(1));

	private Externalizable _attachment = null;
	private String _name = null;
	private Client _client = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param client The client
	 * @param query The query
	 */
	protected Request(Client client, String query) {
		_client = client;
		_name = query;
	}

	/**
	 * Gets the types for JDNI query to get attributes.
	 * 
	 * @return The types.
	 */
	protected abstract int getDNSType();

	/**
	 * Gets the logger.
	 * 
	 * @return The logger.
	 */
	protected abstract Logger getLogger();

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#attach(java.io.Externalizable)
	 */
	public void attach(Externalizable attachment) {
		_attachment = attachment;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#attachment()
	 */
	public Externalizable attachment() {
		return _attachment;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#getDNSClient()
	 */
	public DNSClient getDNSClient() {
		return _client;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#getName()
	 */
	public String getName() {
		return _name;
	}

	public List<R> executeCache(RecordCache cache) {
		if (cache == null)
			return null;
		return (List<R>) cache.get(getType(), getName());
	}

	protected abstract void fillResults(Message response, List<R> results);

	/**
	 * Executes the query on the DNS and fills the result list.
	 * 
	 * @param results The result list to be filled.
	 */
	public void executeDNS(List<R> results, Meters.Set metersSet)
		throws IOException {
		List<Resolver> resolvers = _client.getProperties().getResolvers();
		for (Resolver resolver : resolvers) {
			if (getLogger().isDebugEnabled()) {
				getLogger().debug("executeDNS: resolver=" + resolver);
			}

			Name initialQuery = new Name(getName());
			for (Name path : _client.getProperties().getSearchPaths()) {
				Name query = Name.concatenate(initialQuery, path);
				if (!query.isAbsolute()) {
					query = Name.concatenate(query, Name.root);
				}
				if (getLogger().isDebugEnabled()) {
					getLogger().debug("executeDNS: query name=" + query);
				}

				org.xbill.DNS.Record record = org.xbill.DNS.Record.newRecord(query, getDNSType(), DClass.IN);
				if (getLogger().isDebugEnabled()) {
					getLogger().debug("executeDNS: query record=" + record);
				}
				metersSet._dnsMeter.inc (1);
				Message request = Message.newQuery(record);
				if (getLogger().isDebugEnabled()) {
					getLogger().debug("executeDNS: query request=" + request);
				}
				try {
					Message response = resolver.send(request);
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("executeDNS: reponse=" + response);
					}
					fillResults(response, results);
				}
				catch (IOException ioe) {
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("executeDNS: cannot get response from this query on this resolver -> next search path or next resolver");
					}

				}
				if (!results.isEmpty()) {
					// got the result !
					return;
				}
			}
		}
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#execute()
	 */
	public DNSResponse<R> execute() {
		List<R> results = null;
		DNSProperties properties = _client.getProperties();
		Meters.Set metersSet = properties.getMeters ().getSet (getType ());
		metersSet._reqsMeter.inc (1);

		if (properties.isCacheEnabled()) {
			if (getLogger().isDebugEnabled()) {
				getLogger().debug("execute: search in DNS cache");
			}
			results = executeCache(properties.getCache());
		}
		
		boolean missCache = results == null;
		boolean emptyCache = results == NO_RECORD;

		if (missCache == false){
		    metersSet._cacheHitMeter.inc (1);
		}

		if (missCache || emptyCache){ // in the case of emptyCache -> we still perform FILES check
			for (NsSwitchOption option : properties.getNsSwitchOptions()) {
				if (option == NsSwitchOption.FILES) {
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("execute: search in hosts file");
					}
					results = executeCache(properties.getHostCache());
					if (results != null){
					    metersSet._hostsHitMeter.inc (1);
					    break;
					}
				} else if (option == NsSwitchOption.DNS) {
					if (emptyCache) continue; // emptyCache deactivates dns lookup
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("execute: search in DNS servers");
					}
					results = new ArrayList<R> ();
					try {
					    executeDNS(results, metersSet);
					}
					catch (Exception e) {
						if (getLogger().isEnabledFor(Level.WARN)) {
							getLogger().warn("Cannot get results from the DNS servers", e);
						}
					}
					if (results.isEmpty ()){
						// cache that no response has been found for this request
						if (properties.isCacheEnabled() && properties.getNotFoundTTL() > 0) {
							properties.getCache().put(NO_RECORD, getType(), getName(), properties.getNotFoundTTL());
						}
						metersSet._dnsKOMeter.inc (1);
					} else {
						// cache results if it is OK
						if (properties.isCacheEnabled()) {
							long ttl = results.get(0).getTTL();
							results = (List<R>) java.util.Collections.unmodifiableList (results);
							properties.getCache().put((List<Record>) results, getType(), getName(), ttl);
						}
						metersSet._dnsOKMeter.inc (1);
						break; // dont check FILES
					}
				}
			}
		}

		if (results == null) results = NO_RECORD;
		if (results.isEmpty ()) metersSet._reqsKOMeter.inc (1);
		else metersSet._reqsOKMeter.inc (1);
		DNSResponse<R> response = new Response<R>(this, results);
		return response;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#execute(com.alcatel_lucent.as.service.dns.DNSListener)
	 */
	public void execute(final DNSListener<R> listener) {

		PlatformExecutor thPoolExecutor = PlatformExecutors.getInstance().getThreadPoolExecutor();
		final PlatformExecutor callbackExecutor = PlatformExecutors.getInstance().getCurrentThreadContext().getCallbackExecutor();
		Runnable task = new Runnable() {

			public void run() {

				final DNSResponse<R> res = execute();
				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.dnsRequestCompleted(res);
					}
				};
				callbackExecutor.execute(callbackTask, ExecutorPolicy.SCHEDULE);
			}

		};
		thPoolExecutor.execute(task, ExecutorPolicy.SCHEDULE);
	}

	/**
	 * Unquotes the string.
	 * 
	 * @param s the string to be unquoted.
	 * @return the unquoted string
	 */
	protected String unquote(String s) {
		String res = s;
		if (s != null) {
			if (s.indexOf(QUOTE) == 0) {
				res = s.substring(1, s.lastIndexOf(QUOTE));
			}
		}
		return res;
	}
}
