// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

import org.apache.log4j.Logger;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

abstract public class KeyStoreWatch {

    static final String ksw = "KeyStoreWatch";

    protected Thread _ksWatchThread;

    protected Logger _logger;

    protected String _filename;
    protected String _last_known_hash = "";

    abstract protected void notify_updated();

    abstract protected void notify_stopped();

    public KeyStoreWatch() {
        this._logger = Logger.getLogger(ksw);
    }

    public KeyStoreWatch(Logger logger) {
        this._logger = Logger.getLogger(logger.getName()+"."+ksw);
    }

    public void startKeyStoreWatch (String filename) {
        Objects.requireNonNull(filename);
        try {
            File f = new File(filename);
            if (!f.exists() || f.isDirectory()) {
                throw new FileNotFoundException(filename+" is not a file");
            }
        } catch (FileNotFoundException fnfe) {
            throw new IllegalArgumentException("Bad configuration with "+filename,fnfe);
        }
        this._filename = filename;
        this._last_known_hash = hashfile(filename);

        Runnable r = new Runnable (){
            public void run (){
                if (_logger.isInfoEnabled ())
                    _logger.info (toString()+" : "+ksw+" started");
                try{
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    Path path = Paths.get(filename).toAbsolutePath();
                    path
                            .getParent()
                            .register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY
                            );
                    whileloop : while (true){
                        WatchKey key = watchService.poll (3000, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (key == null) continue whileloop;
                        forloop : for (WatchEvent<?> event : key.pollEvents ()) {
                            Object o = event.context ();
                            if (o == null) continue forloop;
                            Path p = (Path) o;
                            if (!path.getFileName ().equals (p.getFileName ()))
                                continue forloop;
                            WatchEvent.Kind kind = event.kind ();
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE){
                                _logger.warn (toString()+" "+ksw+" : Event : "+path+" : created");
                                check_and_notify();
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE){
                                // we assume this is prior to a create : do NOT change the credentials for now
                                _logger.warn (toString()+" "+ksw+" : Event : "+path+" : deleted");
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY){
                                _logger.warn (toString()+" "+ksw+" : Event : "+path+" : modified");
                                check_and_notify();
                            }
                        }
                        key.reset();
                    }
                } catch(InterruptedException ie) {
                    _logger.info (toString()+" : "+ksw+" stopped");
                } catch(Exception e) {
                    _logger.error (toString()+" : "+ksw+" exception / stopped", e);
                    notify_stopped();
                }
            }
        };
        _ksWatchThread = new Thread (null, r, this+"["+ksw+"]");
        _ksWatchThread.setDaemon(true);
        _ksWatchThread.start ();
    }

    protected void stopKeyStoreWatch () {
        if (_ksWatchThread != null){
            _ksWatchThread.interrupt ();
            _ksWatchThread = null;
        }
    }

    protected String hashfile(String filename) {
        MessageDigest md;
        InputStream is;
        BufferedInputStream bis;
        DigestInputStream dis;
        try {
//            md = MessageDigest.getInstance("SHA-1");
            md = MessageDigest.getInstance("MD5");
            is = Files.newInputStream(Paths.get(filename));
            bis = new BufferedInputStream(is);
            dis = new DigestInputStream(bis, md);

            while (dis.read() != -1) ;

            byte[] digest = md.digest();
            dis.close();
            bis.close();
            is.close();
            return new BigInteger(digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void check_and_notify() {
        String new_hash = hashfile(_filename);
        if (new_hash.equalsIgnoreCase(_last_known_hash)) {
            _logger.warn (toString()+" "+ksw+" : content not modified : "+_filename);
        } else {
            _last_known_hash = new_hash;
            notify_updated();
        }
    }

    @Override
    public String toString() {
        return "KeyStoreWatch{" +
                "filename='" + _filename + '\'' +
                ", last_known_hash='" + _last_known_hash + '\'' +
                '}';
    }
}
