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
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class AvailabilityServlet extends ComponentServlet implements BlackboardClient {

  private BlackboardService blackboard;
  private AlarmService alarmService;
  private List registeredRoles;
  private final static String PATH = "/availabilityServlet";
  private final static String ROLE = "role";
  private final static String AVAILABILITY = "Availability";
  private final static String START_DATE = "startdate";
  private final static String END_DATE = "enddate";
  private final static String ACTION_PARAM = "action";
  private final static String PUBLISH = "Publish All Rows";
  private final static String ADD = "Add";
  private final static String CLEAR = "clear";
  private final static String CLEAR_SELECTED_ROWS = "Clear Selected Rows";
  private final static String BACK = "BACK";

  private List changeMessages = new ArrayList();
  private static Logger logger = Logging.getLogger(AvailabilityServlet.class);
  private PrintWriter out;
  private HttpServletRequest request;

  public void init() {
    // get the blackboard service
    blackboard = (BlackboardService) serviceBroker.getService(this,
                                                              BlackboardService.class, null);
    if (blackboard == null) {
      throw new RuntimeException("Unable to obtain blackboard service");
    }
    alarmService = (AlarmService) serviceBroker.getService(this, AlarmService.class, null);
    if (alarmService == null) {
      throw new RuntimeException("Unable to set alarm service");
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

  // BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // BlackboardClient method:
  public long currentTimeMillis() {
    return alarmService.currentTimeMillis();
  }

  protected static UnaryPredicate availabilityPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof AvailabilityChangeMessage;
    }
  };

  public void doGet(HttpServletRequest req,
                    HttpServletResponse res) throws IOException {
    out = res.getWriter();
    request = req;
    registeredRoles = getProviderRoles();
    String start_date_string = request.getParameter(START_DATE);
    String end_date_string = request.getParameter(END_DATE);
    String role = request.getParameter(ROLE);
    String available = request.getParameter(AVAILABILITY);
    String action = request.getParameter(ACTION_PARAM);
    boolean isAvailable = true;
    if (available != null) {
      isAvailable = available.equals("available");
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    Date start_date = null;
    Date end_date = null;
    if (end_date_string != null) {
      end_date = dateFormat.parse(end_date_string, new ParsePosition(0));
    }
    if (start_date_string != null) {
      start_date = dateFormat.parse(start_date_string, new ParsePosition(0));
    }
    long now = currentTimeMillis();
    printTitles();
    printInfo(now);
     try {
       if (action == null) {
         printForm();
       } else if (action.equals(ADD)) {
         if (end_date == null) {
           printError("Could not read date; try again.");
           printForm();
         } else if (end_date.getTime() < now) {
           printError("End date must be in the future; try again.");
           printForm();
         } else {
           addChangeMessage(start_date, end_date, role, isAvailable);
         }
       } else if (action.equals(CLEAR_SELECTED_ROWS)) {
         clearSelectedRows();
       } else if (action.equals(PUBLISH)) {
         publish();
       } else if (action.equals(BACK)) {
         printForm();
       }
     } catch (Exception e) {
       printError("Servlet error: " + e);
     } finally {
       if (blackboard.isTransactionOpen()) {
         blackboard.closeTransactionDontReset();
       }
       out.println("</body>");
       out.flush();
     }
  }

  private void clearSelectedRows() {
    String paramValues [] = request.getParameterValues(CLEAR);
    for(int i = 0; i < paramValues.length; i++) {
      int index = Integer.parseInt(paramValues[i]);
      changeMessages.remove(index);
    }
    printForm();
    printSelections();
  }

  private void addChangeMessage(Date start_date, Date end_date, String role, boolean available) {
    printForm();
    MutableTimeSpan span = new MutableTimeSpan();
    span.setTimeSpan(start_date.getTime(), end_date.getTime());
    AvailabilityChangeMessage message = new AvailabilityChangeMessage(Role.getRole(role),
                                                                      false, span,
                                                                      available);
    changeMessages.add(message);
    printSelections();
  }

  private void publish() {
    blackboard.openTransaction();
    for (Iterator iterator = changeMessages.iterator(); iterator.hasNext();) {
      AvailabilityChangeMessage acm = (AvailabilityChangeMessage) iterator.next();
      blackboard.publishAdd(acm);
    }
    blackboard.closeTransaction();
    printChangeInfo(changeMessages);
    changeMessages.clear();

  }

  private List getProviderRoles() {
    Collection capabilites = getMyCapabilities();
    List roles = new ArrayList();
    for (Iterator iterator = capabilites.iterator(); iterator.hasNext();) {
      ProviderCapability pc = (ProviderCapability) iterator.next();
      pc.getRole();
      roles.add(pc.getRole());
    }
    return roles;
  }

  private void printTitles() {
    String title = "Availability  Servlet";
    out.println("<HEAD><TITLE>" + title + "</TITLE></HEAD>" +
                "<BODY><H1>" + title + "</H1>");
  }

  private void printInfo(long now) {
    String localAgent = getEncodedAgentName();
    out.println("<p>\n" +
                "Agent: " + localAgent +
                "<p>\n" +
                "Current Society Time: " + new Date(now));
  }

  private void printForm() {
    // Display the registered roles in a table
    out.print("<p>\n" + "<table border>"
              + "<tr><th colspan=1>" + "Registered Provider Roles" + "</th>" + "<th colspan=1>"
              + "Active Contracts" + "</th></tr>");
    Iterator iter = registeredRoles.iterator();
    StringBuffer roleOptions = new StringBuffer();
    while (iter.hasNext()) {
      Role role = (Role) iter.next();
      String client = "No";
      if (hasServiceContract(role)) {
        client = "Yes";
      }
      out.print("<tr><td>" + role + "</td><td align=center>" + client + "</td></tr>");
      roleOptions.append("<option value=\"" + role + "\">" + role + "</option>" + "\n");
    }
    out.print("</table>");
    out.print("<form method=\"GET\" action=\"" +
              request.getRequestURI() +
              "\">\n" +
              "Select role: " +
              "<select size=1 name=\"" + ROLE + "\">\n" +
              roleOptions.toString() +
              "</select>" +
              "<input type=radio name=\"" + AVAILABILITY + "\" value=\"available\">Available" +
              "<input type=radio name=\"" + AVAILABILITY + "\" value=\"unavailable\">Unavailable<BR>" +
              "<p>Enter new start date (MM/DD/YYYY): " +
              "<input type=\"text\" name=\"" + START_DATE + "\" size=40>\n" +
              "</select>" +
              "<p>Enter new end date (MM/DD/YYYY): " +
              "<input type=\"text\" name=\"" + END_DATE + "\" size=40>\n" +
              "<p><input type=submit name=\"" + ACTION_PARAM + "\" value=\"" + ADD + "\">\n" +
              "</form>\n");
    //printSelections(out);
  }

  private void printSelections() {
    if (changeMessages.size() == 0) {
      return;
    }
    out.print("<form method=\"GET\" action=\"" +
              request.getRequestURI() +
              "\">\n");
    // Table column headers
    out.print("<p>\n" + "<table border>" + "<tr>" +
              "<th colspan=1>"+ "Role" + "</th>" +
              "<th colspan=1>" + "Availability" + "</th>" +
              "<th colspan=1>" + "Start" + "</th>" +
              "<th colspan=1>" + "End" + "</th>" +
              "<th colspan=1>" + "Clear Row" + "</th>" +
              "</tr>");

    // table row elements
    for (int i = 0; i < changeMessages.size(); i++) {
      AvailabilityChangeMessage acm = (AvailabilityChangeMessage) changeMessages.get(i);
      out.print("<tr>" +
                "<td>" + acm.getRole().toString() + "</td>" +
                "<td>" + getAvailabilityString(acm.isAvailable()) + "</td>" +
                "<td>" + new Date(acm.getTimeSpan().getStartTime()).toString() + "</td>" +
                "<td>" + new Date(acm.getTimeSpan().getEndTime()).toString() + "</td>" +
                "<td>" + "<input type=checkbox name=\""+ CLEAR
                + "\" value=\""+ i + "\">Clear" + "</td>" +
                "</tr>");
    }
    out.print("<tr><td colspan=5 align=right><input type=submit name=\"" + ACTION_PARAM + "\" value=\""
              + CLEAR_SELECTED_ROWS + "\">\n");
    out.println("</td></tr>");
    out.print("</table>");
    out.print("<p><input type=submit name=\"" + ACTION_PARAM + "\" value=\"" + PUBLISH + "\">\n");
    out.print("</form>");
  }

  private void printChangeInfo(Collection messages) {
    out.println("<p>\n");
    out.println("Successfully posted availability change:");
    for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
      AvailabilityChangeMessage message = (AvailabilityChangeMessage) iterator.next();
      out.println("<ul><li>Role: " + message.getRole() +
                  "<li>Available: " + getAvailabilityString(message.isAvailable()) +
                  "<li>Start Date: " + new Date(message.getTimeSpan().getStartTime()) +
                  "<li>End Date: " + new Date(message.getTimeSpan().getEndTime()) +
                  "<li>Registry Updated: " + message.isRegistryUpdated() +
                  "</ul>");
    }

    out.print("<form method=\"GET\" action=\"" +
              request.getRequestURI() +
              "\">\n" +
              "<p><input type=submit name=\"" + ACTION_PARAM + "\" value=\"" + BACK + "\">\n" +
              "</form>\n");

  }

  private void printError(String text) {
    out.println("<p>\n" +
                "<b>" + text + "</b>" +
                "<p>");
  }

  private String getAvailabilityString(boolean available) {
    if (available == true) {
      return "Available";
    }
    else return "Unavailable";
  }

  private boolean hasServiceContract(Role role) {
    blackboard.openTransaction();
    Collection serviceContracts = blackboard.query(serviceContractRelayPred);
    blackboard.closeTransaction();
    for (Iterator iterator = serviceContracts.iterator(); iterator.hasNext();) {
      ServiceContractRelay scr = (ServiceContractRelay) iterator.next();
      if (role.equals(scr.getServiceContract().getServiceRole())) {
        return true;
      }
    }
    return false;
  }

  protected static UnaryPredicate capabilitiesPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ProviderCapabilities);
    }
  };

  private UnaryPredicate serviceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ServiceContractRelay) {
        ServiceContractRelay relay = (ServiceContractRelay) o;
        return (relay.getProviderName().equals(getEncodedAgentName()));
      } else {
        return false;
      }
    }
  };

  public Collection getMyCapabilities() {
    blackboard.openTransaction();
    Collection capCollect = blackboard.query(capabilitiesPred);
    blackboard.closeTransaction();
    if (capCollect.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    // I think there should only be one
    if (capCollect.size() > 1) {
      if (logger.isErrorEnabled())
        logger.error("There is more than one ProviderCapability object: " + capCollect.size());
    }
    ProviderCapabilities pc = (ProviderCapabilities) capCollect.iterator().next();
    Collection capabilites = pc.getCapabilities();
    return capabilites;
  }
}
