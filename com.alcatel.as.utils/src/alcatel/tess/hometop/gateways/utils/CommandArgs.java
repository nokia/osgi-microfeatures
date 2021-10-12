// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * The class <code>CommandArgs</code> may be used to parse command line arguments.
 * it supports mandatory and optional arguments.
 * @deprecated Use alcatel.tess.hometop.gateways.GetOpt class which is easier to used
 *	       and provide the parsing of unknown flags
 *
 */
public class CommandArgs {
  
  /**
   * Creates a new <code>CommandArgs</code> instance.
   *
   * @param args a <code>String[]</code> value containing command args
   * @param usage a <code>String</code> value containing the command usage 
   * description.
   */
  public CommandArgs(String[] args, String usage) {
    this(args, -1, usage);
  }
  
  /**
   * Creates a new <code>CommandArgs</code> instance.
   * An exception is raised if the specified args counts doesn't match 
   * the <code>minArgs</code> parameter.
   *
   * @param args a <code>String[]</code> value containing the args list
   * @param minArgs an <code>int</code> value corresponding to the number of
   *	mandatory parameters, or -1 if all arguments are optionals
   * @param usage a <code>String</code> value describing the command usage
   * @exception IllegalArgumentException if the args length is less than the
   *	<code>minArgs</code> parameter.
   */
  public CommandArgs(String[] args, int minArgs, String usage) throws IllegalArgumentException {
    this.args = args;
    this.usage = usage;
    
    if (minArgs != -1 && args.length < minArgs) {
      throw new IllegalArgumentException("not enough provided arguments." + LINE_SEPARATOR + usage);
    }
  }
  
  /**
   * Creates a new <code>CommandArgs</code> instance.
   * mandatory and optional named parameters must be specified,
   * and an exception is raised if the command args do not contains
   * mandatory args, or if an unknown args is provided. Unknown args
   * are arguments that do are not contained in <code>mandatoryArgs</code>
   * or <code>optionalArgs</code> parameters.
   *
   * @param args a <code>String[]</code> value containing the args list
   * @param mandatoryArgs a <code>String</code> value containing the 
   *	mandatory args list separated by a blank
   * @param optionalArgs a <code>String</code> value containing the
   *	optional args list separated by a blank
   * @param usage a <code>String</code> value describing the command usage
   * @exception IllegalArgumentException if the args doesn't contains one the
   *	the mandatory args, or if an argument is unknown (not found in
   *	mandatory or optional args list)
   */
  public CommandArgs(String[] args, String mandatoryArgs, String optionalArgs, String usage)
                                                                                            throws IllegalArgumentException {
    this.args = args;
    this.usage = usage;
    this.optionalArgs = optionalArgs;
    this.arguments = new Hashtable();
    
    parseArguments(args, mandatoryArgs, true);
    parseArguments(args, optionalArgs, false);
    
    //
    // Check for unknown args.
    //
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-' && !checkValidArg(args[i], mandatoryArgs)) {
        // 
        // This argument is not a mandatory arg. Check if its an optional arg.
        // 
        if (!checkValidArg(args[i], optionalArgs)) {
          throw new IllegalArgumentException("invalid argument: " + args[i] + LINE_SEPARATOR + this.usage);
        }
      }
    }
  }
  
  /**
   * The <code>length</code> method returns the number of arguments provided 
   * in command line args.
   *
   * @return an <code>int</code> value
   */
  public int length() {
    return (args.length);
  }
  
  /**
   * The <code>getArg</code> method returns argument n from command arguments.
   *
   * @param n an <code>int</code> value
   * @return a <code>String</code> value
   * @exception IllegalArgumentException if args count is less or equals to n
   */
  public String getArg(int n) throws IllegalArgumentException {
    if (args.length <= n) {
      throw new IllegalArgumentException("argument " + n + " not provided." + LINE_SEPARATOR + this.usage);
    }
    
    return (args[n]);
  }
  
  /**
   * The <code>getArg</code> method returns argument n
   * from command arguments. If not found, it returns a default value
   *
   * @param n an <code>int</code> value corresponding to the argument n
   * @param def a <code>String</code> value returned in case argument n is not
   *	found
   * @return a <code>String</code> value corresponding to the value of 
   *	argument n
   */
  public String getArg(int n, String def) {
    if (args.length <= n) {
      return (def);
    }
    
    return (args[n]);
  }
  
  /**
   * The <code>getArgInt</code> method returns the argument n as an int value.
   *
   * @param n an <code>int</code> value corresponding to the argument n
   * @return an <code>int</code> value corresponding to the value of argument n
   * @exception IllegalArgumentException if argument is not found or is not an 
   *	integer
   */
  public int getArgInt(int n) throws IllegalArgumentException {
    if (args.length <= n) {
      throw new IllegalArgumentException("argument " + n + " not provided." + LINE_SEPARATOR + this.usage);
    }
    
    try {
      return (Integer.parseInt(args[n]));
    }
    
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("argument " + n + " (" + args[n] + ") " + "is not an integer."
          + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * The <code>getArgFloat</code> method returns the argument n as an long value.
   *
   * @param n an <code>long</code> value corresponding to the argument n
   * @return an <code>long</code> value corresponding to the value of argument n
   * @exception IllegalArgumentException if argument is not found or is not an 
   *	integer
   */
  public float getArgFloat(int n) throws IllegalArgumentException {
    if (args.length <= n) {
      throw new IllegalArgumentException("argument " + n + " not provided." + LINE_SEPARATOR + this.usage);
    }
    
    try {
      return (Float.parseFloat(args[n]));
    }
    
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("argument " + n + " (" + args[n] + ") " + "is not a long integer."
          + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * The <code>getArgFile</code> method reads the file specified by the
   * argument n and returns its content as a String.
   *
   * @param n an <code>int</code> value
   * @return a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public String getArgFile(int n) throws IllegalArgumentException {
    byte[] b = getArgBinFile(n);
    return (new String(b));
  }
  
  /**
   * The <code>getArgFile</code> method reads the file specified by the
   * argument option name and returns its content as a String.
   *
   * @param option a <code>String</code> value
   * @return a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public String getArgFile(String option) throws IllegalArgumentException {
    byte[] b = getArgBinFile(option);
    return (new String(b));
  }
  
  /**
   * The <code>getArgBinFile</code> method reads the file specified by the
   * argument n and returns its content as a byte array.
   *
   * @param n an <code>int</code> value
   * @return a <code>byte[]</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public byte[] getArgBinFile(int n) throws IllegalArgumentException {
    try {
      return (loadFile(getArg(n)));
    }
    
    catch (IOException e) {
      throw new IllegalArgumentException("argument " + n + " (" + getArg(n) + ") "
          + "is not a an accessible file" + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * The <code>getArgHexFile</code> method reads the file specified by the
   * argument n and returns its content as a byte array. The file is intented
   * to contains bytes in hexadecimal representation.
   *
   * @param n an <code>int</code> value
   * @return a <code>byte[]</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public byte[] getArgHexFile(int n) throws IllegalArgumentException {
    try {
      return (loadHexFile(getArg(n)));
    }
    
    catch (IOException e) {
      throw new IllegalArgumentException("argument " + n + " (" + getArg(n) + ") "
          + "is not a an accessible file" + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * The <code>getArgBinFile</code> method reads the file specified by the
   * option argument and returns its content as a byte array.
   *
   * @param option an <code>String</code> value
   * @return a <code>byte[]</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public byte[] getArgBinFile(String option) throws IllegalArgumentException {
    try {
      return (loadFile(getArg(option)));
    }
    
    catch (IOException e) {
      throw new IllegalArgumentException("argument " + option + " (" + getArg(option) + ") "
          + "is not a an accessible file" + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * The <code>getArgHexFile</code> method reads the file specified by the
   * option argument and returns its content as a byte array. The file is intented
   * to contains bytes in hexadecimal representation.
   *
   * @param option an <code>String</code> value
   * @return a <code>byte[]</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public byte[] getArgHexFile(String option) throws IllegalArgumentException {
    try {
      return (loadHexFile(getArg(option)));
    }
    
    catch (IOException e) {
      throw new IllegalArgumentException("argument " + option + " (" + getArg(option) + ") "
          + "is not a an accessible file" + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * The <code>getArg</code> method returns the argument corresponding
   * to an option name (for example -opt <optValue>). You specify the option 
   * name ("-opt") and the methods returns the option value ("optValue").
   *
   * @param optionName a <code>String</code> value giving the option name (for 
   *	example "-opt")
   * @return a <code>String</code> value corresponding to the option
   *	value
   * @exception IllegalArgumentException if the option name if not found
   */
  public String getArg(String optionName) throws IllegalArgumentException {
    if (optionName.charAt(0) != '-')
      optionName = "-" + optionName;
    
    String value = (String) arguments.get(optionName);
    
    if (value == null && optionalArgs.indexOf(optionName) == -1) {
      throw new IllegalArgumentException("argument " + optionName + " not provided" + LINE_SEPARATOR
          + this.usage);
    }
    
    return (value);
  }
  
  /**
   * Same method as {@link #getArg(String)}, but a default
   * value is returned if the argument is not found or invalid.
   *
   * @param optionName a <code>String</code> value
   * @param def a <code>String</code> value
   * @return a <code>String</code> value corresponding to the option
   *	value
   */
  public String getArg(String optionName, String def) {
    if (optionName.charAt(0) != '-')
      optionName = "-" + optionName;
    
    String value = (String) arguments.get(optionName);
    return ((value != null) ? value : def);
  }
  
  /**
   * Same method as {@link #getArg(String)}, but the value is returned as an int.
   *
   * @param optionName a <code>String</code> value
   * @return an <code>int</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public int getArgInt(String optionName) throws IllegalArgumentException {
    try {
      return (Integer.parseInt(getArg(optionName)));
    }
    
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("argument " + optionName + "(" + arguments.get(optionName) + ")"
          + " is not an integer." + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * Same method as {@link #getArg(String)}, but the value is returned as an int.
   *
   * @param optionName a <code>String</code> value
   * @return an <code>int</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public float getArgFloat(String optionName) throws IllegalArgumentException {
    try {
      return (Float.parseFloat(getArg(optionName)));
    }
    
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("argument " + optionName + "(" + arguments.get(optionName) + ")"
          + " is not a long integer." + LINE_SEPARATOR + this.usage);
    }
  }
  
  /**
   * Same method as  {@link #getArgInt(String)}, but a default value is
   * returned in case of errors.
   *
   * @param optionName a <code>String</code> value
   * @param def an <code>int</code> value
   * @return an <code>int</code> value
   * @exception NumberFormatException if an error occurs
   */
  public int getArgInt(String optionName, int def) throws NumberFormatException {
    String value = getArg(optionName, String.valueOf(def));
    return (Integer.parseInt(value));
  }
  
  private void parseArguments(String[] args, String options, boolean mandatory)
      throws IllegalArgumentException {
    //
    // Parse arguments.
    //
    if (options != null) {
      StringTokenizer tok = new StringTokenizer(options, " ");
      while (tok.hasMoreTokens()) {
        String opt = tok.nextToken();
        
        int i = 0;
        for (; i < args.length; i++) {
          if (args[i].equals(opt))
            break;
        }
        
        if (i == args.length) {
          if (mandatory) {
            throw new IllegalArgumentException("missing mandatory argument: " + opt + "." + LINE_SEPARATOR
                + "usage: " + this.usage);
          }
          
          continue;
        }
        
        // 
        // Check if an argument value is present.
        //
        if (i < args.length - 1 && args[i + 1].charAt(0) != '-') {
          arguments.put(opt, args[i + 1]);
        } else {
          arguments.put(opt, "");
        }
      }
    }
  }
  
  private boolean checkValidArg(String arg, String options) throws IllegalArgumentException {
    if (options != null) {
      StringTokenizer tok = new StringTokenizer(options, " ");
      while (tok.hasMoreTokens()) {
        String opt = tok.nextToken();
        if (opt.equals(arg))
          return (true);
      }
    }
    
    return (false);
  }
  
  private byte[] loadFile(String file) throws IOException {
    FileInputStream in = null;
    ByteArrayOutputStream out = null;
    
    try {
      in = new FileInputStream(file);
      out = new ByteArrayOutputStream();
      
      int i;
      
      while ((i = in.read()) != -1) {
        out.write(i);
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
  
  private byte[] loadHexFile(String file) throws IOException {
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
  
  private Hashtable arguments;
  private String optionalArgs;
  private String usage;
  private String[] args;
  
  /**
   * The <code>main</code> method is used to test this class.
   *
   * @param args[] a <code>String</code> value
   * @exception Exception if an error occurs
   */
  private static void main(String args[]) throws Exception {
    try {
      System.out.println("\nTest 1: options -a and -b are mandatory, while -c and -d are optionals");
      System.out.println("----------------------------------------------------------------------");
      CommandArgs cmd = new CommandArgs(args, "-a -b", "-c -d",
          "java CommandArgs -a <value> -b <value> [-c -d <value>]");
      System.out.println("value for option -a=" + cmd.getArg("-a"));
      System.out.println("value for option -b=" + cmd.getArg("-b"));
      System.out.println("value for option -c=" + cmd.getArg("-c", "defc"));
      System.out.println("value for option -d=" + cmd.getArg("-d", "defd"));
      
      System.out.println("dumping all args...");
      for (int i = 0; i < cmd.length(); i++) {
        System.out.print(cmd.getArg(i));
        if (i == cmd.length() - 1) {
          System.out.println();
        } else {
          System.out.print(" ");
        }
      }
    }
    
    catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    
    try {
      System.out.println("\nTest 2: args do not contains options. The last arg is an int");
      System.out.println("------------------------------------------------------------");
      CommandArgs cmd = new CommandArgs(args, 2, "java CommandsArgs arg1 intarg2");
      System.out.println("value for arg 0=" + cmd.getArg(0));
      System.out.println("value for arg 1=" + cmd.getArgInt(1));
      
      System.out.println("dumping all args...");
      for (int i = 0; i < cmd.length(); i++) {
        System.out.print(cmd.getArg(i));
        if (i == cmd.length() - 1) {
          System.out.println();
        } else {
          System.out.print(" ");
        }
      }
    }
    
    catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
  }
  
  private final static String LINE_SEPARATOR = System.getProperty("line.separator");
}
