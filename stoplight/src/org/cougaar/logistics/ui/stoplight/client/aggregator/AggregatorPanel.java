/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.client.aggregator;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;

import org.cougaar.lib.aggagent.client.AlertMonitor;
import org.cougaar.lib.aggagent.client.AggregationClient;
import org.cougaar.lib.aggagent.client.ResultSetMonitor;
import org.cougaar.lib.aggagent.client.ResultSetTableModel;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.query.UpdateListener;
import org.cougaar.lib.aggagent.util.Enum.*;


import org.cougaar.logistics.ui.stoplight.client.MonitorControl;
import org.cougaar.logistics.ui.stoplight.ui.components.CFrame;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;
import org.cougaar.logistics.ui.stoplight.ui.util.TableMap;
import org.cougaar.logistics.ui.stoplight.ui.util.TableSorter;

public class AggregatorPanel extends JPanel implements CougaarUI
{
  private final static int spacing = 10;
  private static Border emptyBorder =
    BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing);
  private final static int monitorPeriod = 5; // sec
  private static String DEFAULT_SERVER_URL="http://localhost:8800/$Aggregator";
  private static String PSP = "aggregator";
  private static String KEEP_ALIVE_PSP = "aggregatorkeepalive";

  private AggregationClient pspInterface = null;
  private ResultSetMonitor resultSetMonitor = null;
  private AlertMonitor alertMonitor = null;

  private QueryTableModel queryTableModel;
  private AlertTableModel alertTableModel;
  private JTable queryTable;
  private QueryEditor queryForm;
  private JPanel alertPanel;
  private AlertEditor alertEditor;
  private ResultSetTableModel resultSetTM;
  private JTabbedPane tp;

  private Thread shutdownHook = new Thread() {
      public void run()
      {
        AggregatorPanel.this.finalize();
      }
    };

  public void finalize()
  {
    // Free resources on the aggregation agent before leaving.
    resultSetMonitor.cancel();
    alertMonitor.cancel();

    if (!shutdownHook.isAlive())
    {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
  }

  private void initPspClient (String url) {
    pspInterface = new AggregationClient(url, PSP, KEEP_ALIVE_PSP);

    // using keep alive monitors; therefore, no monitor period
    resultSetMonitor = pspInterface.createResultSetMonitor(/*monitorPeriod*/);
    alertMonitor = pspInterface.createAlertMonitor(/*monitorPeriod*/);
  }

  public AggregatorPanel()
  {
    super(new BorderLayout());
    initPspClient(DEFAULT_SERVER_URL);
    createComponents();

    // Make sure that finalizer is called upon program exit.
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public AggregatorPanel (String serverUrl) {
    super(new BorderLayout());
    initPspClient(serverUrl);
    createComponents();
  }

  public boolean supportsPlaf()
  {
    return true;
  }

  public void install(JInternalFrame f)
  {
    f.getContentPane().add(this);
  }

  public void install(JFrame f)
  {
    f.getContentPane().add(this);
  }

  public JPanel makeQueryPanel () {
    JPanel queryPanel = new JPanel(new BorderLayout(spacing, spacing));
    queryPanel.setBorder(emptyBorder);
    queryForm = new QueryEditor(pspInterface.getClusterIds());
    queryPanel.add(queryForm, BorderLayout.CENTER);
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(Box.createGlue());
    JButton createQueryButton = new JButton("Create New Query");
    buttonPanel.add(createQueryButton);
    buttonPanel.add(Box.createGlue());
    queryPanel.add(buttonPanel, BorderLayout.SOUTH);
    tp.add("Query", queryPanel);

    createQueryButton.addActionListener(
      new ActionListener() {
        public void actionPerformed (ActionEvent e) {
          AggregationQuery aq = queryForm.createQuery();
          Object response = pspInterface.createQuery(aq);
          if (aq.getType() == QueryType.TRANSIENT) {
            queryTable.clearSelection();
            tp.setSelectedIndex(2);
            AggregationResultSet resultSet = (AggregationResultSet) response;
            resultSetTM.setResultSet(resultSet);
          }
          else {
            int queryRow = queryTableModel.addQuery(
              new QueryResultAdapter(aq, response.toString()));
            queryTable.setRowSelectionInterval(queryRow, queryRow);
          }
        }
      }
    );

    return queryPanel;
  }

  public void createComponents () {
    JLabel title = new JLabel("Aggregation PSP Interface", JLabel.CENTER);

    // persistent query panel
    JPanel persistentQueryPanel = new JPanel(new BorderLayout());
    persistentQueryPanel.setBorder(
      BorderFactory.createTitledBorder("Active Persistent Queries"));
    Collection activeQueries = pspInterface.getActiveQueries();
    queryTableModel = new QueryTableModel();
    for (Iterator i = activeQueries.iterator(); i.hasNext();)
      queryTableModel.addQuery((QueryResultAdapter)i.next());
    TableSorter sortedQueryTableModel = new TableSorter(queryTableModel);
    queryTable = new JTable(sortedQueryTableModel);
    sortedQueryTableModel.addMouseListenerToHeaderInTable(queryTable);
    TableCellButton cancelButton = new TableCellButton("Cancel");
    queryTable.setDefaultRenderer(Boolean.class,new TableCellButton("Cancel"));
    queryTable.setDefaultEditor(Boolean.class, cancelButton);
    TableCellButton updateButton = new TableCellButton("Update");
    queryTable.setDefaultRenderer(Integer.class,new TableCellButton("Update"));
    queryTable.setDefaultEditor(Integer.class, updateButton);
    queryTable.setRowHeight(queryTable.getRowHeight() * 2);
    queryTable.getColumnModel().getColumn(0).setPreferredWidth(5);
    JScrollPane scrolledList = new JScrollPane(queryTable);
    scrolledList.setPreferredSize(
      new Dimension(250, scrolledList.getPreferredSize().height));
    persistentQueryPanel.add(scrolledList, BorderLayout.CENTER);

    // main alert list
    JPanel mainAlertPanel = new JPanel(new BorderLayout());
    mainAlertPanel.setBorder(
      BorderFactory.createTitledBorder("Active Alerts"));
    alertMonitor.monitorAllObjects();
    alertTableModel = new AlertTableModel(alertMonitor);
    TableSorter sortedAlertTableModel = new TableSorter(alertTableModel);
    AlertTable alertTable = new AlertTable(sortedAlertTableModel);
    sortedAlertTableModel.addMouseListenerToHeaderInTable(alertTable);
    TableCellButton cancelAlertButton = new TableCellButton("Cancel");
    alertTable.setDefaultEditor(JButton.class, cancelAlertButton);
    //alertTable.getColumnModel().getColumn(0).setPreferredWidth(5);
    scrolledList = new JScrollPane(alertTable);
    scrolledList.setPreferredSize(
      new Dimension(250, scrolledList.getPreferredSize().height));
    mainAlertPanel.add(scrolledList, BorderLayout.CENTER);


    //
    // tabbed pane
    //
    tp = new JTabbedPane();
    tp.setBorder(BorderFactory.createTitledBorder("Query"));

    // query panel
    /*
    JPanel queryPanel = new JPanel(new BorderLayout(spacing, spacing));
    queryPanel.setBorder(emptyBorder);
    queryForm = new QueryEditor(pspInterface.getClusterIds());
    queryPanel.add(queryForm, BorderLayout.CENTER);
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(Box.createGlue());
    JButton createQueryButton = new JButton("Create New Query");
    buttonPanel.add(createQueryButton);
    buttonPanel.add(Box.createGlue());
    queryPanel.add(buttonPanel, BorderLayout.SOUTH);
    tp.add("Query", queryPanel);
    */
    JPanel queryPanel = makeQueryPanel();

    // alert panel
    alertPanel = new JPanel(new BorderLayout(spacing, spacing));
    alertPanel.setBorder(emptyBorder);
    JPanel alertList = new JPanel(new BorderLayout());
    alertList.setBorder(
      BorderFactory.createTitledBorder("Active Alerts for Selected Query"));
    final FilteredAlertTableModel singleQueryAlertTM =
      new FilteredAlertTableModel(alertTableModel);
    TableSorter sortedSingleQueryAlertTM = new TableSorter(singleQueryAlertTM);
    AlertTable queryAlertTable = new AlertTable(sortedSingleQueryAlertTM);
    sortedSingleQueryAlertTM.addMouseListenerToHeaderInTable(queryAlertTable);
    queryAlertTable.setDefaultEditor(JButton.class, cancelAlertButton);
    alertList.add(new JScrollPane(queryAlertTable), BorderLayout.CENTER);
    alertPanel.add(alertList, BorderLayout.CENTER);
    JPanel alertForm = new JPanel(new BorderLayout(spacing, spacing));
    alertForm.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Alert Editor"), emptyBorder));
    alertEditor = new AlertEditor();
    alertForm.add(alertEditor, BorderLayout.CENTER);
    JPanel alertButtonPanel = new JPanel();
    alertButtonPanel.setLayout(
      new BoxLayout(alertButtonPanel, BoxLayout.X_AXIS));
    alertButtonPanel.add(Box.createGlue());
    JButton createAlertButton = new JButton("Create New Alert");
    alertButtonPanel.add(createAlertButton);
    alertButtonPanel.add(Box.createGlue());
    alertForm.add(alertButtonPanel, BorderLayout.SOUTH);
    alertPanel.add(alertForm, BorderLayout.SOUTH);
    tp.add("Alerts", alertPanel);

    // result set panel
    JPanel resultSetForm = new JPanel(new BorderLayout());
    resultSetForm.setBorder(emptyBorder);
    // result set table
    resultSetTM = new ResultSetTableModel();
    TableSorter ts = new TableSorter(resultSetTM);
    JTable rsTable = new JTable(ts);
    ts.addMouseListenerToHeaderInTable(rsTable);
    JScrollPane resultSetTable = new JScrollPane(rsTable);
    // exception table
    TableModel exceptionTM = new ExceptionTableModel(resultSetTM);
    ts = new TableSorter(exceptionTM);
    JTable exTable = new ExceptionTable(ts);
    ts.addMouseListenerToHeaderInTable(exTable);
    JScrollPane exceptionTable = new JScrollPane(exTable);
    exceptionTable.setMinimumSize(new Dimension(0,0));
    //exceptionTable.setPreferredSize(new Dimension(0,0));
    // control panel
    JPanel resultSetControlPanel = new JPanel();
    resultSetControlPanel.setBorder(emptyBorder);
    resultSetControlPanel.
      setLayout(new BoxLayout(resultSetControlPanel, BoxLayout.X_AXIS));
    resultSetControlPanel.add(Box.createGlue());
    final MonitorControl monitorControl = new MonitorControl();
    resultSetControlPanel.add(monitorControl);
    resultSetControlPanel.add(Box.createGlue());
    JButton updateResultSetButton = new JButton("Update");
    resultSetControlPanel.add(updateResultSetButton);
    resultSetControlPanel.add(Box.createGlue());
    JSplitPane tablePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                          resultSetTable, exceptionTable);
    tablePane.setDividerLocation(Integer.MAX_VALUE);
    tablePane.setResizeWeight(1);
    resultSetForm.add(tablePane, BorderLayout.CENTER);
    resultSetForm.add(resultSetControlPanel, BorderLayout.SOUTH);
    tp.add("Result Set", resultSetForm);

    // high level layout
    add(title, BorderLayout.NORTH);
    JPanel mainActiveListPanel = new JPanel(new GridLayout(2, 1));
    mainActiveListPanel.add(persistentQueryPanel);
    mainActiveListPanel.add(mainAlertPanel);
    add(mainActiveListPanel, BorderLayout.WEST);
    add(tp, BorderLayout.CENTER);

    setEnabled();

    // event handling
    queryTable.getSelectionModel().addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e)
        {
          QueryResultAdapter qra = getSelectedAdapter();

          if (qra != null)
          {
            queryForm.setFromQuery(qra.getQuery());
            singleQueryAlertTM.setQueryId(qra.getID());
            resultSetTM.setResultSet(qra.getResultSet());
            monitorControl.putClientProperty("IGNORE", Boolean.TRUE);
            monitorControl.setMonitoring(
              resultSetMonitor.isMonitoring(qra.getID()));
          }
          else
          {
            singleQueryAlertTM.setQueryId(null);
            resultSetTM.setResultSet(null);
          }
          setEnabled();
        }
      });

    queryTable.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e)
        {
          if (e.getClickCount() == 1)
          {
            updateResultSet();
          }
        }
      });

    cancelButton.addButtonEditorListener(
      new TableCellButton.ButtonEditorListener() {
        public void buttonPressed(JTable table, int row)
        {
          int idColumn =
            table.convertColumnIndexToView(queryTableModel.QUERY_ID_COLUMN);
          String queryId = table.getValueAt(row, idColumn).toString();
          QueryResultAdapter qra = queryTableModel.getQuery(queryId);
          pspInterface.cancelQuery(qra.getID());
          queryTableModel.removeQuery(queryId);
        }
      });

    updateButton.addButtonEditorListener(
      new TableCellButton.ButtonEditorListener() {
        public void buttonPressed(JTable table, int row)
        {
          int idColumn =
            table.convertColumnIndexToView(queryTableModel.QUERY_ID_COLUMN);
          String queryId = table.getValueAt(row, idColumn).toString();
          QueryResultAdapter qra = queryTableModel.getQuery(queryId);
          qra.updateClusters(queryForm.getSourceClusters());
          pspInterface.updateQuery(qra);
        }
      });
      
    /*
    createQueryButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          AssessmentQuery aq = queryForm.createQuery();
          Object response = pspInterface.createQuery(aq);
          if (aq.getType() == QueryType.TRANSIENT)
          {
            queryTable.clearSelection();
            tp.setSelectedIndex(2);
            AssessmentResultSet resultSet = (AssessmentResultSet)response;
            resultSetTM.setResultSet(resultSet);
          }
          else
          {
            int queryRow = queryTableModel.addQuery(
              new QueryResultAdapter(aq, response.toString()));
            queryTable.setRowSelectionInterval(queryRow, queryRow);
          }
        }
      });
    */

    createAlertButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          AlertDescriptor ad = alertEditor.createAlertDescriptor();
          QueryResultAdapter qra = getSelectedAdapter();
          ad.setQueryId(qra.getID());
          boolean success = pspInterface.createAlert(ad);
          if (success)
          {
            qra.addAlert(ad);
          }
        }
      });

    cancelAlertButton.addButtonEditorListener(
      new TableCellButton.ButtonEditorListener() {
        public void buttonPressed(JTable table, int row)
        {
          int tableIdColumn =
            table.convertColumnIndexToView(AlertTableModel.QUERY_ID_COLUMN);
          int tableNameColumn =
            table.convertColumnIndexToView(AlertTableModel.ALERT_NAME_COLUMN);

          // cancel on aggregation agent
          String queryId = table.getValueAt(row, tableIdColumn).toString();
          String alertName = table.getValueAt(row, tableNameColumn).toString();
          pspInterface.cancelAlert(queryId, alertName);
        }
      });

    updateResultSetButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          updateResultSet();
        }
      });

    monitorControl.addPropertyChangeListener("monitoring",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e)
        {
          Boolean ignore =(Boolean)monitorControl.getClientProperty("IGNORE");
          if ((ignore != null) && (ignore.booleanValue()))
          {
            monitorControl.putClientProperty("IGNORE", Boolean.FALSE);
          }
          else
          {
            QueryResultAdapter qra = getSelectedAdapter();
            if (qra != null)
            {
              if (monitorControl.isMonitoring())
              {
                AggregationResultSet monitoredResultSet =
                  resultSetMonitor.monitorResultSet(qra.getID());
                qra.setResultSet(monitoredResultSet);
                resultSetTM.setResultSet(monitoredResultSet);
              }
              else
              {
                resultSetMonitor.stopMonitoringResultSet(qra.getID());
              }
            }
          }
        }
      });

    // Detect remove events when monitoring a result set.  Upon reception of
    // such an event, remove associated query from query table
    resultSetMonitor.addUpdateListener(new UpdateListener() {
        public void objectAdded(Object sourceObject){};
        public void objectChanged(Object sourceObject) {};
        public void objectRemoved(Object sourceObject)
        {
          AggregationResultSet removedResultSet =
            (AggregationResultSet)sourceObject;
          queryTableModel.removeQuery(
            removedResultSet.getQueryAdapter().getID());
        }
      });
  }

  private void setEnabled()
  {
    setEnabled(alertPanel, getSelectedAdapter() != null);
  }

  private void updateResultSet()
  {
    QueryResultAdapter qra = getSelectedAdapter();
    if (qra != null)
    {
      AggregationResultSet rs = pspInterface.getUpdatedResultSet(qra.getID());

      // if query was not found, remove it from list
      if (rs == null)
      {
        queryTableModel.removeQuery(qra.getID());
      }
      else
      {
        qra.getResultSet().update(rs);
      }
    }
  }

  private QueryResultAdapter getSelectedAdapter()
  {
    QueryResultAdapter qra = null;

    if (queryTable.getSelectedRowCount() != 0)
    {
      int selectedRow = queryTable.getSelectedRow();
      int idColumn =
        queryTable.convertColumnIndexToView(QueryTableModel.QUERY_ID_COLUMN);
      String queryId = queryTable.getValueAt(selectedRow, idColumn).toString();
      qra = queryTableModel.getQuery(queryId);
    }

    return qra;
  }

  private static void setEnabled(Component c, boolean enabled)
  {
    c.setEnabled(enabled);
    if (c instanceof Container)
    {
      Container cont = (Container)c;
      for (int i = 0; i < cont.getComponentCount(); i++)
        setEnabled(cont.getComponent(i), enabled);
    }
  }

  public static void main(String[] args)
  {
    CFrame f = new CFrame("Aggregation PSP Interface", true);

    AggregatorPanel ap = null;
    if (args.length > 0)
      ap = new AggregatorPanel(args[0]);
    else
      ap = new AggregatorPanel();

    ap.install(f);
    f.show();
  }

  private static class QueryTableModel extends AbstractTableModel
  {
    private static final int QUERY_ID_COLUMN = 0;
    private static final int QUERY_NAME_COLUMN = 1;
    private static final int CANCEL_COLUMN = 2;
    private static final int UPDATE_COLUMN = 3;
    private String[] columnHeaders = {"Id", "Name", "Cancel", "UpdateClusters"};
    private Vector queries = new Vector();

    public int addQuery(QueryResultAdapter qra)
    {
      queries.add(qra);
      int rowIndex = getRowCount() - 1;
      fireTableRowsInserted(rowIndex, rowIndex);
      return rowIndex;
    }

    public void removeQuery(int queryIndex)
    {
      queries.remove(queryIndex);
      fireTableRowsDeleted(queryIndex, queryIndex);
    }

    public QueryResultAdapter getQueryAt(int row)
    {
      return (QueryResultAdapter)queries.elementAt(row);
    }

    public QueryResultAdapter getQuery(String queryId)
    {
      for (Iterator i = queries.iterator(); i.hasNext();)
      {
        QueryResultAdapter qra = (QueryResultAdapter)i.next();
        if (qra.checkID(queryId))
        {
          return qra;
        }
      }
      return null;
    }

    public void removeQuery(String queryId)
    {
      queries.remove(getQuery(queryId));
      fireTableDataChanged();
    }

    public int getRowCount()
    {
      return queries.size();
    }

    public int getColumnCount()
    {
      return columnHeaders.length;
    }

    public String getColumnName(int column)
    {
      return columnHeaders[column];
    }

    public Class getColumnClass(int column)
    {
      if (column == CANCEL_COLUMN)
        return Boolean.class;
      else if (column == UPDATE_COLUMN)
        return Integer.class;
        
      return String.class;
    }

    public Object getValueAt(int row, int column)
    {
      Object value = null;
      QueryResultAdapter qra = (QueryResultAdapter)queries.elementAt(row);
      switch (column)
      {
        case QUERY_ID_COLUMN:
          value = qra.getID();
          break;
        case QUERY_NAME_COLUMN:
          value = qra.getQuery().getName();
          break;
        case CANCEL_COLUMN:
          value = Boolean.TRUE;
          break;
        case UPDATE_COLUMN:
          value = new Integer(1);
      }

      return value;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return (columnIndex == CANCEL_COLUMN || columnIndex == UPDATE_COLUMN);
    }
  }

  private static class AlertTableModel extends AbstractTableModel
  {
    public static final int QUERY_ID_COLUMN = 0;
    public static final int ALERT_NAME_COLUMN = 1;
    public static final int CANCEL_COLUMN = 2;
    public static final int TRIGGERED_COLUMN = 3;
    private String[] columnHeaders =
      {"Query Id", "Alert Name", "Cancel", "Triggered"};
    private AlertMonitor alertMonitor;

    public AlertTableModel(AlertMonitor alertMonitor)
    {
      this.alertMonitor = alertMonitor;

      // anytime the alert monitor changes, fire a table update
      alertMonitor.addUpdateListener(new UpdateListener() {
          public void objectAdded(Object sourceObject)
          {
            fireTableDataChanged();
          }
          public void objectRemoved(Object sourceObject)
          {
            fireTableDataChanged();
          }
          public void objectChanged(Object sourceObject)
          {
            fireTableDataChanged();
          }
        });
    }

    public int getRowCount()
    {
      return alertMonitor.getMonitoredObjects().size();
    }

    public int getColumnCount()
    {
      return columnHeaders.length;
    }

    public String getColumnName(int column)
    {
      return columnHeaders[column];
    }

    public Class getColumnClass(int column)
    {
      if (column == CANCEL_COLUMN)
        return JButton.class;
      else if (column == TRIGGERED_COLUMN)
        return Boolean.class;

      return String.class;
    }

    private static JButton dummyButton = new JButton();
    public Object getValueAt(int row, int column)
    {
      Object value = null;
      Object[] alerts = alertMonitor.getMonitoredObjects().toArray();
      AlertDescriptor alert = (AlertDescriptor)alerts[row];
      switch (column)
      {
        case QUERY_ID_COLUMN:
          value = alert.getQueryId();
          break;
        case ALERT_NAME_COLUMN:
          value = alert.getName();
          break;
        case CANCEL_COLUMN:
          value = dummyButton;
          break;
        case TRIGGERED_COLUMN:
          value = new Boolean(alert.isAlerted());
      }

      return value;
    }

    public void setValueAt(Object aValue, int row, int column) {}

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return (columnIndex == CANCEL_COLUMN);
    }
  }

  private static class FilteredAlertTableModel extends TableMap
  {
    private static final int queryColumn = 0;
    private String queryId = null;
    private Vector indexes = new Vector();

    public FilteredAlertTableModel(TableModel model)
    {
      setModel(model);
    }

    public void setModel(TableModel model)
    {
      super.setModel(model);
      tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    public void setQueryId(String queryId)
    {
      this.queryId = queryId;
      tableChanged(new TableModelEvent(this));
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".

    public Object getValueAt(int aRow, int aColumn) {
        return model.getValueAt(((Integer)indexes.elementAt(aRow)).intValue(),
                                aColumn);
    }

    public void setValueAt(Object aValue, int aRow, int aColumn) {
        model.setValueAt(aValue, ((Integer)indexes.elementAt(aRow)).intValue(),
                         aColumn);
    }

    public int getRowCount() {
        return indexes.size();
    }

    public void tableChanged(TableModelEvent e) {
        filter();
        super.tableChanged(e);
    }

    private void filter()
    {
      Vector workingindexes = new Vector();
      for (int row = 0; row < model.getRowCount(); row++)
      {
        boolean filterRecord = filter(row);

        if (!filterRecord)
        {
          workingindexes.add(new Integer(row));
        }
      }

      indexes = workingindexes;
    }

    private boolean filter(int row)
    {
      String subjectQueryId = (String)model.getValueAt(row, queryColumn);
      return !subjectQueryId.equals(queryId);
    }
  }

  private static class ExceptionTableModel extends AbstractTableModel
  {
    private static final int CLUSTER_ID_COLUMN = 0;
    private static final int EXCEPTION_COLUMN = 1;
    private String[] columnHeaders = {"Cluster Id", "Last Exception Thrown"};
    ResultSetTableModel resultSetTableModel = null;

    public ExceptionTableModel(ResultSetTableModel resultSetTableModel)
    {
      this.resultSetTableModel = resultSetTableModel;

      resultSetTableModel.addTableModelListener(new TableModelListener() {
          public void tableChanged(TableModelEvent e)
          {
            fireTableDataChanged();
          }
        });
    }

    public int getRowCount()
    {
      Map exceptionMap = getExceptionMap();
      return (exceptionMap == null) ? 0 : exceptionMap.size();
    }

    public int getColumnCount()
    {
      return columnHeaders.length;
    }

    public String getColumnName(int column)
    {
      return columnHeaders[column];
    }

    public Object getValueAt(int row, int column)
    {
      Object value = null;
      Map.Entry mapEntry =
        (Map.Entry)getExceptionMap().entrySet().toArray()[row];

      switch (column)
      {
        case CLUSTER_ID_COLUMN:
          value = mapEntry.getKey();
          break;
        case EXCEPTION_COLUMN:
          value = mapEntry.getValue();
      }

      return value;
    }

    private Map getExceptionMap()
    {
      if ((resultSetTableModel != null) &&
          (resultSetTableModel.getResultSet() != null))
      {
        return resultSetTableModel.getResultSet().getExceptionMap();
      }
      return null;
    }
  }

  /**
   * used to create table of alerts with colored rows
   */
  private static class AlertTable extends ColoredCellTable
  {
    private static final int triggeredColumn =AlertTableModel.TRIGGERED_COLUMN;

    public AlertTable(TableModel tm)
    {
      super(tm);

      setRowHeight(getRowHeight() * 2);
      setRowSelectionAllowed(false);
      setCellSelectionEnabled(false);
    }

    protected void colorRenderer(Component comp, boolean isSelected,
                               JTable table, int row, int column)
    {
      int modelColumn = table.convertColumnIndexToView(triggeredColumn);
      if (((Boolean)table.getValueAt(row, modelColumn)).booleanValue())
      {
        comp.setBackground(Color.red);
        //comp.setForeground(Color.white);
      }
      else
      {
        super.colorRenderer(comp, isSelected, table, row, column);
      }
    }
  }

  public static class ExceptionTable extends JTable
  {
    private TableCellRenderer areaRenderer = new TextAreaRenderer();

    public ExceptionTable (TableModel tm)
    {
      super(tm);
      getColumnModel().getColumn(
        ExceptionTableModel.CLUSTER_ID_COLUMN).setPreferredWidth(100);
      getColumnModel().getColumn(
        ExceptionTableModel.EXCEPTION_COLUMN).setPreferredWidth(500);
    }

    public TableCellRenderer getCellRenderer(int row, int column)
    {
      return areaRenderer;
    }

    private class TextAreaRenderer extends JTextArea
      implements TableCellRenderer
    {
        public TextAreaRenderer()
        {
            super();
            this.setFont(new Font("sans serif", Font.PLAIN, 12));
            this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        public Component
            getTableCellRendererComponent(JTable table, Object text,
                                          boolean isSelected, boolean hasFocus,
                                          int row, int column) {
            setText(text.toString().trim());

            if (convertColumnIndexToModel(column) ==
                ExceptionTableModel.EXCEPTION_COLUMN)
            {
              int textHeight = this.getPreferredSize().height;
              if (ExceptionTable.this.getRowHeight(row) != textHeight)
                setRowHeight(row, textHeight);
            }
            return this;
        }
    }
  }
}
