package com.nokia.as.jaxrs.jersey;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.alcatel.as.http.parser.HttpMessage.Header;
import com.alcatel.as.ioh.server.TcpServer;
//import com.nokia.as.jaxrs.jersey.parser.HttpMessage;
//import com.nokia.as.jaxrs.jersey.parser.HttpParser;
//import com.nokia.as.jaxrs.jersey.parser.HttpMessageImpl.HeaderImpl;

import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;

public class JerseyProcessorClientContextTest {

//	JerseyProcessorClientContext context;
//	com.alcatel.as.http.parser.HttpParser oldParser;
//	HttpParser newParser = new HttpParser();
//	private TcpServer serveur;
//	private ResourceConfig resourceConfig;
//
//	@Before
//	public void init() {
//		resourceConfig = new ResourceConfig(HelloResource.class);
//		oldParser = new com.alcatel.as.http.parser.HttpParser();
//		serveur = new TcpServer() {
//
//			@Override
//			public void stopListening(boolean closeAll) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void resumeListening() {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public boolean isOpen() {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Map<String, Object> getProperties() {
//				return new HashMap<String, Object>();
//			}
//
//			@Override
//			public void closeAll() {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void close() {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public <T> T attachment() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public <T> T attach(Object attachment) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public TcpServerChannel getServerChannel() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public InetSocketAddress getAddress() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		};
//		
//	}
//
//	@Test
//	@Ignore
//	public void parseHttpMessage_returnsContentTypeHeaderLowerCaseWithoutLastChar() {
//		String firstLine       = "GET /hello HTTP/1.1"                     + System.lineSeparator();
//		String contentTypeLine = "Content-Type: text/html; charset=utf-8"  + System.lineSeparator();
//		ByteBuffer buffer = ByteBuffer.wrap((firstLine + contentTypeLine   + System.lineSeparator()).getBytes());
//
//		serveur.getProperties().put(JerseyProcessor.JERSEYPROCESSOR_PARSER, oldParser);
//		context = new JerseyProcessorClientContext(new ApplicationHandler(resourceConfig), serveur);
//		
//		JerseyParsedRequestParams params = context.parseHttpMessage(buffer);
//		com.nokia.as.jaxrs.jersey.parser.HttpMessage.Header contentTypeHeader = params.getContentTypeHeader();
//
//		assertEquals("content-type", contentTypeHeader.getName());
//		assertEquals("text/html; charset=utf-", contentTypeHeader.getValue());
//	}
//	
//	@Test
//	public void newParser_returnsAllHeadersInReverseOrder() {
//		String firstLine       = "GET /hello HTTP/1.1"                     + System.lineSeparator();
//		String contentTypeLine = "Content-Type: text/html; charset=utf-8"  + System.lineSeparator();
//		String encodingLine    = "Content-Encoding: gzip"                  + System.lineSeparator();
//		ByteBuffer buffer = ByteBuffer.wrap((firstLine + contentTypeLine + encodingLine + System.lineSeparator()).getBytes());
//
//		serveur.getProperties().put(JerseyProcessor.JERSEYPROCESSOR_PARSER, newParser);
//		context = new JerseyProcessorClientContext(new ApplicationHandler(resourceConfig), serveur);
//		HttpMessage req;
//
//		Map<String, HeaderImpl> headersMap = null;
//		while ((req = newParser.parseMessage(buffer)) != null) {
//			headersMap = req.getHeaders();
//		}
//
//		Collection<HeaderImpl> headerValues = headersMap.values();
//		assertEquals(2, headerValues.size());
//
//		int headerIndex = 0;
//		for (Iterator<HeaderImpl> it = headerValues.iterator(); it.hasNext();) {
//			HeaderImpl header = it.next();
//
//			if (headerIndex == 0) {
//				assertEquals("content-encoding", header.getName());
//				assertEquals("gzi", header.getValue());
//
//			} else if (headerIndex == 1) {
//				assertEquals("content-type", header.getName());
//				assertEquals("text/html; charset=utf-", header.getValue());
//			}
//			headerIndex++;
//		}
//	}
}
