// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;
import org.apache.log4j.Logger;
import com.alcatel.as.http2.Http2Error;

class ApiAdapter implements HeaderField.HeaderOnTheFly {
    private final HPACKDecoder.DecoderSouth south;
    private final long cid;
    private final Logger logger;

    public ApiAdapter(HPACKDecoder.DecoderSouth south, Logger logger, long connection_id) {
        this.south = south;
        this.logger = logger;
        this.cid = connection_id;
    }

    @Override
    public void add_header(String name, String value) {
        assert name != null && value != null;
        try {
            south.add(name, value);
	} catch(Http2Error he){
	    throw he;
        } catch(Exception e) {
            logger.warn("unexpected exception adding header:("+name+") ("+value+")",e);
        }
    }

    @Override
    public void add_header(Header well_known, String value) {
        assert well_known != null && value != null;
        try {
            south.add(well_known, value);
        } catch(Http2Error he){
	    throw he;
        } catch(Exception e) {
            logger.warn("unexpected exception adding header:["+well_known+"] ("+value+")",e);
        }
    }

    @Override
    public void add_header(Header well_known) {
        assert well_known != null;
        try {
            south.add(well_known);
        } catch(Http2Error he){
	    throw he;
        } catch(Exception e) {
            logger.warn("unexpected exception adding header:["+well_known+"]",e);
        }
    }

}
