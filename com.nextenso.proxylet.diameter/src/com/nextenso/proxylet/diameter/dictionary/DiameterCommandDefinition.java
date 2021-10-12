// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.dictionary;

import java.util.List;

/**
 * This class defines a Diameter command, including both the request
 * and its corresponding answer. It is comprised of the following elements
 * <ul>
 * <li>The Request name (My-Diameter-Request)
 * <li> the answer name (My-Diameter-Answer)
 * <li> the request abbreviation (MDR)
 * <li> the answer abbreviation (MDA)
 * <li> the command code
 * <li> the application ID
 * <li> Flag policies for the Request, Proxiable and Error bit (required, optional, forbidden)
 * <li> the elements that compose both the request and answer
 * <ul>
 * 
 * @see DiameterCommandDefinitionBuilder
 *
 */
public class DiameterCommandDefinition {

	private String requestName;
	private String answerName;
	private String requestAbbreviation;
	private String answerAbbreviation;
	private int  code;
	private long applicationId;
	private FlagPolicy requestRBitPolicy;
	private FlagPolicy requestPBitPolicy;
	private FlagPolicy requestEBitPolicy;
	private FlagPolicy answerRBitPolicy;
	private FlagPolicy answerPBitPolicy;
	private FlagPolicy answerEBitPolicy;
	
	private List<DiameterCommandDefinitionElement> requestElements;
	private List<DiameterCommandDefinitionElement> answerElements;
	
	
	public DiameterCommandDefinition(String requestName, String answerName, String requestAbbreviation,
			String answerAbbreviation, int code, long applicationId, FlagPolicy requestRBitPolicy,
			FlagPolicy requestPBitPolicy, FlagPolicy requestEBitPolicy, FlagPolicy answerRBitPolicy,
			FlagPolicy answerPBitPolicy, FlagPolicy answerEBitPolicy,
			List<DiameterCommandDefinitionElement> requestElements,
			List<DiameterCommandDefinitionElement> answerElements) {
		super();
		this.requestName = requestName;
		this.answerName = answerName;
		this.requestAbbreviation = requestAbbreviation;
		this.answerAbbreviation = answerAbbreviation;
		this.code = code;
		this.applicationId = applicationId;
		this.requestRBitPolicy = requestRBitPolicy;
		this.requestPBitPolicy = requestPBitPolicy;
		this.requestEBitPolicy = requestEBitPolicy;
		this.answerRBitPolicy = answerRBitPolicy;
		this.answerPBitPolicy = answerPBitPolicy;
		this.answerEBitPolicy = answerEBitPolicy;
		this.requestElements = requestElements;
		this.answerElements = answerElements;
	}


	public String getRequestName() {
		return requestName;
	}


	public void setRequestName(String requestName) {
		this.requestName = requestName;
	}


	public String getAnswerName() {
		return answerName;
	}


	public void setAnswerName(String answerName) {
		this.answerName = answerName;
	}


	public String getRequestAbbreviation() {
		return requestAbbreviation;
	}


	public void setRequestAbbreviation(String requestAbbreviation) {
		this.requestAbbreviation = requestAbbreviation;
	}


	public String getAnswerAbbreviation() {
		return answerAbbreviation;
	}


	public void setAnswerAbbreviation(String answerAbbreviation) {
		this.answerAbbreviation = answerAbbreviation;
	}


	public int getCode() {
		return code;
	}


	public void setCode(int code) {
		this.code = code;
	}


	public long getApplicationId() {
		return applicationId;
	}


	public void setApplicationId(int applicationId) {
		this.applicationId = applicationId;
	}


	public FlagPolicy getRequestRBitPolicy() {
		return requestRBitPolicy;
	}


	public void setRequestRBitPolicy(FlagPolicy requestRBitPolicy) {
		this.requestRBitPolicy = requestRBitPolicy;
	}


	public FlagPolicy getRequestPBitPolicy() {
		return requestPBitPolicy;
	}


	public void setRequestPBitPolicy(FlagPolicy requestPBitPolicy) {
		this.requestPBitPolicy = requestPBitPolicy;
	}


	public FlagPolicy getRequestEBitPolicy() {
		return requestEBitPolicy;
	}


	public void setRequestEBitPolicy(FlagPolicy requestEBitPolicy) {
		this.requestEBitPolicy = requestEBitPolicy;
	}


	public FlagPolicy getAnswerRBitPolicy() {
		return answerRBitPolicy;
	}


	public void setAnswerRBitPolicy(FlagPolicy answerRBitPolicy) {
		this.answerRBitPolicy = answerRBitPolicy;
	}


	public FlagPolicy getAnswerPBitPolicy() {
		return answerPBitPolicy;
	}


	public void setAnswerPBitPolicy(FlagPolicy answerPBitPolicy) {
		this.answerPBitPolicy = answerPBitPolicy;
	}


	public FlagPolicy getAnswerEBitPolicy() {
		return answerEBitPolicy;
	}


	public void setAnswerEBitPolicy(FlagPolicy answerEBitPolicy) {
		this.answerEBitPolicy = answerEBitPolicy;
	}


	public List<DiameterCommandDefinitionElement> getRequestElements() {
		return requestElements;
	}


	public void setRequestElements(List<DiameterCommandDefinitionElement> requestElements) {
		this.requestElements = requestElements;
	}


	public List<DiameterCommandDefinitionElement> getAnswerElements() {
		return answerElements;
	}


	public void setAnswerElements(List<DiameterCommandDefinitionElement> answerElements) {
		this.answerElements = answerElements;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((answerAbbreviation == null) ? 0 : answerAbbreviation.hashCode());
		result = prime * result + ((answerEBitPolicy == null) ? 0 : answerEBitPolicy.hashCode());
		result = prime * result + ((answerElements == null) ? 0 : answerElements.hashCode());
		result = prime * result + ((answerName == null) ? 0 : answerName.hashCode());
		result = prime * result + ((answerPBitPolicy == null) ? 0 : answerPBitPolicy.hashCode());
		result = prime * result + ((answerRBitPolicy == null) ? 0 : answerRBitPolicy.hashCode());
		result = prime * result + (int) (applicationId ^ (applicationId >>> 32));
		result = prime * result + (int) (code ^ (code >>> 32));
		result = prime * result + ((requestAbbreviation == null) ? 0 : requestAbbreviation.hashCode());
		result = prime * result + ((requestEBitPolicy == null) ? 0 : requestEBitPolicy.hashCode());
		result = prime * result + ((requestElements == null) ? 0 : requestElements.hashCode());
		result = prime * result + ((requestName == null) ? 0 : requestName.hashCode());
		result = prime * result + ((requestPBitPolicy == null) ? 0 : requestPBitPolicy.hashCode());
		result = prime * result + ((requestRBitPolicy == null) ? 0 : requestRBitPolicy.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DiameterCommandDefinition other = (DiameterCommandDefinition) obj;
		if (answerAbbreviation == null) {
			if (other.answerAbbreviation != null)
				return false;
		} else if (!answerAbbreviation.equals(other.answerAbbreviation))
			return false;
		if (answerEBitPolicy != other.answerEBitPolicy)
			return false;
		if (answerElements == null) {
			if (other.answerElements != null)
				return false;
		} else if (!answerElements.equals(other.answerElements))
			return false;
		if (answerName == null) {
			if (other.answerName != null)
				return false;
		} else if (!answerName.equals(other.answerName))
			return false;
		if (answerPBitPolicy != other.answerPBitPolicy)
			return false;
		if (answerRBitPolicy != other.answerRBitPolicy)
			return false;
		if (applicationId != other.applicationId)
			return false;
		if (code != other.code)
			return false;
		if (requestAbbreviation == null) {
			if (other.requestAbbreviation != null)
				return false;
		} else if (!requestAbbreviation.equals(other.requestAbbreviation))
			return false;
		if (requestEBitPolicy != other.requestEBitPolicy)
			return false;
		if (requestElements == null) {
			if (other.requestElements != null)
				return false;
		} else if (!requestElements.equals(other.requestElements))
			return false;
		if (requestName == null) {
			if (other.requestName != null)
				return false;
		} else if (!requestName.equals(other.requestName))
			return false;
		if (requestPBitPolicy != other.requestPBitPolicy)
			return false;
		if (requestRBitPolicy != other.requestRBitPolicy)
			return false;
		return true;
	}

	
}
