package org.cougaar.logistics.plugin.seanet;

import java.util.Iterator;

public class SingletonIterator implements Iterator {
  boolean hasNext = true;
  Object datum;

  public SingletonIterator(Object datum) { this.datum = datum; }

  public boolean hasNext() { return hasNext; }

  public Object next() {
    hasNext = false;
    Object result = datum;
    datum = null;
    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
