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

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

/**
 * This servlet publishes a CommStatus object to the agent's blackboard.
 *
 * commStatus?commUp=false&connectedAgentName=<AGENT NAME>
 * <AGENT NAME> refers to the agent that has lost communications
 */

public class CommStatusServlet extends BaseServletComponent implements BlackboardClient {
  private BlackboardService blackboard;
  private AlarmService alarmService;

  protected String getPath() {
    return "/" + getRelativePath();
  }

  protected String getRelativePath() {
    return "commStatus";
  }

  protected Servlet createServlet() {
    blackboard = (BlackboardService) serviceBroker.getService(this, BlackboardService.class, null);
    if (blackboard == null) {
      throw new RuntimeException("Unable to obtain blackboard service");
    }
    alarmService = (AlarmService) serviceBroker.getService(this, AlarmService.class, null);
    if (alarmService == null) {
      throw new RuntimeException("Unable to set alarm service");
    }
    return new MyServlet();
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  // BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  public long currentTimeMillis() {
    return alarmService.currentTimeMillis();
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    return false;
  }

  public void unload() {
    super.unload();
    // release the blackboard service
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, servletService);
      blackboard = null;
    }

    // release the alarm service
    if (alarmService != null) {
      serviceBroker.releaseService(this, AlarmService.class, servletService);
      alarmService = null;
    }
  }

  private class MyServlet extends HttpServlet {
    UnaryPredicate commStatusObjectsQuery = new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof CommStatus;
      }
    };

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
      execute(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
      execute(req, res);
    }

    private void errorMsg(String msg, PrintWriter pw) {
      pw.println("<BR>Error.  " + msg + "</body></html>");
      pw.flush();
    }

    public void execute(HttpServletRequest req, HttpServletResponse res) throws IOException {
      String connectedAgentName = req.getParameter("connectedAgentName");
      boolean commUp;
      CommStatus cs = null;

      //initialize the PrintWriter
      PrintWriter out = res.getWriter();
      out.println("<html><head></head><body>");

      //parse the commUp
      String commUpStr = req.getParameter("commUp");
      if (commUpStr != null)  {
        commUp = commUpStr.trim().equals("true");
      } else {
        errorMsg("commUpStr incorrectly specified: " + commUpStr, out);
        return;
      }

      if (connectedAgentName != null) {
        //iterate over the existing CommStatusObjects to see if one already exists
        blackboard.openTransaction();
        Collection col = blackboard.query(commStatusObjectsQuery);
        for (Iterator i = col.iterator(); i.hasNext();) {
          CommStatus temp = (CommStatus) i.next();
          if (temp.connectedAgentName.equals(connectedAgentName)) {
            cs = temp;
            System.out.println("\n CSS Found matching CommStatus object for connectedAgentName: " +
                               connectedAgentName);
            break;
          }
        }
        blackboard.closeTransaction();
      } else {
        errorMsg("ConnectedAgentName not specified", out);
        return;
      }

      if (cs == null) { //if an existing CommStatus object was not found then create one
        cs = new CommStatus(connectedAgentName);
        if (commUp == false) {
          cs.setCommLoss(currentTimeMillis());
          System.out.println("\n CSS Setting Comm LOSS for NEW CommStatus object for connectedAgentName: " +
                             connectedAgentName);
        } else {
          cs.setCommRestore(currentTimeMillis());
          System.out.println("\n CSS Setting Comm RESTORE for NEW CommStatus object for connectedAgentName: " +
                             connectedAgentName);
        }
        blackboard.openTransaction();
        blackboard.publishAdd(cs);
        blackboard.closeTransaction();
      } else {  //otherwise update the existing CommStatus object
        if (commUp == false) {
          cs.setCommLoss(currentTimeMillis());
          System.out.println("\n CSS Setting Comm LOSS for MATCHING CommStatus object for connectedAgentName: " +
                             connectedAgentName);
        } else {
          cs.setCommRestore(currentTimeMillis());
          System.out.println("\n CSS Setting Comm RESTORE for MATCHING CommStatus object for connectedAgentName: " +
                             connectedAgentName);
        }
        blackboard.openTransaction();
        blackboard.publishChange(cs);
        blackboard.closeTransaction();
      }

      out.println("<BR>Success. </body></html>");
      out.flush();
    }
  }
}