package com.alcatel.as.service.discovery.impl.etcd;

public class EtcdWatchException extends Exception{
	private static final long serialVersionUID = 1L;

	public EtcdWatchException(Throwable e) {
		super(e);
	}
}
