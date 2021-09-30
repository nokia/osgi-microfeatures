package alcatel.tess.hometop.gateways.utils;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class Enumerations<E> implements Enumeration<E> {
  private Enumeration[] _list;
  private int _current = 0;
  
  public Enumerations(Enumeration<E> ... enumerations) {
    this._list = (Enumeration[]) enumerations;
  }
  
  public boolean hasMoreElements() {
    return hasMore();
  }
  
  public E nextElement() {
    if (!hasMore()) {
      throw new NoSuchElementException();
    }
    return ((E) _list[_current].nextElement());
  }
  
  private boolean hasMore() {
    while (_current < _list.length) {
      if (_list[_current] != null && _list[_current].hasMoreElements()) {
        return true;
      }
      _current++;
    }
    return false;
  }
}
