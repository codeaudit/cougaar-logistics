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
package org.cougaar.logistics.ui.stoplight.client;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.*;

import org.cougaar.lib.uiframework.ui.components.CComboSelector;
import org.cougaar.lib.uiframework.ui.components.CDesktopFrame;
import org.cougaar.lib.uiframework.ui.components.CFrame;
import org.cougaar.lib.uiframework.ui.components.CNodeSelector;
import org.cougaar.lib.uiframework.ui.components.CRangeButton;
import org.cougaar.lib.uiframework.ui.components.CMThumbSliderThresholdControl;
import org.cougaar.lib.uiframework.ui.components.CRLabel;
import org.cougaar.lib.uiframework.ui.components.CSliderThresholdControl;
import org.cougaar.lib.uiframework.ui.components.CStoplightTable;
import org.cougaar.lib.uiframework.ui.components.CTreeButton;
import org.cougaar.lib.uiframework.ui.components.CViewFeatureSelectionControl;
import org.cougaar.lib.uiframework.ui.models.DatabaseTableModel;
import org.cougaar.lib.uiframework.ui.models.RangeModel;
import org.cougaar.lib.uiframework.ui.models.StoplightThresholdModel;
import org.cougaar.lib.uiframework.ui.models.TransformableTableModel;
import org.cougaar.lib.uiframework.ui.models.VariableModel;
import org.cougaar.lib.uiframework.ui.util.CougaarUI;
import org.cougaar.lib.uiframework.ui.util.SelectableHashtable;
import org.cougaar.lib.uiframework.ui.util.Selector;
import org.cougaar.lib.uiframework.ui.util.SliderControl;
import org.cougaar.lib.uiframework.ui.util.TableSorter;
import org.cougaar.lib.uiframework.ui.util.VariableInterfaceManager;

/**
 * Panel that contains a stoplight chart and controls for selecting what
 * data is viewed.  Controls pull and aggregate two dimensional cross
 * sections of data out of a database that holds a 4 dimensional data table.
 * The dimensions are: Item, Organization, Metric, and Time.
 */
public class StoplightPanel extends AssessmentUI
{
    private static final RangeModel DEFAULT_TIME_RANGE =
        new RangeModel(AssessmentDataSource.minTimeRange,
                       AssessmentDataSource.maxTimeRange);

    private boolean useMenuButtons = true;
    private final static int spacing = 5;
    private VariableInterfaceManager variableManager;
    private JLabel title = new JLabel("", JLabel.CENTER);
    private JLabel toLabel = new JLabel("Timeout = " + AssessmentDataSource.timeout + " ms", JLabel.CENTER);
    private CStoplightTable stoplightChart;
    private CMThumbSliderThresholdControl thresholdsPanel = null;
    private CComboSelector metricSelector = null;
    private CTreeButton itemTreeButton = null;
    private CTreeButton orgTreeButton = null;
    private CViewFeatureSelectionControl viewPanel = null;
    private UILaunchPopup uiLaunchPopup = new UILaunchPopup();
    private JCheckBoxMenuItem autoScale =
        new JCheckBoxMenuItem("Auto Scale", false);

    /**
     * Create a new stoplight panel
     */
    public StoplightPanel()
    {
        super();
        createComponents();
    }

    /**
     * Create a new stoplight panel
     *
     * @param useMenuButtons true if variable manager should use CMenuButtons
     *                       for variable management; otherwise CComboSelectors
     *                       will be used.
     */
    public StoplightPanel(boolean useMenuButtons)
    {
        super();
        this.useMenuButtons = useMenuButtons;
        createComponents();
    }

    /**
     * Creates the components of the stoplight UI
     */
    private void createComponents()
    {
        DefaultMutableTreeNode root = AssessmentDataSource.itemTree;
        itemTreeButton = new CTreeButton(root, root);

        root = AssessmentDataSource.orgTree;
        orgTreeButton = new CTreeButton(root, root);
        orgTreeButton.addIncludedControl(getIncludeSubordinatesControl());

        metricSelector =
            new CComboSelector(AssessmentDataSource.getAllMetrics().toArray());

        CRangeButton rangeButton =
            new CRangeButton("C", AssessmentDataSource.minTimeRange,
                             AssessmentDataSource.maxTimeRange);

        rangeButton.setSelectedItem(DEFAULT_TIME_RANGE);
        orgTreeButton.setSelectedItem(DEFAULT_ORG_NODE);
        //rangeButton.roundAndSetSliderRange(AssessmentDataSource.minTimeRange,
        //                                   AssessmentDataSource.maxTimeRange);

        VariableModel[] variables =
        {
            new VariableModel("Metric", metricSelector, false,
                              VariableModel.FIXED, true, 80),
            new VariableModel("Time", rangeButton, true,
                              VariableModel.X_AXIS, true, 0),
            new VariableModel("Item", itemTreeButton, true,
                              VariableModel.Y_AXIS, true, 0),
            new VariableModel("Org", orgTreeButton, true,
                              VariableModel.FIXED, true, 80) //,
            //new VariableModel("Location", new JTreeButton(), true),
            //new VariableModel("Pat Cond", new JTreeButton(), true)
        };

        //Border etchedBorder = BorderFactory.createEtchedBorder();
        Border emptyBorder =
           BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing);

        // issue a new query and update table view each time a variable
        // control is changed
        variableManager =
            new VariableInterfaceManager(variables, useMenuButtons);
        variableManager.addVariableListener(
            new VariableInterfaceManager.VariableListener() {
                public void variableChanged(VariableModel vm)
                {
                    updateView();
                }
                public void variablesSwapped(VariableModel vm1,
                                             VariableModel vm2)
                {
                    updateView();
                }
            });

        // stoplight settings panel
        viewPanel = new CViewFeatureSelectionControl(BoxLayout.Y_AXIS);
        viewPanel.setBorder(BorderFactory.createTitledBorder("View"));

        thresholdsPanel = new CMThumbSliderThresholdControl(0f, 5f);
        thresholdsPanel.setBorder(
            BorderFactory.createTitledBorder("Color Thresholds"));

        // generate initial query based on initial variable settings
        updateView();

        JPanel fixedVariablesPanel = new JPanel(new FlowLayout());
        fixedVariablesPanel.setBorder(
            BorderFactory.createTitledBorder("Fixed Variables"));
        Box fvBox = new Box(BoxLayout.X_AXIS);
        fvBox.add(variableManager.getDescriptor("Metric").getControl());
        fvBox.add(Box.createHorizontalStrut(spacing * 2));
        fvBox.add(variableManager.getDescriptor("Org").getControl());
        fixedVariablesPanel.add(fvBox);

        JPanel independentVariablesPanel = new JPanel(new FlowLayout());
        independentVariablesPanel.setBorder(
            BorderFactory.createTitledBorder("Independent Variables"));
        Box ivBox = new Box(BoxLayout.X_AXIS);
        Component xControl =
            ((VariableModel)variableManager.getDescriptors(
            VariableModel.X_AXIS).nextElement()).getControl();
        Component yControl =
            ((VariableModel)variableManager.getDescriptors(
            VariableModel.Y_AXIS).nextElement()).getControl();
        ivBox.add(new JLabel("X Axis: "));
        ivBox.add(xControl);
        ivBox.add(Box.createHorizontalStrut(spacing * 2));
        ivBox.add(new JLabel("Y Axis: "));
        ivBox.add(yControl);
        independentVariablesPanel.add(ivBox);


        // Create the chart and set chart controls
        JPanel stoplightPanel = new JPanel(new BorderLayout());
        CRLabel xAxisLabel = variableManager.getXAxisLabel();
        JPanel xAxisPanel = new JPanel(new GridBagLayout());
        xAxisPanel.add(xAxisLabel);
        stoplightPanel.add(xAxisPanel, BorderLayout.NORTH);
        CRLabel yAxisLabel = variableManager.getYAxisLabel();
        yAxisLabel.setOrientation(CRLabel.DOWN_UP);
        JPanel yAxisPanel = new JPanel(new GridBagLayout());
        yAxisPanel.add(yAxisLabel);
        stoplightPanel.add(yAxisPanel, BorderLayout.WEST);

        TableSorter sorter = new TableSorter(getTableModel());
        stoplightChart = new CStoplightTable(sorter);
        sorter.addMouseListenerToHeaderInTable(stoplightChart);
        JScrollPane scrolledStoplightChart = new JScrollPane(stoplightChart);
        //stoplightPanel.add(axisControlPanel, BorderLayout.NORTH);
        stoplightPanel.add(scrolledStoplightChart, BorderLayout.CENTER);
        stoplightPanel.setBorder(BorderFactory.createEtchedBorder());
        stoplightChart.setViewFeatureSelectionControl(viewPanel);
        viewPanel.setMode(CViewFeatureSelectionControl.VALUE);

        // associate threshold control with stoplight chart
        stoplightChart.setThresholds(thresholdsPanel.getThresholds());

        thresholdsPanel.addPropertyChangeListener("thresholds",
                                                  new PropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent e)
                {
                    stoplightChart.setThresholds((StoplightThresholdModel)
                                                 e.getNewValue());
                }
            });

        MouseListener doubleClickTableListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2)
                    {
                        doubleClickTableHandler(event);
                    }
                }
                public void mouseReleased(MouseEvent event)
                {
                    if (event.isPopupTrigger())
                    {
                        configurePopup(event);
                        uiLaunchPopup.show(
                            stoplightChart, event.getX(), event.getY());
                    }
                }
            };
        stoplightChart.addMouseListener(doubleClickTableListener);
        stoplightChart.getTableHeader().
            addMouseListener(doubleClickTableListener);
        stoplightChart.getRowHeader().
            addMouseListener(doubleClickTableListener);

        // high level layout
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(title, BorderLayout.CENTER);
        titlePanel.add(toLabel, BorderLayout.EAST);
        JPanel controlPanel = new JPanel(new BorderLayout());
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx=0;
        gbc.weighty=0;
        JPanel topControlPanel = new JPanel(gbl);
        gbc.fill=GridBagConstraints.BOTH;
        topControlPanel.add(getMonitorControl(), gbc);
        gbl.setConstraints(fixedVariablesPanel, gbc);
        topControlPanel.add(fixedVariablesPanel);
        gbc.weightx=1;
        gbc.weighty=1;
        gbl.setConstraints(thresholdsPanel, gbc);
        topControlPanel.add(thresholdsPanel);
        gbc.weightx=0;
        gbc.weighty=0;
        JPanel bottomControlPanel = new JPanel(gbl);
        gbl.setConstraints(independentVariablesPanel, gbc);
        bottomControlPanel.add(independentVariablesPanel);
        independentVariablesPanel.setPreferredSize(new Dimension(0,0));
        controlPanel.add(topControlPanel, BorderLayout.NORTH);
        controlPanel.add(bottomControlPanel, BorderLayout.SOUTH);
        add(titlePanel, BorderLayout.NORTH);
        add(stoplightPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    protected void populateMenuBar(JMenuBar mb)
    {
        //
        // Thresholds Menu
        //
        final JMenu thresholdsMenu = new JMenu("Thresholds");
        thresholdsMenu.setMnemonic('T');

        // threshold ranges
        int[] ranges = {1, 2, 3, 5, 10, 100, 1000};
        for (int i = 0; i < ranges.length; i++)
        {
            final int upperBound = ranges[i];
            JMenuItem range = new JMenuItem("0 to " + upperBound);
            thresholdsMenu.add(range);
            range.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        thresholdsPanel.setSliderRange(0, upperBound);
                        thresholdsPanel.evenlyDistributeValues();
                    }
                });
        }

        // Autoscale
        autoScale.setMnemonic('A');
        thresholdsMenu.add(autoScale);
        autoScale.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    updateThresholdExtents();
                }
            });

        // upper thresholds
        thresholdsMenu.add(new JSeparator());
        final JCheckBoxMenuItem upperThresholds =
            new JCheckBoxMenuItem("Upper Thresholds", true);
        upperThresholds.setMnemonic('U');
        thresholdsMenu.add(upperThresholds);
        upperThresholds.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    thresholdsPanel.
                        setUpperThresholds(upperThresholds.isSelected());
                }
            });

        // Attach menu as right-click popup for threshold sliders as alternate
        // point of access
        thresholdsPanel.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e)
                {
                    thresholdsMenu.getPopupMenu().
                        show(thresholdsPanel, e.getX(), e.getY());
                    thresholdsMenu.getPopupMenu().setInvoker(thresholdsMenu);
                }
            });

        mb.add(thresholdsMenu, 1);

        //
        // View Menu
        //
        JMenu viewMenu = viewPanel.convertToMenu("View");
        viewMenu.setMnemonic('V');

        mb.add(viewMenu, 1);

        //
        // Export Menu
        //
        JMenu exportMenu = new JMenu("Export");
        JMenuItem csvExp = new JMenuItem("CSV Format ...");
        csvExp.addActionListener(new FileSaveAction());
        exportMenu.add(csvExp);
        mb.add(exportMenu);

        super.populateMenuBar(mb);
    }

    protected void updateTimeout(long timeout) {
        AssessmentDataSource.timeout = timeout;
        toLabel.setText("Timeout = " + timeout + " ms");
    }

  private class FileSaveAction implements ActionListener {
    public void actionPerformed (ActionEvent ae) {
      String fileName = JOptionPane.showInputDialog(
        StoplightPanel.this, "Save table in file:", "Export to CSV format...",
        JOptionPane.QUESTION_MESSAGE);
      if (fileName == null) {
        return;
      }
      else if (fileName.length() == 0) {
        showErrorMessage("No file was specified.", "Error in CSV export ...");
      }
      else {
        try {
          TableModel tm = StoplightPanel.this.getTableModel();
          if (tm == null || tm.getColumnCount() == 0 || tm.getRowCount() == 0)
            showErrorMessage("Found no data to export (table is empty).", "Error in CSV export ...");
          else
            writeCsvFile(fileName, tm);
        }
        catch (Exception failed) {
          showErrorMessage("Can't write to file \"" + fileName + "\"", "Error in CSV export ...");
        }
      }
    }

    private String csvFormat (String s) {
      char[] chars = s.toCharArray();
      StringBuffer buf = new StringBuffer("\"");
      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];
        if (c == '"')
          buf.append(c);
        buf.append(c);
      }
      buf.append("\"");
      return buf.toString();
    }

    private void writeCsvFile (String f, TableModel t) throws Exception {
      PrintStream ps = new PrintStream(new FileOutputStream(new File(f)));
      int n_cols = t.getColumnCount();
      int n_rows = t.getRowCount();
      // print column headers
      for (int col = 0; col < n_cols; col++) {
        String str = t.getColumnName(col);
        if (str != null)
          ps.print(csvFormat(str));
        if (col < n_cols - 1)
          ps.print(",");
      }
      ps.println();
      for (int row = 0; row < n_rows; row++) {
        for (int col = 0; col < n_cols; col++) {
          Object val = t.getValueAt(row, col);
          if (val != null)
            ps.print(csvFormat(val.toString()));
          if (col < n_cols - 1)
            ps.print(",");
        }
        ps.println();
      }
      ps.close();
    }
  }

    protected String getSelectedMetric()
    {
        return metricSelector.getSelectedItem().toString();
    }

    protected void updateMetricSelector()
    {
        // update metrics in metric selector
        JComboBox tempCombo =
          new JComboBox(AssessmentDataSource.getAllMetrics());
        metricSelector.setModel(tempCombo.getModel());
    }

    protected void updateOrgSelector()
    {
        // update orgs in org selector
        orgTreeButton.setRoot(AssessmentDataSource.orgTree);
        orgTreeButton.setSelectedItem(DEFAULT_ORG_NODE);
    }

    protected void updateItemSelector()
    {
        // update items in item selector
        itemTreeButton.setRoot(AssessmentDataSource.itemTree);
    }

    public void updateView()
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        final Runnable postQueryRunnable = new Runnable() {
            public void run()
            {
              updateThresholdExtents();
              title.setText(variableManager.toString());
              setCursor(Cursor.getDefaultCursor());
            }
          };

         generateQuery(variableManager, postQueryRunnable);
    }

    private void doubleClickTableHandler(MouseEvent e)
    {
        if (e.getSource().equals(stoplightChart.getTableHeader()))
        {
            TableColumnModel columnModel = stoplightChart.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int hIndex =
                stoplightChart.convertColumnIndexToModel(viewColumn);
            Object value = getTableModel().getColumnName(hIndex);
            ((VariableModel)variableManager.
                getDescriptors(VariableModel.X_AXIS).nextElement()).
                getSelector().setSelectedItem(value);
            return;
        }

        if (e.getSource().equals(stoplightChart.getRowHeader()))
        {
            int row = stoplightChart.getRowHeader().getSelectedRow();
            Object value = getTableModel().getValueAt(row, 0);
            ((VariableModel)variableManager.
                getDescriptors(VariableModel.Y_AXIS).nextElement()).
                getSelector().setSelectedItem(value);

            return;
        }

        // Launch line plot chart preconfigured based on double-clicked cell
        configurePopup(e);
        uiLaunchPopup.launchUI(UIConstants.LINEPLOT_UI_NAME);
    }

    /**
     * Configure UILaunchPopup based on mouse position
     */
    private void configurePopup(MouseEvent e)
    {
        int row = stoplightChart.rowAtPoint(e.getPoint());
        int column = stoplightChart.convertColumnIndexToModel(
            stoplightChart.columnAtPoint(e.getPoint()));

        uiLaunchPopup.setInvoker(stoplightChart);

        // set stoplight's fixed variable values in new UI
        Enumeration fixedDescs =
            variableManager.getDescriptors(VariableModel.FIXED);
        while (fixedDescs.hasMoreElements())
        {
            VariableModel v =(VariableModel)fixedDescs.nextElement();
            String vName = v.getName();
            uiLaunchPopup.setConfigProperty(vName, v.getValue());
        }

        // set stoplight's y variable value in new UI
        VariableModel yDesc = (VariableModel)variableManager.
            getDescriptors(VariableModel.Y_AXIS).nextElement();
        String yDescName = yDesc.getName();
        Object selectedYValue = getTableModel().getValueAt(row, 0);
        if (yDescName.equals("Time"))
        {
            int selectedTime = Integer.parseInt(selectedYValue.toString());
            uiLaunchPopup.setConfigProperty(yDescName,
                new RangeModel(selectedTime - 10, selectedTime + 10));
        }
        else
        {
            uiLaunchPopup.setConfigProperty(yDescName, selectedYValue);
        }

        // set stoplight's x variable value in line plot
        VariableModel xDesc = (VariableModel)variableManager.
            getDescriptors(VariableModel.X_AXIS).nextElement();
        String xDescName = xDesc.getName();
        Object selectedXValue = getTableModel().getColumnName(column);
        if (xDescName.equals("Time"))
        {
            int selectedTime = Integer.parseInt(selectedXValue.toString());
            uiLaunchPopup.setConfigProperty(xDescName,
                new RangeModel(selectedTime - 10, selectedTime + 10));
        }
        else
        {
            uiLaunchPopup.setConfigProperty(xDescName, selectedXValue);
        }
    }

    /**
     * Returns the variable interface manager for this UI.  This can be used
     * to configure the selected values and roles of the dimension variables
     * (Item, Organization, Metric, and Time) programmatically.
     *
     * @return the variable interface manager for this UI
     */
    public VariableInterfaceManager getVariableInterfaceManager()
    {
        return variableManager;
    }

    private void updateThresholdExtents()
    {
        if (autoScale.isSelected())
        {
            // find minimum and maximum values in table
            float minValue = Float.MAX_VALUE;
            float maxValue = Float.MIN_VALUE;
            TableModel stoplightTableModel = getTableModel();
            for (int row = 0; row < stoplightTableModel.getRowCount(); row++)
            {
                for (int column = 1;
                     column < stoplightTableModel.getColumnCount(); column++)
                {
                    Object valueObj =
                        stoplightTableModel.getValueAt(row, column);
                    if (valueObj instanceof Number)
                    {
                        float value = ((Number)valueObj).floatValue();
                        minValue = Math.min(minValue, value);
                        maxValue = Math.max(maxValue, value);
                    }
                }
            }

            float newShift =
                thresholdsPanel.roundAndSetSliderRange(minValue, maxValue);
            // if new slider range was modified by an exponential amount,
            // redistribute threshold values
            if (newShift != shift)
            {
                thresholdsPanel.evenlyDistributeValues();
                shift = newShift;
            }
        }
    }
    private float shift = 0;

    /**
     * Main for unit testing.
     *
     * @param args ignored
     */
    public static void main(String[] args)
    {
        if ((System.getProperty("DBTYPE") == null) ||
            (System.getProperty("DBURL") == null) ||
            (System.getProperty("DBUSER") == null) ||
            (System.getProperty("DBPASSWORD") == null))
        {
            System.out.println("You need to set the following system property"+
                               " variables:  DBTYPE, DBURL, DBUSER, and " +
                               "DBPASSWORD");
            return;
        }

        boolean plaf = Boolean.getBoolean("PLAF");
        CFrame frame = new CFrame(UIConstants.STOPLIGHT_UI_NAME, plaf);
        StoplightPanel slp = new StoplightPanel(plaf);
        slp.getVariableInterfaceManager().getDescriptor("Metric").
            setValue("Supply as Proportion of Demand");
        slp.install(frame);
        frame.setVisible(true);
    }
}