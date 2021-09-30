package alcatel.tess.hometop.gateways.reactor.util;

// Jdk
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * This class implements an output stream in which the data is written into a
 * java.nio.ByteBuffer. This class automatically grows the ByteBuffer as data is written to it.
 */
public class DataBuffer extends OutputStream {
  private boolean _bigEndian;
  private ByteBuffer _buf;
  private ByteBuffer _preallocBuf;
  private int _initialCapacity;
  private int _mark = -1;
  
  public DataBuffer() {
    this(16, true);
  }
  
  public DataBuffer(int initialCapacity) {
    this(initialCapacity, true);
  }
  
  public DataBuffer(int initialCapacity, boolean bigEndian) {
    this(initialCapacity, bigEndian, false);
  }
  
  public DataBuffer(int initialCapacity, boolean bigEndian, boolean direct) {
    _initialCapacity = initialCapacity;
    _preallocBuf = direct ? ByteBuffer.allocateDirect(initialCapacity) : ByteBuffer.allocate(initialCapacity);
    _buf = _preallocBuf;
    setBigEndian(bigEndian);
  }
  
  // --- OutputStream implementation -----------------------------------------
  
  @Override
  public void write(int b) throws IOException {
    put((byte) b);
  }
  
  @Override
  public void write(byte b[], int off, int len) throws IOException {
    put(b, off, len);
  }
  
  // --- Usefull java.nio.Buffer methods -------------------------------------
  
  public boolean setBigEndian(boolean bigEndian) {
    boolean wasBigEndian = this._bigEndian;
    this._bigEndian = bigEndian;
    _buf.order((bigEndian) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    return wasBigEndian;
  }
  
  public ByteOrder order() {
    return _buf.order();
  }
  
  public int limit() {
    return (_buf.limit());
  }
  
  public DataBuffer mark() {
    _mark = _buf.position();
    return this;
  }
  
  public DataBuffer reset() {
    if (_mark == -1) {
      throw new RuntimeException("Mark not set");
    }
    _buf.position(_mark);
    _mark = -1;
    return this;
  }
  
  public DataBuffer limit(int newLimit) {
    _buf.limit(newLimit);
    return this;
  }
  
  public int position() {
    return (_buf.position());
  }
  
  public DataBuffer position(int newPos) {
    if (_buf.capacity() < newPos) {
      ensureCapacity(newPos);
    }
    _buf.position(newPos);
    return this;
  }
  
  public DataBuffer flip() {
    _buf.flip();
    return this;
  }
  
  public DataBuffer rewind() {
    _buf.rewind();
    return this;
  }
  
  public DataBuffer compact() {
    _buf.compact();
    return this;
  }
  
  public DataBuffer clear() {
    _buf.clear();
    return this;
  }
  
  public int capacity() {
    return (_buf.capacity());
  }
  
  public int remaining() {
    return (_buf.remaining());
  }
  
  public boolean hasRemaining() {
    return (_buf.hasRemaining());
  }
  
  public boolean hasArray() {
    return _buf.hasArray();
  }
  
  public byte[] array() {
    return _buf.array();
  }
  
  // --- Usefull java.nio.Buffer putter methods -------------------------------------
  
  public DataBuffer putUnsigned(int b) {
    ensureCapacity(1);
    _buf.put((byte) (b & 0xff));
    return this;
  }
  
  public DataBuffer putUnsigned(int pos, int b) {
    ensureCapacity(1);
    _buf.put(pos, (byte) (b & 0xff));
    return this;
  }
  
  public DataBuffer put(byte b) {
    ensureCapacity(1);
    _buf.put(b);
    return this;
  }
  
  public DataBuffer put(ByteBuffer b) {
    ensureCapacity(b.remaining());
    _buf.put(b);
    return this;
  }
  
  public DataBuffer put(ByteBuffer ... bufs) {
    for (ByteBuffer buf : bufs) {
      if (buf.hasRemaining()) {
        ensureCapacity(buf.remaining());
        this._buf.put(buf);
      }
    }
    return this;
  }
  
  public DataBuffer put(DataBuffer b) {
    ensureCapacity(b.remaining());
    _buf.put(b._buf);
    return this;
  }
  
  public DataBuffer put(byte b[]) {
    ensureCapacity(b.length);
    _buf.put(b, 0, b.length);
    return this;
  }
  
  public DataBuffer put(byte b[], int off, int len) {
    ensureCapacity(len);
    _buf.put(b, off, len);
    return this;
  }
  
  public DataBuffer putShort(short val) {
    ensureCapacity(2);
    _buf.putShort(val);
    return this;
  }
  
  public DataBuffer putUnsignedShort(int val) {
    ensureCapacity(2);
    _buf.putShort((short) (val & 0xffff));
    return this;
  }
  
  public DataBuffer putUnsignedShort(int pos, int val) {
    ensureCapacity(2);
    _buf.putShort(pos, (short) (val & 0xffff));
    return this;
  }
  
  public DataBuffer putShort(int position, short val) {
    if (_buf.position() < position + 2) {
      ensureCapacity(2);
    }
    _buf.putShort(position, val);
    return this;
  }
  
  public DataBuffer putInt(int val) {
    ensureCapacity(4);
    _buf.putInt(val);
    return this;
  }
  
  public DataBuffer putInt(int position, int val) {
    if (_buf.position() < position + 4) {
      ensureCapacity(4);
    }
    _buf.putInt(position, val);
    return this;
  }
  
  public DataBuffer putLong(long val) {
    ensureCapacity(8);
    _buf.putLong(val);
    return this;
  }
  
  public DataBuffer putLong(int position, long val) {
    if (_buf.position() < position + 8) {
      ensureCapacity(8);
    }
    _buf.putLong(position, val);
    return this;
  }
  
  // --- Usefull java.nio.Buffer getter methods --------------------------------
  
  public int getUnsigned() {
    return (_buf.get() & 0xff);
  }
  
  public int getUnsigned(int pos) {
    return (_buf.get(pos) & 0xff);
  }
  
  public byte get() {
    return (_buf.get());
  }
  
  public DataBuffer get(byte b[], int off, int len) {
    _buf.get(b, off, len);
    return this;
  }
  
  public DataBuffer get(byte b[]) {
    _buf.get(b, 0, b.length);
    return this;
  }
  
  public short getShort() {
    return (_buf.getShort());
  }
  
  @SuppressWarnings("cast")
  public int getUnsignedShort() {
    return (((int) _buf.getShort()) & 0xffff);
  }
  
  @SuppressWarnings("cast")
  public int getUnsignedShort(int pos) {
    return (((int) _buf.getShort(pos)) & 0xffff);
  }
  
  public short getShort(int position) {
    return (_buf.getShort(position));
  }
  
  public int getInt() {
    return (_buf.getInt());
  }
  
  public int getInt(int pos) {
    return (_buf.getInt(pos));
  }
  
  public long getLong() {
    return _buf.getLong();
  }
  
  public long getLong(int pos) {
    return _buf.getLong(pos);
  }
  
  // --- Other Convenient methods ---------------------------------------
  
  @Override
  public String toString() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      mark();
      write(out);
    }
    
    catch (IOException e) {
      return (super.toString());
    }
    
    finally {
      reset();
    }
    
    return new String(out.toByteArray());
  }
  
  public static String toString(ByteBuffer buf) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    try {
      buf.mark();
      while (buf.hasRemaining()) {
        out.write(buf.get());
      }
    }
    
    finally {
      buf.reset();
    }
    
    return new String(out.toByteArray());
  }
  
  /**
   * Method used to log the packet received by the selector.
   */
  public static StringBuffer toString(StringBuffer sb, ByteBuffer buf) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    buf.mark();
    
    while (buf.hasRemaining()) {
      out.write(buf.get());
    }
    buf.reset();
    
    byte[] bytes = out.toByteArray();
    for (int i = 0; i < bytes.length; i++) {
      if (!Utils.isPrintable(bytes[i])) {
        sb.append(Utils.toString(bytes));
        return sb;
      }
    }
    
    sb.append(new String(bytes));
    return sb;
  }
  
  public int write(DatagramChannel channel, InetSocketAddress to) throws IOException {
    return channel.send(this._buf, to);
  }
  
  public SocketAddress read(DatagramChannel channel) throws IOException {
    return channel.receive(_buf);
  }
  
  public int write(OutputStream out) throws IOException {
    int n = _buf.remaining();
    while (_buf.hasRemaining()) {
      out.write(_buf.get());
    }
    return n;
  }
  
  public int write(GatheringByteChannel channel) throws IOException {
    int n, sent = 0;
    while (_buf.hasRemaining()) {
      if ((n = channel.write(_buf)) == 0) {
        break;
      }
      sent += n;
    }
    return sent;
  }
  
  /**
   * Write this buffer, as well as other some extra buffers to a given channel.
   */
  public long write(GatheringByteChannel channel, ByteBuffer[] bufs) throws IOException {
    long sent = 0;
    if (bufs == null) {
      int n;
      
      // No extra buffers to send along with our buffer: flush out bufer to the socket.
      while (_buf.hasRemaining()) {
        if ((n = channel.write(_buf)) == 0) {
          return sent;
        }
        sent += n;
      }
      return sent;
    }
    
    // We have some extra buffers, if our buffer has some data to be sent,
    // Make a ByteBuffer array containing our buffer, as well as the extra buffers.
    ByteBuffer[] array = bufs;
    long n;
    
    if (_buf.hasRemaining()) {
      array = new ByteBuffer[1 + bufs.length];
      array[0] = _buf;
      System.arraycopy(bufs, 0, array, 1, bufs.length);
    }
    
    while ((n = channel.write(array, 0, array.length)) > 0) {
      sent += n;
    }
    return sent;
  }
  
  public int read(ScatteringByteChannel channel) throws IOException {
    return (channel.read(_buf));
  }
  
  public int read(InputStream in) throws IOException {
    int b, bytesRead = 0;
    
    while ((b = in.read()) != -1) {
      write(b);
      bytesRead++;
    }
    
    return (bytesRead);
  }
  
  public int read(InputStream in, int max) throws IOException {
    int b, bytesRead = 0;
    
    while (--max >= 0 && (b = in.read()) != -1) {
      write(b);
      bytesRead++;
    }
    
    return (bytesRead);
  }
  
  public DataBuffer resetCapacity() {
    if (_buf.capacity() > _initialCapacity) {
      _buf = _preallocBuf;
      _buf.order((_bigEndian) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    }
    _buf.clear();
    return this;
  }
  
  public DataBuffer resetCapacity(int size) {
    if (_buf.capacity() > size) {
      _buf = ByteBuffer.allocate(size);
      _buf.order((_bigEndian) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    } else {
      _buf.clear();
    }
    
    return this;
  }
  
  /**
   * Ensure that minSize bytes can be appended to this buffer.
   */
  public DataBuffer ensureCapacity(int bytesToAdd) {
    if (_buf.remaining() < bytesToAdd) {
      int n = Math.max(_buf.capacity() << 1, _buf.position() + bytesToAdd);
      ByteBuffer newBuf = ByteBuffer.allocate(n);
      
      newBuf.order((_bigEndian) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
      _buf.flip();
      newBuf.put(_buf);
      _buf = newBuf;
    }
    
    return this;
  }
  
  public ByteBuffer getInternalBuffer() {
    return (_buf);
  }
  
  public static void main(String args[]) throws Exception {
    DataBuffer B = new DataBuffer(0);
    System.out.println("b.cap=" + B.capacity());
    
    DataBuffer buf = new DataBuffer(32);
    
    for (int i = 0; i < 32; i++)
      buf.putUnsigned(i);
    
    buf.putInt(29, 10).position(33); // absolute put: we must set the position manually.
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    buf.flip();
    buf.write(baos);
    
    byte[] b = baos.toByteArray();
    System.out.println(Utils.toString(b, 0, b.length));
    
    buf.clear().resetCapacity(16);
    ByteArrayInputStream in = new ByteArrayInputStream(b);
    System.out.println("read " + buf.read(in, 33) + " bytes");
    buf.flip();
    
    baos.reset();
    buf.write(baos);
    b = baos.toByteArray();
    System.out.println(Utils.toString(b, 0, b.length));
  }
}
