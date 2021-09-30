package com.nsn.ood.cls.util.osgi.transaction;

import java.sql.Connection;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;

@Component(provides = TransactionService.class)
public class TransactionService {

	@ServiceDependency
	private volatile TransactionControl txControl;
	
	@ServiceDependency
	private volatile JDBCConnectionProvider provider;
	
	public Connection getConnection() {
		return provider.getResource(txControl);
	}
	
	public TransactionControl txControl() {
		return this.txControl;
	}
}
