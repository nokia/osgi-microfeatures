package com.nokia.casr.samples.redis;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;

import redis.clients.jedis.Jedis;

@Component
public class Example {

	@Start
	void start() {
		String string = "tcp://sand1.sandbox.compaas.vlab.us.alcatel-lucent.com:9104";
		System.out.println("Example.start redis url:" + string);
		Jedis jedis = new Jedis(string);
		jedis.auth("password");
		jedis.set("foo", "bar");
		String value = jedis.get("foo");
		System.out.println("Value from Redis = " + value);
		jedis.close();

	}
}
