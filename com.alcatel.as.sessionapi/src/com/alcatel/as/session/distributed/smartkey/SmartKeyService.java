// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.smartkey;

public interface SmartKeyService
{
  /**
   * Create a smart key: "platformId-groupId-nbIds-id0-id1-...-containerId-uniqueId"
   * @param ids an array of long IDs
   * @return a unique smart key. 
   */
  public String createSmartKey(long ... ids);

  /**
   * Create a smart key with a user part: "platformId-groupId-nbIds-id0-id1-...-containerId-uniqueId-userPart"
   * @param userPart the user part
   * @param ids an array of long IDs
   * @return a unique smart key, appended with the user part.
   */
  public String createSmartKey(String userPart, long ... ids);

  /**
   * Test whether the key is a smart key 
   * @param key the smart key.
   * @return true if the key begins with "platformId-groupId-"
   */
  public boolean isSmartKey(String key);

  /**
   * Get the user part of a smart key. 
   * @param key the smart key.
   * @return userPart the user part you provided when creating the smart key, null if there is no user part.
   */
  public String getUserPart(String key);

  /**
   * Get the identities of the agent stored in the smart key. 
   * If the key is "platformId-groupId-3-12-34-56-1-78-userPart" 
   * it returns a long array with [12, 34, 56].
   * @param key the smart key.
   * @return the array of long IDs, or null if this is not a valid key.
   */
  public long[] getAgentIds(String key);
  

  /**
   * Get the identities of a smart key contained in a string
   * @param value the string containing the smart key
   * @return 
   * <p>null if the string does not contain a smart key.</p>
   * <p>an array of the identities:</p>
   * <ul>
   *   <li>The first one is the platform identity</li>
   *   <li>The second one is the group identity</li>
   *   <li>The third one is the container identity</li>
   *   <li>Then are the agent identities</li>
   * </ul>
   */
  public String[] getIdsFromSmartKey(String value);
  
  /**
   * Extract the SmartKey from a string containing a smart key for the local platform.
   * @param value
   * @return a SmartKey or null if the string does not contain a smart key.
   */
  public SmartKey getSmartKey(String value);

  /**
   * Extract the SmartKey from a string containing a smart key for a given platform ID.
   * @param value a string
   * @param PlatformId the platform ID
   * @return a SmartKey or null if the string does not contain a smart key.
   */
  public SmartKey getSmartKey(String value, String platformId);
  
  /**
   * Get the group ID from the group name.
   * @param groupName the group name
   * @return the group ID
   */
  public String getGroupId(String name);
  
}
