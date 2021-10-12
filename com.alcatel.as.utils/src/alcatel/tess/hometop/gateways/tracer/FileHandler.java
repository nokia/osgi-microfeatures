// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import alcatel.tess.hometop.gateways.utils.Charset;
import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

/**
 * File Handler for log events.
 *
 */
public class FileHandler implements Handler {
  
  /**
   * Creates a new <code>FileHandler</code> instance.
   *
   * @param params an <code>Object[]</code> value
   * @exception Exception if an error occurs
   */
  public FileHandler() {
  }
  
  public FileHandler(String name, String logFile) {
    this.name = name;
    this.logFile = logFile;
    this.maxSize = TracerManager.getMaxFileSize();
    this.maxZipFiles = TracerManager.getMaxZipFiles();
    this.logDir = TracerManager.getLogDir();
    this.applInstance = TracerManager.getApplInstance();
  }
  
  public FileHandler(String handlerName, String fileName, int maxFileSize, int maxZipFiles) {
    this.name = handlerName;
    this.applInstance = "";
    
    File f = new File(fileName);
    this.logFile = f.getName();
    if (f.getParentFile() != null) {
      this.logDir = f.getParentFile().getPath();
    }
    
    if (this.logDir == null) {
      this.logDir = ".";
    }
    
    this.maxSize = maxFileSize;
    this.maxZipFiles = maxZipFiles;
  }
  
  public void init(Config cnf, String applInstance, String name) throws ConfigException {
    this.applInstance = applInstance.replace(' ', '_');
    this.name = name;
    
    propertyChanged(cnf, new String[] { "tracer.logDir", "tracer.maxFileSize", "tracer.maxZipFiles",
        "tracer.handler." + name + ".logFile", });
    
    cleanZipFiles();
  }
  
  /**
   * Reloads changed properties.
   *
   * @param cnf a <code>Config</code> value
   * @param props a <code>String[]</code> value
   */
  public void propertyChanged(Config cnf, String[] props) throws ConfigException {
    for (int i = 0; i < props.length; i++) {
      if (Debug.enabled)
        Debug.p(this, "propertyChanged", "property=" + props[i]);
      
      if (props[i].equalsIgnoreCase("tracer.maxFileSize")) {
        this.maxSize = cnf.getInt("tracer.maxFileSize", Integer.MAX_VALUE);
      } else if (props[i].equalsIgnoreCase("tracer.maxZipFiles")) {
        this.maxZipFiles = cnf.getInt("tracer.maxZipFiles", 10);
        if (maxZipFiles < 2) {
          this.maxZipFiles = 2;
        }
      } else if (props[i].equalsIgnoreCase("tracer.logDir")) {
        this.logDir = cnf.getString("tracer.logDir");
      } else if (props[i].equalsIgnoreCase("tracer.handler." + name + ".logFile")) {
        this.logFile = cnf.getString("tracer.handler." + name + ".logFile", name + ".log");
      }
    }
    
    cleanZipFiles();
  }
  
  /**
   * Handles a log and redirect it to a file.
   *
   * @param le a <code>LogEvent</code> value
   * @return a <code>boolean</code> value
   * @exception IOException if an error occurs
   */
  public boolean handleLog(LogEvent le) {
    try {
      if (this.out == null) {
        reopenFile();
      }
      
      byte[] bmsg = Charset.makeBytes(le.toString());
      
      if (bmsg == null) {
        bmsg = le.toString().getBytes();
      }
      
      try {
        this.out.write(bmsg);
      }
      
      catch (IOException e) {
        close();
        open(true);
        this.out.write(bmsg);
      }
      
      if (maxSize > 0 && (size += bmsg.length) > maxSize) {
        this.out.close();
        
        // 
        // Shift up zip files.
        //
        for (int i = maxZipFiles - 2; i >= 0; i--) {
          File f = new File(this.zipdir.toString() + File.separator + this.file.getName() + '.' + i + ".gz");
          if (f.exists() == true) {
            File next = new File(this.zipdir.toString() + File.separator + this.file.getName() + '.'
                + (i + 1) + ".gz");
            next.delete();
            f.renameTo(next);
          }
        }
        
        File bak = new File(this.zipdir.toString() + File.separator + this.file.getName() + '.' + 0 + ".gz");
        
        BufferedInputStream bin = null;
        GZIPOutputStream gzip = null;
        
        try {
          bin = new BufferedInputStream(new FileInputStream(this.file.getPath().toString()));
          
          gzip = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(bak.toString())));
          
          byte buf[] = new byte[8192];
          int n = 0;
          
          while ((n = bin.read(buf)) != -1) {
            gzip.write(buf, 0, n);
          }
        }
        
        finally {
          if (gzip != null)
            gzip.close();
          
          if (bin != null)
            bin.close();
          
          close();
        }
        
        open(false);
      }
    }
    
    catch (Throwable t) {
      t.printStackTrace(TracerBox.err);
      return (false);
    }
    
    return (true);
  }
  
  /**
   * Describe <code>flush</code> method here.
   *
   * @param info an <code>int</code> value
   * @exception IOException if an error occurs
   */
  public void flush() {
    try {
      if (this.file == null || out == null) {
        return;
      }
      
      if (Debug.enabled)
        Debug.p(this, "flush", "flushing file " + this.file);
      
      if (this.file.exists() == false) {
        close();
        open(true);
      }
      
      this.out.flush();
      this.fout.getFD().sync();
    }
    
    catch (Throwable t) {
      t.printStackTrace(TracerBox.err);
    }
  }
  
  /**
   * Describe <code>flush</code> method here.
   *
   * @param info an <code>int</code> value
   * @exception IOException if an error occurs
   */
  public void clear() {
    try {
      if (this.file == null) {
        return;
      }
      
      close();
      open(false);
    }
    
    catch (Throwable t) {
      t.printStackTrace(TracerBox.err);
    }
  }
  
  /**
   * Describe <code>open</code> method here.
   *
   * @param append a <code>boolean</code> value
   * @exception IOException if an error occurs
   */
  private void open(boolean append) throws IOException {
    if (this.out != null) {
      this.out.close();
    }
    
    this.fout = new FileOutputStream(file.getPath(), append);
    this.out = new BufferedOutputStream(fout, 4096);
    this.size = this.file.length();
  }
  
  /**
   * Describe <code>close</code> method here.
   *
   */
  public void close() {
    try {
      if (out != null) {
        this.out.close();
        out = null;
      }
    } catch (IOException e) {
    }
  }
  
  /**
   * Describe <code>toString</code> method here.
   *
   * @return a <code>String</code> value
   */
  public String toString() {
    return ("[FileHandler: " + "file=" + file.getPath() + ", " + "maxSize=" + maxSize + "]");
  }
  
  public String getName() {
    return (this.name);
  }
  
  public File getRelativePath() {
    return (new File(this.logFile));
  }
  
  public static void clearAllFiles(String logDir, String instance) {
    if (logDir == null || instance == null)
      return;
    
    logDir = logDir + File.separatorChar + instance;
    logDir = logDir.replace('/', File.separatorChar);
    File dir = new File(logDir);
    
    if (dir.exists() == false) {
      return;
    }
    
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].isFile()) {
        files[i].delete();
      }
    }
    
    File zipdir = new File(logDir + File.separatorChar + "zip");
    if (zipdir.exists() == false) {
      return;
    }
    
    files = zipdir.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].exists()) {
        files[i].delete();
      }
    }
  }
  
  /**
   * Describe <code>reopenFile</code> method here.
   *
   * @param cnf a <code>Config</code> value
   * @exception ConfigException if an error occurs
   */
  private void reopenFile() throws ConfigException, IOException {
    close();
    
    String logDir = this.logDir + "/" + applInstance;
    logDir = logDir.replace('/', File.separatorChar);
    File logDirF = new File(logDir);
    
    if (logDirF.exists() == false) {
      logDirF.mkdirs();
      
      if (logDirF.exists() == false) {
        throw new ConfigException("failed to create directory " + logDir);
      }
    }
    
    this.file = new File(logDir + File.separatorChar + logFile);
    
    File parent = file.getParentFile();
    if (parent.exists() == false && parent.mkdirs() == false) {
      throw new ConfigException("could not create or access to file " + logFile);
    }
    
    this.zipdir = new File(parent.getPath() + File.separatorChar + "zip");
    
    if (this.file.exists() == true && !this.file.canWrite()) {
      throw new ConfigException("could not create or access to file " + this.file.getPath());
    }
    
    if (zipdir.exists() == false) {
      zipdir.mkdirs();
      if (zipdir.exists() == false || !zipdir.canWrite()) {
        throw new ConfigException("could not create or access directory " + zipdir.getPath());
      }
    }
    
    open(true);
  }
  
  /**
   * Removes zip files greater than maxZipFiles.
   */
  private void cleanZipFiles() {
    if (Debug.enabled)
      Debug.p(this, "cleanZipFiles", "maxZipFiles=" + maxZipFiles + ", logDir=" + logDir);
    
    if (this.maxZipFiles > 0 && this.logDir != null && this.applInstance != null) {
      File zdir = new File(this.logDir + File.separatorChar + this.applInstance + File.separatorChar + "zip");
      
      if (zdir.exists() == false) {
        return;
      }
      
      File[] files = zdir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          if (Debug.enabled)
            Debug.p(this, "accept", "dir=" + dir + ", name=" + name);
          
          if (name.length() >= 4 && name.endsWith(".gz")) {
            int dot = name.lastIndexOf(".", name.length() - 4);
            
            if (Debug.enabled)
              Debug.p(this, "accept", "dot=" + dot);
            
            if (dot == -1) {
              return (false);
            }
            
            String s = name.substring(dot + 1, name.length() - 3);
            
            try {
              int n = Integer.parseInt(s);
              
              if (Debug.enabled)
                Debug.p(this, "accept", "n=" + n);
              
              if (n > maxZipFiles - 1) {
                return (true);
              }
            }
            
            catch (NumberFormatException e) {
              if (Debug.enabled)
                Debug.p(this, "accept", "exception=" + e.toString());
              
              return (false);
            }
          }
          
          return (false);
        }
      });
      
      for (int i = 0; i < files.length; i++) {
        files[i].delete();
      }
    }
  }
  
  private String applInstance;
  private String name;
  private String logDir;
  private String logFile;
  private FileOutputStream fout;
  private BufferedOutputStream out;
  private File file;
  private File zipdir;
  private long size;
  private long maxSize;
  private int maxZipFiles;
}
