/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

// Portions of this class were copied from the Sun JVM implementation for the java.util.Collections class
// to provide nessacary methods to the InstanceHashtable class

package org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm;

import java.util.*;
import java.io.Serializable;

public class Collections
{
    static Collection synchronizedCollection(Collection c, Object mutex) {
	return new SynchronizedCollection(c, mutex);
  }
    static Set synchronizedSet(Set s, Object mutex) {
	return new SynchronizedSet(s, mutex);
    }

    static class SynchronizedCollection implements Collection, Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 3053995032091335093L;

	Collection c;	   // Backing Collection
	Object	   mutex;  // Object on which to synchronize

	SynchronizedCollection(Collection c) {
            if (c==null)
                throw new NullPointerException();
	    this.c = c;
            mutex = this;
        }
	SynchronizedCollection(Collection c, Object mutex) {
	    this.c = c;
            this.mutex = mutex;
        }

	public int size() {
	    synchronized(mutex) {return c.size();}
        }
	public boolean isEmpty() {
	    synchronized(mutex) {return c.isEmpty();}
        }
	public boolean contains(Object o) {
	    synchronized(mutex) {return c.contains(o);}
        }
	public Object[] toArray() {
	    synchronized(mutex) {return c.toArray();}
        }
	public Object[] toArray(Object[] a) {
	    synchronized(mutex) {return c.toArray(a);}
        }

	public Iterator iterator() {
            return c.iterator(); // Must be manually synched by user!
        }

	public boolean add(Object o) {
	    synchronized(mutex) {return c.add(o);}
        }
	public boolean remove(Object o) {
	    synchronized(mutex) {return c.remove(o);}
        }

	public boolean containsAll(Collection coll) {
	    synchronized(mutex) {return c.containsAll(coll);}
        }
	public boolean addAll(Collection coll) {
	    synchronized(mutex) {return c.addAll(coll);}
        }
	public boolean removeAll(Collection coll) {
	    synchronized(mutex) {return c.removeAll(coll);}
        }
	public boolean retainAll(Collection coll) {
	    synchronized(mutex) {return c.retainAll(coll);}
        }
	public void clear() {
	    synchronized(mutex) {c.clear();}
        }
	public String toString() {
	    synchronized(mutex) {return c.toString();}
        }
    }

    static class SynchronizedSet extends SynchronizedCollection
			         implements Set {
	SynchronizedSet(Set s) {
            super(s);
        }
	SynchronizedSet(Set s, Object mutex) {
            super(s, mutex);
        }

	public boolean equals(Object o) {
	    synchronized(mutex) {return c.equals(o);}
        }
	public int hashCode() {
	    synchronized(mutex) {return c.hashCode();}
        }
    }
}