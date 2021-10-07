package com.alcatel_lucent.as.service.dns.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel_lucent.as.service.dns.DNSClient;
import com.alcatel_lucent.as.service.dns.DNSFactory;
import com.alcatel_lucent.as.service.dns.DNSHelper.Listener;
import com.alcatel_lucent.as.service.dns.DNSRequest;
import com.alcatel_lucent.as.service.dns.DNSResponse;
import com.alcatel_lucent.as.service.dns.Helper;
import com.alcatel_lucent.as.service.dns.RecordA;
import com.alcatel_lucent.as.service.dns.RecordAAAA;
import com.alcatel_lucent.as.service.dns.RecordAddress;
import com.alcatel_lucent.as.service.dns.RecordCName;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate = true, service = { Helper.class })
public class DNSHelperOSGi implements Helper {
	PlatformExecutor _thPoolExecutor;
	PlatformExecutor _callbackExecutor;
	DNSFactory _factory;
	DNSClient _client;

	public DNSHelperOSGi() {
	}

	@org.osgi.service.component.annotations.Reference
	protected void bindDnsFactory(DNSFactory factory) {
		_factory = factory;
	}

	protected void unbindDnsFactory(DNSFactory factory) {
	}

	@org.osgi.service.component.annotations.Reference
	protected void bindExecutors(PlatformExecutors executors) {
		_thPoolExecutor = executors
				.getIOThreadPoolExecutor();
		_callbackExecutor = executors
				.getCurrentThreadContext()
				.getCallbackExecutor();
	}

	@Activate
	protected void activate(BundleContext ctx) {

	}

	@Deactivate
	protected void deactivate() {
	}

	@Override
	public List<RecordAddress> getByName(String hostname) {
		DNSClient client = _factory.newDNSClient();
		int _mode = client.mode();
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
				res.addAll(getByName(alias));
			}

		}
		return res;
	}

	@Override
	public void getByName(final String hostname,
			final Listener<RecordAddress> listener) {

		Runnable task = new Runnable() {

			public void run() {
				final List<RecordAddress> res = getByName(hostname);
				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.requestCompleted(hostname,
								res);
					}
				};
				_callbackExecutor.execute(callbackTask,
						ExecutorPolicy.SCHEDULE);
			}
		};
		_thPoolExecutor.execute(task,
				ExecutorPolicy.SCHEDULE);
	}
}
