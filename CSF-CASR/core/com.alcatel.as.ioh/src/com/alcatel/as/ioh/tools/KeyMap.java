package com.alcatel.as.ioh.tools;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class KeyMap {

    protected final int _mappingSize;
    protected final int[] _mapping;
    protected final Random _rnd = new Random (System.currentTimeMillis ());
    protected final List<Integer> _keys = new ArrayList<> ();
    protected int _keysSize = 0;

    public KeyMap (int size){
	_mappingSize = size;
	_mapping = new int[_mappingSize];
	reset ();
    }
    
    public void reset (){
	for (int i=0; i<_mappingSize; i++)
	    _mapping[i] = -1;
	_keys.clear ();
	_keysSize = 0;
    }

    public int getKey (int hash){
	return _keys.get (_mapping[(hash & 0x7FFFFFFF) % _mappingSize]);
    }

    public int addKey (int keyId){
	_keysSize++;
	int pos = _keys.size ();
	for (int k=0; k<pos; k++){
	    if (_keys.get (k) == -1){
		pos = k;
		break;
	    }
	}
	if (pos != _keys.size ()) _keys.remove (pos);
	_keys.add (pos, keyId);
	int ret = 0;
	for (int index=0; index<_mappingSize; index++){
	    if (_rnd.nextInt (_keysSize) == 0){
		_mapping[index] = pos;
		ret++;
	    }
	}
	return ret;
    }

    public int removeKey (int keyId){
	int pos = -1;
	int[] tmp = new int[_keysSize];
	int count = 0;
	for (int k=0; k<_keys.size (); k++){
	    int id = _keys.get (k);
	    if (id == -1) continue;
	    if (id == keyId){
		_keys.remove (k);
		_keys.add (k, -1);
		pos = k;
		continue;
	    }
	    tmp[count++] = k;
	}
	if (pos == -1) return 0;
	_keysSize--;
	if (_keysSize == 0){
	    reset ();
	    return _mappingSize;
	}
	int tmpSize = _keysSize; // tmp.length too long by 1
	int ret = 0;
	for (int index=0; index<_mappingSize; index++){
	    if (_mapping[index] == pos){
		_mapping[index] = tmp[_rnd.nextInt (tmpSize)];
		ret++;
	    }
	}
	return ret;
    }
}
