/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.logistics.plugin.utils.ALStatusChangeMessage;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.agent.service.alarm.ExecutionTimer;


import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;


public class SDUseCaseServlet extends ComponentServlet implements BlackboardClient {

    private BlackboardService blackboard;
    private static String ROLE = "PackagedPOLSupplyProvider";
    private static String[] ROLE_NAMES = {"AmmunitionProvider", "FuelSupplyProvider",
      "PackagedPOLSupplyProvider", "SparePartsProvider", "StrategicTransportationProvider",
      "SubsistenceSupplyProvider"};
    private static String PATH = "/SD_Use_Case";
    private static String ROLE_PARAMETER = "role";
    private static String END_DATE_PARAMETER = "enddate";

    public void init() {
    	// get the blackboard service
    	blackboard = (BlackboardService) serviceBroker.getService(this,
    								  BlackboardService.class, null);
    	if(blackboard == null) {
    	    throw new RuntimeException("Unable to obtain blackboard service");
    	}
    }

    public void unload() {
	super.unload();
	// release the blackboard service
	if (blackboard != null) {
	    serviceBroker.releaseService(this, BlackboardService.class, servletService);
	    blackboard = null;
	}
    }

    protected String getPath() {
	return PATH;
    }

//     public void setBlackboardService(BlackboardService blackboard) {
// 	this.blackboard = blackboard;
//     }

    // BlackboardClient method:
    public String getBlackboardClientName() {
	return toString();
    }

    // BlackboardClient method:
    public long currentTimeMillis() {
	return new ExecutionTimer().currentTimeMillis();
    }

//     // unused BlackboardClient method:
//     public boolean triggerEvent(Object event) {
// 	return false;
//     }

    UnaryPredicate pred =
	new UnaryPredicate() {
	    public boolean execute(Object o) {
		return o instanceof ALStatusChangeMessage;
	    }
	};

    public void doGet(HttpServletRequest req,
		      HttpServletResponse res) throws IOException {
	PrintWriter out = res.getWriter();
	Collection col;
	String end_date_string = req.getParameter(END_DATE_PARAMETER);
	String role = req.getParameter(ROLE_PARAMETER);
	SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
	Date end_date = null;
	if (end_date_string != null) {
	    end_date = dateFormat.parse(end_date_string, new ParsePosition(0));
	}
	long now = currentTimeMillis();
	printTitles(out);
	printInfo(now, out);
	try {
	    blackboard.openTransaction();
	    col = blackboard.query(pred);

	    if (! col.isEmpty()) {
		Iterator it = col.iterator();
		ALStatusChangeMessage message = (ALStatusChangeMessage) it.next();
		printChangeInfo(message, false, out);

	    } else {
		if (end_date_string == null) {
		    printForm(req, out);
		} else if (end_date == null) {
		    printError("Could not read date; try again.", out);
		    printForm(req, out);
		} else if (end_date.getTime() < now) {
		    printError("End date must be in the future; try again.", out);
		    printForm(req, out);
		} else {
		    ALStatusChangeMessage message =
			new ALStatusChangeMessage(role, false, end_date);
		    blackboard.publishAdd(message);
		    printChangeInfo(message, true, out);
		}
	    }
	} catch (Exception e) {
	    printError("Servlet error: " + e, out);
	} finally {
	    blackboard.closeTransactionDontReset();
	    out.println("</body>");
	    out.flush();
	}
    }

    private void printTitles(PrintWriter out) {
	String title = "Service Discovery Use Case Servlet";
	out.println("<HEAD><TITLE>" + title + "</TITLE></HEAD>" +
		    "<BODY><H1>" + title + "</H1>");
    }

    private void printInfo(long now, PrintWriter out) {
	String localAgent = getEncodedAgentName();
	out.println("<p>\n"+
		    "Agent: "+localAgent+
		    "<p>\n"+
		    "Current Society Time: "+ new Date(now));
    }

    private void printForm(HttpServletRequest req, PrintWriter out) {
      String optionString = "";
      for(int i=0; i<ROLE_NAMES.length; i++) {
        optionString = optionString.concat(
      "<option value=\"" + ROLE_NAMES[i] + "\">" + ROLE_NAMES[i] + "</option>" + "\n");
      }

	out.print("<form method=\"GET\" action=\"" +
		  req.getRequestURI() +
		  "\">\n"+
		  "Enter role: " +
		  "<select size=1 name=\"" + ROLE_PARAMETER + "\">\n" +
                  optionString +
		  "</select>" +
		  "<p>Enter new end date (MM/DD/YYYY): " +
		  "<input type=\"text\" name=\"" + END_DATE_PARAMETER + "\" size=40>\n" +
		  "<p><input type=\"submit\" value=\"Submit\">\n"+
		  "</form>\n");
    }

    private void printChangeInfo(ALStatusChangeMessage message, boolean newChange, PrintWriter out) {
	out.println("<p>\n");
	if (newChange) {
	    out.println("Successfully posted provider change:");
	} else {
	    out.println("Provider already changed:");
	}
	out.println("<ul><li>Role: " + message.getRole() +
		    "<li>End Date: " + message.getEndDate() +
		    "<li>Registry Updated: " + message.registryUpdated() +
		    "</ul>");
    }

    private void printError(String text, PrintWriter out) {
	out.println("<p>\n"+
		    "<b>" + text + "</b>"+
		    "<p>");
    }

    private String headWithTitle(String title) {
	return "<HEAD><TITLE>" + title + "</TITLE></HEAD>";
    }
}



