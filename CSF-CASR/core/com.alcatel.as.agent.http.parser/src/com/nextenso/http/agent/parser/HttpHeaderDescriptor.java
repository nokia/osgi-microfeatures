package com.nextenso.http.agent.parser;

/**
 *  This class defines the descriptor associated with a specific SIP header. It
 *  it used at parser initialization time to build the parsing tree for all
 *  headers and generate the proper tables subsequently used by the parser.
 */
public interface HttpHeaderDescriptor {
  /**
   *  Retrieve the header name.
   *
   *@return    Header name
   */
  String name();

  /**
   *  Retrieve the abbreviated name of this header
   *
   *@return    Abbreviated header name
   */
  String abbreviatedName();

  /**
   *  Check if multiple instances of this header are allowed
   *
   *@return    true if multiple instances of this header are allowed
   */
  boolean multipleHeaders();

  /**
   *  Check if multiple instances of this header can be concatenated in a single
   *  header or not
   *
   *@return    true if multiple headers can be concatenated in a single header
   */
  public boolean allowConcatenation();
}
