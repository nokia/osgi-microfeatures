package com.alcatel_lucent.as.service.dns.impl;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import com.alcatel_lucent.as.service.dns.RecordA;
import com.alcatel_lucent.as.service.dns.RecordDClass;
import com.alcatel_lucent.as.service.dns.RecordNAPTR;
import com.alcatel_lucent.as.service.dns.RecordType;

public class RequestNAPTR
		extends Request<RecordNAPTR> {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger("dns.impl.request.NAPTR");

	/**
	 * Constructor for this class.
	 * 
	 * @param client The client.
	 * @param query The query.
	 */
	protected RequestNAPTR(Client client, String query) {
		super(client, query);
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#getType()
	 */
	public RecordType getType() {
		return RecordType.NAPTR;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#getDNSType()
	 */
	@Override
	protected int getDNSType() {
		return Type.NAPTR;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#fillResults(org.xbill.DNS.Message,
	 *      java.util.List)
	 */
	@Override
	protected void fillResults(Message response, List<RecordNAPTR> results) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("fillResults: response=" + response);
		}
		if (response == null || results == null) {
			return;
		}

		Record[] records = response.getSectionArray(Section.ANSWER);
		for (Record rr : records) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("fillResults: process record=" + rr);
			}
			if (rr.getType() == getDNSType()) {
				NAPTRRecord r = (NAPTRRecord) rr;
				long ttl = r.getTTL();
				String rawData = r.rdataToString();
				int i = rawData.lastIndexOf(' ');
				String replacement = null;
				if (i > 0 && i != rawData.length() - 1) {
					replacement = rawData.substring(i + 1);
				}
				RecordNAPTR record = new RecordNAPTR(getName(), RecordDClass.IN, ttl, r.getOrder(), r.getPreference(), r.getFlags(), r.getService(), r.getRegexp(), replacement);
				results.add(record);
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("fillResults: ignore record=" + rr);
			}

		}
		Collections.sort(results);

	}

}
