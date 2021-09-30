package com.alcatel_lucent.as.management.annotation.stat;

/**
 * This enum is used by Counter and Gauge annotation, in order to define the consolidation mode.
 * (i.e the operation that should be applied on all counter instance values to calculate the consolidated value )
 * Choose NONE means to do not generate a consolidated value for a counter or a gauge.
 */
public enum ConsolidationMode {
	/* Sum */
	SUM,
	/* Average */
	AVERAGE,
	/* Minimum */
	MIN,
	/* Maximum */
	MAX,
	/* when a consolidation has no sense for a counter or gauge */
	NONE

}
