package com.nextenso.proxylet.engine.criterion;

// Callout
import java.util.Calendar;

public class MonthCriterion extends CalendarCriterion {
  private int month;
  private String month_str;
  
  private MonthCriterion(String value) throws CriterionException {
    if (value == null)
      throw new CriterionException(CriterionException.INVALID_MONTH, value);
    value = value.trim().toLowerCase();
    if (value.length() < 3)
      throw new CriterionException(CriterionException.INVALID_MONTH, value);
    month_str = value.substring(0, 3);
    if (month_str.startsWith("jan"))
      month = Calendar.JANUARY;
    else if (month_str.equals("feb"))
      month = Calendar.FEBRUARY;
    else if (month_str.equals("mar"))
      month = Calendar.MARCH;
    else if (month_str.equals("apr"))
      month = Calendar.APRIL;
    else if (month_str.equals("may"))
      month = Calendar.MAY;
    else if (month_str.equals("jun"))
      month = Calendar.JUNE;
    else if (month_str.equals("jul"))
      month = Calendar.JULY;
    else if (month_str.equals("aug"))
      month = Calendar.AUGUST;
    else if (month_str.equals("sep"))
      month = Calendar.SEPTEMBER;
    else if (month_str.equals("oct"))
      month = Calendar.OCTOBER;
    else if (month_str.equals("nov"))
      month = Calendar.NOVEMBER;
    else if (month_str.equals("dec"))
      month = Calendar.DECEMBER;
    else
      throw new CriterionException(CriterionException.INVALID_MONTH, value);
  }
  
  public int getValue() {
    return month;
  }
  
  public int getValue(Calendar calendar) {
    return calendar.get(Calendar.MONTH);
  }
  
  public String toString() {
    return ("[" + Utils.MONTH + "=" + month_str + "]");
  }
  
  public static Criterion getInstance(String value) throws CriterionException {
    Criterion c = CalendarCriterion.getInstance(value);
    return (c != null) ? c : new MonthCriterion(value);
  }
  
  public boolean includes(Criterion c) {
    // (month==jan) implies (month==jan)
    if (c instanceof MonthCriterion)
      return (month == ((MonthCriterion) c).getValue());
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // (month==jan) rejects (month=feb)
    if (c instanceof MonthCriterion)
      return (month != ((MonthCriterion) c).getValue());
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof MonthCriterion)
      return (month == ((MonthCriterion) o).getValue());
    return false;
  }
  
  public int getDepth() {
    return 1;
  }
}
