/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * The file GanttChartView reads a CSV file and displays
 * a Gantt chart UI.
 * <p>
 * This class supports subclassing.  The provided CSV file 
 * format is described below.
 * <p>
 * Blank lines and line starting with "#" are ignored.
 * <p>
 * A blank start date tells the reader to copy the end date
 * from the prior line.  This is handy if your input file
 * only records the end time of the event.
 * <p>
 * A blank end date tells the reader to copy the start date
 * from the current line.  This is handy if the event is
 * instantaneous.
 * <p>
 * Example file:
 * <pre>
 *   # comment
 *   A, 2002-10-08 01:03:00, 2002-10-08 02:50:00, grey, foo
 *   B, 2002-10-08 01:00:00, 2002-10-08 01:30:00, red, bar
 *   B, , 2002-10-08 03:05:00, blue, baz
 * </pre>
 */
public class FileGanttChartView extends SimpleGanttChartView {

  public static void main(String[] args) {
    if (args.length <= 0) {
      System.err.println("Expecting filename parameter");
      return;
    }
    (new FileGanttChartView(args)).launch();
  }

  public void launch() {
    launch(null);
  }

  public void launch(Date baseDate) {
    preProcess();
    try {
      readFile();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
    postProcess();
    if (verbose) {
      System.out.println(this);
    }
    super.launch(baseDate, filename);
  }

  protected final boolean verbose;
  protected final String filename;

  protected final Map instances = new HashMap();
  protected final List legs = new ArrayList();

  protected void preProcess() {
    // called prior to readFile
  }

  public FileGanttChartView(String[] args) {
    if (args.length <= 0) {
      throw new RuntimeException(
          "Requires \".csv\" filename parameter");
    }
    verbose = false;
    filename = args[0];
  }

  // override for non-file input
  protected BufferedReader openFile() throws IOException {
    File sf = new File(filename);
    BufferedReader f = 
      new BufferedReader(
          new InputStreamReader(
            new FileInputStream(sf)));
    return f;
  }

  protected void readFile() throws IOException {
    // parse file
    BufferedReader f = openFile();
    for (int i = 0; ; i++) {
      String l = f.readLine();
      if (l == null) {
        break;
      }
      String sa[];
      try {
        sa = parseLine(l, i);
      } catch (RuntimeException re) {
        throw new RuntimeException(
            "Invalid line "+filename+
            " ["+i+"]: "+l,
            re);
      }
      if (sa != null) {
        instances.put(sa[0], sa);
        legs.add(sa);
      }
    }
    f.close();
  }

  protected String getTokenizerDelim() {
    return ",";
  }

  protected String[] parseLine(
      String l,
      int i) {
    StringTokenizer st = 
      new StringTokenizer(
          l,
          getTokenizerDelim());
    if (!st.hasMoreTokens()) {
      return null;
    }
    final String instId = st.nextToken().trim();
    if (instId.startsWith("#")) {
      return null;
    }
    String[] priorEntry = (String[]) instances.get(instId);
    String priorEnd = 
      (priorEntry != null ? priorEntry[3] : null);
    final String legId = ("#"+i);
    final String rawStart = 
      (st.hasMoreTokens() ? st.nextToken().trim() : null);
    final String rawEnd = 
      (st.hasMoreTokens() ? st.nextToken().trim() : null);
    final String start =
      parseStart(
          priorEnd,
          rawStart,
          rawEnd);
    final String end =
      parseEnd(
          start,
          rawStart,
          rawEnd);
    final String color =
      (st.hasMoreTokens() ? st.nextToken().trim() : null);
    final String info =
      (st.hasMoreTokens() ? st.nextToken().trim() : null);
    String[] ret = {
      instId,
      legId,
      start,
      end,
      color,
      info,
    };
    return ret;
  }

  protected String parseStart(
      String priorEnd,
      String rawStart,
      String rawEnd) {
    if (rawStart == null || rawStart.length() == 0) return priorEnd;
    return rawStart;
  }

  protected String parseEnd(
      String start,
      String rawStart,
      String rawEnd) {
    if (rawEnd == null || rawEnd.length() == 0) return start;
    return rawEnd;
  }

  protected void postProcess() {
    // last chance to mangle legs
  }

  protected Iterator getInstances() {
    // convert "set of names" to "iter of {name,name}"
    Set s = instances.keySet();
    List l = new ArrayList(s);
    Collections.sort(l);
    final Iterator iter = l.iterator();
    return new Iterator() {
      public boolean hasNext() {
        return iter.hasNext();
      }
      public Object next() {
        final String s = (String) iter.next();
        String[] sa = {s, s};
        return sa;
      }
      public void remove() {
        throw new UnsupportedOperationException("remove");
      }
    };
  }

  protected Iterator getLegs() {
    return legs.iterator();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("filename:\n  ");
    sb.append(filename);
    sb.append("\ninstances:\n  ");
    sb.append(instances.keySet());
    sb.append("\nlegs:");
    for (Iterator i = getLegs(); i.hasNext(); ) {
      String[] sa = (String[])i.next();
      sb.append("\n  ");
      for (int j = 0; j < sa.length; j++) {
        sb.append(sa[j]).append(", ");
      }
    }
    sb.append("\n");
    return sb.toString();
  }
}
