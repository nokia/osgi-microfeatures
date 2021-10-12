// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * Class similar to java.io.StreamTokenizer, but without bugs and
 * adapted for wmlscript compilation.
 *
 */
public class Lexer {
  
  public static void main(String argv[]) throws Exception {
    InputStream input = System.in;
    
    if (argv.length > 0) {
      input = new FileInputStream(argv[0]);
    }
    
    //
    // because of wmlscript, we needs to parse escapted octal strings on 
    // three digit.
    // For example: the string: "\150" is valid, but the string
    // "\15" is not valid.
    //
    boolean strictEscapedOctalString = true;
    
    Lexer in = new Lexer(input, strictEscapedOctalString);
    in.resetSyntax();
    in.eolIsSignificant(false);
    in.wordChars('a', 'z');
    in.wordChars('A', 'Z');
    in.wordChars('0', '9');
    in.wordChars('_', '_');
    in.whitespaceChars('\u0000', ' ');
    in.quoteChar('"');
    in.quoteChar('\'');
    in.slashStarComments(true);
    in.slashSlashComments(true);
    in.parseNumbers();
    in.ordinaryChar('.');
    
    try {
      while (in.nextToken() != Lexer.TT_EOF) {
        System.out.println(in.toString());
      }
    }
    
    catch (IOException e) {
      System.out.print("failed to read input data, ");
      e.printStackTrace(System.out);
    }
    
    catch (LexerException e) {
      System.out.print("failed to parse input data, ");
      e.printStackTrace(System.out);
    }
  }
  
  /**
   * After a call to the <code>nextToken</code> method, this field
   * contains the type of the token just read. For a single character
   * token, its value is the single character, converted to an integer.
   * For a quoted string token (see , its value is the quote character.
   * Otherwise, its value is one of the following:
   * <ul>
   * <li><code>TT_WORD</code> indicates that the token is a word.
   * <li><code>TT_INTEGER</code> indicates that the token is a int number.
   * <li><code>TT_DOUBLE</code> indicates that the token is a double number.
   * <li><code>TT_EOL</code> indicates that the end of line has been read.
   *     The field can only have this value if the
   *     <code>eolIsSignificant</code> method has been called with the
   *     argument <code>true</code>.
   * <li><code>TT_EOF</code> indicates that the end of the input stream
   *     has been reached.
   * </ul>
   * <p>
   * The initial value of this field is -4.
   *
   * @see     java.io.Lexer#eolIsSignificant(boolean)
   * @see     java.io.Lexer#nextToken()
   * @see     java.io.Lexer#quoteChar(int)
   * @see     java.io.Lexer#TT_EOF
   * @see     java.io.Lexer#TT_EOL
   * @see     java.io.Lexer#TT_DOUBLE
   * @see     java.io.Lexer#TT_INTEGER
   * @see     java.io.Lexer#TT_WORD
   */
  public int ttype = TT_NOTHING;
  
  /**
   * A constant indicating that the end of the stream has been read.
   */
  public static final int TT_EOF = -1;
  
  /**
   * A constant indicating that the end of the line has been read.
   */
  public static final int TT_EOL = '\n';
  
  /**
   * A constant indicating that a number token has been read.
   */
  public static final int TT_INTEGER = -2;
  
  /**
   * A constant indicating that a word token has been read.
   */
  public static final int TT_WORD = -3;
  
  /* A constant indicating that no token has been read, used for
   * initializing ttype.  FIXME This could be made public and
   * made available as the part of the API in a future release.
   */
  public static final int TT_NOTHING = -4;
  
  /**
   * A constant indicating that a double token has been read.
   */
  public static final int TT_DOUBLE = -5;
  
  /**
   * If the current token is a word token, this field contains a
   * string giving the characters of the word token. When the current
   * token is a quoted string token, this field contains the body of
   * the string.
   * <p>
   * The current token is a word when the value of the
   * <code>ttype</code> field is <code>TT_WORD</code>. The current token is
   * a quoted string token when the value of the <code>ttype</code> field is
   * a quote character.
   * <p>
   * The initial value of this field is null.
   *
   * @see     java.io.Lexer#quoteChar(int)
   * @see     java.io.Lexer#TT_WORD
   * @see     java.io.Lexer#ttype
   */
  public String sval;
  
  /**
   * If the current token is a int number, this field contains the value
   * of that number. The current token is a number when the value of
   * the <code>ttype</code> field is <code>TT_INTEGER</code>.
   * <p>
   * The initial value of this field is 0.0.
   *
   * @see     java.io.Lexer#TT_INTEGER
   * @see     java.io.Lexer#ttype
   */
  public long ival;
  
  /**
   * If the current token is a double number, this field contains the value
   * of that number. The current token is a number when the value of
   * the <code>ttype</code> field is <code>TT_DOUBLE</code>.
   * <p>
   * The initial value of this field is 0.0.
   *
   * @see     java.io.Lexer#TT_DOUBLE
   * @see     java.io.Lexer#ttype
   */
  public double dval;
  
  /**
   * Creates a stream tokenizer that parses the specified input
   * stream. The stream tokenizer is initialized to the following
   * default state:
   * <ul>
   * <li>All byte values <code>'A'</code> through <code>'Z'</code>,
   *     <code>'a'</code> through <code>'z'</code>, and
   *     <code>'&#92;u00A0'</code> through <code>'&#92;u00FF'</code> are
   *     considered to be alphabetic.
   * <li>All byte values <code>'&#92;u0000'</code> through
   *     <code>'&#92;u0020'</code> are considered to be white space.
   * <li><code>'/'</code> is a comment character.
   * <li>Single quote <code>'&#92;''</code> and double quote <code>'"'</code>
   *     are string quote characters.
   * <li>Numbers are parsed.
   * <li>Ends of lines are treated as white space, not as separate tokens.
   * <li>C-style and C++-style comments are not recognized.
   * </ul>
   *
   * @param      is        an input stream.
   * @param      strictEscapedOctalString true if escaped octal string must be parsed on
   *		 3 digits, false if not.
   * @see        java.io.BufferedReader
   * @see        java.io.InputStreamReader
   * @see        java.io.Lexer#Lexer(java.io.Reader)
   */
  public Lexer(InputStream is, boolean strictEscapedOctalString) {
    this(strictEscapedOctalString);
    if (is == null) {
      throw new NullPointerException();
    }
    input = new PushbackInputStream(is);
  }
  
  /**
   * Create a tokenizer that parses the given character stream.
   *
   * @param r  a Reader object providing the input stream.
   * @since   JDK1.1
   */
  public Lexer(Reader r, boolean strictEscapedOctalString) {
    this(strictEscapedOctalString);
    if (r == null) {
      throw new NullPointerException();
    }
    reader = new PushbackReader(r);
  }
  
  /**
   * Resets this tokenizer's syntax table so that all characters are
   * "ordinary." See the <code>ordinaryChar</code> method
   * for more information on a character being ordinary.
   *
   * @see     java.io.Lexer#ordinaryChar(int)
   */
  public void resetSyntax() {
    for (int i = ctype.length; --i >= 0;)
      ctype[i] = 0;
  }
  
  /**
   * Specifies that all characters <i>c</i> in the range
   * <code>low&nbsp;&lt;=&nbsp;<i>c</i>&nbsp;&lt;=&nbsp;high</code>
   * are word constituents. A word token consists of a word constituent
   * followed by zero or more word constituents or number constituents.
   *
   * @param   low   the low end of the range.
   * @param   hi    the high end of the range.
   */
  public void wordChars(int low, int hi) {
    if (low < 0)
      low = 0;
    if (hi >= ctype.length)
      hi = ctype.length - 1;
    while (low <= hi)
      ctype[low++] |= CT_ALPHA;
  }
  
  /**
   * Specifies that all characters <i>c</i> in the range
   * <code>low&nbsp;&lt;=&nbsp;<i>c</i>&nbsp;&lt;=&nbsp;high</code>
   * are white space characters. White space characters serve only to
   * separate tokens in the input stream.
   *
   * @param   low   the low end of the range.
   * @param   hi    the high end of the range.
   */
  public void whitespaceChars(int low, int hi) {
    if (low < 0)
      low = 0;
    if (hi >= ctype.length)
      hi = ctype.length - 1;
    while (low <= hi)
      ctype[low++] = CT_WHITESPACE;
  }
  
  /**
   * Specifies that all characters <i>c</i> in the range
   * <code>low&nbsp;&lt;=&nbsp;<i>c</i>&nbsp;&lt;=&nbsp;high</code>
   * are "ordinary" in this tokenizer. See the
   * <code>ordinaryChar</code> method for more information on a
   * character being ordinary.
   *
   * @param   low   the low end of the range.
   * @param   hi    the high end of the range.
   * @see     java.io.Lexer#ordinaryChar(int)
   */
  public void ordinaryChars(int low, int hi) {
    if (low < 0)
      low = 0;
    if (hi >= ctype.length)
      hi = ctype.length - 1;
    while (low <= hi)
      ctype[low++] = 0;
  }
  
  /**
   * Specifies that the character argument is "ordinary"
   * in this tokenizer. It removes any special significance the
   * character has as a comment character, word component, string
   * delimiter, white space, or number character. When such a character
   * is encountered by the parser, the parser treates it as a
   * single-character token and sets <code>ttype</code> field to the
   * character value.
   *
   * @param   ch   the character.
   * @see     java.io.Lexer#ttype
   */
  public void ordinaryChar(int ch) {
    if (ch >= 0 && ch < ctype.length)
      ctype[ch] = 0;
  }
  
  /**
   * Specified that the character argument starts a single-line
   * comment. All characters from the comment character to the end of
   * the line are ignored by this stream tokenizer.
   *
   * @param   ch   the character.
   */
  public void commentChar(int ch) {
    if (ch >= 0 && ch < ctype.length)
      ctype[ch] = CT_COMMENT;
  }
  
  /**
   * Specifies that matching pairs of this character delimit string
   * constants in this tokenizer.
   * <p>
   * When the <code>nextToken</code> method encounters a string
   * constant, the <code>ttype</code> field is set to the string
   * delimiter and the <code>sval</code> field is set to the body of
   * the string.
   * <p>
   * If a string quote character is encountered, then a string is
   * recognized, consisting of all characters after (but not including)
   * the string quote character, up to (but not including) the next
   * occurrence of that same string quote character, or a line
   * terminator, or end of file. The usual escape sequences such as
   * <code>"&#92;n"</code> and <code>"&#92;t"</code> are recognized and
   * converted to single characters as the string is parsed.
   *
   * @param   ch   the character.
   * @see     java.io.Lexer#nextToken()
   * @see     java.io.Lexer#sval
   * @see     java.io.Lexer#ttype
   */
  public void quoteChar(int ch) {
    if (ch >= 0 && ch < ctype.length)
      ctype[ch] = CT_QUOTE;
  }
  
  /**
   * Specifies that numbers should be parsed by this tokenizer. The
   * syntax table of this tokenizer is modified so that each of the twelve
   * characters:
   * <blockquote><pre>
   *      0 1 2 3 4 5 6 7 8 9 . -
   * </pre></blockquote>
   * <p>
   * has the "numeric" attribute.
   * <p>
   * When the parser encounters a word token that has the format of a
   * double precision floating-point number, it treats the token as a
   * number rather than a word, by setting the the <code>ttype</code>
   * field to the value <code>TT_INTEGER,TT_DOUBLE</code> and putting the numeric
   * value of the token into the <code>nval</code> field.
   *
   * @see     java.io.Lexer#nval
   * @see     java.io.Lexer#TT_INTEGER
   * @see     java.io.Lexer#TT_DOUBLE
   * @see     java.io.Lexer#ttype
   */
  public void parseNumbers() {
    for (int i = '0'; i <= '9'; i++)
      ctype[i] |= CT_DIGIT;
    ctype['.'] |= CT_DIGIT;
    ctype['-'] |= CT_DIGIT;
  }
  
  /**
   * Determines whether or not ends of line are treated as tokens.
   * If the flag argument is true, this tokenizer treats end of lines
   * as tokens; the <code>nextToken</code> method returns
   * <code>TT_EOL</code> and also sets the <code>ttype</code> field to
   * this value when an end of line is read.
   * <p>
   * A line is a sequence of characters ending with either a
   * carriage-return character (<code>'&#92;r'</code>) or a newline
   * character (<code>'&#92;n'</code>). In addition, a carriage-return
   * character followed immediately by a newline character is treated
   * as a single end-of-line token.
   * <p>
   * If the <code>flag</code> is false, end-of-line characters are
   * treated as white space and serve only to separate tokens.
   *
   * @param   flag   <code>true</code> indicates that end-of-line characters
   *                 are separate tokens; <code>false</code> indicates that
   *                 end-of-line characters are white space.
   * @see     java.io.Lexer#nextToken()
   * @see     java.io.Lexer#ttype
   * @see     java.io.Lexer#TT_EOL
   */
  public void eolIsSignificant(boolean flag) {
    eolIsSignificantP = flag;
  }
  
  /**
   * Determines whether or not the tokenizer recognizes C-style comments.
   * If the flag argument is <code>true</code>, this stream tokenizer
   * recognizes C-style comments. All text between successive
   * occurrences of <code>/*</code> and <code>*&#47;</code> are discarded.
  		   * <p>
  		   * If the flag argument is <code>false</code>, then C-style comments
  		   * are not treated specially.
  		   *
  		   * @param   flag   <code>true</code> indicates to recognize and ignore
  		   *                 C-style comments.
  		   */
  public void slashStarComments(boolean flag) {
    slashStarCommentsP = flag;
  }
  
  /**
   * Determines whether or not the tokenizer recognizes C++-style comments.
   * If the flag argument is <code>true</code>, this stream tokenizer
   * recognizes C++-style comments. Any occurrence of two consecutive
   * slash characters (<code>'/'</code>) is treated as the beginning of
   * a comment that extends to the end of the line.
   * <p>
   * If the flag argument is <code>false</code>, then C++-style
   * comments are not treated specially.
   *
   * @param   flag   <code>true</code> indicates to recognize and ignore
   *                 C++-style comments.
   */
  public void slashSlashComments(boolean flag) {
    slashSlashCommentsP = flag;
  }
  
  /**
   * Determines whether or not word token are automatically lowercased.
   * If the flag argument is <code>true</code>, then the value in the
   * <code>sval</code> field is lowercased whenever a word token is
   * returned (the <code>ttype</code> field has the
   * value <code>TT_WORD</code> by the <code>nextToken</code> method
   * of this tokenizer.
   * <p>
   * If the flag argument is <code>false</code>, then the
   * <code>sval</code> field is not modified.
   *
   * @param   fl   <code>true</code> indicates that all word tokens should
   *               be lowercased.
   * @see     java.io.Lexer#nextToken()
   * @see     java.io.Lexer#ttype
   * @see     java.io.Lexer#TT_WORD
   */
  public void lowerCaseMode(boolean fl) {
    forceLower = fl;
  }
  
  /**
   * Parses the next token from the input stream of this tokenizer.
   * The type of the next token is returned in the <code>ttype</code>
   * field. Additional information about the token may be in the
   * <code>nval</code> field or the <code>sval</code> field of this
   * tokenizer.
   * <p>
   * Typical clients of this
   * class first set up the syntax tables and then sit in a loop
   * calling nextToken to parse successive tokens until TT_EOF
   * is returned.
   *
   * @return     the value of the <code>ttype</code> field.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.Lexer#nval
   * @see        java.io.Lexer#sval
   * @see        java.io.Lexer#ttype
   */
  public int nextToken() throws IOException, LexerException {
    if (pushedBack) {
      pushedBack = false;
      return ttype;
    }
    byte ct[] = ctype;
    sval = null;
    
    int c = peekc;
    if (c < 0)
      c = NEED_CHAR;
    if (c == SKIP_LF) {
      c = read();
      if (c < 0)
        return ttype = TT_EOF;
      if (c == '\n')
        c = NEED_CHAR;
    }
    if (c == NEED_CHAR) {
      c = read();
      if (c < 0)
        return ttype = TT_EOF;
    }
    ttype = c; /* Just to be safe */
    
    /* Set peekc so that the next invocation of nextToken will read
     * another character unless peekc is reset in this invocation
     */
    peekc = NEED_CHAR;
    
    int ctype = c < 256 ? ct[c] : CT_ALPHA;
    while ((ctype & CT_WHITESPACE) != 0) {
      if (c == '\r') {
        LINENO++;
        if (eolIsSignificantP) {
          peekc = SKIP_LF;
          return ttype = TT_EOL;
        }
        c = read();
        if (c == '\n')
          c = read();
      } else {
        if (c == '\n') {
          LINENO++;
          if (eolIsSignificantP) {
            return ttype = TT_EOL;
          }
        }
        c = read();
      }
      if (c < 0)
        return ttype = TT_EOF;
      ctype = c < 256 ? ct[c] : CT_ALPHA;
    }
    
    boolean parseNumber = false;
    
    // Check if we have a number to parse.
    
    if (c == '.') {
      int c2 = read();
      
      if (c2 >= '0' && c2 <= '9') {
        parseNumber = true;
      }
      
      unread(c2);
    } else {
      parseNumber = ((ctype & CT_DIGIT) != 0);
    }
    
    if (parseNumber) {
      boolean dbl = false;
      boolean octal = false;
      boolean hexa = false;
      boolean dot = false;
      
      buffer.setLength(0);
      if (c == '-' || c == '+') {
        return (ttype = c);
      } else if (c == '0') {
        octal = true;
      }
      
      while (true) {
        if (c >= '0' && c <= '7') {
          buffer.append((char) c);
        } else if (c > '7' && c <= '9') {
          buffer.append((char) c);
          if (octal) {
            throw new LexerException("Octal constant contains invalid digit (" + buffer.toString()
                + ") at line " + lineno());
          }
          octal = false;
        } else if (c == 'x' || c == 'X') {
          if (buffer.charAt(buffer.length() - 1) != '0') {
            throw new LexerException("could not scan invalid hexa number (" + buffer.toString()
                + ") at line " + lineno());
          }
          buffer.append((char) c);
          hexa = true;
          
          while (true) {
            c = read();
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
              buffer.append((char) c);
            } else {
              break;
            }
          }
          
          break;
        } else if (c == '.' && dot == false) {
          buffer.append((char) c);
          dot = true;
          dbl = true;
        } else if (c == 'e' || c == 'E') {
          dbl = true;
          buffer.append((char) 'e');
          
          // get the exp sign
          c = read();
          if (c == '+' || c == '-') {
            buffer.append((char) c);
          } else if (c >= '0' || c <= '9') {
            buffer.append('+');
            buffer.append((char) c);
          }
          
          while ((c = read()) >= '0' && c <= '9') {
            buffer.append((char) c);
          }
          
          // we have finished the parsing of our exp ...
          break;
        } else {
          break;
        }
        
        c = read();
      }
      
      peekc = c;
      //System.out.println (buffer + ",c=" + (char) c);
      //System.out.println ("dbl=" + dbl);
      //System.out.println ("octal=" + octal);
      
      try {
        if (dbl == false) {
          if (hexa == true) {
            // Skip the "0x" part
            this.ival = Long.parseLong(buffer.substring(2), 16);
          } else if (octal == true) {
            this.ival = Long.parseLong(buffer.toString(), 8);
          } else {
            this.ival = Long.parseLong(buffer.toString());
          }
          return (this.ttype = TT_INTEGER);
        } else {
          this.dval = Double.parseDouble(buffer.toString());
          return (this.ttype = TT_DOUBLE);
        }
      }
      
      catch (NumberFormatException e) {
        throw new LexerException("could not scan invalid number (" + buffer.toString() + ") at line "
            + lineno());
      }
    }
    
    if ((ctype & CT_ALPHA) != 0) {
      int i = 0;
      do {
        if (i >= buf.length) {
          char nb[] = new char[buf.length * 2];
          System.arraycopy(buf, 0, nb, 0, buf.length);
          buf = nb;
        }
        buf[i++] = (char) c;
        c = read();
        ctype = c < 0 ? CT_WHITESPACE : c < 256 ? ct[c] : CT_ALPHA;
      } while ((ctype & (CT_ALPHA | CT_DIGIT)) != 0 && (c != '-'));
      peekc = c;
      sval = String.copyValueOf(buf, 0, i);
      if (forceLower)
        sval = sval.toLowerCase();
      return ttype = TT_WORD;
    }
    
    if ((ctype & CT_QUOTE) != 0) {
      ttype = c;
      int i = 0;
      /* Invariants (because \Octal needs a lookahead):
       *   (i)  c contains char value
       *   (ii) d contains the lookahead
       */
      int d = read();
      while (d >= 0 && d != ttype && d != '\n' && d != '\r') {
        if (d == '\\') {
          c = read();
          int first = c; /* To allow \377, but not \477 */
          if (c >= '0' && c <= '7') {
            c = c - '0';
            int c2 = read();
            if ('0' <= c2 && c2 <= '7') {
              c = (c << 3) + (c2 - '0');
              c2 = read();
              if ('0' <= c2 && c2 <= '7' && first <= '3') {
                c = (c << 3) + (c2 - '0');
                d = read();
              } else if (strictEscapedOctalString) {
                throw new LexerException("could not scan invalid escaped octal value (" + (char) c2
                    + ") at line " + lineno());
              } else
                d = c2;
            } else if (strictEscapedOctalString) {
              throw new LexerException("could not scan invalid escaped octal value (" + (char) c2
                  + ") at line " + lineno());
            } else
              d = c2;
          } else {
            switch (c) {
            case 'a':
              c = 0x7;
              break;
            case 'b':
              c = '\b';
              break;
            case 'f':
              c = 0xC;
              break;
            case 'n':
              c = '\n';
              break;
            case 'r':
              c = '\r';
              break;
            case 't':
              c = '\t';
              break;
            case 'v':
              c = 0xB;
              break;
            
            case 'x':
            case 'u':
              int ii = 0;
              int len;
              int ch2;
              
              if (c == 'x')
                len = 2;
              else
                len = 4;
              
              c = 0;
              
              for (ii = 0; ii < len; ii++) {
                if ((ch2 = read()) == -1) {
                  throw new LexerException("EOF in string literal at line " + lineno());
                }
                
                if (!isHexDigit(ch2)) {
                  throw new LexerException("malformed escape at line " + lineno());
                }
                
                c *= 16;
                c += hexToInt(ch2);
              }
              
              break;
            }
            
            d = read();
          }
        } else {
          c = d;
          d = read();
        }
        
        if (i >= buf.length) {
          char nb[] = new char[buf.length * 2];
          System.arraycopy(buf, 0, nb, 0, buf.length);
          buf = nb;
        }
        buf[i++] = (char) c;
      }
      
      /* If we broke out of the loop because we found a matching quote
       * character then arrange to read a new character next time
       * around; otherwise, save the character.
       */
      peekc = (d == ttype) ? NEED_CHAR : d;
      
      sval = String.copyValueOf(buf, 0, i);
      return ttype;
    }
    
    if (c == '/' && (slashSlashCommentsP || slashStarCommentsP)) {
      c = read();
      if (c == '*' && slashStarCommentsP) {
        int prevc = 0;
        while ((c = read()) != '/' || prevc != '*') {
          if (c == '\r') {
            LINENO++;
            c = read();
            if (c == '\n') {
              c = read();
            }
          } else {
            if (c == '\n') {
              LINENO++;
              c = read();
            }
          }
          if (c < 0)
            return ttype = TT_EOF;
          prevc = c;
        }
        return nextToken();
      } else if (c == '/' && slashSlashCommentsP) {
        while ((c = read()) != '\n' && c != '\r' && c >= 0)
          ;
        peekc = c;
        return nextToken();
      } else {
        /* Now see if it is still a single line comment */
        if ((ct['/'] & CT_COMMENT) != 0) {
          while ((c = read()) != '\n' && c != '\r' && c >= 0)
            ;
          peekc = c;
          return nextToken();
        } else {
          peekc = c;
          return ttype = '/';
        }
      }
    }
    
    if ((ctype & CT_COMMENT) != 0) {
      while ((c = read()) != '\n' && c != '\r' && c >= 0)
        ;
      peekc = c;
      return nextToken();
    }
    
    return ttype = c;
  }
  
  /**
   * Causes the next call to the <code>nextToken</code> method of this
   * tokenizer to return the current value in the <code>ttype</code>
   * field, and not to modify the value in the <code>nval</code> or
   * <code>sval</code> field.
   *
   * @see     java.io.Lexer#nextToken()
   * @see     java.io.Lexer#nval
   * @see     java.io.Lexer#sval
   * @see     java.io.Lexer#ttype
   */
  public void pushBack() {
    if (ttype != TT_NOTHING) /* No-op if nextToken() not called */
      pushedBack = true;
  }
  
  /**
   * Return the current line number.
   *
   * @return  the current line number of this stream tokenizer.
   */
  public int lineno() {
    return LINENO;
  }
  
  /**
   * Returns the string representation of the current stream token.
   *
   * @return  a string representation of the token specified by the
   *          <code>ttype</code>, <code>nval</code>, and <code>sval</code>
   *          fields.
   * @see     java.io.Lexer#nval
   * @see     java.io.Lexer#sval
   * @see     java.io.Lexer#ttype
   */
  public String toString() {
    String ret;
    switch (ttype) {
    case TT_EOF:
      ret = "EOF";
      break;
    case TT_EOL:
      ret = "EOL";
      break;
    case TT_WORD:
      ret = sval;
      break;
    case TT_INTEGER:
      ret = "int=" + ival;
      break;
    case TT_DOUBLE:
      ret = "double=" + dval;
      break;
    case TT_NOTHING:
      ret = "NOTHING";
      break;
    default: {
      /* 
       * ttype is the first character of either a quoted string or
       * is an ordinary character. ttype can definitely not be less
       * than 0, since those are reserved values used in the previous
       * case statements
       */
      if (ttype < 256 && ((ctype[ttype] & CT_QUOTE) != 0)) {
        ret = sval;
        break;
      }
      
      char s[] = new char[3];
      s[0] = s[2] = '\'';
      s[1] = (char) ttype;
      ret = new String(s);
      break;
    }
    }
    return "Token[" + ret + "], line " + LINENO;
  }
  
  /** Private constructor that initializes everything except the streams. */
  private Lexer(boolean strictEscapedOctalString) {
    this.strictEscapedOctalString = strictEscapedOctalString;
    wordChars('a', 'z');
    wordChars('A', 'Z');
    wordChars(128 + 32, 255);
    whitespaceChars(0, ' ');
    commentChar('/');
    quoteChar('"');
    quoteChar('\'');
    parseNumbers();
  }
  
  /** Read the next character */
  private int read() throws IOException {
    if (reader != null)
      return reader.read();
    else if (input != null)
      return input.read();
    else
      throw new IllegalStateException();
  }
  
  /** Read the next character */
  private void unread(int c) throws IOException {
    if (reader != null)
      reader.unread(c);
    else if (input != null)
      input.unread(c);
    else
      throw new IllegalStateException();
  }
  
  /*
   * A predicate to check whether the character `ch' is a hex digit. 
   */
  private boolean isHexDigit(int ch) {
    return ((('0' <= (ch) && (ch) <= '9') || ('a' <= (ch) && (ch) <= 'f') || ('A' <= (ch) && (ch) <= 'F')));
  }
  
  /**
   * Convert the hex digit `ch' to an integer number. 
   */
  private int hexToInt(int ch) {
    return (('0' <= (ch) && (ch) <= '9' ? ((ch) - '0') : ('a' <= (ch) && (ch) <= 'f' ? ((ch) - 'a' + 10)
        : (ch) - 'A' + 10)));
  }
  
  /* Only one of these will be non-null */
  private PushbackReader reader = null;
  private PushbackInputStream input = null;
  
  private char buf[] = new char[20];
  
  /**
   * The next character to be considered by the nextToken method.  May also
   * be NEED_CHAR to indicate that a new character should be read, or SKIP_LF
   * to indicate that a new character should be read and, if it is a '\n'
   * character, it should be discarded and a second new character should be
   * read.
   */
  private int peekc = NEED_CHAR;
  
  private static final int NEED_CHAR = Integer.MAX_VALUE;
  private static final int SKIP_LF = Integer.MAX_VALUE - 1;
  
  private boolean pushedBack;
  private boolean forceLower;
  /** The line number of the last token read */
  private int LINENO = 1;
  
  private boolean eolIsSignificantP = false;
  private boolean slashSlashCommentsP = false;
  private boolean slashStarCommentsP = false;
  
  private byte ctype[] = new byte[256];
  private static final byte CT_WHITESPACE = 1;
  private static final byte CT_DIGIT = 2;
  private static final byte CT_ALPHA = 4;
  private static final byte CT_QUOTE = 8;
  private static final byte CT_COMMENT = 16;
  
  private StringBuffer buffer = new StringBuffer();
  
  // buffer used to parse unicde character.
  private byte unicode[] = new byte[4];
  
  //
  // because of wmlscript, we needs a flag in order to parse 
  // escaped octal strings on three digit.
  // For example: the string: "\150" is valid, but the string
  // "\15" is not valid.
  //
  private boolean strictEscapedOctalString;
}
