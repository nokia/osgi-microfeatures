// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.engine.xml;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import alcatel.tess.hometop.gateways.utils.Utils;

import com.nextenso.http.agent.engine.criterion.ClientIDCriterion;
import com.nextenso.http.agent.engine.criterion.ClientIPCriterion;
import com.nextenso.http.agent.engine.criterion.HeaderCriterion;
import com.nextenso.http.agent.engine.criterion.HostCriterion;
import com.nextenso.http.agent.engine.criterion.HostIPCriterion;
import com.nextenso.http.agent.engine.criterion.PathCriterion;
import com.nextenso.http.agent.engine.criterion.PortCriterion;
import com.nextenso.http.agent.engine.criterion.SessionAttrCriterion;
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

public class HttpXMLUtils extends XMLUtils {
  
  public static final String PUBLIC_ID_CONTAINER = "-//NEXTENSO//DTD CONTAINER 1.1//EN";
  public static final String PUBLIC_ID_CONTEXT = "-//NEXTENSO//DTD CONTEXT 1.1//EN";
  
  // to do ? : read the dtd filenames from config
  public static String DTD_CONTAINER = "httpAgent/container.dtd";
  public static String DTD_CONTEXT = "httpAgent/context.dtd";
  
  //
  // Container tags
  //
  public static final String CONTEXT = "context";
  public static final String CONTEXTS = "contexts";
  public static final String DEFAULT_CONTEXT = "default-context";
  public static final String CONTEXT_ID = "id";
  
  //
  // Context tags
  //
  public static final String CONTEXT_NAME = "context-name";
  public static final String DESCRIPTION = "description";
  public static final String CONTEXT_PARAM = "context-param";
  public static final String INIT_PARAM = "init-param";
  public static final String PARAM_NAME = "param-name";
  public static final String PARAM_VALUE = "param-value";
  public static final String CONTEXT_CRITERION = "context-criterion";
  public static final String REQUEST_CHAIN = "request-chain";
  public static final String RESPONSE_CHAIN = "response-chain";
  public static final String SESSION_LISTENER = "session-listener";
  public static final String REQUEST_LISTENER = "request-listener";
  public static final String RESPONSE_LISTENER = "response-listener";
  public static final String CONTEXT_LISTENER = "context-listener";
  public static final String PROXYLET_NAME = "proxylet-name";
  public static final String PROXYLET_CLASS = "proxylet-class";
  public static final String NEXT_HOP = "next-hop";
  public static final String NEXT_HOP_DIRECT = "direct";
  
  //
  // criterion tags
  //
  public static final String CRITERION = "criterion";
  public static final String CRITERION_NAME = "criterion-name";
  public static final String CRITERION_VALUE = "criterion-value";
  public static final String CRITERION_IPSRC = "ipsrc";
  public static final String CRITERION_IPDEST = "ipdest";
  public static final String CRITERION_DOMAIN = "domain";
  public static final String CRITERION_PATH = "path";
  public static final String CRITERION_PORT = "port";
  public static final String CRITERION_HEADER = "header";
  public static final String CRITERION_HEADER_NAME = "header-name";
  public static final String CRITERION_HEADER_VALUE = "header-value";
  public static final String CRITERION_AND = "and";
  public static final String CRITERION_OR = "or";
  public static final String CRITERION_NOT = "not";
  public static final String CRITERION_REF = "criterion-ref";
  public static final String CRITERION_ALL = "all";
  public static final String CRITERION_FROM = "from";
  public static final String CRITERION_UNTIL = "until";
  public static final String CRITERION_DAY = "day";
  public static final String CRITERION_DATE = "date";
  public static final String CRITERION_MONTH = "month";
  public static final String CRITERION_TIME = "time";
  public static final String CRITERION_SESSION_ATTR = "session-attr";
  public static final String CRITERION_MESSAGE_ATTR = "message-attr";
  public static final String CRITERION_ATTR_NAME = "attr-name";
  public static final String CRITERION_ATTR_VALUE = "attr-value";
  public static final String CRITERION_CLID = "clid";
  
  protected static CriterionWrapper parseCriterion(Element element, String contextId)
      throws XMLConfigException {
    CriterionWrapper wrapper = new CriterionWrapper();
    ArrayList<?> children = getChildElements(element);
    for (int i = 0; i < children.size(); i++) {
      Element e = (Element) children.get(i);
      String tag = e.getTagName();
      if (CRITERION_NAME.equals(tag)) {
        String name = contextId + '-'
            + getString(e, true, "Missing criterion name for context: " + contextId);
        wrapper.setName(name);
      } else if (DESCRIPTION.equals(tag)) {
        String desc = getString(e, false, null);
        wrapper.setDescription(desc);
      } else if (CRITERION_VALUE.equals(tag)) {
        ArrayList<Criterion> crits = parseCriterionValue(e, contextId);
        Criterion c = crits.get(0);
        wrapper.setCriterion(c);
        //System.err.println("criterion: "+c);
        CriterionManager.registerCriterion(wrapper.getName(), c);
      }
    }
    return wrapper;
  }
  
  protected static ArrayList<Criterion> parseCriterionValue(Element element, String contextId) throws XMLConfigException {
    ArrayList<Criterion> criterions = new ArrayList<Criterion>();
    try {
      ArrayList<?> children = getChildElements(element);
      for (int i = 0; i < children.size(); i++) {
        Element e = (Element) children.get(i);
        Criterion criterion = null;
        String tag = e.getTagName();
        if (CRITERION_DOMAIN.equals(tag)) {
          String domain = getString(e, true, "Missing domain value in criterion definition");
          criterion = HostCriterion.getInstance(domain);
        } else if (CRITERION_PORT.equals(tag)) {
          String port = getString(e, true, "Missing port value in criterion definition");
          criterion = PortCriterion.getInstance(port);
        } else if (CRITERION_PATH.equals(tag)) {
          String path = getString(e, true, "Missing path value in criterion definition");
          criterion = PathCriterion.getInstance(path);
        } else if (CRITERION_IPSRC.equals(tag)) {
          String ipsrc = getString(e, true, "Missing ipsrc value in criterion definition");
          criterion = ClientIPCriterion.getInstance(ipsrc);
        } else if (CRITERION_IPDEST.equals(tag)) {
          String ipdest = getString(e, true, "Missing ipdest value in criterion definition");
          criterion = HostIPCriterion.getInstance(ipdest);
        } else if (CRITERION_HEADER.equals(tag)) {
          ArrayList<?> subchildren = getChildElements(e);
          String name = null, value = null;
          for (int j = 0; j < subchildren.size(); j++) {
            Element h = (Element) subchildren.get(j);
            String t = h.getTagName();
            if (CRITERION_HEADER_NAME.equals(t))
              name = getString(h, true, "Missing header-name");
            if (CRITERION_HEADER_VALUE.equals(t))
              value = getString(h, true, "Missing header-value");
          }
          criterion = HeaderCriterion.getInstance(name, value);
        } else if (CRITERION_AND.equals(tag)) {
          ArrayList<Criterion> crits = parseCriterionValue(e, contextId);
          for (int j = 0; j < crits.size(); j++) {
            Criterion tmp = crits.get(j);
            if (j == 0)
              criterion = tmp;
            else
              criterion = LogicalANDCriterion.getInstance(criterion, tmp);
          }
        } else if (CRITERION_OR.equals(tag)) {
          ArrayList<Criterion> crits = parseCriterionValue(e, contextId);
          for (int j = 0; j < crits.size(); j++) {
            Criterion tmp = crits.get(j);
            if (j == 0)
              criterion = tmp;
            else
              criterion = LogicalORCriterion.getInstance(criterion, tmp);
          }
        } else if (CRITERION_NOT.equals(tag)) {
          Criterion tmp = (Criterion) parseCriterionValue(e, contextId).get(0);
          criterion = LogicalNOTCriterion.getInstance(tmp);
        } else if (CRITERION_REF.equals(tag)) {
          String ref = getString(e, true, "Missing reference value in criterion definition");
          criterion = CriterionManager.getCriterion(contextId + '-' + ref);
          if (criterion == null)
            throw new XMLConfigException("Invalid reference value (" + ref + ")");
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
          ArrayList<Criterion> crits = parseCriterionValue(e, contextId);
          CalendarCriterion tmp = (CalendarCriterion) crits.get(0);
          criterion = FromCriterion.getInstance(tmp);
        } else if (CRITERION_UNTIL.equals(tag)) {
          ArrayList<Criterion> crits = parseCriterionValue(e, contextId);
          CalendarCriterion tmp = (CalendarCriterion) crits.get(0);
          criterion = UntilCriterion.getInstance(tmp);
        } else if (CRITERION_SESSION_ATTR.equals(tag)) {
          ArrayList<?> subchildren = getChildElements(e);
          String name = null, value = null;
          for (int j = 0; j < subchildren.size(); j++) {
            Element h = (Element) subchildren.get(j);
            String t = h.getTagName();
            if (CRITERION_ATTR_NAME.equals(t))
              name = getString(h, true, "Missing session-attr name");
            if (CRITERION_ATTR_VALUE.equals(t))
              value = getString(h, true, "Missing session-attr value");
          }
          criterion = SessionAttrCriterion.getInstance(name, value);
        } else if (CRITERION_MESSAGE_ATTR.equals(tag)) {
          ArrayList<?> subchildren = getChildElements(e);
          String name = null, value = null;
          for (int j = 0; j < subchildren.size(); j++) {
            Element h = (Element) subchildren.get(j);
            String t = h.getTagName();
            if (CRITERION_ATTR_NAME.equals(t))
              name = getString(h, true, "Missing message-attr name");
            if (CRITERION_ATTR_VALUE.equals(t))
              value = getString(h, true, "Missing message-attr value");
          }
          criterion = MessageAttrCriterion.getInstance(name, value);
        } else if (CRITERION_CLID.equals(tag)) {
          String id = getString(e, true, "Missing clid value in criterion definition");
          criterion = ClientIDCriterion.getInstance(id);
        } else
          throw new XMLConfigException("Invalid criterion value");
        criterions.add(criterion);
      }
    } catch (CriterionException e) {
      String text;
      switch (e.getErrorId()) {
      case CriterionException.INVALID_HOST:
        text = "Invalid host value: " + e.getMessage();
        break;
      case CriterionException.INVALID_PORT:
        text = "Invalid port value: " + e.getMessage();
        break;
      case CriterionException.INVALID_PATH:
        text = "Invalid path value: " + e.getMessage();
        break;
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
      case CriterionException.INVALID_SESSION_ATTR:
        text = "Invalid session attribute name: " + e.getMessage();
        break;
      case CriterionException.INVALID_MESSAGE_ATTR:
        text = "Invalid message attribute name: " + e.getMessage();
        break;
      case CriterionException.INVALID_IP:
        text = "Invalid IP value: " + e.getMessage();
        break;
      case CriterionException.INVALID_REGEXP:
        text = "Invalid regular expression: " + e.getMessage();
        break;
      case CriterionException.INVALID_HEADER:
        text = "Invalid header name: " + e.getMessage();
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
  
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, java.io.IOException {
    if (PUBLIC_ID_CONTAINER.equals(publicId))
      return new InputSource(Utils.getResource(DTD_CONTAINER, getClass().getClassLoader()).openStream());
    else if (PUBLIC_ID_CONTEXT.equals(publicId))
      return new InputSource(Utils.getResource(DTD_CONTEXT, getClass().getClassLoader()).openStream());
    throw new RuntimeException("Unknown publicId: " + publicId);
  }
  
}
