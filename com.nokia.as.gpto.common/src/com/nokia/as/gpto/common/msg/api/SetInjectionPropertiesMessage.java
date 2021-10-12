// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.common.msg.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class SetInjectionPropertiesMessage extends GPTOMessage {
	/**
	 * 
	 */
	protected static final long serialVersionUID = 65636000000L;
	public int id;
	private String json;

	public SetInjectionPropertiesMessage() {
	}

	public SetInjectionPropertiesMessage(int id, String value) {
		super();
		this.id = id;
		this.json = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMap() {
		return json;
	}

	public void setMap(String map) {
		this.json = map;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(id);
		out.writeUTF(json);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		id = in.readInt();
		json = in.readUTF();
	}

	@Override
	public String toString() {
		return "SetPropertiesMessage [id=" + id + ", json=" + json + "]";
	}

}
