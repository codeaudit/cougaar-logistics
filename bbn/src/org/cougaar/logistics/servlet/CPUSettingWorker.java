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
 
package org.cougaar.logistics.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.cougaar.core.servlet.SimpleServletSupport;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.ServletWorker;

/**
 * <pre>
 * Servlet worker
 *
 * Takes one parameter: 
 * - CPU, which controls the level of the CPU condition in all agents
 *
 * An example URL is :
 * 
 * http://localhost:8800/$TRANSCOM/cpusetter
 *
 * NOTE : If any agent hangs, the whole request will hang...
 * </pre>
 */
public class CPUSettingWorker
  extends ServletWorker {

  public NumberFormat numberFormat = new DecimalFormat ("#.#");

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/hello".
   */
  private final String myPath = "/cpusetting";

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }

  /**
   * Main method. <p>
   * Most of the work is done in getHierarchy.
   * This method mainly checks that the parameters are the right 
   * number and sets the format and recurse fields.
   * <p>
   * @see #getHierarchyData(HttpServletRequest,SimpleServletSupport,HasRelationships,boolean,boolean,Set)
   */
  public void execute(HttpServletRequest request, 
		      HttpServletResponse response,
		      SimpleServletSupport support) throws IOException, ServletException {
    this.support = (CPUSettingSupport) support;
    XMLable responseData = null;

    super.execute (request, response, support);

    if (setAllAgents) {
      format = FORMAT_HTML;

      List knownAgents = support.getAllEncodedAgentNames();
      List validAgents = new ArrayList ();
      for (Iterator iter = knownAgents.iterator(); iter.hasNext(); ) {
	String agentName = (String) iter.next();
	StringBuffer buf = new StringBuffer();
	buf.append("http://");
	buf.append(request.getServerName());
	buf.append(":");
	buf.append(request.getServerPort());
	buf.append("/$");
	buf.append(agentName);
	buf.append(support.getPath());
	buf.append("?CPULevel="+numberFormat.format(CPULevel));

	String url = buf.toString();

	if (support.getLog().isInfoEnabled()) {
	  support.getLog().info(" In "+ support.getAgentIdentifier()+
				", setting CPU on "+agentName+
				", URL: "+url);
	}

	URL myURL = new URL(url);
	URLConnection myConnection = myURL.openConnection();
	try {
	  // throws an FileNotFoundExcep if no servlet at that agent
	  InputStream is = myConnection.getInputStream(); 

	  ObjectInputStream ois = new ObjectInputStream(is); 

	  try {
	    CPUSettingData settingData = (CPUSettingData)ois.readObject();
	    if (settingData.wasSet())
	      validAgents.add (agentName);
	    else
	      support.getLog().warn ("Could not set CPU on agent " + agentName);
	  } catch (Exception e) { 
	    support.getLog().error ("Got exception " + e, e);
	  }
	} catch (FileNotFoundException fnf) {
	  if (support.getLog().isInfoEnabled())
	    support.getLog().info ("Skipping agent " + agentName + " that has no CPUSetter servlet.");
	}
      }

      responseData = new ResponseData (CPULevel, validAgents);
    } else {
      boolean val = setCPUCondition (CPULevel); // set in getSettings
      responseData = new CPUSettingData(val); // return whether set successfully
    }

    writeResponse (responseData, response.getOutputStream(), request, support, format);
  }

  protected static class ResponseData implements XMLable, Serializable {
    double CPULevel;
    List validAgents;

    public ResponseData (double cpu, List agents) {
      CPULevel = cpu;
      validAgents = agents;
    }

    public void toXML (XMLWriter w) throws IOException { 
      w.optagln("setting");
      w.write("Set CPU to " + CPULevel + " on " + validAgents + " agents");
      w.cltagln("setting");
    }
  }

  /** 
   * <pre>
   * sets both recurse and format 
   *
   * recurse is either true or false
   * format  is either data, xml, or html
   *
   * see class description for what these values mean
   * </pre>
   */
  protected void getSettings (String name, String value) {
    super.getSettings (name, value);

    if (eq(CPUSettingServlet.SET_ALL_AGENTS, name)) {
      setAllAgents = true;
    }
    else if (eq(CPUSettingServlet.CPU, name)) {
      try {
	CPULevel = numberFormat.parse (value).doubleValue();
      } catch (Exception e) {
	support.getLog().error ("bad format for decimal cpu level = " + value);
      }
    } else {
      if (support.getLog().isDebugEnabled())
	support.getLog().debug ("NOTE : Ignoring parameter named " + name);
    }
  }

  protected boolean setCPUCondition(double value) {
    boolean successful = false;
    if (support.getConditionService () == null) {
      support.getLog().warn(support.getAgentIdentifier() + " - Could not set " + CPUSettingServlet.CPU_CONDITION_NAME + 
			    " because no condition service available.");
      return false;
    }

    CPUSettingServlet.CPUCondition cpu = (CPUSettingServlet.CPUCondition)
      support.getConditionService().getConditionByName(CPUSettingServlet.CPU_CONDITION_NAME);

    if (cpu != null) {
      cpu.setValue(new Double(value));

      if (support.getLog().isInfoEnabled()) 
	support.getLog().info(support.getAgentIdentifier() + " - Setting " + CPUSettingServlet.CPU_CONDITION_NAME + 
			      " = " + cpu.getValue());

      try {
	support.getBlackboardService().openTransaction();
	support.getBlackboardService().publishChange(cpu);
	successful = true;
      } 
      catch (Exception exc) {
	support.getLog().error ("Could not publish cpu condition???", exc);
      }
      finally{
	support.getBlackboardService().closeTransactionDontReset();
      }  
    }

    return successful;
  }

  protected double CPULevel;
  protected boolean setAllAgents = false;

  protected CPUSettingSupport support;
}

