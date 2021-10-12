// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

public class Launcher {
  String _dir;
  Vector _apps = new Vector();
  URLClassLoader _commonClassLoader;
  private long _lastCommonModified;
  
  public static void main(String args[]) throws Exception {
    Launcher l = new Launcher(args[0]);
    l.launch();
    
    while (true) {
      Thread.sleep(1000);
      l.reload();
    }
  }
  
  public Launcher(String dir) throws Exception {
    _dir = dir;
    File propFile = new File(dir + File.separator + "launcher.properties");
    Parameters params = new Parameters();
    params.load(propFile, true);
    
    String apps = (String) params.get("launcher.apps");
    StringTokenizer tok = new StringTokenizer(apps, " ,");
    while (tok.hasMoreTokens()) {
      App app = new App(tok.nextToken(), params);
      _apps.add(app);
    }
  }
  
  public void launch() throws Exception {
    System.out.println("launching all apps");
    File commonDir = new File(_dir + File.separator + "/lib");
    _lastCommonModified = commonDir.lastModified();
    URL[] urls = getURLJars(commonDir);
    _commonClassLoader = new URLClassLoader(urls);
    Enumeration e = _apps.elements();
    while (e.hasMoreElements()) {
      App app = (App) e.nextElement();
      app.launch();
    }
  }
  
  public void reload() throws Exception {
    if (new File(_dir + File.separator + "/lib").lastModified() > _lastCommonModified) {
      stop();
      launch();
      return;
    }
    
    Enumeration e = _apps.elements();
    while (e.hasMoreElements()) {
      App app = (App) e.nextElement();
      app.reload();
    }
  }
  
  public void stop() {
    Enumeration e = _apps.elements();
    while (e.hasMoreElements()) {
      App app = (App) e.nextElement();
      app.stop();
    }
  }
  
  private URL[] getURLJars(File dir) throws Exception {
    String[] jars = dir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }
    });
    
    URL[] urls = new URL[jars.length + 1];
    for (int i = 0; i < jars.length; i++) {
      urls[i] = new URL("file:" + dir + File.separator + jars[i]);
    }
    urls[urls.length - 1] = new URL("file:" + dir + File.separator);
    return urls;
  }
  
  private class App {
    String _name;
    File _appdir;
    String _class;
    String[] _args;
    ThreadGroup _appThreadGroup;
    URLClassLoader _appClassLoader;
    long _lastAppModified;
    
    App(String name, Parameters params) {
      _name = name;
      _appdir = new File(_dir + "/" + (String) params.get("launcher." + name + ".dir"));
      String clazz = (String) params.get("launcher." + name + ".class");
      QuotedStringTokenizer tok = new QuotedStringTokenizer(clazz, " ");
      _class = tok.nextToken();
      Vector args = new Vector();
      while (tok.hasMoreTokens()) {
        args.add(tok.nextToken());
      }
      _args = (String[]) args.toArray(new String[0]);
    }
    
    public void stop() {
      if (_appThreadGroup != null) {
        _appThreadGroup.interrupt();
      }
    }
    
    public void launch() throws Exception {
      System.out.println("launching app " + _name);
      _lastAppModified = _appdir.lastModified();
      
      URL[] urls = getURLJars(_appdir);
      _appClassLoader = new URLClassLoader(urls, _commonClassLoader);
      Class clazz = _appClassLoader.loadClass(_class);
      
      //final Object app = clazz.newInstance();
      final Method main = clazz.getMethod("main", String[].class);
      Thread.currentThread().setContextClassLoader(_appClassLoader);
      _appThreadGroup = new ThreadGroup(_name);
      Thread appThread = new Thread(_appThreadGroup, new Runnable() {
        public void run() {
          try {
            main.invoke(null, new Object[] { _args });
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      });
      appThread.start();
    }
    
    public void reload() throws Exception {
      if (_appdir.lastModified() > _lastAppModified) {
        stop();
        launch();
      }
    }
  }
}
