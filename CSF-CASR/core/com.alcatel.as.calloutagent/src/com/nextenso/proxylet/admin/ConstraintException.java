package com.nextenso.proxylet.admin;

public class ConstraintException extends Exception {
  
  //  public static final int UNKNOWN_REFERENCE = 10;
  public static final int UNKNOWN_REFERENCE_PROXYLET = 11;
  public static final int UNKNOWN_ARGUMENT_PROXYLET = 12;
  public static final int UNKNOWN_OR_UNIMPLEMENTED_CONSTRAINT = 13;
  public static final int IMPOSSIBLE_CONSTRAINT = 14;
  
  private int id = 0;
  
  public ConstraintException(int id, String msg) {
    super(msg);
    this.id = id;
  }
  
  public int getErrorId() {
    return id;
  }
}
