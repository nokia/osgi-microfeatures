// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.smartkey.impl;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.session.distributed.smartkey.SmartKey;
import com.alcatel.as.session.distributed.smartkey.SmartKeyService;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;

@Component(service=SmartKeyService.class)
public class SmartKeyServiceImpl implements SmartKeyService {

  private String platformId;
  private PlatformExecutors executors;
  private String base;
  private final static Pattern patternSmartKey = Pattern.compile("-");
  private AtomicInteger uniqueCounter = new AtomicInteger();

  

  public SmartKeyServiceImpl() {
    base = "";
  }

  @Reference(target="(service.pid=system)")
  public void setSystemConfig(final Dictionary<String, String> config) {
    this.platformId = ConfigHelper.getString(config, ConfigConstants.PLATFORM_ID);
    String groupId = ConfigHelper.getString(config, ConfigConstants.GROUP_ID);
    base = this.platformId + "-" + groupId + "-";
  }

  @Reference
  public void setPlatformExecutors(PlatformExecutors executors) {
    this.executors = executors;
  }

  @Override
  public String createSmartKey(long... ids) {
    return createSmartKeyFor(base, null, ids);
  }

  @Override
  public String createSmartKey(String userPart, long... ids) {
    return createSmartKeyFor(base, userPart, ids);
  }
  
  @Override
  public long[] getAgentIds(String key) {
    if (key.startsWith(base)) {
      key = key.substring(base.length());
      try {
        int i = 0;
        while (key.charAt(i++) != '-');
        if (i<2) return null;
        int level = Integer.parseInt(key.substring(0,i-1));
        long[] result = new long[level];
        key = key.substring(i);
        for(i = 0; i < level; i++) {
          int k = 0;
          while (key.charAt(k++) != '-');
          result[i] = Long.parseLong(key.substring(0,k-1));
          key = key.substring(k);
        }
        return result;
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }  
  
  @Override
  public boolean isSmartKey(String key) {
    if (key != null) { 
      return key.startsWith(base); 
    } 
    return false; 
  }
    
  @Override
  public String getUserPart(String key) {
    if (!isSmartKey(key)) return null;
    try {
      String part = key.substring(base.length());
      int i = 0;
      while (part.charAt(i++) != '-');
      if (i<2) return null;
      int level = Integer.parseInt(part.substring(0,i-1));
      part = part.substring(i);
      for(i = 0; i <= level+1 ; i++) {
        int k = -1;
        for (k = 0; k < part.length(); k++) {
          if (part.charAt(k) == '-') break;
        }
        if (k == part.length()) return null;
        part = part.substring(++k);
      }
      return part;
    } catch (NumberFormatException e) {
      return null;
    }   
  }
  
  @Override
  public String[] getIdsFromSmartKey(String value) {
    if (value == null) return null;
    int pfidPos = value.indexOf(platformId, 0);
    if (pfidPos < 0) return null;
    // String contains "platformId"
    int pos = pfidPos + platformId.length();
    if (value.length() < pos+8) return null; // too short 
    if (value.charAt(pos) != '-') return null; // wrong separator
    // String contains "platformId-......."
    String[] items = patternSmartKey.split(value.substring(pfidPos), 10); // 5+n : pfid-group-n- ... -containerId-Uid
    if (items.length < 5) return null; // not enough IDs
    try {
      // Number of agents
      int nb = Integer.parseInt(items[2]);
      if (items.length < nb+3) return null;
      String[] ids = new String[nb+3];
      int k = 0;
      // Platform Id
      ids[k++] = items[0];
      // Group Id
      Long.parseLong(items[1], 10);
      ids[k++] = items[1];
      // Container Id
      Long.parseLong(items[nb+3], 10);
      ids[k++] = items[nb+3];
      // Agent IDs
      for (int i = 0; i < nb; i++) {
        Long.parseLong(items[i+3], 10);
        ids[k++] = items[i+3];
      }
      return ids;
    }
    catch (NumberFormatException e) {
      return null;
    }
  }
    
  @Override
  public SmartKey getSmartKey(String value) {
    return getSmartKey(value, this.platformId);
  }
   
  @Override
  public SmartKey getSmartKey(String value, String platformId) {
    if (value == null) return null;
    int pfidPos = value.indexOf(platformId, 0);
    if (pfidPos < 0) return null;
    // String contains "platformId"
    int pos = pfidPos + platformId.length();
    if (value.length() < pos+8) return null; // too short 
    if (value.charAt(pos) != '-') return null; // wrong separator
    // String contains "platformId-......."
    String[] items = patternSmartKey.split(value.substring(pfidPos), 10); // 5+n : pfid-group-n- ... -containerId-Uid
    if (items.length < 5) return null; // not enough IDs
    if (!items[0].equals(platformId)) return null;
    try {
      // Number of agents
      int nb = Integer.parseInt(items[2]);
      long[] ids = new long[nb];
      int nbItems = nb+5;
      if (items.length < nbItems) return null;
      int k = 0;
      // Agent IDs
      for (int i = 0; i < nb; i++) {
        ids[k++] = Long.parseLong(items[i+3], 10);
      }
      // Container & Unique IDs
      int containerId = Integer.parseInt(items[nb+3], 10);
      int uid = Integer.parseInt(items[nb+4], 10);
      // User part ?
      String sessionId;
      if (items.length >nbItems) {
        int p = pfidPos;
        for (int i = 0; i < nbItems; i++) {
          p = value.indexOf('-', p + 1);
        }
        String remaining = value.substring(p+1);
        int end = p + remaining.length() + 1;
        for (int i = 0; i < remaining.length(); i++) {
          char x = remaining.charAt(i);
          if (Character.isLetterOrDigit(x) || '-' == x) continue;
          end = p + i + 1;
          break;
        }
        sessionId = value.substring(pfidPos, end);
      }
      else {
        sessionId = value.substring(pfidPos);
      }
      return new SmartKeyImpl(sessionId, containerId, uid, ids);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }  
  
  @Override
  public String getGroupId(String name) {
    if (name == null) throw new IllegalArgumentException("null group name");
    CRC32 crc = new CRC32();
    crc.reset();
    crc.update(name.getBytes());
    return Long.toString(crc.getValue());
  }

  /*----------------------------------------------------------------------------------------*/

  public String createSmartKeyFor(String base, String userPart, long... ids) {
    int uid = 0;
    while(uid == 0) {
      uid = uniqueCounter.incrementAndGet() & 0x7FFFFFFF;
    }
    StringBuilder key = new StringBuilder(100);
    key.append(base).append(ids.length);
    for (int i = 0; i < ids.length; i++) {
      key.append('-');
      key.append(ids[i]);
    }
    key.append("-").append(getContainerIndex()).append("-").append(uid);
    if (userPart != null) {
      key.append('-');
      key.append(userPart);
    }
    return key.toString();
  }

  private int getContainerIndex() {
    Integer index = executors.getCurrentThreadContext().getCurrentExecutor().attachment();
    return (index != null ? index : 1);
  }
  
}
