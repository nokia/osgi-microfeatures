// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

import java.util.Hashtable;
import java.util.Iterator;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.CriterionValue;
import com.nextenso.proxylet.admin.CriterionValueData;
import com.nextenso.proxylet.admin.CriterionValueDate;
import com.nextenso.proxylet.admin.CriterionValueLogical;
import com.nextenso.proxylet.admin.CriterionValueNamed;
import com.nextenso.proxylet.admin.CriterionValueNot;

public class CommonCriterionParser implements CriterionParser {
  
  protected Hashtable instances = new Hashtable();
  
  public CommonCriterionParser() {
  }
  
  /**
     Parse the criterion value.
     Note: criterion name must have been set before parsing criterion. Otherwise criterion value cannot be used as reference later.
   */
  public Criterion parseCriterionValue(CriterionValue value) throws CriterionException {
    String tagName = value.getTagName();
    Criterion criterion = null;
    
    if (Bearer.AND.equals(tagName)) {
      Iterator it = ((CriterionValueLogical) value).getCriterionValues();
      if (!it.hasNext())
        throw new CriterionException(CriterionException.INVALID_SYNTAX, "Invalid " + Bearer.AND
            + " criterion value");
      
      boolean firstElement = true;
      
      while (it.hasNext()) {
        CriterionValue critValue = (CriterionValue) it.next();
        Criterion tmp = parseCriterionValue(critValue);
        if (firstElement) {
          criterion = tmp;
          firstElement = false;
        } else {
          criterion = LogicalANDCriterion.getInstance(criterion, tmp);
        }
      }
      
    } else if (Bearer.OR.equals(tagName)) {
      Iterator it = ((CriterionValueLogical) value).getCriterionValues();
      if (!it.hasNext())
        throw new CriterionException(CriterionException.INVALID_SYNTAX, "Invalid " + Bearer.OR
            + " criterion value");
      
      boolean firstElement = true;
      
      while (it.hasNext()) {
        CriterionValue critValue = (CriterionValue) it.next();
        Criterion tmp = parseCriterionValue(critValue);
        if (firstElement) {
          criterion = tmp;
          firstElement = false;
        } else {
          criterion = LogicalORCriterion.getInstance(criterion, tmp);
        }
      }
      
    } else if (Bearer.NOT.equals(tagName)) {
      Criterion crit = parseCriterionValue((CriterionValue) (((CriterionValueNot) value).getCriterionValues()
          .next()));
      criterion = LogicalNOTCriterion.getInstance(crit);
      
    } else if (Bearer.ALL.equals(tagName)) {
      criterion = TrueCriterion.getInstance();
      
    } else if (Bearer.FROM.equals(tagName)) {
      CalendarCriterion tmp = parseCriterionValueDate((CriterionValueData) ((CriterionValueDate) value)
          .getCriterion());
      if (tmp == null)
        throw new CriterionException(CriterionException.INVALID_SYNTAX, "Invalid " + Bearer.FROM
            + " criterion value");
      criterion = FromCriterion.getInstance(tmp);
      
    } else if (Bearer.UNTIL.equals(tagName)) {
      CalendarCriterion tmp = parseCriterionValueDate((CriterionValueData) ((CriterionValueDate) value)
          .getCriterion());
      if (tmp == null)
        throw new CriterionException(CriterionException.INVALID_SYNTAX, "Invalid " + Bearer.UNTIL
            + " criterion value");
      criterion = UntilCriterion.getInstance(tmp);
      
    } else if (Bearer.DAY.equals(tagName)) {
      String day = ((CriterionValueData) value).getValue();
      criterion = DayCriterion.getInstance(day);
      
    } else if (Bearer.DATE.equals(tagName)) {
      String date = ((CriterionValueData) value).getValue();
      criterion = DateCriterion.getInstance(date);
      
    } else if (Bearer.MONTH.equals(tagName)) {
      String month = ((CriterionValueData) value).getValue();
      criterion = MonthCriterion.getInstance(month);
      
    } else if (Bearer.TIME.equals(tagName)) {
      String time = ((CriterionValueData) value).getValue();
      criterion = TimeCriterion.getInstance(time);
      
    } else if (Bearer.MESSAGE_ATTR.equals(tagName)) {
      String attrName = ((CriterionValueNamed) value).getName();
      String attrValue = ((CriterionValueNamed) value).getValue();
      if ((attrValue == null) || (attrValue.length() == 0))
        criterion = MessageAttrCriterion.getInstance(attrName);
      else
        criterion = MessageAttrCriterion.getInstance(attrName, attrValue);
      
    } else if (Bearer.REFERENCE.equals(tagName)) {
      String ref = ((CriterionValueData) value).getValue();
      criterion = (Criterion) instances.get(ref);
      if (criterion == null)
        throw new CriterionException(CriterionException.UNKNOWN_REFERENCE, "Unknown reference: " + ref);
      
    }
    
    registerInstance(criterion, value.getCriterionName());
    
    return criterion;
  }
  
  protected void registerInstance(Criterion criterion, String criterionName) {
    if ((criterionName == null) || (criterion == null))
      return;
    else {
      instances.put(criterionName, criterion);
    }
  }
  
  public CalendarCriterion parseCriterionValueDate(CriterionValueData criterion) throws CriterionException {
    String tagName = criterion.getTagName();
    
    if (Bearer.DAY.equals(tagName)) {
      String day = criterion.getValue();
      return (CalendarCriterion) DayCriterion.getInstance(day);
    } else if (Bearer.DATE.equals(tagName)) {
      String date = criterion.getValue();
      return (CalendarCriterion) DateCriterion.getInstance(date);
    } else if (Bearer.MONTH.equals(tagName)) {
      String month = criterion.getValue();
      return (CalendarCriterion) MonthCriterion.getInstance(month);
    }
    
    return null;
  }
  
}
