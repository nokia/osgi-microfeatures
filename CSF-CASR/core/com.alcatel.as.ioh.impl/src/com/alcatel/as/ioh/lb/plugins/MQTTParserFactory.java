package com.alcatel.as.ioh.lb.plugins;

import com.alcatel.as.ioh.lb.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;

import com.alcatel.as.ioh.client.TcpClient.Destination;
import org.apache.log4j.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"parser.id=mqtt"})
public class MQTTParserFactory implements ParserFactory {
    
    public static final Logger LOGGER = Logger.getLogger ("as.ioh.lb.mqtt");

    public static final String PROP_MQTT_BUFFER = "parser.mqtt.buffer";
    public static final String PROP_MQTT_CONNECT = "parser.mqtt.connect";

    @Activate
    public void start (){
    }

    public Object newParserConfig (Map<String, Object> props){
	return new MQTTParserConfig (props);
    }

    public Parser newParser (Object config, int neededBuffer){
	if (neededBuffer != -1) throw new IllegalArgumentException ();
	return new MQTTParser ((MQTTParserConfig) config);
    }
    
    public String toString (){ return "MQTTParserFactory[id=mqtt]";}

    protected static class MQTTParserConfig {
	
	private int _buffLen;
	private boolean _connect;
	
	protected MQTTParserConfig (Map<String, Object> props){
	    String s = (String) props.get (PROP_MQTT_BUFFER);
	    if (s == null) s = "512";
	    _buffLen = Integer.parseInt (s);
	    s = (String) props.get (PROP_MQTT_CONNECT);   // false is actually not managed
	    if (s == null) s = "true";
	    _connect = Boolean.parseBoolean (s.toLowerCase ());
	}
    }

    private static class MQTTParser implements Parser {

	private ByteBuffer _buffer;
	private int _buffLen;
	private int _len = -1;
	private int _fhLen = -1;
	private boolean _connect;
	private boolean _connectDone;

	protected MQTTParser (MQTTParserConfig config){
	    _connect = config._connect;
	    _buffer = ByteBuffer.allocate (config._buffLen);
	}
	protected void reset (){
	    _buffLen = 0;
	    _len = -1;
	    _fhLen = -1;
	    if (_connect){
		_connectDone = true;
		_buffer = null;
	    }
	}

	public Chunk parse (java.nio.ByteBuffer buffer){
	    if (_connectDone) return DirectParserFactory.INSTANCE_NO_DELAY.parse (buffer);
	    int remaining = buffer.remaining ();
	    if (remaining == 0) return null;
	    for (int i=0; i<remaining; i++){
		byte b = buffer.get ();
		if (_buffer.hasRemaining ()){
		    _buffer.put (b);
		    _buffLen++;
		    if (_len == -1){
			if (_buffLen > 1 && b >= 0){
			    parseLen ();
			}
		    }
		    if (_buffLen == _len){
			Chunk ch = new Chunk (true).setData (_buffer, false);
			setDescription (ch);
			_buffer.flip ();
			reset ();
			return ch;
		    }
		} else {
		    throw new RuntimeException ("Buffer exceeded");
		}
		
	    }
	    return null;
	}
	
	private void parseLen (){ // sets the total length and fixed header length
	    int pos = 1;
	    int multiplier = 1;
	    _len = 0;
	    while (pos < _buffLen){
		int i = _buffer.get (pos++) & 0x7F;
		_len += i * multiplier;
		multiplier *= 128;
		if (multiplier > 128*128*128){
		    throw new RuntimeException ("Invalid MQTT message : invalid length");
		}
	    }
	    _len += pos; // include fixed header length
	    _fhLen = pos;
	}
	
	private Chunk setDescription (Chunk chunk){
	    ByteBuffer data = chunk.getData ();
	    String type = null;
	    switch ((data.get (0) & 0xFF) >> 4){
	    case 1 : {
		type = "CONNECT";
		int offset = _fhLen + 10;
		int slen = data.get (offset++) & 0xFF;
		slen <<= 8;
		slen |= data.get (offset++) & 0xFF;
		int id = 0;
		for (int k=0; k<slen; k++){
		    id += hash (data.get (offset + k)+k);
		}
		id = hash (id);
		chunk.setId (id);
		break;
	    }
	    case 2 : type = "CONNACK"; break;
	    case 3 : type = "PUBLISH"; break;
	    case 4 : type = "PUBACK"; break;
	    case 5 : type = "PUBREC"; break;
	    case 6 : type = "PUBREL"; break;
	    case 7 : type = "PUBCOMP"; break;
	    case 8 : type = "SUBSCRIBE"; break;
	    case 9 : type = "SUBACK"; break;
	    case 10 : type = "UNSUBSCRIBE"; break;
	    case 11 : type = "UNSUBACK"; break;
	    case 12 : type = "PINGREQ"; break;
	    case 13 : type = "PINGRESP"; break;
	    case 14 : type = "DISCONNECT"; break;
	    default: type = "-Unknown-"; break;
	    }
	    return chunk.setDescription (type);
	}
    }

    
    private static int hash(int h) {
	// Ensures the number is unsigned.
	h = (h & 0X7FFFFFFF);

	// This function ensures that hashCodes that differ only by
	// constant multiples at each bit position have a bounded
	// number of collisions (approximately 8 at default load factor).
	h ^= (h >>> 20) ^ (h >>> 12);
	return h ^ (h >>> 7) ^ (h >>> 4);
    }
}
