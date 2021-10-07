package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.admin.CriterionValue;
import com.nextenso.proxylet.admin.CriterionValueData;
import com.nextenso.proxylet.admin.CriterionValueNamed;
import com.nextenso.proxylet.admin.http.HttpBearer;
import com.nextenso.proxylet.engine.criterion.CommonCriterionParser;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;

public class HttpCriterionParser extends CommonCriterionParser {
  
  public HttpCriterionParser() {
  }
  
  public Criterion parseCriterionValue(CriterionValue value) throws CriterionException {
    Criterion criterion = super.parseCriterionValue(value);
    
    if (criterion != null)
      return criterion;
    
    String tagName = value.getTagName();
    
    if (HttpBearer.CLID.equals(tagName)) {
      String clid = ((CriterionValueData) value).getValue();
      criterion = ClientIDCriterion.getInstance(clid);
      
    } else if (HttpBearer.IPSRC.equals(tagName)) {
      String ipSrc = ((CriterionValueData) value).getValue();
      criterion = ClientIPCriterion.getInstance(ipSrc);
      
    } else if (HttpBearer.HEADER.equals(tagName)) {
      String attrName = ((CriterionValueNamed) value).getName();
      String attrValue = ((CriterionValueNamed) value).getValue();
      if ((attrValue == null) || (attrValue.length() == 0))
        criterion = HeaderCriterion.getInstance(attrName);
      else
        criterion = HeaderCriterion.getInstance(attrName, attrValue);
      
    } else if (HttpBearer.DOMAIN.equals(tagName)) {
      String host = ((CriterionValueData) value).getValue();
      criterion = HostCriterion.getInstance(host);
      
    } else if (HttpBearer.IPDEST.equals(tagName)) {
      String hostIP = ((CriterionValueData) value).getValue();
      criterion = HostIPCriterion.getInstance(hostIP);
      
    } else if (HttpBearer.PATH.equals(tagName)) {
      String path = ((CriterionValueData) value).getValue();
      criterion = PathCriterion.getInstance(path);
      
    } else if (HttpBearer.PORT.equals(tagName)) {
      String port = ((CriterionValueData) value).getValue();
      criterion = PortCriterion.getInstance(port);
      
    } else if (HttpBearer.SESSION_ATTR.equals(tagName)) {
      String attrName = ((CriterionValueNamed) value).getName();
      String attrValue = ((CriterionValueNamed) value).getValue();
      if ((attrValue == null) || (attrValue.length() == 0))
        criterion = SessionAttrCriterion.getInstance(attrName);
      else
        criterion = SessionAttrCriterion.getInstance(attrName, attrValue);
      
    }
    
    if (value != null)
      registerInstance(criterion, value.getCriterionName());
    
    return criterion;
  }
}
