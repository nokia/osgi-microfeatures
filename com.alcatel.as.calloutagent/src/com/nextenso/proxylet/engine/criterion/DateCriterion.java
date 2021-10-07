package com.nextenso.proxylet.engine.criterion;

// Callout
import java.util.Calendar;

public class DateCriterion extends CalendarCriterion {
  
  private int date;
  
  private DateCriterion(String value) throws CriterionException {
    if (value == null)
      throw new CriterionException(CriterionException.INVALID_DATE, value);
    try {
      date = Integer.parseInt(value);
      if (date < 1 || date > 31)
        throw new CriterionException(CriterionException.INVALID_DATE, value);
    } catch (NumberFormatException e) {
      throw new CriterionException(CriterionException.INVALID_DATE, value);
    }
  }
  
  @Override
  public int getValue() {
    return date;
  }
  
  @Override
  public int getValue(Calendar calendar) {
    return calendar.get(Calendar.DAY_OF_MONTH);
  }
  
  @Override
  public String toString() {
    return ("[" + Utils.DATE + "=" + date + "]");
  }
  
  public static Criterion getInstance(String value) throws CriterionException {
    Criterion c = CalendarCriterion.getInstance(value);
    return (c != null) ? c : new DateCriterion(value);
  }
  
  @Override
  public boolean includes(Criterion c) {
    // (date==21) implies (date==21)
    if (c instanceof DateCriterion)
      return (date == ((DateCriterion) c).getValue());
    return super.includes(c);
  }
  
  @Override
  public boolean excludes(Criterion c) {
    // (date==21) rejects (date=23)
    if (c instanceof DateCriterion)
      return (date != ((DateCriterion) c).getValue());
    return super.excludes(c);
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof DateCriterion)
      return (date == ((DateCriterion) o).getValue());
    return false;
  }
  
  @Override
  public int getDepth() {
    return 1;
  }
}
