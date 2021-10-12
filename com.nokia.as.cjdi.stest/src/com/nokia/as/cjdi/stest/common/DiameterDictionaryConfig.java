// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.common;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

@SuppressWarnings("restriction")
@Config(section = "DiameterConfig")
public interface DiameterDictionaryConfig {
    
    @FileDataProperty(fileData="avpDic.txt", title = "AVP Dictionary")
    public String getAVPDictionary();
	
    @FileDataProperty(fileData="commandDic.txt", title = "Command Dictionary")
    public String getCommandDictionary();

}