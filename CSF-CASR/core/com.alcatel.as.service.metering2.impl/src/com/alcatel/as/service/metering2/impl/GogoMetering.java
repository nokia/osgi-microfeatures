package com.alcatel.as.service.metering2.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.impl.DerivedMeters2.MergedMeters;
import com.alcatel.as.service.metering2.impl.util.Util.MergedMeter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.util.Meters;
import com.alcatel.as.service.metering2.util.MeteringRegistry;

import static com.alcatel.as.util.helper.AnsiFormatter.*;

@Descriptor("ASR Metering Service Commands")
public class GogoMetering {
  
  private volatile MeteringService _meteringService;
  private volatile MeteringRegistry _meteringRegistry;
  
  private volatile DerivedMeters2 _derivedMeters2;
  
  
  private ConcurrentMap<String, DerivedMeters2.Entry> meterNameToEntry = new ConcurrentHashMap<>();
  
  @Descriptor("Lists the available monitorable components.")
  public void list(@Descriptor("A monitorable name to inspect") 
                   @Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
                   String monitorable,
		   @Descriptor("A monitorable pattern to filter with") 
                   @Parameter(names = { "-monitorables", "-ms" }, absentValue = "") 
                   String monPattern,
		   @Descriptor("A meter to inspect") 
                   @Parameter(names = { "-meter", "-mt" }, absentValue = "") 
                   String meter,
		   @Descriptor("A meter pattern to filter with") 
                   @Parameter(names = { "-meters", "-mts" }, absentValue = "") 
                   String meterPattern,
		   @Descriptor("Specifies if blank meters should be returned") 
                   @Parameter(names = { "-nzo" }, presentValue="true", absentValue = "false") 
                   boolean nonZeroOnly,
		   @Descriptor("Specifies that the compact format shall be used") 
                   @Parameter(names = { "-compact", "-c" }, presentValue="true", absentValue = "false") 
                   boolean compact,
		   @Descriptor("Indicates if pretty print should be used") 
                   @Parameter(names = { "-pretty", "-p" }, presentValue="true", absentValue = "false") 
                   boolean pretty
		   ) {
    for (Monitorable m : _meteringRegistry.getMonitorables (Meters.toPattern (monitorable, monPattern)))
	displayMonitorable (m, Meters.getMeters (m, Meters.toPattern (meter, meterPattern), true), nonZeroOnly, compact, pretty);
  }

  @Descriptor("Updates meter values.")
  public void update(@Descriptor("A monitorable name to target") 
		     @Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
		     String monitorable,
		     @Descriptor("A monitorable pattern to target") 
		     @Parameter(names = { "-monitorables", "-ms" }, absentValue = "") 
		     String monPattern,
		     @Descriptor("A meter name to target") 
		     @Parameter(names = { "-meter", "-mt" }, absentValue = "") 
		     String meter,
		     @Descriptor("A meter pattern to target") 
		     @Parameter(names = { "-meters", "-mts" }, absentValue = "") 
		     String meterPattern,
		     @Descriptor("The Value to set") 
		     @Parameter(names = { "-value", "-v" }, absentValue = "") 
		     long value
		     ) {
    for (Monitorable m : _meteringRegistry.getMonitorables (Meters.toPattern (monitorable, monPattern)))
      updateMonitorable(m, Meters.getMeters(m, Meters.toPattern (meter, meterPattern), true), value);
  }

    private void displayMonitorable(Monitorable monitorable,  List<Meter> meters, boolean nonZeroOnly, boolean compact, boolean pretty) {
      if (!compact){
	if (pretty) System.out.print (BOLD);
	System.out.println("* Available Meters for " + monitorable.getName () + " ["+monitorable.getDescription ()+"] :");
	if (pretty) System.out.print (BOLD_OFF);
      }
    for (Meter meter : meters) {
      long value = meter.getValue();
      boolean zero = value == 0L;
      if (nonZeroOnly && zero)
        continue;
      if (compact)
        System.out.printf("%s %s %s\n", monitorable.getName (), meter.getName(), value);
      else{
	boolean negative = value < 0L;
	boolean veryLarge = value >= 0xFFFFFFFFL; // this can be a -1 in long --> suspect
	if (pretty && zero) System.out.print (ITALIC);
	if (pretty && negative) System.out.print (BACKGROUND_BRIGHT_RED);
	if (pretty && veryLarge) System.out.print (BACKGROUND_BRIGHT_YELLOW);
	System.out.printf("\t%-80s: %10s", meter.getName(), value);
	if (pretty) System.out.print (RESET);
	System.out.printf("%n");
      }
    }
    if (pretty) System.out.print (RESET+"\n");
  }

  private void updateMonitorable(Monitorable monitorable,  List<Meter> meters, long value) {
    for (Meter meter : meters) {
      switch (meter.getType()) {
      case ABSOLUTE:
        meter.set(value);
        break;
      case INCREMENTAL:
        if (value == 0)
          meter.getAndReset();
        else
          meter.inc(value);
        break;
      case SUPPLIED:
        // can't update a Meter with this type
        break;
      }
    }
    displayMonitorable(monitorable, meters, false, true, false);
  }

  @Descriptor("Creates a new Rate Meter for a given Meter.")
  public void createRateMeter(@Descriptor("The target monitorable") 
			      @Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
			      String monitorable,
			      @Descriptor("The target meter") 
			      @Parameter(names = { "-meter", "-mt" }, absentValue = "") 
			      String meter,
			      @Descriptor("The time interval to use for the calculation") 
			      @Parameter(names = { "-period", "-p" }, absentValue = "1000")
			      long value
			      ) {
    Monitorable mon = _meteringRegistry.getMonitorable (monitorable);
    Meter mt = mon != null ?  mon.getMeters ().get (meter) : null;
    if (mt == null){
      System.out.println ("Cannot find meter : "+monitorable+"/"+meter);
      return;
    }
    mt = Meters.createRateMeter (_meteringService, mt, value);
    mon.getMeters().put(mt.getName(), mt);
    mon.updated();
    System.out.println (mt+" created");
  }
  @Descriptor("Removes a Rate Meter.")
  public void removeRateMeter(@Descriptor("The target monitorable") 
			      @Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
			      String monitorable,
			      @Descriptor("The rate meter") 
			      @Parameter(names = { "-meter", "-mt" }, absentValue = "") 
			      String meter
			      ) {
    Monitorable mon = _meteringRegistry.getMonitorable (monitorable);
    Meter mt = mon != null ?  mon.getMeters ().get (meter) : null;
    if (mt == null){
      System.out.println ("Cannot find meter : "+monitorable+"/"+meter);
      return;
    }
    if (Meters.stopRateMeter (mt)){
	mon.getMeters().remove(mt.getName());
	mon.updated();
	System.out.println (mt+" removed");
    } else
	System.out.println (mt+" : Stop failed");
  }
  @Descriptor("Creates a new Max Value Meter for a given Meter.")
  public void createMaxValueMeter(@Descriptor("The target monitorable") 
				  @Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
				  String monitorable,
				  @Descriptor("The target meter") 
				  @Parameter(names = { "-meter", "-mt" }, absentValue = "") 
				  String meter,
				  @Descriptor("Indicates if the max value meter should be scheduled") 
				  @Parameter(names = { "-scheduled"}, absentValue = "0") 
				  long scheduled
				  ) {
    Monitorable mon = _meteringRegistry.getMonitorable (monitorable);
    Meter mt = mon != null ?  mon.getMeters ().get (meter) : null;
    if (mt == null){
      System.out.println ("Cannot find meter : "+monitorable+"/"+meter);
      return;
    }
    if (scheduled > 0)
      mt = Meters.createScheduledMaxValueMeter (_meteringService, mt, scheduled, 0);
    else
      mt = Meters.createMaxValueMeter (_meteringService, mt);
    mon.getMeters().put(mt.getName (), mt);
    mon.updated();
    System.out.println (mt+" created");
  }
  @Descriptor("Removes a Max Value Meter.")
  public void removeMaxValueMeter(@Descriptor("The target monitorable") 
				  @Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
				  String monitorable,
				  @Descriptor("The max value meter") 
				  @Parameter(names = { "-meter", "-mt" }, absentValue = "") 
				  String meter
				  ) {
    Monitorable mon = _meteringRegistry.getMonitorable (monitorable);
    Meter mt = mon != null ?  mon.getMeters ().get (meter) : null;
    if (mt == null){
      System.out.println ("Cannot find meter : "+monitorable+"/"+meter);
      return;
    }
    if (Meters.stopMaxValueMeter (mt)){
	mon.getMeters().remove (mt.getName());
	mon.updated();
	System.out.println (mt+" removed");
    } else
	System.out.println (mt+" : Stop failed");
  }
  @Descriptor("Creates a new Max Value Meter for a given Meter.")
  public void createMovingMaxValueMeter(@Descriptor("The target monitorable") 
					@Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
					String monitorable,
					@Descriptor("The max value meter") 
					@Parameter(names = { "-meter", "-mt" }, absentValue = "") 
					String meter,
					@Descriptor("The name of the new moving max value meter") 
					@Parameter(names = { "-name"}, absentValue = "") 
					String name,
					@Descriptor("Indicates the sampling length") 
					@Parameter(names = { "-sampling"}, absentValue = "1000") 
					long sampling,
					@Descriptor("Indicates the number of samples") 
					@Parameter(names = { "-samples"}, absentValue = "5") 
					int samples
					) {
    Monitorable mon = _meteringRegistry.getMonitorable (monitorable);
    Meter mt = mon != null ?  mon.getMeters ().get (meter) : null;
    if (mt == null){
      System.out.println ("Cannot find meter : "+monitorable+"/"+meter);
      return;
    }
    if (name.length () == 0){
      System.out.println ("Missing -name parameter");
      return;
    }
    mt = Meters.createMovingMaxValueMeter (_meteringService, name, mt, sampling, samples);
    mon.getMeters().put(mt.getName(), mt);
    mon.updated();
    System.out.println (mt+" created");
  }

  @Descriptor("Creates a Meter merged from given meters.")
  public void createMergeMeters(
		  			@Descriptor("The target monitorable") 
					@Parameter(names = { "-monitorable", "-m" }, absentValue = "") 
					String monitorable,
					@Descriptor("The target monitorables (pattern)") 
					@Parameter(names = { "-monitorables", "-ms" }, absentValue = "") 
					String monitorables,
					@Descriptor("The name of the new merged meter") 
					@Parameter(names = { "-to", "-toMeter", "-name"}, absentValue = "") 
					String name,
					@Descriptor("The meter pattern (* to select all meters)") 
					@Parameter(names = { "-meters", "-mts" }, absentValue = "") 
					String meters,
					@Descriptor("The list of meters to merge (-mt meter1 meter2 meterN) /!\\ flag to use at the end") 
					@Parameter(names = { "-meter", "-mt" }, absentValue = "") 
					String... meterList
					) {
	  List<String> meterNames = (!meters.isEmpty()) ? Collections.emptyList() :Arrays.asList(meterList);
	  
	  
	  MergedMeters mergeMeter = null;
	  try {
		  mergeMeter =  _derivedMeters2.new MergedMeters(getParam(monitorable), getParam(monitorables), 
						  						 getParam(name), meterNames, getParam(meters));
		  System.out.println(mergeMeter);
	  } catch (Throwable t){
		  mergeMeter = null;
		  System.out.println("Invalid MergedMeters declaration" + t);
	  }
	  if (mergeMeter != null){
		  meterNameToEntry.putIfAbsent(name, mergeMeter);
		  mergeMeter.setOp(DerivedMeters2._addOp);
		  mergeMeter.start();
		  System.out.println("Meter "+ name+" started");
	  } else {
		  System.out.println("Invalid MergedMeters declaration");
	  }
  }
  
  private String getParam(String param){
	return (param.isEmpty())?null:param;  
  }
  
}
