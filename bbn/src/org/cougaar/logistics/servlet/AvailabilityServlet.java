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

import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.description.AvailabilityChangeMessage;
import org.cougaar.servicediscovery.description.ProviderCapabilities;
import org.cougaar.servicediscovery.description.ProviderCapability;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.UnaryPredicate;

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
  private List registeredRoles;
  private List rolesWithClients;
  private static String PATH = "/availabilityServlet";
  private static String ROLE_PARAMETER = "role";
  private static String START_DATE_PARAMETER = "startdate";
  private static String END_DATE_PARAMETER = "enddate";
  private Organization myOrg;

  public void init() {
    // get the blackboard service
    blackboard = (BlackboardService) serviceBroker.getService(this,
                                                              BlackboardService.class, null);
    if (blackboard == null) {
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

  // BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // BlackboardClient method:
  public long currentTimeMillis() {
    return new ExecutionTimer().currentTimeMillis();
  }

  UnaryPredicate availabilityPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof AvailabilityChangeMessage;  //ProviderRoleStatus;
    }
  };

  public void doGet(HttpServletRequest req,
                    HttpServletResponse res) throws IOException {

    Organization myOrg = getMyOrganization();
    registeredRoles = getProviderRoles();
    PrintWriter out = res.getWriter();
    Collection col;
    String start_date_string = req.getParameter(START_DATE_PARAMETER);
    String end_date_string = req.getParameter(END_DATE_PARAMETER);
    String role = req.getParameter(ROLE_PARAMETER);
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
    printTitles(out);
    printInfo(now, out);
    try {
      blackboard.openTransaction();
      col = blackboard.query(availabilityPred);

      if (!col.isEmpty()) {
        Iterator it = col.iterator();
        AvailabilityChangeMessage message = (AvailabilityChangeMessage) it.next();
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
          AvailabilityChangeMessage message =
              new AvailabilityChangeMessage(Role.getRole(role), false, new MutableTimeSpan(), false);
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

  private void printTitles(PrintWriter out) {
    String title = "Availability  Servlet";
    out.println("<HEAD><TITLE>" + title + "</TITLE></HEAD>" +
                "<BODY><H1>" + title + "</H1>");
  }

  private void printInfo(long now, PrintWriter out) {
    String localAgent = getEncodedAgentName();
    out.println("<p>\n" +
                "Agent: " + localAgent +
                "<p>\n" +
                "Current Society Time: " + new Date(now));
  }

  private void printForm(HttpServletRequest req, PrintWriter out) {
    myOrg = getMyOrganization();
    String optionString = "";

    out.print("<p>\n" + "<table border>" + "<tr><th colspan=1>" + "Registered Roles" + "</th>" + "<th colspan=1>"
              + "Clients" + "</th></tr>");
    Iterator iter = registeredRoles.iterator();
    while (iter.hasNext()) {
      Role role = (Role) iter.next();
      String client = "No";
      if (hasClientRelationship(role)) {
        client = "Yes";
      }
      out.print("<tr><td>" + role + "</td><td align=center>" + client + "</td></tr>");
      optionString = optionString.concat(
          "<option value=\"" + role + "\">" + role + "</option>" + "\n");
    }
    out.print("</table>");

    out.print("<form method=\"GET\" action=\"" +
              req.getRequestURI() +
              "\">\n" +
              "Enter role: " +
              "<select size=1 name=\"" + ROLE_PARAMETER + "\">\n" +
              optionString +
              "</select>" +
              "<p>Enter new start date (MM/DD/YYYY): " +
              "<input type=\"text\" name=\"" + START_DATE_PARAMETER + "\" size=40>\n" +
              "</select>" +
              "<p>Enter new end date (MM/DD/YYYY): " +
              "<input type=\"text\" name=\"" + END_DATE_PARAMETER + "\" size=40>\n" +
              "<p><input type=\"submit\" value=\"Submit\">\n" +
              "</form>\n");
  }

  private void printChangeInfo(AvailabilityChangeMessage message, boolean newChange, PrintWriter out) {
    out.println("<p>\n");
    if (newChange) {
      out.println("Successfully posted availability change:");
    } else {
      out.println("Availability already changed:");
    }
    out.println("<ul><li>Role: " + message.getRole() +
                "<li>Available: " + message.isAvailable() +
                "<li>Start Date: " + new Date(message.getTimeSpan().getStartTime()) +
                "<li>End Date: " + new Date(message.getTimeSpan().getEndTime()) +
                "<li>Registry Updated: " + message.isRegistryUpdated() +
                "</ul>");
  }

  private void printError(String text, PrintWriter out) {
    out.println("<p>\n" +
                "<b>" + text + "</b>" +
                "<p>");
  }

  private String headWithTitle(String title) {
    return "<HEAD><TITLE>" + title + "</TITLE></HEAD>";
  }

  private List getAllRelationshipRoles(Collection roles) {
    RelationshipSchedule relSched = myOrg.getRelationshipSchedule();
    ArrayList list = new ArrayList();
    for (Iterator iterator = roles.iterator(); iterator.hasNext();) {
      Role role = (Role) iterator.next();
      Collection relationships = relSched.getMatchingRelationships(role);
      Iterator rit = relationships.iterator();
      while (rit.hasNext()) {
        Relationship r = (Relationship) rit.next();
        HasRelationships hr = relSched.getOther(r);
        //HasRelationships hr = r.getA();
        if (hr instanceof Organization) {
          Role cr = relSched.getOtherRole(r);
          //Role role = relSched.getMyRole(r);
          //RelationshipSchedule rs = hr.getRelationshipSchedule();
          list.add(cr);
          //roles.add(rs.getMyRole(r));
        }
      }
    }

    return list;
  }

  private boolean hasClientRelationship(Role role) {
    RelationshipSchedule relSched = myOrg.getRelationshipSchedule();
    Collection relationships = relSched.getMatchingRelationships(role);
    return !relationships.isEmpty();
  }

  protected Organization getMyOrganization(Iterator orgs) {
    Organization myOrg = null;
    // look for this organization
    if (orgs.hasNext()) {
      myOrg = (Organization) orgs.next();
    }
    return myOrg;
  }

  protected static UnaryPredicate orgsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        return ((Organization) o).isSelf();
      }
      return false;
    }
  };

  protected static UnaryPredicate capabilitiesPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ProviderCapabilities);
    }
  };

  public Organization getMyOrganization() {
    blackboard.openTransaction();
    Collection orgsCollection = blackboard.query(orgsPredicate);
    blackboard.closeTransaction();
    return getMyOrganization(orgsCollection.iterator());
  }

  public Collection getMyCapabilities() {
    blackboard.openTransaction();
    Collection capCollect = blackboard.query(capabilitiesPred);
    blackboard.closeTransaction();
    if (capCollect.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    // I think there should only be one
    if (capCollect.size() > 1) {
      //TODO:  add a logger
      System.out.println("There is more than one ProviderCapability object: " + capCollect.size());
    }
    ProviderCapabilities pc = (ProviderCapabilities) capCollect.iterator().next();
    Collection capabilites = pc.getCapabilities();
    return capabilites;
  }
}



