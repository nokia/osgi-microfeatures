// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.engine.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import alcatel.tess.hometop.gateways.utils.Config;

import com.nextenso.proxylet.engine.criterion.CalendarCriterion;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.CriterionManager;
import com.nextenso.proxylet.engine.criterion.CriterionWrapper;
import com.nextenso.proxylet.engine.criterion.DateCriterion;
import com.nextenso.proxylet.engine.criterion.DayCriterion;
import com.nextenso.proxylet.engine.criterion.FromCriterion;
import com.nextenso.proxylet.engine.criterion.LogicalANDCriterion;
import com.nextenso.proxylet.engine.criterion.LogicalNOTCriterion;
import com.nextenso.proxylet.engine.criterion.LogicalORCriterion;
import com.nextenso.proxylet.engine.criterion.MessageAttrCriterion;
import com.nextenso.proxylet.engine.criterion.MonthCriterion;
import com.nextenso.proxylet.engine.criterion.TimeCriterion;
import com.nextenso.proxylet.engine.criterion.TrueCriterion;
import com.nextenso.proxylet.engine.criterion.UntilCriterion;
import com.nextenso.proxylet.engine.xml.XMLConfigException;
import com.nextenso.proxylet.engine.xml.XMLUtils;
import com.nextenso.radius.agent.engine.criterion.ClientIPCriterion;
import com.nextenso.radius.agent.engine.criterion.ClientMaskCriterion;

public class RadiusXMLUtils
		extends XMLUtils {

	private static final String PUBLIC_ID_CONTEXT = "-//NEXTENSO//DTD CONTEXT 1.1//EN";

	private  static final String DTD_CONTEXT = "radiusplugin/radiusContext.dtd";

	/** Context tags */
	private static final String CRITERION_MESSAGE_ATTR = "message-attr";
	private static final String CRITERION_CLIENT_IP = "client-ip";
	private static final String CRITERION_CLIENT_MASK = "client-mask";

	protected static CriterionWrapper parseCriterion(Element element, String contextId)
		throws XMLConfigException {
		CriterionWrapper wrapper = new CriterionWrapper();
		ArrayList children = getChildElements(element);
		for (int i = 0; i < children.size(); i++) {
			Element e = (Element) children.get(i);
			String tag = e.getTagName();
			if (CRITERION_NAME.equals(tag)) {
				String name = contextId + '-' + getString(e, true, "Missing criterion name for context: " + contextId);
				wrapper.setName(name);
			} else if (DESCRIPTION.equals(tag)) {
				String desc = getString(e, false, null);
				wrapper.setDescription(desc);
			} else if (CRITERION_VALUE.equals(tag)) {
				List<Criterion> crits = parseCriterionValue(e, contextId);
				Criterion c = crits.get(0);
				wrapper.setCriterion(c);
				//System.err.println("criterion: "+c);
				CriterionManager.registerCriterion(wrapper.getName(), c);
			}
		}
		return wrapper;
	}

	protected static List<Criterion> parseCriterionValue(Element element, String contextId)
		throws XMLConfigException {
		ArrayList<Criterion> criterions = new ArrayList<Criterion>();
		try {
			ArrayList children = getChildElements(element);
			for (int i = 0; i < children.size(); i++) {
				Element e = (Element) children.get(i);
				Criterion criterion = null;
				String tag = e.getTagName();
				if (CRITERION_AND.equals(tag)) {
					List<Criterion> crits = parseCriterionValue(e, contextId);
					for (int j = 0; j < crits.size(); j++) {
						Criterion tmp = crits.get(j);
						if (j == 0) {
							criterion = tmp;
						} else {
							criterion = LogicalANDCriterion.getInstance(criterion, tmp);
						}
					}
				} else if (CRITERION_OR.equals(tag)) {
					List<Criterion> crits = parseCriterionValue(e, contextId);
					for (int j = 0; j < crits.size(); j++) {
						Criterion tmp = crits.get(j);
						if (j == 0) {
							criterion = tmp;
						} else {
							criterion = LogicalORCriterion.getInstance(criterion, tmp);
						}
					}
				} else if (CRITERION_NOT.equals(tag)) {
					Criterion tmp = parseCriterionValue(e, contextId).get(0);
					criterion = LogicalNOTCriterion.getInstance(tmp);
				} else if (CRITERION_REF.equals(tag)) {
					String ref = getString(e, true, "Missing reference value in criterion definition");
					criterion = CriterionManager.getCriterion(contextId + '-' + ref);
					if (criterion == null) {
						throw new XMLConfigException("Invalid reference value (" + ref + ")");
					}
				} else if (CRITERION_ALL.equals(tag)) {
					criterion = TrueCriterion.getInstance();
				} else if (CRITERION_DAY.equals(tag)) {
					String day = getString(e, true, "Missing day value in criterion definition");
					criterion = DayCriterion.getInstance(day);
				} else if (CRITERION_DATE.equals(tag)) {
					String date = getString(e, true, "Missing date value in criterion definition");
					criterion = DateCriterion.getInstance(date);
				} else if (CRITERION_MONTH.equals(tag)) {
					String month = getString(e, true, "Missing month value in criterion definition");
					criterion = MonthCriterion.getInstance(month);
				} else if (CRITERION_TIME.equals(tag)) {
					String time = getString(e, true, "Missing time value in criterion definition");
					criterion = TimeCriterion.getInstance(time);
				} else if (CRITERION_FROM.equals(tag)) {
					List<Criterion> crits = parseCriterionValue(e, contextId);
					CalendarCriterion tmp = (CalendarCriterion) crits.get(0);
					criterion = FromCriterion.getInstance(tmp);
				} else if (CRITERION_UNTIL.equals(tag)) {
					List<Criterion> crits = parseCriterionValue(e, contextId);
					CalendarCriterion tmp = (CalendarCriterion) crits.get(0);
					criterion = UntilCriterion.getInstance(tmp);
				} else if (CRITERION_MESSAGE_ATTR.equals(tag)) {
					ArrayList subchildren = getChildElements(e);
					String name = null, value = null;
					for (int j = 0; j < subchildren.size(); j++) {
						Element h = (Element) subchildren.get(j);
						String t = h.getTagName();
						if (CRITERION_ATTR_NAME.equals(t)) {
							name = getString(h, true, "Missing message-attr name");
						}
						if (CRITERION_ATTR_VALUE.equals(t)) {
							value = getString(h, true, "Missing message-attr value");
						}
					}
					criterion = MessageAttrCriterion.getInstance(name, value);
				} else if (CRITERION_CLIENT_IP.equals(tag)) {
					String ip = getString(e, true, "Missing client-ip value in criterion definition");
					criterion = ClientIPCriterion.getInstance(ip);
				} else if (CRITERION_CLIENT_MASK.equals(tag)) {
					String ip = getString(e, true, "Missing client-mask value in criterion definition");
					criterion = ClientMaskCriterion.getInstance(ip);
				} else {
					throw new XMLConfigException("Invalid criterion value");
				}
				criterions.add(criterion);
			}
		}
		catch (CriterionException e) {
			String text;
			switch (e.getErrorId()) {
				case CriterionException.INVALID_DATE:
					text = "Invalid date value: " + e.getMessage();
					break;
				case CriterionException.INVALID_DAY:
					text = "Invalid day value: " + e.getMessage();
					break;
				case CriterionException.INVALID_MONTH:
					text = "Invalid month value: " + e.getMessage();
					break;
				case CriterionException.INVALID_TIME:
					text = "Invalid time value: " + e.getMessage();
					break;
				case CriterionException.INVALID_MESSAGE_ATTR:
					text = "Invalid message attribute name: " + e.getMessage();
					break;
				case CriterionException.INVALID_REGEXP:
					text = "Invalid regular expression: " + e.getMessage();
					break;
				default:
					text = e.getMessage();
					break;
			}
			throw new XMLConfigException("Invalid criterion: " + text);
		}
		return criterions;
	}

	/**********************************
	 * Implementation of EntityResolver
	 **********************************/

	@Override
	public InputSource resolveEntity(String publicId, String systemId)
		throws SAXException, java.io.IOException {
		if (PUBLIC_ID_CONTEXT.equals(publicId)) {
			return new InputSource(Config.getResourceAsStream(DTD_CONTEXT));
		}
		throw new RuntimeException("Unknown publicId: " + publicId);

	}

}
