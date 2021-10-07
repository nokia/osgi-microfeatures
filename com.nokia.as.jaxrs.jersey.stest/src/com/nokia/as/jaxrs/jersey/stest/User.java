package com.nokia.as.jaxrs.jersey.stest;

import javax.validation.constraints.NotNull;

public class User {

	@NotNull
	private String name;

	public User() {
	}

	public User(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}