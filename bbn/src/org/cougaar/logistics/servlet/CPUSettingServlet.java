/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 * An example URL is :
 * 
 * http://localhost:8800/$TRANSCOM/cpusetter
 *
 * </pre>
 */
public class CPUSettingServlet extends ServletBase {

  public static String CPU="CPU";
  public static String SET_ALL_AGENTS="SsetAllAgents";

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/hello".
   */
  private final String myPath = "/cpusetting";

  public static final String CPU_CONDITION_NAME = "CPUSettingServlet.CPULevel";

  private static final OMCRange[] CPU_RANGES = {
    new CPURange(0.0, 1.0)
  };

  protected static class CPURange extends OMCRange {
    public CPURange (double a, double b) { super (a,b); }
  }

  private static final OMCRangeList CPU_VALUES = new OMCRangeList(CPU_RANGES);

  /**
   * Inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   *
   * Package scope
   **/
  static class CPUCondition extends SensorCondition implements NotPersistable {
    public CPUCondition() {
      this (CPUSettingServlet.CPU_CONDITION_NAME,
	    CPUSettingServlet.CPU_VALUES,     
	    CPUSettingServlet.CPU_RANGES[0].getMin());
    }

    public CPUCondition(String name, OMCRangeList allowedValues, Comparable value) {
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

  protected ServletWorker createWorker () { return new CPUSettingWorker (); }
 
  /** <pre>
   *
   * USAGE 
   *
   * Only called if no arguments are given.
   * </pre>
   */
  public void getUsage(PrintWriter out, SimpleServletSupport support) {
    out.print("<HTML><HEAD><TITLE>CPU Setting Usage</TITLE></HEAD><BODY>\n"+
	      "<H2><CENTER>CPU Setting Usage</CENTER></H2><P>\n"+
	      "<FORM METHOD=\"GET\" ACTION=\"/$");
    out.print(support.getEncodedAgentName());
    out.print(support.getPath());
    // choose between shallow and recursive displays
    out.print("\">\n"+
	      "Set CPU condition to this value in all agents:<p>\n"+
	      "&nbsp;&nbsp;<INPUT TYPE=\"text\" NAME=\"" + CPU + "\" "+
	      "VALUE=\"1.0\">&nbsp;Please put in range 0.0-1.0."+
	      "&nbsp;<p>\n");
    out.print("<INPUT TYPE=\"hidden\" NAME=\"" + SET_ALL_AGENTS + "\" "+
	      "VALUE=\"true\">"+
	      "&nbsp;\n");
    out.print("<P>\n"+
	      "<INPUT TYPE=\"submit\" NAME=\"Display\">\n"+
	      "</FORM></BODY></HTML>");
  }
}

