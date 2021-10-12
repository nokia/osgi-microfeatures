// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.StringTokenizer;

/**
 * The IPFilterRule AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information.
 */
public class IPFilterRuleFormat
		extends DiameterAVPFormat {

	/**
	 * The ASCII charset.
	 */
	private static Charset ASCII_CHARSET = null;
	static {
		try {
			ASCII_CHARSET = Charset.forName("ascii");
		}
		catch (UnsupportedCharsetException e) {}
	}

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The single static instance.
	 */
	public static final IPFilterRuleFormat INSTANCE = new IPFilterRuleFormat();

	protected IPFilterRuleFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "IPFilterRule=" + getIPFilterRule(data, off, len);
	}

	/**
	 * Decodes an IPFilterRule AVP value.
	 * 
	 * @param data The data to decode. May be null.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @return The decoded IPFilterRule. Or null if data is null.
	 */
	public IPFilterRule getIPFilterRule(byte[] data) {
		if (data == null) return null;
		return getIPFilterRule(data, 0, data.length);
	}

	/**
	 * Decodes an IPFilterRule AVP value.
	 * 
	 * @param data The data to decode. Must not be null.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @return The decoded IPFilterRule.
	 */
	public IPFilterRule getIPFilterRule(byte[] data, int offset, int length) {
		if (ASCII_CHARSET == null) {
			throw new RuntimeException("ASCII charset not supported");
		}
		String rule = null;
		try {
			rule = new String(data, offset, length, ASCII_CHARSET);
			return new IPFilterRule(rule);
		}
		catch (IllegalArgumentException iae) {
			throw iae;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid IPFilterRule: " + rule, e);
		}
	}

	/**
	 * Encodes into an IPFilterRule AVP value.
	 * 
	 * @param rule The rule to encode.
	 * @return The encoded value.
	 */
	public byte[] toIPFilterRule(IPFilterRule rule) {
		if (ASCII_CHARSET == null) {
			throw new RuntimeException("ASCII charset not supported");
		}
		return rule.toString().getBytes(ASCII_CHARSET);
	}

	/**
	 * Encodes into an IPFilterRule AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param rule The rule to encode.
	 * @return The length of the encoded value.
	 */
	public int toIPFilterRule(byte[] destination, int destinationOffset, IPFilterRule rule) {
		if (ASCII_CHARSET == null) {
			throw new RuntimeException("ASCII charset not supported");
		}
		byte[] b = rule.toString().getBytes(ASCII_CHARSET);
		System.arraycopy(b, 0, destination, destinationOffset, b.length);
		return b.length;
	}

	/**
	 * The inner class that wraps an IPFilter Rule.
	 */
	public static class IPFilterRule {

		/**
		 * The constant to represent the permit action.
		 */
		public static final String ACTION_PERMIT = "permit";
		/**
		 * The constant to represent the deny action.
		 */
		public static final String ACTION_DENY = "deny";

		/**
		 * The constant to represent the in direction.
		 */
		public static final String DIR_IN = "in";
		/**
		 * The constant to represent the out direction.
		 */
		public static final String DIR_OUT = "out";

		/**
		 * The constant to represent the IP protocol.
		 */
		public static final String PROTOCOL_IP = "ip";

		/**
		 * The constant to represent the TCP protocol.
		 */
		public static final String PROTOCOL_TCP = "6";

		/**
		 * The constant to represent the UDP protocol.
		 */
		public static final String PROTOCOL_UDP = "17";

		/**
		 * The constant to represent IP option SSRR.
		 */
		public static final int IP_OPTION_SSRR = 1;
		/**
		 * The constant to represent IP option LSRR.
		 */
		public static final int IP_OPTION_LSRR = 1 << 2;
		/**
		 * The constant to represent IP option RR.
		 */
		public static final int IP_OPTION_RR = 1 << 4;
		/**
		 * The constant to represent IP option TS.
		 */
		public static final int IP_OPTION_TS = 1 << 6;
		/**
		 * The constant to represent IP option NOT_SSRR.
		 */
		public static final int IP_OPTION_NOT_SSRR = 2;
		/**
		 * The constant to represent IP option NOT_LSRR.
		 */
		public static final int IP_OPTION_NOT_LSRR = 2 << 2;
		/**
		 * The constant to represent IP option NOT_RR.
		 */
		public static final int IP_OPTION_NOT_RR = 2 << 4;
		/**
		 * The constant to represent IP option NOT_TS.
		 */
		public static final int IP_OPTION_NOT_TS = 2 << 6;

		/**
		 * The constant to represent TCP option MSS.
		 */
		public static final int TCP_OPTION_MSS = 1;
		/**
		 * The constant to represent TCP option WINDOW.
		 */
		public static final int TCP_OPTION_WINDOW = 1 << 2;
		/**
		 * The constant to represent TCP option SACK.
		 */
		public static final int TCP_OPTION_SACK = 1 << 4;
		/**
		 * The constant to represent TCP option TS.
		 */
		public static final int TCP_OPTION_TS = 1 << 6;
		/**
		 * The constant to represent TCP option CC.
		 */
		public static final int TCP_OPTION_CC = 1 << 8;
		/**
		 * The constant to represent TCP option NOT_MSS.
		 */
		public static final int TCP_OPTION_NOT_MSS = 2;
		/**
		 * The constant to represent TCP option NOT_WINDOW.
		 */
		public static final int TCP_OPTION_NOT_WINDOW = 2 << 2;
		/**
		 * The constant to represent TCP option NOT_SACK.
		 */
		public static final int TCP_OPTION_NOT_SACK = 2 << 4;
		/**
		 * The constant to represent TCP option NOT_TS.
		 */
		public static final int TCP_OPTION_NOT_TS = 2 << 6;
		/**
		 * The constant to represent TCP option NOT_CC.
		 */
		public static final int TCP_OPTION_NOT_CC = 2 << 8;

		/**
		 * The constant to represent TCP flag FIN.
		 */
		public static final int TCP_FLAG_FIN = 1;
		/**
		 * The constant to represent TCP flag SYN.
		 */
		public static final int TCP_FLAG_SYN = 1 << 2;
		/**
		 * The constant to represent TCP flag RST.
		 */
		public static final int TCP_FLAG_RST = 1 << 4;
		/**
		 * The constant to represent TCP flag PSH.
		 */
		public static final int TCP_FLAG_PSH = 1 << 6;
		/**
		 * The constant to represent TCP flag ACK.
		 */
		public static final int TCP_FLAG_ACK = 1 << 8;
		/**
		 * The constant to represent TCP flag URG.
		 */
		public static final int TCP_FLAG_URG = 1 << 10;
		/**
		 * The constant to represent TCP flag NOT_FIN.
		 */
		public static final int TCP_FLAG_NOT_FIN = 2;
		/**
		 * The constant to represent TCP flag NOT_SYN.
		 */
		public static final int TCP_FLAG_NOT_SYN = 2 << 2;
		/**
		 * The constant to represent TCP flag NOT_RST.
		 */
		public static final int TCP_FLAG_NOT_RST = 2 << 4;
		/**
		 * The constant to represent TCP flag NOT_PSH.
		 */
		public static final int TCP_FLAG_NOT_PSH = 2 << 6;
		/**
		 * The constant to represent TCP flag NOT_ACK.
		 */
		public static final int TCP_FLAG_NOT_ACK = 2 << 8;
		/**
		 * The constant to represent TCP flag NOT_URG.
		 */
		public static final int TCP_FLAG_NOT_URG = 2 << 10;

		/**
		 * The constant to represent ICMP type ECHO_REPLY (different from the actual
		 * ICMP value).
		 */
		public static final int ICMP_TYPE_ECHO_REPLY = 1;
		/**
		 * The constant to represent ICMP type DESTINATION_UNREACHABLE (different
		 * from the actual ICMP value).
		 */
		public static final int ICMP_TYPE_DESTINATION_UNREACHABLE = 1 << 1;
		/**
		 * The constant to represent ICMP type SOURCE_QUENCH (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_SOURCE_QUENCH = 1 << 2;
		/**
		 * The constant to represent ICMP type REDIRECT (different from the actual
		 * ICMP value).
		 */
		public static final int ICMP_TYPE_REDIRECT = 1 << 3;
		/**
		 * The constant to represent ICMP type ECHO_REQUEST (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_ECHO_REQUEST = 1 << 4;
		/**
		 * The constant to represent ICMP type ROUTER_ADVERTISEMENT (different from
		 * the actual ICMP value).
		 */
		public static final int ICMP_TYPE_ROUTER_ADVERTISEMENT = 1 << 5;
		/**
		 * The constant to represent ICMP type ROUTER_SOLICITATION (different from
		 * the actual ICMP value).
		 */
		public static final int ICMP_TYPE_ROUTER_SOLICITATION = 1 << 6;
		/**
		 * The constant to represent ICMP type TTL_EXCEEDED (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_TTL_EXCEEDED = 1 << 7;
		/**
		 * The constant to represent ICMP type IP_HEADER_BAD (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_IP_HEADER_BAD = 1 << 8;
		/**
		 * The constant to represent ICMP type TIMESTAMP_REQUEST (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_TIMESTAMP_REQUEST = 1 << 9;
		/**
		 * The constant to represent ICMP type TIMESTAMP_REPLY (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_TIMESTAMP_REPLY = 1 << 10;
		/**
		 * The constant to represent ICMP type INFORMATION_REQUEST (different from
		 * the actual ICMP value).
		 */
		public static final int ICMP_TYPE_INFORMATION_REQUEST = 1 << 11;
		/**
		 * The constant to represent ICMP type INFORMATION_REPLY (different from the
		 * actual ICMP value).
		 */
		public static final int ICMP_TYPE_INFORMATION_REPLY = 1 << 12;
		/**
		 * The constant to represent ICMP type ADDRESS_MASK_REQUEST (different from
		 * the actual ICMP value).
		 */
		public static final int ICMP_TYPE_ADDRESS_MASK_REQUEST = 1 << 13;
		/**
		 * The constant to represent ICMP type ADDRESS_MASK_REPLY (different from
		 * the actual ICMP value).
		 */
		public static final int ICMP_TYPE_ADDRESS_MASK_REPLY = 1 << 14;

		private int _ipOptions, _tcpOptions, _tcpFlags, _icmpTypes;
		private String _action, _direction, _protocol, _from, _to, _fromPorts, _toPorts;
		private boolean _frag, _established, _setup;

		/**
		 * Constructs a new IPFilterRule given its String representation.
		 * 
		 * @param value the String representation
		 * @throws IllegalArgumentException if the value cannot be parsed
		 */
		public IPFilterRule(String value) {
			String[] data = value.split(" +");
			try {
				int index = 0;
				parseAction(data, index);
				index++;
				parseDirection(data, index);
				index++;
				parseProtocol(data, index);
				index++;
				index = parseFrom(data, index);
				index = parseTo(data, index);
				parseOptions(data, index);
			}
			catch (RuntimeException e) {
				throw new IllegalArgumentException("Invalid IPFilterRule " + value, e);
			}

			if (_to == null) {
				throw new IllegalArgumentException("Invalid IPFilterRule (too short): " + value);
			}
		}

		private void parseOptions(String[] data, int index) {
			int i = index;
			while (i < data.length) {
				String token = data[i];
				i++;
				boolean hasNext = (i < data.length);
				String nextToken = null;
				if (hasNext) {
					nextToken = data[i];
				}

				if ("frag".equalsIgnoreCase(token)) {
					_frag = true;
					continue;
				}

				if ("established".equalsIgnoreCase(token)) {
					_established = true;
					continue;
				}

				if ("setup".equalsIgnoreCase(token)) {
					_setup = true;
					continue;
				}

				if ("ipoptions".equalsIgnoreCase(token)) {
					if (!hasNext) {
						throw new IllegalArgumentException("No ipoptions value");
					}
					parseIpOptions(nextToken);
					i++;
					return;
				}

				if ("tcpoptions".equalsIgnoreCase(token)) {
					if (!hasNext) {
						throw new IllegalArgumentException("No tcpoptions value");
					}
					parseTcpOptions(nextToken);
					i++;
					return;
				}
				if ("tcpflags".equalsIgnoreCase(token)) {
					if (!hasNext) {
						throw new IllegalArgumentException("No tcpflags value");
					}
					parseTcpFlags(nextToken);
					i++;
					continue;
				}

				if ("icmptypes".equalsIgnoreCase(token)) {
					if (!hasNext) {
						throw new IllegalArgumentException("No icmptypes value");
					}
					parseIcmpTypes(nextToken);
					i++;
					continue;
				}

				throw new IllegalArgumentException("Invalid option " + token);
			}

		}

		private int parseTo(String[] data, int index) {
			int i = index;
			if (i >= data.length) {
				throw new IllegalArgumentException("No to");
			}
			String token = data[i];
			if (!"to".equalsIgnoreCase(token)) {
				throw new IllegalArgumentException("Invalid to: " + token);
			}
			i++;
			if (i >= data.length) {
				return i;
			}
			_to = data[i];
			i++;
			if (i >= data.length) {
				return i;
			}

			token = data[i];
			char c = token.charAt(0);
			if (c >= '0' && c <= '9' && _toPorts == null) {
				_toPorts = token;
				i++;
			}

			return i;
		}

		private int parseFrom(String[] data, int index) {
			int i = index;
			if (i >= data.length) {
				throw new IllegalArgumentException("No from");
			}
			String token = data[i];
			if (!"from".equalsIgnoreCase(token)) {
				throw new IllegalArgumentException("Invalid from: " + token);
			}
			i++;
			if (i >= data.length) {
				return i;
			}
			_from = data[i];
			i++;
			if (i >= data.length) {
				return i;
			}

			token = data[i];
			char c = token.charAt(0);
			if (c >= '0' && c <= '9' && _fromPorts == null) {
				_fromPorts = token;
				i++;
			}

			return i;
		}

		private void parseProtocol(String[] data, int index) {
			if (index > data.length) {
				throw new IllegalArgumentException("No protocol");
			}
			String token = data[index];
			_protocol = token;
		}

		private void parseDirection(String[] data, int index) {
			if (index > data.length) {
				throw new IllegalArgumentException("No direction");
			}
			String token = data[index];
			if (DIR_IN.equalsIgnoreCase(token)) {
				_direction = DIR_IN;
			} else if (DIR_OUT.equalsIgnoreCase(token)) {
				_direction = DIR_OUT;
			} else {
				throw new IllegalArgumentException("Invalid direction: " + token);
			}
		}

		private void parseAction(String[] data, int index) {
			if (index > data.length) {
				throw new IllegalArgumentException("No action");
			}
			String token = data[index];
			if (ACTION_PERMIT.equalsIgnoreCase(token)) {
				_action = ACTION_PERMIT;
			} else if (ACTION_DENY.equalsIgnoreCase(token)) {
				_action = ACTION_DENY;
			} else {
				throw new IllegalArgumentException("Invalid action: " + token);
			}
		}

		private void parseIpOptions(String s) {
			StringTokenizer st = new StringTokenizer(s, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				boolean not = token.startsWith("!");
				if (not) {
					token = token.substring(1);
				}
				if ("ssrr".equalsIgnoreCase(token)) {
					_ipOptions |= not ? IP_OPTION_NOT_SSRR : IP_OPTION_SSRR;
					continue;
				}
				if ("lsrr".equalsIgnoreCase(token)) {
					_ipOptions |= not ? IP_OPTION_NOT_LSRR : IP_OPTION_LSRR;
					continue;
				}
				if ("rr".equalsIgnoreCase(token)) {
					_ipOptions |= not ? IP_OPTION_NOT_RR : IP_OPTION_RR;
					continue;
				}
				if ("ts".equalsIgnoreCase(token)) {
					_ipOptions |= not ? IP_OPTION_NOT_TS : IP_OPTION_TS;
					continue;
				}
				throw new IllegalArgumentException("Invalid  ipoptions value: " + token);
			}
		}

		private void parseTcpOptions(String s) {
			StringTokenizer st = new StringTokenizer(s, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				boolean not = token.startsWith("!");
				if (not)
					token = token.substring(1);
				if ("mss".equalsIgnoreCase(token)) {
					_tcpOptions |= not ? TCP_OPTION_NOT_MSS : TCP_OPTION_MSS;
					continue;
				}
				if ("window".equalsIgnoreCase(token)) {
					_tcpOptions |= not ? TCP_OPTION_NOT_WINDOW : TCP_OPTION_WINDOW;
					continue;
				}
				if ("sack".equalsIgnoreCase(token)) {
					_tcpOptions |= not ? TCP_OPTION_NOT_SACK : TCP_OPTION_SACK;
					continue;
				}
				if ("ts".equalsIgnoreCase(token)) {
					_tcpOptions |= not ? TCP_OPTION_NOT_TS : TCP_OPTION_TS;
					continue;
				}
				if ("cc".equalsIgnoreCase(token)) {
					_tcpOptions |= not ? TCP_OPTION_NOT_CC : TCP_OPTION_CC;
					continue;
				}
				throw new IllegalArgumentException("Invalid  tcpoptions value: " + token);
			}
		}

		private void parseTcpFlags(String s) {
			StringTokenizer st = new StringTokenizer(s, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				boolean not = token.startsWith("!");
				if (not)
					token = token.substring(1);
				if ("fin".equalsIgnoreCase(token)) {
					_tcpFlags |= not ? TCP_FLAG_NOT_FIN : TCP_FLAG_FIN;
					continue;
				}
				if ("syn".equalsIgnoreCase(token)) {
					_tcpFlags |= not ? TCP_FLAG_NOT_SYN : TCP_FLAG_SYN;
					continue;
				}
				if ("rst".equalsIgnoreCase(token)) {
					_tcpFlags |= not ? TCP_FLAG_NOT_RST : TCP_FLAG_RST;
					continue;
				}
				if ("psh".equalsIgnoreCase(token)) {
					_tcpFlags |= not ? TCP_FLAG_NOT_PSH : TCP_FLAG_PSH;
					continue;
				}
				if ("ack".equalsIgnoreCase(token)) {
					_tcpFlags |= not ? TCP_FLAG_NOT_ACK : TCP_FLAG_ACK;
					continue;
				}
				if ("urg".equalsIgnoreCase(token)) {
					_tcpFlags |= not ? TCP_FLAG_NOT_URG : TCP_FLAG_URG;
					continue;
				}
				throw new IllegalArgumentException("Invalid tcpflags value: " + token);
			}
		}

		private void parseIcmpTypes(String s) {
			StringTokenizer st = new StringTokenizer(s, ",");
			while (st.hasMoreTokens()) {
				try {
					String token = st.nextToken();
					int index = token.indexOf('-');
					if (index == -1) {
						parseIcmpType(Integer.parseInt(token));
					} else {
						int start = Integer.parseInt(token.substring(0, index));
						int stop = Integer.parseInt(token.substring(index + 1));
						if (start > stop || (stop - start > 18)) {
							throw new Exception();
						}
						for (int i = start; i <= stop; i++) {
							parseIcmpType(i);
						}
					}
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Invalid  icmptypes value: " + s);
				}
			}
		}

		private void parseIcmpType(int i) {
			switch (i) {
				case 0:
					_icmpTypes |= ICMP_TYPE_ECHO_REPLY;
					break;
				case 3:
					_icmpTypes |= ICMP_TYPE_DESTINATION_UNREACHABLE;
					break;
				case 4:
					_icmpTypes |= ICMP_TYPE_SOURCE_QUENCH;
					break;
				case 5:
					_icmpTypes |= ICMP_TYPE_REDIRECT;
					break;
				case 8:
					_icmpTypes |= ICMP_TYPE_ECHO_REQUEST;
					break;
				case 9:
					_icmpTypes |= ICMP_TYPE_ROUTER_ADVERTISEMENT;
					break;
				case 10:
					_icmpTypes |= ICMP_TYPE_ROUTER_SOLICITATION;
					break;
				case 11:
					_icmpTypes |= ICMP_TYPE_TTL_EXCEEDED;
					break;
				case 12:
					_icmpTypes |= ICMP_TYPE_IP_HEADER_BAD;
					break;
				case 13:
					_icmpTypes |= ICMP_TYPE_TIMESTAMP_REQUEST;
					break;
				case 14:
					_icmpTypes |= ICMP_TYPE_TIMESTAMP_REPLY;
					break;
				case 15:
					_icmpTypes |= ICMP_TYPE_INFORMATION_REQUEST;
					break;
				case 16:
					_icmpTypes |= ICMP_TYPE_INFORMATION_REPLY;
					break;
				case 17:
					_icmpTypes |= ICMP_TYPE_ADDRESS_MASK_REQUEST;
					break;
				case 18:
					_icmpTypes |= ICMP_TYPE_ADDRESS_MASK_REPLY;
					break;
				default:
			}
		}

		/**
		 * Constructs a new IPFilterRule.
		 * 
		 * @param action The action.
		 * @param direction The direction.
		 * @param protocol The protocol.
		 * @param from The source.
		 * @param to The destination.
		 * @param frag The "frag" flag.
		 * @param ipoptions The IP options.
		 * @param tcpoptions The TCP options.
		 * @param established The "established" flag.
		 * @param setup The setup flag.
		 * @param tcpflags The TCP flags.
		 * @param icmptypes The ICMP types.
		 */
		public IPFilterRule(String action, String direction, String protocol, String from, String fromPorts, String to, String toPorts, boolean frag,
				int ipoptions, int tcpoptions, boolean established, boolean setup, int tcpflags, int icmptypes) {
			_action = action;
			_direction = direction;
			_protocol = protocol;
			_from = from;
			_fromPorts = fromPorts;
			_to = to;
			_toPorts = toPorts;
			_frag = frag;
			_ipOptions = ipoptions;
			_tcpOptions = tcpoptions;
			_established = established;
			_setup = setup;
			_tcpFlags = tcpflags;
			_icmpTypes = icmptypes;
		}

		/**
		 * Gets the action.
		 * 
		 * @return The action.
		 */
		public String getAction() {
			return _action;
		}

		/**
		 * Gets the direction.
		 * 
		 * @return The direction.
		 */
		public String getDirection() {
			return _direction;
		}

		/**
		 * Gets the protocol.
		 * 
		 * @return The protocol.
		 */
		public String getProtocol() {
			return _protocol;
		}

		/**
		 * Gets the source.
		 * 
		 * @return The source.
		 */
		public String getFrom() {
			return _from;
		}

		/**
		 * Gets the destination.
		 * 
		 * @return The destination.
		 */
		public String getTo() {
			return _to;
		}

		/**
		 * Gets the source ports.
		 * 
		 * @return The source ports.
		 */
		public String getFromPorts() {
			return _fromPorts;
		}

		/**
		 * Gets the destination ports.
		 * 
		 * @return The destination ports.
		 */
		public String getToPorts() {
			return _toPorts;
		}

		/**
		 * Gets the "frag" flag.
		 * 
		 * @return The "frag" flag.
		 */
		public boolean getFragOption() {
			return _frag;
		}

		/**
		 * Gets the IP options.
		 * 
		 * @return The IP options.
		 */
		public int getIpOptions() {
			return _ipOptions;
		}

		/**
		 * Gets the TCP options.
		 * 
		 * @return The TCP options.
		 */
		public int getTcpOptions() {
			return _tcpOptions;
		}

		/**
		 * Gets the "established" flag.
		 * 
		 * @return The "established" flag.
		 */
		public boolean getEstablishedOption() {
			return _established;
		}

		/**
		 * Gets the "setup" flag.
		 * 
		 * @return The "setup" flag.
		 */
		public boolean getSetupOption() {
			return _setup;
		}

		/**
		 * Gets the TCP flags.
		 * 
		 * @return The TCP flags.
		 */
		public int getTcpFlags() {
			return _tcpFlags;
		}

		/**
		 * Gets the ICMP types.
		 * 
		 * @return the ICMP types
		 */
		public int getIcmpTypes() {
			return _icmpTypes;
		}

		/**
		 * Indicates if a specific TCP option is set.
		 * 
		 * @param option The option to look up : TCP_OPTION_MSS, TCP_OPTION_NOT_MSS,
		 *          TCP_OPTION_WINDOW, etc...
		 * @return true if the option is set, false otherwise.
		 */
		public boolean hasTcpOptionSet(int option) {
			return ((_tcpOptions & option) == option);
		}

		/**
		 * Indicates if a specific TCP flag is set.
		 * 
		 * @param flag The flag to look up : TCP_FLAG_FIN, TCP_FLAG_NOT_FIN, etc...
		 * @return true if the flag is set, false otherwise.
		 */
		public boolean hasTcpFlagSet(int flag) {
			return ((_tcpFlags & flag) == flag);
		}

		/**
		 * Indicates if a specific IP option is set.
		 * 
		 * @param option The option to look up : IP_OPTION_SSRR, IP_OPTION_NOT_SSRR,
		 *          IP_OPTION_LSRR, etc...
		 * @return true if the option is set, false otherwise.
		 */
		public boolean hasIpOptionSet(int option) {
			return ((_ipOptions & option) == option);
		}

		/**
		 * Indicates if a specific ICMP type is set.
		 * 
		 * @param option the option to look up : ICMP_TYPE_ADDRESS_MASK_REPLY,
		 *          ICMP_TYPE_ADDRESS_MASK_REQUEST, etc...
		 * @return true if the type is set, false otherwise
		 */
		public boolean hasIcmpTypeSet(int option) {
			return ((_icmpTypes & option) == option);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder buff = new StringBuilder();
			buff.append(_action).append(' ').append(_direction).append(' ').append(_protocol).append(" from ").append(_from);
			if (_fromPorts != null)
				buff.append(' ').append(_fromPorts);
			buff.append(" to ").append(_to);
			if (_toPorts != null)
				buff.append(' ').append(_toPorts);
			if (_frag)
				buff.append(" frag");
			if (_established)
				buff.append(" established");
			if (_setup)
				buff.append(" setup");
			String tmp = getIpOptionsAsString();
			if (tmp.length() > 0)
				buff.append(" ipoptions ").append(tmp);
			tmp = getTcpOptionsAsString();
			if (tmp.length() > 0)
				buff.append(" tcpoptions ").append(tmp);
			tmp = getTcpFlagsAsString();
			if (tmp.length() > 0)
				buff.append(" tcpflags ").append(tmp);
			tmp = getIcmpTypesAsString();
			if (tmp.length() > 0)
				buff.append(" icmptypes ").append(tmp);
			return buff.toString();
		}

		/**
		 * Gets the IP options as a string.
		 * 
		 * @return The String representing the IP options.
		 */
		public String getIpOptionsAsString() {
			StringBuilder buff = new StringBuilder();
			if (hasIpOptionSet(IP_OPTION_SSRR))
				buff.append("ssrr,");
			if (hasIpOptionSet(IP_OPTION_NOT_SSRR))
				buff.append("!ssrr,");
			if (hasIpOptionSet(IP_OPTION_LSRR))
				buff.append("lsrr,");
			if (hasIpOptionSet(IP_OPTION_NOT_LSRR))
				buff.append("!lsrr,");
			if (hasIpOptionSet(IP_OPTION_RR))
				buff.append("rr,");
			if (hasIpOptionSet(IP_OPTION_NOT_RR))
				buff.append("!rr,");
			if (hasIpOptionSet(IP_OPTION_TS))
				buff.append("ts,");
			if (hasIpOptionSet(IP_OPTION_NOT_TS))
				buff.append("!ts,");
			if (buff.length() == 0)
				return "";
			return buff.substring(0, buff.length() - 1);
		}

		/**
		 * Gets the TCP options as a string.
		 * 
		 * @return The string representing the TCP options.
		 */
		public String getTcpOptionsAsString() {
			StringBuilder buff = new StringBuilder();
			if (hasTcpOptionSet(TCP_OPTION_MSS))
				buff.append("mss,");
			if (hasTcpOptionSet(TCP_OPTION_NOT_MSS))
				buff.append("!mss,");
			if (hasTcpOptionSet(TCP_OPTION_WINDOW))
				buff.append("window,");
			if (hasTcpOptionSet(TCP_OPTION_NOT_WINDOW))
				buff.append("!window,");
			if (hasTcpOptionSet(TCP_OPTION_SACK))
				buff.append("sack,");
			if (hasTcpOptionSet(TCP_OPTION_NOT_SACK))
				buff.append("!sack,");
			if (hasTcpOptionSet(TCP_OPTION_TS))
				buff.append("ts,");
			if (hasTcpOptionSet(TCP_OPTION_NOT_TS))
				buff.append("!ts,");
			if (hasTcpOptionSet(TCP_OPTION_CC))
				buff.append("cc,");
			if (hasTcpOptionSet(TCP_OPTION_NOT_CC))
				buff.append("!cc,");
			if (buff.length() == 0)
				return "";
			return buff.substring(0, buff.length() - 1);
		}

		/**
		 * Gets the TCP flags as a string.
		 * 
		 * @return The string representing the TCP flags.
		 */
		public String getTcpFlagsAsString() {
			StringBuilder buff = new StringBuilder();
			if (hasTcpFlagSet(TCP_FLAG_FIN))
				buff.append("fin,");
			if (hasTcpFlagSet(TCP_FLAG_NOT_FIN))
				buff.append("!fin,");
			if (hasTcpFlagSet(TCP_FLAG_SYN))
				buff.append("syn,");
			if (hasTcpFlagSet(TCP_FLAG_NOT_SYN))
				buff.append("!syn,");
			if (hasTcpFlagSet(TCP_FLAG_RST))
				buff.append("rst,");
			if (hasTcpFlagSet(TCP_FLAG_NOT_RST))
				buff.append("!rst,");
			if (hasTcpFlagSet(TCP_FLAG_PSH))
				buff.append("psh,");
			if (hasTcpFlagSet(TCP_FLAG_NOT_PSH))
				buff.append("!psh,");
			if (hasTcpFlagSet(TCP_FLAG_ACK))
				buff.append("ack,");
			if (hasTcpFlagSet(TCP_FLAG_NOT_ACK))
				buff.append("!ack,");
			if (hasTcpFlagSet(TCP_FLAG_URG))
				buff.append("urg,");
			if (hasTcpFlagSet(TCP_FLAG_NOT_URG))
				buff.append("!urg,");
			if (buff.length() == 0)
				return "";
			return buff.substring(0, buff.length() - 1);
		}

		/**
		 * Gets the ICMP types as a string.
		 * 
		 * @return The string representing the ICMP types.
		 */
		public String getIcmpTypesAsString() {
			StringBuilder buff = new StringBuilder();
			if (hasIcmpTypeSet(ICMP_TYPE_ECHO_REPLY))
				buff.append("0,");
			if (hasIcmpTypeSet(ICMP_TYPE_DESTINATION_UNREACHABLE))
				buff.append("3,");
			if (hasIcmpTypeSet(ICMP_TYPE_SOURCE_QUENCH))
				buff.append("4,");
			if (hasIcmpTypeSet(ICMP_TYPE_REDIRECT))
				buff.append("5,");
			if (hasIcmpTypeSet(ICMP_TYPE_ECHO_REQUEST))
				buff.append("8,");
			if (hasIcmpTypeSet(ICMP_TYPE_ROUTER_ADVERTISEMENT))
				buff.append("9,");
			if (hasIcmpTypeSet(ICMP_TYPE_ROUTER_SOLICITATION))
				buff.append("10,");
			if (hasIcmpTypeSet(ICMP_TYPE_TTL_EXCEEDED))
				buff.append("11,");
			if (hasIcmpTypeSet(ICMP_TYPE_IP_HEADER_BAD))
				buff.append("12,");
			if (hasIcmpTypeSet(ICMP_TYPE_TIMESTAMP_REQUEST))
				buff.append("13,");
			if (hasIcmpTypeSet(ICMP_TYPE_TIMESTAMP_REPLY))
				buff.append("14,");
			if (hasIcmpTypeSet(ICMP_TYPE_INFORMATION_REQUEST))
				buff.append("15,");
			if (hasIcmpTypeSet(ICMP_TYPE_INFORMATION_REPLY))
				buff.append("16,");
			if (hasIcmpTypeSet(ICMP_TYPE_ADDRESS_MASK_REQUEST))
				buff.append("17,");
			if (hasIcmpTypeSet(ICMP_TYPE_ADDRESS_MASK_REPLY))
				buff.append("18,");
			if (buff.length() == 0)
				return "";
			return buff.substring(0, buff.length() - 1);
		}
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof IPFilterRule) {
			return toIPFilterRule((IPFilterRule) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "An instance of " + IPFilterRule.class + " is expected");
		}
	}
}
