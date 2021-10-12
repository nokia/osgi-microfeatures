// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.ValueSupplier;

public class Util {

	public static class MergedMeter implements ValueSupplier {
		private Operation _op;
		List<Meter> _meters = new CopyOnWriteArrayList<>();

		public MergedMeter(Operation op) {
			_op = op;
		}

		public long getValue() {
			return _op.getValue(_meters);
		}
	}

	public static interface Operation {
		public long getValue(List<Meter> meters);
	}

	private static Operation OP_ADD = new Operation() {
		public long getValue(List<Meter> meters) {
			long l = 0L;
			for (Meter meter : meters) {
				l += meter.getValue();
			}
			return l;
		}
	};
	
	
	private static Operation OP_AVG = new Operation() {
		public long getValue(List<Meter> meters) {
			int nb = meters.size();
			if (nb == 0)
				return 0L;
			return OP_ADD.getValue(meters) / nb;
		}
	};
	
	
	private static Operation OP_MAX = new Operation() {
		public long getValue(List<Meter> meters) {
			if (meters.size() == 0)
				return -1L;
			long max = 0L;
			for (Meter meter : meters) {
				long l = meter.getValue();
				if (l > max)
					max = l;
			}
			return max;
		}
	};
	
	
	private static Operation OP_MIN = new Operation() {
		public long getValue(List<Meter> meters) {
			if (meters.size() == 0)
				return -1L;
			long min = Long.MAX_VALUE;
			for (Meter meter : meters) {
				long l = meter.getValue();
				if (l < min)
					min = l;
			}
			return min;
		}
	};
	
	
	private static Operation OP_OR = new Operation() {
		public long getValue(List<Meter> meters) {
			for (Meter meter : meters) {
				if (meter.getValue() == 1L)
					return 1L;
			}
			return 0L;
		}
	};
	
	
	private static Operation OP_NOR = new Operation() {
		public long getValue(List<Meter> meters) {
			return OP_OR.getValue(meters) == 0L ? 1L : 0L;
		}
	};
	
	
	private static Operation OP_AND = new Operation() {
		public long getValue(List<Meter> meters) {
			if (meters.size() == 0)
				return 0L;
			for (Meter meter : meters) {
				if (meter.getValue() == 0L)
					return 0L;
			}
			return 1L;
		}
	};
	
	
	private static Operation OP_NAND = new Operation() {
		public long getValue(List<Meter> meters) {
			return OP_AND.getValue(meters) == 0L ? 1L : 0L;
		}
	};
}
