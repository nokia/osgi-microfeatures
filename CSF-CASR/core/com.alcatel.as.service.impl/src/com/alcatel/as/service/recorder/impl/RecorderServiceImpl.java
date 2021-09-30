package com.alcatel.as.service.recorder.impl;

import com.alcatel.as.service.recorder.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.apache.felix.dm.annotation.api.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import java.time.*;
import java.time.format.*;
import org.apache.felix.service.command.*;

import static com.alcatel.as.util.helper.AnsiFormatter.*;

@Config(section = "RecorderService configuration")
@Component(provides = RecorderService.class, properties = {
	   @Property(name = CommandProcessor.COMMAND_SCOPE, value = "casr.service.recorder"),
	   @Property(name = CommandProcessor.COMMAND_FUNCTION, value = {"print", "add"})
    })
public class RecorderServiceImpl implements RecorderService {

    @StringProperty(title = "Date Formatter Pattern",
		    help = "The pattern to use when printing Dates.",
		    required = true, dynamic = false, defval = "uuuu-MM-dd HH:mm:ss.SSS")
		    public static final String PROP_DATE_FORMATTER_PATTERN = "date.formatter.pattern";
    @IntProperty(title = "Record Initial Capacity",
		    help = "The initial capacity of the record.",
		    required = true, dynamic = false, defval = 128)
		    public static final String PROP_RECORD_CAPACITY_INIT = RECORD_CAPACITY_INIT;
    @IntProperty(title = "Record Maximum Capacity",
		    help = "The maximum capacity of the record.",
		    required = true, dynamic = false, defval = 1024)
		    public static final String PROP_RECORD_CAPACITY_MAX = RECORD_CAPACITY_MAX;
    @IntProperty(title = "Record Head Preservation",
		    help = "The number of head lines to preserve when compacting.",
		    required = true, dynamic = false, defval = 128)
		    public static final String PROP_RECORD_PRESERVE_HEAD = RECORD_PRESERVE_HEAD;
    @IntProperty(title = "Record Tail Preservation",
		    help = "The number of tail lines to preserve when compacting.",
		    required = true, dynamic = false, defval = 512)
		    public static final String PROP_RECORD_PRESERVE_TAIL = RECORD_PRESERVE_TAIL;

    private static final Record[] ALL = new Record[0];

    private ConcurrentHashMap<String, Record> _records = new ConcurrentHashMap<> ();

    protected DateTimeFormatter _dateFormat;
    protected Dictionary<?, ?> _conf;

    protected Comparator<Entry> _comp = new Comparator<Entry> (){
	    public int compare (Entry e1, Entry e2){
		return e1.event ().time ().compareTo (e2.event ().time ());
	    }
	};
    
    public RecorderServiceImpl (){}
    
    @ConfigurationDependency
    public void updated(Dictionary<?, ?> conf) { 
	if (_conf == null) {
	    _conf = conf;
	    String df = conf.get (PROP_DATE_FORMATTER_PATTERN).toString ();
	    _dateFormat = new DateTimeFormatterBuilder ().appendPattern (df).toFormatter ();
	}
    }

    public Record getRecord (String name){
	return _records.get (name);
    }

    public Record newRecord (String name, Map<String, Object> properties, boolean mustCreate){
	Record newrecord = new RecordImpl (this, name, properties);
	Record oldrecord = _records.putIfAbsent (name, newrecord);
	if (oldrecord == null) return newrecord;
	if (mustCreate) return null;
	return oldrecord;
    }

    public void iterate (java.util.function.Consumer<Record> f){
	_records.forEach ((name, rec) -> {
		f.accept (rec);
	    });
    }

    public void iterate (java.util.function.BiConsumer<Record, Event> f, Record... records){
	if (records == null) records = ALL; // avoid NPE
	if (records.length == 1){
	    Record rec = records[0];
	    rec.iterate ((pos, event) -> {f.accept (rec, event);});
	    return;
	}
	List<Entry> entries = new ArrayList<> ();
	if (records.length == 0){ // all records
	    iterate ( rec -> {rec.iterate ((pos, event) -> {entries.add (new Entry (rec, event));});});
	} else {
	    for (Record rec : records){
		rec.iterate ((pos, event) -> {entries.add (new Entry (rec, event));});
	    }
	}
	entries.sort (_comp);
	Iterator it = entries.iterator ();
	while (it.hasNext ()){
	    Entry entry = (Entry) it.next ();
	    f.accept (entry.record (), entry.event ());
	}
    }

    public DateTimeFormatter getDateFormatter (){ return _dateFormat;}

    protected void destroy (Record record){
	_records.remove (record.name ());
    }

    private static class Entry {
	Record _record;
	Event _event;
	private Entry (Record r, Event e){ _record = r; _event = e;}
	private Event event (){ return _event;}
	private Record record (){ return _record;}
    }

    @Descriptor("Prints the records")
    public void print(@Descriptor("The Record name to print (optional / all records if not specified)") 
		      @Parameter(names = { "-record", "-r" }, absentValue = "") 
		      String name,
		      @Descriptor("The logger to use (optional)") 
		      @Parameter(names = { "-logger", "-l" }, absentValue = "") 
		      String logger
		      ) {
	Record[] records = getRecords (name);
	if (records == null) return;
	LocalDateTime[] now = new LocalDateTime[1];
	int[] index = new int[1];
	StringBuilder dest = new StringBuilder ();
	iterate ((record, event) -> {
		int pos = index[0]++;
		long millis = 0L;
		boolean ms = true;
		if (pos == 0) now[0] = event.time ();
		else {
		    millis = now[0].until(event.time (), java.time.temporal.ChronoUnit.MILLIS);
		    if (millis >= 10000){
			millis = now[0].until(event.time (), java.time.temporal.ChronoUnit.SECONDS);
			ms = false;
		    }
		    now[0] = event.time ();
		}
		dest.append ('#').append (pos).append (": ")
		    .append (record.name ()).append (":\t")
		    .append (_dateFormat.format (event.time ()));
		if (pos > 0)
		    dest.append (" (+").append (millis).append (ms ? "ms):\t\t" : "sec):\t\t");
		else
		    dest.append (":\t\t");
		dest.append (event.message ()).append ('\n');
	    }, records);
	String s = dest.toString ();
	System.out.println (s);
	if (logger.length () > 0) Logger.getLogger (logger).warn ("Record dump:\n"+s);
    }
    @Descriptor("Records an entry")
    public void add(@Descriptor("The Record name to add the entry to (optional / all records if not specified)") 
		    @Parameter(names = { "-record", "-r" }, absentValue = "") 
		    String name,
		    @Descriptor("The event message to add") 
		    @Parameter(names = { "-m" }, absentValue = "") 
		    String msg
		    ) {
	if (msg == null || msg.length () == 0){
	    System.out.println ("No message specified");
	    return;
	}
	Record[] records = getRecords (name);
	if (records == null) return;
	Event event = new Event (msg);
	if (records == ALL){
	    iterate ((record) -> {
		    record.record (event);
		});
	} else {
	    records[0].record (event);
	}
	System.out.println ("Entry added");
    }

    private Record[] getRecords (String name){
	if (name != null && name.length () > 0){
	    Record rec = getRecord (name);
	    if (rec == null){
		System.out.println ("Record "+name+" : Not Found");
		return null;
	    }
	    return new Record[]{rec};
	}
	return ALL;
    }

    public int intProperty (String record, String prop, Map<String, Object> props){
	Object o = props.get (prop);
	boolean def = (o == null);
	if (def){
	    o = _conf.get (record+"."+prop);
	    if (o == null) o = _conf.get (prop);
	}
	int i = toInteger (o);
	if (def) props.put (prop, i);
	return i;
    }
    private static int toInteger (Object o){
	if (o instanceof Number) return ((Number)o).intValue ();
	if (o instanceof String) return Integer.parseInt (o.toString ());
	throw new RuntimeException ("Cannot convert to integer : "+o);
    }

}
