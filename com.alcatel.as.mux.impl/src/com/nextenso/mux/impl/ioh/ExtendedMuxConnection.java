// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import java.util.*;

import com.nextenso.mux.*;
import com.nextenso.mux.util.*;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.impl.MuxConnectionImpl;

import org.osgi.service.event.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.alcatel.as.service.metering2.util.MeteringRegistry.MetersTracker;
import com.alcatel.as.service.shutdown.ShutdownService;

public abstract class ExtendedMuxConnection extends AbstractMuxConnection {

    public static final int FLAG_MUX_METER_GET = 1;
    public static final int FLAG_MUX_METER_VALUE = 2;
    public static final int FLAG_MUX_EXIT = 3;
    public static final int FLAG_MUX_KILL = 4;
    
    // default MuxConnection methods
    
    public void close(Enum<?> reason, String info, Throwable err) {
      MuxConnectionImpl.logClosingMux(this, reason, info, err, _logger4j);
      close();
    }

    public void shutdown(Enum<?> reason, String info, Throwable err) {
      MuxConnectionImpl.logClosingMux(this, reason, info, err, _logger4j);
      shutdown();
    }
    
    // the following does not exist in MuxConnection but may be useful in ioh
    
    protected int _muxVersion;
    
    public ExtendedMuxConnection (Logger logger){ super (logger);}
    public ExtendedMuxConnection (MuxHandler mh, ConnectionListener listener, Logger logger){ super (mh, listener, logger);}
    
    public void setMuxVersion (int version){ _muxVersion = Math.min (version, MuxParser.MUX_VERSION);}
    public int getMuxVersion (){ return _muxVersion;}

    public void sendMuxPingAck (){}

    public boolean sendInternalMuxData (MuxHeader h, boolean copy, ByteBuffer... buff){ return false;}


    /******************* some static methods to handle some internal messages *************/
    
    public static void handleMuxExit (MuxHeader h, ByteBuffer buffer, EventAdmin eventAdmin){
	byte[] bytes = new byte[buffer.remaining ()];
	buffer.get (bytes);
	String txt = null;
	try { txt = new String (bytes, "ascii");}catch(Exception e){} // cannot happen
	int index = txt.indexOf (' ');
	String topic = txt.substring (0, index);
	String instance = txt.substring (index+1);
	long delay = h.getSessionId ();
	Map<String, String> props = new HashMap<> ();
	props.put (ShutdownService.SHUTDOWN_TARGET_INSTANCE_ID, instance);
	if (delay > 0L) props.put (ShutdownService.SHUTDOWN_DELAY, String.valueOf (delay));
	eventAdmin.postEvent (new Event (topic, props));
    }
    public static Object handleMuxMeterGet (final ExtendedMuxConnection connection, MuxHeader h, ByteBuffer buffer, MeteringRegistry registry){
	final int id = h.getChannelId ();
	byte[] bytes = new byte[buffer.remaining ()];
	buffer.get (bytes);
	String txt = null;
	try { txt = new String (bytes, "ascii");}catch(Exception e){} // cannot happen
	int index = txt.indexOf (' ');
	String monName = txt.substring (0, index);
	String meterName = txt.substring (index+1);
	final long period = h.getSessionId ();
	MetersTracker tracker = new MetersTracker (Meters.toPattern (meterName, null), true, false, false){
		MonitoringJob _job;
		@Override
		public void addedMeter (Monitorable monitorable, Meter meter){
		    MeterListener listener = new MeterListener (){
			    public Object updated(Meter meter, Object context){
				MuxHeaderV0 h = new MuxHeaderV0 ();
				h.set (meter.getValue (), id, FLAG_MUX_METER_VALUE);
				connection.sendInternalMuxData (h, false, null);
				return context;
			    }
			};
		    if (period > 0)
			_job = meter.startScheduledJob(listener, null, null, period, 0);
		    else
			_job = meter.startJob (listener, null, null);
		}
		@Override
		public void removedMeter (Monitorable monitorable, Meter meter){
		    stopJob ();
		}
		@Override
		public void removedMonitorable (Monitorable monitorable, List<Meter> meters){
		    stopJob ();
		}
		@Override
		public void destroyed (Object ctx){
		    super.destroyed (ctx);
		    stopJob ();
		}
		private void stopJob (){
		    if (_job != null){
			_job.stop ();
			_job = null;
		    }
		}
	    };
	return registry.trackMonitorable (monName, tracker, null);
    }
}
