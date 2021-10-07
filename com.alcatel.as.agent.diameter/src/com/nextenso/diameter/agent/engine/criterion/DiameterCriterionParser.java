package com.nextenso.diameter.agent.engine.criterion;

import com.nextenso.proxylet.admin.CriterionValue;
import com.nextenso.proxylet.admin.CriterionValueData;
import com.nextenso.proxylet.admin.diameter.DiameterBearer;
import com.nextenso.proxylet.engine.criterion.CommonCriterionParser;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.TrueCriterion;

public class DiameterCriterionParser
		extends CommonCriterionParser {

	public DiameterCriterionParser() {}

	@Override
	public Criterion parseCriterionValue(CriterionValue value)
		throws CriterionException {
		Criterion criterion = super.parseCriterionValue(value);

		if (criterion != null)
			return criterion;

		String tagName = value.getTagName();

		if (DiameterBearer.APPLICATION.equals(tagName) && value instanceof CriterionValueData) {
			String appli = ((CriterionValueData) value).getValue();
			criterion = ApplicationCriterion.getInstance(appli);
		} else if (DiameterBearer.ALL_APPLICATIONS.equals(tagName)) {
			criterion = TrueCriterion.getInstance();
		}

		registerInstance(criterion, value.getCriterionName());

		return criterion;
	}
}
