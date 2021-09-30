package com.alcatel_lucent.as.service.dns.impl;

import java.util.List;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import com.alcatel_lucent.as.service.dns.RecordAAAA;
import com.alcatel_lucent.as.service.dns.RecordDClass;
import com.alcatel_lucent.as.service.dns.RecordType;

public class RequestAAAA
		extends Request<RecordAAAA> {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger("dns.impl.request.AAAA");

	/**
	 * Constructor for this class.
	 * 
	 * @param client The client
	 * @param query The query.
	 */
	protected RequestAAAA(Client client, String query) {
		super(client, query);
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#getType()
	 */
	public RecordType getType() {
		return RecordType.AAAA;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#getDNSType()
	 */
	@Override
	protected int getDNSType() {
		return Type.AAAA;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected void fillResults(Message response, List<RecordAAAA> results) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("fillResults: response=" + response);
		}
		if (response == null || results == null) {
			return;
		}

		Record[] records = response.getSectionArray(Section.ANSWER);
		for (Record r : records) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("fillResults: process record=" + r);
			}

			if (r.getType() == getDNSType()) {
				long ttl = r.getTTL();
				String address = r.rdataToString();
				RecordAAAA record = new RecordAAAA(getName(), RecordDClass.IN, ttl, address);
				results.add(record);
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("fillResults: ignore record=" + r);
			}

		}
	}

}
