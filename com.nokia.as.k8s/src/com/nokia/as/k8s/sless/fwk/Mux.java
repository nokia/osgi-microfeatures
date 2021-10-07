package com.nokia.as.k8s.sless.fwk;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;

public class Mux {

    public static final int FLAG_PUSH = 0;
    public static final int FLAG_UNPUSH = 1;
    public static final int FLAG_GOGO_REQ = 2;
    public static final int FLAG_GOGO_RESP = 3;
    
    public final static int[] CONTROLLER_ID = new int[] {
	499
    };

    
    public static java.nio.charset.Charset UTF_8 = null;
    static {
        try{
            UTF_8 = java.nio.charset.Charset.forName ("utf-8");
        }catch(Exception e){}
    }

    public static String getUTF8 (ByteBuffer buffer){
        byte[] bytes = new byte[buffer.remaining ()];
        buffer.get (bytes);
        return new String (bytes, 0, bytes.length, UTF_8);
    }

    public static ByteBuffer toByteBuffer (CustomResource... resources){
	int i = 0;
	try{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	    for (CustomResource resource : resources){
		ObjectOutputStream oos = new ObjectOutputStream (baos);
		oos.writeObject (resource.attributes ());
		oos.flush ();
		i++;
	    }
	    return ByteBuffer.wrap (baos.toByteArray ());
	}catch(Exception e){
	    throw new RuntimeException ("Failed to serialize : "+resources[i], e);
	}
    }
    
    public static CustomResource readResource (ByteBuffer buff, CustomResourceDefinition crd){
	try{
	    InputStream in = new InputStream (){
		    public int read (){
			if (buff.remaining () == 0) return -1;
			return buff.get () & 0xFF;
		    }
		};
	    ObjectInputStream ois = new ObjectInputStream (in);
	    Map<String, Object> attributes = (Map<String, Object>) ois.readObject ();
	    CustomResource resource = new CustomResource(attributes, crd);
	    return resource;
	}catch(Exception e){
	    throw new RuntimeException ("Failed to deserialize resource", e);
	}
    }
    
}
