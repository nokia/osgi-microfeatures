package com.nextenso.proxylet.diameter.dictionary;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * An element of a Diameter command. Includes the Diameter AVP definition,
 * the minimum occurrence and maximum occurrence (or -1 if unbounded)
 *
 */
public class DiameterCommandDefinitionElement {
	private DiameterAVPDefinition avpDefinition;
	private int minOccurence;
	private int maxOccurence; // -1 for unlimited
	private boolean fixed; 
	
	public DiameterCommandDefinitionElement(DiameterAVPDefinition avpDefinition, int minOccurence, int maxOccurence) {
		super();
		this.avpDefinition = avpDefinition;
		this.minOccurence = minOccurence;
		this.maxOccurence = maxOccurence;
	}

	/**
	 * The diameter AVP definition
	 * @return a Diameter AVP definition
	 */
	public DiameterAVPDefinition getAVPDefinition() {
		return avpDefinition;
	}

	/**
	 * Minimum occurrence. can be 0 if the AVP is optional
	 * @return minimum occurrence
	 */
	public int getMinOccurence() {
		return minOccurence;
	}

	/**
	 * Maximum occurrence or -1 if unbounded
	 * @return maximum occurrence
	 */
	public int getMaxOccurence() {
		return maxOccurence;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((avpDefinition == null) ? 0 : avpDefinition.hashCode());
		result = prime * result + maxOccurence;
		result = prime * result + minOccurence;
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
		DiameterCommandDefinitionElement other = (DiameterCommandDefinitionElement) obj;
		if (avpDefinition == null) {
			if (other.avpDefinition != null)
				return false;
		} else if (!avpDefinition.equals(other.avpDefinition))
			return false;
		if (maxOccurence != other.maxOccurence)
			return false;
		if (minOccurence != other.minOccurence)
			return false;
		return true;
	}
	
}
