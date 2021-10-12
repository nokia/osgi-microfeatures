// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.util;

public class ISO8859 {
  
  public static String ISOtoHTML(int i) {
    if (i >= 0 && i <= 255)
      return theEntities[i];
    else
      return "";
  }
  
  public static String HTMLtoISO(String s) {
    char c = (char) HTMLtoISOChar(s);
    if (c == 0)
      return s;
    else {
      StringBuffer ret = new StringBuffer(1);
      ret.append(c);
      return ret.toString();
    }
  }
  
  private static int HTMLtoISOChar(String s) {
    char c = s.charAt(1);
    char[] res;
    if (s.charAt(0) != '&')
      return 0;
    if (Character.isDigit(s.charAt(1))) {
      int i = Integer.valueOf(s.substring(1, s.indexOf(59, 2))).intValue();
      return i;
    }
    switch (c) {
    case 121: /* 'y' */
      if (s.startsWith("yacute;", 1))
        return 253;
      if (s.startsWith("yuml;", 1))
        return 255;
      if (s.startsWith("yen;", 1))
        return 165;
      // fall through
      
    case 99: /* 'c' */
      if (s.startsWith("ccedil;", 1))
        return 231;
      if (s.startsWith("comma;", 1))
        return 44;
      if (s.startsWith("colon;", 1))
        return 58;
      if (s.startsWith("commat;", 1))
        return 64;
      if (s.startsWith("circ;", 1))
        return 94;
      if (s.startsWith("caret;", 1))
        return 94;
      if (s.startsWith("cent;", 1))
        return 162;
      if (s.startsWith("curren;", 1))
        return 164;
      if (s.startsWith("copy;", 1))
        return 169;
      if (s.startsWith("cedil;", 1))
        return 184;
      // fall through
      
    case 98: /* 'b' */
      if (s.startsWith("blank;", 1))
        return 32;
      if (s.startsWith("bsol;", 1))
        return 92;
      if (s.startsWith("brvbar;", 1))
        return 166;
      if (s.startsWith("brkbar;", 1))
        return 166;
      // fall through
      
    case 97: /* 'a' */
      if (s.startsWith("agrave;", 1))
        return 224;
      if (s.startsWith("aacute;", 1))
        return 225;
      if (s.startsWith("acirc;", 1))
        return 226;
      if (s.startsWith("atilde;", 1))
        return 227;
      if (s.startsWith("auml;", 1))
        return 228;
      if (s.startsWith("aring;", 1))
        return 229;
      if (s.startsWith("aelig;", 1))
        return 230;
      if (s.startsWith("amp;", 1))
        return 38;
      if (s.startsWith("apos;", 1))
        return 39;
      if (s.startsWith("ast;", 1))
        return 42;
      if (s.startsWith("acute;", 1))
        return 180;
      if (s.startsWith("angst;", 1))
        return 197;
      // fall through
      
    case 89: /* 'Y' */
      if (s.startsWith("Yacute;", 1))
        return 221;
      // fall through
      
    case 85: /* 'U' */
      if (s.startsWith("Ugrave;", 1))
        return 217;
      if (s.startsWith("Uacute;", 1))
        return 218;
      if (s.startsWith("Ucirc;", 1))
        return 219;
      if (s.startsWith("Uuml;", 1))
        return 220;
      // fall through
      
    case 84: /* 'T' */
      if (s.startsWith("THORN;", 1))
        return 222;
      // fall through
      
    case 79: /* 'O' */
      if (s.startsWith("Ograve;", 1))
        return 210;
      if (s.startsWith("Oacute;", 1))
        return 211;
      if (s.startsWith("Ocirc;", 1))
        return 212;
      if (s.startsWith("Otilde;", 1))
        return 213;
      if (s.startsWith("Ouml;", 1))
        return 214;
      if (s.startsWith("Oslash;", 1))
        return 216;
      // fall through
      
    case 78: /* 'N' */
      if (s.startsWith("Ntilde;", 1))
        return 209;
      // fall through
      
    case 73: /* 'I' */
      if (s.startsWith("Igrave;", 1))
        return 204;
      if (s.startsWith("Iacute;", 1))
        return 205;
      if (s.startsWith("Icirc;", 1))
        return 206;
      if (s.startsWith("Iuml;", 1))
        return 207;
      // fall through
      
    case 69: /* 'E' */
      if (s.startsWith("Egrave;", 1))
        return 200;
      if (s.startsWith("Eacute;", 1))
        return 201;
      if (s.startsWith("Ecirc;", 1))
        return 202;
      if (s.startsWith("Euml;", 1))
        return 203;
      if (s.startsWith("ETH;", 1))
        return 208;
      // fall through
      
    case 68: /* 'D' */
      if (s.startsWith("Dstrok;", 1))
        return 208;
      // fall through
      
    case 67: /* 'C' */
      if (s.startsWith("Ccedil;", 1))
        return 199;
      // fall through
      
    case 65: /* 'A' */
      if (s.startsWith("Agrave;", 1))
        return 192;
      if (s.startsWith("Aacute;", 1))
        return 193;
      if (s.startsWith("Acirc;", 1))
        return 194;
      if (s.startsWith("Atilde;", 1))
        return 195;
      if (s.startsWith("Auml;", 1))
        return 196;
      if (s.startsWith("Aring;", 1))
        return 197;
      if (s.startsWith("AElig;", 1))
        return 198;
      // fall through
      
    case 100: /* 'd' */
      if (s.startsWith("divide;", 1))
        return 247;
      if (s.startsWith("dollar;", 1))
        return 36;
      if (s.startsWith("dash;", 1))
        return 45;
      if (s.startsWith("die;", 1))
        return 168;
      if (s.startsWith("deg;", 1))
        return 176;
      // fall through
      
    case 117: /* 'u' */
      if (s.startsWith("ugrave;", 1))
        return 249;
      if (s.startsWith("uacute;", 1))
        return 250;
      if (s.startsWith("ucirc;", 1))
        return 251;
      if (s.startsWith("uuml;", 1))
        return 252;
      if (s.startsWith("uml;", 1))
        return 168;
      // fall through
      
    case 116: /* 't' */
      if (s.startsWith("thorn;", 1))
        return 254;
      if (s.startsWith("tilde;", 1))
        return 126;
      if (s.startsWith("times;", 1))
        return 215;
      // fall through
      
    case 115: /* 's' */
      if (s.startsWith("sp;", 1))
        return 32;
      if (s.startsWith("sol;", 1))
        return 47;
      if (s.startsWith("semi;", 1))
        return 59;
      if (s.startsWith("sim;", 1))
        return 126;
      if (s.startsWith("sect;", 1))
        return 167;
      if (s.startsWith("shy;", 1))
        return 173;
      if (s.startsWith("sup2;", 1))
        return 178;
      if (s.startsWith("sup3;", 1))
        return 179;
      if (s.startsWith("sup1;", 1))
        return 185;
      // fall through
      
    case 114: /* 'r' */
      if (s.startsWith("rpar;", 1))
        return 41;
      if (s.startsWith("rsqb;", 1))
        return 93;
      if (s.startsWith("rcub;", 1))
        return 125;
      if (s.startsWith("reg;", 1))
        return 174;
      if (s.startsWith("raquo;", 1))
        return 187;
      // fall through
      
    case 113: /* 'q' */
      if (s.startsWith("quot;", 1))
        return 34;
      if (s.startsWith("quest;", 1))
        return 63;
      // fall through
      
    case 112: /* 'p' */
      if (s.startsWith("percnt;", 1))
        return 37;
      if (s.startsWith("plus;", 1))
        return 43;
      if (s.startsWith("period;", 1))
        return 46;
      if (s.startsWith("pound;", 1))
        return 163;
      if (s.startsWith("plusmn;", 1))
        return 177;
      if (s.startsWith("para;", 1))
        return 182;
      // fall through
      
    case 111: /* 'o' */
      if (s.startsWith("ograve;", 1))
        return 242;
      if (s.startsWith("oacute;", 1))
        return 243;
      if (s.startsWith("ocirc;", 1))
        return 244;
      if (s.startsWith("otilde;", 1))
        return 245;
      if (s.startsWith("ouml;", 1))
        return 246;
      if (s.startsWith("oslash;", 1))
        return 248;
      if (s.startsWith("ordf;", 1))
        return 170;
      if (s.startsWith("ordm;", 1))
        return 186;
      // fall through
      
    case 110: /* 'n' */
      if (s.startsWith("ntilde;", 1))
        return 241;
      if (s.startsWith("num;", 1))
        return 35;
      if (s.startsWith("nbsp;", 1))
        return 160;
      if (s.startsWith("not;", 1))
        return 172;
      // fall through
      
    case 109: /* 'm' */
      if (s.startsWith("minus;", 1))
        return 45;
      if (s.startsWith("macr;", 1))
        return 175;
      if (s.startsWith("micro;", 1))
        return 181;
      if (s.startsWith("middot;", 1))
        return 183;
      // fall through
      
    case 108: /* 'l' */
      if (s.startsWith("lpar;", 1))
        return 40;
      if (s.startsWith("lt;", 1))
        return 60;
      if (s.startsWith("lsqb;", 1))
        return 91;
      if (s.startsWith("lowbar;", 1))
        return 95;
      if (s.startsWith("lcub;", 1))
        return 123;
      if (s.startsWith("laquo;", 1))
        return 171;
      // fall through
      
    case 105: /* 'i' */
      if (s.startsWith("igrave;", 1))
        return 236;
      if (s.startsWith("iacute;", 1))
        return 237;
      if (s.startsWith("icirc;", 1))
        return 238;
      if (s.startsWith("iuml;", 1))
        return 239;
      if (s.startsWith("iexcl;", 1))
        return 161;
      if (s.startsWith("iquest;", 1))
        return 191;
      // fall through
      
    case 104: /* 'h' */
      if (s.startsWith("hyphen;", 1))
        return 45;
      if (s.startsWith("hibar;", 1))
        return 175;
      if (s.startsWith("half;", 1))
        return 189;
      // fall through
      
    case 103: /* 'g' */
      if (s.startsWith("gt;", 1))
        return 62;
      if (s.startsWith("grave;", 1))
        return 96;
      // fall through
      
    case 102: /* 'f' */
      if (s.startsWith("frac14;", 1))
        return 188;
      if (s.startsWith("frac12;", 1))
        return 189;
      if (s.startsWith("frac34;", 1))
        return 190;
      // fall through
      
    case 101: /* 'e' */
      if (s.startsWith("egrave;", 1))
        return 232;
      if (s.startsWith("eacute;", 1))
        return 233;
      if (s.startsWith("ecirc;", 1))
        return 234;
      if (s.startsWith("euml;", 1))
        return 235;
      if (s.startsWith("eth;", 1))
        return 240;
      if (s.startsWith("excl;", 1))
        return 33;
      if (s.startsWith("equals;", 1))
        return 61;
      // fall through
      
    default:
      return 0;
      
    }
  }
  
  static String theEntities[] = { "&#000;", "&#001;", "&#002;", "&#003;", "&#004;", "&#005;", "&#006;",
      "&#007;", "&#008;", "&#009;", "&#010;", "&#011;", "&#012;", "&#013;", "&#014;", "&#015;", "&#016;",
      "&#017;", "&#018;", "&#019;", "&#020;", "&#021;", "&#022;", "&#023;", "&#024;", "&#025;", "&#026;",
      "&#027;", "&#028;", "&#029;", "&#030;", "&#031;", "&sp;", "&excl;", "&quot;", "&num;", "&dollar;",
      "&percnt;", "&amp;", "&apos;", "&lpar;", "&rpar;", "&ast;", "&plus;", "&comma;", "&hyphen;",
      "&period;", "&sol;", "&#048;", "&#049;", "&#050;", "&#051;", "&#052;", "&#053;", "&#054;", "&#055;",
      "&#056;", "&#057;", "&colon;", "&semi;", "&lt;", "&equals;", "&gt;", "&quest;", "&commat;", "&#065;",
      "&#066;", "&#067;", "&#068;", "&#069;", "&#070;", "&#071;", "&#072;", "&#073;", "&#074;", "&#075;",
      "&#076;", "&#077;", "&#078;", "&#079;", "&#080;", "&#081;", "&#082;", "&#083;", "&#084;", "&#085;",
      "&#086;", "&#087;", "&#088;", "&#089;", "&#090;", "&lsqb;", "&bsol;", "&rsqb;", "&circ;", "&lowbar;",
      "&grave;", "&#097;", "&#098;", "&#099;", "&#100;", "&#101;", "&#102;", "&#103;", "&#104;", "&#105;",
      "&#106;", "&#107;", "&#108;", "&#109;", "&#110;", "&#111;", "&#112;", "&#113;", "&#114;", "&#115;",
      "&#116;", "&#117;", "&#118;", "&#119;", "&#120;", "&#121;", "&#122;", "&lcub;", "&verbar;", "&rcub;",
      "&tilde;", "&#127;", "&#128;", "&#129;", "&#130;", "&#131;", "&#132;", "&#133;", "&#134;", "&#135;",
      "&#136;", "&#137;", "&#138;", "&#139;", "&#140;", "&#141;", "&#142;", "&#143;", "&#144;", "&#145;",
      "&#146;", "&#147;", "&#148;", "&#149;", "&#150;", "&#151;", "&#152;", "&#153;", "&#154;", "&#155;",
      "&#156;", "&#157;", "&#158;", "&#159;", "&nbsp;", "&iexcl;", "&cent;", "&pound;", "&curren;", "&yen;",
      "&brvbar;", "&sect;", "&uml;", "&copy;", "&ordf;", "&laquo;", "&not;", "&shy;", "&reg;", "&hibar;",
      "&deg;", "&plusmn;", "&sup2;", "&sup3;", "&acute;", "&micro;", "&para;", "&middot;", "&cedil;",
      "&sup1;", "&ordm;", "&raquo;", "&frac14;", "&frac12;", "&frac34;", "&iquest;", "&Agrave;", "&Aacute;",
      "&Acirc;", "&Atilde;", "&Auml;", "&Aring;", "&AElig;", "&Ccedil;", "&Egrave;", "&Eacute;", "&Ecirc;",
      "&Euml;", "&Igrave;", "&Iacute;", "&Icirc;", "&Iuml;", "&ETH;", "&Ntilde;", "&Ograve;", "&Oacute;",
      "&Ocirc;", "&Otilde;", "&Ouml;", "&times;", "&Oslash;", "&Ugrave;", "&Uacute;", "&Ucirc;", "&Uuml;",
      "&Yacute;", "&THORN;", "&szlig;", "&agrave;", "&aacute;", "&acirc;", "&atilde;", "&auml;", "&aring;",
      "&aelig;", "&ccedil;", "&egrave;", "&eacute;", "&ecirc;", "&euml;", "&igrave;", "&iacute;", "&icirc;",
      "&iuml;", "&eth;", "&ntilde;", "&ograve;", "&oacute;", "&ocirc;", "&otilde;", "&ouml;", "&divide;",
      "&oslash;", "&ugrave;", "&uacute;", "&ucirc;", "&uuml;", "&yacute;", "&thorn;", "&yuml;" };
}
