package com.alcatel.as.service.recorder;

public interface Record {

    public String name ();

    public java.util.Map<String, Object> properties ();

    public Record record (Event event);
    
    public Record dismissBefore (java.time.LocalDateTime time);

    public Record dismissBefore (java.time.Duration duration);
    
    public Record destroy ();
    
    public void iterate (java.util.function.BiConsumer<Integer, Event> f);

    public RecorderService service ();
    
}
