/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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


package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.io.InputStream;
import java.net.URL;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import org.cougaar.logistics.ui.stoplight.ui.components.CChartLegend;

import org.cougaar.mlm.ui.data.UISimpleInventory;
import org.cougaar.mlm.ui.data.UISimpleSchedule;
import org.cougaar.mlm.ui.planviewer.ConnectionHelper;
import org.cougaar.mlm.ui.planviewer.XMLClientConfiguration;

import org.cougaar.util.ThemeFactory;


public class QueryHelper implements ActionListener,
            ItemListener{
  static final String UPDATE = "Update";
  static final String RESET = "Reset";
  static final String CLEAR = "Clear";
  static final String SAVE = "Save";
  static final String CDAY_MODE = "Cdays";

  String clusterURL;
  String PSP_package;
  String PSP_id;
  Container container;

  Query query;
  String clusterName;
  String assetName;
  boolean doDisplayTable = true;
  

  private BlackJackInventoryChart chart = null;
  private CChartLegend legend = null;
  private JTable table = null;

  /* Called by application or applet; if its an applet, then
     use the container passed as an argument.
   */

  public QueryHelper(Query query, String clusterURL, BlackJackInventoryChart chart, JTable table, CChartLegend legend, boolean doDisplayTable)
  {
    this.query = query;
    this.clusterURL = clusterURL;
    System.out.println("clusterRL" + clusterURL + "dummy");
    this.doDisplayTable = doDisplayTable;
    
    clusterName = clusterURL.substring(clusterURL.indexOf('$')+1, clusterURL.length()-1);
    assetName = query.getQueryToSend();
    
    if (assetName == null)
    {
      assetName = "";
    }
    
    int indx = assetName.indexOf(":");
    if (indx > -1) assetName = assetName.substring(indx+1);

    PSP_package = XMLClientConfiguration.PSP_package;

    this.chart = chart;
    this.legend = legend;
    this.table = table;
    System.out.println("QueryHelper");

    PSP_id = query.getPSP_id();
    System.out.println("Submitting: fresh chart ");
    executeQuery();
    System.out.println("buildTableModel ");
    query.buildTableModel();

    if (doDisplayTable && table != null)
      displayTable(query);

    if (chart != null)
      displayChart();
  }

  /** Called by an application when data is retrieved from a file.
   */

  public QueryHelper(Query query, BlackJackInventoryChart chart, JTable table, CChartLegend legend)
  {
    System.out.println("QueryHelper file version");
    this.query = query;
    this.chart = chart;
    this.legend = legend;
    this.table = table;
    assetName = query.getQueryToSend();
    int indx = assetName.indexOf(":");
    query.buildTableModel();

    if (doDisplayTable && table != null)
      displayTable(query);

    if (chart != null)
      displayChart();
  }



  /*  Submit request to the cluster, read response(s) and display results.
   */

 private void doUpdate() {

//    System.out.println("Submitting: fresh chart ");
//    System.out.println("QueryHelper::doUpdate: Thread priority: " + Thread.currentThread().getPriority());

    executeQuery();
    updateChart();

  }


  private void executeQuery() {
    String request = query.getQueryToSend();
    InputStream is = null;

    System.out.println("ExecutingQuery: " + request + " to: " + clusterURL +
                       " for: " + PSP_id);

    try {
      ConnectionHelper connection =
	new ConnectionHelper(clusterURL, "inventory");
	//        new ConnectionHelper(clusterURL, PSP_package, PSP_id);
      //System.out.println("connection");
      connection.sendData(request);
      //System.out.println("senddata");
      is = connection.getInputStream();
      //System.out.println("is");
    } catch (Exception e) {
      displayErrorString(e.toString());
      System.out.println("caught exception");
    }
    //System.out.println("try read reply");
    query.readReply(is);

  }



  private void displayErrorString(String reply) {
    JOptionPane.showMessageDialog(chart, reply, reply,
                                  JOptionPane.ERROR_MESSAGE);
  }


  private void displayChart()
  {
    if (!query.setChartData(clusterName, chart, legend))
    {
      displayErrorString("No data received");
    }
  }


  private void displayTable(Query query)
  {
    if (!query.setTableData(clusterName, table, legend))
    {
      //displayErrorString("No data received");
    }
  }











  private void updateChart() {
    JPanel chart =
  query.reinitializeAndUpdateChart(clusterName);

    // for scrolling chart, doesn't work
    //    if (chart != null) {
      //      JScrollPane scrollpane = new JScrollPane(chart);
      //      scrollpane.setBorder(new BevelBorder(BevelBorder.LOWERED));
      //      scrollpane.setPreferredSize(new Dimension(300, 100));
      //      container.setLayout(new BorderLayout());
      //      container.add(scrollpane, BorderLayout.CENTER);
      //      JButton updateButton = new JButton("update");
      //      updateButton.setActionCommand(UPDATE);
      //    updateButton.addActionListener(this);
      //      container.add(updateButton, BorderLayout.SOUTH);
      //      container.validate();
      //    }

    if (chart != null) {
  /* MWD Remove
      container.setLayout(new BorderLayout());
      container.add(chart, BorderLayout.CENTER);
      chart.setBorder(new BevelBorder(BevelBorder.LOWERED));
      container.add(createButtonsPanel(), BorderLayout.SOUTH);
      container.validate();
  */
    } else
      displayErrorString("No data received");

  }

  public void itemStateChanged(ItemEvent e) {
      if(e.getSource() instanceof JCheckBox) {
    JCheckBox source = (JCheckBox) e.getSource();
    if(source.getActionCommand().equals(CDAY_MODE)) {
        query.setToCDays(e.getStateChange() == e.SELECTED);
    }
      }
  }

  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();

    if (command.equals(RESET)){
  query.resetChart();
    }
//     else if (command.equals(CLEAR))
//       ((InventoryChart)query.getChart()).setViewsVisible(false);
    else if (command.equals(UPDATE)) {
  doUpdate();
  //System.out.println("Should be doing update (QueryHelper::actionPerformed)");
    }
    else if (command.equals(SAVE)) {
      JFileChooser fc = new JFileChooser();
      //if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
        //query.save(fc.getSelectedFile());
    }
  }

    //  Invoked when a day rolls over from the universal time status
    //  handler.  Basically does the same as doUpdate(), except
    //  screen update is done through the event queue.
    public void dayRolloverUpdate(long date, double rate) {
  executeQuery();
  SwingUtilities.invokeLater(new ChartUpdatePost());
    }


    public class ChartUpdatePost implements Runnable {
  public synchronized void run() {
      updateChart();
  }
    }

   /* public static void windowOnInventory(UISimpleInventory inventory) {
  ThemeFactory.establishMetalTheme();
  InventoryQuery iq =
      new InventoryQuery(inventory);
  QueryHelper qh = new QueryHelper(iq);

  qh.frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
    System.exit(0);
      }});

    }*/
}




