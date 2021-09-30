package alcatel.tess.hometop.gateways.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import alcatel.tess.hometop.gateways.utils.Parameters;

public class TestParameters implements Observer {
  private static void debug(String s) {
    System.out.println(s);
  }
  
  public TestParameters(Parameters params) {
    debug("foo=" + params.get("foo"));
    
    debug("Enumeration ...");
    Enumeration e = params.getKeys("*");
    
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      debug(name + "=" + params.get(name));
    }
    params.addObserver(this);
    params.clearModificationHistory();
  }
  
  /** Observer interface */
  public void update(Observable observable, Object arg) {
    Parameters config = (Parameters) observable;
    String[] names = (String[]) arg;
    debug("config changed: " + config);
    
    for (int i = 0; i < names.length; i++) {
      if (names[i].equals("bar")) {
        debug(names[i] + "=" + config.valueOf(names[i], Parameters.Type.INT, "bad int value !"));
      } else {
        debug(names[i] + "=" + config.get(names[i], "NULL"));
      }
    }
  }
  
  public static void main(String args[]) throws Exception {
    Parameters p = new Parameters();
    debug("setting foo bar");
    p.set("foo", "default", true);
    p.set("bar", "123", true);
    TestParameters tc = new TestParameters(p);
    debug("Notifying ...");
    p.notifyObservers();
    debug("setting prop foo");
    p.set("foo", "non-default", false);
    debug("Notifying ...");
    p.notifyObservers();
    
    debug("removing non-default prop foo");
    p.remove("foo", false);
    debug("Notifying ...");
    p.notifyObservers();
    
    debug("removing default prop foo");
    p.remove("foo", true);
    debug("Notifying ...");
    p.notifyObservers();
    
    debug("setting invalid int value for bar");
    p.set("bar", "123a", false);
    debug("Notifying ...");
    p.notifyObservers();
    
    debug("Writing to /tmp/foo.properties");
    p.set("foo1", "bar1", false);
    p.set("foo2", "bar2", false);
    p.writeTo(new FileOutputStream("/tmp/foo.properties"));
    
    debug("Clearing ...");
    p.clear();
    
    debug("Loading /tmp/foo.properties ...");
    p.load(new File("/tmp/foo.properties"), false);
    p.notifyObservers();
    
    debug("Please modify /tmp/foo.properties and press a key ...");
    System.in.read();
    debug("Reloading /tmp/foo.properties ...");
    p.reload(new File("/tmp/foo.properties"), false);
    p.notifyObservers();
    
    debug("Loading args: -foo -bar bar ...");
    p.load(new String[] { "-foo", "-bar", "bar" }, false);
    p.notifyObservers();
    
    debug("toProperties ...");
    Properties prop = p.toProperties();
    debug(prop.toString());
    
    debug("fromProperties ...");
    p.load(prop, false);
    p.notifyObservers();
  }
}
