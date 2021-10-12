// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamTokenizer implements Recyclable {
  
  public static final int READ_EOF = -1;
  public static final int READ_EOL = -2;
  public static final int READ_WORD = -3;
  
  private static final int STATE_INIT = 0; // no state
  private static final int STATE_IN_WORD = 1; // we are reading a word
  private static final int STATE_DELIM = 2; // we have a delimiter in cache
  private static final int STATE_CR = 4; // we have a CR in cache
  private static final int STATE_LF = 5; // we have a LF in cache
  private static final int STATE_OPEN_QUOTE = 6; // we have a OPEN_QUOTE in cache
  private static final int STATE_EOF = 7; // we have an EOF in cache
  
  private int state;
  private char[] word = new char[512];
  private int wordPosition = 0;
  private int currentDelim = 0;
  private boolean quoting;
  private int quoteOpen, quoteClose;
  private boolean inUTF8 = false;
  private int utf8Cache = 0;
  private int utf8State = 0;
  
  public InputStreamTokenizer() {
    reset();
  }
  
  public void reset() {
    state = STATE_INIT;
    wordPosition = 0;
    quoting = false;
    inUTF8 = false;
  }
  
  /**
   * @return the number of bytes read (which may be lower than the expectedWord length if EOF is met and throwEOF==false)
   */
  public int readConstant(InputStream in, byte[] constant, boolean throwEOF) throws IOException {
    if (state != STATE_INIT)
      throw new IOException("Invalid stream state");
    int c;
    int len = constant.length;
    int read = 0;
    for (int i = 0; i < len; i++) {
      c = in.read();
      if (c == -1) {
        if (throwEOF)
          throw new EOFException();
        
        return read;
      }
      if (c != (constant[i] & 0xFF))
        throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
    }
    return len;
  }
  
  /**
   * @return 1 if the byte was read, or 0 if EOF was met before and throwEOF==false
   */
  public int readByte(InputStream in, int constant, boolean throwEOF) throws IOException {
    if (state != STATE_INIT)
      throw new IOException("Invalid stream state");
    int c = in.read();
    if (c == -1) {
      if (throwEOF)
        throw new EOFException();
      
      return 0;
    }
    if (c != constant)
      throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
    return 1;
  }
  
  public int read(InputStream in, Syntax syntax) throws IOException {
    switch (state) {
    case STATE_OPEN_QUOTE:
      word[wordPosition++] = syntax.values[quoteOpen];
      break;
    case STATE_DELIM:
      state = STATE_INIT;
      return currentDelim;
    case STATE_LF:
      state = STATE_INIT;
      return READ_EOL;
    case STATE_CR:
      return readLF(in, syntax);
    case STATE_EOF:
      state = STATE_INIT;
      return READ_EOF;
    }
    int c = 0;
    char[] values = syntax.values;
    while (true) {
      c = in.read();
      if (c == -1) {
        if (syntax.throwEOF)
          throw new EOFException();
        if (syntax.delimEOF) {
          if (inUTF8)
            throw new IOException("Invalid UTF-8 character");
          if (quoting)
            throw new IOException("Missing Quote-Close");
          if (state == STATE_IN_WORD) {
            state = STATE_EOF;
            return READ_WORD;
          }
          state = STATE_INIT;
          return READ_EOF;
        }
        return READ_EOF;
      }
      if (inUTF8) {
        if (syntax.codes[c] == CHAR_10) {
          utf8State--;
          utf8Cache <<= 6;
          utf8Cache |= c & 0x3F;
          if (utf8State == 0) {
            word[wordPosition++] = (char) utf8Cache;
            inUTF8 = false;
          }
          continue;
        }
        throw new IOException("Invalid UTF-8 character");
      }
      switch (syntax.codes[c]) {
      case CHAR_11110:
        inUTF8 = true;
        utf8State = 3;
        utf8Cache = c & 0x07;
        state = STATE_IN_WORD;
        break;
      case CHAR_1110:
        inUTF8 = true;
        utf8State = 2;
        utf8Cache = c & 0x0F;
        state = STATE_IN_WORD;
        break;
      case CHAR_110:
        inUTF8 = true;
        utf8State = 1;
        utf8Cache = c & 0x1F;
        state = STATE_IN_WORD;
        break;
      case CHAR_10:
        throw new IOException("Invalid UTF-8 character");
      case CHAR_0:
        word[wordPosition++] = values[c];
        state = STATE_IN_WORD;
        break;
      case DELIM:
        if (quoting) {
          word[wordPosition++] = values[c];
          break;
        }
        if (state == STATE_IN_WORD) {
          state = STATE_DELIM;
          currentDelim = c;
          return READ_WORD;
        }
        return c;
      case SPACE:
        if (quoting) {
          word[wordPosition++] = values[c];
          break;
        }
        if (state == STATE_IN_WORD) {
          state = STATE_INIT;
          return READ_WORD;
        }
        break;
      case CR:
        if (quoting)
          throw new IOException("Missing Quote-Close");
        if (state == STATE_IN_WORD) {
          state = STATE_CR;
          return READ_WORD;
        }
        return readLF(in, syntax);
      case LF:
        if (quoting)
          throw new IOException("Missing Quote-Close");
        if (syntax.strictCRLF)
          throw new IOException("Found LF without CR");
        if (state == STATE_IN_WORD) {
          state = STATE_LF;
          return READ_WORD;
        }
        state = STATE_INIT;
        return READ_EOL;
      case QUOTE:
        if (quoting) {
          if (c == quoteClose) {
            if (syntax.writeQuotes)
              word[wordPosition++] = values[c];
            quoting = false;
            state = STATE_INIT;
            return READ_WORD;
          }
          word[wordPosition++] = values[c];
          break;
        }
        if (state == STATE_IN_WORD) {
          quoting = true;
          quoteClose = syntax.quoteCloses[c];
          if (syntax.writeQuotes) {
            quoteOpen = c;
            state = STATE_OPEN_QUOTE;
          }
          return READ_WORD;
        }
        quoting = true;
        quoteClose = syntax.quoteCloses[c];
        if (quoteClose == UNDEF)
          throw new IOException("Quote-Close without Quote-Open");
        if (syntax.writeQuotes)
          word[wordPosition++] = values[c];
        state = STATE_IN_WORD;
        break;
      case UNDEF:
        throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
      }
    }
  }
  
  private int readLF(InputStream in, Syntax syntax) throws IOException {
    int c = in.read();
    if (c == -1) {
      if (syntax.throwEOF)
        throw new EOFException();
      state = STATE_CR;
      return READ_EOF;
    }
    if (c == 10) { // 10 == '\n'
      state = STATE_INIT;
      return READ_EOL;
    }
    throw new IOException("Missing LF");
  }
  
  public char[] getWord() {
    return word;
  }
  
  public int getWordLength() {
    return wordPosition;
  }
  
  public void resetWord() {
    wordPosition = 0;
  }
  
  /***** Recyclable *****/
  
  public boolean isValid() {
    return true;
  }
  
  public void recycled() {
    reset();
  }
  
  /**********************/
  
  private static final byte UNDEF = 0;
  private static final byte DELIM = 1;
  private static final byte SPACE = 2;
  private static final byte CR = 3;
  private static final byte LF = 4;
  private static final byte QUOTE = 5;
  private static final byte CHAR_0 = 6;
  private static final byte CHAR_10 = 7;
  private static final byte CHAR_110 = 8;
  private static final byte CHAR_1110 = 9;
  private static final byte CHAR_11110 = 10;
  
  public static class Syntax {
    
    private byte[] codes = new byte[256];
    private byte[] quoteCloses = new byte[128];
    private char[] values = new char[128];
    private boolean writeQuotes = false;
    private boolean throwEOF = true;
    private boolean delimEOF = false;
    private boolean strictCRLF = false;
    
    public Syntax(boolean setDefaultSpaces, boolean setDefaultQuotes) {
      for (int i = 0; i < values.length; i++)
        values[i] = (char) i;
      codes['\r'] = CR;
      codes['\n'] = LF;
      if (setDefaultSpaces) {
        setSpaceCharacter(' ');
        setSpaceCharacter('\t');
      }
      if (setDefaultQuotes) {
        setQuoteCharacter('"');
        setQuoteCharacter('\'');
      }
    }
    
    // lowercases ascii 'A' to 'Z' only
    public void setLowerCase() {
      for (int i = 0; i < 26; i++)
        values['A' + i] = (char) ('a' + i);
    }
    
    // set to true by default
    public void stripQuotes(boolean stripQuotes) {
      this.writeQuotes = (!stripQuotes);
    }
    
    // set to true by default
    public void throwEOF(boolean doThrow) {
      this.throwEOF = doThrow;
    }
    
    // set EOF as a delimiter
    public void setEOFDelimiter() {
      this.throwEOF = false;
      this.delimEOF = true;
    }
    
    // set to false by default
    public void strictCRLF() {
      strictCRLF = true;
    }
    
    // set to false by default
    public void allowUTF8WordCharacters() {
      for (int i = 0x80; i < 0xC0; i++)
        codes[i] = CHAR_10;
      for (int i = 0xC0; i < 0xE0; i++)
        codes[i] = CHAR_110;
      for (int i = 0xE0; i < 0xF0; i++)
        codes[i] = CHAR_1110;
      for (int i = 0xF0; i < 0xF8; i++)
        codes[i] = CHAR_11110;
    }
    
    public void setWordCharacter(int i) {
      codes[i] = CHAR_0;
    }
    
    // set the charaters between from and to (included) as word characters, except CR and LF (if they are included in the from-to interval)
    public void setWordCharacter(int from, int to) {
      for (int i = from; i <= to; i++) {
        if (i != '\r' && i != '\n')
          codes[i] = CHAR_0;
      }
    }
    
    public void setDelimiterCharacter(int i) {
      codes[i] = DELIM;
    }
    
    public void setSpaceCharacter(int i) {
      codes[i] = SPACE;
    }
    
    public void setQuoteCharacters(int open, int close) {
      codes[open] = QUOTE;
      codes[close] = QUOTE;
      quoteCloses[open] = (byte) close;
    }
    
    public void setQuoteCharacter(int i) {
      codes[i] = QUOTE;
      quoteCloses[i] = (byte) i;
    }
    
    public void setNotAllowed(int i) {
      codes[i] = UNDEF;
      quoteCloses[i] = UNDEF;
    }
  }
  
}
