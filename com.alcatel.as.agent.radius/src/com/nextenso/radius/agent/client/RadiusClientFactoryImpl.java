package com.nextenso.radius.agent.client;

import com.nextenso.proxylet.radius.RadiusClient;
import com.nextenso.proxylet.radius.RadiusClientFactory;

public class RadiusClientFactoryImpl extends RadiusClientFactory {

	/**
	 * @see com.nextenso.proxylet.radius.RadiusClientFactory#create(java.lang.String, byte[])
	 */
    @Override
		protected RadiusClient create(String server, byte[] secret) {
			try {
      return new RadiusClientFacade( server, secret);
			} catch(Exception e) {
				throw new RuntimeException("Exception while instanciating RadiusClient", e);
			}
    }


    /**
     * @see com.nextenso.proxylet.radius.RadiusClientFactory#create(java.lang.String, byte[], java.lang.String)
     */
    @Override
		protected RadiusClient create(String server, byte[] secret, String callerId){
			try {
      return new RadiusClientFacade(server, secret, callerId);
			} catch(Exception e) {
				throw new RuntimeException("Exception while instanciating RadiusClient", e);
			}
		}
}

