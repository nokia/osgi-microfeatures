package com.alcatel.as.diameter.lb.impl;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.*;
import java.net.*;

import com.alcatel.as.ioh.client.TcpClient;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.diameter.lb.DestinationManager;
import alcatel.tess.hometop.gateways.reactor.*;

public class WeightedDestinationManager extends AbstractDestinationManager {

    private int _totalWeight = 0;
    private List<Element> _elementsList = new ArrayList<Element> ();
    private Map<Object, Element> _elementsMap = new HashMap<Object, Element> ();
    private Destination[] _distribution;
    private int _initialSize, _maxWeigth;
    
    public WeightedDestinationManager(String group, int size){
	super (group);
	if (size == 0) return; // this is an edge case tested later in ClientContext
	_distribution = new Destination[_initialSize = size];
	_maxWeigth = Integer.MAX_VALUE / _initialSize;
    }
    public String toString(){ return "WeightedDestinationManager["+_group+", "+_elementsList.size()+"]";}

    public void clear (){
	_totalWeight = 0;
	_elementsList.clear ();
	_elementsMap.clear ();
	//configure (); not needed : this WeightedDestinationManager wont be re-used
    }

    // IMPORTANT : WeightedDestinationManager disables get(int hash): this simplified the AAA router - also not sure if it makes sense with weights...
    @Override
    public Destination get (int hash){
	return getAny ();
    }

    public void available (Destination dest, int weight){
	update (dest, weight);
    }
    
    public void unavailable (Destination dest){
	update (dest, 0);
    }

    private class Element {
	private Destination x;
	private int weight;
	private Element (Destination x, int weight){
	    this.x = x;
	    this.weight = weight;
	}
    }
    private void configure (){
	int size = _distribution.length;
	if (_totalWeight == 0){
	    for (int i=0; i<size; i++) _distribution[i] = null;
	    return;
	}
	int off = 0;
	Element maxEl = null;
	for (Element e : _elementsList){
	    if (e.weight == 0) continue;
	    int correctedWeight = (e.weight * size) / _totalWeight;
	    if (maxEl == null || maxEl.weight < e.weight) maxEl = e;
	    int correctedWeightLimit = correctedWeight + off;
	    for (int i=off; i<correctedWeightLimit; i++) _distribution[i] = e.x;
	    off = correctedWeightLimit;
	}
	for (int i=off; i<size; i++) _distribution[i] = maxEl.x;
    }
    
    private void checkWeight (Destination x, int weight){
	if (weight < 0 || weight > _maxWeigth)
	    throw new IllegalArgumentException ("Invalid weight : "+weight+" for : "+x);
    }
    
    public void add (Destination x, int weight){
	super.add (x, weight);
	checkWeight (x, weight);
	Element el = new Element (x, weight);
	_elementsList.add (el);
	_elementsMap.put (x, el);
	if (weight == 0) return;
	_totalWeight += weight;
	configure ();
    }

    public void update (Destination x, int weight){
	checkWeight (x, weight);
	Element e = _elementsMap.get (x);
	if (e != null){
	    if (e.weight == weight) return;
	    _totalWeight += weight - e.weight;
	    e.weight = weight;
	    configure ();
	}
    }

    public boolean remove (Destination x){
	super.remove (x);
	Element e = _elementsMap.remove (x);
	if (e != null){
	    _elementsList.remove (e);
	    _totalWeight -= e.weight;
	    configure ();
	    return true;
	}
	return false;
    }

    public Destination getAny (){
	if (_totalWeight == 0){
	    int size = _elementsList.size ();
	    if (size == 0) return null;
	    Element el = _elementsList.get (ThreadLocalRandom.current().nextInt (size));
	    return el.x;
	}
	int random = ThreadLocalRandom.current().nextInt (_distribution.length);
	return _distribution[random];
    }

    public static void main (String[] s){
	WeightedDestinationManager m = new WeightedDestinationManager ("test", 100);
	Dummy d1 = new Dummy ("d1");
	Dummy d2 = new Dummy ("d2");
	Dummy d3 = new Dummy ("d3");
	m.add (d1, 10);
	m.add (d2, 0);
	m.add (d3, 0);
	int ok1 = 0, ok2 = 0, ok3=0;
	for (int i=0; i<1000000; i++){
	    Dummy d = (Dummy) m.getAny ();
	    if (d == d1) ok1++;
	    else if (d == d2) ok2++;
	    else if (d == d3) ok3++;
	}
	int ok = ok1+ok2+ok3;
	System.out.println ("-----> OK "+((ok1*100)/ok)+"/"+((ok2*100)/ok)+"/"+((ok3*100)/ok));
    }

    public static class Dummy implements Destination {
	private String _id;	
	private Dummy (String s){ _id = s;}
	public String toString(){ return _id;}
	public void execute (Runnable r){}
	public TcpClient getTcpClient (){return null;}
	public InetSocketAddress getRemoteAddress (){return null;}
	public TcpChannel getChannel (){return null;}
	public boolean isOpen (){return true;}
	public void close (){}
	public boolean isAvailable (){return true;}
	public int[] getHistory (){return null;}
	public Map<String, Object> getProperties (){return null;}
	public <T> T attach (Object attachment){return null;}
	public <T> T attachment (){return null;}
	public int send (byte[] data, boolean availableOnly){return 0;}
	public void open () {}
    }
}
