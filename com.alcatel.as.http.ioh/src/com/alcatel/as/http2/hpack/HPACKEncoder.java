// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;
import org.apache.log4j.Logger;

public class HPACKEncoder {

    private final HeaderFieldEncoderBO    encoder;
    private final Logger                  logger;
    private final ExceptionTrapSouthProxy south_proxy;

    private       boolean                 is_safe_to_resize = true;
    private       boolean                 is_resize_pending = false;
    private       int                     resize_pending_sz = -1;

    public HPACKEncoder (Config config) {
        this.logger = config.getLogger();
        encoder = new HeaderFieldEncoderBO(config.getHeaderTableSize(),logger, config.getConnectionId());
        this.south_proxy = new ExceptionTrapSouthProxy();
    }

    public void update (Config config) {
        assert is_safe_to_resize : "Can't call update in the middle of an encoding session";
        if (is_safe_to_resize) {

            if (is_resize_pending) {
                /*
                 RFC-7541:
                 4.2.  Maximum Table Size
                 Multiple updates to the maximum table size can occur between the
                 transmission of two header blocks.  In the case that this size is
                 changed more than once in this interval, the smallest maximum table
                 size that occurs in that interval MUST be signaled in a dynamic table
                 size update.
                 */
                resize_pending_sz = Math.min(resize_pending_sz, config.getHeaderTableSize());
            }
            else {
                resize_pending_sz = config.getHeaderTableSize();
                is_resize_pending = true;
            }
        } else throw new IllegalStateException("Can't call update in the middle of an encoding session");
    }

    private void resize(ExceptionTrapSouthProxy south_proxy) {
        if (is_resize_pending) {
            assert resize_pending_sz != -1 : "Illegal value for dynamic table resizing";
            is_resize_pending = false;
            encoder.update(resize_pending_sz,south_proxy);
            resize_pending_sz = -1;
        }
    }

    public void encode (Header h, EncoderSouth south) {
        assert south != null;
        // XXX : use a method instead of this hack
        assert h.name_value_code != Header.DO_NOT_USE;
        is_safe_to_resize = false;
        south_proxy.proxy = south;
        if (is_resize_pending) resize(south_proxy);
        encoder.encode(h,south_proxy);
        south_proxy.proxy=null;
    }

    public void encode (Header h, String value, EncoderSouth south) {
        assert south != null;
        is_safe_to_resize = false;
        south_proxy.proxy = south;
        if (is_resize_pending) resize(south_proxy);
        encoder.encode(h,value,south_proxy);
        south_proxy.proxy=null;
    }

    public void encode (String name, String value, EncoderSouth south) {
        assert south != null;
        is_safe_to_resize = false;
        south_proxy.proxy = south;
        if (is_resize_pending) resize(south_proxy);
        encoder.encode(name,value,south_proxy);
        south_proxy.proxy=null;
    }

    public void last_or_end_or_finish_or_stop_or_complete (EncoderSouth south) {
        assert south != null;
        encoder.last();
        south_proxy.proxy = south;
        if (is_resize_pending) resize(south_proxy);
        south_proxy.last_or_end_or_finish_or_stop_or_complete();
        south_proxy.proxy=null;
        is_safe_to_resize = true;
    }

    public interface EncoderSouth extends ByteOutput {
        void last_or_end_or_finish_or_stop_or_complete();
        void error();
    }

    private class ExceptionTrapSouthProxy implements EncoderSouth {
        private EncoderSouth proxy;
        @Override
        public void last_or_end_or_finish_or_stop_or_complete() {
            try {
                proxy.last_or_end_or_finish_or_stop_or_complete();
            } catch (Exception e) {
                logger.warn("exception during delegation end of headers.",e);
                proxy.error();
            }
        }

        @Override
        public void error() {
            try {
                proxy.error();
                is_safe_to_resize = true;
            } catch (Exception e) {
                logger.warn("exception during delegation error handling.",e);
                proxy.error();
                is_safe_to_resize = true;
            }
        }

        @Override
        public void put(byte b) {
            try {
                proxy.put(b);
            } catch (Exception e) {
                logger.warn("exception during delegation of byte writing.",e);
                proxy.error();
                is_safe_to_resize = true;
            }
        }

        @Override
        public void put(byte[] bytes) {
            try {
                proxy.put(bytes);
            } catch (Exception e) {
                logger.warn("exception during delegation of bytes writing.",e);
                proxy.error();
                is_safe_to_resize = true;
            }
        }
    }

}
