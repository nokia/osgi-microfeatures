// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.Originator;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Message-Body AVP wrapper.
 */
public class MessageBody {

	private String _contentType = null;
	private Long _contentLength = null;
	private String _contentDisposition = null;
	private Originator _originator = null;

	public MessageBody(String contentType, Long contentLength, Version version)
			throws DiameterMissingAVPException {
		if (version == null) {
			throw new IllegalArgumentException("null version");
		}

		if (contentType == null) {
			DiameterAVPDefinition def = ChargingUtils.getContentTypeAVP(version);
			throw new DiameterMissingAVPException(def);
		}
		_contentType = contentType;
		if (contentLength == null) {
			DiameterAVPDefinition def = ChargingUtils.getContentLengthAVP(version);
			throw new DiameterMissingAVPException(def);
		}
		_contentLength = contentLength;
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MessageBody(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MessageBody(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getContentTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				_contentType = UTF8StringFormat.getUtf8String(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getContentLengthAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				if (def.getDiameterAVPFormat() == UTF8StringFormat.INSTANCE) {
					_contentLength = Long.parseLong(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
				} else {
					_contentLength = Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0));
				}
			}
		}

		def = ChargingUtils.getContentDispositionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setContentDisposition(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getOriginatorAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOriginator(Originator.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getMessageBodyAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getContentTypeAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getContentType()), false);
			l.add(avp);
		}

		def = ChargingUtils.getContentLengthAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			if (def.getDiameterAVPFormat() == Unsigned32Format.INSTANCE) {
				avp.setValue(Unsigned32Format.toUnsigned32(getContentLength()), false);
			} else {
				avp.setValue(UTF8StringFormat.toUtf8String(Long.toString(getContentLength())), false);
			}
			l.add(avp);
		}

		if (getContentDisposition() != null) {
			def = ChargingUtils.getContentDispositionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getContentDisposition()), false);
				l.add(avp);
			}
		}

		if (getOriginator() != null) {
			def = ChargingUtils.getOriginatorAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getOriginator().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Gets the content type (mapped to the Content-Type AVP).
	 * 
	 * @return The content type.
	 */
	public String getContentType() {
		return _contentType;
	}

	/**
	 * Gets the content length (mapped to the Content-Length AVP).
	 * 
	 * @return The content Length.
	 */
	public long getContentLength() {
		return _contentLength;
	}

	/**
	 * Sets the content disposition.
	 * 
	 * @param disposition The content disposition.
	 */
	public void setContentDisposition(String disposition) {
		_contentDisposition = disposition;
	}

	/**
	 * Gets the content disposition (mapped to the Content-Disposition AVP).
	 * 
	 * @return The content disposition.
	 */
	public String getContentDisposition() {
		return _contentDisposition;
	}

	/**
	 * Sets the originator.
	 * 
	 * @param originator The originator.
	 */
	public void setOriginator(Originator originator) {
		_originator = originator;
	}

	/**
	 * Gets the originator (mapped to the Originator AVP).
	 * 
	 * @return The originator.
	 */
	public Originator getOriginator() {
		return _originator;
	}
}
