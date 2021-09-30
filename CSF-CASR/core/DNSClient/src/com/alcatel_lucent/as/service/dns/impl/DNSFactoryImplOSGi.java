package com.alcatel_lucent.as.service.dns.impl;

import java.util.Dictionary;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel_lucent.as.service.dns.DNSFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(immediate = true,
		service = { DNSFactory.class }, name = "dns", configurationPolicy=ConfigurationPolicy.REQUIRE)
public class DNSFactoryImplOSGi extends DNSFactoryImpl {

	private final static Logger MY_LOGGER = Logger
			.getLogger("dns.impl.impl");

	private static volatile AlarmService _alrmSrv;

	private MeteringService _metering;

	private Dictionary<String, Object> _system;

	public DNSFactoryImplOSGi() {
		if (MY_LOGGER.isDebugEnabled()) {
			MY_LOGGER.debug("new FactoryActivator");
		}
	}

	@Reference(target = "(service.pid=system)", policy = ReferencePolicy.DYNAMIC)
	protected void bindSystemConfig(
			Dictionary<String, Object> config) {
		_system = config;
	}

	protected void unbindSystemConfig(
			Dictionary<String, Object> config) {
	}

	@Reference
	protected void bindManagementService(
			AlarmService mgmtService) {
		_alrmSrv = mgmtService;
	}

	@Reference
	protected void bindMeteringService(
			MeteringService metering) {
		_metering = metering;
	}

	@Activate
	protected void activate(Map<String, Object> config,
			BundleContext ctx) {
		MY_LOGGER.info("activate");
		Meters meters = new Meters().init(_metering);
		meters.start(ctx);
		super.init(config, _system, _alrmSrv);
		_properties.setMeters(meters);
		super.start();
	}

	@Deactivate
	protected void deactivate() {
		MY_LOGGER.info("deactivate");
		super.stop();
	}

	@Modified
	protected void changed(Map<String, Object> config) {
		MY_LOGGER.info("modified with "
				+ config.get("dns.ipvmode"));
		super.changed(config);
	}
}
