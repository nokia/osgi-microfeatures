// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb;

import java.nio.charset.Charset;

public abstract class UserLocator {

    public static final String DEF_GROUP = "def";

    protected static Charset UTF8 = null;
    static {
	try{ UTF8 = Charset.forName ("utf-8");
	}catch(Exception e){}// cannot happen
    }
    protected static String getKeyAsString (byte[] key, int keyOff, int keyLen){
	if (key == null) return null;
	return new String (key, keyOff, keyLen, UTF8);
    }
    
    public abstract void getLocation (byte[] key, int keyOff, int keyLen, java.util.function.Consumer<String> cb);
    
    
}
