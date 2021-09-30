package com.alcatel.as.http2.hpack;

import java.nio.ByteBuffer;

public abstract class TrivialHuffmanEncoderAbstract<E> {

    int current = 0;
    int index   = 0;
    
    final private void write_code(E bb, int len, int first, int second, int third, int fourth) {
	write_code(bb,8,first);
	write_code(bb,8,second);
	write_code(bb,8,third);
	write_code(bb,len,fourth);
    }

    final private void write_code(E bb, int len, int first, int second, int third) {
	write_code(bb,8,first);
	write_code(bb,8,second);
	write_code(bb,len,third);
    }

    final private void write_code(E bb, int len, int first, int second) {
	write_code(bb,8,first);
	write_code(bb,len,second);
    }

    final private void write_code_padding(E bb) {
        if (index != 0)
          write_code(bb,8-index,0xFF);
    }

    protected abstract void write_code(E bb, int len, int first) ;
    
    final public void encode(ByteBuffer in, E out) {
      current = 0;
      index   = 0;

    int available = in.remaining();
    for(int i=0; i< available; i++) {
      int b = ((int)in.get(i)) & 0xff ;
      switch( b ) {


        case 48 :

          write_code(out,5, 0 );

          break;


        case 49 :

          write_code(out,5, 8 );

          break;


        case 50 :

          write_code(out,5, 16 );

          break;


        case 97 :

          write_code(out,5, 24 );

          break;


        case 99 :

          write_code(out,5, 32 );

          break;


        case 101 :

          write_code(out,5, 40 );

          break;


        case 105 :

          write_code(out,5, 48 );

          break;


        case 111 :

          write_code(out,5, 56 );

          break;


        case 115 :

          write_code(out,5, 64 );

          break;


        case 116 :

          write_code(out,5, 72 );

          break;


        case 32 :

          write_code(out,6, 80 );

          break;


        case 37 :

          write_code(out,6, 84 );

          break;


        case 45 :

          write_code(out,6, 88 );

          break;


        case 46 :

          write_code(out,6, 92 );

          break;


        case 47 :

          write_code(out,6, 96 );

          break;


        case 51 :

          write_code(out,6, 100 );

          break;


        case 52 :

          write_code(out,6, 104 );

          break;


        case 53 :

          write_code(out,6, 108 );

          break;


        case 54 :

          write_code(out,6, 112 );

          break;


        case 55 :

          write_code(out,6, 116 );

          break;


        case 56 :

          write_code(out,6, 120 );

          break;


        case 57 :

          write_code(out,6, 124 );

          break;


        case 61 :

          write_code(out,6, 128 );

          break;


        case 65 :

          write_code(out,6, 132 );

          break;


        case 95 :

          write_code(out,6, 136 );

          break;


        case 98 :

          write_code(out,6, 140 );

          break;


        case 100 :

          write_code(out,6, 144 );

          break;


        case 102 :

          write_code(out,6, 148 );

          break;


        case 103 :

          write_code(out,6, 152 );

          break;


        case 104 :

          write_code(out,6, 156 );

          break;


        case 108 :

          write_code(out,6, 160 );

          break;


        case 109 :

          write_code(out,6, 164 );

          break;


        case 110 :

          write_code(out,6, 168 );

          break;


        case 112 :

          write_code(out,6, 172 );

          break;


        case 114 :

          write_code(out,6, 176 );

          break;


        case 117 :

          write_code(out,6, 180 );

          break;


        case 58 :

          write_code(out,7, 184 );

          break;


        case 66 :

          write_code(out,7, 186 );

          break;


        case 67 :

          write_code(out,7, 188 );

          break;


        case 68 :

          write_code(out,7, 190 );

          break;


        case 69 :

          write_code(out,7, 192 );

          break;


        case 70 :

          write_code(out,7, 194 );

          break;


        case 71 :

          write_code(out,7, 196 );

          break;


        case 72 :

          write_code(out,7, 198 );

          break;


        case 73 :

          write_code(out,7, 200 );

          break;


        case 74 :

          write_code(out,7, 202 );

          break;


        case 75 :

          write_code(out,7, 204 );

          break;


        case 76 :

          write_code(out,7, 206 );

          break;


        case 77 :

          write_code(out,7, 208 );

          break;


        case 78 :

          write_code(out,7, 210 );

          break;


        case 79 :

          write_code(out,7, 212 );

          break;


        case 80 :

          write_code(out,7, 214 );

          break;


        case 81 :

          write_code(out,7, 216 );

          break;


        case 82 :

          write_code(out,7, 218 );

          break;


        case 83 :

          write_code(out,7, 220 );

          break;


        case 84 :

          write_code(out,7, 222 );

          break;


        case 85 :

          write_code(out,7, 224 );

          break;


        case 86 :

          write_code(out,7, 226 );

          break;


        case 87 :

          write_code(out,7, 228 );

          break;


        case 89 :

          write_code(out,7, 230 );

          break;


        case 106 :

          write_code(out,7, 232 );

          break;


        case 107 :

          write_code(out,7, 234 );

          break;


        case 113 :

          write_code(out,7, 236 );

          break;


        case 118 :

          write_code(out,7, 238 );

          break;


        case 119 :

          write_code(out,7, 240 );

          break;


        case 120 :

          write_code(out,7, 242 );

          break;


        case 121 :

          write_code(out,7, 244 );

          break;


        case 122 :

          write_code(out,7, 246 );

          break;


        case 38 :

          write_code(out,8, 248 );

          break;


        case 42 :

          write_code(out,8, 249 );

          break;


        case 44 :

          write_code(out,8, 250 );

          break;


        case 59 :

          write_code(out,8, 251 );

          break;


        case 88 :

          write_code(out,8, 252 );

          break;


        case 90 :

          write_code(out,8, 253 );

          break;


        case 33 :

          write_code(out,2, 254,0 );

          break;


        case 34 :

          write_code(out,2, 254,64 );

          break;


        case 40 :

          write_code(out,2, 254,128 );

          break;


        case 41 :

          write_code(out,2, 254,192 );

          break;


        case 63 :

          write_code(out,2, 255,0 );

          break;


        case 39 :

          write_code(out,3, 255,64 );

          break;


        case 43 :

          write_code(out,3, 255,96 );

          break;


        case 124 :

          write_code(out,3, 255,128 );

          break;


        case 35 :

          write_code(out,4, 255,160 );

          break;


        case 62 :

          write_code(out,4, 255,176 );

          break;


        case 0 :

          write_code(out,5, 255,192 );

          break;


        case 36 :

          write_code(out,5, 255,200 );

          break;


        case 64 :

          write_code(out,5, 255,208 );

          break;


        case 91 :

          write_code(out,5, 255,216 );

          break;


        case 93 :

          write_code(out,5, 255,224 );

          break;


        case 126 :

          write_code(out,5, 255,232 );

          break;


        case 94 :

          write_code(out,6, 255,240 );

          break;


        case 125 :

          write_code(out,6, 255,244 );

          break;


        case 60 :

          write_code(out,7, 255,248 );

          break;


        case 96 :

          write_code(out,7, 255,250 );

          break;


        case 123 :

          write_code(out,7, 255,252 );

          break;


        case 92 :

          write_code(out,3, 255,254,0 );

          break;


        case 195 :

          write_code(out,3, 255,254,32 );

          break;


        case 208 :

          write_code(out,3, 255,254,64 );

          break;


        case 128 :

          write_code(out,4, 255,254,96 );

          break;


        case 130 :

          write_code(out,4, 255,254,112 );

          break;


        case 131 :

          write_code(out,4, 255,254,128 );

          break;


        case 162 :

          write_code(out,4, 255,254,144 );

          break;


        case 184 :

          write_code(out,4, 255,254,160 );

          break;


        case 194 :

          write_code(out,4, 255,254,176 );

          break;


        case 224 :

          write_code(out,4, 255,254,192 );

          break;


        case 226 :

          write_code(out,4, 255,254,208 );

          break;


        case 153 :

          write_code(out,5, 255,254,224 );

          break;


        case 161 :

          write_code(out,5, 255,254,232 );

          break;


        case 167 :

          write_code(out,5, 255,254,240 );

          break;


        case 172 :

          write_code(out,5, 255,254,248 );

          break;


        case 176 :

          write_code(out,5, 255,255,0 );

          break;


        case 177 :

          write_code(out,5, 255,255,8 );

          break;


        case 179 :

          write_code(out,5, 255,255,16 );

          break;


        case 209 :

          write_code(out,5, 255,255,24 );

          break;


        case 216 :

          write_code(out,5, 255,255,32 );

          break;


        case 217 :

          write_code(out,5, 255,255,40 );

          break;


        case 227 :

          write_code(out,5, 255,255,48 );

          break;


        case 229 :

          write_code(out,5, 255,255,56 );

          break;


        case 230 :

          write_code(out,5, 255,255,64 );

          break;


        case 129 :

          write_code(out,6, 255,255,72 );

          break;


        case 132 :

          write_code(out,6, 255,255,76 );

          break;


        case 133 :

          write_code(out,6, 255,255,80 );

          break;


        case 134 :

          write_code(out,6, 255,255,84 );

          break;


        case 136 :

          write_code(out,6, 255,255,88 );

          break;


        case 146 :

          write_code(out,6, 255,255,92 );

          break;


        case 154 :

          write_code(out,6, 255,255,96 );

          break;


        case 156 :

          write_code(out,6, 255,255,100 );

          break;


        case 160 :

          write_code(out,6, 255,255,104 );

          break;


        case 163 :

          write_code(out,6, 255,255,108 );

          break;


        case 164 :

          write_code(out,6, 255,255,112 );

          break;


        case 169 :

          write_code(out,6, 255,255,116 );

          break;


        case 170 :

          write_code(out,6, 255,255,120 );

          break;


        case 173 :

          write_code(out,6, 255,255,124 );

          break;


        case 178 :

          write_code(out,6, 255,255,128 );

          break;


        case 181 :

          write_code(out,6, 255,255,132 );

          break;


        case 185 :

          write_code(out,6, 255,255,136 );

          break;


        case 186 :

          write_code(out,6, 255,255,140 );

          break;


        case 187 :

          write_code(out,6, 255,255,144 );

          break;


        case 189 :

          write_code(out,6, 255,255,148 );

          break;


        case 190 :

          write_code(out,6, 255,255,152 );

          break;


        case 196 :

          write_code(out,6, 255,255,156 );

          break;


        case 198 :

          write_code(out,6, 255,255,160 );

          break;


        case 228 :

          write_code(out,6, 255,255,164 );

          break;


        case 232 :

          write_code(out,6, 255,255,168 );

          break;


        case 233 :

          write_code(out,6, 255,255,172 );

          break;


        case 1 :

          write_code(out,7, 255,255,176 );

          break;


        case 135 :

          write_code(out,7, 255,255,178 );

          break;


        case 137 :

          write_code(out,7, 255,255,180 );

          break;


        case 138 :

          write_code(out,7, 255,255,182 );

          break;


        case 139 :

          write_code(out,7, 255,255,184 );

          break;


        case 140 :

          write_code(out,7, 255,255,186 );

          break;


        case 141 :

          write_code(out,7, 255,255,188 );

          break;


        case 143 :

          write_code(out,7, 255,255,190 );

          break;


        case 147 :

          write_code(out,7, 255,255,192 );

          break;


        case 149 :

          write_code(out,7, 255,255,194 );

          break;


        case 150 :

          write_code(out,7, 255,255,196 );

          break;


        case 151 :

          write_code(out,7, 255,255,198 );

          break;


        case 152 :

          write_code(out,7, 255,255,200 );

          break;


        case 155 :

          write_code(out,7, 255,255,202 );

          break;


        case 157 :

          write_code(out,7, 255,255,204 );

          break;


        case 158 :

          write_code(out,7, 255,255,206 );

          break;


        case 165 :

          write_code(out,7, 255,255,208 );

          break;


        case 166 :

          write_code(out,7, 255,255,210 );

          break;


        case 168 :

          write_code(out,7, 255,255,212 );

          break;


        case 174 :

          write_code(out,7, 255,255,214 );

          break;


        case 175 :

          write_code(out,7, 255,255,216 );

          break;


        case 180 :

          write_code(out,7, 255,255,218 );

          break;


        case 182 :

          write_code(out,7, 255,255,220 );

          break;


        case 183 :

          write_code(out,7, 255,255,222 );

          break;


        case 188 :

          write_code(out,7, 255,255,224 );

          break;


        case 191 :

          write_code(out,7, 255,255,226 );

          break;


        case 197 :

          write_code(out,7, 255,255,228 );

          break;


        case 231 :

          write_code(out,7, 255,255,230 );

          break;


        case 239 :

          write_code(out,7, 255,255,232 );

          break;


        case 9 :

          write_code(out,8, 255,255,234 );

          break;


        case 142 :

          write_code(out,8, 255,255,235 );

          break;


        case 144 :

          write_code(out,8, 255,255,236 );

          break;


        case 145 :

          write_code(out,8, 255,255,237 );

          break;


        case 148 :

          write_code(out,8, 255,255,238 );

          break;


        case 159 :

          write_code(out,8, 255,255,239 );

          break;


        case 171 :

          write_code(out,8, 255,255,240 );

          break;


        case 206 :

          write_code(out,8, 255,255,241 );

          break;


        case 215 :

          write_code(out,8, 255,255,242 );

          break;


        case 225 :

          write_code(out,8, 255,255,243 );

          break;


        case 236 :

          write_code(out,8, 255,255,244 );

          break;


        case 237 :

          write_code(out,8, 255,255,245 );

          break;


        case 199 :

          write_code(out,1, 255,255,246,0 );

          break;


        case 207 :

          write_code(out,1, 255,255,246,128 );

          break;


        case 234 :

          write_code(out,1, 255,255,247,0 );

          break;


        case 235 :

          write_code(out,1, 255,255,247,128 );

          break;


        case 192 :

          write_code(out,2, 255,255,248,0 );

          break;


        case 193 :

          write_code(out,2, 255,255,248,64 );

          break;


        case 200 :

          write_code(out,2, 255,255,248,128 );

          break;


        case 201 :

          write_code(out,2, 255,255,248,192 );

          break;


        case 202 :

          write_code(out,2, 255,255,249,0 );

          break;


        case 205 :

          write_code(out,2, 255,255,249,64 );

          break;


        case 210 :

          write_code(out,2, 255,255,249,128 );

          break;


        case 213 :

          write_code(out,2, 255,255,249,192 );

          break;


        case 218 :

          write_code(out,2, 255,255,250,0 );

          break;


        case 219 :

          write_code(out,2, 255,255,250,64 );

          break;


        case 238 :

          write_code(out,2, 255,255,250,128 );

          break;


        case 240 :

          write_code(out,2, 255,255,250,192 );

          break;


        case 242 :

          write_code(out,2, 255,255,251,0 );

          break;


        case 243 :

          write_code(out,2, 255,255,251,64 );

          break;


        case 255 :

          write_code(out,2, 255,255,251,128 );

          break;


        case 203 :

          write_code(out,3, 255,255,251,192 );

          break;


        case 204 :

          write_code(out,3, 255,255,251,224 );

          break;


        case 211 :

          write_code(out,3, 255,255,252,0 );

          break;


        case 212 :

          write_code(out,3, 255,255,252,32 );

          break;


        case 214 :

          write_code(out,3, 255,255,252,64 );

          break;


        case 221 :

          write_code(out,3, 255,255,252,96 );

          break;


        case 222 :

          write_code(out,3, 255,255,252,128 );

          break;


        case 223 :

          write_code(out,3, 255,255,252,160 );

          break;


        case 241 :

          write_code(out,3, 255,255,252,192 );

          break;


        case 244 :

          write_code(out,3, 255,255,252,224 );

          break;


        case 245 :

          write_code(out,3, 255,255,253,0 );

          break;


        case 246 :

          write_code(out,3, 255,255,253,32 );

          break;


        case 247 :

          write_code(out,3, 255,255,253,64 );

          break;


        case 248 :

          write_code(out,3, 255,255,253,96 );

          break;


        case 250 :

          write_code(out,3, 255,255,253,128 );

          break;


        case 251 :

          write_code(out,3, 255,255,253,160 );

          break;


        case 252 :

          write_code(out,3, 255,255,253,192 );

          break;


        case 253 :

          write_code(out,3, 255,255,253,224 );

          break;


        case 254 :

          write_code(out,3, 255,255,254,0 );

          break;


        case 2 :

          write_code(out,4, 255,255,254,32 );

          break;


        case 3 :

          write_code(out,4, 255,255,254,48 );

          break;


        case 4 :

          write_code(out,4, 255,255,254,64 );

          break;


        case 5 :

          write_code(out,4, 255,255,254,80 );

          break;


        case 6 :

          write_code(out,4, 255,255,254,96 );

          break;


        case 7 :

          write_code(out,4, 255,255,254,112 );

          break;


        case 8 :

          write_code(out,4, 255,255,254,128 );

          break;


        case 11 :

          write_code(out,4, 255,255,254,144 );

          break;


        case 12 :

          write_code(out,4, 255,255,254,160 );

          break;


        case 14 :

          write_code(out,4, 255,255,254,176 );

          break;


        case 15 :

          write_code(out,4, 255,255,254,192 );

          break;


        case 16 :

          write_code(out,4, 255,255,254,208 );

          break;


        case 17 :

          write_code(out,4, 255,255,254,224 );

          break;


        case 18 :

          write_code(out,4, 255,255,254,240 );

          break;


        case 19 :

          write_code(out,4, 255,255,255,0 );

          break;


        case 20 :

          write_code(out,4, 255,255,255,16 );

          break;


        case 21 :

          write_code(out,4, 255,255,255,32 );

          break;


        case 23 :

          write_code(out,4, 255,255,255,48 );

          break;


        case 24 :

          write_code(out,4, 255,255,255,64 );

          break;


        case 25 :

          write_code(out,4, 255,255,255,80 );

          break;


        case 26 :

          write_code(out,4, 255,255,255,96 );

          break;


        case 27 :

          write_code(out,4, 255,255,255,112 );

          break;


        case 28 :

          write_code(out,4, 255,255,255,128 );

          break;


        case 29 :

          write_code(out,4, 255,255,255,144 );

          break;


        case 30 :

          write_code(out,4, 255,255,255,160 );

          break;


        case 31 :

          write_code(out,4, 255,255,255,176 );

          break;


        case 127 :

          write_code(out,4, 255,255,255,192 );

          break;


        case 220 :

          write_code(out,4, 255,255,255,208 );

          break;


        case 249 :

          write_code(out,4, 255,255,255,224 );

          break;


        case 10 :

          write_code(out,6, 255,255,255,240 );

          break;


        case 13 :

          write_code(out,6, 255,255,255,244 );

          break;


        case 22 :

          write_code(out,6, 255,255,255,248 );

          break;


      }
    }
    write_code_padding(out);
    
  }

   final public void encode(byte [] in, E out) {
    current = 0;
    index   = 0;

    int available = in.length;
    for(int i=0; i< available; i++) {
      int b = ((int)in[i]) & 0xff ;
      switch( b ) {


        case 48 :

          write_code(out,5, 0 );

          break;


        case 49 :

          write_code(out,5, 8 );

          break;


        case 50 :

          write_code(out,5, 16 );

          break;


        case 97 :

          write_code(out,5, 24 );

          break;


        case 99 :

          write_code(out,5, 32 );

          break;


        case 101 :

          write_code(out,5, 40 );

          break;


        case 105 :

          write_code(out,5, 48 );

          break;


        case 111 :

          write_code(out,5, 56 );

          break;


        case 115 :

          write_code(out,5, 64 );

          break;


        case 116 :

          write_code(out,5, 72 );

          break;


        case 32 :

          write_code(out,6, 80 );

          break;


        case 37 :

          write_code(out,6, 84 );

          break;


        case 45 :

          write_code(out,6, 88 );

          break;


        case 46 :

          write_code(out,6, 92 );

          break;


        case 47 :

          write_code(out,6, 96 );

          break;


        case 51 :

          write_code(out,6, 100 );

          break;


        case 52 :

          write_code(out,6, 104 );

          break;


        case 53 :

          write_code(out,6, 108 );

          break;


        case 54 :

          write_code(out,6, 112 );

          break;


        case 55 :

          write_code(out,6, 116 );

          break;


        case 56 :

          write_code(out,6, 120 );

          break;


        case 57 :

          write_code(out,6, 124 );

          break;


        case 61 :

          write_code(out,6, 128 );

          break;


        case 65 :

          write_code(out,6, 132 );

          break;


        case 95 :

          write_code(out,6, 136 );

          break;


        case 98 :

          write_code(out,6, 140 );

          break;


        case 100 :

          write_code(out,6, 144 );

          break;


        case 102 :

          write_code(out,6, 148 );

          break;


        case 103 :

          write_code(out,6, 152 );

          break;


        case 104 :

          write_code(out,6, 156 );

          break;


        case 108 :

          write_code(out,6, 160 );

          break;


        case 109 :

          write_code(out,6, 164 );

          break;


        case 110 :

          write_code(out,6, 168 );

          break;


        case 112 :

          write_code(out,6, 172 );

          break;


        case 114 :

          write_code(out,6, 176 );

          break;


        case 117 :

          write_code(out,6, 180 );

          break;


        case 58 :

          write_code(out,7, 184 );

          break;


        case 66 :

          write_code(out,7, 186 );

          break;


        case 67 :

          write_code(out,7, 188 );

          break;


        case 68 :

          write_code(out,7, 190 );

          break;


        case 69 :

          write_code(out,7, 192 );

          break;


        case 70 :

          write_code(out,7, 194 );

          break;


        case 71 :

          write_code(out,7, 196 );

          break;


        case 72 :

          write_code(out,7, 198 );

          break;


        case 73 :

          write_code(out,7, 200 );

          break;


        case 74 :

          write_code(out,7, 202 );

          break;


        case 75 :

          write_code(out,7, 204 );

          break;


        case 76 :

          write_code(out,7, 206 );

          break;


        case 77 :

          write_code(out,7, 208 );

          break;


        case 78 :

          write_code(out,7, 210 );

          break;


        case 79 :

          write_code(out,7, 212 );

          break;


        case 80 :

          write_code(out,7, 214 );

          break;


        case 81 :

          write_code(out,7, 216 );

          break;


        case 82 :

          write_code(out,7, 218 );

          break;


        case 83 :

          write_code(out,7, 220 );

          break;


        case 84 :

          write_code(out,7, 222 );

          break;


        case 85 :

          write_code(out,7, 224 );

          break;


        case 86 :

          write_code(out,7, 226 );

          break;


        case 87 :

          write_code(out,7, 228 );

          break;


        case 89 :

          write_code(out,7, 230 );

          break;


        case 106 :

          write_code(out,7, 232 );

          break;


        case 107 :

          write_code(out,7, 234 );

          break;


        case 113 :

          write_code(out,7, 236 );

          break;


        case 118 :

          write_code(out,7, 238 );

          break;


        case 119 :

          write_code(out,7, 240 );

          break;


        case 120 :

          write_code(out,7, 242 );

          break;


        case 121 :

          write_code(out,7, 244 );

          break;


        case 122 :

          write_code(out,7, 246 );

          break;


        case 38 :

          write_code(out,8, 248 );

          break;


        case 42 :

          write_code(out,8, 249 );

          break;


        case 44 :

          write_code(out,8, 250 );

          break;


        case 59 :

          write_code(out,8, 251 );

          break;


        case 88 :

          write_code(out,8, 252 );

          break;


        case 90 :

          write_code(out,8, 253 );

          break;


        case 33 :

          write_code(out,2, 254,0 );

          break;


        case 34 :

          write_code(out,2, 254,64 );

          break;


        case 40 :

          write_code(out,2, 254,128 );

          break;


        case 41 :

          write_code(out,2, 254,192 );

          break;


        case 63 :

          write_code(out,2, 255,0 );

          break;


        case 39 :

          write_code(out,3, 255,64 );

          break;


        case 43 :

          write_code(out,3, 255,96 );

          break;


        case 124 :

          write_code(out,3, 255,128 );

          break;


        case 35 :

          write_code(out,4, 255,160 );

          break;


        case 62 :

          write_code(out,4, 255,176 );

          break;


        case 0 :

          write_code(out,5, 255,192 );

          break;


        case 36 :

          write_code(out,5, 255,200 );

          break;


        case 64 :

          write_code(out,5, 255,208 );

          break;


        case 91 :

          write_code(out,5, 255,216 );

          break;


        case 93 :

          write_code(out,5, 255,224 );

          break;


        case 126 :

          write_code(out,5, 255,232 );

          break;


        case 94 :

          write_code(out,6, 255,240 );

          break;


        case 125 :

          write_code(out,6, 255,244 );

          break;


        case 60 :

          write_code(out,7, 255,248 );

          break;


        case 96 :

          write_code(out,7, 255,250 );

          break;


        case 123 :

          write_code(out,7, 255,252 );

          break;


        case 92 :

          write_code(out,3, 255,254,0 );

          break;


        case 195 :

          write_code(out,3, 255,254,32 );

          break;


        case 208 :

          write_code(out,3, 255,254,64 );

          break;


        case 128 :

          write_code(out,4, 255,254,96 );

          break;


        case 130 :

          write_code(out,4, 255,254,112 );

          break;


        case 131 :

          write_code(out,4, 255,254,128 );

          break;


        case 162 :

          write_code(out,4, 255,254,144 );

          break;


        case 184 :

          write_code(out,4, 255,254,160 );

          break;


        case 194 :

          write_code(out,4, 255,254,176 );

          break;


        case 224 :

          write_code(out,4, 255,254,192 );

          break;


        case 226 :

          write_code(out,4, 255,254,208 );

          break;


        case 153 :

          write_code(out,5, 255,254,224 );

          break;


        case 161 :

          write_code(out,5, 255,254,232 );

          break;


        case 167 :

          write_code(out,5, 255,254,240 );

          break;


        case 172 :

          write_code(out,5, 255,254,248 );

          break;


        case 176 :

          write_code(out,5, 255,255,0 );

          break;


        case 177 :

          write_code(out,5, 255,255,8 );

          break;


        case 179 :

          write_code(out,5, 255,255,16 );

          break;


        case 209 :

          write_code(out,5, 255,255,24 );

          break;


        case 216 :

          write_code(out,5, 255,255,32 );

          break;


        case 217 :

          write_code(out,5, 255,255,40 );

          break;


        case 227 :

          write_code(out,5, 255,255,48 );

          break;


        case 229 :

          write_code(out,5, 255,255,56 );

          break;


        case 230 :

          write_code(out,5, 255,255,64 );

          break;


        case 129 :

          write_code(out,6, 255,255,72 );

          break;


        case 132 :

          write_code(out,6, 255,255,76 );

          break;


        case 133 :

          write_code(out,6, 255,255,80 );

          break;


        case 134 :

          write_code(out,6, 255,255,84 );

          break;


        case 136 :

          write_code(out,6, 255,255,88 );

          break;


        case 146 :

          write_code(out,6, 255,255,92 );

          break;


        case 154 :

          write_code(out,6, 255,255,96 );

          break;


        case 156 :

          write_code(out,6, 255,255,100 );

          break;


        case 160 :

          write_code(out,6, 255,255,104 );

          break;


        case 163 :

          write_code(out,6, 255,255,108 );

          break;


        case 164 :

          write_code(out,6, 255,255,112 );

          break;


        case 169 :

          write_code(out,6, 255,255,116 );

          break;


        case 170 :

          write_code(out,6, 255,255,120 );

          break;


        case 173 :

          write_code(out,6, 255,255,124 );

          break;


        case 178 :

          write_code(out,6, 255,255,128 );

          break;


        case 181 :

          write_code(out,6, 255,255,132 );

          break;


        case 185 :

          write_code(out,6, 255,255,136 );

          break;


        case 186 :

          write_code(out,6, 255,255,140 );

          break;


        case 187 :

          write_code(out,6, 255,255,144 );

          break;


        case 189 :

          write_code(out,6, 255,255,148 );

          break;


        case 190 :

          write_code(out,6, 255,255,152 );

          break;


        case 196 :

          write_code(out,6, 255,255,156 );

          break;


        case 198 :

          write_code(out,6, 255,255,160 );

          break;


        case 228 :

          write_code(out,6, 255,255,164 );

          break;


        case 232 :

          write_code(out,6, 255,255,168 );

          break;


        case 233 :

          write_code(out,6, 255,255,172 );

          break;


        case 1 :

          write_code(out,7, 255,255,176 );

          break;


        case 135 :

          write_code(out,7, 255,255,178 );

          break;


        case 137 :

          write_code(out,7, 255,255,180 );

          break;


        case 138 :

          write_code(out,7, 255,255,182 );

          break;


        case 139 :

          write_code(out,7, 255,255,184 );

          break;


        case 140 :

          write_code(out,7, 255,255,186 );

          break;


        case 141 :

          write_code(out,7, 255,255,188 );

          break;


        case 143 :

          write_code(out,7, 255,255,190 );

          break;


        case 147 :

          write_code(out,7, 255,255,192 );

          break;


        case 149 :

          write_code(out,7, 255,255,194 );

          break;


        case 150 :

          write_code(out,7, 255,255,196 );

          break;


        case 151 :

          write_code(out,7, 255,255,198 );

          break;


        case 152 :

          write_code(out,7, 255,255,200 );

          break;


        case 155 :

          write_code(out,7, 255,255,202 );

          break;


        case 157 :

          write_code(out,7, 255,255,204 );

          break;


        case 158 :

          write_code(out,7, 255,255,206 );

          break;


        case 165 :

          write_code(out,7, 255,255,208 );

          break;


        case 166 :

          write_code(out,7, 255,255,210 );

          break;


        case 168 :

          write_code(out,7, 255,255,212 );

          break;


        case 174 :

          write_code(out,7, 255,255,214 );

          break;


        case 175 :

          write_code(out,7, 255,255,216 );

          break;


        case 180 :

          write_code(out,7, 255,255,218 );

          break;


        case 182 :

          write_code(out,7, 255,255,220 );

          break;


        case 183 :

          write_code(out,7, 255,255,222 );

          break;


        case 188 :

          write_code(out,7, 255,255,224 );

          break;


        case 191 :

          write_code(out,7, 255,255,226 );

          break;


        case 197 :

          write_code(out,7, 255,255,228 );

          break;


        case 231 :

          write_code(out,7, 255,255,230 );

          break;


        case 239 :

          write_code(out,7, 255,255,232 );

          break;


        case 9 :

          write_code(out,8, 255,255,234 );

          break;


        case 142 :

          write_code(out,8, 255,255,235 );

          break;


        case 144 :

          write_code(out,8, 255,255,236 );

          break;


        case 145 :

          write_code(out,8, 255,255,237 );

          break;


        case 148 :

          write_code(out,8, 255,255,238 );

          break;


        case 159 :

          write_code(out,8, 255,255,239 );

          break;


        case 171 :

          write_code(out,8, 255,255,240 );

          break;


        case 206 :

          write_code(out,8, 255,255,241 );

          break;


        case 215 :

          write_code(out,8, 255,255,242 );

          break;


        case 225 :

          write_code(out,8, 255,255,243 );

          break;


        case 236 :

          write_code(out,8, 255,255,244 );

          break;


        case 237 :

          write_code(out,8, 255,255,245 );

          break;


        case 199 :

          write_code(out,1, 255,255,246,0 );

          break;


        case 207 :

          write_code(out,1, 255,255,246,128 );

          break;


        case 234 :

          write_code(out,1, 255,255,247,0 );

          break;


        case 235 :

          write_code(out,1, 255,255,247,128 );

          break;


        case 192 :

          write_code(out,2, 255,255,248,0 );

          break;


        case 193 :

          write_code(out,2, 255,255,248,64 );

          break;


        case 200 :

          write_code(out,2, 255,255,248,128 );

          break;


        case 201 :

          write_code(out,2, 255,255,248,192 );

          break;


        case 202 :

          write_code(out,2, 255,255,249,0 );

          break;


        case 205 :

          write_code(out,2, 255,255,249,64 );

          break;


        case 210 :

          write_code(out,2, 255,255,249,128 );

          break;


        case 213 :

          write_code(out,2, 255,255,249,192 );

          break;


        case 218 :

          write_code(out,2, 255,255,250,0 );

          break;


        case 219 :

          write_code(out,2, 255,255,250,64 );

          break;


        case 238 :

          write_code(out,2, 255,255,250,128 );

          break;


        case 240 :

          write_code(out,2, 255,255,250,192 );

          break;


        case 242 :

          write_code(out,2, 255,255,251,0 );

          break;


        case 243 :

          write_code(out,2, 255,255,251,64 );

          break;


        case 255 :

          write_code(out,2, 255,255,251,128 );

          break;


        case 203 :

          write_code(out,3, 255,255,251,192 );

          break;


        case 204 :

          write_code(out,3, 255,255,251,224 );

          break;


        case 211 :

          write_code(out,3, 255,255,252,0 );

          break;


        case 212 :

          write_code(out,3, 255,255,252,32 );

          break;


        case 214 :

          write_code(out,3, 255,255,252,64 );

          break;


        case 221 :

          write_code(out,3, 255,255,252,96 );

          break;


        case 222 :

          write_code(out,3, 255,255,252,128 );

          break;


        case 223 :

          write_code(out,3, 255,255,252,160 );

          break;


        case 241 :

          write_code(out,3, 255,255,252,192 );

          break;


        case 244 :

          write_code(out,3, 255,255,252,224 );

          break;


        case 245 :

          write_code(out,3, 255,255,253,0 );

          break;


        case 246 :

          write_code(out,3, 255,255,253,32 );

          break;


        case 247 :

          write_code(out,3, 255,255,253,64 );

          break;


        case 248 :

          write_code(out,3, 255,255,253,96 );

          break;


        case 250 :

          write_code(out,3, 255,255,253,128 );

          break;


        case 251 :

          write_code(out,3, 255,255,253,160 );

          break;


        case 252 :

          write_code(out,3, 255,255,253,192 );

          break;


        case 253 :

          write_code(out,3, 255,255,253,224 );

          break;


        case 254 :

          write_code(out,3, 255,255,254,0 );

          break;


        case 2 :

          write_code(out,4, 255,255,254,32 );

          break;


        case 3 :

          write_code(out,4, 255,255,254,48 );

          break;


        case 4 :

          write_code(out,4, 255,255,254,64 );

          break;


        case 5 :

          write_code(out,4, 255,255,254,80 );

          break;


        case 6 :

          write_code(out,4, 255,255,254,96 );

          break;


        case 7 :

          write_code(out,4, 255,255,254,112 );

          break;


        case 8 :

          write_code(out,4, 255,255,254,128 );

          break;


        case 11 :

          write_code(out,4, 255,255,254,144 );

          break;


        case 12 :

          write_code(out,4, 255,255,254,160 );

          break;


        case 14 :

          write_code(out,4, 255,255,254,176 );

          break;


        case 15 :

          write_code(out,4, 255,255,254,192 );

          break;


        case 16 :

          write_code(out,4, 255,255,254,208 );

          break;


        case 17 :

          write_code(out,4, 255,255,254,224 );

          break;


        case 18 :

          write_code(out,4, 255,255,254,240 );

          break;


        case 19 :

          write_code(out,4, 255,255,255,0 );

          break;


        case 20 :

          write_code(out,4, 255,255,255,16 );

          break;


        case 21 :

          write_code(out,4, 255,255,255,32 );

          break;


        case 23 :

          write_code(out,4, 255,255,255,48 );

          break;


        case 24 :

          write_code(out,4, 255,255,255,64 );

          break;


        case 25 :

          write_code(out,4, 255,255,255,80 );

          break;


        case 26 :

          write_code(out,4, 255,255,255,96 );

          break;


        case 27 :

          write_code(out,4, 255,255,255,112 );

          break;


        case 28 :

          write_code(out,4, 255,255,255,128 );

          break;


        case 29 :

          write_code(out,4, 255,255,255,144 );

          break;


        case 30 :

          write_code(out,4, 255,255,255,160 );

          break;


        case 31 :

          write_code(out,4, 255,255,255,176 );

          break;


        case 127 :

          write_code(out,4, 255,255,255,192 );

          break;


        case 220 :

          write_code(out,4, 255,255,255,208 );

          break;


        case 249 :

          write_code(out,4, 255,255,255,224 );

          break;


        case 10 :

          write_code(out,6, 255,255,255,240 );

          break;


        case 13 :

          write_code(out,6, 255,255,255,244 );

          break;


        case 22 :

          write_code(out,6, 255,255,255,248 );

          break;


      }
    }
    write_code_padding(out);
    
  }

  final public void encode(String in, E out) {
    current = 0;
    index   = 0;

    int available = in.length();
    for(int i=0; i< available; i++) {
      char b = in.charAt(i) ;
      switch( b ) {


        case 48 :

          write_code(out,5, 0 );

          break;


        case 49 :

          write_code(out,5, 8 );

          break;


        case 50 :

          write_code(out,5, 16 );

          break;


        case 97 :

          write_code(out,5, 24 );

          break;


        case 99 :

          write_code(out,5, 32 );

          break;


        case 101 :

          write_code(out,5, 40 );

          break;


        case 105 :

          write_code(out,5, 48 );

          break;


        case 111 :

          write_code(out,5, 56 );

          break;


        case 115 :

          write_code(out,5, 64 );

          break;


        case 116 :

          write_code(out,5, 72 );

          break;


        case 32 :

          write_code(out,6, 80 );

          break;


        case 37 :

          write_code(out,6, 84 );

          break;


        case 45 :

          write_code(out,6, 88 );

          break;


        case 46 :

          write_code(out,6, 92 );

          break;


        case 47 :

          write_code(out,6, 96 );

          break;


        case 51 :

          write_code(out,6, 100 );

          break;


        case 52 :

          write_code(out,6, 104 );

          break;


        case 53 :

          write_code(out,6, 108 );

          break;


        case 54 :

          write_code(out,6, 112 );

          break;


        case 55 :

          write_code(out,6, 116 );

          break;


        case 56 :

          write_code(out,6, 120 );

          break;


        case 57 :

          write_code(out,6, 124 );

          break;


        case 61 :

          write_code(out,6, 128 );

          break;


        case 65 :

          write_code(out,6, 132 );

          break;


        case 95 :

          write_code(out,6, 136 );

          break;


        case 98 :

          write_code(out,6, 140 );

          break;


        case 100 :

          write_code(out,6, 144 );

          break;


        case 102 :

          write_code(out,6, 148 );

          break;


        case 103 :

          write_code(out,6, 152 );

          break;


        case 104 :

          write_code(out,6, 156 );

          break;


        case 108 :

          write_code(out,6, 160 );

          break;


        case 109 :

          write_code(out,6, 164 );

          break;


        case 110 :

          write_code(out,6, 168 );

          break;


        case 112 :

          write_code(out,6, 172 );

          break;


        case 114 :

          write_code(out,6, 176 );

          break;


        case 117 :

          write_code(out,6, 180 );

          break;


        case 58 :

          write_code(out,7, 184 );

          break;


        case 66 :

          write_code(out,7, 186 );

          break;


        case 67 :

          write_code(out,7, 188 );

          break;


        case 68 :

          write_code(out,7, 190 );

          break;


        case 69 :

          write_code(out,7, 192 );

          break;


        case 70 :

          write_code(out,7, 194 );

          break;


        case 71 :

          write_code(out,7, 196 );

          break;


        case 72 :

          write_code(out,7, 198 );

          break;


        case 73 :

          write_code(out,7, 200 );

          break;


        case 74 :

          write_code(out,7, 202 );

          break;


        case 75 :

          write_code(out,7, 204 );

          break;


        case 76 :

          write_code(out,7, 206 );

          break;


        case 77 :

          write_code(out,7, 208 );

          break;


        case 78 :

          write_code(out,7, 210 );

          break;


        case 79 :

          write_code(out,7, 212 );

          break;


        case 80 :

          write_code(out,7, 214 );

          break;


        case 81 :

          write_code(out,7, 216 );

          break;


        case 82 :

          write_code(out,7, 218 );

          break;


        case 83 :

          write_code(out,7, 220 );

          break;


        case 84 :

          write_code(out,7, 222 );

          break;


        case 85 :

          write_code(out,7, 224 );

          break;


        case 86 :

          write_code(out,7, 226 );

          break;


        case 87 :

          write_code(out,7, 228 );

          break;


        case 89 :

          write_code(out,7, 230 );

          break;


        case 106 :

          write_code(out,7, 232 );

          break;


        case 107 :

          write_code(out,7, 234 );

          break;


        case 113 :

          write_code(out,7, 236 );

          break;


        case 118 :

          write_code(out,7, 238 );

          break;


        case 119 :

          write_code(out,7, 240 );

          break;


        case 120 :

          write_code(out,7, 242 );

          break;


        case 121 :

          write_code(out,7, 244 );

          break;


        case 122 :

          write_code(out,7, 246 );

          break;


        case 38 :

          write_code(out,8, 248 );

          break;


        case 42 :

          write_code(out,8, 249 );

          break;


        case 44 :

          write_code(out,8, 250 );

          break;


        case 59 :

          write_code(out,8, 251 );

          break;


        case 88 :

          write_code(out,8, 252 );

          break;


        case 90 :

          write_code(out,8, 253 );

          break;


        case 33 :

          write_code(out,2, 254,0 );

          break;


        case 34 :

          write_code(out,2, 254,64 );

          break;


        case 40 :

          write_code(out,2, 254,128 );

          break;


        case 41 :

          write_code(out,2, 254,192 );

          break;


        case 63 :

          write_code(out,2, 255,0 );

          break;


        case 39 :

          write_code(out,3, 255,64 );

          break;


        case 43 :

          write_code(out,3, 255,96 );

          break;


        case 124 :

          write_code(out,3, 255,128 );

          break;


        case 35 :

          write_code(out,4, 255,160 );

          break;


        case 62 :

          write_code(out,4, 255,176 );

          break;


        case 0 :

          write_code(out,5, 255,192 );

          break;


        case 36 :

          write_code(out,5, 255,200 );

          break;


        case 64 :

          write_code(out,5, 255,208 );

          break;


        case 91 :

          write_code(out,5, 255,216 );

          break;


        case 93 :

          write_code(out,5, 255,224 );

          break;


        case 126 :

          write_code(out,5, 255,232 );

          break;


        case 94 :

          write_code(out,6, 255,240 );

          break;


        case 125 :

          write_code(out,6, 255,244 );

          break;


        case 60 :

          write_code(out,7, 255,248 );

          break;


        case 96 :

          write_code(out,7, 255,250 );

          break;


        case 123 :

          write_code(out,7, 255,252 );

          break;


        case 92 :

          write_code(out,3, 255,254,0 );

          break;


        case 195 :

          write_code(out,3, 255,254,32 );

          break;


        case 208 :

          write_code(out,3, 255,254,64 );

          break;


        case 128 :

          write_code(out,4, 255,254,96 );

          break;


        case 130 :

          write_code(out,4, 255,254,112 );

          break;


        case 131 :

          write_code(out,4, 255,254,128 );

          break;


        case 162 :

          write_code(out,4, 255,254,144 );

          break;


        case 184 :

          write_code(out,4, 255,254,160 );

          break;


        case 194 :

          write_code(out,4, 255,254,176 );

          break;


        case 224 :

          write_code(out,4, 255,254,192 );

          break;


        case 226 :

          write_code(out,4, 255,254,208 );

          break;


        case 153 :

          write_code(out,5, 255,254,224 );

          break;


        case 161 :

          write_code(out,5, 255,254,232 );

          break;


        case 167 :

          write_code(out,5, 255,254,240 );

          break;


        case 172 :

          write_code(out,5, 255,254,248 );

          break;


        case 176 :

          write_code(out,5, 255,255,0 );

          break;


        case 177 :

          write_code(out,5, 255,255,8 );

          break;


        case 179 :

          write_code(out,5, 255,255,16 );

          break;


        case 209 :

          write_code(out,5, 255,255,24 );

          break;


        case 216 :

          write_code(out,5, 255,255,32 );

          break;


        case 217 :

          write_code(out,5, 255,255,40 );

          break;


        case 227 :

          write_code(out,5, 255,255,48 );

          break;


        case 229 :

          write_code(out,5, 255,255,56 );

          break;


        case 230 :

          write_code(out,5, 255,255,64 );

          break;


        case 129 :

          write_code(out,6, 255,255,72 );

          break;


        case 132 :

          write_code(out,6, 255,255,76 );

          break;


        case 133 :

          write_code(out,6, 255,255,80 );

          break;


        case 134 :

          write_code(out,6, 255,255,84 );

          break;


        case 136 :

          write_code(out,6, 255,255,88 );

          break;


        case 146 :

          write_code(out,6, 255,255,92 );

          break;


        case 154 :

          write_code(out,6, 255,255,96 );

          break;


        case 156 :

          write_code(out,6, 255,255,100 );

          break;


        case 160 :

          write_code(out,6, 255,255,104 );

          break;


        case 163 :

          write_code(out,6, 255,255,108 );

          break;


        case 164 :

          write_code(out,6, 255,255,112 );

          break;


        case 169 :

          write_code(out,6, 255,255,116 );

          break;


        case 170 :

          write_code(out,6, 255,255,120 );

          break;


        case 173 :

          write_code(out,6, 255,255,124 );

          break;


        case 178 :

          write_code(out,6, 255,255,128 );

          break;


        case 181 :

          write_code(out,6, 255,255,132 );

          break;


        case 185 :

          write_code(out,6, 255,255,136 );

          break;


        case 186 :

          write_code(out,6, 255,255,140 );

          break;


        case 187 :

          write_code(out,6, 255,255,144 );

          break;


        case 189 :

          write_code(out,6, 255,255,148 );

          break;


        case 190 :

          write_code(out,6, 255,255,152 );

          break;


        case 196 :

          write_code(out,6, 255,255,156 );

          break;


        case 198 :

          write_code(out,6, 255,255,160 );

          break;


        case 228 :

          write_code(out,6, 255,255,164 );

          break;


        case 232 :

          write_code(out,6, 255,255,168 );

          break;


        case 233 :

          write_code(out,6, 255,255,172 );

          break;


        case 1 :

          write_code(out,7, 255,255,176 );

          break;


        case 135 :

          write_code(out,7, 255,255,178 );

          break;


        case 137 :

          write_code(out,7, 255,255,180 );

          break;


        case 138 :

          write_code(out,7, 255,255,182 );

          break;


        case 139 :

          write_code(out,7, 255,255,184 );

          break;


        case 140 :

          write_code(out,7, 255,255,186 );

          break;


        case 141 :

          write_code(out,7, 255,255,188 );

          break;


        case 143 :

          write_code(out,7, 255,255,190 );

          break;


        case 147 :

          write_code(out,7, 255,255,192 );

          break;


        case 149 :

          write_code(out,7, 255,255,194 );

          break;


        case 150 :

          write_code(out,7, 255,255,196 );

          break;


        case 151 :

          write_code(out,7, 255,255,198 );

          break;


        case 152 :

          write_code(out,7, 255,255,200 );

          break;


        case 155 :

          write_code(out,7, 255,255,202 );

          break;


        case 157 :

          write_code(out,7, 255,255,204 );

          break;


        case 158 :

          write_code(out,7, 255,255,206 );

          break;


        case 165 :

          write_code(out,7, 255,255,208 );

          break;


        case 166 :

          write_code(out,7, 255,255,210 );

          break;


        case 168 :

          write_code(out,7, 255,255,212 );

          break;


        case 174 :

          write_code(out,7, 255,255,214 );

          break;


        case 175 :

          write_code(out,7, 255,255,216 );

          break;


        case 180 :

          write_code(out,7, 255,255,218 );

          break;


        case 182 :

          write_code(out,7, 255,255,220 );

          break;


        case 183 :

          write_code(out,7, 255,255,222 );

          break;


        case 188 :

          write_code(out,7, 255,255,224 );

          break;


        case 191 :

          write_code(out,7, 255,255,226 );

          break;


        case 197 :

          write_code(out,7, 255,255,228 );

          break;


        case 231 :

          write_code(out,7, 255,255,230 );

          break;


        case 239 :

          write_code(out,7, 255,255,232 );

          break;


        case 9 :

          write_code(out,8, 255,255,234 );

          break;


        case 142 :

          write_code(out,8, 255,255,235 );

          break;


        case 144 :

          write_code(out,8, 255,255,236 );

          break;


        case 145 :

          write_code(out,8, 255,255,237 );

          break;


        case 148 :

          write_code(out,8, 255,255,238 );

          break;


        case 159 :

          write_code(out,8, 255,255,239 );

          break;


        case 171 :

          write_code(out,8, 255,255,240 );

          break;


        case 206 :

          write_code(out,8, 255,255,241 );

          break;


        case 215 :

          write_code(out,8, 255,255,242 );

          break;


        case 225 :

          write_code(out,8, 255,255,243 );

          break;


        case 236 :

          write_code(out,8, 255,255,244 );

          break;


        case 237 :

          write_code(out,8, 255,255,245 );

          break;


        case 199 :

          write_code(out,1, 255,255,246,0 );

          break;


        case 207 :

          write_code(out,1, 255,255,246,128 );

          break;


        case 234 :

          write_code(out,1, 255,255,247,0 );

          break;


        case 235 :

          write_code(out,1, 255,255,247,128 );

          break;


        case 192 :

          write_code(out,2, 255,255,248,0 );

          break;


        case 193 :

          write_code(out,2, 255,255,248,64 );

          break;


        case 200 :

          write_code(out,2, 255,255,248,128 );

          break;


        case 201 :

          write_code(out,2, 255,255,248,192 );

          break;


        case 202 :

          write_code(out,2, 255,255,249,0 );

          break;


        case 205 :

          write_code(out,2, 255,255,249,64 );

          break;


        case 210 :

          write_code(out,2, 255,255,249,128 );

          break;


        case 213 :

          write_code(out,2, 255,255,249,192 );

          break;


        case 218 :

          write_code(out,2, 255,255,250,0 );

          break;


        case 219 :

          write_code(out,2, 255,255,250,64 );

          break;


        case 238 :

          write_code(out,2, 255,255,250,128 );

          break;


        case 240 :

          write_code(out,2, 255,255,250,192 );

          break;


        case 242 :

          write_code(out,2, 255,255,251,0 );

          break;


        case 243 :

          write_code(out,2, 255,255,251,64 );

          break;


        case 255 :

          write_code(out,2, 255,255,251,128 );

          break;


        case 203 :

          write_code(out,3, 255,255,251,192 );

          break;


        case 204 :

          write_code(out,3, 255,255,251,224 );

          break;


        case 211 :

          write_code(out,3, 255,255,252,0 );

          break;


        case 212 :

          write_code(out,3, 255,255,252,32 );

          break;


        case 214 :

          write_code(out,3, 255,255,252,64 );

          break;


        case 221 :

          write_code(out,3, 255,255,252,96 );

          break;


        case 222 :

          write_code(out,3, 255,255,252,128 );

          break;


        case 223 :

          write_code(out,3, 255,255,252,160 );

          break;


        case 241 :

          write_code(out,3, 255,255,252,192 );

          break;


        case 244 :

          write_code(out,3, 255,255,252,224 );

          break;


        case 245 :

          write_code(out,3, 255,255,253,0 );

          break;


        case 246 :

          write_code(out,3, 255,255,253,32 );

          break;


        case 247 :

          write_code(out,3, 255,255,253,64 );

          break;


        case 248 :

          write_code(out,3, 255,255,253,96 );

          break;


        case 250 :

          write_code(out,3, 255,255,253,128 );

          break;


        case 251 :

          write_code(out,3, 255,255,253,160 );

          break;


        case 252 :

          write_code(out,3, 255,255,253,192 );

          break;


        case 253 :

          write_code(out,3, 255,255,253,224 );

          break;


        case 254 :

          write_code(out,3, 255,255,254,0 );

          break;


        case 2 :

          write_code(out,4, 255,255,254,32 );

          break;


        case 3 :

          write_code(out,4, 255,255,254,48 );

          break;


        case 4 :

          write_code(out,4, 255,255,254,64 );

          break;


        case 5 :

          write_code(out,4, 255,255,254,80 );

          break;


        case 6 :

          write_code(out,4, 255,255,254,96 );

          break;


        case 7 :

          write_code(out,4, 255,255,254,112 );

          break;


        case 8 :

          write_code(out,4, 255,255,254,128 );

          break;


        case 11 :

          write_code(out,4, 255,255,254,144 );

          break;


        case 12 :

          write_code(out,4, 255,255,254,160 );

          break;


        case 14 :

          write_code(out,4, 255,255,254,176 );

          break;


        case 15 :

          write_code(out,4, 255,255,254,192 );

          break;


        case 16 :

          write_code(out,4, 255,255,254,208 );

          break;


        case 17 :

          write_code(out,4, 255,255,254,224 );

          break;


        case 18 :

          write_code(out,4, 255,255,254,240 );

          break;


        case 19 :

          write_code(out,4, 255,255,255,0 );

          break;


        case 20 :

          write_code(out,4, 255,255,255,16 );

          break;


        case 21 :

          write_code(out,4, 255,255,255,32 );

          break;


        case 23 :

          write_code(out,4, 255,255,255,48 );

          break;


        case 24 :

          write_code(out,4, 255,255,255,64 );

          break;


        case 25 :

          write_code(out,4, 255,255,255,80 );

          break;


        case 26 :

          write_code(out,4, 255,255,255,96 );

          break;


        case 27 :

          write_code(out,4, 255,255,255,112 );

          break;


        case 28 :

          write_code(out,4, 255,255,255,128 );

          break;


        case 29 :

          write_code(out,4, 255,255,255,144 );

          break;


        case 30 :

          write_code(out,4, 255,255,255,160 );

          break;


        case 31 :

          write_code(out,4, 255,255,255,176 );

          break;


        case 127 :

          write_code(out,4, 255,255,255,192 );

          break;


        case 220 :

          write_code(out,4, 255,255,255,208 );

          break;


        case 249 :

          write_code(out,4, 255,255,255,224 );

          break;


        case 10 :

          write_code(out,6, 255,255,255,240 );

          break;


        case 13 :

          write_code(out,6, 255,255,255,244 );

          break;


        case 22 :

          write_code(out,6, 255,255,255,248 );

          break;


      }
    }
    write_code_padding(out);
  }

  static public int compute_size(String in) {
      int nb_bits = 0;
    int available = in.length();
    for(int i=0; i< available; i++) {
      char b = in.charAt(i) ;
      switch( b ) {


        case 48 :

          nb_bits+=5;

          break;


        case 49 :

          nb_bits+=5;

          break;


        case 50 :

          nb_bits+=5;

          break;


        case 97 :

          nb_bits+=5;

          break;


        case 99 :

          nb_bits+=5;

          break;


        case 101 :

          nb_bits+=5;

          break;


        case 105 :

          nb_bits+=5;

          break;


        case 111 :

          nb_bits+=5;

          break;


        case 115 :

          nb_bits+=5;

          break;


        case 116 :

          nb_bits+=5;

          break;


        case 32 :

          nb_bits+=6;

          break;


        case 37 :

          nb_bits+=6;

          break;


        case 45 :

          nb_bits+=6;

          break;


        case 46 :

          nb_bits+=6;

          break;


        case 47 :

          nb_bits+=6;

          break;


        case 51 :

          nb_bits+=6;

          break;


        case 52 :

          nb_bits+=6;

          break;


        case 53 :

          nb_bits+=6;

          break;


        case 54 :

          nb_bits+=6;

          break;


        case 55 :

          nb_bits+=6;

          break;


        case 56 :

          nb_bits+=6;

          break;


        case 57 :

          nb_bits+=6;

          break;


        case 61 :

          nb_bits+=6;

          break;


        case 65 :

          nb_bits+=6;

          break;


        case 95 :

          nb_bits+=6;

          break;


        case 98 :

          nb_bits+=6;

          break;


        case 100 :

          nb_bits+=6;

          break;


        case 102 :

          nb_bits+=6;

          break;


        case 103 :

          nb_bits+=6;

          break;


        case 104 :

          nb_bits+=6;

          break;


        case 108 :

          nb_bits+=6;

          break;


        case 109 :

          nb_bits+=6;

          break;


        case 110 :

          nb_bits+=6;

          break;


        case 112 :

          nb_bits+=6;

          break;


        case 114 :

          nb_bits+=6;

          break;


        case 117 :

          nb_bits+=6;

          break;


        case 58 :

          nb_bits+=7;

          break;


        case 66 :

          nb_bits+=7;

          break;


        case 67 :

          nb_bits+=7;

          break;


        case 68 :

          nb_bits+=7;

          break;


        case 69 :

          nb_bits+=7;

          break;


        case 70 :

          nb_bits+=7;

          break;


        case 71 :

          nb_bits+=7;

          break;


        case 72 :

          nb_bits+=7;

          break;


        case 73 :

          nb_bits+=7;

          break;


        case 74 :

          nb_bits+=7;

          break;


        case 75 :

          nb_bits+=7;

          break;


        case 76 :

          nb_bits+=7;

          break;


        case 77 :

          nb_bits+=7;

          break;


        case 78 :

          nb_bits+=7;

          break;


        case 79 :

          nb_bits+=7;

          break;


        case 80 :

          nb_bits+=7;

          break;


        case 81 :

          nb_bits+=7;

          break;


        case 82 :

          nb_bits+=7;

          break;


        case 83 :

          nb_bits+=7;

          break;


        case 84 :

          nb_bits+=7;

          break;


        case 85 :

          nb_bits+=7;

          break;


        case 86 :

          nb_bits+=7;

          break;


        case 87 :

          nb_bits+=7;

          break;


        case 89 :

          nb_bits+=7;

          break;


        case 106 :

          nb_bits+=7;

          break;


        case 107 :

          nb_bits+=7;

          break;


        case 113 :

          nb_bits+=7;

          break;


        case 118 :

          nb_bits+=7;

          break;


        case 119 :

          nb_bits+=7;

          break;


        case 120 :

          nb_bits+=7;

          break;


        case 121 :

          nb_bits+=7;

          break;


        case 122 :

          nb_bits+=7;

          break;


        case 38 :

          nb_bits+=8;

          break;


        case 42 :

          nb_bits+=8;

          break;


        case 44 :

          nb_bits+=8;

          break;


        case 59 :

          nb_bits+=8;

          break;


        case 88 :

          nb_bits+=8;

          break;


        case 90 :

          nb_bits+=8;

          break;


        case 33 :

          nb_bits+=2+8;

          break;


        case 34 :

          nb_bits+=2+8;

          break;


        case 40 :

          nb_bits+=2+8;

          break;


        case 41 :

          nb_bits+=2+8;

          break;


        case 63 :

          nb_bits+=2+8;

          break;


        case 39 :

          nb_bits+=3+8;

          break;


        case 43 :

          nb_bits+=3+8;

          break;


        case 124 :

          nb_bits+=3+8;

          break;


        case 35 :

          nb_bits+=4+8;

          break;


        case 62 :

          nb_bits+=4+8;

          break;


        case 0 :

          nb_bits+=5+8;

          break;


        case 36 :

          nb_bits+=5+8;

          break;


        case 64 :

          nb_bits+=5+8;

          break;


        case 91 :

          nb_bits+=5+8;

          break;


        case 93 :

          nb_bits+=5+8;

          break;


        case 126 :

          nb_bits+=5+8;

          break;


        case 94 :

          nb_bits+=6+8;

          break;


        case 125 :

          nb_bits+=6+8;

          break;


        case 60 :

          nb_bits+=7+8;

          break;


        case 96 :

          nb_bits+=7+8;

          break;


        case 123 :

          nb_bits+=7+8;

          break;


        case 92 :

          nb_bits+=3+16;

          break;


        case 195 :

          nb_bits+=3+16;

          break;


        case 208 :

          nb_bits+=3+16;

          break;


        case 128 :

          nb_bits+=4+16;

          break;


        case 130 :

          nb_bits+=4+16;

          break;


        case 131 :

          nb_bits+=4+16;

          break;


        case 162 :

          nb_bits+=4+16;

          break;


        case 184 :

          nb_bits+=4+16;

          break;


        case 194 :

          nb_bits+=4+16;

          break;


        case 224 :

          nb_bits+=4+16;

          break;


        case 226 :

          nb_bits+=4+16;

          break;


        case 153 :

          nb_bits+=5+16;

          break;


        case 161 :

          nb_bits+=5+16;

          break;


        case 167 :

          nb_bits+=5+16;

          break;


        case 172 :

          nb_bits+=5+16;

          break;


        case 176 :

          nb_bits+=5+16;

          break;


        case 177 :

          nb_bits+=5+16;

          break;


        case 179 :

          nb_bits+=5+16;

          break;


        case 209 :

          nb_bits+=5+16;

          break;


        case 216 :

          nb_bits+=5+16;

          break;


        case 217 :

          nb_bits+=5+16;

          break;


        case 227 :

          nb_bits+=5+16;

          break;


        case 229 :

          nb_bits+=5+16;

          break;


        case 230 :

          nb_bits+=5+16;

          break;


        case 129 :

          nb_bits+=6+16;

          break;


        case 132 :

          nb_bits+=6+16;

          break;


        case 133 :

          nb_bits+=6+16;

          break;


        case 134 :

          nb_bits+=6+16;

          break;


        case 136 :

          nb_bits+=6+16;

          break;


        case 146 :

          nb_bits+=6+16;

          break;


        case 154 :

          nb_bits+=6+16;

          break;


        case 156 :

          nb_bits+=6+16;

          break;


        case 160 :

          nb_bits+=6+16;

          break;


        case 163 :

          nb_bits+=6+16;

          break;


        case 164 :

          nb_bits+=6+16;

          break;


        case 169 :

          nb_bits+=6+16;

          break;


        case 170 :

          nb_bits+=6+16;

          break;


        case 173 :

          nb_bits+=6+16;

          break;


        case 178 :

          nb_bits+=6+16;

          break;


        case 181 :

          nb_bits+=6+16;

          break;


        case 185 :

          nb_bits+=6+16;

          break;


        case 186 :

          nb_bits+=6+16;

          break;


        case 187 :

          nb_bits+=6+16;

          break;


        case 189 :

          nb_bits+=6+16;

          break;


        case 190 :

          nb_bits+=6+16;

          break;


        case 196 :

          nb_bits+=6+16;

          break;


        case 198 :

          nb_bits+=6+16;

          break;


        case 228 :

          nb_bits+=6+16;

          break;


        case 232 :

          nb_bits+=6+16;

          break;


        case 233 :

          nb_bits+=6+16;

          break;


        case 1 :

          nb_bits+=7+16;

          break;


        case 135 :

          nb_bits+=7+16;

          break;


        case 137 :

          nb_bits+=7+16;

          break;


        case 138 :

          nb_bits+=7+16;

          break;


        case 139 :

          nb_bits+=7+16;

          break;


        case 140 :

          nb_bits+=7+16;

          break;


        case 141 :

          nb_bits+=7+16;

          break;


        case 143 :

          nb_bits+=7+16;

          break;


        case 147 :

          nb_bits+=7+16;

          break;


        case 149 :

          nb_bits+=7+16;

          break;


        case 150 :

          nb_bits+=7+16;

          break;


        case 151 :

          nb_bits+=7+16;

          break;


        case 152 :

          nb_bits+=7+16;

          break;


        case 155 :

          nb_bits+=7+16;

          break;


        case 157 :

          nb_bits+=7+16;

          break;


        case 158 :

          nb_bits+=7+16;

          break;


        case 165 :

          nb_bits+=7+16;

          break;


        case 166 :

          nb_bits+=7+16;

          break;


        case 168 :

          nb_bits+=7+16;

          break;


        case 174 :

          nb_bits+=7+16;

          break;


        case 175 :

          nb_bits+=7+16;

          break;


        case 180 :

          nb_bits+=7+16;

          break;


        case 182 :

          nb_bits+=7+16;

          break;


        case 183 :

          nb_bits+=7+16;

          break;


        case 188 :

          nb_bits+=7+16;

          break;


        case 191 :

          nb_bits+=7+16;

          break;


        case 197 :

          nb_bits+=7+16;

          break;


        case 231 :

          nb_bits+=7+16;

          break;


        case 239 :

          nb_bits+=7+16;

          break;


        case 9 :

          nb_bits+=8+16;

          break;


        case 142 :

          nb_bits+=8+16;

          break;


        case 144 :

          nb_bits+=8+16;

          break;


        case 145 :

          nb_bits+=8+16;

          break;


        case 148 :

          nb_bits+=8+16;

          break;


        case 159 :

          nb_bits+=8+16;

          break;


        case 171 :

          nb_bits+=8+16;

          break;


        case 206 :

          nb_bits+=8+16;

          break;


        case 215 :

          nb_bits+=8+16;

          break;


        case 225 :

          nb_bits+=8+16;

          break;


        case 236 :

          nb_bits+=8+16;

          break;


        case 237 :

          nb_bits+=8+16;

          break;


        case 199 :

          nb_bits+=1+24;

          break;


        case 207 :

          nb_bits+=1+24;

          break;


        case 234 :

          nb_bits+=1+24;

          break;


        case 235 :

          nb_bits+=1+24;

          break;


        case 192 :

          nb_bits+=2+24;

          break;


        case 193 :

          nb_bits+=2+24;

          break;


        case 200 :

          nb_bits+=2+24;

          break;


        case 201 :

          nb_bits+=2+24;

          break;


        case 202 :

          nb_bits+=2+24;

          break;


        case 205 :

          nb_bits+=2+24;

          break;


        case 210 :

          nb_bits+=2+24;

          break;


        case 213 :

          nb_bits+=2+24;

          break;


        case 218 :

          nb_bits+=2+24;

          break;


        case 219 :

          nb_bits+=2+24;

          break;


        case 238 :

          nb_bits+=2+24;

          break;


        case 240 :

          nb_bits+=2+24;

          break;


        case 242 :

          nb_bits+=2+24;

          break;


        case 243 :

          nb_bits+=2+24;

          break;


        case 255 :

          nb_bits+=2+24;

          break;


        case 203 :

          nb_bits+=3+24;

          break;


        case 204 :

          nb_bits+=3+24;

          break;


        case 211 :

          nb_bits+=3+24;

          break;


        case 212 :

          nb_bits+=3+24;

          break;


        case 214 :

          nb_bits+=3+24;

          break;


        case 221 :

          nb_bits+=3+24;

          break;


        case 222 :

          nb_bits+=3+24;

          break;


        case 223 :

          nb_bits+=3+24;

          break;


        case 241 :

          nb_bits+=3+24;

          break;


        case 244 :

          nb_bits+=3+24;

          break;


        case 245 :

          nb_bits+=3+24;

          break;


        case 246 :

          nb_bits+=3+24;

          break;


        case 247 :

          nb_bits+=3+24;

          break;


        case 248 :

          nb_bits+=3+24;

          break;


        case 250 :

          nb_bits+=3+24;

          break;


        case 251 :

          nb_bits+=3+24;

          break;


        case 252 :

          nb_bits+=3+24;

          break;


        case 253 :

          nb_bits+=3+24;

          break;


        case 254 :

          nb_bits+=3+24;

          break;


        case 2 :

          nb_bits+=4+24;

          break;


        case 3 :

          nb_bits+=4+24;

          break;


        case 4 :

          nb_bits+=4+24;

          break;


        case 5 :

          nb_bits+=4+24;

          break;


        case 6 :

          nb_bits+=4+24;

          break;


        case 7 :

          nb_bits+=4+24;

          break;


        case 8 :

          nb_bits+=4+24;

          break;


        case 11 :

          nb_bits+=4+24;

          break;


        case 12 :

          nb_bits+=4+24;

          break;


        case 14 :

          nb_bits+=4+24;

          break;


        case 15 :

          nb_bits+=4+24;

          break;


        case 16 :

          nb_bits+=4+24;

          break;


        case 17 :

          nb_bits+=4+24;

          break;


        case 18 :

          nb_bits+=4+24;

          break;


        case 19 :

          nb_bits+=4+24;

          break;


        case 20 :

          nb_bits+=4+24;

          break;


        case 21 :

          nb_bits+=4+24;

          break;


        case 23 :

          nb_bits+=4+24;

          break;


        case 24 :

          nb_bits+=4+24;

          break;


        case 25 :

          nb_bits+=4+24;

          break;


        case 26 :

          nb_bits+=4+24;

          break;


        case 27 :

          nb_bits+=4+24;

          break;


        case 28 :

          nb_bits+=4+24;

          break;


        case 29 :

          nb_bits+=4+24;

          break;


        case 30 :

          nb_bits+=4+24;

          break;


        case 31 :

          nb_bits+=4+24;

          break;


        case 127 :

          nb_bits+=4+24;

          break;


        case 220 :

          nb_bits+=4+24;

          break;


        case 249 :

          nb_bits+=4+24;

          break;


        case 10 :

          nb_bits+=6+24;

          break;


        case 13 :

          nb_bits+=6+24;

          break;


        case 22 :

          nb_bits+=6+24;

          break;


      }
    }
    if ( (nb_bits & 0x7) == 0)
      return nb_bits >> 3;
    else
      return (nb_bits >> 3)+1;
  }


}

