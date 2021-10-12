// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

import alcatel.tess.hometop.gateways.utils.Charset;

import com.nextenso.mux.util.MuxUtils;

/**
 * This class parses DNS responses from the stack.
 * 
 * @deprecated This class may be removed in the future. Please use the DNS
 *             Service instead of this class.
 * @internal
 */
@Deprecated
public class DNSParser
{

    private static String[] VOID = new String[0];

    public static String[] parseDNSResponse(int flags, int sockId, byte[] data, int off)
    {
        if (flags == (MuxProtocol.PROTOCOL_DNS | MuxProtocol.ACTION_GET_BY_ADDR)
                || flags == (MuxProtocol.PROTOCOL_DNS | MuxProtocol.ACTION_GET_BY_NAME))
            return parseDNSResponse(sockId, data, off);
        return null;
    }

    public static String[] parseDNSResponse(int sockId, byte[] data, int offset)
    {
        String[] response = VOID;
        int off = offset;
        try
        {
            if (sockId == 0)
            {
                int n = MuxUtils.get_16(data, off, true);
                if (n > 0)
                {
                    off += 2;
                    response = new String[n];
                    int end = off + 1;
                    for (int i = 0; i < n; i++)
                    {
                        while (data[end++] != (byte) ';')
                        {
                        }
                        response[i] = Charset.makeString(data, off, end - off - 1);
                        off = end++;
                    }
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(); // should never happen
            response = VOID;
        }
        return response;
    }

}
