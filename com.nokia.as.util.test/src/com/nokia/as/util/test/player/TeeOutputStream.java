package com.nokia.as.util.test.player;

import java.io.IOException;
import java.io.OutputStream;

public final class TeeOutputStream extends OutputStream {

	private final OutputStream _out1;
	private final OutputStream _out2;

	public TeeOutputStream(OutputStream out, OutputStream tee) {
		this._out1 = out;
		this._out2 = tee;
	}

	@Override
	public void write(int b) throws IOException {
		_out1.write(b);
		_out2.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		_out1.write(b);
		_out2.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		_out1.write(b, off, len);
		_out2.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		_out1.flush();
		_out2.flush();
	}

	@Override
	public void close() throws IOException {
		try {
			_out1.close();
		} finally {
			_out2.close();
		}
	}
}
