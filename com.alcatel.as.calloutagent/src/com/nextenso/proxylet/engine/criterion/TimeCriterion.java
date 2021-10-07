package com.nextenso.proxylet.engine.criterion;

// Callout
import java.util.Calendar;

public class TimeCriterion extends CalendarCriterion {
  private int time;
  
  private TimeCriterion(String value) throws CriterionException {
    if (value == null)
      throw new CriterionException(CriterionException.INVALID_TIME, value);
    try {
      value = value.trim();
      int index = value.indexOf(':');
      if (index == -1 || index == 0 || index == (value.length() - 1))
        throw new CriterionException(CriterionException.INVALID_TIME, value);
      int hour = Integer.parseInt(value.substring(0, index).trim());
      int min = Integer.parseInt(value.substring(index + 1).trim());
      if (hour < 0 || hour > 23 || min < 0 || min > 59)
        throw new CriterionException(CriterionException.INVALID_TIME, value);
      time = hour * 60 + min;
    } catch (NumberFormatException e) {
      throw new CriterionException(CriterionException.INVALID_TIME, value);
    }
  }
  
  public int getValue() {
    return time;
  }
  
  public int getValue(Calendar calendar) {
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
  }
  
  public String toString() {
    int h = time / 60;
    int m = time % 60;
    String mm = (m < 10) ? "0" + m : String.valueOf(m);
    return ("[" + Utils.TIME + "=" + h + ':' + mm + "]");
  }
  
  public static Criterion getInstance(String value) throws CriterionException {
    Criterion c = CalendarCriterion.getInstance(value);
    return (c != null) ? c : new TimeCriterion(value);
  }
  
  public boolean includes(Criterion c) {
    // (time==21) implies (time==21)
    if (c instanceof TimeCriterion)
      return (time == ((TimeCriterion) c).getValue());
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // (time==21) rejects (time=23)
    if (c instanceof TimeCriterion)
      return (time != ((TimeCriterion) c).getValue());
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof TimeCriterion)
      return (time == ((TimeCriterion) o).getValue());
    return false;
  }
  
  public int getDepth() {
    return 1;
  }
}
