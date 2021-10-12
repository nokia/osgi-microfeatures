// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import java.nio.ByteBuffer;

import com.alcatel.as.http2.headers.Header;
import org.apache.log4j.Logger;
import com.alcatel.as.http2.Http2Error;

public class HPACKDecoder {

    private final DynamicTable dt;
    private final HeaderField  decoder ;
    private final long         cid;
    private final Logger       logger;

    private       boolean      is_safe_to_resize = true;

    public HPACKDecoder(Config config) {
        this.dt = new DynamicTable(config.getHeaderTableSize(),config.getLogger(),config.getConnectionId());
        this.decoder = new HeaderField(dt,config.getLogger(),config.getConnectionId());
        logger = config.getLogger();
        cid = config.getConnectionId();
    }

    /**
     * is this really necessary since it won't last longer than the
     * @param config
     */
    public void update(Config config) {
        assert is_safe_to_resize : "Can't call update in the middle of an encoding session";
        if (is_safe_to_resize) {
            dt.resize(config.getHeaderTableSize());
        } else throw new IllegalStateException("Can't call update in the middle of an encoding session");
    }

    private byte[] accu = null;
    public void decode(ByteBuffer buffer,DecoderSouth south) {
        is_safe_to_resize = false;
        try {
            ApiAdapter hotf = new ApiAdapter(south, logger, cid);
            if (accu == null) {
                int result = decoder.parser(buffer, hotf);
                if (result == decoder.ERROR)
                    south.error();
                if (result == decoder.UNDERFLOW) {
                    assert decoder.bb_position != -1;
                    buffer.position(decoder.bb_position);
                    accu = new byte[buffer.remaining()];
                    buffer.get(accu);
                }
            } else {
                ByteBuffer buffer_ = ByteBuffer.allocate(accu.length + buffer.remaining());
                buffer_.put(accu);
                accu = accu = new byte[buffer.remaining()];
                buffer.get(accu);
                buffer_.put(accu);
                buffer_.flip();
                accu = null;
                decode(buffer_, south);
            }
	} catch (Http2Error he) {
	    throw he;
        } catch (Exception e) {
            if (decoder!=null && decoder.bb_position!=-1)
              logger.warn("unexpected exception during decoding @"+decoder.bb_position,e);
            else
              logger.warn("unexpected exception during decoding",e);
            accu=null;
            south.error();
            is_safe_to_resize = true;
        }
    }

    public void last_or_end_or_finish_or_stop_or_complete(DecoderSouth south) {
        try {
            if (accu != null) {
                logger.warn("left-over data " + accu.length + " byte(s)");
                accu = null;
                south.error();
            }
            south.last_or_end_or_finish_or_stop_or_complete();
            is_safe_to_resize = true;
        } catch (Http2Error he) {
	    throw he;
        } catch (Exception e) {
            logger.warn("unexpected exception closing headers.",e);
            is_safe_to_resize = true;
        }
    }

    public interface DecoderSouth {

        void add(Header h);

        void add(Header h, String value);

        void add(String name, String value);

        void last_or_end_or_finish_or_stop_or_complete();

        void error();

    }

}
