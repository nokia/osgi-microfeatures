package com.alcatel.as.service.metering.impl.renderer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.or.ObjectRenderer;

import com.alcatel.as.service.metering.Stat;

/**
 * Allows to generate some debug informations when some configured meters values reachs a 
 * given water mark. For now, this class performs a dump stack when one of the specified meters values
 * are greater than the specified ones.
 * 
 * Usage:
 * 
 * 1) from log4j configuration, define the following renderer for the metering Stat object:
 *    
 *   log4j.renderer.com.alcatel.as.service.metering.Stat=com.alcatel.as.service.metering.impl.StatRenderer
 *   
 * 2) in jvm parameters, you can define three properties:
 * 
 *   -DMeteringStatRenderer.average (list of meters whose average must be checked)
 *   -DMeteringStatRenderer.max (list of meters whose max value must be checked)
 *   -DMeteringStatRenderer.value (list of meters whose value must be checked)
 *   
 * Each property must contain a list of meters (comma separated), and each meter must have the syntax: 
 * "meter:maxvalue".
 * 
 * Examples: the following will dump a stack trace if one of the following condition is met:
 * (OR conditions):
 * 
 *    - the average of meter as.stat.meter1 is >= 90
 *    - the averate of meter as.stat.meter2 is >= 50
 *    - the max value of meter as.stat.meter3 is >= 30
 *    - the max value of meter as.stat.meter4 is >= 20
 *    - the value of meter as.stat.meter5 is >= 10
 *    - the value of meter as.stat.meter6 is >= 20
 * 
 *   -DMeteringStatRenderer.average=as.stat.meter1:90, as.stat.meter2:50
 *   -DMeteringStatRenderer.max=as.stat.meter3:30, as.stat.meter4:20
 *   -DMeteringStatRenderer.value=as.stat.meter5:10, as.stat.meter6:20
 */
public class StatRenderer implements ObjectRenderer
{
    private final Map<String, Tracker> _tracked = new HashMap<String, StatRenderer.Tracker>();
    private final static String MAX = "man";
    private final static String VALUE = "value";
    private final static String AVERAGE = "average";

    private abstract static class Tracker
    {
        protected final int _max;

        protected Tracker(int max)
        {
            _max = max;
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + " (max=" + _max + ")";
        }

        abstract boolean isOverloaded(Stat stat);
    }

    private static class ValueTracker extends Tracker
    {
        ValueTracker(int max)
        {
            super(max);
        }

        @Override
        public boolean isOverloaded(Stat stat)
        {
            return stat.getValue() >= _max;
        }
    }

    private static class MaxTracker extends Tracker
    {
        MaxTracker(int max)
        {
            super(max);
        }

        @Override
        public boolean isOverloaded(Stat stat)
        {
            return stat.getMax() >= _max;
        }
    }

    private static class AverageTracker extends Tracker
    {
        AverageTracker(int max)
        {
            super(max);
        }

        @Override
        public boolean isOverloaded(Stat stat)
        {
            return stat.getMean() >= _max;
        }
    }

    public StatRenderer()
    {
        // parse meters to track from system configuration
        parse("MeteringStatRenderer." + MAX, "MeteringStatRenderer." + VALUE, "MeteringStatRenderer."
                + AVERAGE);
        if (_tracked.size() > 0)
        {
            System.out.println("Metering Stat Renderer active: tracked meters: " + _tracked);
        }
    }

    @Override
    public String doRender(Object obj)
    {
        Stat stat = (Stat) obj;
        // Check if one of the configured tracked meters is overloaded, and
        // dump a stack trace if correct.

        Tracker tracker = _tracked.get(stat.getMeter().getName());
        if (tracker != null && tracker.isOverloaded(stat))
        {
            StringBuilder log = new StringBuilder(stat.toString());
            log.append("\nDetetected overloaded meter: " + stat.getMeter().getName()
                    + ": dumping stack traces:\n");
            Map<Thread, StackTraceElement[]> mapStacks = Thread.getAllStackTraces();
            Iterator<Thread> threads = mapStacks.keySet().iterator();
            while (threads.hasNext())
            {
                Thread thread = threads.next();
                StackTraceElement[] stes = mapStacks.get(thread);
                log.append("\nThread [")
                        .append(thread.getName())
                        .append(" state=")
                        .append(thread.getState())
                        .append("] -->\n");
                for (StackTraceElement ste : stes)
                {
                    log.append("\t").append(ste.toString()).append("\n");
                }
            }
            return log.toString();
        }
        return stat.toString();
    }

    private void parse(String... properties)
    {
        for (String property : properties)
        {
            String meters = System.getProperty(property, "");
            for (String meter : meters.split(","))
            {
                meter = meter.trim();
                if (meter.length() > 0)
                {
                    String[] infos = meter.split(":");
                    String meterName = infos[0];
                    int maxValue = Integer.valueOf(infos[1]);

                    if (property.endsWith(MAX))
                    {
                        _tracked.put(meterName, new MaxTracker(maxValue));
                    }
                    else if (property.endsWith(VALUE))
                    {
                        _tracked.put(meterName, new ValueTracker(maxValue));
                    }
                    else if (property.endsWith(AVERAGE))
                    {
                        _tracked.put(meterName, new AverageTracker(maxValue));
                    }
                }
            }
        }
    }
}
