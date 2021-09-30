package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class provides the iana assignment numbers for well-known iana charsets.
 * see url http://www.iana.org/assignments/character-sets
 */
public class CharsetMib {
  
  /**
   * Return the charset code for a given charset name or alias.
   */
  public static int getCharsetCode(String nameOrAlias) {
    Charset cs = (Charset) charsetByNames.getObject(nameOrAlias);
    if (cs == null) {
      return -1;
    }
    
    return (cs.mibEnum);
  }
  
  /**
   * Return the charset aliases for a given charset name or alias.
   */
  public static String[] getCharsetAliases(String nameOrAlias) {
    Charset cs = (Charset) charsetByNames.getObject(nameOrAlias);
    if (cs == null) {
      return null;
    }
    
    return (cs.aliases);
  }
  
  /**
   * Return the charset name for a given charset code.
   */
  public static String getCharsetName(int code) {
    Charset cs = (Charset) charsetByCodes.get(code);
    if (cs == null) {
      return null;
    }
    
    return (cs.name);
  }
  
  /**
   * Return the preferred charset name for a given charset code.
   */
  public static String getPreferredCharsetName(int code) {
    Charset cs = (Charset) charsetByCodes.get(code);
    if (cs == null) {
      return null;
    }
    
    return (cs.preferredName != null ? cs.preferredName : cs.name);
  }
  
  /**
   * Return the charset aliases for a given charset code.
   */
  public static String[] getCharsetAliases(int code) {
    Charset cs = (Charset) charsetByCodes.get(code);
    
    if (cs == null) {
      return null;
    }
    
    return (cs.aliases);
  }
  
  /**
   * Return the charset name for a given Locale.
   */
  public static String getCharsetFromLocale(String locale) {
    Charset cs = (Charset) charsetByLocales.getObject(locale);
    if (cs != null) {
      return (cs.preferredName != null ? cs.preferredName : cs.name);
    }
    
    return null;
  }
  
  /**
   * Return the charset name for a given Locale.
   */
  public static String getCharsetFromLocale(Locale locale) {
    // Try to match full name (language + country)
    Charset cs = (Charset) charsetByLocales.getObject(locale.toString());
    if (cs != null) {
      return (cs.preferredName != null ? cs.preferredName : cs.name);
    }
    
    // try to match just the language.
    cs = (Charset) charsetByLocales.getObject(locale.getLanguage());
    if (cs != null) {
      return (cs.preferredName != null ? cs.preferredName : cs.name);
    }
    
    return null;
  }
  
  private static String nextLine(BufferedReader reader) throws IOException {
    String line = null;
    while ((line = reader.readLine()) != null) {
      lineNo++;
      line = line.trim();
      
      if (line.startsWith("#")) {
        continue;
      }
      return line;
    }
    
    return null;
  }
  
  private static int getIntValue(String line) {
    int semi = line.indexOf(":");
    if (semi == -1) {
      error("did not find any semicolumn");
    }
    
    int code = -1;
    try {
      code = Integer.parseInt(line.substring(semi + 1).trim());
    } catch (NumberFormatException e) {
      error("value is not an integer");
    }
    
    return code;
  }
  
  private static String getValue(String line) {
    int semi = line.indexOf(":");
    if (semi == -1) {
      error("did not find any semicolumn");
    }
    
    return line.substring(semi + 1).trim();
  }
  
  private static void error(String msg) {
    System.err.println("Error while parsing CharsetMib.properties at line " + lineNo + ": " + msg);
    System.exit(1);
  }
  
  /**
   * Indicates if the name is the preferred charset name.
   */
  private final static String PREFERRED = "(preferred MIME name)";
  
  /**
   * Charset map (key=charset name/alias value=Charset)
   */
  private static StringCaseHashtable charsetByNames = new StringCaseHashtable();
  
  /**
   * Charset map (key=Locale/value=Charset)
   */
  private static StringCaseHashtable charsetByLocales = new StringCaseHashtable();
  
  /**
   * Charset map (key=charset code value=Charset)
   */
  private static IntHashtable charsetByCodes = new IntHashtable();
  
  /**
   * Current line number when parsing the property file.
   */
  private static int lineNo;
  
  /**
   * A Charset description: name/code/aliases.
   */
  private static class Charset {
    private String name;
    private String preferredName;
    private String[] aliases;
    private int mibEnum;
    private String[] locales; // for example, local "en" corresponds to the charset ISO-8859-1.
  }
  
  static {
    BufferedReader reader = null;
    try {
      URL url = Utils.getResource("CharsetMib.properties",
                                  new ClassLoader[] { ClassLoader.getSystemClassLoader(),
                                      CharsetMib.class.getClassLoader() });
      if (url == null) {
        throw new ExceptionInInitializerError("CharsetMib.properties not found from the classpath");
      }
      
      reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String line;
      
      while ((line = nextLine(reader)) != null) {
        // Get charset name.
        
        if (line.length() == 0) {
          continue;
        }
        
        if (!line.startsWith("Name: ")) {
          error("line does not starts with \"Name:\"");
        }
        String preferredName = null;
        
        String name = getValue(line);
        if (name.indexOf(PREFERRED) != -1) {
          name = Utils.replace(name, PREFERRED, "").trim();
          preferredName = name;
        }
        
        // Get mib enum.
        
        if ((line = nextLine(reader)) == null || !line.startsWith("MIBenum: ")) {
          error("Cannot find mib enum");
        }
        int code = getIntValue(line);
        
        // Get Locale
        
        if ((line = nextLine(reader)) == null || !line.startsWith("Locale:")) {
          error("Cannot find Locale");
        }
        String s = getValue(line);
        String[] locales = null;
        if (s.equalsIgnoreCase("None")) {
          locales = null;
        } else {
          Vector v = new Vector();
          StringTokenizer tok = new StringTokenizer(s, ",");
          while (tok.hasMoreTokens()) {
            v.add(tok.nextToken().trim());
          }
          locales = new String[v.size()];
          v.toArray(locales);
        }
        
        // Get Alias.
        
        Vector aliases = new Vector();
        while ((line = nextLine(reader)) != null && line.length() > 0 && line.startsWith("Alias: ")) {
          String alias = getValue(line);
          if (alias.equalsIgnoreCase("None")) {
            continue;
          }
          if (alias.indexOf(PREFERRED) != -1) {
            alias = Utils.replace(alias, PREFERRED, "").trim();
            if (preferredName == null) {
              preferredName = alias;
            }
          }
          aliases.add(alias);
        }
        
        Charset charset = new Charset();
        charset.name = name;
        charset.preferredName = preferredName;
        charset.mibEnum = code;
        charset.locales = locales;
        charset.aliases = new String[aliases.size()];
        for (int i = 0; i < aliases.size(); i++) {
          charset.aliases[i] = (String) aliases.get(i);
        }
        
        charsetByCodes.put(code, charset);
        if (locales != null) {
          for (int i = 0; i < locales.length; i++) {
            charsetByLocales.put(locales[i], charset);
          }
        }
        charsetByNames.putObject(name, charset);
        for (int i = 0; i < charset.aliases.length; i++) {
          charsetByNames.putObject(charset.aliases[i], charset);
        }
      }
    }
    
    catch (Throwable t) {
      error("Error while loading CharsetMib.properties");
    }
    
    finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
  }
  
  public static void main(String args[]) throws Exception {
    if (args[0].equals("-c")) {
      int code = Integer.parseInt(args[1]);
      String name = CharsetMib.getPreferredCharsetName(code);
      System.out.println("Preferred name=" + name);
      String[] aliases = CharsetMib.getCharsetAliases(code);
      for (int i = 0; i < aliases.length; i++) {
        System.out.println(" " + aliases[i]);
      }
    } else if (args[0].equals("-l")) {
      String locale = args[1];
      System.out
          .println("Charset for locale \"" + locale + "\" = " + CharsetMib.getCharsetFromLocale(locale));
    } else {
      String name = args[0];
      System.out.println("Code=" + CharsetMib.getCharsetCode(name));
      String[] aliases = CharsetMib.getCharsetAliases(name);
      for (int i = 0; i < aliases.length; i++) {
        System.out.println(" " + aliases[i]);
      }
    }
  }
}
