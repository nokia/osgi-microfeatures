package com.nokia.as.microfeatures.packager.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nokia.as.microfeatures.packager.Packager;
import com.nokia.as.microfeatures.packager.Packager.Params;

//@Component
public class Tester {

	final static Logger _log = Logger.getLogger(Tester.class);

//	final static List<String> _urls = Arrays.asList(
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.configadmin/1.8.16/org.apache.felix.configadmin-1.8.16.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.jsr166e/1.7.0/com.nokia.as.thirdparty.jsr166e-1.7.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.ioh.engine.meters/1.0.5/com.alcatel.as.ioh.engine.meters-1.0.5.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.utils/2.2.4/com.alcatel.as.utils-2.2.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.diagnostics/1.0.1/com.alcatel.as.service.impl.diagnostics-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.sipservlet11/1.1.0/com.alcatel.as.sipservlet11-1.1.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.ioh/1.6.0/com.alcatel.as.ioh-1.6.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.felix.launcher/1.0.3/com.alcatel.as.felix.launcher-1.0.3.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.main/5.6.4/org.apache.felix.main-5.6.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.fileconfig/1.0.2/com.alcatel.as.service.impl.fileconfig-1.0.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.api/1.4.0/com.alcatel.as.service.api-1.4.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/osgi/org.osgi.service.http/1.2.1/org.osgi.service.http-1.2.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.shutdown/1.0.1/com.alcatel.as.service.impl.shutdown-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.json/1.0.1/com.nokia.as.thirdparty.json-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.servlet.api/3.1.2/com.nokia.as.thirdparty.servlet.api-3.1.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.log4j/1.2.18/com.nokia.as.thirdparty.log4j-1.2.18.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.standalone-discovery/1.0.2/com.alcatel.as.service.impl.standalone-discovery-1.0.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.dependencymanager.shell/4.0.5/org.apache.felix.dependencymanager.shell-4.0.5.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.gatewaysutils.configs.environment/1.0.2/com.alcatel.as.gatewaysutils.configs.environment-1.0.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel_lucent.as.service.dns/1.0.1/com.alcatel_lucent.as.service.dns-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.gmetric4j/1.0.4/com.nokia.as.thirdparty.gmetric4j-1.0.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.osgi.jre18/1.0.0/com.nokia.as.osgi.jre18-1.0.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.bundleinstaller.impl/1.0.2/com.alcatel.as.service.bundleinstaller.impl-1.0.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.features.felix/1.0.4/com.nokia.as.features.felix-1.0.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.features.common/1.0.4/com.nokia.as.features.common-1.0.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.eventadmin/1.4.8/org.apache.felix.eventadmin-1.4.8.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel_lucent.as.management.annotation/1.0.3/com.alcatel_lucent.as.management.annotation-1.0.3.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.gogo.shell/0.12.0/org.apache.felix.gogo.shell-0.12.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.gogo.runtime/0.16.2/org.apache.felix.gogo.runtime-0.16.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.dependencymanager.runtime/4.0.5/org.apache.felix.dependencymanager.runtime-4.0.5.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.gs/1.0.1/com.alcatel.as.service.impl.gs-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.concurrent/1.0.2/com.alcatel.as.service.impl.concurrent-1.0.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.ioh.impl.core/2.2.1/com.alcatel.as.ioh.impl.core-2.2.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.features.runtime.minimal/1.0.4/com.nokia.as.features.runtime.minimal-1.0.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.metatype/1.1.2/org.apache.felix.metatype-1.1.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.xstream/1.4.9/com.nokia.as.thirdparty.xstream-1.4.9.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.thirdparty.dependencymanager/4.4.2/com.nokia.as.thirdparty.dependencymanager-4.4.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.util.jartool/1.0.0/com.nokia.as.util.jartool-1.0.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.scr/2.0.12/org.apache.felix.scr-2.0.12.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.scr.compat/1.0.4/org.apache.felix.scr.compat-1.0.4.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.gogo.command/0.16.0/org.apache.felix.gogo.command-0.16.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nextenso.mux/2.2.0/com.nextenso.mux-2.2.0.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.mux.impl/2.2.1/com.alcatel.as.mux.impl-2.2.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.metering.impl/1.0.1/com.alcatel.as.service.metering.impl-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.felix.cmd/1.0.1/com.alcatel.as.felix.cmd-1.0.1.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.as.dtls.provider/1.0.2/com.nokia.as.dtls.provider-1.0.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/javax/mail/mail/1.4.7/mail-1.4.7.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/org/apache/felix/org.apache.felix.shell/1.4.3/org.apache.felix.shell-1.4.3.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.metering2.impl/1.1.2/com.alcatel.as.service.metering2.impl-1.1.2.jar",
//			"http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.alcatel.as.service.impl.log4j/1.0.2/com.alcatel.as.service.impl.log4j-1.0.2.jar");

	final static List<String> _urls = Arrays.asList(
			"file:///home/pderop/Tmp/org.apache.felix.configadmin-1.8.16.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.jsr166e-1.7.0.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.ioh.engine.meters-1.0.5.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.utils-2.2.4.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.diagnostics-1.0.1.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.sipservlet11-1.1.0.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.ioh-1.6.0.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.felix.launcher-1.0.3.jar",
			"file:///home/pderop/Tmp/org.apache.felix.main-5.6.4.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.fileconfig-1.0.2.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.api-1.4.0.jar",
			"file:///home/pderop/Tmp/org.osgi.service.http-1.2.1.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.shutdown-1.0.1.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.json-1.0.1.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.servlet.api-3.1.2.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.log4j-1.2.18.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.standalone-discovery-1.0.2.jar",
			"file:///home/pderop/Tmp/org.apache.felix.dependencymanager.shell-4.0.5.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.gatewaysutils.configs.environment-1.0.2.jar",
			"file:///home/pderop/Tmp/com.alcatel_lucent.as.service.dns-1.0.1.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.gmetric4j-1.0.4.jar",
			"file:///home/pderop/Tmp/com.nokia.as.osgi.jre18-1.0.0.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.bundleinstaller.impl-1.0.2.jar",
			"file:///home/pderop/Tmp/com.nokia.as.features.felix-1.0.4.jar",
			"file:///home/pderop/Tmp/com.nokia.as.features.common-1.0.4.jar",
			"file:///home/pderop/Tmp/org.apache.felix.eventadmin-1.4.8.jar",
			"file:///home/pderop/Tmp/com.alcatel_lucent.as.management.annotation-1.0.3.jar",
			"file:///home/pderop/Tmp/org.apache.felix.gogo.shell-0.12.0.jar",
			"file:///home/pderop/Tmp/org.apache.felix.gogo.runtime-0.16.2.jar",
			"file:///home/pderop/Tmp/org.apache.felix.dependencymanager.runtime-4.0.5.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.gs-1.0.1.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.concurrent-1.0.2.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.ioh.impl.core-2.2.1.jar",
			"file:///home/pderop/Tmp/com.nokia.as.features.runtime.minimal-1.0.4.jar",
			"file:///home/pderop/Tmp/org.apache.felix.metatype-1.1.2.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.xstream-1.4.9.jar",
			"file:///home/pderop/Tmp/com.nokia.as.thirdparty.dependencymanager-4.4.2.jar",
			"file:///home/pderop/Tmp/com.nokia.as.util.jartool-1.0.0.jar",
			"file:///home/pderop/Tmp/org.apache.felix.scr-2.0.12.jar",
			"file:///home/pderop/Tmp/org.apache.felix.scr.compat-1.0.4.jar",
			"file:///home/pderop/Tmp/org.apache.felix.gogo.command-0.16.0.jar",
			"file:///home/pderop/Tmp/com.nextenso.mux-2.2.0.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.mux.impl-2.2.1.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.metering.impl-1.0.1.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.felix.cmd-1.0.1.jar",
			"file:///home/pderop/Tmp/com.nokia.as.dtls.provider-1.0.2.jar",
			"file:///home/pderop/Tmp/mail-1.4.7.jar",
			"file:///home/pderop/Tmp/org.apache.felix.shell-1.4.3.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.metering2.impl-1.1.2.jar",
			"file:///home/pderop/Tmp/com.alcatel.as.service.impl.log4j-1.0.2.jar");
	@ServiceDependency
	Packager _packager;

	@Start
	void start() throws MalformedURLException {
		try {
			PropertyConfigurator log4jConf = new PropertyConfigurator();
			log4jConf.configure("conf/log4j.properties");

			_log.warn("Starting to test packager");

			List<URL> urls = new ArrayList<>();
			for (String u : _urls) {
				urls.add(new URL(u));
			}

			Map<Params, Object> params = new HashMap<>();
			params.put(Params.TARGET, "/tmp/runtime/test-1.0.0");
			CompletableFuture<Path> zip = _packager.packageRuntime(urls, params);
			zip.thenAccept(path -> {
				_log.warn("Test done: zip file=" + path);
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}
