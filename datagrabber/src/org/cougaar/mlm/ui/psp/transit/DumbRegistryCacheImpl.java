/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
