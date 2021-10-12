// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This is a File log4j file Appender. This appender do the following:
 * <p>
 * <ul>
 * <li>It zip files if max file size is exceeded.
 * <li>It syncs logs with level ERR/WARN, and displays them to log4j.
 * <li>It calls LogManager.shutdown when jvm exits.
 * <li>It halts the jvm if the "java.lang.OutOfMemoryError" string is logged.
 * </ul>
 */
public class Log4jAppender extends AppenderSkeleton
{
	  /**
	   * represents the root directory of the ASR installation
	   */
	  final static String INSTALL_DIR = "INSTALL_DIR";  

	  /**
	   * part of the system configuration:
	   * the "group" represents a high level application unit comprised of IO handlers and scalable processing nodes. 
	   * the "group" is also the logical scope boundary for distributed sessions.
	   * processing agents and IO handlers only connect within their group.
	   * a group may spawn accross several hardware nodes. the same hardware nodes may be used by several groups.
	   */
	  final static String GROUP_NAME = "group.name";

	  /**
	   * part of the system configuration:
	   * the "component" represents a type of process which may be identically instantiated several times for scalability.
	   */
	  final static String COMPONENT_NAME = "component.name";

	  /**
	   * part of the system configuration:
	   * a numerical identifier for the "component"
	   */
	  final static String COMPONENT_ID = "component.id";

	  /**
	   * part of the system configuration:
	   * it represents the current "instance" of the parent component.
	   */
	  final static String INSTANCE_NAME = "instance.name";

	  /**
	   * part of the system configuration:
	   * a numerical identifier for the "instance"
	   */
	  final static String INSTANCE_ID = "instance.id";

	  /**
	   * part of the system configuration:
	   * the process id assigned by the OS. 
	   */
	  final static String INSTANCE_PID = "instance.pid";

	  /**
	   * part of the system configuration:
	   * the local host name
	   */
	  final static String HOST_NAME = "host.name";

	  /**
	   * part of the system configuration:
	   * the "platform" represents a high level application encompassing one or more groups
	   */
	  final static String PLATFORM_NAME = "platform.name";

	
  /**
   * Constructor.
   */
  public Log4jAppender()
  {
  }
  
  public static void setDefaultFilePath(String dir)
  {
    // no need to to this anymore, we initialize the file path from our static initializer.
  }

  /**
   * Set debug mode
   */
  public void setDebug(boolean debug)
  {
    _debug = debug;
  }

  /**
   * Set the log file.
   * 
   * @param file The file name. If the file does not contain any path, then the default path is
   *          used (the default path may be initialized using the static setDefaultFilePath()
   *          method.
   */
  public void setFile(String file)
  {
    String installDir = System.getenv("INSTALL_DIR");
    if (installDir != null)
    {
      file = file.replaceFirst("\\{INSTALL_DIR\\}", installDir);
    }

    _file = new File(file);
    if (_file.getParentFile() == null)
    {
      _file =
          new File(Log4jAppender._defaultFilePath.getPath() + File.separator + _file.getName());
    }

    File parentDir = _file.getParentFile();
    if (parentDir != null)
    {
      parentDir.mkdirs();
      if (!parentDir.exists())
      {
        stdout("Can not access dir " + parentDir);
        return;
      }
      _zipDir = new File(parentDir.toString() + File.separator + "zip");
    }
    else
    {
      _zipDir = new File(".");
    }
  }

  /**
   * Set the max log file size.
   * 
   * @param maxFileSize The max log file size (0=unlimited)
   */
  public void setMaxFileSize(long maxFileSize)
  {
    _maxSize = maxFileSize;
  }

  /**
   * Set the max number of zipped files.
   * 
   * @param maxZipFiles The max number of zipped files.
   */
  public void setMaxZipFiles(int maxZipFiles)
  {
    _maxZipFiles = maxZipFiles;
  }

  /**
   * Configure this logger in order to exit when an "OutOfMemory exception" is logged.
   */
  public void setCheckOutOfMemory(boolean checkOutOfMemory)
  {
    _checkOutOfMemory = checkOutOfMemory;
  }

  /**
   * Configure the file appender buffer size. 0 means we autoflush (default value), else we
   * never flush on each logs
   */
  public void setBufferSize(int bufsize)
  {
    _bufsize = bufsize;
  }

  /**
   * Sync logs on ERROR log level.
   * 
   * @param syncOnError
   */
  public void setSyncOnError(boolean syncOnError)
  {
    _syncOnError = syncOnError;
  }
  
  /**
   * Configure a header, which has to be inserted in the first line of each new log files.
   */
  public void setHeader(String header) 
  {
    _header = header;
  }
  
  public void activateOptions()
  {
    try
    {
      openFile(true);
    }

    catch (IOException e)
    {
      stdout("Can not open file " + _file, e);
    }

    this._shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(_shutdownHook);

    if (_bufsize > 0)
    { // We don't autoflush, and the flusher thread will flush every N
      // seconds.
      _autoFlush = false;
      _flusherThread = new FlusherThread();
      _flusherThread.start();
    }
    else
    { // We'll autoflush each logs
      _autoFlush = true;
    }
  }

  // -----------------------------------------------------------------------------------------
  // AppenderSkeleton methods
  // -----------------------------------------------------------------------------------------

  /**
   * Appends a logging event. This method is invoked by the AppenderSkeleton parent class and is
   * already synchronized.
   */
  public void append(LoggingEvent event)
  {
    Throwable throwable = event.getThrowableInformation() != null ? event.getThrowableInformation().getThrowable() : null;

    if (throwable instanceof OutOfMemoryError) {
      _lastResort = null;
    }
    
    boolean sync = (_syncOnError) ? event.getLevel().isGreaterOrEqual(Level.ERROR) : false;
    StringBuilder sb = new StringBuilder();
    String msg = null;
    sb.append(this.layout.format(event));

    try
    {
      if (layout.ignoresThrowable() && event.getThrowableInformation() != null)
      {
        sb.append(LINE_SEPARATOR);
        String[] tab = event.getThrowableInformation().getThrowableStrRep();
        for (int i = 0; i < tab.length; i++)
        {
          sb.append(tab[i]);
          sb.append("\n");
        }
        sb.append("\n");
      }

      msg = sb.toString();
      doAppend(msg);
      if (_autoFlush)
      {
        flush(sync);
      }
    }

    catch (Throwable t)
    {
      stdout("Could not append log: " + msg, t);
      if (t instanceof OutOfMemoryError) {
	throwable = t;
      }
    }

    finally
    {
      if (_checkOutOfMemory && throwable instanceof OutOfMemoryError)
      {
        if (!sync)
        {
          flush(true);
        }

        Runtime.getRuntime().halt(1);
      }
    }
  }

  public boolean requiresLayout()
  {
    return true;
  }

  public void close()
  {
    synchronized (this)
    {
      if (_shuttingDown && _shutdownHook != null)
      {
        return; // the jvm is shutting down and we don't want to loose any log events.
      }
    }

    try
    {
      Runtime.getRuntime().removeShutdownHook(_shutdownHook);
    }
    catch (IllegalStateException e)
    {
      // the jvm is shutting down ...
    }

    // close the appender.
    if (_flusherThread != null)
    {
      _flusherThread.interrupt();
    }
    closeFile();
  }

  // ------------------------- Private methods ----------------------------------------

  protected static String parse(Throwable e)
  {
    StringWriter buffer = new StringWriter();
    PrintWriter pw = new PrintWriter(buffer);
    e.printStackTrace(pw);
    return (buffer.toString());
  }

  protected void closeFile()
  {
    try
    {
      if (_out != null)
      {
        _out.close();
      }
    }

    catch (IOException e)
    {
    }

    finally
    {
      _out = null;
    }
  }

  /**
   * Appends a string to our logfile. This method is invoked by the append method, which is
   * already synchronized.
   * 
   * @param msg the string to log
   * @throws Exception on any errors.
   */
  protected void doAppend(String msg) throws Exception
  {
    checkOpened();
    byte[] bmsg = msg.getBytes();
    try
    {
      _out.write(bmsg);
    }

    catch (IOException e)
    {
      closeFile();
      openFile(true);
      _out.write(bmsg);
    }

    if ((_maxSize > 0 && (_size += bmsg.length) > _maxSize))
    {
      // We must zip because either the max log file size has been reached or our daily
      // rotation date has been reached.

      if (_zipperThread != null || _shuttingDown)
      {
        // The zipping thread is currently running, or we are shutting down: don't zip for
        // now.
        return;
      }
      
      debug("starting rotation for %s, _size=%d, maxSize=%d", _file, _size, _maxSize);

      // Create our zipper thread. Our shutdown hook will join it, if we are currently
      // exiting.
      _zipperThread = new ZipperThread();
      _zipperThread.start();
    }
  }

  protected void stdout(String msg)
  {
    stdout(msg, null);
  }

  protected void stdout(String msg, Throwable t)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
    LogLog.warn("[" + sdf.format(new Date()) + "/" + Thread.currentThread().getName() + "/"
        + _file.getName() + "] " + msg, t);
  }

  protected void debug(String format, Object ... args)
  {
    if (_debug) {
      String msg = String.format(format, args);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");
      LogLog.warn("[" + sdf.format(new Date()) + "/" + Thread.currentThread().getName() + "/"
        + _file.getName() + "] " + msg);
    }
  }

  protected void checkOpened() throws IOException
  {
    if (!_file.exists() || _out == null)
    {
      openFile(true);
    }
  }

  protected void openFile(boolean append) throws IOException
  {
    closeFile();
    boolean addHeader = false;
    
    if (! append || _file.exists() == false) {
      addHeader = true;
    }

    File dir = _file.getParentFile();
    if (dir != null)
    {
      dir.mkdirs();
      if (!dir.exists())
      {
        throw new IOException("Can not open dir " + dir.getPath());
      }
    }

    _fout = new FileOutputStream(_file, append);
    if (_bufsize == 0)
    {
      _out = new BufferedOutputStream(_fout, 4096);
    }
    else
    {
      _out = new BufferedOutputStream(_fout, _bufsize);
    }
    
    if (addHeader && _header != null) {
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(_out));
      writer.println(_header); 
      writer.flush();
    }
    _size = _file.length();
  }

  protected synchronized void flush(boolean sync)
  {
    try
    {
      if (_file == null || _out == null)
      {
        return;
      }

      if (_file.exists() == false)
      {
        closeFile();
        openFile(true);
      }

      _out.flush();
      if (sync)
      {
        _fout.getFD().sync();
      }
    }

    catch (Exception t)
    {
      stdout("Could not flush logs", t);
    }
  }

  // ------------------------ Private attributes ------------------------------------

  private static byte[] _lastResort = new byte[256*1024];
  protected File _file, _zipDir;
  protected long _maxSize;
  protected int _maxZipFiles;
  protected FileOutputStream _fout;
  protected BufferedOutputStream _out;
  protected long _size;
  protected int _bufsize; // if 0, we autoflush
  protected boolean _autoFlush = true;
  protected boolean _checkOutOfMemory;
  protected final static String OUT_OF_MEMORY_ERROR = "java.lang.OutOfMemoryError";
  protected boolean _syncOnError;
  protected boolean _shuttingDown;
  protected Thread _shutdownHook;
  protected boolean _debug = false;
  protected String _header;

  private FlusherThread _flusherThread;
  private ZipperThread _zipperThread;

  protected final static String LINE_SEPARATOR = System.getProperty("line.separator");

  protected static File _defaultFilePath = new File(System.getProperty(INSTALL_DIR) + "/var/log/" + getGroupInstName()) ;

  public static String getGroupInstName() {    	
      String grpInstName = null;
      if (System.getProperty(GROUP_NAME) == null)
      {
          grpInstName = System.getProperty("platform.agent.instanceName");// deprecated
      }
      else
      {
          String group = System.getProperty(PLATFORM_NAME);
          group = group == null ? System.getProperty(GROUP_NAME) : group + "." + System.getProperty(GROUP_NAME);
          String instance = System.getProperty(COMPONENT_NAME);
          instance = instance == null ? System.getProperty(INSTANCE_NAME) : instance + "." + System.getProperty(INSTANCE_NAME);
          grpInstName = group + "__" + instance;
      }
      return grpInstName;
  }

  // ------------------------ Inner classes ----------------------------------------

  // Only active if buffsize > 0
  private class ShutdownHook extends Thread
  {
    public void run()
    {
      try
      {
        // Ensure all buffered logs are flushed.
        flush(false /* don't sync */);

        // Wait for the zipper thread end:
        Thread zipperThread;
        synchronized (Log4jAppender.this)
        {
          zipperThread = _zipperThread;
          _shuttingDown = true; // avoid zipping while we are shutting down ...
        }
        if (zipperThread != null)
        {
          zipperThread.join();
        }
      }
      catch (Throwable t)
      {
        stdout("Got unexpected exception while shutting down log4j appender on file: " + _file,
               t);
      }
    }
  }

  /**
   * Thread used to flush logs periodically. Only active if buffsize > 0
   */
  class FlusherThread extends Thread
  {
    FlusherThread()
    {
      super("Log4jFlusherThread");
      setDaemon(true);
    }

    public void run()
    {
      try
      {
        while (true)
        {
          sleep(5000);
          flush(false /* don't sync */);
        }
      }
      catch (InterruptedException e)
      {
      }
      catch (Throwable t)
      {
        stdout("Flusher thread caught unexpected exception", t);
        return;
      }
    }
  }

  /**
   * This task is in charge of ziping a source File asynchronously.
   */
  class ZipperThread extends Thread
  {
    /**
     * Makes a new ZipTask.
     * 
     * @param source the source file to be compressed.
     */
    ZipperThread()
    {
      super("Log4jZipperThread");
      super.setDaemon(true);
    }

    File getZipFile()
    {
      return new File(_zipDir.toString() + File.separator + _file.getName() + ".0.gz");
    }

    void setupZipFile(File zipFile)
    {
      // We are about to zip zip/msg.log.tmp into zip/msg.log.0.gz
      // If zip/msg.log.0.gz already exists: we have to shift up our already zipped file.
      if (zipFile.exists())
      {
        shiftUpZipFiles();
      }
    }

    String getRotatePolicy()
    {
      return "maxsizeOnly";
    }

    void rotated()
    {
    }

    @Override
    public void run()
    {
      BufferedInputStream tmpIn = null;
      GZIPOutputStream tmpgzOut = null;
      File tmp = new File(_zipDir.toString() + File.separator + _file.getName() + ".tmp");
      File tmpgz = new File(_zipDir.toString() + File.separator + _file.getName() + ".tmp.gz");
      final File destgz = getZipFile();

      try
      {
        // Check if the zip dir must be created.
        if (_zipDir.exists() == false)
        {
          _zipDir.mkdirs();
        }

        // If the source to be compressed already exists, it means that we have crashed during
        // the last time we were zipping. In this case, we'll rezip the file once again.
        if (!tmp.exists())
        {
          synchronized (Log4jAppender.this)
          {
            // close/flush the current logfile.
            closeFile();

            // Move the current msg.log file into the source file.
            _file.renameTo(tmp);
            if (tmp.exists() == false)
            {
              throw new IOException("Could not rename " + _file + " to " + tmp);
            }

            // Re-open logfile for next logging event
            openFile(false /* don't append */);
          }
        }

        setupZipFile(destgz);
        debug("Compressing %s into %s", _file, destgz);


        // Initialize output files: we'll zip tmp into tmpgz file.
        tmpIn = new BufferedInputStream(new FileInputStream(tmp));
        tmpgzOut = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tmpgz)));

        byte buf[] = new byte[8192];
        int n = 0;

        while ((n = tmpIn.read(buf)) != -1)
        {
          tmpgzOut.write(buf, 0, n);
        }

        tmpgzOut.close();
        tmpIn.close();

        // All done: move dest into zip/msg.log.0.gz
        destgz.delete();
        tmpgz.renameTo(destgz);

        rotated();
      }

      catch (Throwable t)
      {
        stdout("Error while zipping file " + tmp, t);
      }

      finally
      {
        tmp.delete();
        if (tmpgzOut != null)
        {
          try
          {
            tmpgzOut.close();
          }
          catch (IOException e)
          {
          }
        }

        if (tmpIn != null)
        {
          try
          {
            tmpIn.close();
          }
          catch (IOException e)
          {
          }
        }

        synchronized (Log4jAppender.this)
        {
          _zipperThread = null;
        }
      }
    }

    /**
     * We have to shift up our already zipped file, because we'll zip the source into
     * zip/msg.log.0.gz. But we take care of any eventual previous crash. First we calculate the
     * last index of the file to be shift up. The first index is normally "_maxZipFiles - 2".
     * But, because we may have crashed during a last jvm run while shifting up, we must
     * calculate the index boolean previousCrashDetected = false;
     * 
     * @return the last index at which we'll start to shift up zip files.
     */
    private void shiftUpZipFiles()
    {
      // First, lets perform house keeping: check if some old zip files exists with a number
      // which exceeds our _maxZipFiles configuration
      cleanOldZipFiles();

      int index = calculateLastIndexToShiftUp();

      // Now: shift up all files, starting at the index.
      for (; index >= 0; index--)
      {
        File curr =
            new File(_zipDir.toString() + File.separator + _file.getName() + '.' + index + ".gz");
        if (curr.exists() == true)
        {
          File next =
              new File(_zipDir.toString() + File.separator + _file.getName() + '.' + (index + 1)
                  + ".gz");
          next.delete();
          curr.renameTo(next);
        }
      }
    }

    private int calculateLastIndexToShiftUp()
    {
      // First, get the last index which corresponds to an existing zip files.
      int index = _maxZipFiles - 2;

      // The following code is meant to detect eventual previous crash, but it's a bit tricky
      // and I prefer to activate it in the 4.0
      if (false /* I will activate this code in the next 4.0 release */)
      {
        for (; index >= 0; index--)
        {
          File file =
              new File(_zipDir.toString() + File.separator + _file.getName() + '.' + index
                  + ".gz");
          if (file.exists())
          {
            break;
          }
        }

        // Detect eventual previous crash
        boolean previousCrashDetected = false;
        for (; index >= 0; index--)
        {
          File file =
              new File(_zipDir.toString() + File.separator + _file.getName() + '.' + index
                  + ".gz");
          if (!file.exists())
          {
            if (index > 0)
            {
              index--;
            }
            previousCrashDetected = true;
            break;
          }
        }
        if (!previousCrashDetected)
        {
          index = _maxZipFiles - 2;
        }
      }

      return index;
    }

    private void cleanOldZipFiles()
    {
      if (_zipDir.exists() && _maxZipFiles > 0)
      {
        File[] files = _zipDir.listFiles(new FilenameFilter()
        {
          public boolean accept(File dir, String name)
          {
            if (!name.startsWith(_file.getName()) || !name.endsWith(".gz"))
            {
              return false;
            }
            if (name.length() >= 4 && name.endsWith(".gz"))
            {
              int dot = name.lastIndexOf(".", name.length() - 4);
              if (dot == -1)
              {
                return (false);
              }
              String s = name.substring(dot + 1, name.length() - 3);

              try
              {
                int n = Integer.parseInt(s);
                if (n > _maxZipFiles - 1)
                {
                  return (true);
                }
              }

              catch (NumberFormatException e)
              {
                return (false);
              }
            }

            return (false);
          }
        });

        for (int i = 0; files != null && i < files.length; i++)
        {
          files[i].delete();
        }
      }
    }
  }
}
