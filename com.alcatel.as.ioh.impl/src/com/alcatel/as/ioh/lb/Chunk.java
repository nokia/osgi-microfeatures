// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.nio.ByteBuffer;

public class Chunk {
    
    public ByteBuffer _data;
    public boolean _newMessage, _isCopy;
    public int _id = -1;
    public String _toString;
    public Object _attachment;
    
    public Chunk (boolean newMessage){_newMessage = newMessage;}

    public Chunk setDescription (String toString){ _toString = toString; return this;}

    public Chunk setData (ByteBuffer data, boolean isCopy){ _data = data; _isCopy = isCopy; return this;}

    public Chunk setId (int id){ _id = id; return this;}

    public boolean newMessage (){ return _newMessage;}

    public ByteBuffer getData (){ return _data;}

    public boolean isCopy (){ return _isCopy;}

    public int size (){ return _data.remaining ();}

    public int getId (){ return _id;}

    public String toString (){
	return (_newMessage ? "Chunk[" : "ChunkCont[")+_toString+", id="+_id+", len="+(_data != null ? _data.remaining () : "N/A")+(_attachment != null ? ", "+_attachment.toString () : "")+"]";
    }

    public <T> T attachment (){ return (T) _attachment;}
    public Chunk attach (Object o){ _attachment = o; return this;}

}
