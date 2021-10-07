package com.alcatel_lucent.as.service.dns;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.TelURL;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

/**
 * This is a utility Class used to perform transformation from a TelURL to SIP
 * URI DNS handling.
 * 
 * It currently provides support for RFC 3966 and RFC5341.
 * 
 * 
 * <h3>Example of API usage</h3>
 * 
 * <h4>Synchronously make a resolution</h4>
 * 
 * <ul>
 * <CODE><PRE>
   TelURL tel=...
   String sipuri = EnumHelper.resolve(tel);
 </PRE></CODE>
 * </ul>
 * 
 * <h4>Asynchronously make a resolution</h4>
 * 
 * This example is shown for in a SIP servlet context:
 * <ul>
 * <CODE><PRE>
 class EnumListener implements EnumHelper.Listener  {
   private SipServletRequest _request = null;
   
   public EnumListener(SipServletRequest request) {
     _request = request;
   }
   
   public void completed(TelURL tel, String uri) {
     int code = 200;
   	 ...
   	 SipServletResponse response = _request.createResponse(code);
   	 ...
   	 response.send();
   }
 }
</PRE></CODE>
 * </ul>
 * In the servlet doRequest() method:
 * <ul>
 * <CODE><PRE>
   TelURL tel=...
   EnumHelper.Listener listener = new EnumListener(myRequest);
   EnumHelper.resolve(tel, listener);

 </PRE></CODE>
 * </ul>
 */
public class EnumHelper {
	/**
	 * The logger for this helper ("dns.helper.enum");
	 */
	public final static Logger LOGGER = Logger
			.getLogger("dns.helper.enum");

	private static final String ARPA_SUFFIX = ".arpa";
	public static final String E212 = "e212";
	public static final String E164 = "e164";
	private static final char DIGIT_SEPARATOR = '.';
	public static final String U_FLAG = "u";
	public static final String S_FLAG = "s";
	private static final char GLOBAL_CHAR = '+';
	public static final String AAA_SERVICE = "aaa";
	public static final String E2U_SERVICE = "E2U";
	public static final String SIP_SERVICE = "sip";
	private static final String REGEXP_TERM1 = "(\\\\\\\\)";
	private static final String REGEXP_TERM2 = "\\\\";

	private static final String REGEXP_SPLIT = "\\!";

	private static final String PHONE_CONTEXT_PARAMATER = "phone-context";

	// Constances for the replacement of the format of the capturing group
	private static final String REPLACEMENT_REGEXP = "\\\\([1-9])";
	private static final String REPLACEMENT_REPLACEMENT = "\\$$1";

	protected EnumHelper() {
	}

	/**
	 * The listener for asynchronous tel URL resolution.
	 */
	public interface Listener {

		/**
		 * Called when the resolution is completed.
		 * 
		 * @param tel
		 *            The Tel URL.
		 * @param uri
		 *            The resolved SIP URI.
		 */
		public void completed(TelURL tel, String uri);
	}

	/**
	 * The listener for asynchronous tel URL resolution with naptr
	 */
	public interface DomainListener {

		/**
		 * Called when the resolution is completed.
		 * 
		 * @param subdomain
		 *            ex : 63133957218
		 * 
		 * @param plan
		 *            ex : e164
		 * @param values
		 *            a list of the NAPTR records as result
		 */
		public void completed(String subdomain, String plan,
				List<RecordNAPTR> result);
	}

	/**
	 * asynchronously returns a sorted list of records in a listener
	 * callback<br>
	 * i.e <br>
	 * resolve("1234",EnumHelper.E164, EnumHelper.S,EnumHelper.AAA_SERVICE, new
	 * DomainListener() { <br>
	 * void completed(String subdomain,String arpa,List<RecordNAPTR> urns) {
	 * <br>
	 * &nbsp;&nbsp;for (RecordNAPTR urn:urns) { <br>
	 * &nbsp;&nbsp;&nbsp; urn.getTTL() ... // do any logic <br>
	 * &nbsp;&nbsp;} <br>
	 * } <br>
	 * }); <br>
	 * <br>
	 * will perform a NAPTR request over 4.3.2.1.e164.arpa. <br>
	 * and perform the results with sorting and filtering only "s" flag and
	 * "aaa" service
	 * 
	 * @param domain
	 *            mandatory, a domain to resolve. Can be a subdomain if a
	 *            numbering plan is used
	 * @param numberingplan
	 *            when not null, completes the subdomain and trigger a
	 *            conversion with dot.
	 * @param flag
	 *            keep only records with flag (null for all records)
	 * @param service
	 *            keep only records with service (null for all records)
	 * @param listener
	 *            the domain listener for result processing
	 */
	public static void resolve(final String subdomain,
			final String plan, final String flag,
			final String service,
			final DomainListener listener) {
		PlatformExecutor thPoolExecutor = PlatformExecutors
				.getInstance().getThreadPoolExecutor();
		final PlatformExecutor callbackExecutor = PlatformExecutors
				.getInstance().getCurrentThreadContext()
				.getCallbackExecutor();
		Runnable task = new Runnable() {

			public void run() {
				final List<RecordNAPTR> urns = resolve(
						subdomain, plan, flag, service);
				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.completed(subdomain, plan,
								urns);
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
	 * Asynchronously returns the Sip Uri after the resolution of the Tel URL.
	 * 
	 * @param tel
	 *            The Tel URL.
	 * @param listener
	 *            The listener for result process.
	 */
	public static void resolve(final TelURL tel,
			final Listener listener) {
		PlatformExecutor thPoolExecutor = PlatformExecutors
				.getInstance().getThreadPoolExecutor();
		final PlatformExecutor callbackExecutor = PlatformExecutors
				.getInstance().getCurrentThreadContext()
				.getCallbackExecutor();
		Runnable task = new Runnable() {

			public void run() {
				final String uri = resolve(tel);
				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.completed(tel, uri);
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
	 * synchronously returns a sorted list of records <br>
	 * i.e <br>
	 * resolve("1234",EnumHelper.E164, EnumHelper.S,EnumHelper.AAA_SERVICE);
	 * <br>
	 * <br>
	 * will perform a NAPTR request over 4.3.2.1.e164.arpa. <br>
	 * and perform the results with sorting and filtering only "s" flag and
	 * "aaa" service
	 * 
	 * @param domain
	 *            mandatory, a domain to resolve. Can be a subdomain if a
	 *            numbering plan is used
	 * @param numberingplan
	 *            when not null, completes the subdomain and trigger a
	 *            conversion with dot.
	 * @param flag
	 *            keep only records with flag (null for all records)
	 * @param service
	 *            keep only records with service (null for all records)
	 * @return a list of NAPTR records
	 */
	public static List<RecordNAPTR> resolve(String domain,
			String plan, String flag, String service) {
		return resolve(
				plan == null ? domain
						: toDomainName(domain, plan),
				flag, service);
	}

	/**
	 * quick helper to convert list record into list of replacement
	 * 
	 * @param records
	 *            list of record NATPR
	 * @return replacements list of replacement
	 */
	public static List<String> replacements(
			List<RecordNAPTR> records) {
		List<String> res = new ArrayList<String>();
		for (RecordNAPTR record : records) {
			res.add(record.getReplacement());
		}
		return res;
	}

	private static List<RecordNAPTR> resolve(String domain,
			String flag, String service) {
		List<RecordNAPTR> res = new ArrayList<RecordNAPTR>();

		DNSFactory factory = DNSFactory.getInstance();
		if (factory == null) {
			LOGGER.error(
					"resolve: cannot get an instance of DNSFactory");
			return null;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
					"resolve: querying NAPTR record from DNS for domain "
							+ domain);
		}
		DNSClient client = factory.newDNSClient();

		try {
			DNSRequest<RecordNAPTR> request = client
					.newDNSRequest(domain,
							RecordNAPTR.class);
			DNSResponse<RecordNAPTR> response = request
					.execute();
			for (RecordNAPTR record : response
					.getRecords()) {
				String naptrFlags = record.getFlags();
				String naptrService = record.getService();
				if (flag == null || naptrFlags
						.equalsIgnoreCase(flag)) {
					if (service == null || naptrService
							.indexOf(service) >= 0) {
						res.add(record);
					}
				}
			}
		} finally {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("resolve: res=" + res);
			}
		}
		return res;
	}

	/**
	 * Synchronously returns the Sip Uri after the resolution of the Tel URL.
	 * 
	 * @param tel
	 *            The Tel URL.
	 * @return The SIP URI or null if the Tel URL cannot be resolved.
	 */
	public static String resolve(TelURL tel) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("resolve: tel url=" + tel);
		}
		if (tel == null) {
			return null;
		}

		String res = null;

		DNSFactory factory = DNSFactory.getInstance();
		if (factory == null) {
			LOGGER.error(
					"resolve: cannot get an instance of DNSFactory");
			return null;
		}
		String domain = toDomainName(tel);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
					"resolve: querying NAPTR record from DNS for domain "
							+ domain);
		}
		DNSClient client = factory.newDNSClient();
		DNSRequest<RecordNAPTR> request = client
				.newDNSRequest(domain, RecordNAPTR.class);
		DNSResponse<RecordNAPTR> response = request
				.execute();

		try {
			for (RecordNAPTR record : response
					.getRecords()) {
				String naptrFlags = record.getFlags();
				String naptrService = record.getService();
				if (naptrFlags.equalsIgnoreCase(U_FLAG)
						&& naptrService
								.indexOf(E2U_SERVICE) >= 0
						&& naptrService.indexOf(
								SIP_SERVICE) >= 0) {
					String rexp = record.getRegexp();
					rexp = rexp.replaceAll(REGEXP_TERM1,
							REGEXP_TERM2); // un-backslashify!!!

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(
								"resolve: found ENUM DNS record matching "
										+ tel
										+ " with regexp="
										+ rexp);
					}

					String[] terms = rexp
							.split(REGEXP_SPLIT);

					res = getGlobalPN(tel);
					if (terms.length > 1) {
						terms[2] = terms[2].replaceAll(
								REPLACEMENT_REGEXP,
								REPLACEMENT_REPLACEMENT);
						res = res.replaceAll(terms[1],
								terms[2]);
					}
					return res;
				}
			}
			return res;
		} finally {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("resolve: res=" + res);
			}
		}
	}

	/**
	 * 
	 * @param tel
	 * @return
	 */
	private static String getGlobalPN(TelURL tel) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getGlobalPN: isGlobal="
					+ tel.isGlobal());
		}

		StringBuilder res = new StringBuilder();
		if (!tel.isGlobal()) {
			// RFC 3966 #5.1.5:
			// If a <local-phone-number> is used, an <area-specifier> MUST be
			// included as well.
			String area = tel
					.getParameter(PHONE_CONTEXT_PARAMATER);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"getGlobalPN: area (phone-context)="
								+ area);
			}
			if (area != null && area.length() > 0) {
				if (area.charAt(0) == GLOBAL_CHAR) {
					// global prefix
					res.append(area)
							.append(tel.getPhoneNumber());
					return res.toString();
				}
				// local prefix
				// TODO: add "+"+<country code>
				res.append(area)
						.append(tel.getPhoneNumber());
				return res.toString();
			}
			return tel.getPhoneNumber();
		}
		res.append(GLOBAL_CHAR)
				.append(tel.getPhoneNumber());
		return res.toString();
	}

	private static String toDomainName(String domain,
			String arpa) {
		StringBuilder res = new StringBuilder();
		for (int i = domain.length(); --i >= 0;) {
			char c = domain.charAt(i);
			if (Character.isDigit(c)) {
				res.append(c).append(DIGIT_SEPARATOR);
			}
		}
		// 4. Append the string ".e164.arpa" to the end. Example:
		// 8.4.1.0.6.4.9.7.0.2.4.4.e164.arpa
		res.append(arpa + ARPA_SUFFIX);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("toDomainName: res= " + res);
		}
		return res.toString();
	}

	/**
	 * Converts the Tel URL into a domain name according to RFC 3761 section
	 * 2.4.
	 * 
	 * @param tel
	 *            The telephone URL to convert into domain name
	 * @return The domain name which can be used to request NAPTR record.
	 */
	private static String toDomainName(TelURL tel) {
		return toDomainName(getGlobalPN(tel), E164);
	}

	static {
		String domain = System.getProperty("enum.domain");
		if (domain != null)
			try {
				main(new String[] { domain });
			} catch (Throwable e) {
				System.out.println("failure" + e);
			}

	}

	static public void main(String[] args)
			throws Throwable {
		String domain = args[0];
		if (domain != null) {
			System.out.println(
					"EnumHelper : testing resolving for "
							+ domain);
			List<String> results = replacements(resolve(
					domain, E164, U_FLAG, AAA_SERVICE));
			System.out
					.println("EnumHelper : ..." + results);
			Thread.sleep(10000);
		}
	}
}