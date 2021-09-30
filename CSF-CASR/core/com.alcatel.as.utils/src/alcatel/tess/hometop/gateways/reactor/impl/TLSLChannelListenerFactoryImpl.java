package alcatel.tess.hometop.gateways.reactor.impl;

import java.util.Map;

import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.spi.ChannelListenerFactory;

/**
 * OSGI service registered from the Activator.
 */
public class TLSLChannelListenerFactoryImpl implements ChannelListenerFactory {

	// injected 
	private ReactorProvider _provider;
	
	@Override
	public TcpClientChannelListener createListener(TcpClientChannelListener listener, Map<?, Object> options) throws Exception {
		return new TcpClientListenerTLSFilter(listener, options, (ReactorProviderImpl) _provider);
	}

	@Override
	public TcpServerChannelListener createListener(TcpServerChannelListener listener, Map<?, Object> options) throws Exception {
		throw new UnsupportedOperationException("method not implemented");
	}

}
