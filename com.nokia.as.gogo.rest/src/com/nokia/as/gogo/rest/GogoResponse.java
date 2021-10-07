package com.nokia.as.gogo.rest;

public class GogoResponse {
	private String standardOut;
	private String errorOut;
	
	public GogoResponse(String standardOut, String errorOut) {
		super();
		this.standardOut = standardOut;
		this.errorOut = errorOut;
	}

	public String getStandardOut() {
		return standardOut;
	}

	public void setStandardOut(String standardOut) {
		this.standardOut = standardOut;
	}

	public String getErrorOut() {
		return errorOut;
	}

	public void setErrorOut(String errorOut) {
		this.errorOut = errorOut;
	}
	
	
	
}
