package com.alcatel.as.session.distributed.event;

public interface GroupListener
{

  /**
   * Indicates that there is new member(s) in a group
   * @param name the group name
   */
  public void connected(String name);
 
  /**
   * Indicates that a group is empty
   * @param name the group name
   */
  public void disconnected(String name);

}
