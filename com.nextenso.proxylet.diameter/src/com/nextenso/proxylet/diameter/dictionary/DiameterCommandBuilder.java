// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.dictionary;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition.Dictionary;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandChecker.CheckException;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterAVP;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterCommand;
import com.nextenso.proxylet.diameter.util.AddressFormat.DiameterAddress;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat.IPFilterRule;
import com.nextenso.proxylet.diameter.util.URIFormat.URI;

/**
 * This class let you generate Diameter requests and answers from an annotated
 * POJO. In addition, the generated message will be checked against the command
 * definition provided by the passed dictionary in order to verify whether it is
 * well formed.
 * <p/>
 * The command generator will use the {@link DiameterAVP DiameterAVP} and
 * {@link DiameterCommand DiameterCommand} annotations to generate the Diameter message from
 * the passed object.
 * <p/>
 * AVPs will be placed in the order the annotated fields are found in the object
 * instance
 * <p/>
 * The annotated field values will be encoded in the corresponding Diameter
 * Format according to the AVP definition. Specific types are expected for
 * each format:
 * <table>
 * <tr>
 * <th>Diameter Format</th>
 * <th>Java Type</th>
 * </tr>
 * <tr>
 * <td>Address</td>
 * <td>{@link DiameterAddress com.nextenso.proxylet.diameter.util.AddressFormat.DiameterAddress}</td>
 * </tr>
 * <tr>
 * <td>Enumerated</td>
 * <td>Integer</td>
 * </tr>
 * <tr>
 * <td>Float32</td>
 * <td>Float</td>
 * </tr>
 * <tr>
 * <td>Float64</td>
 * <td>Double</td>
 * </tr>
 * <tr>
 * <td>Grouped</td>
 * <td>List of {@link com.nextenso.proxylet.diameter.DiameterAVP com.nextenso.proxylet.diameter.DiameterAVP}</td>
 * </tr>
 * <tr>
 * <td>Identity</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <td>Integer32</td>
 * <td>Integer</td>
 * </tr>
 * <tr>
 * <td>Integer64</td>
 * <td>Long</td>
 * </tr>
 * <tr>
 * <td>IPFilterRule</td>
 * <td>{@link IPFilterRule com.nextenso.proxylet.diameter.util.IPFilterRuleFormat.IPFilterRule}</td>
 * </tr>
 * <tr>
 * <td>OctetString</td>
 * <td>byte[]</td>
 * </tr>
 * <tr>
 * <td>Time</td>
 * <td>Long (Java Format timestamp)</td>
 * </tr>
 * <tr>
 * <td>Unsigned32</td>
 * <td>Long</td>
 * </tr>
 * <tr>
 * <td>Unsigned64</td>
 * <td>BigInteger</td>
 * </tr>
 * <tr>
 * <td>URI</td>
 * <td>{@link URI com.nextenso.proxylet.diameter.util.URIFormat.URI}</td>
 * </tr>
 * <tr>
 * <td>UTF8String</td>
 * <td>String</td>
 * </tr>
 * </table>
 */
public class DiameterCommandBuilder {

	private static class CommandElement {
		public final DiameterAVP avp;
		public final Object value;

		public CommandElement(DiameterAVP avp, Object value) {
			super();
			this.avp = avp;
			this.value = value;
		}
	}

	/**
	 * Exception thrown when the message generation failed
	 *
	 */
	public static class GenerationException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4507605655453503404L;

		public GenerationException(String message, Throwable cause) {
			super(message, cause);
		}

		public GenerationException(String message) {
			super(message);
		}
	}

	private AbstractDiameterAVPDictionary avpDico;
	private DiameterCommandDictionary cmdDico;
	private DiameterCommandChecker checker = null;
	private boolean fillSystemAVPs;

	/**
	 * Construct a new DiameterCommandBuilder. The system AVP dictionary will be
	 * used. It contains all the standard Diameter base AVPs and the NAS
	 * application AVPs.
	 * 
	 * @param cmdDico the Diameter command dictionary
	 */
	public DiameterCommandBuilder(DiameterCommandDictionary cmdDico) {
		this(cmdDico, Dictionary.getBackingAVPDictionary(), true, true);
	}

	/**
	 * Construct a new DiameterCommandBuilder
	 * 
	 * @param cmdDico the Diameter command dictionary
	 * @param avpDico the Diameter AVP dictionary
	 */
	public DiameterCommandBuilder(DiameterCommandDictionary cmdDico, AbstractDiameterAVPDictionary avpDico) {
		this(cmdDico, avpDico, true, true);
	}

	/**
	 * Construct a new DiameterCommandBuilder. if fillSystemAVPs is set to true, the
	 * following AVPs will be filled with the values provided by the DiameterClient
	 * passed when buildRequest is called:
	 * <ul>
	 * <li>Session-Id (if stateful),
	 * <li>Destination-Host
	 * <li>Destination-Realm
	 * <li>Auth-Application-Id,
	 * <li>Acct-Application-Id
	 * </ul>
	 * 
	 * @param cmdDico        the Diameter command dictionary
	 * @param avpDico        the Diameter AVP dictionary
	 * @param checkCommand   if false, the generated message will not be checked
	 *                       against its definition
	 * @param fillSystemAVPs if true, some special AVPs values will be overriden
	 *                       with the values provided by the DiameterClient
	 */
	public DiameterCommandBuilder(DiameterCommandDictionary cmdDico, AbstractDiameterAVPDictionary avpDico,
			boolean checkCommand, boolean fillSystemAVPs) {
		this.cmdDico = Objects.requireNonNull(cmdDico);
		this.avpDico = Objects.requireNonNull(avpDico);

		if (checkCommand) {
			checker = new DiameterCommandChecker(cmdDico);
		}

		this.fillSystemAVPs = fillSystemAVPs;
	}

	/**
	 * Try to generate a Diameter request from the passed annotated POJO and check
	 * it against its definition found in the dictionary.
	 * 
	 * An exception will be thrown in the following case:
	 * <ul>
	 * <li>The {@link DiameterCommand DiameterCommand} annotation could not be found
	 * on the object
	 * <li>A referenced Diameter Command or AVP could not be found the dictionaries
	 * <li>the Diameter Command application Id differs from the passed
	 * DiameterClient
	 * <li>A value couldn't be encoded in the corresponding Diameter format
	 * <li>the generated request failed the check against its definition, excepted
	 * if it's disabled
	 * </ul>
	 * <p/>
	 * If the DiameterCommandBuilder has been constructor with fillSystemAVPs, the
	 * following AVPs will be filled with info from the DiameterClient
	 * <ul>
	 * <li>Session-Id (if stateful),
	 * <li>Destination-Host
	 * <li>Destination-Realm
	 * <li>Auth-Application-Id,
	 * <li>Acct-Application-Id
	 * </ul>
	 * 
	 * 
	 * @param client a DiameterClient used to construct new requests and get some
	 *               relevant informations
	 * @param bean   the annotated java object to generate the request from
	 * @return a ready to use {@link DiameterClientRequest DiameterClientRequest}
	 * @throws GenerationException if the passed annotated object is malformed, if a
	 *                             check failed, etc
	 */
	public DiameterClientRequest buildRequest(DiameterClient client, Object bean) throws GenerationException {
		Objects.requireNonNull(bean);
		Objects.requireNonNull(client);
		DiameterCommandDefinition cmdDef = getCommandDefinition(bean);

		List<com.nextenso.proxylet.diameter.DiameterAVP> avps = getAVPs(bean);

		if (cmdDef.getApplicationId() != client.getDiameterApplication()) {
			throw new GenerationException("Cannot generate a request " + cmdDef.getRequestName()
					+ " because it has an applicationId (" + cmdDef.getApplicationId()
					+ ") that differs from the passed " + "DiameterClient (" + client.getDiameterApplication());
		}

		DiameterClientRequest request = client.newRequest((int) cmdDef.getCode(),
				cmdDef.getRequestPBitPolicy() == FlagPolicy.FORBIDDEN ? false : true);

		for (com.nextenso.proxylet.diameter.DiameterAVP avp : avps) {
			request.addDiameterAVP(avp);
		}

		if (checker != null) {
			checkRequest(request);
		}

		if (fillSystemAVPs) {
			client.fillMessage(request);
		}

		return request;
	}

	/**
	 * Try to generate a Diameter Answer from the passed annotated POJO and check
	 * it against its definition found in the dictionary.
	 * 
	 * An exception will be thrown in the following case:
	 * <ul>
	 * <li>The {@link DiameterCommand DiameterCommand} annotation could not be found
	 * on the object
	 * <li>A referenced Diameter Command or AVP could not be found in the dictionaries
	 * <li>A value couldn't be encoded in the corresponding Diameter format
	 * <li>the generated request failed the check against its definition, except
	 * if the check is disabled disabled
	 * </ul>
	 * 
	 * @param request the request to generate a response for
	 * @param bean    the annotated java object to generate the request from
	 * @return the filled {@link DiameterResponse DiameterResponse}
	 * @throws GenerationException if the passed annotated object is malformed, if a
	 *                             check failed, etc
	 */
	public DiameterResponse buildResponse(DiameterRequest req, Object bean) throws GenerationException {
		Objects.requireNonNull(bean);
		Objects.requireNonNull(req);
		DiameterCommandDefinition cmdDef = getCommandDefinition(bean);

		if (cmdDef.getApplicationId() != req.getDiameterApplication()) {
			throw new GenerationException("Cannot generate a response " + cmdDef.getAnswerName()
					+ " because it has an applicationId (" + cmdDef.getApplicationId()
					+ ") that differs from the passed " + "DiameterClient (" + req.getDiameterApplication());
		}

		List<com.nextenso.proxylet.diameter.DiameterAVP> avps = getAVPs(bean);

		DiameterResponse resp = req.getResponse();
		resp.removeDiameterAVPs();

		for (com.nextenso.proxylet.diameter.DiameterAVP avp : avps) {
			resp.addDiameterAVP(avp);
		}

		if (checker != null) {
			checkResponse(resp);
		}

		return resp;
	}
	
	/**
	 * Try to generate a Diameter Answer from the passed annotated POJO and check
	 * it against its definition found in the dictionary.
	 * 
	 * An exception will be thrown in the following case:
	 * <ul>
	 * <li>The {@link DiameterCommand DiameterCommand} annotation could not be found
	 * on the object
	 * <li>A referenced Diameter Command or AVP could not be found in the dictionaries
	 * <li>A value couldn't be encoded in the corresponding Diameter format
	 * <li>the generated request failed the check against its definition, except
	 * if the check is disabled disabled
	 * </ul>
	 * If the DiameterCommandBuilder has been constructor with fillSystemAVPs set to true, the
	 * following AVPs will be filled with info from the DiameterClient:
	 * <ul>
	 * <li>Session-Id (if stateful),
	 * <li>Destination-Host
	 * <li>Destination-Realm
	 * <li>Auth-Application-Id,
	 * <li>Acct-Application-Id
	 * </ul>
	 * @param request the request to generate a response for
	 * @param bean    the annotated java object to generate the request from
	 * @return the filled {@link DiameterResponse DiameterResponse}
	 * @throws GenerationException if the passed annotated object is malformed, if a
	 *                             check failed, etc
	 */
	public DiameterResponse buildResponse(DiameterClient client, DiameterRequest req, Object bean) throws GenerationException {
		DiameterResponse resp = buildResponse(req, bean);
		
		if(fillSystemAVPs) {
			client.fillMessage(resp);
		}
		
		return resp;
	}

	private void checkRequest(DiameterRequest req) throws GenerationException {
		try {
			checker.checkRequest(req);
		} catch (CheckException e) {
			throw new GenerationException("Check failed, see underlying exception", e);
		}
	}

	private void checkResponse(DiameterResponse resp) throws GenerationException {
		try {
			checker.checkResponse(resp);
		} catch (CheckException e) {
			throw new GenerationException("Check failed, see underlying exception", e);
		}
	}

	private DiameterCommandDefinition getCommandDefinition(Object bean) throws GenerationException {
		Class<?> klass = bean.getClass();

		DiameterCommand dcAnnotation = klass.getAnnotation(DiameterCommand.class);

		if (dcAnnotation == null) {
			throw new GenerationException("Object is not annotated with DiameterCommand annotation");
		} else if (dcAnnotation.name() == null || dcAnnotation.name().isEmpty()) {
			if (dcAnnotation.abbreviation() != null && !dcAnnotation.abbreviation().isEmpty()
					&& dcAnnotation.vendorId() != 1L) {
				DiameterCommandDefinition def = cmdDico.getCommandDefinitionByAbbreviation(dcAnnotation.abbreviation(),
						dcAnnotation.vendorId());

				if (def == null) {
					throw new GenerationException("No command definition found for abbreviation "
							+ dcAnnotation.abbreviation() + " and vendorId " + dcAnnotation.vendorId());
				} else {
					return def;
				}
			} else {
				throw new GenerationException("Diameter Command name and abbreviation missing in annotation");
			}
		} else {
			DiameterCommandDefinition def = cmdDico.getCommandDefinitionByName(dcAnnotation.name());

			if (def == null) {
				throw new GenerationException("No command definition found for name " + dcAnnotation.name());
			} else {
				return def;
			}
		}
	}

	private List<com.nextenso.proxylet.diameter.DiameterAVP> getAVPs(Object bean) throws GenerationException {
		com.nextenso.proxylet.diameter.DiameterAVP currentGroup = null;
		List<com.nextenso.proxylet.diameter.DiameterAVP> currentGroupAVPs = null;

		List<CommandElement> elements = getCommandElements(bean);
		List<com.nextenso.proxylet.diameter.DiameterAVP> avps = new ArrayList<>(elements.size());

		for (CommandElement ce : elements) {
			DiameterAVPDefinition avpDef = avpDico.getAVPDefinitionByName(ce.avp.name());
			if (avpDef == null) {
				throw new GenerationException("no AVP Definition found in dictionary for " + ce.avp.name());
			}

			com.nextenso.proxylet.diameter.DiameterAVP myAVP = new com.nextenso.proxylet.diameter.DiameterAVP(avpDef);

			if (ce.value != null) {
				try {
					myAVP.setValue(avpDef.getDiameterAVPFormat().encode(ce.value), false);
				} catch (Exception e) {
					throw new GenerationException("failed to encode value " + ce.value, e);
				}
			} else if (ce.avp.skipIfNull()) {
				continue;
			}

			if (ce.avp.group() != null && !ce.avp.group().isEmpty()) {
				DiameterAVPDefinition groupDef = avpDico.getAVPDefinitionByName(ce.avp.group());
				if (groupDef == null) {
					throw new GenerationException(
							"no AVP definition found " + "in dictionary for group AVP " + ce.avp.group());
				}
				if (!(groupDef.getDiameterAVPFormat() instanceof GroupedFormat)) {
					throw new GenerationException("AVP " + groupDef.getAVPName() + " is not of type Grouped");
				}
				
				
				if (currentGroup == null) {
					currentGroup = new com.nextenso.proxylet.diameter.DiameterAVP(groupDef);
					currentGroupAVPs = new ArrayList<>();
				} else if(!currentGroup.getDiameterAVPDefinition().equals(groupDef)) {
					currentGroup.setValue(GroupedFormat.toGroupedAVP(currentGroupAVPs), true);
					avps.add(currentGroup);
					currentGroup = new com.nextenso.proxylet.diameter.DiameterAVP(groupDef);
					currentGroupAVPs = new ArrayList<>();
				}
				
				currentGroupAVPs.add(myAVP);
			} else if(currentGroup != null && currentGroupAVPs != null){
				currentGroup.setValue(GroupedFormat.toGroupedAVP(currentGroupAVPs), true);
				avps.add(currentGroup);
				currentGroup = null;
				currentGroupAVPs = null;
			} else {
				avps.add(myAVP);
			}
		}

		if (currentGroup != null && currentGroupAVPs != null) {
			currentGroup.setValue(GroupedFormat.toGroupedAVP(currentGroupAVPs), true);
			avps.add(currentGroup);
		}

		return avps;
	}

	private List<CommandElement> getCommandElements(Object bean) throws GenerationException {
		Class<?> klass = bean.getClass();
		List<CommandElement> ceList = new ArrayList<>();

		for (Field f : klass.getDeclaredFields()) {
			DiameterAVP avpAnnotation = f.getAnnotation(DiameterAVP.class);
			if (avpAnnotation == null) {
				continue;
			}

			if (avpAnnotation.name() == null || avpAnnotation.name().isEmpty()) {
				throw new GenerationException("null or empty avp name annotation on " + f);
			}
			Object value;
			try {
				f.setAccessible(true);
				value = f.get(bean);
			} catch (Exception e) {
				throw new GenerationException("Accessing field " + f + " raised an exception", e);
			}

			ceList.add(new CommandElement(avpAnnotation, value));
		}

		return ceList;

	}

}
