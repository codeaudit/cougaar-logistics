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


import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Verb;


public class AllocResultsServlet extends BaseServletComponent implements BlackboardClient {
 
  private ArrayList supplyTasks = new ArrayList();
  private ArrayList projectSupplyTasks = new ArrayList();
  protected MessageAddress agentId;
  protected BlackboardService blackboard;

  public static final String SUPPLY = "SUPPLY Tasks";
  public static final String PROJECTSUPPLY = "Project Supply Tasks";
  public static final String PHASES = "Phases";
  public static final String Results = "Results";
  

  /** A zero-argument constructor is required for dynamically loaded PSPs,
      required by Class.newInstance()
  **/
  public AllocResultsServlet() {
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

  public final void setAgentIdentificationService(AgentIdentificationService ais) {
    MessageAddress an;
    if ((ais != null) &&
        ((an = ais.getMessageAddress()) instanceof MessageAddress)) {
      this.agentId = (MessageAddress) an;
    } else {
      // FIXME: Log something?
    }
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
    return "/AllocationResults";
  }

  protected MessageAddress getAgentID() {
    return agentId;
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
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


  protected void displayResults(PrintWriter out) {
    try {
      blackboard.openTransaction();
      supplyTasks = new ArrayList(blackboard.query(taskPred("Supply")));
      projectSupplyTasks = new ArrayList(blackboard.query(taskPred("ProjectSupply")));
      blackboard.closeTransaction();
      
      printHtmlBegin(out);

      out.println("<p align=\"center\"><b><font size=\"5\">ALLOCATION RESULTS: SUPPLY TASKS</font></b></p>");
       out.println("<table width=\"100%\" border=\"1\">");
       //table headers
       out.println("<tr>");  
       out.println("<td>" + "Task UID" + "</td>");
       out.println("<td>" + "Phased" + "</td>");
       //out.println("<td>" + "# of Phases" + "</td>");
       out.println("<td>" + "Results" + "</td>");
       out.println("<td>" + "Confidence" + "</td>");
       out.println("<td>" + "IsSuccess" + "</td>");
       out.println("</tr>");  

      for (Iterator iterator = supplyTasks.iterator();
           iterator.hasNext();) {
        Task sTask = (Task)iterator.next();
        PlanElement pe = sTask.getPlanElement();
        if (pe != null) {
          AllocationResult estResult = pe.getEstimatedResult();
          AllocationResult repResult = pe.getReportedResult();
          if (estResult != null) {
            out.println("<tr>");  
            out.println("<td>" + sTask.getUID() + "</td>");
            out.println("<td>" + estResult.isPhased() + "</td>");
            // out.println("<td>" + phasedResults.size() + "</td>");
            out.println("<td>" + "EstimatedResults=" + parseResult(estResult) + "</td>");
            out.println("<td>" + estResult.getConfidenceRating() + "</td>");
            out.println("<td>" + estResult.isSuccess() + "</td>");
            out.println("</tr>");  
          }
          
          if (repResult != null) {
            out.println("<tr>");  
            out.println("<td>" + sTask.getUID() + "</td>");
            out.println("<td>" + repResult.isPhased() + "</td>");
            //out.println("<td>" + repPhasedResults.size() + "</td>");
            out.println("<td>" + "ReportedResults=" + parseResult(repResult) + "</td>");
            out.println("<td>" + repResult.getConfidenceRating() + "</td>");
            out.println("<td>" + repResult.isSuccess() + "</td>");
            out.println("</tr>"); 
          }

        }
      }

      out.println("</table>");
      printHtmlEnd(out);
      out.flush();

      //BEGIN PROJECTSUPPLY

      out.println("<p align=\"center\"><b><font size=\"5\">ALLOCATION RESULTS: PROJECT SUPPLY TASKS</font></b></p>");
       out.println("<table width=\"100%\" border=\"1\">");
       //table headers
       out.println("<tr>");  
       out.println("<td>" + "Task UID" + "</td>");
       out.println("<td>" + "Phased" + "</td>");
       //out.println("<td>" + "# of Phases" + "</td>");
       out.println("<td>" + "Results" + "</td>");
       out.println("<td>" + "Confidence" + "</td>");
       out.println("<td>" + "IsSuccess" + "</td>");
       out.println("</tr>");  

      for (Iterator iterator = projectSupplyTasks.iterator();
           iterator.hasNext();) {
        Task psTask = (Task)iterator.next();
        PlanElement pspe = psTask.getPlanElement();
        if (pspe != null) {
          AllocationResult estResult = pspe.getEstimatedResult();
          AllocationResult repResult = pspe.getReportedResult();
          
          if (estResult != null) {
            out.println("<tr>");  
            out.println("<td>" + psTask.getUID() + "</td>");
            out.println("<td>" + estResult.isPhased() + "</td>");
            // out.println("<td>" + phasedResults.size() + "</td>");
            out.println("<td>" + "EstimatedResults=" + parseResult(estResult) + "</td>");
            out.println("<td>" + estResult.getConfidenceRating() + "</td>");
            out.println("<td>" + estResult.isSuccess() + "</td>");
            out.println("</tr>");  
          }

          if (repResult != null) {
            out.println("<tr>");  
            out.println("<td>" + psTask.getUID() + "</td>");
            out.println("<td>" + repResult.isPhased() + "</td>");
            //out.println("<td>" + repPhasedResults.size() + "</td>");
            out.println("<td>" + "ReportedResults=" + parseResult(repResult) + "</td>");
            out.println("<td>" + repResult.getConfidenceRating() + "</td>");
            out.println("<td>" + repResult.isSuccess() + "</td>");
            out.println("</tr>"); 
          }

        }
      }

      out.println("</table>");
      printHtmlEnd(out);
      out.flush();

    } catch (Exception e) {
      out.println("</table>");
      printHtmlEnd(out);
      e.printStackTrace();
    }
  }


  protected void printHtmlBegin(PrintWriter out) {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Allocation Results Servlet</title>");
    out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    out.println("</head>");
    out.println("<body bgcolor=\"#FFFFFF\">");
  }

  protected void printHtmlEnd(PrintWriter out)
  {
    out.println("</body>");
    out.println("</html>");
  }

  private String parseResult(AllocationResult ar) {
    ArrayList phasedResults;
    String printableResults = "[";
    if (ar.isPhased()) {
      phasedResults = (ArrayList)ar.getPhasedAspectValueResults();
    } else {
      phasedResults = new ArrayList(1);
      phasedResults.add(ar.getAspectValueResults());
    }
    Iterator it = phasedResults.iterator();
    while (it.hasNext()) {
      Object obj = it.next();
      if (obj instanceof AspectValue) {
        AspectValue av = (AspectValue) obj;
        printableResults = printableResults + "{" + av.getAspectType() + " , " + av.getValue() + "} ";
      } else if (obj instanceof AspectValue[]) {
        AspectValue[] avs = (AspectValue[]) obj;
        for (int i=0; i < avs.length; i++) {
          AspectValue anAV = (AspectValue) avs[i];
          printableResults = printableResults + "{" + anAV.getAspectType() + " , " + anAV.getValue() + "} ";
        }
      } else if (obj instanceof Collection) {
        ArrayList alist = (ArrayList)obj;
        Iterator listIt = alist.iterator();
        while (listIt.hasNext()) {
          AspectValue av = (AspectValue) listIt.next();
          printableResults = printableResults + "{" + av.getAspectType() + " , " + av.getValue() + "} ";
        }
      } else {
        System.out.println("AllocResults servlet got unexpected object: " + obj);
      }
    }
    printableResults = printableResults + "]";
    return printableResults;
  }


  protected UnaryPredicate taskPred(String verbString) {
    final String vString = verbString;
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Task) {
          Task task = (Task) o;
          if (task.getVerb().equals(Verb.getVerb(vString))) {
            return true;
          }
        } 
        return false;
      }
    };
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
        
        try {
            displayResults(out);
        } catch (Exception topLevelException) {
          displayExceptionFailure(out, topLevelException);
        }
      } 

    }
  }

    
} // end of class


