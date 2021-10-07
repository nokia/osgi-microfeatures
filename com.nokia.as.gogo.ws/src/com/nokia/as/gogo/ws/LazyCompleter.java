package com.nokia.as.gogo.ws;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class LazyCompleter implements Completer {
	
	private volatile Completer wrapped;

	@Override
	public void complete(LineReader arg0, ParsedLine arg1, List<Candidate> arg2) {
		if(wrapped != null) {
			wrapped.complete(arg0, arg1, arg2);
		}
	}

	public void setWrapped(Completer wrapped) {
		this.wrapped = wrapped;
	}
}
