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

import java.io.*;
import java.util.Collection;

import org.cougaar.lib.planserver.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.Subscription;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;

/**
 * This is a PSP that serves up TPFDD/DataGrabber Transportation Objects.
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
public class PSP_DataGatherer 
    extends PSP_BaseAdapter 
    implements PlanServiceProvider, UISubscriber {

  /**
   * Our single RegistryCache instance.
   *
   * Note that this will be kept across multiple (possibly simultaneous)
   * PSP invocations -- it must be carefully synchronized and garbage
   * collected.  This is all done within the RegistryCache instance.
   */
  protected RegistryCache regCache;

  /**
   * A zero-argument constructor is required for dynamically loaded PSPs,
   *  required by Class.newInstance()
   **/
  public PSP_DataGatherer() {
    super();
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

  /****************************************************************************
   * Main execute method for PSP
   **/
  public void execute(PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception {
    MyPSPState myState = new MyPSPState(this, query_parameters, psc);
    myState.configure(query_parameters);

    System.out.println("PSP_DataGatherer Invoked...");

    // parse myState command, use "regCache", maybe call "computeRegistry".
    //
    // returns an "XMLable" result.
    XMLable result = getResult(myState);

    if (result != null) {
      // write the result back to the user
      try {
        if (myState.format == MyPSPState.FORMAT_DATA) {
          // serialize
          ObjectOutputStream oos = new ObjectOutputStream(out);
          oos.writeObject(result);
          oos.flush();
        } else {
          // xml or html-wrapped xml
          XMLWriter w;
          if (myState.format == MyPSPState.FORMAT_HTML) {
            // wrapped xml
            out.println(
              "<HTML><HEAD><TITLE>DataGatherer at "+
              myState.clusterID+
              "</TITLE></HEAD><BODY>\n"+
              "<H2><CENTER>DataGatherer at "+
              myState.clusterID+
              "</CENTER></H2><p><pre>\n");
            w = 
              new XMLWriter(
                new OutputStreamWriter(
                  new XMLtoHTMLOutputStream(out)),
                true);
          } else {
            // raw xml
            out.println("<?xml version='1.0'?>");
            w = 
              new XMLWriter(
                new OutputStreamWriter(out));
          }
          // write as xml
          result.toXML(w);
          w.flush();
          if (myState.format == MyPSPState.FORMAT_HTML) {
            out.println(
              "\n</pre></BODY></HTML>\n");
          }
        }
      } catch (Exception writeException) {
        System.err.println("PSP_DataGatherer: Exception processing: "+
            writeException);
        writeException.printStackTrace();
      }
    }
  }

  //
  // BEGIN COMPUTE
  //

  /**
   * 
   * Our <code>RegistryCache</code> will call this <tt>computeRegistry</tt>
   * via the "MyPSPState" (a <code>RegistryComputer</code>).
   */
  private static void computeRegistry(
      MyPSPState myState,
      Registry reg) {
    // create a new registry
    reg.beginComputeTime = System.currentTimeMillis();

    // get all the carrier assets
    Collection carrierAssets = 
      myState.sps.queryForSubscriber(
        DataComputer.getCarrierAssetsPredicate());
    DataComputer.computeAllCarriers(reg, carrierAssets);

    // get all the convoys
    Collection convoyTasks = 
      myState.sps.queryForSubscriber(
        DataComputer.getConvoysPredicate());
    DataComputer.computeAllConvoys(reg, convoyTasks);

    // get all the legs
    Collection legTasks = 
      myState.sps.queryForSubscriber(
				     DataComputer.getLegTasksPredicate(false /** ignore transit legs */));
    DataComputer.computeAllLegs(reg, legTasks);

    // success!!!
  }

  //
  // END COMPUTE
  //

  /**
   * Parse myState command, use "regCache", maybe call "computeRegistry".
   *
   * @return an "XMLable" result.
   */
  private final XMLable getResult(MyPSPState myState) {

    int flags = myState.flags;

    if ((flags == 0) ||
        ((flags & MyPSPState.FLAG_USAGE) != 0)) {
      // usage
      return new Failure(myState.getUsage());
    }

    if ((flags & myState.FLAG_BEGIN_SESSION) != 0) {
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
      //   2) the regCache may call "MyPSPState.computeRegistry",
      //      since "MyPSPState" is a "RegistryComputer"
      //   3) myState calls our "PSP_DataGatherer.computeRegistry"
      //   4) "PSP_DataGatherer.computeRegistry" calls DataComputer
      // this looks odd but lets us keep a clean interface
      //  -- just pretend it calls "computeRegistry" directly.
      return regCache.beginSession(myState, myState.minEndTime, false);
    }

    XMLable result = null;

    if ((flags & myState.MASK_GET_ALL) != 0) {
      // get data from an existing session
      //
      // lookup the session
      Registry reg = regCache.lookupRegistry(myState.sessionId);
      if (reg != null) {
        // get the value (legs, locations, etc)
        //
        // FIXME prune down the data?
        if ((flags & MyPSPState.FLAG_GET_LEGS) != 0) {
          result = reg.legs;
        } else if ((flags & MyPSPState.FLAG_GET_LOCATIONS) != 0) {
          result = reg.locs;
        } else if ((flags & MyPSPState.FLAG_GET_CARRIERS) != 0) {
          result = reg.carriers;
        } else if ((flags & MyPSPState.FLAG_GET_CARGO_INSTANCES) != 0) {
          result = reg.cargoInstances;
        } else if ((flags & MyPSPState.FLAG_GET_CARGO_PROTOTYPES) != 0) {
          result = reg.cargoPrototypes;
        } else if ((flags & MyPSPState.FLAG_GET_CONVOYS) != 0) {
          result = reg.convoys;
        } else if ((flags & MyPSPState.FLAG_GET_ROUTES) != 0) {
          result = reg.routes;
        } else {
          // shouldn't happen, guarded by myState parser.
          result = null;
        }
      } else {
        // no such session!
        return new Failure("No such session: "+myState.sessionId);
      }
    }

    if ((flags & myState.FLAG_END_SESSION) != 0) {
      // end the session
      regCache.endSession(myState.sessionId);
    }

    return result;
  }

  /**
   * Convert XML to HTML-friendly output.
   *
   * Taken from PSP_PlanView.  For Internet Explorer this isn't such
   * a big deal...
   */
  protected static class XMLtoHTMLOutputStream 
      extends FilterOutputStream {
    protected static final byte[] LESS_THAN;
    protected static final byte[] GREATER_THAN;
    static {
      LESS_THAN = "<font color=green>&lt;".getBytes();
      GREATER_THAN = "&gt;</font>".getBytes();
    }
    public XMLtoHTMLOutputStream(OutputStream o) {
      super(o);
    }
    public void write(int b) throws IOException {
      if (b == '<') {
        out.write(LESS_THAN);
      } else if (b == '>') {
        out.write(GREATER_THAN);
      } else {
        out.write(b);
      }
    }
  }

  /** 
   * Use instead of "this" to force no instance field usage.
   **/
  protected static class MyPSPState 
      extends PSPState 
      implements RegistryComputer {

    public void computeRegistry(Registry reg) {
      // let the PSP compute!
      PSP_DataGatherer.computeRegistry(this, reg);
    }

    /** fields **/
    // output FORMAT_*
    public int format;
    // binary OR of FLAG_ constants
    public int flags;
    // session identifier for FLAG_GET_* and FLAG_END_SESSION
    public String sessionId;
    // minimal endComputeTime for FLAG_BEGIN_SESSION
    public long minEndTime;

    // format
    public static final int FORMAT_DATA = 0; // default
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

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
    // end the session
    public static final int FLAG_END_SESSION              = (1<< 9);

    // some useful flag combinations
    public static final int MASK_GET_ALL =
      (FLAG_GET_LEGS |
       FLAG_GET_LOCATIONS |
       FLAG_GET_CARRIERS |
       FLAG_GET_CARGO_INSTANCES |
       FLAG_GET_CARGO_PROTOTYPES |
       FLAG_GET_CONVOYS |
       FLAG_GET_ROUTES);

    /**
     * constructor 
     */
    public MyPSPState(
        UISubscriber xsubscriber,
        HttpInput query_parameters,
        PlanServiceContext xpsc) {
      super(xsubscriber, query_parameters, xpsc);
    }

    /**
     * get usage help
     */
    public String getUsage() {
      return
        "Invalid usage, try \"?beginSession\".";
    }

    /** 
     * use a query parameter to set a field 
     */
    public void setParam(String name, String value) {
      //super.setParam(name, value);
      if (eq("format", name)) {
        if (eq("data", value)) {
          format = FORMAT_DATA;
        } else if (eq("xml", value)) {
          format = FORMAT_XML;
        } else if (eq("html", value)) {
          format = FORMAT_HTML;
        } else {
          // unknown format
          flags |= FLAG_USAGE;
        }
      } else if (eq("sessionId", name)) {
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
      }
    }

    // startsWithIgnoreCase
    private static final boolean eq(String a, String b) {
      return a.regionMatches(true, 0, b, 0, a.length());
    }
  }

  //
  // END HELPER CLASSES
  //


  //
  // uninteresting/obsolete methods
  //

  public boolean returnsXML() { return true; }
  public boolean returnsHTML() { return false; }
  public String getDTD()  {
    return "";
  }
  public void subscriptionChanged(Subscription subscription) {
  }
  public PSP_DataGatherer(String pkg, String id) throws RuntimePSPException {
    this();
    setResourceLocation(pkg, id);
  }
  public boolean test(HttpInput query_parameters, PlanServiceContext sc) {
    super.initializeTest();
    return false;
  }

}
