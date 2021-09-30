package com.nokia.as.keycloak.jaxrs.adapter;

import java.util.ArrayList;
import java.util.List;

public class WebXml {

	private List<SecurityConstraint> securityConstraints = new ArrayList<>();

	public List<SecurityConstraint> getSecurityConstraints() {
		return securityConstraints;
	}

	public void setSecurityConstraint(List<SecurityConstraint> securityConstraints) {
		this.securityConstraints = securityConstraints;
	}

	public void addConstraint(SecurityConstraint securityConstraint) {
		securityConstraints.add(securityConstraint);
	}
	
	@Override
	public String toString() {
		return "Web [securityConstraint=" + securityConstraints + "]";
	}
}
