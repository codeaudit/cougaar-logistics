package org.cougaar.logistics.plugin.seanet;

import java.util.Iterator;

/**
 * A <code>SequenceIterator</code> iterates over the elements of two
 * iterators in sequence.
 *
 * @see Iterator */
public class SequenceIterator implements Iterator {
  Iterator a;
  Iterator b;

  public SequenceIterator(Iterator a, Iterator b) {
    this.a = a;
    this.b = b;
  }
  public boolean hasNext() {
    if (a.hasNext()) return true;
    else if (b != null) {
      a = b;
      b = null;
      return this.hasNext();
    }
    else return false;
  }
  
  public Object next() {
    if (this.hasNext()) return a.next();
    else throw new java.util.NoSuchElementException();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
