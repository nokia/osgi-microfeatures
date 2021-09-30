package com.alcatel.as.diameter.lb.impl.test;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.impl.*;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

public class DiameterTestApplication {
    
    public static final long APP_ID = 123;

    public static final int COMMAND_GENERIC = 1;
    public static final int COMMAND_IGNORE_REQ = 10;
    public static final int COMMAND_PROCESS_REQ = 11;
    public static final int COMMAND_IGNORE_CER = 20;
    public static final int COMMAND_PROCESS_CER = 21;
    public static final int COMMAND_IGNORE_DWR = 50;
    public static final int COMMAND_PROCESS_DWR = 51;
    public static final int COMMAND_IGNORE_DPR = 60;
    public static final int COMMAND_PROCESS_DPR = 61;
    public static final int COMMAND_CLOSE_ON_DPR = 62;
        
    public static final int COMMAND_SEND_REQ_1 = 100;
    public static final int COMMAND_SEND_REQ_2 = 101;

    public static final int COMMAND_SEND_DPR = 201;
    
    public static final int AVP_IGNORE = 1;
    public static final int AVP_ECHO = 2;
    public static final int AVP_SLEEP = 3;
    public static final int AVP_FILLBACK = 4;
    public static final int AVP_TCP_BUFFER = 5;
    public static final int AVP_TCP_BUFFER_BACK = 6;
    public static final int AVP_SEND_DPR = 7;
    public static final int AVP_LOAD_FACTOR = 8;
    
    public static boolean process (DiameterMessage msg){
	return msg.getAvp (AVP_IGNORE, (int) APP_ID) == null;
    }
}
