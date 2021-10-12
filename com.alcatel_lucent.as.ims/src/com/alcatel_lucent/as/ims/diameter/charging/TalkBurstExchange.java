// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PoCChangeCondition;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Talk-Burst-Exchange AVP wrapper.
 */
public class TalkBurstExchange {

	private Date _pocChangeTime = null;
	private Long _numberOfTalkBursts = null;
	private Long _talkBurstVolume = null;
	private Long _talkBurstTime = null;
	private Long _numberOfReceivedTalkBursts = null;
	private Long _receivedTalkBurstVolume = null;
	private Long _receivedTalkBurstTime = null;
	private Long _numberOfParticipants = null;
	private PoCChangeCondition _pocChangeCondition = null;

	private TalkBurstExchange() {}

	public TalkBurstExchange(Date pocChangeTime) {
		this();
		setPocChangeTime(pocChangeTime);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public TalkBurstExchange(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getPocChangeTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			setPocChangeTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
		}

		def = ChargingUtils.getNumberOfTalkBurstsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfTalkBursts(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTalkBurstVolumeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTalkBurstVolume(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTalkBurstTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTalkBurstTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getNumberOfReceivedTalkBurstsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfReceivedTalkBursts(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getReceivedTalkBurstVolumeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setReceivedTalkBurstVolume(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getReceivedTalkBurstTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setReceivedTalkBurstTime(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getNumberOfParticipantsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setNumberOfParticipants(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPocChangeConditionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocChangeCondition(PoCChangeCondition.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getTalkBurstExchangeAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getPocChangeTimeAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(TimeFormat.toTime(getPocChangeTime().getTime()), false);
			l.add(avp);
		}

		Long value = getNumberOfTalkBursts();
		if (value != null) {
			def = ChargingUtils.getNumberOfTalkBurstsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}

		value = getTalkBurstVolume();
		if (value != null) {
			def = ChargingUtils.getTalkBurstVolumeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}

		value = getTalkBurstTime();
		if (value != null) {
			def = ChargingUtils.getTalkBurstTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}

		value = getNumberOfReceivedTalkBursts();
		if (value != null) {
			def = ChargingUtils.getNumberOfReceivedTalkBurstsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}
		value = getReceivedTalkBurstVolume();
		if (value != null) {
			def = ChargingUtils.getReceivedTalkBurstVolumeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}

		value = getReceivedTalkBurstTime();
		if (value != null) {
			def = ChargingUtils.getReceivedTalkBurstTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}

		value = getNumberOfParticipants();
		if (value != null) {
			def = ChargingUtils.getNumberOfParticipantsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(value), false);
				l.add(avp);
			}
		}

		if (getPocChangeCondition() != null) {
			def = ChargingUtils.getPocChangeConditionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPocChangeCondition().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the PoC-Change-Time.
	 * 
	 * @param time The time.
	 */
	public void setPocChangeTime(Date time) {
		if (time == null) {
			throw new NullPointerException("PoC-Change-Time cannot be null");
		}

		_pocChangeTime = (Date) time.clone();
	}

	/**
	 * Gets the PoC-Change-Time.
	 * 
	 * @return The time.
	 */
	public Date getPocChangeTime() {
		if (_pocChangeTime == null) {
			return null;
		}

		return (Date) _pocChangeTime.clone();
	}

	/**
	 * Sets the Number-Of-Talk-Bursts.
	 * 
	 * @param number The number.
	 */
	public void setNumberOfTalkBursts(Long number) {
		_numberOfTalkBursts = number;
	}

	/**
	 * Gets the Number-Of-Talk-Bursts.
	 * 
	 * @return The number.
	 */
	public Long getNumberOfTalkBursts() {
		return _numberOfTalkBursts;
	}

	/**
	 * Sets the Talk-Burst-Volume.
	 * 
	 * @param volume The volume.
	 */
	public void setTalkBurstVolume(Long volume) {
		_talkBurstVolume = volume;
	}

	/**
	 * Gets the Talk-Burst-Volume.
	 * 
	 * @return The volume.
	 */
	public Long getTalkBurstVolume() {
		return _talkBurstVolume;
	}

	/**
	 * Sets the Talk-Burst-Time.
	 * 
	 * @param time The time.
	 */
	public void setTalkBurstTime(Long time) {
		_talkBurstTime = time;
	}

	/**
	 * Gets the Talk-Burst-Time.
	 * 
	 * @return The time.
	 */
	public Long getTalkBurstTime() {
		return _talkBurstTime;
	}

	/**
	 * Sets the Number-Of-Received-Talk-Bursts.
	 * 
	 * @param number The number.
	 */
	public void setNumberOfReceivedTalkBursts(Long number) {
		_numberOfReceivedTalkBursts = number;
	}

	/**
	 * Gets the Number-Of-Received-Talk-Bursts.
	 * 
	 * @return The number.
	 */
	public Long getNumberOfReceivedTalkBursts() {
		return _numberOfReceivedTalkBursts;
	}

	/**
	 * Sets the Received-Talk-Burst-Volume.
	 * 
	 * @param volume The volume.
	 */
	public void setReceivedTalkBurstVolume(Long volume) {
		_receivedTalkBurstVolume = volume;
	}

	/**
	 * Gets the Received-Talk-Burst-Volume.
	 * 
	 * @return The volume.
	 */
	public Long getReceivedTalkBurstVolume() {
		return _receivedTalkBurstVolume;
	}

	/**
	 * Sets the Received-Talk-Burst-Time.
	 * 
	 * @param time The time.
	 */
	public void setReceivedTalkBurstTime(Long time) {
		_receivedTalkBurstTime = time;
	}

	/**
	 * Gets the Received-Talk-Burst-Time.
	 * 
	 * @return The time.
	 */
	public Long getReceivedTalkBurstTime() {
		return _receivedTalkBurstTime;
	}

	/**
	 * Sets the PoC-Change-Condition.
	 * 
	 * @param condition The condition.
	 */
	public void setPocChangeCondition(PoCChangeCondition condition) {
		_pocChangeCondition = condition;
	}

	/**
	 * Gets the PoC-Change-Condition.
	 * 
	 * @return The condition.
	 */
	public PoCChangeCondition getPocChangeCondition() {
		return _pocChangeCondition;
	}

	/**
	 * Sets the Number-Of-Participants.
	 * 
	 * @param number The number.
	 */
	public void setNumberOfParticipants(Long number) {
		_numberOfParticipants = number;
	}

	/**
	 * Gets the Number-Of-Participants.
	 * 
	 * @return The number.
	 */
	public Long getNumberOfParticipants() {
		return _numberOfParticipants;
	}

}
