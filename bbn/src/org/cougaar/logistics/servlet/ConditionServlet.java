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
 
package org.cougaar.logistics.servlet;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.adaptivity.*;

import org.cougaar.core.servlet.SimpleServletSupport;

import org.cougaar.planning.servlet.ServletBase;
import org.cougaar.planning.servlet.ServletWorker;
import org.cougaar.core.persist.NotPersistable;

/**
 * <pre>
 * Sets a double condition constrained to be in the range 0.0-1.0, in every agent with this servlet.
 *
 *
 * The name of the condition can be specified as a parameter to the component, e.g.
 *
 * plugin = org.cougaar.core.servlet.BlackboardServletComponent(org.cougaar.logistics.servlet.ConditionServlet, /conditionsetter, conditionName=ferris)
 * 
 * would create a condition named "ferris".  
 *
 * This condition can then be set in every other agent with a servlet that specifies "ferris" as the
 * the name of it's condition.  
 *
 * To concurrently specify a different condition, specify a different servlet name-condition name pair, e.g.
 *
 * plugin = org.cougaar.core.servlet.BlackboardServletComponent(org.cougaar.logistics.servlet.ConditionServlet, /buellersetter, conditionName=bueller)
 *
 * An example URL to access the servlet is :
 * 
 * http://localhost:8800/$TRANSCOM/conditionsetter
 *
 * NOTES :
 *  - the default name of the condition is "DoubleCondition".
 *  - the ConditionServiceProvider should also be loaded in any agent with this servlet
 *  - the servlet does *not* depend on load order
 *
 * </pre>
 */
public class ConditionServlet extends ServletBase {

  public static String CONDITION_PARAM="conditionValue";
  public static String SET_ALL_AGENTS="setAllAgents";

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/hello".
   */
  private final String myPath = "/conditionsetter";

  public static final String CONDITION_IDENTIFIER = "ConditionServlet.DoubleCondition";

  private static final OMCRange[] Condition_RANGES = {
    new ConditionRange(0.0, 10.0)
  };

  protected static class ConditionRange extends OMCRange {
    public ConditionRange (double a, double b) { super (a,b); }
  }

  private static final OMCRangeList Condition_VALUES = new OMCRangeList(Condition_RANGES);

  /**
   * Inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   *
   * Package scope
   **/
  static class DoubleCondition extends SensorCondition implements NotPersistable {
    public DoubleCondition(String name) {
      this (name,
	    ConditionServlet.Condition_VALUES,     
	    ConditionServlet.Condition_RANGES[0].getMin());
    }

    public DoubleCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }

  protected ServletWorker createWorker () { return new ConditionWorker (); }
 
  /** <pre>
   *
   * USAGE 
   *
   * Only called if no arguments are given.
   * </pre>
   */
  public void getUsage(PrintWriter out, SimpleServletSupport support) {
    out.print("<HTML><HEAD><TITLE>Condition Setting Usage</TITLE></HEAD><BODY>\n"+
	      "<H2><CENTER>Condition Setting Usage</CENTER></H2><P>\n"+
	      "<FORM METHOD=\"GET\" ACTION=\"/$");
    out.print(support.getEncodedAgentName());
    out.print(support.getPath());
    // choose between shallow and recursive displays
    out.print("\">\n"+
	      "Set generic condition to this value in all agents:<p>\n"+
	      "&nbsp;&nbsp;<INPUT TYPE=\"text\" NAME=\"" + CONDITION_PARAM + "\" "+
	      "VALUE=\"1.0\">&nbsp;Please type in a double value, e.g. in the range 0.0-1.0."+
	      "&nbsp;<p>\n");
    out.print("<INPUT TYPE=\"hidden\" NAME=\"" + SET_ALL_AGENTS + "\" "+
	      "VALUE=\"true\">"+
	      "&nbsp;\n");
    out.print("<P>\n"+
	      "<INPUT TYPE=\"submit\" NAME=\"Display\">\n"+
	      "</FORM></BODY></HTML>");
  }
}

