package com.nextenso.proxylet.diameter.util;

import java.nio.charset.Charset;
import java.util.Locale;

import com.nextenso.proxylet.diameter.DiameterPeer;

/**
 * The DiameterURI AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information.
 */
public class URIFormat
		extends DiameterAVPFormat {

	/**
	 * The default charset.
	 */
	private static Charset DEFAULT_CHARSET = Charset.defaultCharset();

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The single static instance.
	 */
	public final static URIFormat INSTANCE = new URIFormat();

	protected URIFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "URI=" + (getURI(data, off, len).toString());
	}

	/**
	 * Decodes an AVP value.
	 * 
	 * @param data The value to decode. May be null.
	 * @return The decoded URI. Or null if data is null.
	 */
	public static final URI getURI(byte[] data) {
		if (data == null) return null;
		return getURI(data, 0, data.length);
	}

	/**
	 * Decodes an AVP value.
	 * 
	 * @param data The value to decode. Must not be null.
	 * @param offset The offset in the value.
	 * @param length The length of the address value.
	 * @return The decoded URI.
	 */
	public static final URI getURI(byte[] data, int offset, int length) {
		String uri = new String(data, offset, length, DEFAULT_CHARSET);
		return new URI(uri);
	}

	/**
	 * Encodes into a DiameterURI AVP value.
	 * 
	 * @param uri The URI to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toURI(URI uri) {
		return uri.toString().getBytes(DEFAULT_CHARSET);
	}

	/**
	 * Encodes into a DiameterURI AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param uri The URI to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toURI(byte[] destination, int destinationOffset, URI uri) {
		byte[] b = uri.toString().getBytes(DEFAULT_CHARSET);
		System.arraycopy(b, 0, destination, destinationOffset, b.length);
		return b.length;
	}

	/**
	 * The inner class that wraps a Diameter URI.
	 */
	public static class URI {

		/**
		 * The constant that represents TCP transport.
		 */
		public static final String TRANSPORT_TCP = "tcp";
		/**
		 * The constant that represents SCTP transport.
		 */
		public static final String TRANSPORT_SCTP = "sctp";
		/**
		 * The constant that represents UDP transport.
		 */
		public static final String TRANSPORT_UDP = "udp";
		/**
		 * The constant that represents Diameter protocol.
		 */
		public static final String PROTOCOL_DIAMETER = "diameter";
		/**
		 * The constant that represents Radius protocol.
		 */
		public static final String PROTOCOL_RADIUS = "radius";
		/**
		 * The constant that represents TACACS+ protocol.
		 */
		public static final String PROTOCOL_TACACS_PLUS = "tacacs+";

		private static final String AAA_SCHEME = "aaa://";
		private static final String AAAS_SCHEME = "aaas://";
		private static final String TRANSPORT_PART = ";transport=";
		private static final String PROTOCOL_PART = ";protocol=";

		private String _host;
		private String _transport = TRANSPORT_SCTP;
		private String _protocol = PROTOCOL_DIAMETER;
		private int _port = DiameterPeer.DIAMETER_PORT;
		private boolean _secure = false;

		/**
		 * Constructs a new URI given its String representation.
		 * 
		 * @param val the String representation of the URI.
		 * @throws IllegalArgumentException if the value cannot be parsed.
		 */
		public URI(String val) {
			boolean secure = false;
			String value = val.toLowerCase(Locale.getDefault());
			int begin = 0;
			if (value.startsWith(AAA_SCHEME)) {
				begin += 6;
			} else if (value.startsWith(AAAS_SCHEME)) {
				secure = true;
				begin += 7;
			} else {
				throw new IllegalArgumentException("Invalid URI: " + value + " - bad scheme");
			}

			String host = null;
			int port = 0;

			int col = value.indexOf(':', begin);
			int semi = value.indexOf(';', begin);
			if (col != -1) {
				host = value.substring(begin, col);
				try {
					if (semi != -1) {
						port = Integer.parseInt(value.substring(col + 1, semi));
					} else {
						port = Integer.parseInt(value.substring(col + 1));
					}
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid URI: " + value + " - bad port", e);
				}
			} else {
				if (semi != -1) {
					host = value.substring(begin, semi);
				} else {
					host = value.substring(begin);
				}
			}

			String transport = null;
			int index = value.indexOf(TRANSPORT_PART, semi);
			if (index != -1) {
				int end = value.indexOf(';', index + 11);
				if (end != -1) {
					transport = value.substring(index + 11, end);
				} else {
					transport = value.substring(index + 11);
				}
			}

			String protocol = null;
			index = value.indexOf(PROTOCOL_PART, semi);
			if (index != -1) {
				int end = value.indexOf(';', index + 10);
				if (end != -1) {
					protocol = value.substring(index + 10, end);
				} else {
					protocol = value.substring(index + 10);
				}
			}

			setUriData(secure, host, port, transport, protocol);
		}

		/**
		 * Constructs a new IPFilterRule given its String representation.
		 * 
		 * @param secure The secure flag (for aaa or aaas scheme).
		 * @param host The host.
		 * @param port The port.
		 * @param transport The transport.
		 * @param protocol The protocol.
		 */
		public URI(boolean secure, String host, int port, String transport, String protocol) {
			setUriData(secure, host, port, transport, protocol);
		}

		private void setUriData(boolean secure, String host, int port, String transport, String protocol) {
			setSecure(secure);
			setHost(host);
			setPort(port);
			setTransport(transport);
			setProtocol(protocol);

			// verify the consistency of the URI if syntax is valid (see RFC 3588 section 4.3)
			// not diameter and UDP
			if (PROTOCOL_DIAMETER.equals(getProtocol()) && TRANSPORT_UDP.equals(getTransport())) {
				throw new IllegalArgumentException("Invalid URI:  diameter does not support UDP");
			}
		}

		/**
		 * Returns the host.
		 * 
		 * @return The host.
		 */
		public String getHost() {
			return _host;
		}

		/**
		 * Sets the host.
		 * 
		 * @param host The host.
		 */
		public void setHost(String host) {
			if (host == null || host.trim().isEmpty()) {
				throw new IllegalArgumentException("Invalid URI:  bad host");
			}

			_host = host.toLowerCase(Locale.getDefault());
		}

		/**
		 * Specifies if the URI is secure.
		 * 
		 * @return true if scheme is aaas, false if scheme is aaa.
		 */
		public boolean isSecure() {
			return _secure;
		}

		/**
		 * Sets the scheme security level (aaa or aaas).
		 * 
		 * @param secure true if aaas, false if aaa.
		 */
		public void setSecure(boolean secure) {
			_secure = secure;
		}

		/**
		 * Sets the port.
		 * 
		 * @param port The port.
		 */
		public void setPort(int port) {
			if (port == 0) {
				_port = DiameterPeer.DIAMETER_PORT;
				return;
			}
			if (port < 0 || port > 0xFFFF)
			    throw new IllegalArgumentException("Invalid URI: invalid port : "+port);;
			_port = port;
		}

		/**
		 * Returns the port.
		 * 
		 * @return The port.
		 */
		public int getPort() {
			return _port;
		}

		/**
		 * Returns the transport.
		 * 
		 * @return The transport.
		 */
		public String getTransport() {
			return _transport;
		}

		/**
		 * Sets the transport.
		 * 
		 * @param transport The transport.
		 */
		public void setTransport(String transport) {
			if (transport == null) {
				_transport = TRANSPORT_SCTP;
				return;
			}
			transport = transport.toLowerCase(Locale.getDefault());

			// valid transport
			if (!TRANSPORT_TCP.equals(transport) &&
			    !TRANSPORT_SCTP.equals(transport) &&
			    !TRANSPORT_UDP.equals(transport)) {
			    throw new IllegalArgumentException("Invalid URI: invalid transport : "+transport);
			}

			_transport = transport;
		}

		/**
		 * Returns the protocol.
		 * 
		 * @return The protocol.
		 */
		public String getProtocol() {
			return _protocol;
		}

		/**
		 * Sets the protocol.
		 * 
		 * @param protocol The protocol.
		 */
		public void setProtocol(String protocol) {
			if (protocol == null) {
				_protocol = PROTOCOL_DIAMETER;
				return;
			}
			protocol = protocol.toLowerCase(Locale.getDefault());

			// valid protocol
			if (!PROTOCOL_DIAMETER.equals(protocol) && !PROTOCOL_RADIUS.equals(protocol) && !PROTOCOL_TACACS_PLUS.equals(protocol)) {
				throw new IllegalArgumentException("Invalid URI: invalid protocol : "+protocol);
			}

			_protocol = protocol;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append((isSecure()) ? AAAS_SCHEME : AAA_SCHEME);
			buffer.append(getHost());
			buffer.append(':').append(getPort());
			buffer.append(";transport=").append(getTransport());
			buffer.append(";protocol=").append(getProtocol());
			return buffer.toString();
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_host == null) ? 0 : _host.hashCode());
			result = prime * result + _port;
			result = prime * result + ((_protocol == null) ? 0 : _protocol.hashCode());
			result = prime * result + (_secure ? 1231 : 1237);
			result = prime * result + ((_transport == null) ? 0 : _transport.hashCode());
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			URI other = (URI) obj;
			if (_port != other._port) {
				return false;
			}
			if (_secure != other._secure) {
				return false;
			}
			if (!_host.equals(other._host)) {
				return false;
			}
			if (!_protocol.equals(other._protocol)) {
				return false;
			}
			if (!_transport.equals(other._transport)) {
				return false;
			}
			return true;
		}

	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof String) {
			return toURI(new URI((String) value));
		} else if(value instanceof URI) {
			return toURI((URI) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A String or"  + URI.class + " is expected");
		}
	}
}
