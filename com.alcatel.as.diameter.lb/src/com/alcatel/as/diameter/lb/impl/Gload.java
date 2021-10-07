package com.alcatel.as.diameter.lb.impl;

import java.util.*;
import java.nio.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.client.*;
import org.apache.log4j.Logger;
import com.alcatel.as.diameter.lb.DiameterUtils;
import com.alcatel.as.service.metering2.*;

// this class reports overload info to the remote gload process from SDM

public class Gload implements Runnable, MeterListener {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.gload");
    private static volatile int _3004 = 0;
    
    private ClientState _state;
    private DiameterLoadBalancer _lb;
    private PlatformExecutor _exec;
    private int _id, _cpu = 0;
    private MonitoringJob _job;

    public static void set3004Ratio (int ratio){
	_3004 = ratio;
    }
    
    public Gload (DiameterLoadBalancer lb, int id){
	_lb = lb;
	_id = id;
	_exec = _lb.getPlatformExecutors ().createQueueExecutor (_lb.getPlatformExecutors ().getProcessingThreadPoolExecutor ());
    }
    public Gload start (){
	_exec.execute (this);
	return this;
    }

    public void run (){
	open ();
    }
    private void open (){
	LOGGER.info ("Gload : open");

	if (_state != null){
	    _state = null;
	}
	Map<String, Object> props  = new HashMap<> ();
	props.put (TcpClient.PROP_READ_EXECUTOR, _exec);
	TcpClient client = _lb.getClientFactory ().newTcpClient ("gload", props);
	if (client != null && client.getDestinations ().size () > 0){
	    LOGGER.info ("Gload : activated");
	    try{
		client.open (_state = new ClientState (client));
		if (_job == null){
		    LOGGER.info ("Gload : starting cpu meter monitoring");
		    _job = _lb.getCpuMeter ().startScheduledJob (this, null, _exec, 1000, 0);
		}
		return;
	    }catch(Exception e){
		LOGGER.error ("Exception while instanciating Gload", e);
	    }
	} else {
	    if (_job != null){
		LOGGER.info ("Gload : stopping cpu meter monitoring");
		_job.stop ();
		_job = null;
	    }
	}
	_exec.schedule (this, 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    public Object updated(Meter meter,
			  Object context){
	_cpu = (int) meter.getValue ();
	return null;
    }

    private class ClientState implements TcpClientListener, TcpChannelListener, Runnable {

	private boolean _closed = false;
	private int _pings = 0;
	private int _failed = 0;
	private int _failedMax = 10;
	private TcpClient _client;
	private TcpChannel _channel;
	private byte[] _data = new byte[8];
	private int _read = 0;
	private int _cpuCache = 0, _3004Cache = 0;
	private long _update = 1000L;
	private String _readTimeout;
	
	private ClientState (TcpClient client){
	    _client = client;
	    String s = (String) client.getProperties ().get ("gload.update");
	    if (s != null) _update = Long.parseLong (s);
	    s = (String) client.getProperties ().get ("gload.failed.max");
	    if (s != null) _failedMax = Integer.parseInt (s);
	    _readTimeout = (String) client.getProperties ().get (TcpClient.PROP_READ_TIMEOUT);
	}

	private void close (){
	    _closed = true;
	    _client.close ();
	}

	public void run (){
	    if (_closed) return;
	    int tmpcpu = _cpu;
	    int tmp3004 = _3004;
	    boolean sendcpu = tmpcpu != _cpuCache;
	    boolean send3004 = tmp3004 != _3004Cache;
	    if (LOGGER.isDebugEnabled ()){
		LOGGER.debug ("Gload : sending cpu : "+(sendcpu ? "Y("+tmpcpu+")" : "N("+tmpcpu+")")+", sending 3004 : "+(send3004 ? "Y("+tmp3004+")" : "N("+tmp3004+")"));
	    }
	    if (sendcpu && send3004){ InfoMessage.write (_channel, keyValue (0x11, tmpcpu), keyValue (0x101, tmp3004));}
	    else if (sendcpu) { InfoMessage.write (_channel, keyValue (0x11, tmpcpu));}
	    else if (send3004) { InfoMessage.write (_channel, keyValue (0x101, tmp3004));}
	    _cpuCache = tmpcpu;
	    _3004Cache = tmp3004;
	    _exec.schedule (this, _update, java.util.concurrent.TimeUnit.MILLISECONDS);
	}
        
	public TcpChannelListener connectionEstablished(TcpClient client,
							TcpClient.Destination destination){
	    LOGGER.info ("Gload : connectionEstablished : "+destination);
	    InfoMessage.write (_channel = destination.getChannel (),
			       keyValue (0x01, _id), // send the id
			       keyValue (0x11, 0),
			       keyValue (0x101, 0)
			       );
	    if (_readTimeout == null) _channel.setSoTimeout (3000); // default
	    run ();
	    return this;
	}
	public void connectionFailed(TcpClient client,
				     TcpClient.Destination destination){
	    if (_closed) return;
	    if (++_failed == 1)
		LOGGER.info ("Gload : connectionFailed : "+destination);
	    if (_failed == _failedMax){
		close ();
		open ();
	    }
	}
	public void receiveTimeout(TcpChannel cnx){
	    if (_closed) return;
	    if (++_pings == 2){
		LOGGER.warn ("Gload : ping failed : shutting down the connection");
		cnx.shutdown ();
	    } else {
		PingMessage.write (cnx);
	    }
	}
	public int messageReceived(TcpChannel cnx,
				   java.nio.ByteBuffer buffer){
	    if (_closed) {
		buffer.position (buffer.limit ());
		return 0;
	    }
	    _pings = 0;
	    while (true){
		try{
		    Message msg = parse (buffer);
		    if (msg != null){
			if (LOGGER.isDebugEnabled ())
			    LOGGER.debug ("Gload : received message : "+msg);
			msg.received (cnx);
		    } else {
			break;
		    }
		}catch(Exception e){
		    LOGGER.warn ("Exception while reading message from Gload", e);
		    buffer.position (buffer.limit ());
		    cnx.shutdown ();
		}
	    }
	    return 0;
	}
	public void connectionClosed(TcpChannel cnx){
	    if (_closed) return;
	    LOGGER.info ("Gload : connectionClosed : "+cnx);
	    close ();
	    open ();
	}
	public void writeBlocked(TcpChannel cnx){
	    if (_closed) return;
	    LOGGER.info ("Gload : writeBlocked : "+cnx);
	    cnx.shutdown ();
	}
	public void writeUnblocked(TcpChannel cnx){
	}

	private Message parse (ByteBuffer buffer) throws Exception {
	    int available = buffer.remaining ();
	    int needed = 8 - _read;
	    int willUse = Math.min (needed, available);
	    for (int i=0; i<willUse; i++){
		_data[_read + i] = buffer.get ();
	    }
	    _read += willUse;
	    if (_read == 8){
		_read = 0;
		return messageRead (_data, false);
	    }
	    return null;
	}
    }

    public static Message messageRead (byte[] data, boolean allowInfo) throws Exception {
	if (data[0] != 0x4C ||
	    data[1] != 0x4F ||
	    data[2] != 0x56 ||
	    data[3] != 0x45) throw new Exception ("Invalid Magic Number");
	if (data[4] != 0x01) throw new Exception ("Invalid Version number : "+(data[4] & 0xFF));
	if (!allowInfo){
	    if (data[6] != 0x00 ||
		data[7] != 0x08) throw new Exception ("Invalid Message Length");
	}
	switch ((int) data[5]){
	case 0x01 : return PingMessage.INSTANCE;
	case 0x02 : return PongMessage.INSTANCE;
	case 0x10 : if (!allowInfo) throw new Exception ("Unexpected Overload info from Gload");
	    byte[] content = new byte[data.length - 8];
	    System.arraycopy (data, 8, content, 0, content.length);
	    return new InfoMessage (content);
	case 0x11 : return AckMessage.INSTANCE;
	default : throw new Exception ("Unknown message from Gload : "+ (data[5] & 0xFF));
	}
    }

    public static class Message {
	public int type (){ return -1;}
	public void received (TcpChannel channel){}
    }
    public static class PingMessage extends Message {
	public static final PingMessage INSTANCE = new PingMessage ();
	private static final byte[] CONTENT = new byte[]{(int) 0x4C, (int) 0x4F, (int) 0x56, (int) 0x45, (int) 0x01, (int) 0x01, (int) 0x0, (int) 0x8};
	public int type (){ return 0x1;}
	public void received (TcpChannel channel){
	    PongMessage.write (channel);
	}
	public static void write (TcpChannel channel){
	    channel.send (ByteBuffer.wrap (CONTENT), false);
	}
	public String toString (){ return "PingMessage";}
    }
    public static class PongMessage extends Message {
	public static final PongMessage INSTANCE = new PongMessage ();
	private static final byte[] CONTENT = new byte[]{(int) 0x4C, (int) 0x4F, (int) 0x56, (int) 0x45, (int) 0x01, (int) 0x02, (int) 0x0, (int) 0x8};
	public int type (){ return 0x2;}
	public void received (TcpChannel channel){}
	public static void write (TcpChannel channel){
	    channel.send (ByteBuffer.wrap (CONTENT), false);
	}
	public String toString (){ return "PongMessage";}
    }
    public static class InfoMessage extends Message {
	private static final byte[] CONTENT = new byte[]{(int) 0x4C, (int) 0x4F, (int) 0x56, (int) 0x45, (int) 0x01, (int) 0x10};
	private byte[] _data;
	private InfoMessage (byte[] data){ _data = data;}
	public int type (){ return 0x10;}
	public void received (TcpChannel channel){
	    AckMessage.write (channel);
	}
	public static void write (TcpChannel channel, byte[]... values){
	    if (values == null || values.length == 0) return;
	    int len = 8;
	    for (int i =0; i<values.length; i++) len += values[i].length;
	    byte[] lenB = new byte[]{(byte) (len >> 8), (byte) len};
	    ByteBuffer[] buffs = new ByteBuffer[2 + values.length];
	    buffs[0] = ByteBuffer.wrap (CONTENT);
	    buffs[1] = ByteBuffer.wrap (lenB);
	    for (int i =0; i<values.length; i++) buffs[2+i] = ByteBuffer.wrap (values[i]);
	    channel.send (buffs, false);
	}
	public String toString (){
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("InfoMessage[");
	    if (_data != null){
		for (int i=0; i<_data.length; i++) sb.append (Integer.toHexString (_data[i] & 0xFF)).append (' ');
	    }
	    sb.append (']');
	    return sb.toString ();
	};
    }
    public static class AckMessage extends Message {
	private static final byte[] CONTENT = new byte[]{(int) 0x4C, (int) 0x4F, (int) 0x56, (int) 0x45, (int) 0x01, (int) 0x11, (int) 0x0, (int) 0x8};
	public static final AckMessage INSTANCE = new AckMessage ();
	public int type (){ return 0x11;}
	public void received (TcpChannel channel){}
	public String toString (){ return "AckMessage";}
	public static void write (TcpChannel channel){
	    channel.send (ByteBuffer.wrap (CONTENT), false);
	}
    }

    public static final byte[] keyValue (int key, int value){
	byte[] ret = new byte[8];
	DiameterUtils.setIntValue (key, ret, 0);
	DiameterUtils.setIntValue (value, ret, 4);
	return ret;
    }
    
};
