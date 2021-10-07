package com.alcatel.as.ioh.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class CompositeByteArray {
    
    private static class Block {
	private byte[] data;
	private Block next;
	private int writeIndex = 0, number, limit, offset;
	private Segment removed;
	private CompositeByteArray parent;
	private Block (CompositeByteArray parent, int number, int size){
	    if (number > parent.maxBlocks) throw new java.nio.BufferOverflowException ();
	    this.parent = parent;
	    this.number = number;
	    data = new byte[size];
	    if (number != 0){
		limit = parent.root.data.length + number*size;
		offset = limit - size;
	    } else {
		offset = 0;
		limit = size;
	    }
	}
	private boolean add (byte b){
	    if (writeIndex == data.length) return false;
	    data[writeIndex++] = b;
	    return true;
	}
	public int add (byte[] b, int off, int len){
	    int written = Math.min (len, data.length - writeIndex);
	    System.arraycopy (b, off, data, writeIndex, written);
	    writeIndex += written;
	    return written;
	}
	private byte get (int pos){
	    return data[pos - offset];
	}
	private int getInt (int pos){
	    int index = pos - offset;
	    if (index >= writeIndex) return -1;
	    return data[index] & 0xFF;
	}
	private Object iterate (int start, int end, SegmentIterator it, Object ctx){
	    if (removed == null)
		return it.next (data, start - offset, end - start, ctx);
	    Segment s = removed;
	    while (s != null){
		if (start < s.start){
		    if (end <= s.start){
			return it.next (data, start - offset, end - start, ctx);
		    } else {
			ctx = it.next (data, start - offset, s.start - start, ctx);
		    }
		}
		start = s.end;
		if (start >= end) return ctx;
		s = s.next;
	    }
	    int len = end - start;
	    if (len > 0)
		ctx = it.next (data, start - offset, len, ctx);
	    return ctx;
	}
	
	private static SegmentIterator writeIterator = new SegmentIterator (){
		public Object next (byte[] data, int start, int len, Object ctx){
		    try{
			((OutputStream)ctx).write (data, start, len);
		    }catch(IOException e){}
		    return ctx;
		}
	    };
	private void write (OutputStream out, int start, int end){
	    iterate (start, end, writeIterator, out);
	}
	private static SegmentIterator toByteBufferIterator = new SegmentIterator (){
		public Object next (byte[] data, int start, int len, Object ctx){
		    return ((ByteBuffer)ctx).put (data, start, len);
		}
	    };
	private ByteBuffer toByteBuffer (ByteBuffer out, int start, int end){
	    return (ByteBuffer) iterate (start, end, toByteBufferIterator, out);
	}
	private static SegmentIterator nbSegmentsIterator = new SegmentIterator (){
		public Object next (byte[] data, int start, int len, Object ctx){
		    return ((Integer)ctx)+1;
		}
	    };
	private int nbSegments (int start, int end){
	    if (removed == null) return 1;
	    return (Integer) iterate (start, end, nbSegmentsIterator, 0);
	}
	private static SegmentIterator sizeIterator = new SegmentIterator (){
		public Object next (byte[] data, int start, int len, Object ctx){
		    return ((Integer)ctx)+len;
		}
	    };
	private int size (int start, int end){
	    if (removed == null) return end - start;
	    return (Integer) iterate (start, end, sizeIterator, 0);
	}
	private int writeTo (final ByteBuffer[] buffs, final int bOffset, int start, int end){
	    SegmentIterator it = new SegmentIterator (){
		    public Object next (byte[] data, int start, int len, Object ctx){
			int pos = (Integer) ctx;
			buffs[pos++] = ByteBuffer.wrap (data, start, len);
			return pos;
		    }
		};
	    return (Integer) iterate (start, end, it, bOffset);
	}
	private void remove (Segment s){
	    Segment prev = null;
	    Segment current = removed;
	    while (true){
		if (current == null){
		    if (prev == null) removed = s;
		    else prev.next = s;
		    return;
		}
		if (current.start > s.start){
		    if (prev == null){
			removed = s;
		    } else {
			prev.next = s;
		    }
		    s.next = current;
		    return;
		} else {
		    prev = current;
		    current = current.next;
		}
	    }
	}
	private String debug (){ return debug (new StringBuilder ()).toString ();}
	private StringBuilder debug(StringBuilder sb){
	    sb.append ("\tnumber=").append (number);
	    sb.append (" offset=").append (offset);
	    sb.append (" limit=").append (limit);
	    sb.append (" writeIndex=").append (writeIndex);
	    Segment s = removed;
	    while (s != null){
		sb.append (" removed.start/end=").append (s.start);
		sb.append ('/').append (s.end);
		s = s.next;
	    }
	    return sb;
	}
    }
    private static class Segment {
	private int start, end;
	private Segment next;
	private Segment (int start, int end){
	    this.start = start;
	    this.end = end;
	}
    }
    
    protected static interface SegmentIterator {
	public Object next (byte[] data, int start, int len, Object ctx);
    }
    protected static interface BlockIterator {
	public Object next (Block b, int start, int len, Object ctx);
    }
    
    private int blockSize, limit = 0, maxBlocks;
    private Block root, current;
    
    public CompositeByteArray (int initialSize, int extensionSize){
	this (initialSize, extensionSize, Integer.MAX_VALUE);
    }
    public CompositeByteArray (int initialSize, int extensionSize, int maxExtensions){
	maxBlocks = maxExtensions;
	blockSize = extensionSize;
	root = current = new Block(this, 0, initialSize);
    }

    public String debug(){
	StringBuilder sb = new StringBuilder ();
	Block b = root;
	sb.append ("limit=").append (limit);
	sb.append ('\n');
	while (b!=null){
	    b.debug (sb);
	    sb.append ('\n');
	    b = b.next;
	}
	return sb.toString ();
    }

    public int nbBlocks (){
	int n = 1;
	Block b = root;
	while ((b = b.next) != null) n++;
	return n;
    }

    public int limit (){ return limit;}
    
    public int add (byte b){
	if (current.add (b) == false){
	    current = current.next = new Block (this, current.number+1, blockSize);
	    current.add (b);
	}
	return ++limit;
    }
    public int add (byte[] b) { return add (b, 0, b.length);}
    public int add (byte[] b, int off, int len){
	int written = current.add (b, off, len);
	if (written < len){
	    current = current.next = new Block (this, current.number+1, blockSize);
	    add (b, off + written, len - written);
	}
	return limit += written;
    }

    public CompositeByteArray remove (int start){
	return remove (start, limit);
    }
    public CompositeByteArray remove (int start, int end){
	return remove (start, end, root);
    }
    private CompositeByteArray remove (int start, int end, Block from){
	Block b = getBlock (start, from);
	if (end <= b.limit){
	    b.remove (new Segment (start, end));
	} else {
	    b.remove (new Segment (start, b.limit));
	    remove (b.limit, end, b.next);
	}
	return this;
    }

    public CompositeByteArray cutFrom (int pos){
	return cut (limit - pos);
    }
    public CompositeByteArray cut (int n){
	limit -= n;
	current = getBlock (limit);
	Block b = current.next;
	while (b != null){
	    n -= b.writeIndex;
	    b = b.next;
	}
	current.next = null;
	current.writeIndex -= n;
	return this;
    }

    public byte get (int pos){
	return getBlock (pos).get (pos);
    }
    private byte get (int pos, Block from){
	return getBlock (pos, from).get (pos);
    }

    private Block getBlock (int pos){
	return getBlock (pos, root);
    }
    private Block getBlock (int pos, Block from){
	Block b = from;
	while (true){
	    if (pos < b.limit) return b;
	    if ((b=b.next) == null) return null;
	}
    }

    private Object iterate (int start, int end, BlockIterator it, Object ctx){
	Block b = getBlock (start);
	while (b != null){
	    if (end > b.limit){
		ctx = it.next (b, start, b.limit, ctx);
		start = b.limit;
		b = b.next;
	    } else {
		return it.next (b, start, end, ctx);
	    }
	}
	return ctx;
    }
    
    public void write (OutputStream out) throws IOException {
	write (out, 0);
    }
    public void write (OutputStream out, int start) throws IOException {
	write (out, start, limit);
    }
    private static BlockIterator writeIterator = new BlockIterator (){
	    public Object next (Block b, int bstart, int bend, Object ctx){
		b.write ((OutputStream)ctx, bstart, bend);
		return ctx;
	    }
	};
    public void write (final OutputStream out, int start, int end) throws IOException{
	iterate (start, end, writeIterator, out);
    }

    public InputStream getInputStream (){
	return getInputStream (0);
    }
    public InputStream getInputStream (int from){
	return getInputStream (from, Integer.MAX_VALUE);
    }
    public InputStream getInputStream (final int from, final int len){
	return new InputStream (){
	    private int left = len;
	    private Block b = getBlock (from);
	    private int index = from;
	    public int read (){
		if (left-- <= 0) return -1;
		return (b = getBlock (index, b)).getInt (index++);
	    }
	};
    }

    private static BlockIterator nbSegmentsIterator = new BlockIterator (){
	    public Object next (Block b, int start, int len, Object ctx){
		return ((Integer)ctx)+b.nbSegments (start, len);
	    }
	};
    public int nbSegments (){
	return nbSegments (0);
    }
    public int nbSegments (int start){
	return nbSegments (start, limit);
    }
    public int nbSegments (int start, int end){
	return (Integer) iterate (start, end, nbSegmentsIterator, 0);
    }

    private static BlockIterator sizeIterator = new BlockIterator (){
	    public Object next (Block b, int start, int len, Object ctx){
		return ((Integer)ctx)+b.size (start, len);
	    }
	};
    public int size (){
	return size (0);
    }
    public int size (int start){
	return size (start, limit);
    }
    public int size (int start, int end){
	return (Integer) iterate (start, end, sizeIterator, 0);
    }

    public ByteBuffer[] toByteBuffers (){
	return toByteBuffers (0);
    }
    public ByteBuffer[] toByteBuffers (int start){
	return toByteBuffers (start, limit);
    }
    public ByteBuffer[] toByteBuffers (int start, int end){
	int nb = nbSegments (start, end);
	final ByteBuffer[] res = new ByteBuffer[nb];
	BlockIterator it = new BlockIterator (){
		public Object next (Block b, int start, int end, Object ctx){
		    int pos = (Integer) ctx;
		    return b.writeTo (res, pos, start, end);
		}
	    };
	iterate (start, end, it, 0);
	return res;
    }
    
    public boolean hasRemaining() {
    	return current.writeIndex > 0;
    }

    public ByteBuffer toByteBuffer (){
	return toByteBuffer (0);
    }
    public ByteBuffer toByteBuffer (int start){
	return toByteBuffer (start, limit);
    }
    private static BlockIterator toByteBufferIterator = new BlockIterator (){
	    public Object next (Block b, int start, int end, Object ctx){
		return b.toByteBuffer ((ByteBuffer) ctx, start, end);
	    }
	};
    public ByteBuffer toByteBuffer (int start, int end){
	int size = size (start, end);
	if (size == end - start && nbBlocks () == 1){
	    Block b = getBlock (start);
	    return ByteBuffer.wrap (b.data, start - b.offset, size);
	}
	ByteBuffer buff = ByteBuffer.allocate (size);
	iterate (start, end, toByteBufferIterator, buff);
	return (ByteBuffer)buff.flip ();
    }

    public static void main (String[] ss) throws Exception{
	CompositeByteArray cba = test();
	ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	cba.write (baos);
	String s = new String (baos.toByteArray ());
	System.out.println ("All is : "+s);
	cba.remove (0, 10);
	baos = new ByteArrayOutputStream ();
	cba.write (baos);
	s = new String (baos.toByteArray ());
	System.out.println ("strip A : "+s);
	
	cba = test().remove (5, 15);
	baos = new ByteArrayOutputStream ();
	cba.write (baos);
	s = new String (baos.toByteArray ());
	System.out.println ("strip half A and B : "+s);
	
	cba = cba.test ().remove (10, 15);
	baos = new ByteArrayOutputStream ();
	cba.write (baos);
	s = new String (baos.toByteArray ());
	System.out.println ("strip B : "+s);
	
	cba = cba.test ().remove (10);
	baos = new ByteArrayOutputStream ();
	cba.write (baos);
	s = new String (baos.toByteArray ());
	System.out.println ("only A : "+s);

	cba = cba.test ().remove (2, 3).remove (5, 6).remove (10, 11).remove (15, 16).remove (16, 20);
	baos = new ByteArrayOutputStream ();
	cba.write (baos);
	s = new String (baos.toByteArray ());
	System.out.println ("only 8 A and 4 B and no C: "+s);
	baos = new ByteArrayOutputStream ();
	cba.write (baos, 10);
	s = new String (baos.toByteArray ());
	System.out.println ("no A 4 B and no C: "+s);
	baos = new ByteArrayOutputStream ();
	cba.write (baos, 10, 15);
	s = new String (baos.toByteArray ());
	System.out.println ("4B : "+s);

	cba = cba.test ().cut (14);
	baos = new ByteArrayOutputStream ();
	cba.write (baos);
	s = new String (baos.toByteArray ());
	System.out.println ("1 C no D and no E : "+s);
    }
    public static CompositeByteArray test () {
	CompositeByteArray cba = new CompositeByteArray (10, 5);
	char c = 'A';
	for (int i=0; i<10; i++) cba.add ((byte)c);
	for (int n=1; n<5; n++) {
	    c++;
	    for (int i=0; i<5; i++) cba.add ((byte)c);
	}
	return cba;
    }
}
