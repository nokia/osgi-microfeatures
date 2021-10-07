package com.nokia.as.gpto.common.msg.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public abstract class GPTOMessage implements Externalizable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6563606592490417965L;

	public GPTOMessage() {
		
	}
	
	public ByteBuffer toBytes() throws IOException {
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
				ObjectOutputStream oos = new ObjectOutputStream(baos)) {

			oos.writeObject(this);
			return ByteBuffer.wrap(baos.toByteArray());
		}
	}
	
	public static GPTOMessage fromBytes(ByteBuffer buffer) throws Exception {	
		byte[] b = new byte[buffer.remaining()];
		buffer.get(b);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(b);
				ObjectInputStream ois = new ObjectInputStream(bais)) {
			GPTOMessage msg = (GPTOMessage) ois.readObject();
			return msg;
		}
	}	
}
