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

import java.io.File;
import java.io.InputStream;
import java.util.Vector;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JSplitPane;

import org.cougaar.logistics.ui.stoplight.ui.components.CChartLegend;


/**
 * Define an interface that must be implemented to add a query
 * to the user interface.
 */

public interface Query {

  /* Get the query to send to the cluster.
   */

  String getQueryToSend();

  /* Read the reply sent from the clusters.
   */

  void readReply(InputStream is);

  /* Save the reply sent from the clusters.
   */

  void save(File file);

  /* Create chart.
     */

  JPanel createChart(String title, JSplitPane split);

  boolean setChartData(String title, BlackJackInventoryChart chart, CChartLegend legend);

  /* Create table.
   */

  JPanel reinitializeAndUpdateChart(String title);
    /* Reinit Chart
     */

  JTable createTable(String title);

  void buildTableModel();

  boolean setTableData(String title, JTable table, CChartLegend legend);

  /** Get chart created.
   */

//  JCChart getChart();

  void resetChart();

  void setToCDays(boolean useCDays);

  /* Get the identifier of the PlanServiceProvider (PSP)
     that this client wants to communicate with.
   */

  String getPSP_id();

}

