package com.alcatel_lucent.as.service.dns;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

/**
 * This is a utility Class used to perform basic DNS handling
 * 
 * use OSGI DNSFactory injection instead
 */

public class DNSHelper {

	/**
	 * The Listener interface for this helper..
	 * 
	 * @param <R>
	 *            The record class.
	 */
	public interface Listener<R extends Record> {

		/**
		 * Called method when results are available.
		 * 
		 * @param query
		 *            The query (hostname or address).
		 * @param records
		 *            The response record list (it can be empty).
		 */
		public void requestCompleted(String query,
				List<R> records);
	}

	/**
	 * The logger for this helper ("dns.helper");
	 */
	public final static Logger LOGGER = Logger
			.getLogger("dns.helper");

	private DNSHelper() {
	}

	/**
	 * Asynchronously returns the list of IP addresses associated to the given
	 * host name. This method performs 'A' and 'AAAA' record lookups in DNS.
	 * 
	 * @param hostname
	 *            The host name.
	 * @param listener
	 *            The object called when the response is available.
	 * @deprecated use Helper.getByName OSGI service
	 */
	public static void getHostByName(final String hostname,
			final Listener<RecordAddress> listener) {
		PlatformExecutor thPoolExecutor = PlatformExecutors
				.getInstance().getThreadPoolExecutor();
		final PlatformExecutor callbackExecutor = PlatformExecutors
				.getInstance().getCurrentThreadContext()
				.getCallbackExecutor();

		Runnable task = new Runnable() {

			public void run() {
				final List<RecordAddress> res = getHostsByName(hostname);
				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.requestCompleted(hostname,
								res);
					}
				};
				callbackExecutor.execute(callbackTask,
						ExecutorPolicy.SCHEDULE);
			}
		};

		thPoolExecutor.execute(task,
				ExecutorPolicy.SCHEDULE);
	}

	/**
	 * Synchronously returns the list of IP addresses associated to the given
	 * host name. This method performs 'A' and 'AAAA' record lookups in DNS.
	 * 
	 * @param hostname
	 *            The host name.
	 * @return A list with the matching IP addresses or an empty array if the
	 *         DNS request failed for any reason (no match, timeout...).
	 * 
	 * @deprecated use Helper.getByName OSGI Service
	 */
	public static List<RecordAddress> getHostByName(
			String hostname) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getHostByName: hostname="
					+ hostname);
		}
		List<RecordAddress> addresses = getHostsByName(hostname);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getHostByName: res="
					+ addresses.toString());
		}
		return addresses;
	}

	/**
	 * Synchronously returns the list of records associated to the given host
	 * name. This method performs a 'A' and 'AAAA' record lookup in DNS.
	 * 
	 * @param hostname
	 *            The host name.
	 * @return A list with the matching IP addresses or an empty array if the
	 *         DNS request failed for any reason (no match, timeout...).
	 */
	private static List<RecordAddress> getHostsByName(
			String hostname) {

		DNSFactory factory = DNSFactory.getInstance();
		DNSClient client = factory.newDNSClient();
		int _mode = client.mode();
		LOGGER.info("lookup " + _mode);
		List<RecordAddress> res = new ArrayList<RecordAddress>();
		if (_mode == 4 || _mode > 6) {
			// gets the IPV4 records
			DNSRequest<RecordA> requestA = client
					.newDNSRequest(hostname, RecordA.class);
			DNSResponse<RecordA> responseA = requestA
					.execute();
			for (RecordA ra : responseA.getRecords()) {
				res.add(ra);
			}
		}
		if (_mode >= 6) {
			// gets the IPV6 records
			DNSRequest<RecordAAAA> requestAAAA = client
					.newDNSRequest(hostname,
							RecordAAAA.class);
			DNSResponse<RecordAAAA> responseAaaa = requestAAAA
					.execute();
			for (RecordAAAA ra : responseAaaa.getRecords()) {
				res.add(ra);
			}
		}
		if (res.isEmpty()) {
			DNSRequest<RecordCName> requestCName = client
					.newDNSRequest(hostname,
							RecordCName.class);

			DNSResponse<RecordCName> responseCName = requestCName
					.execute();
			for (RecordCName ra : responseCName
					.getRecords()) {
				String alias = ra.getAlias();
				res.addAll(getHostByName(alias));
			}

		}
		return res;
	}
}