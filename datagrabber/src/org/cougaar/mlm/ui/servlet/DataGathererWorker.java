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
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.cougaar.core.blackboard.Subscription;

import org.cougaar.core.servlet.SimpleServletSupport;

import org.cougaar.mlm.ui.psp.transit.DataComputer;
import org.cougaar.mlm.ui.psp.transit.Registry;
import org.cougaar.mlm.ui.psp.transit.RegistryCache;
import org.cougaar.mlm.ui.psp.transit.RegistryComputer;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.planning.servlet.ServletBase;
import org.cougaar.planning.servlet.ServletWorker;

import org.cougaar.util.UnaryPredicate;

/**
 * This is a Servlet that serves up TPFDD/DataGrabber Transportation Objects.
 * <p>
 * TOPS developers should see <code>DataComputer</code>, which does all the 
 * real work.
 * <p>
 * Also see <code>Registry</code>, which holds the data, and 
 * <code>RegistryCache</code>, which manages multiple Registry instances and
 * multiple "reader" sessions.
 *
 * @see DataComputer
 * @see RegistryCache
 */
public class DataGathererWorker
  extends ServletWorker implements RegistryComputer {
  public static boolean VERBOSE = false;
  static {
    VERBOSE = Boolean.getBoolean("org.cougaar.planning.servlet.dataGathererWorker.verbose");
  }

  public DataGathererWorker (ServletBase servlet) {
    this.servlet = servlet;
  }

  /**
   * Here is our inner class that will handle all HTTP and
   * HTTPS service requests for our <tt>myPath</tt>.
   */
  public void execute(HttpServletRequest request, 
		      HttpServletResponse response,
		      SimpleServletSupport support) throws IOException, ServletException {
    Enumeration params = request.getParameterNames ();
    for (;params.hasMoreElements();) {
      String name  = (String) params.nextElement (); 
      String value = request.getParameter (name);
      getSettings (name, value);
    }

    long start = System.currentTimeMillis ();

    if (support.getLog().isDebugEnabled())
      support.getLog ().debug (support.getAgentIdentifier() + " - got request <" + getRequestType () + 
			       "> at " + new Date (start));

    // parse command, use "regCache", maybe call "computeRegistry".
    XMLable result = getResult();

    if (result != null)
      writeResponse (result, response.getOutputStream(), request, support, format);

    long end = System.currentTimeMillis ();

    if (support.getLog().isInfoEnabled()) {
      String agent = support.getAgentIdentifier().toString();
      String tabs = (agent.length () > 9) ? "\t" : "\t\t";
      support.getLog ().info (agent + tabs + "did <" + getRequestType () + 
			      "> in " + (end-start) + " millis at " + new Date (end));
    }
  }

  protected String getPrefix () { return "DataGatherer at "; }

  //
  // BEGIN COMPUTE
  //

  /** implemented for RegistryComputer interface */
  public void computeRegistry(Registry reg) { 
    computeRegistry (servlet.getSupport (), reg); 
  }

  /**
   * 
   * Our <code>RegistryCache</code> will call this <tt>computeRegistry</tt>
   * via the "?" (a <code>RegistryComputer</code>).
   */
  protected static void computeRegistry(SimpleServletSupport support,
					Registry reg) {
    // create a new registry
    reg.beginComputeTime = System.currentTimeMillis();

    // get all the carrier assets
    Collection carrierAssets = 
      support.queryBlackboard(
			      DataComputer.getCarrierAssetsPredicate());
    DataComputer.computeAllCarriers(reg, carrierAssets);

    // get all the convoys
    Collection convoyTasks = 
      support.queryBlackboard(
			      DataComputer.getConvoysPredicate());
    DataComputer.computeAllConvoys(reg, convoyTasks);

    // get all the legs
    Collection legTasks = 
      support.queryBlackboard(
			      DataComputer.getLegTasksPredicate(reg.includeTransitLegs));
    DataComputer.computeAllLegs(reg, legTasks);

    // success!!!
  }

  //
  // END COMPUTE
  //

  /**
   * Parse command, use "regCache", maybe call "computeRegistry".
   *
   * @return an "XMLable" result.
   */
  protected XMLable getResult() {
    if ((flags == 0) ||
        ((flags & FLAG_USAGE) != 0)) {
      // usage
      return new Failure("Invalid usage, try \"?beginSession\".");
    }

    DataGathererServlet dgServlet = (DataGathererServlet) servlet;

    if ((flags & FLAG_BEGIN_SESSION) != 0) {
      // begin a new session
      //
      // returns either:
      //   a Registration, containing a sessionId
      // or
      //   a Failure, indicating the computeRegistry failure
      //
      // ******************************
      // * may call "computeRegistry" *
      // ******************************
      //
      // note there is a somewhat circular method trace:
      //   1) here we call "RegistryCache.beginSession"
      //   2) the regCache may call "computeRegistry",
      //      since "this" is a "RegistryComputer"
      //   3) this calls our "DataGathererWorker.computeRegistry"
      //   4) "DataGathererServlet.computeRegistry" calls DataComputer
      // this looks odd but lets us keep a clean interface
      //  -- just pretend it calls "computeRegistry" directly.
      return dgServlet.getRegCache().beginSession(this, minEndTime, includeTransitLegs);
    }

    XMLable result = null;

    if ((flags & MASK_GET_ALL) != 0) {
      // get data from an existing session
      //
      // lookup the session
      Registry reg = dgServlet.getRegCache().lookupRegistry(sessionId);
      if (reg != null) {
        // get the value (legs, locations, etc)
        //
        // FIXME prune down the data?
        if ((flags & FLAG_GET_LEGS) != 0) {
          result = reg.legs;
        } else if ((flags & FLAG_GET_LOCATIONS) != 0) {
          result = reg.locs;
        } else if ((flags & FLAG_GET_CARRIERS) != 0) {
          result = reg.carriers;
        } else if ((flags & FLAG_GET_CARGO_INSTANCES) != 0) {
          result = reg.cargoInstances;
        } else if ((flags & FLAG_GET_CARGO_PROTOTYPES) != 0) {
          result = reg.cargoPrototypes;
        } else if ((flags & FLAG_GET_CONVOYS) != 0) {
          result = reg.convoys;
        } else if ((flags & FLAG_GET_ROUTES) != 0) {
          result = reg.routes;
        } else {
          // shouldn't happen
          result = null;
        }
      } else {
        // no such session!
        return new Failure("No such session: "+sessionId);
      }
    }

    if ((flags & FLAG_END_SESSION) != 0) {
      // end the session
      dgServlet.getRegCache().endSession (sessionId);
    }

    return result;
  }

  /** 
   * use a query parameter to set a field 
   */
  public void getSettings(String name, String value) {
    super.getSettings (name, value);

    if (eq("sessionId", name)) {
      if ((flags & FLAG_BEGIN_SESSION) != 0) {
	// begin session is exclusive
	flags |= FLAG_USAGE;
	return;
      }
      sessionId = value;
    } else if (eq("minEndTime", name) ||
	       eq("time", name)) {
      if (((flags & FLAG_END_SESSION) != 0) ||
	  ((flags & MASK_GET_ALL) != 0) ||
	  (sessionId != null)) {
	// begin session is exclusive
	flags |= FLAG_USAGE;
	return;
      }
      // parse value
      try {
	minEndTime = Long.parseLong(value);
      } catch (NumberFormatException nfe) {
	// invalid long
	flags |= FLAG_USAGE;
      }
    } else if (eq("beginSession", name)) {
      if (((flags & FLAG_END_SESSION) != 0) ||
	  ((flags & MASK_GET_ALL) != 0) ||
	  (sessionId != null)) {
	// begin session is exclusive
	flags |= FLAG_USAGE;
	return;
      }
      flags |= FLAG_BEGIN_SESSION;
    } else if (eq("get", name)) {
      if ((flags & FLAG_BEGIN_SESSION) != 0) {
	// begin session is exclusive
	flags |= FLAG_USAGE;
	return;
      }
      if ((flags & MASK_GET_ALL) != 0) {
	// multiple get UNSUPPORTED!
	flags |= FLAG_USAGE;
	return;
      }
      // set the "get" flag
      String getArg = name.substring(3);
      if (eq("leg", getArg)) {
	flags |= FLAG_GET_LEGS;
      } else if (eq("loc", getArg)) {
	flags |= FLAG_GET_LOCATIONS;
      } else if (eq("carrier", getArg) ||
		 eq("convey", getArg) ||
		 eq("popul", getArg)) {
	flags |= FLAG_GET_CARRIERS;
      } else if (eq("cargoInst", getArg) ||
		 eq("inst", getArg)) {
	flags |= FLAG_GET_CARGO_INSTANCES;
      } else if (eq("cargoProto", getArg) ||
		 eq("proto", getArg)) {
	flags |= FLAG_GET_CARGO_PROTOTYPES;
      } else if (eq("convoy", getArg)) {
	flags |= FLAG_GET_CONVOYS;
      } else if (eq("route", getArg)) {
	flags |= FLAG_GET_ROUTES;
      } else {
	// unknown
	flags |= FLAG_USAGE;
      }
    } else if (eq("endSession", name)) {
      if ((flags & FLAG_BEGIN_SESSION) != 0) {
	// begin session is exclusive
	flags |= FLAG_USAGE;
	return;
      }
      flags |= FLAG_END_SESSION;
    } else if (eq("transitLegs", name)) {
      includeTransitLegs = true;
    }
  }

  protected String getRequestType () {
    if ((flags & FLAG_BEGIN_SESSION) != 0) {
      return "Begin Session";
    } else if ((flags & MASK_GET_ALL) != 0) {
      if ((flags & FLAG_GET_LEGS) != 0) {
	return "Get Legs";
      } else if ((flags & FLAG_GET_LOCATIONS) != 0) {
	return "Get Locations";
      } else if ((flags & FLAG_GET_CARRIERS) != 0) {
	return "Get Carriers";
      } else if ((flags & FLAG_GET_CARGO_INSTANCES) != 0) {
	return "Get Cargo Instances";
      } else if ((flags & FLAG_GET_CARGO_PROTOTYPES) != 0) {
	return "Get Cargo Prototypes";
      } else if ((flags & FLAG_GET_CONVOYS) != 0) {
	return "Get Convoys";
      } else if ((flags & FLAG_GET_ROUTES) != 0) {
	return "Get Routes";
      } else {
	return "Unknown????";
      }
    }
    else if ((flags & FLAG_END_SESSION) != 0) {
      return "End Session";
    }
    return "Huh?  Unknown????";
  }

  /** fields **/
  // output FORMAT_*

  // binary OR of FLAG_ constants
  public int flags;
  // session identifier for FLAG_GET_* and FLAG_END_SESSION
  public String sessionId;
  // minimal endComputeTime for FLAG_BEGIN_SESSION
  public long minEndTime;
  public boolean includeTransitLegs = false;

  // usage
  public static final int FLAG_USAGE                    = (1<< 0);
  // register
  public static final int FLAG_BEGIN_SESSION            = (1<< 1);
  // get the legs
  public static final int FLAG_GET_LEGS                 = (1<< 2);
  // get the locations
  public static final int FLAG_GET_LOCATIONS            = (1<< 3);
  // get the carriers
  public static final int FLAG_GET_CARRIERS             = (1<< 4);
  // get the cargo instances
  public static final int FLAG_GET_CARGO_INSTANCES      = (1<< 5);
  // get the cargo prototypes
  public static final int FLAG_GET_CARGO_PROTOTYPES     = (1<< 6);
  // get the convoys
  public static final int FLAG_GET_CONVOYS              = (1<< 7);
  // get the routes
  public static final int FLAG_GET_ROUTES               = (1<< 8);
  // get the routes
  public static final int FLAG_GET_TRANSIT_LEGS         = (1<< 9);
  // end the session
  public static final int FLAG_END_SESSION              = (1<< 10);

  // some useful flag combinations
  public static final int MASK_GET_ALL =
    (FLAG_GET_LEGS |
     FLAG_GET_LOCATIONS |
     FLAG_GET_CARRIERS |
     FLAG_GET_CARGO_INSTANCES |
     FLAG_GET_CARGO_PROTOTYPES |
     FLAG_GET_CONVOYS |
     FLAG_GET_ROUTES |
     FLAG_GET_TRANSIT_LEGS);

  protected ServletBase servlet;
}
