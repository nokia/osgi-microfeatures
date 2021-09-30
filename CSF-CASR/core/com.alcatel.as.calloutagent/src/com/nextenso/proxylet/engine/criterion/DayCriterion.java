package com.nextenso.proxylet.engine.criterion;

// Callout
import java.util.Calendar;

public class DayCriterion extends CalendarCriterion {
  private static final int MY_SUNDAY = Calendar.SATURDAY + 1;
  private int day;
  
  private DayCriterion(String value) throws CriterionException {
    if (value == null)
      throw new CriterionException(CriterionException.INVALID_DAY, value);
    value = value.trim().toLowerCase();
    if (value.length() < 3)
      throw new CriterionException(CriterionException.INVALID_DAY, value);
    if (value.startsWith("mon"))
      day = Calendar.MONDAY;
    else if (value.startsWith("tue"))
      day = Calendar.TUESDAY;
    else if (value.startsWith("wed"))
      day = Calendar.WEDNESDAY;
    else if (value.startsWith("thu"))
      day = Calendar.THURSDAY;
    else if (value.startsWith("fri"))
      day = Calendar.FRIDAY;
    else if (value.startsWith("sat"))
      day = Calendar.SATURDAY;
    else if (value.startsWith("sun"))
      day = MY_SUNDAY;
    else
      throw new CriterionException(CriterionException.INVALID_DAY, value);
  }
  
  public int getValue() {
    return day;
  }
  
  public int getValue(Calendar calendar) {
    // in Java the first day is sunday - we move the beginning to monday
    int d = calendar.get(Calendar.DAY_OF_WEEK);
    return (d == Calendar.SUNDAY) ? MY_SUNDAY : d;
  }
  
  public String toString() {
    String s = "[" + Utils.DAY;
    switch (day) {
    case Calendar.MONDAY:
      return (s + "=mon]");
    case Calendar.TUESDAY:
      return (s + "=tue]");
    case Calendar.WEDNESDAY:
      return (s + "=wed]");
    case Calendar.THURSDAY:
      return (s + "=thu]");
    case Calendar.FRIDAY:
      return (s + "=fri]");
    case Calendar.SATURDAY:
      return (s + "=sat]");
    default:
      return (s + "=sun]");
    }
  }
  
  public static Criterion getInstance(String value) throws CriterionException {
    Criterion c = CalendarCriterion.getInstance(value);
    return (c != null) ? c : new DayCriterion(value);
  }
  
  public boolean includes(Criterion c) {
    // (day==wed) implies (day==wed)
    if (c instanceof DayCriterion)
      return (day == ((DayCriterion) c).getValue());
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // (day==wed) rejects (day=mon)
    if (c instanceof DayCriterion)
      return (day != ((DayCriterion) c).getValue());
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof DayCriterion)
      return (day == ((DayCriterion) o).getValue());
    return false;
  }
  
  public int getDepth() {
    return 1;
  }
}
