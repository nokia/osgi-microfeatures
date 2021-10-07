package com.nokia.as.jaxrs.jersey.common.impl;


import java.io.*;
import java.util.*;

public class HttpBodyInputStream extends InputStream {

    private List<byte[]> _data = new ArrayList<> ();
    private int _dataIndex, _byteIndex;
    private byte[] _currentData;

    public HttpBodyInputStream (){
    }

    public HttpBodyInputStream addBody (byte[] data){
	if (data == null || data.length == 0) return this;
	_data.add (data);
	if (_currentData == null) _currentData = data;
	return this;
    }

    public int read (){
	if (_currentData == null) return -1;
	if (_byteIndex == _currentData.length){
	    if (_dataIndex == _data.size () - 1)
		return -1;
	    _currentData = (byte[]) _data.get (++_dataIndex);
	    _byteIndex = 0;
	    return read ();
	}
	return _currentData[_byteIndex++];
    }

    public int read(byte[] b,
		    int off,
		    int len){
	if (_currentData == null) return -1;
	if (_byteIndex == _currentData.length){
	    if (_dataIndex == _data.size () - 1)
		return -1;
	    _currentData = (byte[]) _data.get (++_dataIndex);
	    _byteIndex = 0;
	    return read (b, off, len);
	}
	int read = Math.min (_currentData.length - _byteIndex, len);
	System.arraycopy (_currentData, _byteIndex, b, off, read);
	_byteIndex += read;
	len -= read;
	if (len > 0){
	    int readAgain = read (b, off + read, len);
	    if (readAgain > 0)
		read += readAgain;
	}
	return read;
    }

    public int available (){
	if (_currentData == null) return 0;
	int available = _currentData.length - _byteIndex;
	for (int i = _dataIndex+1; i<_data.size (); i++)
	    available += ((byte[]) _data.get (i)).length;
	return available;
    }
    
}
