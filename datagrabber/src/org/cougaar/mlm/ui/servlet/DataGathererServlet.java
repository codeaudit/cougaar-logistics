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

package org.cougaar.mlm.ui.servlet;

import java.io.*;
import java.util.Collection;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.Subscription;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.planning.servlet.ServletBase;
import org.cougaar.planning.servlet.ServletWorker;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.cougaar.core.servlet.SimpleServletSupport;

import org.cougaar.mlm.ui.psp.transit.DumbRegistryCacheImpl;
import org.cougaar.mlm.ui.psp.transit.RegistryCache;

/**
 * This is a servlet that serves up TPFDD/DataGrabber Transportation Objects.
 * <p>
 * TOPS/GLMTrans developers should see <code>DataComputer</code>, which does all the 
 * real work.
 * <p>
 * Also see <code>Registry</code>, which holds the data, and 
 * <code>RegistryCache</code>, which manages multiple Registry instances and
 * multiple "reader" sessions.
 *
 * @see org.cougaar.mlm.ui.psp.transit.DataComputer
 * @see org.cougaar.mlm.ui.psp.transit.RegistryCache
 */
public class DataGathererServlet
  extends ServletBase {
  public static boolean VERBOSE = false;
  static {
    VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.HierarchyServlet.verbose");
  }

  /**
   * Our single RegistryCache instance.
   *
   * Note that this will be kept across multiple (possibly simultaneous)
   * PSP invocations -- it must be carefully synchronized and garbage
   * collected.  This is all done within the RegistryCache instance.
   */
  protected RegistryCache regCache;

  public RegistryCache getRegCache () { return regCache; }

  /**
   * just makes a registry cache
   **/
  public void setSimpleServletSupport(SimpleServletSupport support) {
    super.setSimpleServletSupport(support);
    // pick either the "dumb" cache implementation or the "smart" one
    //
    // for now we'll use the "dumb" one until we've fully debugged the
    //   DataComputer
    regCache = 
      new DumbRegistryCacheImpl();
    //new SmartRegistryCacheImpl();
  }

  /** DEBUG forced to off! **/
  public static final boolean DEBUG = false;

  protected ServletWorker createWorker () {
    return new DataGathererWorker (this);
  }

  public void getUsage (PrintWriter out, SimpleServletSupport support) {
    out.print("<HTML><HEAD><TITLE>DataGathererServlet Usage</TITLE></HEAD><BODY>\n"+
	      "<H2><CENTER>DataGathererServlet Usage</CENTER></H2><P>\n");
    out.print("Invalid usage, try \"?beginSession\".");
    out.print("<P>\n</BODY></HTML>");
  }
}
