// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

// Utils
import alcatel.tess.hometop.gateways.utils.Tokenizer;

public class TestTokenizer {
  public static void main(String[] args) {
    if (args.length == 2) {
      test(args[0], args[1]);
      return;
    }
    test(";\"ga; bu\"- zo;foo=bar; ;", ";-", '\"');
    test("Basic white space", " ");
    test("   Basic   white     space  ", " ");
    test("One token-abcde -fghij- klmno - pqrst    -     uvwxy   -   ", "-");
    test("Two tokens-abcde ,fghij- klmno , pqrst    -     uvwxy   ,   ", "-,");
    test("\"Literal and two tokens\"-abcde ,fghij- klmno , pqrst - uvwxy, ", "xs-,");
    test(" \"Literal and two tokens\"-abcde ,fghij- klmno , pqrst - uvwxy, ", "xs-,", '"');
    test("<toto@tata.com>", "<@>");
    test("bob bill foo bar", "-");
  }
  
  private static void test(String s, String delims) {
    test(s, delims, -1);
  }
  
  private static void test(String s, String delims, int charToTrim) {
    System.out.println("\nTokenizing string '" + s + "' with delims = '" + delims + "'");
    Tokenizer lexer = Tokenizer.acquire(s);
    
    while (lexer.nextToken(delims)) {
      String token = null;
      if (charToTrim == -1) {
        token = lexer.token();
      } else {
        token = lexer.trimToken(charToTrim);
      }
      
      int delim = lexer.delimiter();
      System.out.print("    Got token '" + token + "'");
      if (delim == -1) {
        System.out.println(" (Token not found)");
      } else {
        System.out.println(" (Token = '" + (char) delim + "')");
      }
    }
    
    Tokenizer.release(lexer);
  }
}
