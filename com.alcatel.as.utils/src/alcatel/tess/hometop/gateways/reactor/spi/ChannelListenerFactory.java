// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.spi;

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

/**
 * Any OSGI service can be registered with this interface in order to provide Channel Listener Filters.
 */
@ConsumerType
public interface ChannelListenerFactory {

	TcpClientChannelListener createListener(TcpClientChannelListener listener, Map<?, Object> options)
			throws Exception;
	
	TcpServerChannelListener createListener(TcpServerChannelListener listener, Map<?, Object> options)
			throws Exception;
	
}
