package com.alcatel_lucent.as.service.dns.impl;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import com.alcatel_lucent.as.service.dns.RecordDClass;
import com.alcatel_lucent.as.service.dns.RecordSRV;
import com.alcatel_lucent.as.service.dns.RecordType;

public class RequestSRV
		extends Request<RecordSRV> {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger("dns.impl.request.SRV");

	/**
	 * Constructor for this class.
	 * 
	 * @param client The client.
	 * @param query The query.
	 */
	protected RequestSRV(Client client, String query) {
		super(client, query);
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.DNSRequest#getType()
	 */
	public RecordType getType() {
		return RecordType.SRV;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#getDNSType()
	 */
	@Override
	protected int getDNSType() {
		return Type.SRV;
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.impl.Request#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected void fillResults(Message response, List<RecordSRV> results) {
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
				SRVRecord r = (SRVRecord) rr;
				long ttl = r.getTTL();
				String rawData = r.rdataToString();
				int i = rawData.lastIndexOf(' ');
				String target = null;
				if (i > 0 && i != rawData.length() - 1) {
					target = rawData.substring(i + 1);
				}

				RecordSRV record = new RecordSRV(getName(), RecordDClass.IN, ttl, r.getPriority(), r.getWeight(), r.getPort(), target);
				results.add(record);

			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("fillResults: ignore record=" + rr);
			}
		}
		Collections.sort(results);
	}

}
