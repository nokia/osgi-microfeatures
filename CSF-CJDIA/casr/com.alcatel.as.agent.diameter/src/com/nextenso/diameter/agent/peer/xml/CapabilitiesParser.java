package com.nextenso.diameter.agent.peer.xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.diameter.agent.peer.Capabilities;
import com.nextenso.proxylet.diameter.DiameterApplication;

public class CapabilitiesParser
		extends DiameterXMLUtils {

	private static String DTD_CONTEXT = "diameterAgent/diameterCapabilities.dtd";

	// Context tags
	//	private  static final String TAG_CAPABILITIES = "capabilities";
	//private static final String TAG_ACCT_APPLICATION = "acct-application";
	private static final String TAG_RELAY = "relay";
	private static final String TAG_AUTH_APPLICATION = "auth-application";

	private static final String ATTR_ID = "id";
	private static final String ATTR_VENDOR_ID = "vendor-id";
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.capabilities.parser");

	/**
	 * Constructor for this class.
	 */
	public CapabilitiesParser() {
		super();
	}

	/**
	 * Parses the configuration.
	 * 
	 * @param xml The xml.
	 * @param capability The capability.
	 * @throws ConfigException if a XML error occurs.
	 */
	public void parse(String xml, Capabilities capability)
		throws ConfigException {
		Document document = parse(xml, DTD_CONTEXT);
		Element capabilitiesElt = document.getDocumentElement();

		if (getChildElement(capabilitiesElt, TAG_RELAY) != null) {
			capability.setRelay(true);
			return;
		}

		List<DiameterApplication> applications = new ArrayList<DiameterApplication>();

		List<Element> apps = getChildElements(capabilitiesElt);
		for (Element e : apps) {
			boolean isAuth = (e.getTagName().equals(TAG_AUTH_APPLICATION));
			String s = e.getAttribute(ATTR_VENDOR_ID);
			long vendorId = -1;
			if (s != null && s.length() > 0) {
				vendorId = parseLong(s);
			}
			long applicationId = parseLong(e.getAttribute(ATTR_ID));
			DiameterApplication app = new DiameterApplication(applicationId, vendorId, isAuth);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("parse: new application:" + app);
			}
			applications.add(app);
		}

		capability.setSupportedApplications(applications);
	}

}
