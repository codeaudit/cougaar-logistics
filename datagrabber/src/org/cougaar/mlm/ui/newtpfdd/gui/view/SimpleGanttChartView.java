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

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoInstance;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoType;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LegNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;

/**
 * The "simple" GanttChartView base class allows a developer
 * to reuse the existing Gantt UI with non-TPFDD data.
 *
 * @see FileGanttChartView subclass for CSV file parsing
 */
public abstract class SimpleGanttChartView {

  /**
   * Example to illustrate use.
   * <p>
   * Running will popup a UI with two lines, one
   * labelled "Alpha" and another "Beta".  The Alpha
   * row will be a single grey line.  The Beta row
   * will be red, blue, (oversized) yellow, and green.
   */
  public static void main(String[] args) {
    SimpleGanttChartView g = new SimpleGanttChartView() {
      protected Iterator getInstances() {
        String[][] INSTANCES = {
          {"A",        // instance id
            "Alpha"},  // instance pretty name
          {"B",
            "Beta"},
        };
        return Arrays.asList(INSTANCES).iterator();
      }
      protected Iterator getLegs() {
        int i = 0;
        String[][] LEGS = {
          {"A",                    // id of instance
            ("#"+(i++)),           // unique leg id
            "2002-10-08 01:03:00", // start time
            "2002-10-08 05:50:00", // end time
            "grey",                // color
            "text info"},          // info
          {"B",
            ("#"+(i++)),
            "2002-10-08 01:00:00",
            "2002-10-08 04:30:00",
            "red",
            "begin"},
          {"B",
            ("#"+(i++)),
            "2002-10-08 04:30:00",
            "2002-10-08 07:00:00",
            "blue",
            "middle"},
          {"B",
            ("#"+(i++)),
            "2002-10-08 09:00:00", // time gap 7-9
            "2002-10-08 15:20:00",
            "green",
            "end"},
        };
        return Arrays.asList(LEGS).iterator();
      }
      // could override "parseDate(String)" to change
      // the above "2002-10-.." date format
    };
    g.launch(null, "test");
  }

  /**
   * Get an iterator of the Gantt row instances.
   * <pre>
   * The format of each element in the iterator
   * is a string array:<i>
   *   {instance_identifier,
   *    readable_name}</i>
   * e.g.
   *   { "inst3",
   *     "foo"}
   * </pre>
   * <p>
   * In practice these two names are often the same.
   *
   * @return an Iterator of String[] matching the above format
   */
  protected abstract Iterator getInstances();

  /**
   * Get an iterator of the Gantt column legs.
   * <pre>
   * The format of each element in the iterator
   * is a string array:<i>
   *   {instance_identifier,
   *    leg_identifier,
   *    start_date,
   *    end_date,
   *    color,
   *    info}</i>
   * e.g.
   *   { "inst3",
   *     "leg7",
   *     "2002-10-01 01:23:45", 
   *     "2002-10-12 10:20:30", 
   *     "red",
   *     "any text"}
   * </pre>
   * <p>
   * The instance identifier must match an instance name
   * from "getInstances()".
   * <p>
   * The leg identifier can be arbitrary so long as it is
   * unique.  A simple counter can be used
   * (e.g. <code>"#"+(++counter)</code>).
   * <p>
   * The date format is controlled by the "parseDate(String)"
   * method.
   * <p>
   * Valid colors are:<ul>
   *   <li>red</li>
   *   <li>yellow</li>
   *   <li>green</li>
   *   <li>blue</li>
   *   <li>purple</li>
   *   <li>burgundy</li>
   *   <li>grey <i>(default)</i></li>
   * </ul>
   * <p>
   * The info is shown on the Gantt line, and is only readable
   * if the Gantt is zoomed in close, and on the mouse-over
   * line at the bottom of the UI.
   *
   * @return an Iterator of String[] matching the above format
   */
  protected abstract Iterator getLegs();

  public void launch(Date baseDate, String title) {
    // disable begin/end lozenges
    System.setProperty("originLozengeSize", "0");
    System.setProperty("destinationLozengeSize", "0");
    // launch UI
    TaskModel model = createTaskModel();
    String now = 
      (new SimpleDateFormat("MM/dd/yyyy")
       ).format(
         baseDate != null ? baseDate : (new Date()));
    GanttChartView ganttChartView = 
      new GanttChartView(
          model, 
          now);
    JPanel ganttChartPanel = new JPanel();
    ganttChartPanel.setLayout(new BorderLayout());
    ganttChartPanel.add(ganttChartView, BorderLayout.CENTER);
    JFrame ganttChartFrame = new JFrame();
    ganttChartFrame.getContentPane().add(ganttChartPanel, BorderLayout.CENTER);
    ganttChartFrame.setTitle(title);
    ganttChartFrame.setSize(640, 480);
    ganttChartFrame.setVisible(true);
    //ganttChartFrame.setJMenuBar(ganttChartView.getMenuBar());
    ganttChartView.showTPFDDLines(null);
  }

  /**
   * Create the task model.
   */
  protected TaskModel createTaskModel() {
    return new SimpleTaskModel(null);
  }

  /** default date parser */
  private static final Object DATE_PARSER_LOCK = new Object();
  private static final SimpleDateFormat SHORT_DATE_FORMAT = 
    new SimpleDateFormat("HH:mm:ss,SSS");
  private static final SimpleDateFormat FULL_DATE_FORMAT = 
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  /**
   * Parse a date in a leg.
   * <p>
   * The default format is either the logger format ("HH:mm:ss,SSS")
   * or "yyyy-MM-dd HH:mm:ss"
   */
  protected Date parseDate(String dateString) {
    if (dateString == null) {
      return null;
    }
    synchronized (DATE_PARSER_LOCK) {
      try {
        if (dateString.indexOf(':') < 0) {
          // e.g. 1078155547241
          return new Date(Long.parseLong(dateString));
        } else if (dateString.indexOf(' ') < 0) {
          // e.g. 10:39:07,241
          return SHORT_DATE_FORMAT.parse(dateString);
        } else {
          // e.g. 2004-03-01 10:39:07
	  return FULL_DATE_FORMAT.parse(dateString);
        }
      } catch (Exception e) {
        return new Date(0);
      }
    }
  }

  protected String getInfoPrefix() {
    return "";
  }

  protected class SimpleTaskModel extends TaskModel {
    public SimpleTaskModel(DatabaseState dbState) {
      super(dbState);
    }
    protected void doTPFDDQuery(FilterClauses filterClauses, Set forest) {
      forest.add(createTree());
    }

    protected Tree createTree() {
      Tree cargoTree = new Tree();
      UIDGenerator generator = cargoTree.getGenerator();
      String unitID = "none";
      Node cargoRoot = new CargoType(generator, unitID); // could be anything...?
      cargoTree.setRoot(cargoRoot);
      Map instanceToNode = new HashMap();
      attachInstances(cargoTree, instanceToNode);
      attachLegs(cargoTree, instanceToNode);
      return cargoTree;
    }

    protected void attachInstances(Tree cargoTree, Map instanceToNode) {
      UIDGenerator generator = cargoTree.getGenerator();
      Iterator instancesIter = getInstances();
      while (instancesIter.hasNext()) {
        String[] args = (String[]) instancesIter.next();
        String instId = args[0];
        if (instanceToNode.get(instId) == null) {
          CargoInstance instanceNode = 
            parseInstance(generator, instId, args);
          cargoTree.addNode(cargoTree.getRoot(), instanceNode);
          instanceToNode.put(instId, instanceNode);
        }
      }
    }

    protected CargoInstance parseInstance(
        UIDGenerator generator, String instId, String[] args) {
      //String instId = args[0];
      String name = args[1];

      CargoInstance instanceNode = new CargoInstance(generator, instId);
      instanceNode.setDisplayName(name);

      return instanceNode;
    }

    protected void attachLegs(Tree cargoTree, Map instanceToNode) {
      UIDGenerator generator = cargoTree.getGenerator();
      Iterator legsIter = getLegs();
      while (legsIter.hasNext()) {
        String[] args = (String[]) legsIter.next();
        String instId = args[0];
        Node instanceNode = (Node) instanceToNode.get(instId);
        if (instanceNode == null){
          throw new RuntimeException("Unknown instance id: "+instId);
        } else {
          LegNode leg = parseLeg(generator, instId, args);
          cargoTree.addNode(instanceNode.getUID(), leg);
        }
      }
    }

    protected LegNode parseLeg(
        UIDGenerator generator, String instId, String[] args) {
      //String instId = args[0];
      String legId = args[1];
      String start = args[2];
      String end = args[3];
      String color = args[4];
      String info = args[5];

      LegNode leg = new LegNode(generator, legId);
      leg.setDisplayName("");
      Date startDate = parseDate(start);
      Date endDate = parseDate(end);
      leg.setActualStart(startDate);
      leg.setActualEnd(endDate);
      leg.setReadyAt(startDate);
      leg.setCarrierType(getInfoPrefix());
      leg.setMode(parseColor(color));
      leg.setCarrierName(info != null ? info : "");

      /*
       leg.setFrom(startName);
       leg.setTo(endName);
       */

      return leg;
    }

    protected int parseColor(String color) {
      return
        color == null ? Node.MODE_UNKNOWN :
        color.equals("red") ? (-1) :
        color.equals("yellow") ? Node.MODE_GROUND :
        color.equals("green") ? Node.MODE_SEA :
        color.equals("blue") ? Node.MODE_AIR :
        color.equals("burgundy") ? Node.MODE_ITINERARY :
        color.equals("purple") ? Node.MODE_AGGREGATE :
        Node.MODE_UNKNOWN;
    }
  }
}
