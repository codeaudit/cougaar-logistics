/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import org.cougaar.mlm.ui.psp.transit.data.registration.Registration;
import org.cougaar.planning.servlet.data.xml.XMLable;

/**
 * Thread-safe cache of computed Registry instances.
 * <p>
 * This class will call <b><tt>computeRegistry</tt></b> as necessary
 * to supply sessions.
 * <p>
 * <pre>
 * Users must:
 *   1) beginSession to get a Registration (containing "sessionId")
 *   2) optionally lookup a Registry with a sessionId
 *   3) optionally repeat step (2) as many times as necessary, but
 *      note the "expirationTime" on the Registration
 *   4) endSession to close the session, which allows garbage collection.
 *      One a sessionId is closed a new one must be requested (step 1).
 * </pre>
 */
public interface RegistryCache {

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
      boolean includeTransitLegs);

  /**
   * Lookup a cached Registry.
   */
  public Registry lookupRegistry(String sessionId);

  /**
   * End a session.
   * <p>
   * If no other readers exist, or the entry is "ancient", it will
   * be removed from the cache.
   */
  public void endSession(String sessionId);

}
