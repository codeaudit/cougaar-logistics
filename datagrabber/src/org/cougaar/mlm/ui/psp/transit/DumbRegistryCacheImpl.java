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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.mlm.ui.psp.transit.data.registration.Registration;
import org.cougaar.planning.servlet.data.xml.XMLable;

/**
 * Thread-safe cache of computed Registry instances.
 * <p>
 * This <code>RegistryCache</code> creates a new <code>Registry</code>
 * per request -- it does no caching.
 * <p>
 * @see RegistryCache
 */
public class DumbRegistryCacheImpl
    implements RegistryCache {

  public static boolean VERBOSE = false;
  static {
    VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.DataGathererWorker.verbose");
  }

  private int sessionCounter;
  private Map regs;
  // regs.size() limit?

  public DumbRegistryCacheImpl() {
    regs = new HashMap();
  }

  /**
   * Create a new session where we want a Registry computed no earlier
   * than the given <tt>minEndTime</tt>.
   * <p>
   * Typically returns a <code>Registration</code>, but if an 
   * exception was caught then a <code>Failure</code> is returned.
   */
  public XMLable beginSession(
      RegistryComputer rcomp,
      long minEndTime,
      boolean includeTransitLegs) {
    Registry reg = new Registry();
    reg.setIncludeTransitLegs (includeTransitLegs);
    String sessionId = Integer.toString(sessionCounter++);
    reg.beginComputeTime = System.currentTimeMillis();
    // indicate in-progress with "infinite" end time
    reg.endComputeTime = Long.MAX_VALUE;
    if (VERBOSE) {
      System.out.println(
			 "## created new reg["+sessionId+"] beginComputeTime: "+
			 (reg.beginComputeTime % 100000)+
			 " to match requested "+
			 (minEndTime % 100000));
    }
    try {
      // compute, maybe throw an exception
      //
      // *************************
      // * this is the big call! *
      // *************************
      //
      rcomp.computeRegistry(reg);
      if (VERBOSE)
	System.out.println("## this thread computed reg["+sessionId+"]");
    } catch (Exception e) {
      // failed.
      return new Failure(e);
    }
    // SUCCESS!!!
    synchronized (regs) {
      regs.put(sessionId, reg);
    }
    return 
      new Registration(
          sessionId, 
          Long.MAX_VALUE);
  }

  /**
   * Lookup a cached Registry.
   */
  public Registry lookupRegistry(String sessionId) {
    synchronized (regs) {
      return (Registry)regs.get(sessionId);
    }
  }

  /**
   * End a session.
   */
  public void endSession(String sessionId) {
    synchronized (regs) {
      regs.remove(sessionId);
    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("regs:\n");
    buf.append("*******************************\n");
    synchronized (regs) {
      Iterator entryIter = regs.entrySet().iterator();
      while (entryIter.hasNext()) {
        Map.Entry ei = (Map.Entry)entryIter.next();
        buf.append("  ").append(ei.getKey());
        buf.append(" -> ").append(ei.getValue());
        buf.append("\n");
      }
    }
    buf.append("*******************************\n");
    return buf.toString();
  }
}
