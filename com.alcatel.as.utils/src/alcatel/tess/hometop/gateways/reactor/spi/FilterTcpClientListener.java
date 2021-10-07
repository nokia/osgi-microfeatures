package alcatel.tess.hometop.gateways.reactor.spi;

import java.util.Map;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;

/**
 * This class is the superclass of all classes that filter Tcp Client Listeners. These classes sit on top of an already existing Tcp Client listener 
 * which it uses as its basic sink of data, but possibly transforming the data along the way or providing additional functionality
 */
public class FilterTcpClientListener extends FilterTcpListener implements TcpClientChannelListener {

	protected final TcpClientChannelListener _listener;
	
    public FilterTcpClientListener(TcpClientChannelListener listener, Map<?, Object> options) {
    	super(listener, options);
    	_listener = listener;
	}
		
	public void connectionEstablished(TcpChannel cnx) {
		setChannel(cnx);
		_listener.connectionEstablished(cnx);
	}
	
	public void connectionFailed(TcpChannel cnx, Throwable error) {
		setChannel(cnx);
		_listener.connectionFailed(cnx, error);
	}
	
}
