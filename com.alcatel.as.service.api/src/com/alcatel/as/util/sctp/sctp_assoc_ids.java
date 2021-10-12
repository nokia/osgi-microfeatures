// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

import java.util.Arrays;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * struct sctp_assoc_ids {
 *	__u32			gaids_number_of_ids;
 *	sctp_assoc_t	gaids_assoc_id[];
 * };
 */
public class sctp_assoc_ids implements SctpSocketParam {
	public long gaids_number_of_ids;
	public long[] gaids_assoc_id;

	public sctp_assoc_ids() { }
	
	public sctp_assoc_ids(long gaids_number_of_ids, long[] gaids_assoc_id) {
		this.gaids_number_of_ids = gaids_number_of_ids;
		this.gaids_assoc_id = gaids_assoc_id;
	}

	@Override
	public String toString() {
		return "sctp_assoc_ids [gaids_number_of_ids=" + gaids_number_of_ids + ", gaids_assoc_id="
				+ Arrays.toString(gaids_assoc_id) + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(gaids_number_of_ids);
		
		int len = gaids_assoc_id.length;
		out.writeInt(len);
		for(int i = 0; i < len; i++) out.writeLong(gaids_assoc_id[i]);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		gaids_number_of_ids = in.readLong();

		int len = in.readInt();
		gaids_assoc_id = new long[len];
		for(int i = 0; i < len; i++) gaids_assoc_id[i] = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		return other;
	}
}
