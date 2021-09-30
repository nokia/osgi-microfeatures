package alcatel.tess.hometop.gateways.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Utility class that parses multipart.
 */

public class Multipart {
  
  /**
   * The MIME prefix "multipart/"
   */
  public static final String MULTIPART = "multipart/";
  /**
   * The MIME type "multipart/form-data"
   */
  public static final String MULTIPART_FORM_DATA = "multipart/form-data";
  /**
   * The MIME type "multipart/mixed"
   */
  public static final String MULTIPART_MIXED = "multipart/mixed";
  /**
   * The MIME type "multipart/related"
   */
  public static final String MULTIPART_RELATED = "multipart/related";
  
  private static final byte byteCR = (byte) '\r';
  private static final byte byteLF = (byte) '\n';
  private static final byte byteDASH = (byte) '-';
  private static final byte byteSP = (byte) ' ';
  private static final byte byteTAB = (byte) '\t';
  private static final int intCR = (int) '\r';
  private static final int intLF = (int) '\n';
  private static final int intDASH = (int) '-';
  
  private String mime;
  private String boundary;
  private byte[] boundaryBytes;
  private String preamble, epilogue;
  private ArrayList partsList = new ArrayList();
  
  /**
   * Reset the fields (but not the boudary unless the parameter is true).
   */
  public void reset(boolean resetBoundary) {
    mime = null;
    preamble = null;
    epilogue = null;
    if (resetBoundary) {
      boundary = null;
      boundaryBytes = null;
    }
    partsList.clear();
  }
  
  /**
   * Creates a new Multipart with no field set.
   * The boundary and mime type must be set later.
   */
  public Multipart() {
  }
  
  /**
   * Creates a new Multipart with the specified boundary and mime-type.
   */
  public Multipart(String mime_type, String boundary) {
    setMimeType(mime_type);
    setBoundary(boundary);
  }
  
  /**
   * Creates a new Multipart with the specified mime-type, boundary and data.
   * Throws an IllegalArgumentException if the data are invalid.
   */
  public Multipart(String mime_type, String boundary, byte[] data) {
    this(mime_type, boundary, data, 0, data.length);
  }
  
  /**
   * Creates a new Multipart with the specified mime-type, boundary, data and data range.
   * Throws an IllegalArgumentException if the data are invalid.
   */
  public Multipart(String mime_type, String boundary, byte[] data, int offset, int len) {
    this(mime_type, boundary);
    try {
      parse(data, offset, len);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid data");
    }
  }
  
  /**
   * Creates a new Multipart given its content-type (which includes the
   * mime-type and boundary).
   * Throws an IllegalArgumentException if the content-type is invalid.
   */
  public Multipart(String contentType) {
    setContentType(contentType);
  }
  
  /**
   * Creates a new Multipart given its content-type (which includes the
   * mime-type and boundary) and data.
   * Throws an IllegalArgumentException if the content-type is invalid.
   * Throws an IllegalArgumentException if the data are invalid.
   */
  public Multipart(String contentType, byte[] data) {
    this(contentType, data, 0, data.length);
  }
  
  /**
   * Creates a new Multipart given its content-type (which includes the
   * mime-type and boundary), data and data range.
   * Throws an IllegalArgumentException if the content-type is invalid.
   * Throws an IllegalArgumentException if the data are invalid.
   */
  public Multipart(String contentType, byte[] data, int offset, int len) {
    this(contentType);
    try {
      parse(data, offset, len);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid data");
    }
  }
  
  /**
   * Sets the mime-type.
   */
  public void setMimeType(String value) {
    mime = value;
  }
  
  /**
   * Sets the boundary.
   */
  public void setBoundary(String value) {
    boundary = value;
    boundaryBytes = ("--" + boundary).getBytes();
  }
  
  /**
   * Sets the content-type (and therefore the mime-type and boundary).
   * Throws an IllegalArgumentException if the content-type is invalid.
   */
  public void setContentType(String contentType) {
    if (contentType == null)
      throw new IllegalArgumentException("Invalid content-type");
    contentType = clean(contentType);
    
    int index = contentType.indexOf(';');
    if (index == -1)
      throw new IllegalArgumentException("Invalid content-type: no boundary");
    
    setMimeType(contentType.substring(0, index).trim());
    if (!mime.startsWith(MULTIPART))
      throw new IllegalArgumentException("Invalid content-type");
    
    String tmp = getParameter(";boundary=", contentType);
    if (tmp == null) {
      tmp = getParameter(";Boundary=", contentType); // frequent case
      if (tmp == null)
        throw new IllegalArgumentException("Invalid content-type: no boundary");
    }
    setBoundary(tmp);
  }
  
  /**
   * Returns the mime-type.
   */
  public String getMimeType() {
    return mime;
  }
  
  /**
   * Returns the boundary.
   */
  public String getBoundary() {
    return boundary;
  }
  
  /**
   * Returns the content-type.
   */
  public String getContentType() {
    return new StringBuffer().append(mime).append(";boundary=\"").append(boundary).append("\"").toString();
  }
  
  /**
   * Returns the preamble (if any).
   */
  public String getPreamble() {
    return preamble;
  }
  
  /**
   * Sets the preamble.
   */
  public void setPreamble(String value) {
    preamble = value;
  }
  
  /**
   * Returns the epilogue (if any).
   */
  public String getEpilogue() {
    return epilogue;
  }
  
  /**
   * Sets the epilogue.
   */
  public void setEpilogue(String value) {
    epilogue = value;
  }
  
  /**
   * Returns the data
   * Throws an IllegalStateException if the Multipart contains no Part.
   */
  public byte[] getData() {
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      writeData(os);
      return os.toByteArray();
    } catch (IOException e) {
      return null;
    }
  }
  
  /**
   * Writes the data to an OutputStream.
   * Throws an IllegalStateException if the Multipart contains no Part.
   */
  public void writeData(OutputStream out) throws IOException {
    if (partsList.size() == 0)
      throw new IllegalStateException("Multipart has no Part");
    
    byte[] b;
    if (preamble != null) {
      b = preamble.getBytes();
      out.write(b, 0, b.length);
    }
    for (int i = 0; i < partsList.size(); i++) {
      out.write(intCR);
      out.write(intLF);
      out.write(boundaryBytes, 0, boundaryBytes.length);
      out.write(intCR);
      out.write(intLF);
      Part part = (Part) partsList.get(i);
      part.write(out);
    }
    out.write(intCR);
    out.write(intLF);
    out.write(boundaryBytes, 0, boundaryBytes.length);
    out.write(intDASH);
    out.write(intDASH);
    if (epilogue != null) {
      b = epilogue.getBytes();
      out.write(b, 0, b.length);
    }
  }
  
  /**
   * Returns the number of parts.
   */
  public int getPartsSize() {
    return partsList.size();
  }
  
  /**
   * Adds a new part at the bottom of the data.
   */
  public void addPart(Part part) {
    addPart(partsList.size(), part);
  }
  
  /**
   * Adds a new part at the specified location.
   */
  public void addPart(int location, Part part) {
    partsList.add(location, part);
  }
  
  /**
   * Removes a part
   */
  public Part removePart(int location) {
    return (Part) partsList.remove(location);
  }
  
  /**
   * Removes all the parts
   */
  public void removeParts() {
    partsList.clear();
  }
  
  /**
   * Returns a part.
   */
  public Part getPart(int location) {
    return (Part) partsList.get(location);
  }
  
  /**
   * Returns a string representation for this multipart message.
   */
  public String toString() {
    return (toString(false));
  }
  
  public String toString(boolean noBinary) {
    StringBuffer buf = new StringBuffer();
    
    byte[] b;
    if (preamble != null) {
      buf.append(preamble);
    }
    
    for (int i = 0; i < getPartsSize(); i++) {
      Multipart.Part part = getPart(i);
      buf.append((char) intCR).append((char) intLF);
      buf.append((char) intDASH).append((char) intDASH);
      buf.append(getBoundary());
      buf.append((char) intCR).append((char) intLF);
      buf.append(part.toString(noBinary));
    }
    
    buf.append((char) intCR).append((char) intLF);
    buf.append((char) intDASH).append((char) intDASH);
    buf.append(getBoundary());
    buf.append((char) intDASH).append((char) intDASH);
    
    if (epilogue != null) {
      buf.append(epilogue);
    }
    
    return (buf.toString());
  }
  
  /************************
        private methods
  ************************/
  
  private void parse(byte[] data, int offset, int len) throws Exception {
    int end = offset + len;
    if (offset < 0 || len < 0 || end > data.length)
      throw new Exception("Index out of range");
    int index1 = getBoundaryIndex(data, offset, end);
    // get preamble
    switch ((index1 == -1) ? -1 : (index1 - offset)) {
    case -1:
      throw new Exception("Invalid data");
    case 0:
      break; // no preamble - CRLF is missing
    case 1:
      // we take the first byte as the preamble unless it is CR or LF
      switch (data[offset]) {
      case byteCR:
      case byteLF:
        break;
      default:
        preamble = new String(data, offset, 1);
        break;
      }
      break;
    case 2:
      if (data[offset] == byteCR && data[offset + 1] == byteLF)
        break; // no preamble - CRLF is OK
    default:
      // try to get the preamble
      switch (data[index1 - 1]) {
      case byteCR:
        preamble = new String(data, offset, index1 - offset - 1);
        break;
      case byteLF:
        if (data[index1 - 2] == byteCR)
          preamble = new String(data, offset, index1 - offset - 2);
        else
          preamble = new String(data, offset, index1 - offset - 1);
        break;
      default:
        preamble = new String(data, offset, index1 - offset);
        break;
      }
      break;
    }
    index1 += boundaryBytes.length;
    if (data[index1] == byteCR)
      index1++;
    if (data[index1] == byteLF)
      index1++;
    while (true) {
      int index2 = getBoundaryIndex(data, index1, end);
      if (index2 == -1)
        throw new Exception("Invalid data");
      if (index1 < index2)
        addPart(new Part(data, index1, index2, true));
      index1 = index2 + boundaryBytes.length;
      if (data[index1] == byteDASH && data[index1 + 1] == byteDASH) {
        index1 += 2;
        break;
      }
      if (data[index1] == byteCR)
        index1++;
      if (data[index1] == byteLF)
        index1++;
    }
    // get epilogue
    if (index1 != end)
      epilogue = new String(data, index1, end - index1);
  }
  
  private int getBoundaryIndex(byte[] data, int offset, int end) {
    if (offset >= end)
      return -1;
    end = end - boundaryBytes.length;
    loop: for (int i = offset; i <= end; i++) {
      for (int k = 0; k < boundaryBytes.length; k++) {
        if (data[i + k] != boundaryBytes[k])
          continue loop;
      }
      return i;
    }
    return -1;
  }
  
  private static int getEOL(byte[] data, int offset, int end) {
    for (int i = offset; i < end; i++)
      if (data[i] == byteLF)
        return i + 1;
    return -1;
  }
  
  private static String getParameter(String param, String header) {
    int index = header.indexOf(param);
    if (index == -1)
      return null;
    index += param.length();
    if (index == header.length())
      return "";
    if (header.charAt(index) == '"') {
      index++;
      int end = header.indexOf('"', index);
      if (end == -1)
        // no ending quote
        return null;
      return header.substring(index, end);
    } else if (header.charAt(index) == '\'') {
      index++;
      int end = header.indexOf('\'', index);
      if (end == -1)
        // no ending quote
        return null;
      return header.substring(index, end);
    }
    // not quoted
    int end = header.indexOf(';', index);
    if (end == -1)
      end = header.length();
    return header.substring(index, end);
  }
  
  private static String clean(String s) {
    StringBuffer buff = new StringBuffer();
    boolean inQuotes = false;
    char delimiter = ' ';
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"' || c == '\'') {
        if (inQuotes) {
          if (c == delimiter)
            inQuotes = false;
        } else {
          inQuotes = true;
          delimiter = c;
        }
        buff.append(c);
      } else {
        if (inQuotes)
          buff.append(c);
        else if (c > ' ')
          buff.append(c);
      }
    }
    return buff.toString();
  }
  
  /***************************
   * Inner class that encapsulates a Multipart Part.
   * It can be extended for specific use (ex: mail).
   ***************************/
  
  public static class Part implements Cloneable {
    
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    
    private static final byte[] VOID = new byte[0];
    protected StringCaseHashtable headers = new StringCaseHashtable();
    protected byte[] data = VOID;
    protected int dataOff, dataLen;
    
    public Part() {
    }
    
    public Part(byte[] value, int offset, int end) {
      this(value, offset, end, false);
    }
    
    private Part(byte[] value, int offset, int end, boolean stripCRLF) {
      String headerCache = null;
      while (true) {
        int index = getEOL(value, offset, end);
        if (index == -1)
          throw new IllegalArgumentException("Invalid Part");
        String header;
        boolean headerContinuation = (value[offset] == byteSP || value[offset] == byteTAB);
        int len;
        if (value[index - 2] == byteCR)
          len = index - offset - 2;
        else
          len = index - offset - 1;
        if (len == 0) {
          offset = index;
          break;
        }
        header = new String(value, offset, len).trim();
        offset = index;
        
        if (headerContinuation) {
          if (headerCache == null)
            throw new IllegalArgumentException("Invalid Part");
          if ("".equals(header))
            continue;
          header = headerCache + header;
        }
        
        int limit = header.indexOf(':');
        if (limit == -1)
          throw new IllegalArgumentException("Invalid Part");
        String name = header.substring(0, limit).trim();
        String val = header.substring(limit + 1).trim();
        setHeader(name, val);
        headerCache = header;
      }
      if (stripCRLF) {
        if (value[end - 1] == byteLF)
          end--;
        if (value[end - 1] == byteCR)
          end--;
      }
      if (offset > end)
        // data is empty - it is actually incorrect : a CRLF is missing...
        end = offset; // we keep the offset
      setData(value, offset, end - offset, false);
    }
    
    public Enumeration getHeaderNames() {
      return headers.keys();
    }
    
    public void setHeader(String name, String value) {
      if (value == null)
        removeHeader(name);
      else
        headers.put(name, value);
    }
    
    public void setHeaderParam(String hName, String pName, String pValue) {
      String oldValue = (String) headers.get(hName);
      if (oldValue == null)
        return;
      StringBuffer buff = new StringBuffer(oldValue);
      buff.append(';').append(pName);
      if (pValue != null)
        buff.append('=').append(pValue);
      headers.put(hName, buff.toString());
    }
    
    public String getHeaderParam(String header, String param) {
      String h = getHeader(header);
      if (h == null)
        return null;
      h = clean(h);
      String full_param = ";" + param + "=";
      String resp = getParameter(full_param, h);
      if (resp != null)
        return resp;
      // try lowercase
      param = Utils.toASCIILowerCase(param);
      full_param = ";" + param + "=";
      resp = getParameter(full_param, h);
      if (resp != null)
        return resp;
      // try capitalize
      param = Utils.capitalizeFirstLetter(param);
      full_param = ";" + param + "=";
      resp = getParameter(full_param, h);
      if (resp != null)
        return resp;
      // try uppercase
      param = Utils.toASCIIUpperCase(param);
      full_param = ";" + param + "=";
      return getParameter(full_param, h);
    }
    
    public void addHeader(String name, String value) {
      if (value == null)
        return;
      if (name.equalsIgnoreCase(CONTENT_TYPE)) {
        setContentType(value);
        return;
      }
      String oldValue = (String) headers.get(name);
      if (oldValue == null) {
        headers.put(name, value);
        return;
      }
      StringBuffer buff = new StringBuffer(oldValue);
      headers.put(name, buff.append(',').append(value).toString());
    }
    
    public String getHeader(String name) {
      return (String) headers.get(name);
    }
    
    public String getHeadersAsString() {
      StringBuffer buff = new StringBuffer();
      Enumeration e = headers.keys();
      while (e.hasMoreElements()) {
        String name = (String) e.nextElement();
        buff.append(name);
        buff.append(':');
        buff.append((String) headers.get(name));
        buff.append("\r\n");
      }
      return buff.toString();
    }
    
    public String removeHeader(String name) {
      return (String) headers.remove(name);
    }
    
    public void removeHeaders() {
      headers.clear();
    }
    
    public String getContentType() {
      String ctype = (String) headers.get(CONTENT_TYPE);
      
      if (ctype == null) {
        return (null);
      }
      
      int i = ctype.indexOf(';');
      if (i != -1)
        return (ctype.substring(0, i).trim());
      
      return (ctype);
    }
    
    public void setContentType(String type) {
      headers.put(CONTENT_TYPE, type);
    }
    
    public void setCharacterEncoding(String enc) {
      StringBuffer buff = new StringBuffer();
      buff.append(getContentType());
      buff.append(";charset=").append(enc);
      headers.put(CONTENT_TYPE, buff.toString());
    }
    
    public String getCharacterEncoding() {
      String ct = getHeader(CONTENT_TYPE);
      if (ct == null)
        return null;
      ct = clean(ct);
      String enc = getParameter(";charset=", ct);
      if (enc != null)
        return enc;
      else
        return getParameter(";Charset=", ct); // common case
    }
    
    public byte[] getData() {
      if (dataOff == 0 && dataLen == data.length)
        return data;
      else {
        byte[] tmp = new byte[dataLen];
        System.arraycopy(data, dataOff, tmp, 0, dataLen);
        data = tmp;
        dataOff = 0;
        // dataLen unchanged
        return data;
      }
    }
    
    public byte[] getInternalData() {
      return data;
    }
    
    // writes the whole part (headers + data) to the OutputStream
    public void write(OutputStream out) throws IOException {
      byte[] b = getHeadersAsString().getBytes();
      out.write(b, 0, b.length);
      out.write(intCR);
      out.write(intLF);
      writeData(out);
    }
    
    // writes the data only to the OutputStream
    public void writeData(OutputStream out) throws IOException {
      out.write(data, dataOff, dataLen);
    }
    
    public String getDataAsString() {
      try {
        String enc = getCharacterEncoding();
        if (enc == null)
          return null;
        return new String(data, dataOff, dataLen, enc);
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    
    public void setData(byte[] value, int offset, int len, boolean copy) {
      if (value == null) {
        data = VOID;
        dataOff = 0;
        dataLen = 0;
      } else {
        if (copy) {
          data = new byte[len];
          dataOff = 0;
          dataLen = len;
          System.arraycopy(value, offset, data, 0, len);
        } else {
          data = value;
          dataOff = offset;
          dataLen = len;
        }
      }
      Object o = headers.get(CONTENT_LENGTH);
      if (o != null)
        headers.put(CONTENT_LENGTH, String.valueOf(getDataSize()));
    }
    
    public int getDataOffset() {
      return dataOff;
    }
    
    public int getDataSize() {
      return dataLen;
    }
    
    public String toString() {
      return (toString(false));
    }
    
    /**
     * Return this part as a string.
     * @param noBinary true if the binary body part must not be displayed, false otherwize
     */
    public String toString(boolean noBinary) {
      String ct = getContentType();
      if (noBinary) {
        // Only display header part , if body part contains some binary data
        boolean containsBinary = false;
        for (int i = 0; i < dataLen; i++) {
          if (!Utils.isPrintable(data[dataOff + i])) {
            containsBinary = true;
            break;
          }
        }
        
        if (containsBinary) {
          StringBuffer buf = new StringBuffer();
          buf.append(getHeadersAsString());
          buf.append("\r\n");
          return (buf.toString());
        }
      }
      
      StringBuffer buffer = new StringBuffer();
      buffer.append(getHeadersAsString());
      buffer.append('\r');
      buffer.append('\n');
      String dataS = getDataAsString();
      if (dataS == null)
        dataS = new String(data, dataOff, dataLen);
      buffer.append(dataS);
      return buffer.toString();
    }
    
    public Object clone() {
      try {
        Part p = (Part) super.clone();
        if (headers != null) {
          p.headers = (StringCaseHashtable) headers.clone();
        }
        
        p.data = new byte[dataLen];
        System.arraycopy(data, dataOff, p.data, 0, dataLen);
        
        return (p);
      }
      
      catch (CloneNotSupportedException e) {
        // this shouldn't happen, since we are Cloneable
        throw new InternalError();
      }
    }
  }
  
  public static void main(String[] s) throws Exception {
    //usage: java <classname> <contentType> <filename>
    // contentType exemple: "multipart/mixed; boundary=DELIOS"
    FileInputStream fin = new FileInputStream(new File(s[1]));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int i;
    while ((i = fin.read()) != -1)
      bos.write(i);
    byte[] b = bos.toByteArray();
    Multipart m = new Multipart(s[0], b, 0, b.length);
    
    /*
      System.err.println ("******************** Parsing and Testing getData method ...");
      System.err.println ("Found "+m.getPartsSize()+" parts");
      System.err.println ("********************");
      System.err.println (new String (m.getData ()));
    */
    
    System.err.println("******************** Parsing and Testing toString method ...");
    System.err.println(m.toString());
    
    System.err.println("******************** Parsing and Testing toString(true) method ...");
    System.err.println(m.toString(true));
    System.err.println("********************");
    
    if (true)
      return;
    
    Multipart.Part p = m.getPart(1);
    Multipart.Part p2 = (Multipart.Part) p.clone();
    
    p2.removeHeader("Content-Type");
    System.out.println(p.getHeader("Content-Type"));
    System.out.println(p2.getHeader("Content-Type"));
    System.out.println(p2.toString());
  }
}
