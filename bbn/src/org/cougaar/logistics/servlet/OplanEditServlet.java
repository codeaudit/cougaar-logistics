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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.oplan.OplanCoupon;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.oplan.TimeSpan;

public class OplanEditServlet extends BaseServletComponent implements BlackboardClient {
  private static Comparator orgActivityComparator;
  private static Comparator timeSpanComparator;

  static {
    orgActivityComparator = new OrgActivityComparator();
    timeSpanComparator = new TimeSpanComparator();
  }

  private Oplan oplan = null;

  private ArrayList orgActivities = new ArrayList();
  
  private HashMap modifiedOrgActivities = new HashMap();
  private HashSet changedOplans = new HashSet();


  protected ClusterIdentifier agentId;

  protected BlackboardService blackboard;

  /**
   * Used to convert day offsets from dates to Date and back to offsets
   */
  private static Calendar formatter;
  static {
    formatter = Calendar.getInstance();
  }


  public static final String ACTION_PARAM = "action";
  public static final String SHOW_OPLAN = "show_oplan";
  public static final String EDIT_ORGACTIVITY = "edit_orgactivity";
  public static final String CHANGE_ORGACTIVITY = "change_orgactivity";
  public static final String PUBLISH = "Publish";
  public static final String REFRESH = "Refresh";
  
  public static final String PARAM_SEPARATOR = "&";
  public static final String OPTEMPO = "optempo";
  public static final String ORG_ID = "org_ID";
  public static final String ORG_UID = "org_UID";
  public static final String START_OFFSET = "start_offset";
  public static final String END_OFFSET = "end_offset";


  /** A zero-argument constructor is required for dynamically loaded PSPs,
      required by Class.newInstance()
  **/
  public OplanEditServlet() {
    super();
  }

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  // aquire services:
  public void load() {
    // FIXME need AgentIdentificationService
    org.cougaar.core.plugin.PluginBindingSite pbs =
      (org.cougaar.core.plugin.PluginBindingSite) bindingSite;
    this.agentId = pbs.getAgentIdentifier();

    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
  }

  protected String getPath() {
    return "/editOplan";
  }

  protected ClusterIdentifier getAgentID() {
    return agentId;
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }


  /**
   * Does lots of "new String" allocs for no good reason.
   * Odd postData format -- should allow both postdata and
   *   URL parameter line (ala PSP_PluginLoader).
   */
  protected boolean changeOrg(OrgActivity orgActivity, HttpServletRequest request) {
    String startOffset;
    String endOffset;
    String select = " ";
    OrgActivity origOrgActivity = orgActivity;
    ChangeInfo changeInfo = (ChangeInfo) modifiedOrgActivities.get(orgActivity.getUID());

    // Make sure we change the  copy so we can refresh if required.
    // Don't want our changes on the blackboard until we publish
    if (changeInfo == null) {
      // Keep orig and current set of changes for publish 
      changeInfo = new ChangeInfo(orgActivity);
      modifiedOrgActivities.put(orgActivity.getUID(), changeInfo);

      // replace entry in orgActivities so we display the changes
      // assumes that modifications do not change the sort order 
      OrgActivity copyOrgActivity = (OrgActivity) orgActivity.clone();
      int index = orgActivities.indexOf(orgActivity);
      if (index >= 0) {
        orgActivities.set(index, copyOrgActivity);
      } else {
        throw new IllegalArgumentException("OrgActivity - " + 
                                           orgActivity + 
                                           " - not found in current set of OrgActivities.");
      }

      // Work with copy from now on
      System.out.println("Made copy : " + copyOrgActivity + " of " + orgActivity);
      orgActivity = copyOrgActivity;
    } 

    select = request.getParameter(OPTEMPO);
    startOffset = request.getParameter(START_OFFSET);
    endOffset = request.getParameter(END_OFFSET);
    
    if ((select != null) && (select.length() > 0)) {
      orgActivity.setOpTempo(select);
    }

    Date startDate;
    if ((startOffset != null) && (startOffset.length() > 0)) {
      startDate = getRelativeDate(oplan.getCday(), startOffset);
    } else {
      startDate = orgActivity.getTimeSpan().getStartDate();
    }

    Date endDate;
    if ((endOffset != null) && (endOffset.length() > 0)) {
      endDate = getRelativeDate(oplan.getCday(), endOffset);
    } else {
      endDate = orgActivity.getTimeSpan().getEndDate();
    }

    // Make sure start is before end
    if (! startDate.before(endDate)) {
      return false;
    } 

    orgActivity.getTimeSpan().setStartDate(startDate);
    orgActivity.getTimeSpan().setEndDate(endDate);

    // Save changes to changeInfo if we need to publish later on
    changeInfo.setTimeSpan(orgActivity.getTimeSpan());
    changeInfo.setOpTempo(orgActivity.getOpTempo());

    System.out.println("Orig: " + origOrgActivity.getOpTempo() + " " + origOrgActivity.getTimeSpan());
    System.out.println("Modified: " + orgActivity.getOpTempo() + " " + orgActivity.getTimeSpan());
    return true;
  }

  protected void displayOplanNotFound(PrintWriter out) {
    printHtmlBegin(out);
    out.println("<font size=+1>Oplan not found</font>");
    printHtmlEnd(out);
  }

  protected void displayExceptionFailure(PrintWriter out, Exception e) {
    printHtmlBegin(out);
    out.println("<font size=+1>Failed due to Exception</font><p>");
    out.println(e);
    out.println("<p><pre>");
    e.printStackTrace(out);
    out.println("</pre>");
    printHtmlEnd(out);
  }

  protected void displayPostSuccess(PrintWriter out) {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Untitled Document</title>");
    out.print("<meta http-equiv=\"refresh\" content=\" 1;URL=");
    out.print("/$" + getAgentID() + getPath());
    out.print("\">");
    out.println("</head>");
    out.println("<body bgcolor=\"#FFFFFF\">");
    out.println("<H3><CENTER>Submit Successful !!</CENTER></H3>");
    printHtmlEnd(out);
  }

  protected void displayPostFailure(PrintWriter out) {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Untitled Document</title>");
    out.print("<meta http-equiv=\"refresh\" content=\" 2;URL=");
    out.print("/$" + getAgentID() + getPath());
    out.println("\">");
    out.println("</head>");
    out.println("<body bgcolor=\"#FFFFFF\">");
    out.println("<H3><CENTER> !!ERROR !! Start Date is bigger than End Date</CENTER></H3>");
    printHtmlEnd(out);
  }

  protected void displayOplanEdit(
      PrintWriter out, boolean refresh) {
    try {
      if ((orgActivities.size() == 0) || (refresh == true)) {
        System.out.println("Refreshing org activities");
        blackboard.openTransaction();
        orgActivities = new ArrayList(blackboard.query(orgActivitiesForOplanPred(oplan)));
        blackboard.closeTransaction();

        Collections.sort(orgActivities, orgActivityComparator);
      }

      printHtmlBegin(out);

      out.println("<p align=\"center\"><b><font size=\"5\">OPLAN EDITOR</font></b></p>");
      printPublishChanges(out);

      out.println("<table width=\"100%\" border=\"1\">");
      out.println("<tr>");
      out.println("<td><b>Organization</b></td>");
      out.println("</tr>");

      String previousOrgID = " ";

      for (Iterator iterator = orgActivities.iterator();
           iterator.hasNext();) {
        OrgActivity org = (OrgActivity)iterator.next();

        if (!(org.getOrgID().equals(previousOrgID))) {
          previousOrgID = org.getOrgID();
          out.println("</tr>");
          out.println("<td>" + org.getOrgID() +"</td>");
        }
          
        out.println("<td><a href=\"/$" +
                    getAgentID() + getPath() + "?" + ACTION_PARAM + "=" + EDIT_ORGACTIVITY + PARAM_SEPARATOR +
                    ORG_ID + "="+ org.getOrgID() + PARAM_SEPARATOR +
                    ORG_UID + "=" + org.getUID() + "\">" + 
                    org.getActivityType() + 
" C+" +
                    getRelativeOffsetDays(oplan.getCday(), org.getTimeSpan().getStartDate()) +
                    " To C+" +
                    getRelativeOffsetDays(oplan.getCday(), org.getTimeSpan().getEndDate()) +
                    "</a></td>");
      }

      out.println("</table>");
      printHtmlEnd(out);
      out.flush();
    } catch (Exception e) {
      out.println("</table>");
      printHtmlEnd(out);
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  protected void displayOrgNotFound(PrintWriter out, String orgID, String orgUID)
  {
    printHtmlBegin(out);
    out.println("<font size=+1>OrgActivity not found:<p><ul>");
    if (orgID != null) {
      out.print("<li><b>OrgID</b>: ");
      out.print(orgID);
      out.println("</li>");
    }
      out.print("<li><b>UID</b>: ");
      out.print(orgUID);
      out.println("</li>");
    out.println("</ul>");
    printHtmlEnd(out);
  }

  protected void displayOrgActivity(
      PrintWriter out, OrgActivity org)
  {
    printHtmlBegin(out);
    out.println("<p align=\"center\"><b><font size=\"5\">"+
      org.getOrgID() +"</font></b></p>");
    out.println("<form method=\"get\" action=\"/$" + getAgentID() + getPath() + 
                "\">");
    out.println("<table width=\"90%\" border=\"1\">");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\"><b>Activity Type</b></td>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;" +
      org.getActivityType() + "</td>");
    if (org.getActivityName() == null) {
      out.println("<td colspan=\"2\">&nbsp;" + 
        "&nbsp" + "</td>");
    } else {
      out.println("<td colspan=\"2\">&nbsp;" +
        org.getActivityName()+ "</td>");
    }
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\"><b>OpTempo</b></td>");
    out.println("<td colspan=\"2\">");
 
    printHtmlSelectOption(out, org.getOpTempo() );
 
    out.println("</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"4\">&nbsp;</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"4\"><b>TimeSpan</b></td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("<td><b>Start Time</b></td>");
    out.println("<td><b>End Time</b></td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td>&nbsp; </td>");
    out.println("<td>");
    out.println("<div align=\"right\"><b>C-Time</b></div>");
    out.println("</td>");
    out.println("<td>C+ <input type=\"text\" name=\"" + START_OFFSET + "\" value=\"" + 
      getRelativeOffsetDays(oplan.getCday(), org.getTimeSpan().getStartDate()) +
      "\" size=\"6\"> </td>");
    out.println("<td>C+ <input type=\"text\" name=\"" + END_OFFSET + "\" value=\""+ 
      getRelativeOffsetDays(oplan.getCday(), org.getTimeSpan().getEndDate()) +
      "\" size=\"6\"></td>");
 
    out.println("<tr>");
    out.println("<td>&nbsp;</td>");
    out.println("<td>");
    out.println("<div align=\"right\"><b>Absolute Time</b></div>");
    out.println("</td>");
    out.println("<td>" +
      org.getTimeSpan().getStartDate().toGMTString() + "</td>");
    out.println("<td>" +
      org.getTimeSpan().getEndDate().toGMTString() + "</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("<td>&nbsp;</td>");
    out.println("<td>&nbsp;</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\"><b>Geographic Location</b></td>");
    out.println("<td>&nbsp;</td>");
    out.println("<td>&nbsp;</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("<td><b>Name</b></td>");
    out.println("<td>" + org.getGeoLoc().getName() +"</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("<td><b>Code</b></td>");
    out.println("<td>"+ org.getGeoLoc().getGeolocCode() +"</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("<td><b>Latitude</b></td>");
    out.println("<td>"+org.getGeoLoc().getLatitude().getDegrees()+"</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<td colspan=\"2\">&nbsp;</td>");
    out.println("<td><b>Longitude</b></td>");
    out.println("<td>"+org.getGeoLoc().getLongitude().getDegrees()+"</td>");
    out.println("</tr>");
 
    out.println("<tr>");
    out.println("<input type=\"hidden\" name=\"" + ACTION_PARAM +
                "\" value=\"" + CHANGE_ORGACTIVITY + "\">\n");
    out.println("<input type=\"hidden\" name=\"" + ORG_ID +
                "\" value=\"" + org.getOrgID() + "\">\n");
    out.println("<input type=\"hidden\" name=\"" + ORG_UID +
                "\" value=\"" + org.getUID() + "\">\n");
    out.println("<td colspan=\"4\">");
    out.println("<div align=\"center\">");
    out.println("<input type=\"submit\" name=\"save_changes\" value=\"Save\">\n");
    out.println("</div>");
    out.println("</td>");
    out.println("</tr>");
    
    out.println("</table>");
    out.println("</form>");
    out.println("<p>&nbsp;</p>");
 
    printHtmlEnd(out);
  }
  

  protected void printHtmlBegin(PrintWriter out) {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Oplan Editor</title>");
    out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    out.println("</head>");
    out.println("<body bgcolor=\"#FFFFFF\">");
  }

  protected void printHtmlEnd(PrintWriter out)
  {
    out.println("</body>");
    out.println("</html>");
  }

  protected void printPublishChanges(PrintWriter out)
  {
    out.println("<form method=\"get\" action=\"/$" + getAgentID() + getPath() + "\">");
    out.println("<tr>");
    out.println("<td colspan=\"4\">");
    out.println("<div align=\"center\">");
    out.println("<input type=\"submit\" name=\"" + ACTION_PARAM +
                "\" value=\"" + PUBLISH + "\">\n");
    out.println("<input type=\"submit\" name=\""  + ACTION_PARAM +
                "\" value=\"" + REFRESH + "\">\n");
    out.println("</div>");
    out.println("</td>");
    out.println("</tr>");
    out.println("</form>");
  }

  protected void printHtmlSelectOption( PrintWriter out, String opTempoValue)
  {
    if( opTempoValue != null) {
      out.println("<select name=\"" + OPTEMPO + "\">");
      if (opTempoValue.equals("High")) {
        out.println("<option value=\"High\" selected>High</option>");
        out.println("<option value=\"Medium\">Medium</option>");
        out.println("<option value=\"Low\">Low</option>");
      } else if (opTempoValue.equals("Medium")) {
        out.println("<option value=\"High\">High</option>");
        out.println("<option value=\"Medium\" selected>Medium</option>");
        out.println("<option value=\"Low\">Low</option>");
      } else {
        out.println("<option value=\"High\">High</option>");
        out.println("<option value=\"Medium\">Medium</option>");
        out.println("<option value=\"Low\" selected>Low</option>");
      }
      out.println("</select>");
    } else {
      out.println("<td>&nbsp;</td>");
    }
  }

  protected void publish(PrintWriter out) {
    blackboard.openTransaction();
    
    Collection changes = modifiedOrgActivities.values();

    for (Iterator iterator = changes.iterator();
         iterator.hasNext();) {
      ChangeInfo changeInfo = (ChangeInfo) iterator.next();
      OrgActivity orgActivity = changeInfo.getOrgActivity();
      
      // Apply changes
      orgActivity.setOpTempo(changeInfo.getOpTempo());
      orgActivity.getTimeSpan().setStartDate(changeInfo.getTimeSpan().getStartDate());
      orgActivity.getTimeSpan().setEndDate(changeInfo.getTimeSpan().getEndDate());

      boolean status = blackboard.publishChange(orgActivity);
      System.out.println("Publish status of " + status + " for " + orgActivity);
      System.out.println(orgActivity.getUID() + " " + orgActivity.getOpTempo() + " " + 
                         orgActivity.getTimeSpan().getStartDate() + " " +
                         orgActivity.getTimeSpan().getEndDate());
    }
    /*
     * KLUDGE - Don't publish the coupon because the GLSInitServlet will
     * do it for us.
     Collection coupons = 
     blackboard.query(new CouponPredicate(plan.getUID())); 
     
     for (Iterator couponIt = coupons.iterator(); couponIt.hasNext();) {
     System.out.println("OplanEditServlet: publishChanging OplanCoupon");
     blackboard.publishChange(couponIt.next());
     }
    */
    
    displayPostSuccess(out);
    
    blackboard.closeTransactionDontReset();
    modifiedOrgActivities.clear();
    orgActivities.clear();
    
    displayOplanEdit(out, true);
  }

  protected UnaryPredicate orgActivitiesForOplanPred(final Oplan oplan) {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof OrgActivity) {
          OrgActivity orgActivity = (OrgActivity) o;
          return orgActivity.getOplanUID().equals(oplan.getUID());
        } else {
          return false;
        }
      }
    };
  }
      

  protected OrgActivity findOrg(UID orgUID) {

    for (Iterator iterator = orgActivities.iterator();
         iterator.hasNext();) {
      OrgActivity next = (OrgActivity) iterator.next();
      if (next.getUID().equals(orgUID)) {
        return next;
      }
    }

    System.out.println("Should never be trying to get specific org " +
                       "before initializing OrgActivities");
    return null;
  }

  private static class OrgActivityComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1 == null) {
        if (o2 == null) {
          return 0;
        } else {
          return -1;
        }
      } else if (o2 == null) {
        return 1;
      } else if (o1.equals(o2)) {
        return 0;
      }

      OrgActivity orgActivity1 = (OrgActivity) o1;
      OrgActivity orgActivity2 = (OrgActivity) o2;
      
      String orgID1 = orgActivity1.getOrgID();
      String orgID2 = orgActivity2.getOrgID();

      int idCompare = orgID1.compareTo(orgID2);

      if (idCompare != 0) {
        return idCompare;
      } else  {
        return timeSpanComparator.compare(orgActivity1, orgActivity2);
      }
    }
  }

  private static class TimeSpanComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1 == null) {
        if (o2 == null) {
          return 0;
        } else {
          return -1;
        }
      } else if (o2 == null) {
        return 1;
      } else if (o1.equals(o2)) {
        return 0;
      }

      TimeSpan timeSpan1 = ((OrgActivity) o1).getTimeSpan();
      TimeSpan timeSpan2 = ((OrgActivity) o2).getTimeSpan();
      
      long diff = timeSpan1.getStartTime() - timeSpan2.getStartTime();

      // Compare starts
      if (diff > 0L) {
        return 1;
      } else if (diff < 0L) {
        return -1;
      } 

      // Compare ends
      diff = timeSpan1.getEndTime() - timeSpan2.getEndTime();
      if (diff > 0L) {
        return 1;
      } else if (diff < 0L) {
        return -1;
      } 

      // Start and end are equal
      return System.identityHashCode(timeSpan1) - 
        System.identityHashCode(timeSpan2);
    }
  }

  private static class CouponPredicate implements UnaryPredicate {
    UID _oplanUID;
    public CouponPredicate(UID oplanUID) {
      _oplanUID = oplanUID;
    }
    public boolean execute(Object o) {
      if (o instanceof OplanCoupon) {
	if (((OplanCoupon ) o).getOplanUID().equals(_oplanUID)) {
	  return true;
	}
      }
      return false;
    }
  }

  protected void initOplan(PrintWriter out) {
    // get the oplan and cdate
    blackboard.openTransaction();
    Collection oplans = blackboard.query(allOplansPredicate);
    blackboard.closeTransaction();

    if ((oplans == null) || (oplans.size() ==0)) {
      displayOplanNotFound(out);
      return;
    }
    
    
    // No provision for handling multiple oplans so pick the first one
    for (Iterator iterator = oplans.iterator(); iterator.hasNext();) {
      oplan = (Oplan) iterator.next();
      break;
    }
    
    // Reset all existing oplan related info.
    orgActivities.clear();
    modifiedOrgActivities.clear();
  }
  
  // Predicate for all Oplans
  private static UnaryPredicate allOplansPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {          
      return (o instanceof Oplan);
    }
  };

  private static Date getRelativeDate(Date baseDate, int offsetDays) {
    formatter.setTime(baseDate);
    formatter.add(formatter.DATE, offsetDays);
    return formatter.getTime();
  }

  private static Date getRelativeDate(Date baseDate, String sOffsetDays) {
    return getRelativeDate(baseDate, Integer.parseInt(sOffsetDays));
  }

  private static int getRelativeOffsetDays(Date baseDate, Date offsetDate) {
    try {
      return (int)((offsetDate.getTime() - baseDate.getTime())/
                   (1000*60*60*24));
    } catch (Exception e) {
      return 0;
    }
  }

  private class MyServlet extends HttpServlet {
    
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse resp) throws IOException {
      MyWorker mw = new MyWorker(req, resp);
      mw.execute();
    }

    private class MyWorker {
      
      // from the "doGet(..)":
      private HttpServletRequest request;
      private HttpServletResponse response;
      
      public MyWorker(HttpServletRequest req, 
                      HttpServletResponse resp) {
        request = req;
        response = resp;
      }

      public void execute() throws IOException {
        PrintWriter out = response.getWriter();
        
        // make sure we have an oplan
        if (oplan == null) {
          initOplan(out);
        }
        
        String action = request.getParameter(ACTION_PARAM);

        try {
          // Default to displaying oplan editor
          if ((action == null) ||
              (action.equals(SHOW_OPLAN))) {
            displayOplanEdit(out, false);
          } else if (action.equals(EDIT_ORGACTIVITY)) {
            editOrgActivity(out);
          }  else if (action.equals(CHANGE_ORGACTIVITY)) {
            changeOrgActivity(out); 
          } else if (action.equals(PUBLISH)) { 
            publish(out);
          } else if (action.equals(REFRESH)) { 
            displayOplanEdit(out, true);
          }
        } catch (Exception topLevelException) {
          displayExceptionFailure(out, topLevelException);
        }
      } 

      protected void editOrgActivity(PrintWriter out) { 
        
        String orgID = request.getParameter(ORG_ID);
        String orgUID = request.getParameter(ORG_UID);
        
        if ((orgID == null) || (orgUID == null)) {
          System.err.println("Malformed " + EDIT_ORGACTIVITY + " request. " +
                             " Must include " + ORG_ID + " and " + ORG_UID + " values.");
          return;
        }
        
        OrgActivity orgActivity = findOrg(UID.toUID(orgUID));
        
        // display it
        if (orgActivity != null) {
          displayOrgActivity(out, orgActivity);
        } else {
          displayOrgNotFound(out, orgID, orgUID);
        }
      }
      
      protected void changeOrgActivity(PrintWriter out) {
        String orgID = request.getParameter(ORG_ID);
        String orgUID = request.getParameter(ORG_UID);

        if ((orgID == null) || (orgUID == null)) {
          System.err.println("Malformed " + CHANGE_ORGACTIVITY + " request. " +
                             " Must include " + ORG_ID + " and " + ORG_UID + " values.");

          return;
        }
        
        OrgActivity orgActivity = findOrg(UID.toUID(orgUID));
        
        if (changeOrg(orgActivity, request)) {
          displayPostSuccess(out);
        } else {
          displayPostFailure(out);
        }
      }
    }
  }

  private static class ChangeInfo {
    private OrgActivity myOrgActivity;
    private String myOpTempo;
    private TimeSpan myTimeSpan;

    public ChangeInfo(OrgActivity orgActivity, String opTempo, 
                      TimeSpan timeSpan) {
      myOrgActivity = orgActivity;
      myOpTempo = opTempo;
      myTimeSpan = timeSpan;
    }

    public ChangeInfo(OrgActivity orgActivity) {
      myOrgActivity = orgActivity;
    }

    public String getOpTempo() {
      return myOpTempo;
    }

    public void setOpTempo(String opTempo) {
      myOpTempo = opTempo;
    }

    public TimeSpan getTimeSpan() {
      return myTimeSpan;
    }
    
    public void setTimeSpan(TimeSpan timeSpan) {
      myTimeSpan = timeSpan;
    }

    public OrgActivity getOrgActivity() {
      return myOrgActivity;
    }
  }

    
} // end of class


