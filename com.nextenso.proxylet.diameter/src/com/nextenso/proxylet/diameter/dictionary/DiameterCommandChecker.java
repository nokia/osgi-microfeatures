// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.dictionary;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterResponse;


/**
 * This class check if a DiameterRequest or DiameterResponse is well formed
 * according to its definition.
 * <br/>
 * Currently, the checker will verify the flags, the presence of AVPs and whether
 * their count is compliant with the definition.
 */
public class DiameterCommandChecker {
	
	/**
	 * An exception thrown when a check failed
	 *
	 */
	public static class CheckException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public CheckException(String message, Throwable cause) {
			super(message, cause);
		}

		public CheckException(String message) {
			super(message);
		}
	}

	private DiameterCommandDictionary cmdDico;

	
	/**
	 * Construct a new DiameterCommandChecker.
	 * @param cmdDico the Diameter command dictionary 
	 */
	public DiameterCommandChecker(DiameterCommandDictionary cmdDico) {
		this.cmdDico = Objects.requireNonNull(cmdDico);
	}

	/**
	 * Check a DiameterRequest. An exception will be thrown if the check failed.
	 * 
	 * @param msg the request to check
	 * @throws CheckException thrown if the check failed
	 */
	public void checkRequest(DiameterRequest msg) throws CheckException {
		Objects.requireNonNull(msg);
		DiameterCommandDefinition def = cmdDico.getCommandDefinitionByCode(msg.getDiameterCommand(),
				(int) msg.getDiameterApplication());

		if (def == null) {
			throw new CheckException("no command def found for id " + msg.getDiameterCommand());
		}

		checkFlags(msg, def);
		checkAVPs(msg, def, true);

	}
	
	/**
	 * Check a DiameterResponse. An exception will be thrown if the check failed.
	 * 
	 * @param msg the response to check
	 * @throws CheckException thrown if the check failed
	 */
	public void checkResponse(DiameterResponse msg) throws CheckException {
		Objects.requireNonNull(msg);
		DiameterCommandDefinition def = cmdDico.getCommandDefinitionByCode(msg.getDiameterCommand(),
				(int) msg.getDiameterApplication());

		if (def == null) {
			throw new CheckException("no command def found for id " + msg.getDiameterCommand());
		}

		checkFlags(msg, def);
		checkAVPs(msg, def, false);

	}

	private void checkAVPs(DiameterMessage msg, DiameterCommandDefinition def, boolean isReq) throws CheckException {
		List<DiameterCommandDefinitionElement> elements = isReq ? def.getRequestElements() : def.getAnswerElements();
		
		Enumeration<?> enumerAVP = msg.getDiameterAVPs();
		Map<DiameterAVPDefinition, Integer> avpCount = new HashMap<>();
		while(enumerAVP.hasMoreElements()) {
			DiameterAVP avp = (DiameterAVP) enumerAVP.nextElement();
			DiameterAVPDefinition avpDef = avp.getDiameterAVPDefinition();
			if(avpDef == null) {
				throw new CheckException("no avp definition associated with avp " + avp);
			}
			checkAVPContent(avp);
			int count = avpCount.containsKey(avpDef) ? avpCount.get(avpDef) : 0;
			avpCount.put(avpDef, count + 1);
		}
		
		for(DiameterCommandDefinitionElement i : elements) {
			Integer count = avpCount.get(i.getAVPDefinition());
			
			if((count == null || count == 0) && i.getMinOccurence() > 0) {
				throw new CheckException("avp " + i.getAVPDefinition().getAVPName() + " not found in message "
						+ " but is required to appears at least " + i.getMinOccurence() + " time(s)");
			} else if(count != null && count < i.getMinOccurence()) {
				throw new CheckException("avp " + i.getAVPDefinition().getAVPName() + " must appears at least"
						+ i.getMinOccurence() + "time(s) but was found " + count + " time(s) in the message" );
			} else if(count != null && count > i.getMaxOccurence() && i.getMaxOccurence() >= 0) {
				throw new CheckException("avp " + i.getAVPDefinition().getAVPName() + " must appears " +
						i.getMaxOccurence() + " time(s) at most, but was found " + count + " time(s) "
						+ "in the message");
			}
		}
		
		
	}

	private void checkFlags(DiameterRequest msg, DiameterCommandDefinition def) throws CheckException {
		if (msg.hasProxyFlag() && def.getRequestPBitPolicy() == FlagPolicy.FORBIDDEN) {
			throw new CheckException("P Bit is set but is forbidden in the command definition");
		} else if (!msg.hasProxyFlag() && def.getRequestPBitPolicy() == FlagPolicy.REQUIRED) {
			throw new CheckException("P Bit is not set but is mandatory in the command definition");
		}
	}
	
	private void checkFlags(DiameterResponse msg, DiameterCommandDefinition def) throws CheckException {
		if (msg.hasErrorFlag() && def.getAnswerEBitPolicy() == FlagPolicy.FORBIDDEN) {
			throw new CheckException("E Bit is set but is forbidden in the command definition");
		} else if (!msg.hasErrorFlag() && def.getAnswerEBitPolicy() == FlagPolicy.REQUIRED) {
			throw new CheckException("P Bit is not set but is mandatory in the command definition");
		}
	}
	
	private void checkAVPContent(DiameterAVP avp) throws CheckException {
		//TODO
	}
}
