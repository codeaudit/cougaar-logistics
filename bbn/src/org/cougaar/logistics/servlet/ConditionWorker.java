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
import org.cougaar.core.wp.ListAllAgents;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.ServletWorker;

/**
 * <pre>
 * Servlet worker
 *
 * Takes one parameter: 
 * - conditionValue, which controls the level of the double condition named 
 * in the component parameter *conditionName* in all agents with ConditionSetterServlets 
 * with the same condition name.
 *
 * An example URL is :
 * 
 * http://localhost:8800/$TRANSCOM/conditionsetter
 *
 * NOTE : If any agent hangs, the whole request will hang...
 * </pre>
 */
public class ConditionWorker
  extends ServletWorker {

  public NumberFormat numberFormat = new DecimalFormat ("#.#");

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/conditionsetter".
   */
  private final String myPath = "/conditionsetter";

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }

  /**
   * Main method. <p>
   * <p>
   * Setting debug level to *info* will give more information about which agents 
   * could not have their condition set.
   */
  public void execute(HttpServletRequest request, 
		      HttpServletResponse response,
		      SimpleServletSupport support) throws IOException, ServletException {
    this.support = (ConditionSupport) support;
    ConditionSupport conditionSupport = (ConditionSupport) support;

    XMLable responseData = null;

    super.execute (request, response, support);

    if (setAllAgents) {
      format = FORMAT_HTML;

      List knownAgents = getAllEncodedAgentNames();
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
	buf.append("?" + ConditionServlet.CONDITION_PARAM + "="+numberFormat.format(doubleCondition));

	String url = buf.toString();

	if (support.getLog().isInfoEnabled()) {
	  support.getLog().info(" In "+ support.getAgentIdentifier()+
				", setting " + conditionSupport.getConditionName() + " on "+agentName+
				", URL: "+url);
	}

	URL myURL = new URL(url);
	HttpURLConnection myConnection = (HttpURLConnection) myURL.openConnection();
	try {
	  if (myConnection.getResponseCode () != HttpURLConnection.HTTP_OK) {
	    if (myConnection.getResponseCode () == HttpURLConnection.HTTP_NOT_FOUND) {
	      support.getLog().info (support.getAgentIdentifier()+ 
				     " - servlet not at agent.  Got NOT FOUND code reading from URL " + myURL);
	    }
	    else {
	      support.getLog().error (support.getAgentIdentifier()+ 
				      " - got error reading from URL " + myURL + " code was " + 
				      myConnection.getResponseCode());
	    }
	  }
	  else {
	    // throws an FileNotFoundExcep if no servlet at that agent
	    InputStream is = myConnection.getInputStream(); 

	    ObjectInputStream ois = new ObjectInputStream(is); 

	    try {
	      ConditionData settingData = (ConditionData)ois.readObject();
	      if (settingData.wasSet()) {
		validAgents.add (agentName);
	      }
	      else {
		support.getLog().info (support.getAgentIdentifier()+ 
				       " - Could not set " + 
				       conditionSupport.getConditionName() + " on agent " + agentName);
	      }
	    } catch (Exception e) { 
	      support.getLog().error ("Got exception " + e, e);
	    } finally {
	      ois.close();
	    }
	  }
	} catch (FileNotFoundException fnf) {
	  if (support.getLog().isInfoEnabled())
	    support.getLog().info ("Skipping agent " + agentName + " that has no Condition servlet.");
	} catch (Exception other) {
	  if (support.getLog().isWarnEnabled())
	    support.getLog().warn ("Skipping agent " + agentName + " that has returned an exception.", other);
	} finally {
	  myConnection.disconnect();
	}
      }

      responseData = new ResponseData (doubleCondition, validAgents, conditionSupport.getConditionName());
    } else {
      boolean val = setDoubleCondition (doubleCondition); // set in getSettings
      responseData = new ConditionData(val); // return whether set successfully
    }

    writeResponse (responseData, response.getOutputStream(), request, support, format);
  }

  protected List getAllEncodedAgentNames() {
    try {
      // do full WP list (deprecated!)
      Set s = ListAllAgents.listAllAgents(support.getWhitePagesService());
      // URLEncode the names and sort
      List l = ListAllAgents.encodeAndSort(s);
      return l;
    } catch (Exception e) {
      throw new RuntimeException(
          "List all agents failed", e);
    }
  }

  protected static class ResponseData implements XMLable, Serializable {
    double doubleCondition;
    List validAgents;
    String conditionName;

    public ResponseData (double cpu, List agents, String conditionName) {
      doubleCondition = cpu;
      validAgents = agents;
      this.conditionName = conditionName;
    }

    public void toXML (XMLWriter w) throws IOException { 
      w.optagln("setting");
      w.write("Set condition " + conditionName + " to " + doubleCondition + " on " + validAgents + " agents.");
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

    if (eq(ConditionServlet.SET_ALL_AGENTS, name)) {
      setAllAgents = true;
    }
    else if (eq(ConditionServlet.CONDITION_PARAM, name)) {
      try {
	doubleCondition = numberFormat.parse (value).doubleValue();
      } catch (Exception e) {
	support.getLog().error ("bad format for decimal cpu level = " + value);
      }
    } else {
      if (support.getLog().isDebugEnabled())
	support.getLog().debug ("NOTE : Ignoring parameter named " + name);
    }
  }

  protected boolean setDoubleCondition(double value) {
    boolean successful = false;
    ConditionServlet.DoubleCondition doubleCondition = support.getCondition ();

    if (doubleCondition != null) {
      doubleCondition.setValue(new Double(value));

      if (support.getLog().isInfoEnabled()) 
	support.getLog().info(support.getAgentIdentifier() + " - Setting " + support.getConditionName () +
			      " = " + doubleCondition.getValue());

      try {
	support.getBlackboardService().openTransaction();
	support.getBlackboardService().publishChange(doubleCondition);
	successful = true;
      } 
      catch (Exception exc) {
	support.getLog().error ("Could not publish " + support.getConditionName() + " condition???", exc);
      }
      finally{
	support.getBlackboardService().closeTransactionDontReset();
      }  
    }
    else if (support.getLog().isInfoEnabled())
      support.getLog().info (support.getAgentIdentifier() + 
			     " - condition service could not find condition " +
			     support.getConditionName());

    return successful;
  }

  protected double doubleCondition;
  protected boolean setAllAgents = false;

  protected ConditionSupport support;
}

