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
 
package org.cougaar.mlm.ui.psp.transit;

import java.io.*;
import java.net.*;
import java.util.*;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.lib.planserver.*;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.planning.servlet.data.completion.*;

/**
 * PSP to gather TOPS scheduling progress.
 *
 * <pre>
 *    % and number of tasks that are successfully planned to 100% confidence
 *    % and number of tasks that are not yet planned
 *    %, number, and a list of failed tasks, which we would then use to 
 *       create links to the TASK psp.
 *    parent task of each failed task (allows us to show failed task 
 *       chain...)
 *    optionally, date of last received task
 * </pre>
 */
public class PSP_Completion 
extends PSP_BaseAdapter
implements PlanServiceProvider, UISubscriber
{

  /** 
   * A zero-argument constructor is required for dynamically loaded PSPs,
   * required by Class.newInstance()
   **/
  public PSP_Completion() {
    super();
  }

  protected static final UnaryPredicate TASK_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Task);
      }
    };


  public void execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception
  {
    // parse parameters
    MyPSPState myState = new MyPSPState(this, query_parameters, psc);
    myState.configure(query_parameters);

    // run
    execute(myState, out);
  }

  /**
   * Fetch CompletionData and write to output.
   */
  protected void execute(
      MyPSPState myState,
      PrintStream out) {
    // get result
    CompletionData result = getCompletionData(myState);
    // write data
    try {
      if (myState.format == MyPSPState.FORMAT_HTML) {
        // html
        printCompletionDataAsHTML(
            myState, 
            out, 
            result);
      } else {
        // unsupported
        if (myState.format == MyPSPState.FORMAT_DATA) {
          // serialize
          ObjectOutputStream oos = new ObjectOutputStream(out);
          oos.writeObject(result);
          oos.flush();
        } else {
          // xml
          out.println("<?xml version='1.0'?>");
          XMLWriter w =
            new XMLWriter(
                new OutputStreamWriter(out));
          result.toXML(w);
          w.flush();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected static Collection getAllTasks(
      MyPSPState myState) {
    Collection col =
      myState.psc.getServerPluginSupport().queryForSubscriber(
          TASK_PRED);
    if (col == null) {
      col = new ArrayList(0);
    }
    return col;
  }

  protected static CompletionData getCompletionData(
      MyPSPState myState) {
    // get tasks
    Collection tasks = getAllTasks(myState);
    long nowTime = System.currentTimeMillis();
    int nTasks = tasks.size();
    Iterator taskIter = tasks.iterator();
    if (myState.showTables) {
      // create and initialize our result
      FullCompletionData result = new FullCompletionData();
      result.setNumberOfTasks(nTasks);
      result.setTimeMillis(nowTime);
      // examine tasks
      for (int i = 0; i < nTasks; i++) {
        Task ti = (Task)taskIter.next();
        PlanElement pe = ti.getPlanElement();
        if (pe != null) {
          AllocationResult peEstResult = pe.getEstimatedResult();
          if (peEstResult != null) {
            double estConf = peEstResult.getConfidenceRating();
            if (peEstResult.isSuccess()) {
              if (estConf > 0.99) {
                // 100% success
              } else {
                result.addUnconfidentTask(
                    makeUnconfidentTask(myState, estConf, ti));
              }
            } else {
              result.addFailedTask(makeFailedTask(myState, estConf, ti));
            }
          } else {
            result.addUnestimatedTask(makeUnestimatedTask(myState, ti));
          }
        } else {
          result.addUnplannedTask(makeUnplannedTask(myState, ti));
        }
      }
      return result;
    } else {
      // create and initialize our result
      SimpleCompletionData result = new SimpleCompletionData();
      result.setNumberOfTasks(nTasks);
      result.setTimeMillis(nowTime);
      // examine tasks
      int nUnplannedTasks = 0;
      int nUnestimatedTasks = 0;
      int nFailedTasks = 0;
      int nUnconfidentTasks = 0;
      for (int i = 0; i < nTasks; i++) {
        Task ti = (Task)taskIter.next();
        PlanElement pe = ti.getPlanElement();
        if (pe != null) {
          AllocationResult peEstResult = pe.getEstimatedResult();
          if (peEstResult != null) {
            double estConf = peEstResult.getConfidenceRating();
            if (peEstResult.isSuccess()) {
              if (estConf > 0.99) {
                // 100% success
              } else {
                nUnconfidentTasks++;
              }
            } else {
              nFailedTasks++;
            }
          } else {
            nUnestimatedTasks++;
          }
        } else {
          nUnplannedTasks++;
        }
      }
      result.setNumberOfUnplannedTasks(nUnplannedTasks);
      result.setNumberOfUnestimatedTasks(nUnestimatedTasks);
      result.setNumberOfFailedTasks(nFailedTasks);
      result.setNumberOfUnconfidentTasks(nUnconfidentTasks);
      return result;
    }
  }

  /**
   * Create an <code>UnplannedTask</code> for the given <code>Task</code>.
   */
  protected static UnplannedTask makeUnplannedTask(
      MyPSPState myState, Task task) {
    UnplannedTask upt = new UnplannedTask();
    fillAbstractTask(myState, upt, task);
    // leave confidence as 0%
    return upt;
  }

  /**
   * Create an <code>UnestimatedTask</code> for the given <code>Task</code>.
   */
  protected static UnestimatedTask makeUnestimatedTask(
      MyPSPState myState, Task task) {
    UnestimatedTask uet = new UnestimatedTask();
    fillAbstractTask(myState, uet, task);
    // leave confidence as 0%
    return uet;
  }

  /**
   * Create an <code>UnconfidentTask</code> for the given <code>Task</code>.
   *
   * @param confidence a double &gt;= 0.0 and &lt; 1.0
   */
  protected static UnconfidentTask makeUnconfidentTask(
      MyPSPState myState, double confidence, Task task) {
    UnconfidentTask uct = new UnconfidentTask();
    fillAbstractTask(myState, uct, task);
    uct.setConfidence(confidence);
    return uct;
  }

  /**
   * Create a <code>FailedTask</code> for the given <code>Task</code>.
   */
  protected static FailedTask makeFailedTask(
      MyPSPState myState, double confidence, Task task) {
    FailedTask ft = new FailedTask();
    fillAbstractTask(myState, ft, task);
    ft.setConfidence(confidence);
    return ft;
  }

  /**
   * Fill an <code>AbstractTask</code> for the given <code>Task</code>,
   * which will grab:<pre>
   *   the UID, 
   *   TASK.PSP's URL for that UID,
   *   the ParentUID, 
   *   TASK.PSP's URL for that ParentUID,
   *   a String description of the PlanElement</pre>.
   */
  protected static void fillAbstractTask(
      MyPSPState myState, AbstractTask toAbsTask, Task task) {
    // set task UID
    UID taskUID = ((task != null) ? task.getUID() : null);
    String sTaskUID = ((taskUID != null) ? taskUID.toString() : null);
    if (sTaskUID == null) {
      return;
    }
    toAbsTask.setUID(sTaskUID);
    String sourceClusterId = task.getSource().toString();
    toAbsTask.setUID_URL(
        getTaskUID_URL(myState.base_url, myState.clusterID, sTaskUID));
    // set parent task UID
    UID pTaskUID = task.getParentTaskUID();
    String spTaskUID = ((pTaskUID != null) ? pTaskUID.toString() : null);
    if (spTaskUID != null) {
      toAbsTask.setParentUID(spTaskUID);
      toAbsTask.setParentUID_URL(
          getTaskUID_URL(myState.base_url, sourceClusterId, spTaskUID));
    }
    // set plan element
    toAbsTask.setPlanElement(getPlanElement(task.getPlanElement()));
  }

  /**
   * Get the TASKS.PSP URL for the given UID String.
   *
   * Assumes that the TASKS.PSP URL is fixed at "/alpine/demo/TASKS.PSP".
   */
  protected static String getTaskUID_URL(
      String baseURL, String clusterId, String sTaskUID) {
    return 
      //baseURL+
      "/$"+
      clusterId+
      "/alpine/demo/TASKS.PSP?mode=3?uid="+
      sTaskUID;
  }

  /**
   * Get a brief description of the given <code>PlanElement</code>.
   */
  protected static String getPlanElement(
      PlanElement pe) {
    return 
      (pe instanceof Allocation) ?
      "Allocation" :
      (pe instanceof Expansion) ?
      "Expansion" :
      (pe instanceof Aggregation) ?
      "Aggregation" :
      (pe instanceof Disposition) ?
      "Disposition" :
      (pe instanceof AssetTransfer) ?
      "AssetTransfer" :
      (pe != null) ?
      pe.getClass().getName() : 
      null;
  }

  /**
   * Write the given <code>CompletionData</code> as formatted HTML.
   */
  protected static void printCompletionDataAsHTML(
      MyPSPState myState, 
      PrintStream out, 
      CompletionData result) {
    // javascript based on TASKS.PSP (PSP_PlanView)
    out.print(
        "<html>\n"+
        "<script language=\"JavaScript\">\n"+
        "<!--\n"+
        "function mySubmit() {\n"+
        "  var tidx = document.myForm.formCluster.selectedIndex\n"+
        "  var cluster = document.myForm.formCluster.options[tidx].text\n"+
        "  document.myForm.action=\"/$\"+cluster+\"");
    out.print(myState.psp_path);
    out.print("?POST\"\n"+
        "  return true\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n"+
        "<head>\n"+
        "<title>");
    out.print(myState.clusterID);
    out.print(
        "</title>"+
        "</head>\n"+
        "<body>"+
        "<h2><center>Completion at ");
    out.print(myState.clusterID);
    out.print(
        "</center></h2>\n"+
        "<form name=\"myForm\" method=\"post\" "+
        "onSubmit=\"return mySubmit()\">\n"+
        "Completion data at "+
        "<select name=\"formCluster\">\n");
    // lookup all known cluster names
    Vector names = new Vector();
    myState.psc.getAllNames(names, true);
    int sz = names.size();
    for (int i = 0; i < sz; i++) {
      String n = (String)names.elementAt(i);
      out.print("  <option ");
      if (n.equals(myState.clusterID)) {
        out.print("selected ");
      }
      out.print("value=\"");
      out.print(n);
      out.print("\">");
      out.print(n);
      out.print("</option>\n");
    }
    out.print(
        "</select>, \n"+
        "<input type=\"checkbox\" name=\"showTables\" value=\"true\" ");
    if (myState.showTables) {
      out.print("checked");
    }
    out.print("> show table, \n"+
        "<input type=\"submit\" name=\"formSubmit\" value=\"Reload\"><br>\n"+
        "</form>\n");
    printCountersAsHTML(myState, out, result);
    printTablesAsHTML(myState, out, result);
    out.print("</body></html>");
    out.flush();
  }

  protected static void printCountersAsHTML(
      MyPSPState myState, 
      PrintStream out, 
      CompletionData result) {
    out.print(
        "<pre>\n"+
        "Time: <b>");
    long timeMillis = result.getTimeMillis();
    out.print(new Date(timeMillis));
    out.print("</b>   (");
    out.print(timeMillis);
    out.print(
        " MS)"+
        "\n\nNumber of Tasks: <b>");
    int nTasks = result.getNumberOfTasks();
    out.print(nTasks);
    out.print("\n</b>Subset of Tasks[");
    out.print(nTasks);
    out.print("] planned (non-null PlanElement): <b>");
    int nUnplannedTasks = result.getNumberOfUnplannedTasks();
    int nPlannedTasks = (nTasks - nUnplannedTasks);
    out.print(nPlannedTasks);
    out.print("</b>  (<b>");
    double percentPlannedTasks =
      ((nTasks > 0) ? 
       (100.0 * (((double)nPlannedTasks) / nTasks)) :
       0.0);
    out.print(percentPlannedTasks);
    out.print(
        " %</b>)"+
        "\nSubset of planned[");
    out.print(nPlannedTasks);
    out.print("] estimated (non-null EstimatedResult): <b>");
    int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
    int nEstimatedTasks = (nPlannedTasks - nUnestimatedTasks);
    out.print(nEstimatedTasks);
    out.print("</b>  (<b>");
    double percentEstimatedTasks =
      ((nPlannedTasks > 0) ? 
       (100.0 * (((double)nEstimatedTasks) / nPlannedTasks)) :
       0.0);
    out.print(percentEstimatedTasks);
    out.print(
        " %</b>)"+
        "\nSubset of estimated[");
    out.print(nEstimatedTasks);
    out.print("] that are estimated successful: <b>");
    int nFailedTasks = result.getNumberOfFailedTasks();
    int nSuccessfulTasks = (nEstimatedTasks - nFailedTasks);
    out.print(nSuccessfulTasks);
    out.print("</b>  (<b>");
    double percentSuccessfulTasks =
      ((nEstimatedTasks > 0) ? 
       (100.0 * (((double)nSuccessfulTasks) / nEstimatedTasks)) :
       0.0);
    out.print(percentSuccessfulTasks);
    out.print(
        " %</b>)"+
        "\nSubset of estimated successful[");
    out.print(nSuccessfulTasks);
    out.print("] with 100% confidence: <b>");
    int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
    int nFullConfidenceTasks = (nSuccessfulTasks - nUnconfidentTasks);
    out.print(nFullConfidenceTasks);
    out.print("</b>  (<b>");
    double percentFullConfidenceTasks =
      ((nSuccessfulTasks > 0) ? 
       (100.0 * (((double)nFullConfidenceTasks) / nSuccessfulTasks)) :
       0.0);
    out.print(percentFullConfidenceTasks);
    out.print(" %</b>)\n");
    out.print("</pre>\n");
  }

  protected static void printTablesAsHTML(
      MyPSPState myState, 
      PrintStream out, 
      CompletionData result) {
    if (result instanceof FullCompletionData) {
      int nUnplannedTasks = result.getNumberOfUnplannedTasks();
      beginTaskHTMLTable(
          out, 
          ("Unplanned Tasks["+nUnplannedTasks+"]"),
          "(PlanElement == null)");
      for (int i = 0; i < nUnplannedTasks; i++) {
        printAbstractTaskAsHTML(out, i, result.getUnplannedTaskAt(i));
      }
      endTaskHTMLTable(out);
      int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
      beginTaskHTMLTable(
          out, 
          ("Unestimated Tasks["+nUnestimatedTasks+"]"),
          "(Est. == null)");
      for (int i = 0; i < nUnestimatedTasks; i++) {
        printAbstractTaskAsHTML(out, i, result.getUnestimatedTaskAt(i));
      }
      endTaskHTMLTable(out);
      int nFailedTasks = result.getNumberOfFailedTasks();
      beginTaskHTMLTable(
          out, 
          ("Failed Tasks["+nFailedTasks+"]"),
          "(Est.isSuccess() == false)");
      for (int i = 0; i < nFailedTasks; i++) {
        printAbstractTaskAsHTML(out, i, result.getFailedTaskAt(i));
      }
      endTaskHTMLTable(out);
      int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
      beginTaskHTMLTable(
          out, 
          ("Unconfident Tasks["+nUnconfidentTasks+"]"),
          "((Est.isSuccess() == true) &amp;&amp; (Est.Conf. < 100%))");
      for (int i = 0; i < nUnconfidentTasks; i++) {
        printAbstractTaskAsHTML(out, i, result.getUnconfidentTaskAt(i));
      }
      endTaskHTMLTable(out);
    } else {
      // no table data
      out.print(
          "<p>"+
          "<a href=\"");
      //out.print(myState.cluster_psp_url);
      out.print("/$");
      out.print(myState.clusterID);
      out.print(myState.psp_path);
      out.print(
          "?showTables=true\">"+
          "Full Listing of Unplanned/Unestimated/Failed/Unconfident Tasks (");
      out.print(
          (result.getNumberOfTasks() - 
           result.getNumberOfFullySuccessfulTasks()));
      out.print(
          " lines)</a>\n");
    }
  }

  /**
   * Begin a table of <tt>printAbstractTaskAsHTML</tt> entries.
   */
  protected static void beginTaskHTMLTable(
      PrintStream out, String title, String subTitle) {
    out.print(
        "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n"+
        "<tr bgcolor=lightgrey><th align=left colspan=5>");
    out.print(title);
    if (subTitle != null) {
      out.print(
          "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>");
      out.print(subTitle);
      out.print("</i></tt>");
    }
    out.print(
        "</th></tr>\n"+
        "<tr>"+
        "<th></th>"+
        "<th>UID</th>"+
        "<th>ParentUID</th>"+
        "<th>Confidence</th>"+
        "<th>PlanElement</th>"+
        "</tr>\n");
  }

  /**
   * End a table of <tt>printAbstractTaskAsHTML</tt> entries.
   */
  protected static void endTaskHTMLTable(
      PrintStream out) {
    out.print(
        "</table>\n"+
        "<p>\n");
  }

  /**
   * Write the given <code>AbstractTask</code> as formatted HTML.
   */
  protected static void printAbstractTaskAsHTML(
      PrintStream out, int index, AbstractTask at) {
    out.print("<tr align=right><td>");
    out.print(index);
    out.print("</td><td>");
    String uidURL = at.getUID_URL();
    if (uidURL != null) {
      out.print("<a href=\"");
      out.print(uidURL);
      out.print("\" target=\"itemFrame\">");
    }
    out.print(at.getUID());
    if (uidURL != null) {
      out.print("</a>");
    }
    out.print("</td><td>");
    String pUidURL = at.getParentUID_URL();
    if (pUidURL != null) {
      out.print("<a href=\"");
      out.print(at.getParentUID_URL());
      out.print("\" target=\"itemFrame\">");
    }
    out.print(at.getParentUID());
    if (pUidURL != null) {
      out.print("</a>");
    }
    out.print("</td><td>");
    double conf = at.getConfidence();
    out.print(
        (conf < 0.001) ? 
        "0.0%" : 
        ((100.0 * conf) + "%"));
    out.print("</td><td>");
    out.print(at.getPlanElement());
    out.print("</td></tr>\n");
  }

  /** 
   * Holds PSP state.
   */
  protected static class MyPSPState extends PSPState {

    /** my additional fields **/
    public boolean anyArgs;
    public int format;
    public boolean showTables;

    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    /** constructor **/
    public MyPSPState(
        UISubscriber xsubscriber,
        HttpInput query_parameters,
        PlanServiceContext xpsc) {
      super(xsubscriber, query_parameters, xpsc);
      // default to HTML
      format = FORMAT_HTML;
    }

    /** use a query parameter to set a field **/
    public void setParam(String name, String value) {
      //super.setParam(name, value);
      if (eq("format", name)) {
        anyArgs = true;
        if (eq("data", value)) {
          format = FORMAT_DATA;
        } else if (eq("xml", value)) {
          format = FORMAT_XML;
        } else if (eq("html", value)) {
          format = FORMAT_HTML;
        }
      } else if (eq("showTables", name)) {
        anyArgs = true;
        showTables =
          ((value == null) || 
           (eq("true", value)));
        // stay backwards-compatable
      } else if (eq("data", name)) {
        anyArgs = true;
        format = FORMAT_DATA;
      } else if (eq("xml", name)) {
        anyArgs = true;
        format = FORMAT_XML;
      } else if (eq("html", name)) {
        anyArgs = true;
        format = FORMAT_HTML;
      }
    }

    // startsWithIgnoreCase
    private static final boolean eq(String a, String b) {
      return a.regionMatches(true, 0, b, 0, a.length());
    }
  }


  //
  // uninteresting and/or obsolete methods 
  //
  public PSP_Completion(
      String pkg, String id) 
    throws RuntimePSPException {
      setResourceLocation(pkg, id);
    }
  public boolean test(
      HttpInput query_parameters, PlanServiceContext sc) {
    super.initializeTest();
    return false;
  }
  public void subscriptionChanged(Subscription subscription) {
  }
  public boolean returnsXML() {
    return true;
  }
  public boolean returnsHTML() {
    return false;
  }
  public String getDTD()  {
    return null;
  }

}

