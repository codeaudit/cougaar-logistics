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
