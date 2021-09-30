package alcatel.tess.hometop.gateways.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class InputStreamTokenizerUtils {
  
  private InputStreamTokenizerUtils() {
  }
  
  public static int getWordAsInt(InputStreamTokenizer st, int def) {
    int n = 0;
    char c;
    char[] word = st.getWord();
    int len = st.getWordLength();
    for (int i = 0; i < len; i++) {
      c = word[i];
      n *= 10;
      switch (c) {
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        n += c - '0';
      case '0':
        break;
      default:
        return def;
      }
    }
    return n;
  }
  
  public static boolean compareWord(InputStreamTokenizer st, String comparee) {
    char[] word = st.getWord();
    int len = st.getWordLength();
    if (comparee.length() != len)
      return false;
    for (int i = 0; i < len; i++) {
      if (word[i] != comparee.charAt(i))
        return false;
    }
    return true;
  }
  
  public static void main(String[] args) throws IOException {
    
    InputStreamTokenizer.Syntax syntax = new InputStreamTokenizer.Syntax(true, true);
    syntax.setWordCharacter('a', 'z');
    syntax.setWordCharacter('A', 'Z');
    syntax.setWordCharacter('0', '9');
    syntax.setWordCharacter('-');
    syntax.setDelimiterCharacter(':');
    syntax.setQuoteCharacters('(', ')');
    syntax.setQuoteCharacters('<', '>');
    syntax.setLowerCase();
    syntax.allowUTF8WordCharacters();
    
    InputStreamTokenizer st = new InputStreamTokenizer();
    
    String s = "test2\u20AC\u1110test1\u1234testaccent\u00E9test\n";
    ByteArrayInputStream bin = new ByteArrayInputStream(s.getBytes("utf8"));
    if (st.read(bin, syntax) != InputStreamTokenizer.READ_WORD)
      throw new IOException();
    String ss = new String(st.getWord(), 0, st.getWordLength()) + "\n";
    if (ss.equals(s) == false)
      throw new IOException();
    
    System.err.println("UTF-8 OK : " + ss);
    
    st.reset();
    while (true) {
      int i = st.read(System.in, syntax);
      switch (i) {
      case InputStreamTokenizer.READ_WORD:
        System.err.println("word=" + new String(st.getWord(), 0, st.getWordLength()));
        st.resetWord();
        break;
      case InputStreamTokenizer.READ_EOL:
        System.err.println("EOL");
        break;
      case InputStreamTokenizer.READ_EOF:
        System.err.println("EOF");
        break;
      default:
        System.err.println("delim=" + (char) i);
      }
    }
  }
}
