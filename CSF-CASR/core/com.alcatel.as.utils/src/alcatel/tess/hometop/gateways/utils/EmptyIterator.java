package alcatel.tess.hometop.gateways.utils;

// JDK imports
import java.util.Iterator;
import java.util.NoSuchElementException;

public class EmptyIterator implements Iterator {
  /* **********
  * Constants *
  *************/
  public final static EmptyIterator INSTANCE = new EmptyIterator();
  
  /* *********************
   * Implements Iterator *
   ***********************/
  public boolean hasNext() {
    return false;
  }
  
  public Object next() throws NoSuchElementException {
    throw new NoSuchElementException();
  }
  
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
