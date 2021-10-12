// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import java.nio.ByteBuffer;

public final class HuffmanDecoder {
    // XXX: rename to EOS
    public static final int EOF     = 256;
    public static final int SUSPEND = 257;

    public interface In {
        int fetchByte();

        void advance(int len);

        void rewind(int len);

        boolean isEos();
    }
    ;

    static final public void parse(In in, ByteBuffer out) {
        while (!in.isEos()) {
            parse_one(in, out);
        }
    }

    static final public int parse_one(In in, ByteBuffer out) {
        int ret = EOF;
        switch (in.fetchByte()) {

            /* nb_bytes:0, inlining:0 */
            /* plain:48, name_value_code[0]/5 */
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                out.put((byte)48);
                ret = 48;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:49, name_value_code[8]/5 */
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                out.put((byte)49);
                ret = 49;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:50, name_value_code[10]/5 */
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                out.put((byte)50);
                ret = 50;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:97, name_value_code[18]/5 */
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
                out.put((byte)97);
                ret = 97;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:99, name_value_code[20]/5 */
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
                out.put((byte)99);
                ret = 99;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:101, name_value_code[28]/5 */
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
                out.put((byte)101);
                ret = 101;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:105, name_value_code[30]/5 */
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
                out.put((byte)105);
                ret = 105;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:111, name_value_code[38]/5 */
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
                out.put((byte)111);
                ret = 111;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:115, name_value_code[40]/5 */
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
                out.put((byte)115);
                ret = 115;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:116, name_value_code[48]/5 */
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
                out.put((byte)116);
                ret = 116;
                in.advance(5);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:32, name_value_code[50]/6 */
            case 80:
            case 81:
            case 82:
            case 83:
                out.put((byte)32);
                ret = 32;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:37, name_value_code[54]/6 */
            case 84:
            case 85:
            case 86:
            case 87:
                out.put((byte)37);
                ret = 37;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:45, name_value_code[58]/6 */
            case 88:
            case 89:
            case 90:
            case 91:
                out.put((byte)45);
                ret = 45;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:46, name_value_code[5c]/6 */
            case 92:
            case 93:
            case 94:
            case 95:
                out.put((byte)46);
                ret = 46;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:47, name_value_code[60]/6 */
            case 96:
            case 97:
            case 98:
            case 99:
                out.put((byte)47);
                ret = 47;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:51, name_value_code[64]/6 */
            case 100:
            case 101:
            case 102:
            case 103:
                out.put((byte)51);
                ret = 51;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:52, name_value_code[68]/6 */
            case 104:
            case 105:
            case 106:
            case 107:
                out.put((byte)52);
                ret = 52;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:53, name_value_code[6c]/6 */
            case 108:
            case 109:
            case 110:
            case 111:
                out.put((byte)53);
                ret = 53;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:54, name_value_code[70]/6 */
            case 112:
            case 113:
            case 114:
            case 115:
                out.put((byte)54);
                ret = 54;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:55, name_value_code[74]/6 */
            case 116:
            case 117:
            case 118:
            case 119:
                out.put((byte)55);
                ret = 55;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:56, name_value_code[78]/6 */
            case 120:
            case 121:
            case 122:
            case 123:
                out.put((byte)56);
                ret = 56;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:57, name_value_code[7c]/6 */
            case 124:
            case 125:
            case 126:
            case 127:
                out.put((byte)57);
                ret = 57;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:61, name_value_code[80]/6 */
            case 128:
            case 129:
            case 130:
            case 131:
                out.put((byte)61);
                ret = 61;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:65, name_value_code[84]/6 */
            case 132:
            case 133:
            case 134:
            case 135:
                out.put((byte)65);
                ret = 65;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:95, name_value_code[88]/6 */
            case 136:
            case 137:
            case 138:
            case 139:
                out.put((byte)95);
                ret = 95;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:98, name_value_code[8c]/6 */
            case 140:
            case 141:
            case 142:
            case 143:
                out.put((byte)98);
                ret = 98;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:100, name_value_code[90]/6 */
            case 144:
            case 145:
            case 146:
            case 147:
                out.put((byte)100);
                ret = 100;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:102, name_value_code[94]/6 */
            case 148:
            case 149:
            case 150:
            case 151:
                out.put((byte)102);
                ret = 102;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:103, name_value_code[98]/6 */
            case 152:
            case 153:
            case 154:
            case 155:
                out.put((byte)103);
                ret = 103;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:104, name_value_code[9c]/6 */
            case 156:
            case 157:
            case 158:
            case 159:
                out.put((byte)104);
                ret = 104;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:108, name_value_code[a0]/6 */
            case 160:
            case 161:
            case 162:
            case 163:
                out.put((byte)108);
                ret = 108;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:109, name_value_code[a4]/6 */
            case 164:
            case 165:
            case 166:
            case 167:
                out.put((byte)109);
                ret = 109;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:110, name_value_code[a8]/6 */
            case 168:
            case 169:
            case 170:
            case 171:
                out.put((byte)110);
                ret = 110;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:112, name_value_code[ac]/6 */
            case 172:
            case 173:
            case 174:
            case 175:
                out.put((byte)112);
                ret = 112;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:114, name_value_code[b0]/6 */
            case 176:
            case 177:
            case 178:
            case 179:
                out.put((byte)114);
                ret = 114;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:117, name_value_code[b4]/6 */
            case 180:
            case 181:
            case 182:
            case 183:
                out.put((byte)117);
                ret = 117;
                in.advance(6);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:58, name_value_code[b8]/7 */
            case 184:
            case 185:
                out.put((byte)58);
                ret = 58;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:66, name_value_code[ba]/7 */
            case 186:
            case 187:
                out.put((byte)66);
                ret = 66;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:67, name_value_code[bc]/7 */
            case 188:
            case 189:
                out.put((byte)67);
                ret = 67;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:68, name_value_code[be]/7 */
            case 190:
            case 191:
                out.put((byte)68);
                ret = 68;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:69, name_value_code[c0]/7 */
            case 192:
            case 193:
                out.put((byte)69);
                ret = 69;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:70, name_value_code[c2]/7 */
            case 194:
            case 195:
                out.put((byte)70);
                ret = 70;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:71, name_value_code[c4]/7 */
            case 196:
            case 197:
                out.put((byte)71);
                ret = 71;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:72, name_value_code[c6]/7 */
            case 198:
            case 199:
                out.put((byte)72);
                ret = 72;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:73, name_value_code[c8]/7 */
            case 200:
            case 201:
                out.put((byte)73);
                ret = 73;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:74, name_value_code[ca]/7 */
            case 202:
            case 203:
                out.put((byte)74);
                ret = 74;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:75, name_value_code[cc]/7 */
            case 204:
            case 205:
                out.put((byte)75);
                ret = 75;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:76, name_value_code[ce]/7 */
            case 206:
            case 207:
                out.put((byte)76);
                ret = 76;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:77, name_value_code[d0]/7 */
            case 208:
            case 209:
                out.put((byte)77);
                ret = 77;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:78, name_value_code[d2]/7 */
            case 210:
            case 211:
                out.put((byte)78);
                ret = 78;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:79, name_value_code[d4]/7 */
            case 212:
            case 213:
                out.put((byte)79);
                ret = 79;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:80, name_value_code[d6]/7 */
            case 214:
            case 215:
                out.put((byte)80);
                ret = 80;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:81, name_value_code[d8]/7 */
            case 216:
            case 217:
                out.put((byte)81);
                ret = 81;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:82, name_value_code[da]/7 */
            case 218:
            case 219:
                out.put((byte)82);
                ret = 82;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:83, name_value_code[dc]/7 */
            case 220:
            case 221:
                out.put((byte)83);
                ret = 83;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:84, name_value_code[de]/7 */
            case 222:
            case 223:
                out.put((byte)84);
                ret = 84;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:85, name_value_code[e0]/7 */
            case 224:
            case 225:
                out.put((byte)85);
                ret = 85;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:86, name_value_code[e2]/7 */
            case 226:
            case 227:
                out.put((byte)86);
                ret = 86;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:87, name_value_code[e4]/7 */
            case 228:
            case 229:
                out.put((byte)87);
                ret = 87;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:89, name_value_code[e6]/7 */
            case 230:
            case 231:
                out.put((byte)89);
                ret = 89;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:106, name_value_code[e8]/7 */
            case 232:
            case 233:
                out.put((byte)106);
                ret = 106;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:107, name_value_code[ea]/7 */
            case 234:
            case 235:
                out.put((byte)107);
                ret = 107;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:113, name_value_code[ec]/7 */
            case 236:
            case 237:
                out.put((byte)113);
                ret = 113;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:118, name_value_code[ee]/7 */
            case 238:
            case 239:
                out.put((byte)118);
                ret = 118;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:119, name_value_code[f0]/7 */
            case 240:
            case 241:
                out.put((byte)119);
                ret = 119;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:120, name_value_code[f2]/7 */
            case 242:
            case 243:
                out.put((byte)120);
                ret = 120;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:121, name_value_code[f4]/7 */
            case 244:
            case 245:
                out.put((byte)121);
                ret = 121;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:122, name_value_code[f6]/7 */
            case 246:
            case 247:
                out.put((byte)122);
                ret = 122;
                in.advance(7);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:38, name_value_code[f8]/8 */
            case 248:
                out.put((byte)38);
                ret = 38;
                in.advance(8);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:42, name_value_code[f9]/8 */
            case 249:
                out.put((byte)42);
                ret = 42;
                in.advance(8);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:44, name_value_code[fa]/8 */
            case 250:
                out.put((byte)44);
                ret = 44;
                in.advance(8);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:59, name_value_code[fb]/8 */
            case 251:
                out.put((byte)59);
                ret = 59;
                in.advance(8);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:88, name_value_code[fc]/8 */
            case 252:
                out.put((byte)88);
                ret = 88;
                in.advance(8);
                break;
            /* nb_bytes:0, inlining:0 */
            /* plain:90, name_value_code[fd]/8 */
            case 253:
                out.put((byte)90);
                ret = 90;
                in.advance(8);
                break;

            // inlining 254
            // increase temporarily nb_bytes to 1
            case 254:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:1, inlining:254 */
                    /* plain:33, name_value_code[fe,0]/10 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)33);
                        ret = 33;
                        in.advance(2);
                        break;
                    /* nb_bytes:1, inlining:254 */
                    /* plain:34, name_value_code[fe,40]/10 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)34);
                        ret = 34;
                        in.advance(2);
                        break;
                    /* nb_bytes:1, inlining:254 */
                    /* plain:40, name_value_code[fe,80]/10 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)40);
                        ret = 40;
                        in.advance(2);
                        break;
                    /* nb_bytes:1, inlining:254 */
                    /* plain:41, name_value_code[fe,c0]/10 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)41);
                        ret = 41;
                        in.advance(2);
                        break;

                    // closing inline of 254
                    //1 decrease to nb_bytes: 0
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(0 + 1);
                        break;

                }
                break;


            // increase nb_bytes : 1
            case 0xFF:
                in.advance(8);
                ret = parse_ff_1(in, out);
                break;
            case EOF:
                ret = EOF;
                break;
            case SUSPEND:
                in.rewind(1);
                ret = SUSPEND;
                break;
            //default:
            //  ret = EOF;
            //  break;
        }
        return ret;
    }

    static final public int parse_ff_1(In in, ByteBuffer out) {
        int ret = EOF;
        // increase nb_bytes : 1
        switch (in.fetchByte()) {

            /* nb_bytes:1, inlining:0 */
            /* plain:63, name_value_code[ff,0]/10 */
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
                out.put((byte)63);
                ret = 63;
                in.advance(2);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:39, name_value_code[ff,40]/11 */
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                out.put((byte)39);
                ret = 39;
                in.advance(3);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:43, name_value_code[ff,60]/11 */
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
                out.put((byte)43);
                ret = 43;
                in.advance(3);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:124, name_value_code[ff,80]/11 */
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
                out.put((byte)124);
                ret = 124;
                in.advance(3);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:35, name_value_code[ff,a0]/12 */
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 172:
            case 173:
            case 174:
            case 175:
                out.put((byte)35);
                ret = 35;
                in.advance(4);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:62, name_value_code[ff,b0]/12 */
            case 176:
            case 177:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            case 190:
            case 191:
                out.put((byte)62);
                ret = 62;
                in.advance(4);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:0, name_value_code[ff,c0]/13 */
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
                out.put((byte)0);
                ret = 0;
                in.advance(5);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:36, name_value_code[ff,c8]/13 */
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206:
            case 207:
                out.put((byte)36);
                ret = 36;
                in.advance(5);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:64, name_value_code[ff,d0]/13 */
            case 208:
            case 209:
            case 210:
            case 211:
            case 212:
            case 213:
            case 214:
            case 215:
                out.put((byte)64);
                ret = 64;
                in.advance(5);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:91, name_value_code[ff,d8]/13 */
            case 216:
            case 217:
            case 218:
            case 219:
            case 220:
            case 221:
            case 222:
            case 223:
                out.put((byte)91);
                ret = 91;
                in.advance(5);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:93, name_value_code[ff,e0]/13 */
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
                out.put((byte)93);
                ret = 93;
                in.advance(5);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:126, name_value_code[ff,e8]/13 */
            case 232:
            case 233:
            case 234:
            case 235:
            case 236:
            case 237:
            case 238:
            case 239:
                out.put((byte)126);
                ret = 126;
                in.advance(5);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:94, name_value_code[ff,f0]/14 */
            case 240:
            case 241:
            case 242:
            case 243:
                out.put((byte)94);
                ret = 94;
                in.advance(6);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:125, name_value_code[ff,f4]/14 */
            case 244:
            case 245:
            case 246:
            case 247:
                out.put((byte)125);
                ret = 125;
                in.advance(6);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:60, name_value_code[ff,f8]/15 */
            case 248:
            case 249:
                out.put((byte)60);
                ret = 60;
                in.advance(7);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:96, name_value_code[ff,fa]/15 */
            case 250:
            case 251:
                out.put((byte)96);
                ret = 96;
                in.advance(7);
                break;
            /* nb_bytes:1, inlining:0 */
            /* plain:123, name_value_code[ff,fc]/15 */
            case 252:
            case 253:
                out.put((byte)123);
                ret = 123;
                in.advance(7);
                break;

            // inlining 254
            // increase temporarily nb_bytes to 2
            case 254:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:2, inlining:254 */
                    /* plain:92, name_value_code[ff,fe,0]/19 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                        out.put((byte)92);
                        ret = 92;
                        in.advance(3);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:195, name_value_code[ff,fe,20]/19 */
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)195);
                        ret = 195;
                        in.advance(3);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:208, name_value_code[ff,fe,40]/19 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                        out.put((byte)208);
                        ret = 208;
                        in.advance(3);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:128, name_value_code[ff,fe,60]/20 */
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                        out.put((byte)128);
                        ret = 128;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:130, name_value_code[ff,fe,70]/20 */
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)130);
                        ret = 130;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:131, name_value_code[ff,fe,80]/20 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                        out.put((byte)131);
                        ret = 131;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:162, name_value_code[ff,fe,90]/20 */
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                        out.put((byte)162);
                        ret = 162;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:184, name_value_code[ff,fe,a0]/20 */
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                        out.put((byte)184);
                        ret = 184;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:194, name_value_code[ff,fe,b0]/20 */
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)194);
                        ret = 194;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:224, name_value_code[ff,fe,c0]/20 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                        out.put((byte)224);
                        ret = 224;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:226, name_value_code[ff,fe,d0]/20 */
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                        out.put((byte)226);
                        ret = 226;
                        in.advance(4);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:153, name_value_code[ff,fe,e0]/21 */
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                        out.put((byte)153);
                        ret = 153;
                        in.advance(5);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:161, name_value_code[ff,fe,e8]/21 */
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                        out.put((byte)161);
                        ret = 161;
                        in.advance(5);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:167, name_value_code[ff,fe,f0]/21 */
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                        out.put((byte)167);
                        ret = 167;
                        in.advance(5);
                        break;
                    /* nb_bytes:2, inlining:254 */
                    /* plain:172, name_value_code[ff,fe,f8]/21 */
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)172);
                        ret = 172;
                        in.advance(5);
                        break;

                    // closing inline of 254
                    //1 decrease to nb_bytes: 1
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(1 + 1);
                        break;

                }
                break;


            // increase nb_bytes : 2
            case 0xFF:
                in.advance(8);
                ret = parse_ff_2(in, out);
                break;
            case EOF:
                ret = EOF;
                break;
            case SUSPEND:
                in.rewind(2);
                ret = SUSPEND;
                break;
            //default:
            //  ret = EOF;
            //  break;
        }
        return ret;
    }

    static final public int parse_ff_2(In in, ByteBuffer out) {
        int ret = EOF;
        // increase nb_bytes : 2
        switch (in.fetchByte()) {

            /* nb_bytes:2, inlining:0 */
            /* plain:176, name_value_code[ff,ff,0]/21 */
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                out.put((byte)176);
                ret = 176;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:177, name_value_code[ff,ff,8]/21 */
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                out.put((byte)177);
                ret = 177;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:179, name_value_code[ff,ff,10]/21 */
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                out.put((byte)179);
                ret = 179;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:209, name_value_code[ff,ff,18]/21 */
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
                out.put((byte)209);
                ret = 209;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:216, name_value_code[ff,ff,20]/21 */
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
                out.put((byte)216);
                ret = 216;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:217, name_value_code[ff,ff,28]/21 */
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
                out.put((byte)217);
                ret = 217;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:227, name_value_code[ff,ff,30]/21 */
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
                out.put((byte)227);
                ret = 227;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:229, name_value_code[ff,ff,38]/21 */
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
                out.put((byte)229);
                ret = 229;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:230, name_value_code[ff,ff,40]/21 */
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
                out.put((byte)230);
                ret = 230;
                in.advance(5);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:129, name_value_code[ff,ff,48]/22 */
            case 72:
            case 73:
            case 74:
            case 75:
                out.put((byte)129);
                ret = 129;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:132, name_value_code[ff,ff,4c]/22 */
            case 76:
            case 77:
            case 78:
            case 79:
                out.put((byte)132);
                ret = 132;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:133, name_value_code[ff,ff,50]/22 */
            case 80:
            case 81:
            case 82:
            case 83:
                out.put((byte)133);
                ret = 133;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:134, name_value_code[ff,ff,54]/22 */
            case 84:
            case 85:
            case 86:
            case 87:
                out.put((byte)134);
                ret = 134;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:136, name_value_code[ff,ff,58]/22 */
            case 88:
            case 89:
            case 90:
            case 91:
                out.put((byte)136);
                ret = 136;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:146, name_value_code[ff,ff,5c]/22 */
            case 92:
            case 93:
            case 94:
            case 95:
                out.put((byte)146);
                ret = 146;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:154, name_value_code[ff,ff,60]/22 */
            case 96:
            case 97:
            case 98:
            case 99:
                out.put((byte)154);
                ret = 154;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:156, name_value_code[ff,ff,64]/22 */
            case 100:
            case 101:
            case 102:
            case 103:
                out.put((byte)156);
                ret = 156;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:160, name_value_code[ff,ff,68]/22 */
            case 104:
            case 105:
            case 106:
            case 107:
                out.put((byte)160);
                ret = 160;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:163, name_value_code[ff,ff,6c]/22 */
            case 108:
            case 109:
            case 110:
            case 111:
                out.put((byte)163);
                ret = 163;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:164, name_value_code[ff,ff,70]/22 */
            case 112:
            case 113:
            case 114:
            case 115:
                out.put((byte)164);
                ret = 164;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:169, name_value_code[ff,ff,74]/22 */
            case 116:
            case 117:
            case 118:
            case 119:
                out.put((byte)169);
                ret = 169;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:170, name_value_code[ff,ff,78]/22 */
            case 120:
            case 121:
            case 122:
            case 123:
                out.put((byte)170);
                ret = 170;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:173, name_value_code[ff,ff,7c]/22 */
            case 124:
            case 125:
            case 126:
            case 127:
                out.put((byte)173);
                ret = 173;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:178, name_value_code[ff,ff,80]/22 */
            case 128:
            case 129:
            case 130:
            case 131:
                out.put((byte)178);
                ret = 178;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:181, name_value_code[ff,ff,84]/22 */
            case 132:
            case 133:
            case 134:
            case 135:
                out.put((byte)181);
                ret = 181;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:185, name_value_code[ff,ff,88]/22 */
            case 136:
            case 137:
            case 138:
            case 139:
                out.put((byte)185);
                ret = 185;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:186, name_value_code[ff,ff,8c]/22 */
            case 140:
            case 141:
            case 142:
            case 143:
                out.put((byte)186);
                ret = 186;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:187, name_value_code[ff,ff,90]/22 */
            case 144:
            case 145:
            case 146:
            case 147:
                out.put((byte)187);
                ret = 187;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:189, name_value_code[ff,ff,94]/22 */
            case 148:
            case 149:
            case 150:
            case 151:
                out.put((byte)189);
                ret = 189;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:190, name_value_code[ff,ff,98]/22 */
            case 152:
            case 153:
            case 154:
            case 155:
                out.put((byte)190);
                ret = 190;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:196, name_value_code[ff,ff,9c]/22 */
            case 156:
            case 157:
            case 158:
            case 159:
                out.put((byte)196);
                ret = 196;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:198, name_value_code[ff,ff,a0]/22 */
            case 160:
            case 161:
            case 162:
            case 163:
                out.put((byte)198);
                ret = 198;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:228, name_value_code[ff,ff,a4]/22 */
            case 164:
            case 165:
            case 166:
            case 167:
                out.put((byte)228);
                ret = 228;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:232, name_value_code[ff,ff,a8]/22 */
            case 168:
            case 169:
            case 170:
            case 171:
                out.put((byte)232);
                ret = 232;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:233, name_value_code[ff,ff,ac]/22 */
            case 172:
            case 173:
            case 174:
            case 175:
                out.put((byte)233);
                ret = 233;
                in.advance(6);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:1, name_value_code[ff,ff,b0]/23 */
            case 176:
            case 177:
                out.put((byte)1);
                ret = 1;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:135, name_value_code[ff,ff,b2]/23 */
            case 178:
            case 179:
                out.put((byte)135);
                ret = 135;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:137, name_value_code[ff,ff,b4]/23 */
            case 180:
            case 181:
                out.put((byte)137);
                ret = 137;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:138, name_value_code[ff,ff,b6]/23 */
            case 182:
            case 183:
                out.put((byte)138);
                ret = 138;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:139, name_value_code[ff,ff,b8]/23 */
            case 184:
            case 185:
                out.put((byte)139);
                ret = 139;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:140, name_value_code[ff,ff,ba]/23 */
            case 186:
            case 187:
                out.put((byte)140);
                ret = 140;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:141, name_value_code[ff,ff,bc]/23 */
            case 188:
            case 189:
                out.put((byte)141);
                ret = 141;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:143, name_value_code[ff,ff,be]/23 */
            case 190:
            case 191:
                out.put((byte)143);
                ret = 143;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:147, name_value_code[ff,ff,c0]/23 */
            case 192:
            case 193:
                out.put((byte)147);
                ret = 147;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:149, name_value_code[ff,ff,c2]/23 */
            case 194:
            case 195:
                out.put((byte)149);
                ret = 149;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:150, name_value_code[ff,ff,c4]/23 */
            case 196:
            case 197:
                out.put((byte)150);
                ret = 150;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:151, name_value_code[ff,ff,c6]/23 */
            case 198:
            case 199:
                out.put((byte)151);
                ret = 151;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:152, name_value_code[ff,ff,c8]/23 */
            case 200:
            case 201:
                out.put((byte)152);
                ret = 152;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:155, name_value_code[ff,ff,ca]/23 */
            case 202:
            case 203:
                out.put((byte)155);
                ret = 155;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:157, name_value_code[ff,ff,cc]/23 */
            case 204:
            case 205:
                out.put((byte)157);
                ret = 157;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:158, name_value_code[ff,ff,ce]/23 */
            case 206:
            case 207:
                out.put((byte)158);
                ret = 158;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:165, name_value_code[ff,ff,d0]/23 */
            case 208:
            case 209:
                out.put((byte)165);
                ret = 165;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:166, name_value_code[ff,ff,d2]/23 */
            case 210:
            case 211:
                out.put((byte)166);
                ret = 166;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:168, name_value_code[ff,ff,d4]/23 */
            case 212:
            case 213:
                out.put((byte)168);
                ret = 168;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:174, name_value_code[ff,ff,d6]/23 */
            case 214:
            case 215:
                out.put((byte)174);
                ret = 174;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:175, name_value_code[ff,ff,d8]/23 */
            case 216:
            case 217:
                out.put((byte)175);
                ret = 175;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:180, name_value_code[ff,ff,da]/23 */
            case 218:
            case 219:
                out.put((byte)180);
                ret = 180;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:182, name_value_code[ff,ff,dc]/23 */
            case 220:
            case 221:
                out.put((byte)182);
                ret = 182;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:183, name_value_code[ff,ff,de]/23 */
            case 222:
            case 223:
                out.put((byte)183);
                ret = 183;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:188, name_value_code[ff,ff,e0]/23 */
            case 224:
            case 225:
                out.put((byte)188);
                ret = 188;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:191, name_value_code[ff,ff,e2]/23 */
            case 226:
            case 227:
                out.put((byte)191);
                ret = 191;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:197, name_value_code[ff,ff,e4]/23 */
            case 228:
            case 229:
                out.put((byte)197);
                ret = 197;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:231, name_value_code[ff,ff,e6]/23 */
            case 230:
            case 231:
                out.put((byte)231);
                ret = 231;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:239, name_value_code[ff,ff,e8]/23 */
            case 232:
            case 233:
                out.put((byte)239);
                ret = 239;
                in.advance(7);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:9, name_value_code[ff,ff,ea]/24 */
            case 234:
                out.put((byte)9);
                ret = 9;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:142, name_value_code[ff,ff,eb]/24 */
            case 235:
                out.put((byte)142);
                ret = 142;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:144, name_value_code[ff,ff,ec]/24 */
            case 236:
                out.put((byte)144);
                ret = 144;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:145, name_value_code[ff,ff,ed]/24 */
            case 237:
                out.put((byte)145);
                ret = 145;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:148, name_value_code[ff,ff,ee]/24 */
            case 238:
                out.put((byte)148);
                ret = 148;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:159, name_value_code[ff,ff,ef]/24 */
            case 239:
                out.put((byte)159);
                ret = 159;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:171, name_value_code[ff,ff,f0]/24 */
            case 240:
                out.put((byte)171);
                ret = 171;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:206, name_value_code[ff,ff,f1]/24 */
            case 241:
                out.put((byte)206);
                ret = 206;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:215, name_value_code[ff,ff,f2]/24 */
            case 242:
                out.put((byte)215);
                ret = 215;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:225, name_value_code[ff,ff,f3]/24 */
            case 243:
                out.put((byte)225);
                ret = 225;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:236, name_value_code[ff,ff,f4]/24 */
            case 244:
                out.put((byte)236);
                ret = 236;
                in.advance(8);
                break;
            /* nb_bytes:2, inlining:0 */
            /* plain:237, name_value_code[ff,ff,f5]/24 */
            case 245:
                out.put((byte)237);
                ret = 237;
                in.advance(8);
                break;

            // inlining 246
            // increase temporarily nb_bytes to 3
            case 246:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:246 */
                    /* plain:199, name_value_code[ff,ff,f6,0]/25 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)199);
                        ret = 199;
                        in.advance(1);
                        break;
                    /* nb_bytes:3, inlining:246 */
                    /* plain:207, name_value_code[ff,ff,f6,80]/25 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)207);
                        ret = 207;
                        in.advance(1);
                        break;

                    // closing inline of 246
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 247
            // increase temporarily nb_bytes to 3
            case 247:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:247 */
                    /* plain:234, name_value_code[ff,ff,f7,0]/25 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)234);
                        ret = 234;
                        in.advance(1);
                        break;
                    /* nb_bytes:3, inlining:247 */
                    /* plain:235, name_value_code[ff,ff,f7,80]/25 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)235);
                        ret = 235;
                        in.advance(1);
                        break;

                    // closing inline of 247
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 248
            // increase temporarily nb_bytes to 3
            case 248:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:248 */
                    /* plain:192, name_value_code[ff,ff,f8,0]/26 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)192);
                        ret = 192;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:248 */
                    /* plain:193, name_value_code[ff,ff,f8,40]/26 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)193);
                        ret = 193;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:248 */
                    /* plain:200, name_value_code[ff,ff,f8,80]/26 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)200);
                        ret = 200;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:248 */
                    /* plain:201, name_value_code[ff,ff,f8,c0]/26 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)201);
                        ret = 201;
                        in.advance(2);
                        break;

                    // closing inline of 248
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 249
            // increase temporarily nb_bytes to 3
            case 249:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:249 */
                    /* plain:202, name_value_code[ff,ff,f9,0]/26 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)202);
                        ret = 202;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:249 */
                    /* plain:205, name_value_code[ff,ff,f9,40]/26 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)205);
                        ret = 205;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:249 */
                    /* plain:210, name_value_code[ff,ff,f9,80]/26 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)210);
                        ret = 210;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:249 */
                    /* plain:213, name_value_code[ff,ff,f9,c0]/26 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)213);
                        ret = 213;
                        in.advance(2);
                        break;

                    // closing inline of 249
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 250
            // increase temporarily nb_bytes to 3
            case 250:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:250 */
                    /* plain:218, name_value_code[ff,ff,fa,0]/26 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)218);
                        ret = 218;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:250 */
                    /* plain:219, name_value_code[ff,ff,fa,40]/26 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)219);
                        ret = 219;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:250 */
                    /* plain:238, name_value_code[ff,ff,fa,80]/26 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)238);
                        ret = 238;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:250 */
                    /* plain:240, name_value_code[ff,ff,fa,c0]/26 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)240);
                        ret = 240;
                        in.advance(2);
                        break;

                    // closing inline of 250
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 251
            // increase temporarily nb_bytes to 3
            case 251:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:251 */
                    /* plain:242, name_value_code[ff,ff,fb,0]/26 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)242);
                        ret = 242;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:251 */
                    /* plain:243, name_value_code[ff,ff,fb,40]/26 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)243);
                        ret = 243;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:251 */
                    /* plain:255, name_value_code[ff,ff,fb,80]/26 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)255);
                        ret = 255;
                        in.advance(2);
                        break;
                    /* nb_bytes:3, inlining:251 */
                    /* plain:203, name_value_code[ff,ff,fb,c0]/27 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                        out.put((byte)203);
                        ret = 203;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:251 */
                    /* plain:204, name_value_code[ff,ff,fb,e0]/27 */
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)204);
                        ret = 204;
                        in.advance(3);
                        break;

                    // closing inline of 251
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 252
            // increase temporarily nb_bytes to 3
            case 252:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:252 */
                    /* plain:211, name_value_code[ff,ff,fc,0]/27 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                        out.put((byte)211);
                        ret = 211;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:212, name_value_code[ff,ff,fc,20]/27 */
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)212);
                        ret = 212;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:214, name_value_code[ff,ff,fc,40]/27 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                        out.put((byte)214);
                        ret = 214;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:221, name_value_code[ff,ff,fc,60]/27 */
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)221);
                        ret = 221;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:222, name_value_code[ff,ff,fc,80]/27 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                        out.put((byte)222);
                        ret = 222;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:223, name_value_code[ff,ff,fc,a0]/27 */
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)223);
                        ret = 223;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:241, name_value_code[ff,ff,fc,c0]/27 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                        out.put((byte)241);
                        ret = 241;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:252 */
                    /* plain:244, name_value_code[ff,ff,fc,e0]/27 */
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)244);
                        ret = 244;
                        in.advance(3);
                        break;

                    // closing inline of 252
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 253
            // increase temporarily nb_bytes to 3
            case 253:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:253 */
                    /* plain:245, name_value_code[ff,ff,fd,0]/27 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                        out.put((byte)245);
                        ret = 245;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:246, name_value_code[ff,ff,fd,20]/27 */
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)246);
                        ret = 246;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:247, name_value_code[ff,ff,fd,40]/27 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                        out.put((byte)247);
                        ret = 247;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:248, name_value_code[ff,ff,fd,60]/27 */
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)248);
                        ret = 248;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:250, name_value_code[ff,ff,fd,80]/27 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                        out.put((byte)250);
                        ret = 250;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:251, name_value_code[ff,ff,fd,a0]/27 */
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)251);
                        ret = 251;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:252, name_value_code[ff,ff,fd,c0]/27 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                        out.put((byte)252);
                        ret = 252;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:253 */
                    /* plain:253, name_value_code[ff,ff,fd,e0]/27 */
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)253);
                        ret = 253;
                        in.advance(3);
                        break;

                    // closing inline of 253
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // inlining 254
            // increase temporarily nb_bytes to 3
            case 254:
                in.advance(8);
                switch (in.fetchByte()) {

                    /* nb_bytes:3, inlining:254 */
                    /* plain:254, name_value_code[ff,ff,fe,0]/27 */
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                        out.put((byte)254);
                        ret = 254;
                        in.advance(3);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:2, name_value_code[ff,ff,fe,20]/28 */
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                        out.put((byte)2);
                        ret = 2;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:3, name_value_code[ff,ff,fe,30]/28 */
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        out.put((byte)3);
                        ret = 3;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:4, name_value_code[ff,ff,fe,40]/28 */
                    case 64:
                    case 65:
                    case 66:
                    case 67:
                    case 68:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                        out.put((byte)4);
                        ret = 4;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:5, name_value_code[ff,ff,fe,50]/28 */
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 94:
                    case 95:
                        out.put((byte)5);
                        ret = 5;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:6, name_value_code[ff,ff,fe,60]/28 */
                    case 96:
                    case 97:
                    case 98:
                    case 99:
                    case 100:
                    case 101:
                    case 102:
                    case 103:
                    case 104:
                    case 105:
                    case 106:
                    case 107:
                    case 108:
                    case 109:
                    case 110:
                    case 111:
                        out.put((byte)6);
                        ret = 6;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:7, name_value_code[ff,ff,fe,70]/28 */
                    case 112:
                    case 113:
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                    case 120:
                    case 121:
                    case 122:
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        out.put((byte)7);
                        ret = 7;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:8, name_value_code[ff,ff,fe,80]/28 */
                    case 128:
                    case 129:
                    case 130:
                    case 131:
                    case 132:
                    case 133:
                    case 134:
                    case 135:
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                    case 141:
                    case 142:
                    case 143:
                        out.put((byte)8);
                        ret = 8;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:11, name_value_code[ff,ff,fe,90]/28 */
                    case 144:
                    case 145:
                    case 146:
                    case 147:
                    case 148:
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                    case 153:
                    case 154:
                    case 155:
                    case 156:
                    case 157:
                    case 158:
                    case 159:
                        out.put((byte)11);
                        ret = 11;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:12, name_value_code[ff,ff,fe,a0]/28 */
                    case 160:
                    case 161:
                    case 162:
                    case 163:
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                    case 173:
                    case 174:
                    case 175:
                        out.put((byte)12);
                        ret = 12;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:14, name_value_code[ff,ff,fe,b0]/28 */
                    case 176:
                    case 177:
                    case 178:
                    case 179:
                    case 180:
                    case 181:
                    case 182:
                    case 183:
                    case 184:
                    case 185:
                    case 186:
                    case 187:
                    case 188:
                    case 189:
                    case 190:
                    case 191:
                        out.put((byte)14);
                        ret = 14;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:15, name_value_code[ff,ff,fe,c0]/28 */
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case 202:
                    case 203:
                    case 204:
                    case 205:
                    case 206:
                    case 207:
                        out.put((byte)15);
                        ret = 15;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:16, name_value_code[ff,ff,fe,d0]/28 */
                    case 208:
                    case 209:
                    case 210:
                    case 211:
                    case 212:
                    case 213:
                    case 214:
                    case 215:
                    case 216:
                    case 217:
                    case 218:
                    case 219:
                    case 220:
                    case 221:
                    case 222:
                    case 223:
                        out.put((byte)16);
                        ret = 16;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:17, name_value_code[ff,ff,fe,e0]/28 */
                    case 224:
                    case 225:
                    case 226:
                    case 227:
                    case 228:
                    case 229:
                    case 230:
                    case 231:
                    case 232:
                    case 233:
                    case 234:
                    case 235:
                    case 236:
                    case 237:
                    case 238:
                    case 239:
                        out.put((byte)17);
                        ret = 17;
                        in.advance(4);
                        break;
                    /* nb_bytes:3, inlining:254 */
                    /* plain:18, name_value_code[ff,ff,fe,f0]/28 */
                    case 240:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    case 255:
                        out.put((byte)18);
                        ret = 18;
                        in.advance(4);
                        break;

                    // closing inline of 254
                    //1 decrease to nb_bytes: 2
                    case EOF:
                        ret = EOF;
                        break;
                    case SUSPEND:
                        ret = SUSPEND;
                        in.rewind(2 + 1);
                        break;

                }
                break;


            // increase nb_bytes : 3
            case 0xFF:
                in.advance(8);
                ret = parse_ff_3(in, out);
                break;
            case EOF:
                ret = EOF;
                break;
            case SUSPEND:
                in.rewind(3);
                ret = SUSPEND;
                break;
            //default:
            //  ret = EOF;
            //  break;
        }
        return ret;
    }

    static final public int parse_ff_3(In in, ByteBuffer out) {
        int ret = EOF;
        // increase nb_bytes : 3
        switch (in.fetchByte()) {

            /* nb_bytes:3, inlining:0 */
            /* plain:19, name_value_code[ff,ff,ff,0]/28 */
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                out.put((byte)19);
                ret = 19;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:20, name_value_code[ff,ff,ff,10]/28 */
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
                out.put((byte)20);
                ret = 20;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:21, name_value_code[ff,ff,ff,20]/28 */
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
                out.put((byte)21);
                ret = 21;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:23, name_value_code[ff,ff,ff,30]/28 */
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
                out.put((byte)23);
                ret = 23;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:24, name_value_code[ff,ff,ff,40]/28 */
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
                out.put((byte)24);
                ret = 24;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:25, name_value_code[ff,ff,ff,50]/28 */
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                out.put((byte)25);
                ret = 25;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:26, name_value_code[ff,ff,ff,60]/28 */
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
                out.put((byte)26);
                ret = 26;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:27, name_value_code[ff,ff,ff,70]/28 */
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
                out.put((byte)27);
                ret = 27;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:28, name_value_code[ff,ff,ff,80]/28 */
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
                out.put((byte)28);
                ret = 28;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:29, name_value_code[ff,ff,ff,90]/28 */
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
                out.put((byte)29);
                ret = 29;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:30, name_value_code[ff,ff,ff,a0]/28 */
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 172:
            case 173:
            case 174:
            case 175:
                out.put((byte)30);
                ret = 30;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:31, name_value_code[ff,ff,ff,b0]/28 */
            case 176:
            case 177:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            case 190:
            case 191:
                out.put((byte)31);
                ret = 31;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:127, name_value_code[ff,ff,ff,c0]/28 */
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206:
            case 207:
                out.put((byte)127);
                ret = 127;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:220, name_value_code[ff,ff,ff,d0]/28 */
            case 208:
            case 209:
            case 210:
            case 211:
            case 212:
            case 213:
            case 214:
            case 215:
            case 216:
            case 217:
            case 218:
            case 219:
            case 220:
            case 221:
            case 222:
            case 223:
                out.put((byte)220);
                ret = 220;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:249, name_value_code[ff,ff,ff,e0]/28 */
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
            case 232:
            case 233:
            case 234:
            case 235:
            case 236:
            case 237:
            case 238:
            case 239:
                out.put((byte)249);
                ret = 249;
                in.advance(4);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:10, name_value_code[ff,ff,ff,f0]/30 */
            case 240:
            case 241:
            case 242:
            case 243:
                out.put((byte)10);
                ret = 10;
                in.advance(6);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:13, name_value_code[ff,ff,ff,f4]/30 */
            case 244:
            case 245:
            case 246:
            case 247:
                out.put((byte)13);
                ret = 13;
                in.advance(6);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:22, name_value_code[ff,ff,ff,f8]/30 */
            case 248:
            case 249:
            case 250:
            case 251:
                out.put((byte)22);
                ret = 22;
                in.advance(6);
                break;
            /* nb_bytes:3, inlining:0 */
            /* plain:256, name_value_code[ff,ff,ff,fc]/30 */
            case 252:
            case 253:
            case 254:
            case 255:
                out.put((byte)256);
                ret = 256;
                in.advance(6);
                break;

        }
        return ret;
    }
}