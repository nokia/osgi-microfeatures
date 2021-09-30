package alcatel.tess.hometop.gateways.utils;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * High performance date/time formatter.
 */
public class DateFormatter {
  
  /** Formatter like "1999/11/27 15:49:37 459" */
  public final static int SIMPLE_FORMAT = 0;
  public final static int SIMPLE_FORMAT_LEN_WITHOUT_MS = 20;
  
  /** Formatter like "[16/Mar/2004:14:49:24 +0100]" */
  public final static int NCSA_FORMAT = 1;
  public final static int NCSA_FORMAT_LEN_WITHOUT_MS = 28;
  
  /** Formatter like "Tue, 16 Mar 2004 15:16:59 GMT" */
  public final static int GMT_FORMAT = 2;
  public final static int GMT_FORMAT_LEN_WITHOUT_MS = 29;
  
  public DateFormatter(int format) {
    this(format, TimeZone.getDefault());
  }
  
  public DateFormatter(int format, TimeZone timeZone) {
    this.calendar = Calendar.getInstance(timeZone);
    this.format = format;
    
    switch (format) {
    case SIMPLE_FORMAT:
      lastTimeString = new char[SIMPLE_FORMAT_LEN_WITHOUT_MS];
      break;
    
    case NCSA_FORMAT:
      lastTimeString = new char[NCSA_FORMAT_LEN_WITHOUT_MS];
      break;
    
    case GMT_FORMAT:
      lastTimeString = new char[GMT_FORMAT_LEN_WITHOUT_MS];
      break;
    
    default:
      throw new IllegalArgumentException("format must match either " + "DateFormatter.SIMPLE_FORMAT, "
          + "DateFormatter.NCSA_FORMAT, " + "or DateFormatter.RFC_FORMAT");
    }
  }
  
  /**
   * Returns a date in the format "YYYY-mm-dd HH:mm:ss SSS" to sbuf. 
   * Example: "2004-01-27 19:01:02 123".
   *
   * @param timeStamp the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * @param sbuf the <code>StringBuffer</code> to write to
   * @return a string in the format "YYYY-mm-dd HH:mm:ss SSS"
   */
  public String format(long timeStamp) {
    sb.setLength(0);
    return (format(timeStamp, sb).toString());
  }
  
  public StringBuffer format(long timeStamp, StringBuffer out) {
    switch (format) {
    case SIMPLE_FORMAT:
      return (formatSimple(timeStamp, out));
      
    case NCSA_FORMAT:
      return (formatNCSA(timeStamp, out));
      
    case GMT_FORMAT:
      return (formatGMT(timeStamp, out));
      
    default:
      return (formatSimple(timeStamp, out));
    }
  }
  
  /**
   * Appends a date in the format "YYYY/mm/dd HH:mm:ss SSS" to sbuf. 
   * Example: "1999/11/27 15:49:37 459".
   *
   * @param timeStamp the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * @param out the StringBuffer to write to
   * @return the StringBuffer to write to
   */
  protected StringBuffer formatSimple(long timeStamp, StringBuffer out) {
    long now = timeStamp;
    int ms = (int) (now % 1000);
    
    if ((now - ms) != lastTime) {
      calendar.setTimeInMillis(timeStamp);
      
      int start = out.length();
      
      int year = calendar.get(Calendar.YEAR);
      out.append(year);
      
      out.append(getMonth2());
      
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      if (day < 10)
        out.append('0');
      out.append(day);
      
      out.append(' ');
      
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      if (hour < 10) {
        out.append('0');
      }
      out.append(hour);
      out.append(':');
      
      int mins = calendar.get(Calendar.MINUTE);
      if (mins < 10) {
        out.append('0');
      }
      out.append(mins);
      out.append(':');
      
      int secs = calendar.get(Calendar.SECOND);
      if (secs < 10) {
        out.append('0');
      }
      out.append(secs);
      out.append(' ');
      
      out.getChars(start, out.length(), lastTimeString, 0);
      
      lastTime = now - ms;
    } else {
      out.append(lastTimeString, 0, SIMPLE_FORMAT_LEN_WITHOUT_MS);
    }
    
    if (ms < 100)
      out.append('0');
    if (ms < 10)
      out.append('0');
    
    out.append(ms);
    return out;
  }
  
  /**
   * Appends a date in the format NCSA to sbuf. 
   * Example: [16/Mar/2004:14:49:24 +0100]
   *
   * @param timeStamp the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * @param out the StringBuffer to write to
   * @return the StringBuffer to write to
   */
  protected StringBuffer formatNCSA(long timeStamp, StringBuffer out) {
    long now = timeStamp;
    int ms = (int) (now % 1000);
    
    if ((now - ms) != lastTime) {
      calendar.setTimeInMillis(timeStamp);
      
      int start = out.length();
      
      out.append('[');
      
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      if (day < 10)
        out.append('0');
      out.append(day);
      
      out.append('/');
      out.append(getMonth3());
      out.append('/');
      
      int year = calendar.get(Calendar.YEAR);
      out.append(year);
      
      out.append(' ');
      
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      if (hour < 10) {
        out.append('0');
      }
      out.append(hour);
      out.append(':');
      
      int mins = calendar.get(Calendar.MINUTE);
      if (mins < 10) {
        out.append('0');
      }
      out.append(mins);
      out.append(':');
      
      int secs = calendar.get(Calendar.SECOND);
      if (secs < 10) {
        out.append('0');
      }
      out.append(secs);
      out.append(' ');
      
      out.append(GMTOffset);
      out.append(']');
      
      out.getChars(start, out.length(), lastTimeString, 0);
      
      lastTime = now - ms;
    } else {
      out.append(lastTimeString, 0, NCSA_FORMAT_LEN_WITHOUT_MS);
    }
    
    return out;
  }
  
  /**
   * Appends a date in the format NCSA to sbuf. 
   * Example: Tue, 16 Mar 2004 15:16:59 GMT
   *
   * @param timeStamp the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * @param out the StringBuffer to write to
   * @return the StringBuffer to write to
   */
  protected StringBuffer formatGMT(long timeStamp, StringBuffer out) {
    long now = timeStamp;
    int ms = (int) (now % 1000);
    
    if ((now - ms) != lastTime) {
      calendar.setTimeInMillis(timeStamp);
      
      int start = out.length();
      
      int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
      out.append(days[dayOfWeek]);
      out.append(", ");
      
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      if (day < 10)
        out.append('0');
      out.append(day);
      out.append(' ');
      
      out.append(getMonth3());
      out.append(' ');
      
      int year = calendar.get(Calendar.YEAR);
      out.append(year);
      out.append(' ');
      
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      if (hour < 10) {
        out.append('0');
      }
      out.append(hour);
      out.append(':');
      
      int mins = calendar.get(Calendar.MINUTE);
      if (mins < 10) {
        out.append('0');
      }
      out.append(mins);
      out.append(':');
      
      int secs = calendar.get(Calendar.SECOND);
      if (secs < 10) {
        out.append('0');
      }
      out.append(secs);
      out.append(' ');
      out.append("GMT");
      
      out.getChars(start, out.length(), lastTimeString, 0);
      
      lastTime = now - ms;
    } else {
      out.append(lastTimeString);
    }
    
    return out;
  }
  
  private String getMonth2() {
    switch (calendar.get(Calendar.MONTH)) {
    case Calendar.JANUARY:
      return ("/01/");
      
    case Calendar.FEBRUARY:
      return ("/02/");
      
    case Calendar.MARCH:
      return ("/03/");
      
    case Calendar.APRIL:
      return ("/04/");
      
    case Calendar.MAY:
      return ("/05/");
      
    case Calendar.JUNE:
      return ("/06/");
      
    case Calendar.JULY:
      return ("/07/");
      
    case Calendar.AUGUST:
      return ("/08/");
      
    case Calendar.SEPTEMBER:
      return ("/09/");
      
    case Calendar.OCTOBER:
      return ("/10/");
      
    case Calendar.NOVEMBER:
      return ("/11/");
      
    case Calendar.DECEMBER:
      return ("/12/");
      
    default:
      return ("/NA");
    }
  }
  
  private String getMonth3() {
    switch (calendar.get(Calendar.MONTH)) {
    case Calendar.JANUARY:
      return ("Jan");
      
    case Calendar.FEBRUARY:
      return ("Feb");
      
    case Calendar.MARCH:
      return ("Mar");
      
    case Calendar.APRIL:
      return ("Apr");
      
    case Calendar.MAY:
      return ("May");
      
    case Calendar.JUNE:
      return ("Jun");
      
    case Calendar.JULY:
      return ("Jul");
      
    case Calendar.AUGUST:
      return ("Aug");
      
    case Calendar.SEPTEMBER:
      return ("Sep");
      
    case Calendar.OCTOBER:
      return ("Oct");
      
    case Calendar.NOVEMBER:
      return ("Nov");
      
    case Calendar.DECEMBER:
      return ("Dec");
      
    default:
      return ("NA");
    }
  }
  
  public static void main(String args[]) throws Exception {
    System.out.println("Testing simple formatter");
    test(new DateFormatter(DateFormatter.SIMPLE_FORMAT));
    
    System.out.println("\nTesting NCSA formatter");
    test(new DateFormatter(DateFormatter.NCSA_FORMAT));
    
    System.out.println("\nTesting GMT formatter");
    test(new DateFormatter(DateFormatter.GMT_FORMAT));
  }
  
  static void test(DateFormatter f) throws InterruptedException {
    System.out.println("Date=" + (new Date()).toString() + "\t" + f.format(System.currentTimeMillis()));
    Thread.sleep(100);
    System.out.println("Date=" + (new Date()).toString() + "\t" + f.format(System.currentTimeMillis()));
    Thread.sleep(1000);
    System.out.println("Date=" + (new Date()).toString() + "\t" + f.format(System.currentTimeMillis()));
  }
  
  /** Last time we cached a date in lastTimeString */
  private long lastTime;
  
  /** Cached dates */
  private char[] lastTimeString;
  
  /** Calendar used to format */
  private Calendar calendar;
  
  /** format used (see constants) */
  private int format;
  
  /** GMT offset complying to the apache combined log format */
  private static String GMTOffset;
  
  /** Cached StringBuffer used format date into string */
  private StringBuffer sb = new StringBuffer();
  
  /** days corresponding to Calendar.SUNDAY, Calendar.MONDAY, etc... */
  private final static String[] days = new String[] { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
  
  static {
    //
    // Initialize the GMT offset used to format access logs that
    // comply with the apache access log format. The following code
    // (func "log_request_time").
    //
    long tz = TimeZone.getDefault().getRawOffset() / 1000 / 60;
    char sign = (tz < 0 ? '-' : '+');
    
    if (tz < 0L) {
      tz = -tz;
    }
    
    DecimalFormat df = new DecimalFormat("00");
    GMTOffset = sign + df.format(tz / 60) + df.format(tz % 60);
  }
}
