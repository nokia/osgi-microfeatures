This Module provides generic tools used by many modules.
The followng packages are supplied:

- alcatel.tess.hometop.gateways.concurrent

  This contains some concurrency helper classes, like thread pool, Queues, etc ...

- alcatel.tess.hometop.gateways.tracer

  This package contains the tracer used by the proxyplatform.

- alcatel.tess.hometop.gateways.utils

  This package contains general utilities, like fast collections, object pooling, etc ...

Here is a brief breakdown for all classes:

Classes in concurrent package:
---------------------------------

AsyncOutputStream.java

  Class for parallel OutputStream writing.
  One thread is started and will concurrently write bytes to the underlying output stream.
  Notice that this stream is not thread safe. If you need a thread safe version, please
  decorate this stream with the SynchronizedOutputStream class.

QueueIF.java

  Base class for all queues. Queues are used to let threads exchange some objects between them.

BufferQueue.java

  A variation of the Doug Lea's "bounded buffer queue". (see book "Java concurrent progaming, second edition").
  This class is the default one used in the proxyplatform. It reduces thread contention between 
  reader/writer threads.
  
BoundedBufferWithDelegates.java

  Doug Lee's "BufferWithDelegates" implementation (see book "Java concurrent programing, second edition).
  Actually, this class is just an experimental class and is not used by the product.

LinkedQueue.java

  Doug Lea's unbounded Linked Queue. Not used by default. It provides a good level of concurrency, but
  does not allow flow control between reader/writer threads.

BoundedLinkedQueue.java
 
  Bounded version of the LinkedQueue class. Warning: this class is experimental and currently
  does not provide good performances.

IOEventListener.java and PollingIOWorker.java

  Classes used to poll many sockets using a single thread. Deprecated: you can now use the
  java.nio package available from the jdk 1.4

ThreadPool.java

  A simple thread manager that provide fast thread reuse.
  The constructor takes the following parameters:
    -The max numer of thread allocated by the pool
    -The max size of the internal queue used by the thread pool forholding tasks before 
     they are executed. This queue will hold only the Runnable tasks submitted by the 
     ThreadPool.start() method.
    -The queue implementation ("buffer" will use the BufferQueue.java implem). 
     See QueueFactory for details.
    -Max number of idle threads: By default, an idle thread within the pool will be kept alive
     at most 10 seconds. This parameter specifies the number of threads that must be kept 
     for ever, even if they are idle.

EventDispatcher.java

  A thread pool variation: you can use this thread pool when you wan't to handle user message
  concurrently (using a thread pool), but you also need to ensure the "happened before" rule:
  all user messages must be serially executed (ie: if user "Bob" sends message M1,M2, then 
  M1 must be handled before M2.

  Here is an example: 

  1/ create the thread pool:

    ThreadPool tp = new ThreadPool("TestThreadPool", 100, 1024, "buffer", 0);
    EventDispatcher dispatcher = new EventDispatcher(tp);

  2/ When Bob is connected, register him into the event dispatcher:
  
    dispatcher.addUser("Bob");

  3/ When Bob sends two request R1,R2, handle them like this:

    dispatcher.dispatch("Bob", new MyRunnable("R1")); // The runnable will handle request R1
    dispatcher.dispatch("Bob", new MyRunnable("R2")); // The runnable will handle request R2

  4/ When user Bob disconnect: deregister him from the dispatcher:

    dispatcher.removeUser("Bob");

ReadWriteLock.java

  Simple read/write locker

ReentrantLock.java: 

  Class similar to jdk 1.5 java.util.concurrent.lock.ReentrantLock

ThreadContext.java

  This class is rather similar to java.lang.ThreadLocal, except that it uses the
  hashtable provided in the "utils" package, which is far more efficient than standard jdk
  hashtables.
  
Classes in utils package:
-------------------------

ByteArrayManager.java
  
  Pool of byte arrays: you can acquire a byte array from this pool quickly, provided that
  you release it using the ByteArrayManager.releaseByteArray() method.

ByteBuffer.java:

  The ByteBuffer and Bytes classes are similar to the java.lan.StringBuffer,
  and manage byte arrays efficiently

ByteInputStream.java

  Same as ByteArrayInputStream except that this class is not synchronized and
  there is an init method which may be used to reset the internal buffer.

Bytes.java:

  Deprecated. this class similar to ByteBuffer.java.

ChangeWatcher.java ChangeWatcher.java

  This class is a rewrite of the java.util.Observable class.
  This class implements the javaworld article that may be found in 
  http://www.javaworld.com/javaworld/jw-02-2000/jw-02-2000/jw-02.fast_p.html
  and should give better performance than the standard java.util.Observable 
  class (no clone operation and no thread synchronization are performed in
  crucial methods notification).

CharBuffer.java

  This class is similar to java.io.CharArrayWriter, but is not synchronized
  and do not copy the internal char array when calling the toCharArray method.

Charset.java

  The <code>Charset</code> class is meant to be used when you need to build
  efficiently strings from bytes, and conversly when you need to build bytes 
  from strings.

CharsetMib.java

  This class provides the iana assignment numbers for well-known iana charsets.
  see url http://www.iana.org/assignments/character-sets
  All charset are loaded from $INSTALL_DIR/resource/CharsetMib.properties

CIString.java

  Case Insensitive string. 

Coder.java

  Base64 encoder/decoder.

CommandArgs.java
  
  Class used to parse command line arguments (similar to getopt). Deprecated (use GetOpt.java).

Config.java ConfigListener.java
  
  This class extends java.util.Properties and provides convenient methods like getInt() getLong()
  etc ... You can also add properties's listener.

DateFormatter.java

  High performance date/time formatter. This class gives higher performances than 
  java.text.SimpleDateFormat

EmptyEnumeration.java
  
  Class usefull when you have to returm an emtpy Enumeration.

EmptyIterator.java
   
  Class usefull when you have to returm an emtpy Iterator.

GetOpt.java 
 
  The class <code>GetOpt</code> may be used to parse command line arguments.
  it supports flags (with no values) and valued agruments.

Hashtable.java

  A more efficient version of <code>java.util.Hashtable</code>
  It is not synchronized.  It only performs allocation when
  the hash table is resized.

IntHashtable.java

  Class similar to Hashtable.java, except that you can use int keys, instead of Object keys.

LongHashtable.java

  Class similar to Hashtable.java, except that you can use long keys, instead of Object keys.

Multipart.java

  Class used to parse http multipart content. See the main method for an example.


ObjectPool.java Recyclable.java

  A simple Object Pool implementation. Object that may be allocated 
  with this pool class must implements the <code>Recyclable</code> interface.
  Every acquired object must be released using the ObjectPool.release method.
  multiple release are not allowed. You can turn on debug mode with the static variable 
  ObjectPoo.DEBUG boolean.

QuotedStringTokenizer.java

  Quoted string version of the standard java.util.StringTokenizer.
  If delimiters are part of quoted string, they are not considered as
  separators.

Splitter.java

  General string splitter utility.

StringCaseHashtable.java

  This class implements an hashtable that stores string keys/values.
  Keys are case insensitive strings.

Utils.java

  This class regroups static misc methods.

Classes in the tracer package:
-----------------------------

 An example that shows how to use the tracer may be found in the test/TestTracer.java file.
 usage: java alcatel.tess.hometop.gateways.test.TestTracer $INSTALL_DIR/resource/Tracer.properties
