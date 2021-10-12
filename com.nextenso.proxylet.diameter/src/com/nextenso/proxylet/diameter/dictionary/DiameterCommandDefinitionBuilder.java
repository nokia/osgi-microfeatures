// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * Builder class that facilitates the creation of 
 * {@link DiameterCommandDefinitionBuilder DiameterCommandDefinitionBuilder} 
 */
public class DiameterCommandDefinitionBuilder {
	private String requestName;
	private String answerName;
	private String requestAbbreviation;
	private String answerAbbreviation;
	private int code;
	private long applicationId;
	private FlagPolicy requestRBitPolicy;
	private FlagPolicy requestPBitPolicy;
	private FlagPolicy requestEBitPolicy;
	private FlagPolicy answerRBitPolicy;
	private FlagPolicy answerPBitPolicy;
	private FlagPolicy answerEBitPolicy;
	
	private List<DiameterCommandDefinitionElement> requestElements;
	private List<DiameterCommandDefinitionElement> answerElements;
	
	
	public DiameterCommandDefinitionBuilder() {
		this.requestElements = new ArrayList<>();
		this.answerElements = new ArrayList<>();
	}
	
	public long getApplicationId() {
		return applicationId;
	}
	public DiameterCommandDefinitionBuilder applicationId(long l) {
		this.applicationId = l;
		return this;
	}
	public DiameterCommandDefinitionBuilder name(String requestName, String answerName) {
		Objects.requireNonNull(requestName);
		Objects.requireNonNull(answerName);
		
		this.requestName = requestName;
		this.answerName  = answerName;
		return this;
	}
	public DiameterCommandDefinitionBuilder abbreviation(String requestAbbreviation, String answerAbbreviation) {
		Objects.requireNonNull(requestAbbreviation);
		Objects.requireNonNull(answerAbbreviation);
		this.requestAbbreviation = requestAbbreviation;
		this.answerAbbreviation = answerAbbreviation;
		return this;
	}
	public DiameterCommandDefinitionBuilder code(int code) {
		this.code = code;
		return this;
	}
	public DiameterCommandDefinitionBuilder requestBitPolicy(FlagPolicy requestRBitPolicy, FlagPolicy answerRBitPolicy) {
		this.requestRBitPolicy = requestRBitPolicy;
		this.answerRBitPolicy = answerRBitPolicy;
		return this;
	}
	public DiameterCommandDefinitionBuilder proxiableBitPolicy(FlagPolicy requestPBitPolicy, FlagPolicy answerPBitPolicy) {
		this.requestPBitPolicy = requestPBitPolicy;
		this.answerPBitPolicy = answerPBitPolicy;
		return this;
	}
	public DiameterCommandDefinitionBuilder errorBitPolicy(FlagPolicy requestEBitPolicy, FlagPolicy answerEBitPolicy) {
		this.requestEBitPolicy = requestEBitPolicy;
		this.answerEBitPolicy = answerEBitPolicy;
		return this;
	}
	
	public DiameterCommandDefinitionBuilder requestAVP(DiameterAVPDefinition avpDef, int minOccurence, int maxOccurence) {
		requestElements.add(new DiameterCommandDefinitionElement(avpDef, minOccurence, maxOccurence));
		return this;
	}
	
	public DiameterCommandDefinitionBuilder answerAVP(DiameterAVPDefinition avpDef, int minOccurence, int maxOccurence) {
		answerElements.add(new DiameterCommandDefinitionElement(avpDef, minOccurence, maxOccurence));
		return this;
	}
	
	public DiameterCommandDefinition build() {
		if(requestName == null || answerName == null 
				|| requestAbbreviation == null || answerAbbreviation== null) {
			throw new IllegalStateException("incomplete definition");
		}
		
		return new DiameterCommandDefinition(requestName,
				answerName,
				requestAbbreviation,
				answerAbbreviation,
				code,
				applicationId,
				requestRBitPolicy,
				requestPBitPolicy,
				requestEBitPolicy,
				answerRBitPolicy,
				answerPBitPolicy,
				answerEBitPolicy,
				requestElements,
				answerElements);
	}
	
}
