package com.nextenso.diameter.agent.dictionary;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder.GenerationException;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinition;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinitionBuilder;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDictionary;
import com.nextenso.proxylet.diameter.dictionary.FlagPolicy;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterAVP;
import com.nextenso.proxylet.diameter.dictionary.annotations.DiameterCommand;
import com.nextenso.proxylet.diameter.util.AddressFormat;
import com.nextenso.proxylet.diameter.util.AddressFormat.DiameterAddress;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.Float32Format;
import com.nextenso.proxylet.diameter.util.Float64Format;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat.IPFilterRule;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.Integer64Format;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.URIFormat;
import com.nextenso.proxylet.diameter.util.URIFormat.URI;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

public class DiameterCommandBuilderTest {

	@DiameterCommand(name = "My-Command-Request")
	static class MyDiameterBean {
		
		@DiameterAVP(name = "Origin-State-Id")
		private long stateId;
		
		@DiameterAVP(name = "User-Name")
		private String userName;
		
		@DiameterAVP(name = "Product-Name")
		private String productName;
		
		@DiameterAVP(name = "Proxy-Host", group="Proxy-Info")
		private String proxyHost;
		
		@DiameterAVP(name = "Proxy-State", group="Proxy-Info")
		private byte[] proxyState;

		public long getStateId() {
			return stateId;
		}

		public void setStateId(long stateId) {
			this.stateId = stateId;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getProductName() {
			return productName;
		}

		public void setProductName(String productName) {
			this.productName = productName;
		}

		public String getProxyHost() {
			return proxyHost;
		}

		public void setProxyHost(String proxyHost) {
			this.proxyHost = proxyHost;
		}

		public byte[] getProxyState() {
			return proxyState;
		}

		public void setProxyState(byte[] proxyState) {
			this.proxyState = proxyState;
		}
	}
	
	@DiameterCommand(name = "My-Command-Answer")
	static class MyDiameterBeanResponse {
		
		@DiameterAVP(name = "Accounting-Record-Number")
		private long recordNumber;
		
		@DiameterAVP(name = "Product-Name")
		private String productName;

		public long getRecordNumber() {
			return recordNumber;
		}

		public void setRecordNumber(long recordNumber) {
			this.recordNumber = recordNumber;
		}

		public String getProductName() {
			return productName;
		}

		public void setProductName(String productName) {
			this.productName = productName;
		}
	}
	
	@Test
	public void testSimple() throws Exception {
		MyDiameterBean bean = new MyDiameterBean();
		bean.setStateId(777);
		bean.setProductName("foo");
		bean.setUserName("bar");
		bean.setProxyHost("aaa://foobar");
		bean.setProxyState("hello".getBytes());
		
		DiameterClient dc = Mockito.mock(DiameterClient.class);
		Mockito.when(dc.newRequest(Mockito.anyInt(), Mockito.anyBoolean()))
			.then(call -> {
				Integer code = call.getArgumentAt(0, Integer.class);
				Boolean proxiable = call.getArgumentAt(1, Boolean.class);
				
				return new DiameterRequestFacade(null, 
						0,
						0,
						code, 
						(proxiable) ? DiameterRequestFacade.RP_FLAGS : DiameterRequestFacade.REQUEST_FLAG,
						0,
						0);
			});
		
		
		DiameterCommandDefinitionBuilder defBuilder = new DiameterCommandDefinitionBuilder();
		
		DiameterCommandDefinition def = defBuilder
				.name("My-Command-Request","My-Command-Answer")
			.abbreviation("MCR", "MCA")
			.applicationId(0)
			.code(555)
			.proxiableBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
			.requestBitPolicy(FlagPolicy.REQUIRED, FlagPolicy.FORBIDDEN)
			.errorBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
			.requestAVP(DiameterBaseConstants.AVP_ORIGIN_STATE_ID, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_USER_NAME, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_PRODUCT_NAME, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_PROXY_INFO, 1, -1)
			.answerAVP(DiameterBaseConstants.AVP_ACCOUNTING_RECORD_NUMBER, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_PRODUCT_NAME, 1, 1)
			.build();
		
		DiameterCommandBuilder builder = new DiameterCommandBuilder( 
				new DiameterCommandDictionary(Arrays.asList(def))
			);
		
		DiameterClientRequest req = builder.buildRequest(dc, bean);
		Assert.assertNotNull(req);
		System.out.println(req);
		
		MyDiameterBeanResponse respBean = new MyDiameterBeanResponse();
		respBean.setProductName("test");
		respBean.setRecordNumber(1337L);
		
		
		DiameterResponse myResp = builder.buildResponse(req, respBean);
		Assert.assertNotNull(myResp);
		System.out.println(myResp);
	}
	
	
	@DiameterCommand(name = "My-Cool-Command")
	static class AllTypeBean {
		
		@DiameterAVP(name = "An-Address")
		public DiameterAddress address;
		
		@DiameterAVP(name = "An-Enumerated")
		public int enumInt;
		
		@DiameterAVP(name = "A-Float32")
		public float aFloat;
		
		@DiameterAVP(name = "A-Float64")
		public double aDouble;
		
		@DiameterAVP(name = "A-Grouped")
		public List<com.nextenso.proxylet.diameter.DiameterAVP> grouped;
		
		@DiameterAVP(name = "An-Identity")
		public String identity;
		
		@DiameterAVP(name = "An-Integer32")
		public int int32;
		
		@DiameterAVP(name = "An-Integer64")
		public long int64;
		
		@DiameterAVP(name = "An-IPFilterRule")
		public IPFilterRule rule;
		
		@DiameterAVP(name = "An-OctetString")
		public byte[] bytes;
		
		@DiameterAVP(name = "A-Time")
		public long time;
		
		@DiameterAVP(name = "An-Unsigned32")
		public long unsigned32;
		
		@DiameterAVP(name = "An-Unsigned64")
		public BigInteger unsigned64;
		
		@DiameterAVP(name = "An-URI")
		public URI uri;
		
		@DiameterAVP(name = "A-String")
		public String string;
		
	}
	
	@DiameterCommand(name = "My-Cool-Answer")
	static class AllTypeBeanResponse {
		
		@DiameterAVP(name = "An-Address")
		public DiameterAddress address;
		
		@DiameterAVP(name = "An-Enumerated")
		public int enumInt;
		
		@DiameterAVP(name = "A-Float32")
		public float aFloat;
		
		@DiameterAVP(name = "A-Float64")
		public double aDouble;
		
		@DiameterAVP(name = "A-Grouped")
		public List<com.nextenso.proxylet.diameter.DiameterAVP> grouped;
		
		@DiameterAVP(name = "An-Identity")
		public String identity;
		
		@DiameterAVP(name = "An-Integer32")
		public int int32;
		
		@DiameterAVP(name = "An-Integer64")
		public long int64;
		
		@DiameterAVP(name = "An-IPFilterRule")
		public IPFilterRule rule;
		
		@DiameterAVP(name = "An-OctetString")
		public byte[] bytes;
		
		@DiameterAVP(name = "A-Time")
		public long time;
		
		@DiameterAVP(name = "An-Unsigned32")
		public long unsigned32;
		
		@DiameterAVP(name = "An-Unsigned64")
		public BigInteger unsigned64;
		
		@DiameterAVP(name = "An-URI")
		public URI uri;
		
		@DiameterAVP(name = "A-String")
		public String string;
		
	}
	
	@Test
	public void testAllTypes() throws Exception {
		DiameterClient dc = Mockito.mock(DiameterClient.class);
		Mockito.when(dc.newRequest(Mockito.anyInt(), Mockito.anyBoolean()))
			.then(call -> {
				Integer code = call.getArgumentAt(0, Integer.class);
				Boolean proxiable = call.getArgumentAt(1, Boolean.class);
				
				return new DiameterRequestFacade(null, 
						0,
						0,
						code, 
						(proxiable) ? DiameterRequestFacade.RP_FLAGS : DiameterRequestFacade.REQUEST_FLAG,
						0,
						0);
			});
		
		DiameterCommandDefinitionBuilder defBuilder = new DiameterCommandDefinitionBuilder();

		DiameterAVPDefinition addressAvp = new DiameterAVPDefinition("An-Address", 20000L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						AddressFormat.INSTANCE,
						false),
		enumAvp = new DiameterAVPDefinition("An-Enumerated", 20001L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						EnumeratedFormat.INSTANCE,
						false),
		float32Avp = new DiameterAVPDefinition("A-Float32", 20002L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Float32Format.INSTANCE,
						false),
		float64Avp = new DiameterAVPDefinition("A-Float64", 20003L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Float64Format.INSTANCE,
						false),
		groupedAvp = new DiameterAVPDefinition("A-Grouped", 20004L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						GroupedFormat.INSTANCE,
						false),
		identityAvp = new DiameterAVPDefinition("An-Identity", 20005L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						IdentityFormat.INSTANCE,
						false),
		integer32Avp = new DiameterAVPDefinition("An-Integer32", 20006L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Integer32Format.INSTANCE,
						false),
		integer64Avp = new DiameterAVPDefinition("An-Integer64", 20007L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Integer64Format.INSTANCE,
						false),
		ipFilterRuleAvp = new DiameterAVPDefinition("An-IPFilterRule", 20008L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						IPFilterRuleFormat.INSTANCE,
						false),
		octetStringAvp = new DiameterAVPDefinition("An-OctetString", 20009L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						OctetStringFormat.INSTANCE,
						false),
		timeAvp = new DiameterAVPDefinition("A-Time", 20010L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						TimeFormat.INSTANCE,
						false),
		unsigned32Avp = new DiameterAVPDefinition("An-Unsigned32", 20011L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Unsigned32Format.INSTANCE,
						false),
		unsigned64Avp = new DiameterAVPDefinition("An-Unsigned64", 20012L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Unsigned64Format.INSTANCE,
						false),
		uriAvp = new DiameterAVPDefinition("An-URI", 20013L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						URIFormat.INSTANCE,
						false),
		stringAvp = new DiameterAVPDefinition("A-String", 20014L, 10L, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						UTF8StringFormat.INSTANCE,
						false);
		
		DiameterAVPDictionary avpDico = new DiameterAVPDictionary(
				Arrays.asList(addressAvp, enumAvp, float32Avp, float64Avp,
						groupedAvp, identityAvp, integer32Avp, integer64Avp,
						ipFilterRuleAvp, octetStringAvp, timeAvp, unsigned32Avp,
						unsigned64Avp, uriAvp, stringAvp));
		
		DiameterCommandDefinition cmd = defBuilder.name("My-Cool-Command", "My-Cool-Answer")
			.applicationId(0L)
			.code(679)
			.errorBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
			.proxiableBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
			.requestBitPolicy(FlagPolicy.REQUIRED, FlagPolicy.FORBIDDEN)
			.abbreviation("MCC", "MCA")
			.requestAVP(addressAvp, 1, -1)
			.requestAVP(enumAvp, 1, -1)
			.requestAVP(float32Avp, 1, -1)
			.requestAVP(float64Avp, 1, -1)
			.requestAVP(groupedAvp, 1, -1)
			.requestAVP(identityAvp, 1, -1)
			.requestAVP(integer32Avp, 1, -1)
			.requestAVP(integer64Avp, 1, -1)
			.requestAVP(ipFilterRuleAvp, 1, -1)
			.requestAVP(octetStringAvp, 1, -1)
			.requestAVP(timeAvp, 1, -1)
			.requestAVP(unsigned32Avp, 1, -1)
			.requestAVP(unsigned64Avp, 1, -1)
			.requestAVP(uriAvp, 1, -1)
			.requestAVP(stringAvp, 1, -1)
			.answerAVP(addressAvp, 1, -1)
			.answerAVP(enumAvp, 1, -1)
			.answerAVP(float32Avp, 1, -1)
			.answerAVP(float64Avp, 1, -1)
			.answerAVP(groupedAvp, 1, -1)
			.answerAVP(identityAvp, 1, -1)
			.answerAVP(integer32Avp, 1, -1)
			.answerAVP(integer64Avp, 1, -1)
			.answerAVP(ipFilterRuleAvp, 1, -1)
			.answerAVP(octetStringAvp, 1, -1)
			.answerAVP(timeAvp, 1, -1)
			.answerAVP(unsigned32Avp, 1, -1)
			.answerAVP(unsigned64Avp, 1, -1)
			.answerAVP(uriAvp, 1, -1)
			.answerAVP(stringAvp, 1, -1)
			.build();
		
		DiameterCommandBuilder builder = new DiameterCommandBuilder(
				new DiameterCommandDictionary(Arrays.asList(cmd)),
				avpDico
				);
		
		com.nextenso.proxylet.diameter.DiameterAVP bonusAvp1 = new com.nextenso.proxylet.diameter.DiameterAVP(stringAvp);
		bonusAvp1.setValue(UTF8StringFormat.toUtf8String("hello"), false);
		
		
		com.nextenso.proxylet.diameter.DiameterAVP bonusAvp2 = new com.nextenso.proxylet.diameter.DiameterAVP(stringAvp);
		bonusAvp2.setValue(UTF8StringFormat.toUtf8String("bye"), false);
		
		
		com.nextenso.proxylet.diameter.DiameterAVP bonusAvp3 = new com.nextenso.proxylet.diameter.DiameterAVP(integer32Avp);
		bonusAvp3.setValue(Integer32Format.toInteger32(1337), false);
		
		
		AllTypeBean bean = new AllTypeBean();
		bean.address = new DiameterAddress(AddressFormat.APPLETALK, "zobi".getBytes());
		bean.enumInt = 42;
		bean.aFloat = 3.14F;
		bean.aDouble = 1.337D;
		bean.grouped = Arrays.asList(bonusAvp1, bonusAvp2, bonusAvp3);
		bean.identity = "aaa://hi.lol";
		bean.int32 = -6464;
		bean.int64 = -646464646464L;
		bean.rule = null; //Don't know how to write an IPFilterRule
		bean.bytes = "foobar".getBytes();
		bean.time = Instant.now().toEpochMilli();
		bean.unsigned32 = 32L;
		bean.unsigned64 = new BigInteger("9999999999999999999999");
		bean.uri = new URI(false, "127.0.0.1", 3868, "sctp", "diameter");
		bean.string = "what";
		
		DiameterClientRequest  req = builder.buildRequest(dc, bean);
		System.out.println(req);
		
		AllTypeBeanResponse respBean = new AllTypeBeanResponse();
		respBean.address = new DiameterAddress(AddressFormat.APPLETALK, "zobi".getBytes());
		respBean.enumInt = 42;
		respBean.aFloat = 3.14F;
		respBean.aDouble = 1.337D;
		respBean.grouped = Arrays.asList(bonusAvp1, bonusAvp2, bonusAvp3);
		respBean.identity = "aaa://hi.lol";
		respBean.int32 = -6464;
		respBean.int64 = -646464646464L;
		respBean.rule = null; //Don't know how to write an IPFilterRule
		respBean.bytes = "foobar".getBytes();
		respBean.time = Instant.now().toEpochMilli();
		respBean.unsigned32 = 32L;
		respBean.unsigned64 = new BigInteger("9999999999999999999999");
		respBean.uri = new URI(false, "127.0.0.1", 3868, "sctp", "diameter");
		respBean.string = "what";
		
		DiameterResponse resp = builder.buildResponse(req, respBean);
		Assert.assertNotNull(resp);
		System.out.println(resp);
	}
	
	@DiameterCommand(name = "My-Great-Command")
	static class MultipleGroupBean {
		@DiameterAVP(name = "Session-Id")
		public String sessionId;
		
		@DiameterAVP(name = "A-Float32", group = "A-Grouped")
		public float aFloat;
		
		@DiameterAVP(name = "A-Float64", group = "A-Grouped")
		public double aDouble;
		
		@DiameterAVP(name = "A-Float32", group = "Another-Grouped")
		public float anotherFloat;
		
		@DiameterAVP(name = "A-Float64", group = "Another-Grouped")
		public double anotherDouble;
	}
	
	@Test
	public void testMultipleGrouped() throws GenerationException {
		DiameterClient dc = Mockito.mock(DiameterClient.class);
		Mockito.when(dc.newRequest(Mockito.anyInt(), Mockito.anyBoolean()))
			.then(call -> {
				Integer code = call.getArgumentAt(0, Integer.class);
				Boolean proxiable = call.getArgumentAt(1, Boolean.class);
				
				return new DiameterRequestFacade(null, 
						0,
						0,
						code, 
						(proxiable) ? DiameterRequestFacade.RP_FLAGS : DiameterRequestFacade.REQUEST_FLAG,
						0,
						0);
			});
		
		DiameterCommandDefinitionBuilder defBuilder = new DiameterCommandDefinitionBuilder();

		DiameterAVPDefinition enumAvp = new DiameterAVPDefinition("An-Enumerated", 20001L, 10L, 
				DiameterAVPDefinition.OPTIONAL_FLAG, 
				DiameterAVPDefinition.OPTIONAL_FLAG, 
				DiameterAVPDefinition.OPTIONAL_FLAG, 
				false, 
				EnumeratedFormat.INSTANCE,
				false),
		float32Avp = new DiameterAVPDefinition("A-Float32", 20002L, 10L, 
						DiameterAVPDefinition.REQUIRED_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Float32Format.INSTANCE,
						true),
		float64Avp = new DiameterAVPDefinition("A-Float64", 20003L, 10L, 
						DiameterAVPDefinition.REQUIRED_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						Float64Format.INSTANCE,
						true),
		groupedAvp = new DiameterAVPDefinition("A-Grouped", 20004L, 10L, 
						DiameterAVPDefinition.REQUIRED_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						GroupedFormat.INSTANCE,
						true),
		anotherGroupedAvp = new DiameterAVPDefinition("Another-Grouped", 20005L, 10L, 
						DiameterAVPDefinition.REQUIRED_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						GroupedFormat.INSTANCE,
						true);
		

		DiameterCommandDefinition cmd = defBuilder.name("My-Great-Command", "My-Great-Answer")
				.applicationId(0L)
				.code(679)
				.errorBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
				.proxiableBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
				.requestBitPolicy(FlagPolicy.REQUIRED, FlagPolicy.FORBIDDEN)
				.abbreviation("MCC", "MCA")
				.requestAVP(DiameterBaseConstants.AVP_SESSION_ID, 1, -1)
				.requestAVP(groupedAvp, 1, -1)
				.requestAVP(anotherGroupedAvp, 1, -1)
				.answerAVP(DiameterBaseConstants.AVP_SESSION_ID, 1, -1)
				.answerAVP(groupedAvp, 1, -1)
				.answerAVP(anotherGroupedAvp, 1, -1)
				.build();
		
		
		DiameterCommandDictionary cmdDico = new DiameterCommandDictionary(
				Arrays.asList(cmd));
		
		DiameterCommandBuilder builder = new DiameterCommandBuilder(cmdDico);
		
		MultipleGroupBean bean = new MultipleGroupBean();
		bean.sessionId = "hi";
		bean.aFloat = 2.0f;
		bean.aDouble = -1.0d;
		bean.anotherFloat  = 4.0f;
		bean.anotherDouble = 1337.420d;
		
		DiameterRequest req = builder.buildRequest(dc, bean);
		Assert.assertNotNull(req);
		Assert.assertEquals(req.getDiameterAVPsSize(), 3);
		List<com.nextenso.proxylet.diameter.DiameterAVP> avpGroup =
				GroupedFormat.getGroupedAVPs(req.getDiameterAVP(1).getBytes(), true);
		Assert.assertNotNull(avpGroup);
		System.out.println(req);
	}
}
