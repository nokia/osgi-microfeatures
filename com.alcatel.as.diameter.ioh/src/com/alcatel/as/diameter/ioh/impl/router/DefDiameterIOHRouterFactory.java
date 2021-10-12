// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh.impl.router;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.service.metering2.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

@Component(service={DiameterIOHRouterFactory.class}, property={"router.id=def"}, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DefDiameterIOHRouterFactory extends DiameterIOHRouterFactory {

    public static Logger LOGGER = Logger.getLogger ("as.ioh.diameter.router.def");
    public static final int DIAMETER_AVP_SESSION_PRIORITY_CODE = 650;
    public static final int DIAMETER_3GPP_VENDOR_ID = 10415;

    public final static String CONF_ROUTE_BY_E2E = "diameter.ioh.route.by-e2e";
    public final static String CONF_ROUTE_BY_APP = "diameter.ioh.route.by-app";
    public final static String CONF_ROUTE_BY_AVP = "diameter.ioh.route.by-avp";
    
    @BooleanProperty(title="Respond 3004",
		     defval=true,
		     required=true,
		     dynamic=true,
		     section="Server Overload",
		     help="Indicates if a request, when discarded, should be responded to with 3004 (instead of being silently dropped).")
    public final static String CONF_OVERLOAD_3004 = "diameter.ioh.overload.3004";
    
    @BooleanProperty(title="Respond 3004 Error-Message",
		     defval=false,
		     required=false,
		     dynamic=false,
		     section="Server Overload",
		     help="Indicates if a human-readable Error-Message should be included in 3004 responses locally initiated. The message may disclose internal information and so this property should be mainly used for tests.")
    public final static String CONF_OVERLOAD_3004_ERROR_MSG_LOCAL = "diameter.ioh.overload.3004.errmsg.local";
    
    @FileDataProperty(title="Message Weights",
		      fileData="diameterMsgWeights.txt",
		      required=true,
		      dynamic=false,
		      section="Server Overload",
		      help="Indicates the messages weights.")
    public final static String CONF_OVERLOAD_SERVER_MESSAGE_WEIGHTS = "diameter.ioh.overload.msg.weights";

    @StringProperty(title="Overload meter",
		    defval="cpu.system.load.average",
		    required=true,
		    section="Server Overload",
		    help="The Meter providing the overload level in the format : monitorable/meter. By default the monitorable is the system monitorable.")
    public final static String CONF_OVERLOAD_SERVER_METER = "diameter.ioh.overload.meter";

    public final static String CONF_UNDELIVER_RESPOND = "diameter.ioh.undeliver.respond";
    public final static String CONF_UNDELIVER_RESPOND_RESULT = "diameter.ioh.undeliver.respond.result";
    public final static String CONF_UNDELIVER_RESPOND_ERRMSG = "diameter.ioh.undeliver.respond.errmsg";

    private MeteringRegistry _reg;
    private Meter _overloadMeter;

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=system)")
    public void setSystemConfig(Dictionary<String, String> system){
	super.setSystemConfig (system);
    }
    public void unsetSystemConfig(Dictionary<String, String> system){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]			
    }

    @Reference
    public void setMeteringRegistry (MeteringRegistry reg){
	_reg = reg;
    }

    
    @Reference
    public void setExecutors(PlatformExecutors executors){
	super.setExecutors (executors);
    }
    @Reference
    public void setMetering(MeteringService metering){
	super.setMetering (metering);
    }
    @Reference(target="(strict=false)")
    public void setTimerService(TimerService timerService){
	super.setTimerService (timerService);
    }
    @Modified
    public void updated(Map<String, String> conf) {
	setConf (conf);
    }
    @Activate
    public void activate(Map<String, String> conf) {
	setConf (conf);
	parseMessageWeights (conf);

	String meterName = conf.get (CONF_OVERLOAD_SERVER_METER);
	if (meterName != null && meterName.length () > 0){ // else no overload watch
	    String monName = MeteringConstants.SYSTEM;
	    int index = meterName.indexOf ('/');
	    if (index != -1){
		monName = meterName.substring (0, index);
		meterName = meterName.substring (index+1);
	    }
	    final String monNameF = monName;
	    final String meterNameF = meterName;
	
	    MeteringRegistry.MetersTracker tracker = new MeteringRegistry.MetersTracker ("*", false, false, false){
		    public void addedMeter (Monitorable monitorable, Meter meter){
			if (meter.getName ().equals (meterNameF)){
			    LOGGER.warn ("Tracking overload Meter : "+monNameF+"  / "+meterNameF);
			    _overloadMeter = meter;
			}
		    }
		};
	    _reg.trackMonitorable (monName, tracker, null);
	}
    }
    
    @Deactivate
    public void stop() {
    }

    @Override
    public String toString (){
	return "DefDiameterIOHRouterFactory";
    }

    public DiameterIOHRouter newDiameterIOHRouter (){
	return new DefDiameterIOHRouter (this, LOGGER);
    }

    public long getOverload (){
	if (_minOverload == 100L) return 0L;
	return _overloadMeter != null ? _overloadMeter.getValue () : 0L;
    }

    private Map<Long, Weight> _weights = new HashMap<> ();
    private Weight _defWeight;
    private long _minOverload = 100L;
    
    private void parseMessageWeights (Map<String, String> conf) {
	_weights.put (0L, new Weight (".default", 0, 0, (int)_minOverload, null));
	for (String line : ConfigHelper.getLines (conf.get (CONF_OVERLOAD_SERVER_MESSAGE_WEIGHTS), "weight")){
	    for (Weight weight : Weight.parse (line)){
		_weights.put (weight._key, weight);
		_minOverload = Math.min (_minOverload, weight._value);
		if (weight._valueWithSessionPriority != null){
		    _minOverload = Math.min (_minOverload, weight._valueWithSessionPriority[0]);
		    _minOverload = Math.min (_minOverload, weight._valueWithSessionPriority[1]);
		    _minOverload = Math.min (_minOverload, weight._valueWithSessionPriority[2]);
		    _minOverload = Math.min (_minOverload, weight._valueWithSessionPriority[3]);
		    _minOverload = Math.min (_minOverload, weight._valueWithSessionPriority[4]);
		}
		LOGGER.warn ("Defined : "+weight);
	    }
	}
	LOGGER.warn ("Min overload : "+_minOverload);
	if (_weights.size () == 1){
	    _defWeight = _weights.get (0L);
	    LOGGER.warn ("Default Weight : "+_defWeight);
	}
    }

    public int getMessageWeight (DiameterMessage msg){
	if (msg.isRequest () == false) return 100;
	if (_defWeight != null) return _defWeight.getMessageWeight (msg);

	long app = msg.getApplicationID () << 32;
	int cmd = msg.getCommandCode ();

	long key = app | (((long)cmd) & 0xFFFFFFFFL);

	Weight weight = _weights.get (key);
	if (weight == null){
	    weight = _weights.get (app);
	    if (weight == null)
		weight = _weights.get (0L);
	}
	return weight.getMessageWeight (msg);
    }
    // used for agent overload : need to return from 0 (incl.) to 9
    public int getPriority (DiameterMessage msg){
	if (msg.isRequest () == false) return DiameterIOHRouter.PRIORITY_RESPONSE;
	if (_minOverload == 100L){
	    // the usual def behavior
	    return msg.isRetransmitted () ? DiameterIOHRouter.PRIORITY_INITIAL_RETRANSMISSION : DiameterIOHRouter.PRIORITY_INITIAL;
	}
	int weight = getMessageWeight (msg);
	return (weight - 1) / 10; // from 0 to 9
    }

    private static class Weight {
	private long _key;
	private String _name, _keyS, _toString;
	private int _value;
	private int[] _valueWithSessionPriority;
	private boolean _useSessionPriority;
	private Weight (String name, int app, int command, int value, int[] valueWithSessionPriority){
	    _key = ((long)app) << 32;
	    _key |= (long) command & 0xFFFFFFFFL;
	    _keyS = app+"/"+command;
	    _name = name != null ? name : _keyS;
	    _value = value;
	    _valueWithSessionPriority = valueWithSessionPriority;
	    _useSessionPriority = _valueWithSessionPriority != null;
	    _toString = "Weight[name="+_name+" app="+app+" cmd="+command+" value="+value+"]";
	}
	public String toString (){
	    return _toString;
	}
	public int getMessageWeight (DiameterMessage msg){
	    if (_useSessionPriority){
		int sessionPriority = msg.getIntAvp (DIAMETER_AVP_SESSION_PRIORITY_CODE, DIAMETER_3GPP_VENDOR_ID, -1);
		switch (sessionPriority){ // we check the value via the cases
		case 0:
		case 1:
		case 2:
		case 3:
		case 4: return _valueWithSessionPriority[sessionPriority];
		}
	    }
	    return _value;
	}
	private static List<Weight> parse (String line){
	    // weight -name def -app 0 -value 1
	    // weight -name p1 -app 123 -cmd 1 -cmd 2 -value 0
	    List<Weight> policies = new ArrayList<> ();
	    int app = 0;
	    String appS = ConfigHelper.getParam (line, true, "-app", "-application");
	    List<String> codeL = ConfigHelper.getParams (line, false, "-cmd", "-command");
	    if (codeL.isEmpty ()) codeL.add ("0"); // all commands by default
	    if (appS.startsWith ("0x"))
		app= Integer.parseInt (appS.substring (2), 16);
	    else
		app= Integer.parseInt (appS);
	    for (String codeS : codeL){
		int code = 0;
		if (codeS.startsWith ("0x"))
		    code= Integer.parseInt (codeS.substring (2), 16);
		else
		    code= Integer.parseInt (codeS);
		int value = Integer.parseInt (ConfigHelper.getParam (line, true, "-v", "-value"));
		int p0 = Integer.parseInt (ConfigHelper.getParam (line, "-1", "-sp0", "-SessionPriority0"));
		int p1 = Integer.parseInt (ConfigHelper.getParam (line, "-1", "-sp1", "-SessionPriority1"));
		int p2 = Integer.parseInt (ConfigHelper.getParam (line, "-1", "-sp2", "-SessionPriority2"));
		int p3 = Integer.parseInt (ConfigHelper.getParam (line, "-1", "-sp3", "-SessionPriority3"));
		int p4 = Integer.parseInt (ConfigHelper.getParam (line, "-1", "-sp4", "-SessionPriority4"));
		int[] valueWithSessionPriority = null;
		if (p0 != -1 && p1 != -1 && p2 != -1 && p3 != -1 && p4 != -1)
		    valueWithSessionPriority = new int[]{p0, p1, p2, p3, p4};
		policies.add (new Weight (ConfigHelper.getParam (line, null, "-name"), app, code, value, valueWithSessionPriority));

	    }
	    return policies;
	}
    }
    }
