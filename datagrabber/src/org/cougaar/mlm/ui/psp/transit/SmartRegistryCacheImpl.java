/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.mlm.ui.psp.transit;

import java.util.List;
import java.util.ArrayList;

import org.cougaar.mlm.ui.psp.transit.data.registration.Registration;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.data.Failure;

/**
 * Thread-safe cache of computed Registry instances.
 * <p>
 * This <code>RegistryCache</code> attempts to cache prior computations
 * and manage multiple readers for a single "session" group.  Cache
 * synchronization is complex, so odds are that some bugs might be hiding
 * in here!
 * <p>
 * @see RegistryCache
 */
public class SmartRegistryCacheImpl
    implements RegistryCache {

  /**
   * No entry in the cache lives longer than this time (in millis).
   */
  public static final long ENTRY_LIFETIME = (30*60*1000); // thirty minutes

  /**
   * A <code>List</code> of "SharedRegistry" Objects.
   */
  private int sregCounter = 0;
  private List sregs;
  // session and/or sregs.size() limit?

  public SmartRegistryCacheImpl() {
    sregs = new ArrayList();
  }

  /**
   * Create a new session where we want a Registry computed no earlier
   * than the given <tt>minEndTime</tt>.
   * <p>
   * Typically returns a <code>Registration</code>, but if an 
   * exception was caught then a <code>Failure</code> is returned.
   * <p>
   * If a matching session already exists then the user is added as
   * a "reader" of that session, otherwise a new Registry is
   * <b>computed</b>.
   */
  public XMLable beginSession(
      RegistryComputer rcomp,
      long minEndTime,
      boolean includeTransitLegs) {
    SharedRegistry sreg = null;
    Registry reg = null;
    String sessionId = null;
    // find a shared registry, create one if none match
    synchronized (sregs) {
      // remove the ancient/unused sregs
      privReclaimMemory();
      // get the most recent sreg
      int nSregs = sregs.size();
      SharedRegistry newestSreg;
      // FIXME check interpolate flag!
      if ((nSregs > 0) &&
          ((newestSreg = 
            (SharedRegistry)sregs.get(nSregs-1)).reg.endComputeTime >= 
           minEndTime)) {
        // join this existing Registry.  note that endComputeTime can be 
        //   Long.MAX_VALUE, indicating that the computation is in-progress.
        sreg = newestSreg;
        reg = sreg.reg;
        System.out.println(
          "## using old sreg["+sreg.sregId+"] endComputeTime: "+
          ((reg.endComputeTime < Long.MAX_VALUE) ? 
           Long.toString(reg.endComputeTime % 100000) :
           "<computing>")+
          " to match requested "+
          (minEndTime % 100000));
      } else {
        // too old or non-existent, create a new reg
        sreg = new SharedRegistry();
        sreg.sregId = sregCounter++;
        reg = new Registry();
        sreg.reg = reg;
        reg.beginComputeTime = System.currentTimeMillis();
        // indicate in-progress with "infinite" end time
        reg.endComputeTime = Long.MAX_VALUE;
        System.out.println(
          "## created new reg["+sreg.sregId+"] beginComputeTime: "+
          (reg.beginComputeTime % 100000)+
          " to match requested "+
          (minEndTime % 100000));
        sregs.add(sreg);
      }
      // add a reader
      sessionId = sreg.sregId+"-"+sreg.addReader();
    }
    // if created one, fill it in (compute!)
    if (reg.endComputeTime == Long.MAX_VALUE) {
      synchronized (reg) {
        if (reg.endComputeTime == Long.MAX_VALUE) {
          try {
            // compute, maybe throw an exception
            //
            // *************************
            // * this is the big call! *
            // *************************
            //
            rcomp.computeRegistry(reg);
            System.out.println(
              "## this thread computed reg["+sreg.sregId+"]");
          } catch (Exception e) {
            // failed.
            sreg.failedCompute = new Failure(e);
            synchronized (sregs) {
              // remove the sreg!  we don't want to sync sregs
              //   while we compute, so we live with some
              //   dead session attempts here...
              sregs.remove(sreg);
            }
          }
        } else {
          System.out.println(
          "## another thread computed reg["+sreg.sregId+"]");
        }
      }
    }
    // see what we got...
    if (sreg.failedCompute != null) {
      // failed.
      return sreg.failedCompute;
    }
    // SUCCESS!!!
    return 
      new Registration(
        sessionId, 
        (reg.endComputeTime + ENTRY_LIFETIME));
  }

  /**
   * Lookup a cached Registry.
   */
  public Registry lookupRegistry(String sessionId) {
    // parse the session id
    int sregId;
    int sreaderId;
    try {
      int sep = sessionId.indexOf("-");
      sregId = Integer.parseInt(sessionId.substring(0, sep));
      sreaderId = Integer.parseInt(sessionId.substring(sep+1));
    } catch (Exception e) {
      // invalid session id
      return null;
    }
    // lookup in sregs
    synchronized (sregs) {
      // lookup
      SharedRegistry sreg =
        privLookupSharedRegistry(sregId);
      if (sreg == null) {
        // check reader's hold/release status
        if (sreg.isReader(sreaderId)) {
          return sreg.reg;
        }
      }
      // remove the ancient sregs, since we have no reaper thread
      privReclaimMemory();
    }
    return null;
  }

  /**
   * End a session.
   * <p>
   * If no other readers exist, or the entry is "ancient", it will
   * be removed from the cache.
   * <p>
   * We could create a "reaper" thread to do this...
   */
  public void endSession(String sessionId) {
    // parse the session id
    int sregId;
    int sreaderId;
    try {
      int sep = sessionId.indexOf("-");
      sregId = Integer.parseInt(sessionId.substring(0, sep));
      sreaderId = Integer.parseInt(sessionId.substring(sep+1));
    } catch (Exception e) {
      // invalid session id
      return;
    }
    synchronized (sregs) {
      // lookup
      SharedRegistry sreg =
        privLookupSharedRegistry(sregId);
      if (sreg != null) {
        // remove this reader
        sreg.removeReader(sreaderId);
        if (!(sreg.anyReaders())) {
          // no more readers for this sreg
          //
          // still want to keep the newest sreg
          if (sregs.get(sregs.size()-1) != sreg) {
            System.out.println(
              "## removing unused reg["+sreg.sregId+"]");
            sregs.remove(sreg);
          }
        }
      }
      // remove the ancient sregs, since we have no reaper thread
      privReclaimMemory();
    }
  }

  //
  // private methods
  //

  /**
   * Get the SharedRegistry with the given <tt>sregId</tt>.
   * <p>
   * Assumes <tt>sregs</tt> synchronization.
   */
  private SharedRegistry privLookupSharedRegistry(
      int sregId) {
    // get the specified reg
    int i = sregs.size();
    while (true) {
      if (--i < 0) {
        // no such session, or expired
        return null;
      }
      SharedRegistry sri = (SharedRegistry)sregs.get(i);
      if (sri.sregId == sregId) {
        // found it
        return sri;
      }
    }
  }

  /**
   * Release any ancient SharedRegistry entries to enable
   * garbage collection.
   * <p>
   * Assumes <tt>sregs</tt> synchronization.
   */
  private void privReclaimMemory() {
    int nSregs = sregs.size();
    if (nSregs > 0) {
      // remove ancient sregs
      long expTime = System.currentTimeMillis() - ENTRY_LIFETIME;
      do {
        SharedRegistry earliestSreg = (SharedRegistry)sregs.get(0);
        if (earliestSreg.reg.endComputeTime < expTime) {
          // ancient
          System.out.println(
            "## removing ancient reg["+earliestSreg.sregId+"]");
          sregs.remove(0);
          --nSregs;
        } else {
          // sregs in nondecreasing time order, so no more ancient sregs
          break;
        }
      } while (nSregs > 0);
    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    synchronized (sregs) {
      int nSregs = sregs.size();
      buf.append("reg[").append(nSregs).append("]:\n");
      buf.append("*******************************\n");
      for (int i = 0; i < nSregs; i++) {
        buf.append("reg[").append(i);
        buf.append(" / ").append(nSregs).append("]: \n");
        buf.append(sregs.get(i)).append("\n");
      }
      buf.append("*******************************\n");
    }
    return buf.toString();
  }


  static class SharedRegistry {
    // id which is used with a reader number to create a unique
    //   "sessionId":   sregId+"-"+sreaderId
    public int sregId;

    // failure if unable to compute
    //   this is an artifact of the sync/threading
    public Failure failedCompute;

    // multiple readers
    public int nReaders;
    public boolean[] readers;

    // the shared Registry!
    public Registry reg;

    public SharedRegistry() {
      readers = new boolean[3];
      nReaders = 0;
    }

    /**
     * return reader number.
     */
    public int addReader() {
      if (nReaders >= readers.length) {
        int newCapacity = 2 * readers.length;
        boolean[] newReaders = new boolean[newCapacity];
        System.arraycopy(readers, 0, newReaders, 0, readers.length);
        this.readers = newReaders;
      }
      readers[nReaders] = true;
      return nReaders++;
    }

    public void removeReader(int i) {
      if (i < nReaders) {
        readers[i] = false;
      }
    }

    public boolean isReader(int i) {
      return 
        ((i < nReaders) &&
         readers[i]);
    }

    public boolean anyReaders() {
      int i = nReaders;
      while (--i >= 0) {
        if (readers[i]) {
          // could cache this index
          System.out.println("********** reg["+sregId+"]["+i+"]");
          return true;
        }
      }
      return false;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("SharedRegistry {");
      buf.append("\n  sregId: ").append(sregId);
      if (failedCompute != null) {
        buf.append("\n  failure: ").append(failedCompute);
      }
      buf.append("\n  readers[").append(nReaders).append("]:");
      for (int i = 0; i < nReaders; i++) {
        if (readers[i]) { 
          buf.append("\n  ").append(i);
        }
      }
      if (reg != null) {
        buf.append("\n  registry: ").append(reg);
      }
      buf.append("\n}");
      return buf.toString();
    }
  }
}
