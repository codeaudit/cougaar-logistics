/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
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
 * The file GanttChartView reads a CSV file and displays a Gantt chart
 * UI.
 * <p>
 * The standard CSV file format is described below:
 * <p>
 * Blank lines and line starting with "#" are ignored.  Data lines
 * look like:<pre>
 *   NAME, [START], [END], [COLOR], [COMMENT]
 * </pre>
 * The start and end dates can be in milliseconds since 1970,
 * "HH:mm:ss,SSS", or "yyyy-MM-dd HH:mm:ss".  If the start date
 * is not specified then it is copied from the prior line for that
 * NAME, or the minimal date of all names if this is the first entry
 * for the NAME.  If the end date is not specified then it is copied
 * from the next line's start date for that NAME, or the maximum date
 * for all names if this is the last entry for the NAME.  Valid
 * colors are:<ul>
 *   <li>red</li>
 *   <li>yellow</li>
 *   <li>green</li>
 *   <li>blue</li>
 *   <li>purple</li>
 *   <li>burgundy</li>
 *   <li>grey <i>(default)</i></li>
 * </ul>
 * <p>
 * Example input:<pre>
 *   # any comment 
 *   A, 12:34:56,789, , red,
 *   A, 15:00:00,000, , yellow, test me
 *   A, 19:20:21,230, , green,
 *   B, 11:00:00,000, , blue,
 *   B, 23:00:00,000, , grey,
 * </pre>
 */
public class FileGanttChartView extends SimpleGanttChartView {

  public static void main(String[] args) {
    if (args.length <= 0 || "--help".equals(args[0])) {
      System.err.println(
          "Usage: java "+FileGanttChartView.class+" CSV_FILE"+
          "\n"+
          "\nThe CSV_FILE can be a file name or \"-\" for stdin."+
          "\nSee the javadocs for file format details."+
          "\n"+
          "\nExample input:"+
          "\n   # any comment"+
          "\n   FOO, 12:34:56,789, , red,"+
          "\n   FOO, 15:00:00,000, , yellow, my label"+
          "\n   FOO, 19:20:21,230, , green,"+
          "\n   BAR, 11:00:00,000, , blue,"+
          "\n   BAR, 23:00:00,000, , grey,");
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

  protected final List instances = new ArrayList();
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
    filename = args[0].trim();
  }

  // override for non-file input
  protected BufferedReader openFile() throws IOException {
    InputStream is;
    if ("-".equals(filename)) {
      is = System.in;
    } else {
      URI uri = URI.create(filename);
      if (uri.isAbsolute()) {
        is = uri.toURL().openStream();
      } else {
        is = new FileInputStream(filename);
      }
    }
    return
      new BufferedReader(
          new InputStreamReader(is));
  }

  protected void readFile() throws IOException {
    // parse file into map of (inst -> List(String[5]))
    Map m = new HashMap(); 
    BufferedReader f = openFile();
    boolean inHttpHeader = false;
    for (int i = 0; ; i++) {
      String l = f.readLine();
      if (l == null) {
        break;
      }
      if (i == 0 && l.startsWith("HTTP 200")) {
        inHttpHeader = true;
        continue;
      }
      if (inHttpHeader) {
        if (l.trim().length() == 0) {
          inHttpHeader = false;
        }
        continue;
      }
      String sa[];
      try {
        sa = parseLine(l, i);
      } catch (RuntimeException re) {
        throw new RuntimeException(
            "Invalid line "+filename+" ["+i+"]: "+l,
            re);
      }
      if (sa == null) {
        continue;
      }
      List l2 = (List) m.get(sa[0]);
      if (l2 == null) {
        l2 = new ArrayList();
        m.put(sa[0], l2);
      }
      l2.add(sa);
    }
    f.close();
    // find min/max dates
    String min_start = null; 
    String max_end = null; 
    Date min_start_date = null; 
    Date max_end_date = null; 
    for (Iterator itr = m.values().iterator(); itr.hasNext(); ) {
      List l = (List) itr.next();
      String[] sa0 = (String[]) l.get(0);
      String start = sa0[2];
      if (start == null) {
        start = sa0[3];
      }
      if (start != null) {
        Date d = parseDate(start);
        if ((min_start_date == null) ||
            (min_start_date.compareTo(d) > 0)) {
          min_start_date = d;
          min_start = start;
        }
      }
      String[] saN = (String[]) l.get(l.size()-1);
      String end = saN[3];
      if (end == null) {
        end = saN[2];
      }
      if (end != null) {
        Date d = parseDate(end);
        if ((max_end_date == null) ||
            (max_end_date.compareTo(d) < 0)) {
          max_end_date = d;
          max_end = end;
        }
      }
    }
    // fix dates
    for (Iterator itr = m.values().iterator(); itr.hasNext(); ) {
      List l = (List) itr.next();
      for (int i = 0, n = l.size(); i < n; i++) {
        String[] sa = (String[]) l.get(i);
        String start = sa[2];
        String end = sa[3];
        if (start == null) {
          if (i > 0) {
            String[] prev = (String[]) l.get(i-1);
            String prev_end = prev[3];
            start = prev_end;
          } else {
            start = min_start;
          }
        }
        if (end == null) {
          if ((i+1) < n) {
            String[] next = (String[]) l.get(i+1);
            String next_start = next[2];
            end = next_start;
          } else {
            end = max_end;
          }
        }
        // fix wrap-around?  (e.g. 23:59:59 -> 00:00:01)
        sa[2] = start;
        sa[3] = end;
      }
    }
    // add instances (set_of_names -> sorted_list{name,name})
    List tmp = new ArrayList(m.keySet());
    Collections.sort(tmp);
    for (int i = 0, n = tmp.size(); i < n; i++) {
      final String s = (String) tmp.get(i);
      String[] ia = {s, s};
      instances.add(ia);
    }
    // add legs
    for (Iterator itr = m.values().iterator(); itr.hasNext(); ) {
      List l = (List) itr.next();
      for (int i = 0, n = l.size(); i < n; i++) {
        String[] sa = (String[]) l.get(i);
        legs.add(sa);
      }
    }
  }

  protected String[] parseLine(
      String l,
      int i) {
    StringTokenizer st = new StringTokenizer(l, ",");
    final String instId = nextToken(st);
    if (instId == null || instId.startsWith("#")) {
      return null;
    }
    final String legId = ("#"+i);
    String s1 = nextToken(st);
    String s2 = nextToken(st);
    if (isMillis(s2)) {
      // fix "HH:mm:ss,SSS"
      s1 += ","+s2; 
      s2 = nextToken(st);
    }
    String s3 = nextToken(st);
    if (isMillis(s3)) {
      // fix "HH:mm:ss,SSS"
      s2 += ","+s3; 
      s3 = nextToken(st);
    }
    final String start = s1;
    final String end = s2;
    final String color = s3;
    final String info = nextToken(st);
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

  protected static final String nextToken(StringTokenizer st) {
    if (!st.hasMoreTokens()) {
      return null;
    }
    String s = st.nextToken().trim();
    if (s != null && s.length() == 0) {
      s = null;
    }
    return s;
  }

  private static final boolean isMillis(String s) {
    // same as s.matches("^\d\d\d$");
    return 
      (s != null &&
       s.length() == 3 &&
       Character.isDigit(s.charAt(0)) &&
       Character.isDigit(s.charAt(1)) &&
       Character.isDigit(s.charAt(2)));
  }

  protected void postProcess() {
    // last chance to mangle legs
  }

  protected Iterator getInstances() {
    return instances.iterator();
  }

  protected Iterator getLegs() {
    return legs.iterator();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("filename:\n  ");
    sb.append(filename);
    sb.append("\ninstances:\n  ");
    sb.append(instances);
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
