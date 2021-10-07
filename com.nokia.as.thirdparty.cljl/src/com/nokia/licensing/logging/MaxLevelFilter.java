package com.nokia.licensing.logging;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;


public class MaxLevelFilter implements Filter {
    private final int maxLevel;

    public MaxLevelFilter(final Level maxLevel) {
        this.maxLevel = maxLevel.intValue();
    }

    @Override
    public boolean isLoggable(final LogRecord record) {
        return record.getLevel().intValue() <= this.maxLevel;
    }
}
