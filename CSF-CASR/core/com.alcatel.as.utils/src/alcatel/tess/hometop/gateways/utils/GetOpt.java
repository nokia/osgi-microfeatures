package alcatel.tess.hometop.gateways.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

/**
 * The class <code>GetOpt</code> may be used to parse command line arguments.
 * it supports flags (with no values) and valued agruments.
 *
 */
public class GetOpt {
  /** 
   * Constructor
   * @param args command line arguments
   * @param optstring string containing the legitimate option
   * characters. A colon in optstring means that the previous character
   * is an option that wants an argument which is then taken from the
   * rest of the current args-element. Here is an example of what
   * optstring might look like: "arg1 arg2: arg3".
   */
  public GetOpt(String[] args, String optstring) {
    this(args, optstring, true);
  }
  
  /**  
   * Constructor
   * @param args command line arguments
   * @param optstring string containing the legitimate option
   * characters. A colon in optstring means that the previous character
   * is an option that wants an argument which is then taken from the
   * rest of the current args-element. Here is an example of what
   * optstring might look like: "c:dP:p".
   * @param returnAllArgs specify true if next() should return any
   * arguments, even if not in the optstring.  This is useful for
   * providing an error message.
   */
  public GetOpt(String[] args, String optstring, boolean returnAllArgs) {
    this.returnAllArgs = returnAllArgs;
    
    // Cache the arguments
    this.args = args;
    this.hasArg = false;
    
    // Build the arg hashtable
    parse(optstring);
  }
  
  /**
   * Resets the parser to the first argument.
   */
  public void reset() {
    index = 0;
  }
  
  /** 
   * Scan elements specified in optstring for next option flag.
   * @return The string corresponding to the next argument name.
   */
  public String nextArg() {
    if (this.args == null)
      return (null);
    
    while (this.index < this.args.length) {
      String arg = this.args[this.index++];
      
      // Make sure flag starts with "-"
      if (!arg.startsWith("-"))
        continue;
      
      arg = arg.substring(1);
      
      // So far so good
      // Check if the flag is in the arg_table and if it is get the
      // associated binding.
      Boolean valued = (Boolean) this.arglist.get(arg);
      
      if (valued == null) {
        if (this.returnAllArgs)
          return arg;
        else
          return (null);
      }
      
      if (valued.equals(Boolean.FALSE)) {
        this.hasArg = false;
        return arg;
      } else if (valued.equals(Boolean.TRUE)) {
        this.hasArg = true;
        return arg;
      } else {
        return (arg);
      }
    }
    
    return (null);
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   * @deprecated use #nextString instead of this method.
   */
  public String nextValue() {
    return (nextString());
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public String nextString() {
    if (this.hasArg) {
      return this.args[this.index++];
    } else {
      if (returnAllArgs) {
        return (this.args[this.index++]);
      } else {
        return (null);
      }
    }
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public int nextInt() throws NumberFormatException {
    String s = nextString();
    if (s == null)
      throw new NumberFormatException("no argument specified");
    return (Integer.parseInt(s));
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public boolean nextBoolean() throws NumberFormatException {
    String s = nextString();
    if (s == null)
      throw new NumberFormatException("no argument specified");
    return (Boolean.parseBoolean(s));
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public long nextLong() throws NumberFormatException {
    String s = nextString();
    if (s == null)
      throw new NumberFormatException("no argument specified");
    return (Long.parseLong(s));
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public float nextFloat() throws NumberFormatException {
    String s = nextString();
    if (s == null)
      throw new NumberFormatException("no argument specified");
    return (Float.parseFloat(s));
  }
  
  /**
   * Get the argument (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public double nextDouble() throws NumberFormatException {
    String s = nextString();
    if (s == null)
      throw new NumberFormatException("no argument specified");
    return (Double.parseDouble(s));
  }
  
  /**
   * Get the string content of the file (if any) associated with the flag.
   * @return the argument associated with the flag.
   */
  public String nextFile() throws IOException {
    String file = nextString();
    
    if (file == null) {
      return (null);
    }
    
    StringBuffer buf = new StringBuffer();
    FileReader in = new FileReader(file);
    String line = null;
    char tmp[] = new char[512];
    int n;
    
    try {
      while ((n = in.read(tmp)) != -1) {
        buf.append(tmp, 0, n);
      }
      
      return (buf.toString());
    }
    
    finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
    }
  }
  
  /**
   * Get the binary content of the file (if any) associated with the flag.
   * 
   * @return the argument associated with the flag.
   */
  public boolean nextBinFile(OutputStream out) throws IOException {
    String file = nextString();
    
    if (file == null) {
      return (false);
    }
    
    FileInputStream in = new FileInputStream(file);
    
    try {
      byte[] data = new byte[512];
      int i;
      
      while ((i = in.read(data, 0, data.length)) != -1) {
        out.write(data, 0, i);
      }
    }
    
    finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
    }
    
    return (true);
  }
  
  /**
   * Get the binary content of the file (if any) associated with the flag.
   * 
   * @return the argument associated with the flag.
   */
  public byte[] nextBinFile() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      if (!nextBinFile(out)) {
        return (null);
      }
      
      return (out.toByteArray());
    }
    
    finally {
      try {
        out.close();
      } catch (IOException e) {
      }
    }
  }
  
  /**
   * Get the binary content of the file argument (if any) associated with the flag.
   * The file is intented to contains bytes in hexadecimal representation.
   * @return the argument associated with the flag.
   */
  public byte[] nextHexFile() throws IOException {
    String file = nextString();
    
    if (file == null) {
      return (null);
    }
    
    BufferedReader in = null;
    ByteArrayOutputStream out = null;
    String line = null;
    
    try {
      in = new BufferedReader(new FileReader(file));
      out = new ByteArrayOutputStream();
      
      while ((line = in.readLine()) != null) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        while (tok.hasMoreTokens()) {
          int hex = Integer.parseInt(tok.nextToken(), 16);
          out.write(hex);
        }
      }
      
      return (out.toByteArray());
    }
    
    finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
      
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
        }
      }
    }
  }
  
  /** 
   * Helper method used to display an usage and exit.
   *
   * @param clazz The class name which is displaying an usage
   * @param the usage string.
   */
  public static void usage(Class clazz, String usage) {
    usage(null, clazz, usage);
  }
  
  /** 
   * Helper method used to display an usage and exit.
   *
   * @param msg an error message
   * @param clazz The class name which is displaying an usage
   * @param the usage string.
   */
  public static void usage(String msg, Class clazz, String usage) {
    if (msg != null) {
      System.err.println(msg);
    }
    System.err.println("Usage: " + clazz.getName() + " " + usage);
  }
  
  // Build the argument table
  private void parse(String s) {
    this.arglist = new Hashtable();
    StringTokenizer tokens = new StringTokenizer(s, " ");
    
    while (tokens.hasMoreTokens()) {
      String argName = tokens.nextToken();
      
      if (argName.endsWith(":")) {
        this.arglist.put(argName.substring(0, argName.length() - 1), Boolean.TRUE);
      } else {
        this.arglist.put(argName, Boolean.FALSE);
      }
    }
  }
  
  // command line arguments
  private String[] args;
  
  // Indicator that the flag has an argument following it
  private boolean hasArg;
  
  // Index into the array of arguments
  private int index;
  
  // Table of flags that take arguments after them
  private Hashtable arglist;
  
  // When true, GetOpt returns arguments even if not present in the
  // given optstring.
  private boolean returnAllArgs;
  
  public static void main(String args[]) throws Exception {
    GetOpt opt = new GetOpt(args, "foo bar: int: long: float: double: file: binfile: hexfile:", true);
    String arg;
    
    while ((arg = opt.nextArg()) != null) {
      if (arg.equals("foo")) {
        System.out.println("foo is present");
        continue;
      }
      
      if (arg.equals("bar")) {
        System.out.println("bar: " + opt.nextString());
        continue;
      }
      
      if (arg.equals("int")) {
        System.out.println("int: " + opt.nextInt());
        continue;
      }
      
      if (arg.equals("long")) {
        System.out.println("long: " + opt.nextLong());
        continue;
      }
      
      if (arg.equals("float")) {
        System.out.println("float: " + opt.nextFloat());
        continue;
      }
      
      if (arg.equals("double")) {
        System.out.println("double: " + opt.nextDouble());
        continue;
      }
      
      if (arg.equals("file")) {
        System.out.print("file: " + opt.nextFile());
        continue;
      }
      
      if (arg.equals("binfile")) {
        System.out.print("binfile: " + new String(opt.nextBinFile()));
        continue;
      }
      
      if (arg.equals("hexfile")) {
        System.out.print("hexfile: " + new String(opt.nextHexFile()));
        continue;
      }
      
      System.out.println("Unknown arg: " + arg + "=" + opt.nextString());
    }
  }
}
