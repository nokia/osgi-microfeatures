// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.recorder;

import java.time.LocalDateTime;

public class Event {

    private final String message;
    private final LocalDateTime time;
    
    public Event (String message){
	this.message = message;
	time = LocalDateTime.now ();
    }

    public String message (){ return message;}
    public LocalDateTime time (){ return time;}

}
