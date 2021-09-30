package com.nextenso.proxylet.engine.criterion;

// Callout

public abstract class RegexpCriterion extends Criterion {
  
  // make sure MIDDLE=START+END
  protected static final int NONE = 0, START = 1, END = 2, MIDDLE = START + END;
  protected int type = NONE;
  protected String regexp;
  
  protected RegexpCriterion() {
    type = NONE;
    regexp = null;
  }
  
  protected RegexpCriterion(String regexp) throws CriterionException {
    if (regexp == null)
      throw new CriterionException(CriterionException.INVALID_REGEXP, regexp);
    if (regexp.length() == 0)
      throw new CriterionException(CriterionException.INVALID_REGEXP, regexp);
    if (regexp.charAt(0) == '*')
      type += START;
    if (regexp.charAt(regexp.length() - 1) == '*')
      type += END;
    String sub = null;
    switch (type) {
    case NONE:
      sub = regexp;
      break;
    case START:
      sub = regexp.substring(1);
      break;
    case END:
      sub = regexp.substring(0, regexp.length() - 1);
      break;
    case MIDDLE:
      sub = regexp.substring(1, regexp.length() - 1);
      break;
    }
    if (sub.indexOf('*') != -1)
      throw new CriterionException(CriterionException.INVALID_REGEXP, regexp);
    this.regexp = sub;
  }
  
  protected String getRegexp() {
    switch (type) {
    case START:
      return "*" + regexp;
    case END:
      return regexp + "*";
    case MIDDLE:
      return "*" + regexp + "*";
      // NONE
    default:
      return (regexp != null) ? regexp : "*";
    }
  }
  
  protected int getType() {
    return type;
  }
  
  protected boolean match(String exp) {
    if (exp == null)
      return false;
    switch (type) {
    case START:
      return exp.endsWith(regexp);
    case END:
      return exp.startsWith(regexp);
    case MIDDLE:
      return (exp.indexOf(regexp) != -1);
      // NONE
    default:
      return (regexp != null) ? exp.equals(regexp) : true;
    }
  }
  
  public static Criterion getInstance(String regexp) throws CriterionException {
    if ("*".equals(regexp) || "**".equals(regexp))
      return TrueCriterion.getInstance();
    return null;
  }
  
  public boolean includes(Criterion c) {
    // (*xxx.com) implies (*com)
    if (getClass().getName().equals(c.getClass().getName())) {
      RegexpCriterion rc = (RegexpCriterion) c;
      return (rc.match(getRegexp()));
    }
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // (*com) excludes (*net)
    // (com*) excludes (net*)
    // (com) excludes (net)
    if (getClass().getName().equals(c.getClass().getName())) {
      RegexpCriterion rc = (RegexpCriterion) c;
      int rc_type = rc.getType();
      if (type == rc_type) {
        if (type != MIDDLE)
          return (!(this.includes(c) || c.includes(this)));
        else
          return false;
      }
      return false;
    }
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof RegexpCriterion)
      return (getRegexp().equals(((RegexpCriterion) o).getRegexp()));
    return false;
  }
  
  public int getDepth() {
    return 1;
  }
}
