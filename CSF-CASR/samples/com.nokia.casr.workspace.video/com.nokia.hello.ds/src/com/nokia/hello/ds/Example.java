package com.nokia.hello.ds;

import org.osgi.service.component.annotations.*;

import com.nokia.database.ds.IDatabase;

@Component
public class Example {

	@Reference
	IDatabase database;

	@Activate
	public void start() {
		System.out.println("example started: " + database.get());
	}

	@Deactivate
	public void stop() {
		System.out.println("example stopped");
	}
}
