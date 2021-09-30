package com.alcatel.as.ioh.engine.tools;

import java.io.*;

public class MyStreamTokenizer {

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

    private int wordMaxLen;
    private int state;
    private char[] word;
    private int wordPosition = 0;
    private int currentDelim = 0;
    private boolean quoting;
    private int quoteOpen, quoteClose;
    private boolean inUTF8 = false;
    private int utf8Cache = 0;
    private int utf8State = 0;

    public MyStreamTokenizer() {
	this(512);
    }
    public MyStreamTokenizer(int wordBufferLen) {
	this (wordBufferLen, wordBufferLen);
    }
    public MyStreamTokenizer(int wordBufferLenStart, int wordBufferLenMax) {
	word = new char[wordBufferLenStart];
	wordMaxLen = wordBufferLenMax;
	reset();
    }

    public void reset() {
	state = STATE_INIT;
	wordPosition = 0;
	quoting = false;
	inUTF8 = false;
    }

    /**
     * @return true if the constant was read, false is EOF was met before (and throwEOF = false), throws an IOException if the value read does not match the constant
     */
    public boolean readConstant(InputStream in, byte[] constant, boolean throwEOF)
	throws IOException {
	if (state != STATE_INIT)
	    throw new IOException("Invalid stream state");
	int len = constant.length;
	for (int i = 0; i < len; i++) {
	    int c = in.read();
	    if (c == -1) {
		if (throwEOF)
		    throw new EOFException();

		return false;
	    }
	    if (c != (constant[i] & 0xFF))
		throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
	}
	return true;
    }
    public boolean readConstant(InputStream in, int[] constant, boolean throwEOF)
	throws IOException {
	if (state != STATE_INIT)
	    throw new IOException("Invalid stream state");
	int len = constant.length;
	for (int i = 0; i < len; i++) {
	    int c = in.read();
	    if (c == -1) {
		if (throwEOF)
		    throw new EOFException();

		return false;
	    }
	    if (c != constant[i])
		throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
	}
	return true;
    }

    /**
     * @return true if the byte was read, or false if EOF was met before and throwEOF==false
     * note that it is possible to use -1 (EOF) as an expected byte.
     */
    public boolean readByte(InputStream in, int constant, boolean throwEOF)
	throws IOException {
	if (state != STATE_INIT)
	    throw new IOException("Invalid stream state");
	int c = in.read();
	if (c == constant) return true;
	if (c == -1) {
	    if (throwEOF)
		throw new EOFException();

	    return false;
	}
	throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
    }

    public int read(InputStream in, Syntax syntax)
	throws IOException {
	switch (state) {
	case STATE_OPEN_QUOTE:
	    appendWord(syntax.values[quoteOpen]);
	    state = STATE_IN_WORD; // fixed from PoC version
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
	// from here, the possible states are STATE_INIT or STATE_IN_WORD and possibly inUTF8 and/or quoting
	int c;
	char[] values = syntax.values;
	while (true) {
	    c = in.read();
	    if (c == -1) {
		if (syntax.throwEOF)
		    throw new EOFException();
		if (syntax.delimEOF){
		    if (inUTF8)
			throw new IOException("Invalid UTF-8 character");
		    if (quoting)
			throw new IOException("Missing Quote-Close");
		    if (state == STATE_IN_WORD) {
			state = STATE_EOF;
			return READ_WORD;
		    }
		    // we know state = STATE_INIT;
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
			if (syntax.ignoreWord)
			    wordPosition++;
			else
			    appendWord((char) utf8Cache);
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
		if (syntax.ignoreWord)
		    wordPosition++;
		else
		    appendWord(values[c]);
		state = STATE_IN_WORD;
		break;
	    case DELIM:
		if (quoting) {
		    appendWord(values[c]);
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
		    appendWord(values[c]);
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
		state = STATE_CR;
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
			    appendWord(values[c]);
			quoting = false;
			state = STATE_INIT;
			return READ_WORD;
		    }
		    appendWord(values[c]);
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
		    appendWord(values[c]);
		state = STATE_IN_WORD;
		break;
	    case UNDEF:
		throw new IOException("Unexpected byte : " + c + " (character=" + ((char) c) + ")");
	    }
	}
    }
    
    private int readLF(InputStream in, Syntax syntax)
	throws IOException {
	int c = in.read();
	if (c == -1) {
	    if (syntax.throwEOF)
		throw new EOFException();
	    return READ_EOF;
	}
	if (c == 10) { // 10 == '\n'
	    state = STATE_INIT;
	    return READ_EOL;
	}
	throw new IOException("Missing LF: found "+(char)c+" (code="+c+")");
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

    public void appendWord (char c){
	if (wordPosition == word.length){
	    if (word.length == wordMaxLen) throw new RuntimeException ("Input word too large");
	    char[] chars = new char[wordMaxLen];
	    System.arraycopy (word, 0, chars, 0, word.length);
	    word = chars;
	}
	word[wordPosition++] = c;
    }

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
	private boolean throwEOF = false;
	private boolean delimEOF = false;
	private boolean strictCRLF = false;
	private boolean ignoreWord = false;

	public Syntax(){
	    for (int i = 0; i < values.length; i++)
		values[i] = (char) i;
	    codes['\r'] = CR;
	    codes['\n'] = LF;
	}
	    
	public Syntax(boolean alpha, boolean digits, boolean setDefaultSpaces, boolean setDefaultQuotes, boolean toLowerCase, boolean throwEOF) {
	    this ();
	    if (setDefaultSpaces) setSpaceCharacter(' ', '\t');
	    if (setDefaultQuotes) setQuoteCharacter('"', '\'');
	    if (alpha) setWordCharacters ('A', 'Z').setWordCharacters ('a', 'z');
	    if (digits) setWordCharacters ('0', '9');
	    if (toLowerCase) setLowerCase ();
	    throwEOF (throwEOF);
	}

	public Syntax (Syntax other){
	    System.arraycopy (other.codes, 0, codes, 0, codes.length);
	    System.arraycopy (other.quoteCloses, 0, quoteCloses, 0, quoteCloses.length);
	    System.arraycopy (other.values, 0, values, 0, values.length);
	    writeQuotes = other.writeQuotes;
	    throwEOF = other.throwEOF;
	    delimEOF = other.delimEOF;
	    strictCRLF = other.strictCRLF;
	    ignoreWord = other.ignoreWord;
	}

	// lowercases ascii 'A' to 'Z' only
	public Syntax setLowerCase() {
	    for (int i = 0; i < 26; i++)
		values['A' + i] = (char) ('a' + i);
	    return this;
	}

	// uppercase ascii 'A' to 'Z' only
	public Syntax setUpperCase() {
	    for (int i = 0; i < 26; i++)
		values['a' + i] = (char) ('A' + i);
	    return this;
	}

	// set to true by default
	public Syntax stripQuotes(boolean stripQuotes) {
	    this.writeQuotes = (!stripQuotes);
	    return this;
	}

	// set to true by default
	public Syntax throwEOF(boolean doThrow) {
	    this.throwEOF = doThrow;
	    return this;
	}
	    
	// set EOF as a delimiter
	public Syntax setEOFDelimiter (){
	    this.throwEOF = false;
	    this.delimEOF = true;
	    return this;
	}

	// set to false by default
	public Syntax strictCRLF() {
	    strictCRLF = true;
	    return this;
	}

	// set to false by default : only an optimization (not checked all over the place for lisibility)
	public Syntax ignoreWord() {
	    ignoreWord = true;
	    return this;
	}

	// set to false by default
	public Syntax allowUTF8WordCharacters() {
	    for (int i = 0x80; i < 0xC0; i++)
		codes[i] = CHAR_10;
	    for (int i = 0xC0; i < 0xE0; i++)
		codes[i] = CHAR_110;
	    for (int i = 0xE0; i < 0xF0; i++)
		codes[i] = CHAR_1110;
	    for (int i = 0xF0; i < 0xF8; i++)
		codes[i] = CHAR_11110;
	    return this;
	}

	public Syntax setWordCharacter(int... chars) {
	    for (int c: chars) codes[c] = CHAR_0;
	    return this;
	}

	// set the charaters between from and to (included) as word characters, except CR and LF (if they are included in the from-to interval)
	public Syntax setWordCharacters(int from, int to) {
	    for (int i = from; i <= to; i++) {
		if (i != '\r' && i != '\n')
		    codes[i] = CHAR_0;
	    }
	    return this;
	}
		
	public Syntax setDelimiterCharacter(int... chars) {
	    for (int c : chars) codes[c] = DELIM;
	    return this;
	}

	public Syntax setSpaceCharacter(int... chars) {
	    for (int c: chars) codes[c] = SPACE;
	    return this;
	}

	public Syntax setQuoteCharacters(int open, int close) {
	    codes[open] = QUOTE;
	    codes[close] = QUOTE;
	    quoteCloses[open] = (byte) close;
	    return this;
	}

	public Syntax setQuoteCharacter(int... chars) {
	    for (int c: chars){
		codes[c] = QUOTE;
		quoteCloses[c] = (byte) c;
	    }
	    return this;
	}

	public Syntax setNotAllowed(int... chars) {
	    for (int c: chars){
		codes[c] = UNDEF;
		quoteCloses[c] = UNDEF;
	    }
	    return this;
	}
    }

    public static void main(String[] args)
	throws IOException {

	Syntax syntax = new Syntax(true, true, true, true, true, true);
	syntax.setWordCharacter('-');
	syntax.setDelimiterCharacter(':');
	syntax.setQuoteCharacters('(', ')');
	syntax.setQuoteCharacters('<', '>');
	syntax.allowUTF8WordCharacters();

	MyStreamTokenizer st = new MyStreamTokenizer();

	String s = "test2\u20AC\u1110test1\u1234testaccent\u00E9test\n";
	ByteArrayInputStream bin = new ByteArrayInputStream(s.getBytes("utf8"));
	if (st.read(bin, syntax) != READ_WORD)
	    throw new IOException();
	String ss = new String(st.getWord(), 0, st.getWordLength()) + "\n";
	if (ss.equals(s) == false)
	    throw new IOException();

	System.err.println("UTF-8 OK : " + ss);

	st.reset();
	while (true) {
	    int i = st.read(System.in, syntax);
	    switch (i) {
	    case READ_WORD:
		System.err.println("word=" + new String(st.getWord(), 0, st.getWordLength()));
		st.resetWord();
		break;
	    case READ_EOL:
		System.err.println("EOL");
		break;
	    case READ_EOF:
		System.err.println("EOF");
		break;
	    default:
		System.err.println("delim=" + (char) i);
	    }
	}
    }
}
