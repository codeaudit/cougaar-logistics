/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.logistics.ui.stoplight.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.*;
import javax.swing.table.TableModel;
import javax.swing.tree.*;

import org.cougaar.lib.uiframework.ui.components.CChartLegend;
import org.cougaar.lib.uiframework.ui.components.CFrame;
import org.cougaar.lib.uiframework.ui.components.CGraphFeatureSelectionControl;
import org.cougaar.lib.uiframework.ui.components.CLinePlotChart;
import org.cougaar.lib.uiframework.ui.components.CRangeButton;
import org.cougaar.lib.uiframework.ui.components.CRLabel;
import org.cougaar.lib.uiframework.ui.components.CRowHeaderTable;
import org.cougaar.lib.uiframework.ui.components.CTreeButton;
import org.cougaar.lib.uiframework.ui.models.DatabaseTableModel;
import org.cougaar.lib.uiframework.ui.models.RangeModel;
import org.cougaar.lib.uiframework.ui.models.VariableModel;
import org.cougaar.lib.uiframework.ui.util.CougaarUI;
import org.cougaar.lib.uiframework.ui.util.SelectableHashtable;
import org.cougaar.lib.uiframework.ui.util.TableSorter;
import org.cougaar.lib.uiframework.ui.util.VariableInterfaceManager;

/**
 * Panel that contains a line plot chart and controls for selecting what
 * data is plotted.  Controls pull and aggregate two dimensional cross
 * sections of data out of a database that holds a 4 dimensional data table.
 * The dimensions are: Item, Organization, Metric, and Time.
 */
public class LinePlotPanel extends AssessmentUI
{
    private static final String DEFAULT_ITEM_NODE = "MEAL READY-TO-EAT";
    private static final String DEFAULT_ITEM_CODE = "NSN/8970001491094";
    private static final RangeModel DEFAULT_TIME_RANGE =
        new RangeModel(AssessmentDataSource.minTimeRange,
                       AssessmentDataSource.maxTimeRange);

    private boolean useMenuButtons = true;
    private CTreeButton itemTreeButton = null;
    private CTreeButton orgTreeButton = null;
    private CTreeButton metricTreeButton = null;
    private CLinePlotChart chart = null;
    private CChartLegend legend = new CChartLegend();
    private final static int spacing = 5;
    private VariableInterfaceManager variableManager;

    /**
     * Creates new line plot panel.
     */
    public LinePlotPanel()
    {
        this(true);
    }

    /**
     * Creates new line plot panel in given frame.
     *
     * @param useMenuButtons true if variable manager should use CMenuButtons
     *                       for variable management; otherwise CComboSelectors
     *                       will be used.
     */
    public LinePlotPanel(boolean useMenuButtons)
    {
        this(useMenuButtons, false);
    }

    /**
     * Creates new line plot panel in given frame.
     *
     * @param useMenuButtons true if variable manager should use CMenuButtons
     *                       for variable management; otherwise CComboSelectors
     *                       will be used.
     * @param noInitialQuery true if initial query should not be made
     */
    public LinePlotPanel(boolean useMenuButtons, boolean noInitialQuery)
    {
        super();
        this.useMenuButtons = useMenuButtons;

        setIgnoreQueries(noInitialQuery);
        createComponents();
        setIgnoreQueries(false);
    }

    /**
     * Creates the components of the line plot UI
     */
    private void createComponents()
    {
        DefaultMutableTreeNode root = AssessmentDataSource.makeMetricTree();
        metricTreeButton =
            new CTreeButton(root,
                            (DefaultMutableTreeNode)root.getChildAt(0));
        metricTreeButton.setRootVisible(false);
        metricTreeButton.expandFirstLevel();

        root = AssessmentDataSource.itemTree;
        itemTreeButton = new CTreeButton(root, root);

        root = AssessmentDataSource.orgTree;
        orgTreeButton = new CTreeButton(root, root);
        orgTreeButton.addIncludedControl(getIncludeSubordinatesControl());

        CRangeButton rangeButton =
            new CRangeButton("C", AssessmentDataSource.minTimeRange,
                             AssessmentDataSource.maxTimeRange);

        // set to defaults
        rangeButton.setSelectedItem(DEFAULT_TIME_RANGE);
        orgTreeButton.setSelectedItem(DEFAULT_ORG_NODE);
        if (AssessmentDataSource.
            getShowProperty(AssessmentDataSource.itemTree).equals("UID"))
        {
            itemTreeButton.setSelectedItem(DEFAULT_ITEM_CODE);
        }
        else
        {
            itemTreeButton.setSelectedItem(DEFAULT_ITEM_NODE);
        }
        //rangeButton.roundAndSetSliderRange(AssessmentDataSource.minTimeRange,
        //                                  AssessmentDataSource.maxTimeRange);

        VariableModel[] variables =
        {
            new VariableModel("Metric",
                                   /*new JComboBox(TestDataSource.metrics)*/
                                   metricTreeButton, true,
                                   VariableModel.Y_AXIS, true, 0),
            new VariableModel("Time", rangeButton,
                                   false, VariableModel.X_AXIS, true, 0),
            new VariableModel("Item", itemTreeButton, true,
                                   VariableModel.FIXED, true, 0),
            new VariableModel("Org", orgTreeButton, true,
                                   VariableModel.FIXED, true, 0) /*,
            new VariableModel("Location", new JTreeButton(), true),
            new VariableModel("Pat Cond", new JTreeButton(), true) */
        };

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
                    // Special case for metrics control, if control is not a
                    // independent variable, it must be set to a leaf node.
                    // (because we don't support aggregation across different
                    // metrics)
                    fixVariableValue(vm1);
                    fixVariableValue(vm2);

                    updateView();
                }
            });

        // Fixed Variables
        JPanel fixedVariablesPanel = new JPanel();
        fixedVariablesPanel.setBorder(
            BorderFactory.createTitledBorder("Fixed Parameters"));
        Box fvBox = new Box(BoxLayout.X_AXIS);
        fvBox.add(variableManager.getDescriptor("Item").getControl());
        fvBox.add(Box.createHorizontalStrut(spacing * 2));
        fvBox.add(variableManager.getDescriptor("Org").getControl());
        fixedVariablesPanel.add(fvBox);

        // Independent Variables
        JPanel independentVariablesPanel = new JPanel();
        independentVariablesPanel.setBorder(
            BorderFactory.createTitledBorder("Independent Parameters"));
        Box ivBox = new Box(BoxLayout.X_AXIS);
        ivBox.add(new JLabel("X Axis: "));
        ivBox.add(((VariableModel)variableManager.
                  getDescriptors(VariableModel.X_AXIS).nextElement()).
                  getControl());
        ivBox.add(Box.createHorizontalStrut(spacing * 2));
        ivBox.add(new JLabel("Y Axis: "));
        ivBox.add(((VariableModel)variableManager.
                  getDescriptors(VariableModel.Y_AXIS).nextElement()).
                  getControl());
        independentVariablesPanel.add(ivBox);

        // Chart and Legend
        //legend.setBorder(
        //    BorderFactory.createTitledBorder("Legend"));
        chart = new CLinePlotChart(getTableModel());
        legend.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        chart.setLegend(legend);

        // line plot panel
        //chart.getXAxis().setTitleText("Time (C+)");
        JPanel linePlotPanel = new JPanel(new BorderLayout());
        JPanel xAxisPanel = new JPanel();
        xAxisPanel.add(variableManager.getXAxisLabel());
        //xAxisPanel.add(((VariableModel)variableManager.
        //    getDescriptors(VariableModel.X_AXIS).nextElement()).
        //    getControl(), BorderLayout.SOUTH);
        JPanel yAxisPanel = new JPanel(new GridBagLayout());
        CRLabel yAxisLabel = variableManager.getYAxisLabel();
        yAxisLabel.setOrientation(CRLabel.DOWN_UP);
        yAxisPanel.add(yAxisLabel);
        linePlotPanel.add(chart, BorderLayout.CENTER);
        linePlotPanel.add(yAxisPanel, BorderLayout.WEST);
        linePlotPanel.add(xAxisPanel, BorderLayout.SOUTH);
        chart.setTitle(variableManager.toString());

        // generate initial query based on initial variable settings
        updateView();

        // table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        TableSorter sorter = new TableSorter(getTableModel());
        JTable table = new CRowHeaderTable(sorter);
        sorter.addMouseListenerToHeaderInTable(table);
        JScrollPane scrolledTable = new JScrollPane(table);
        tablePanel.add(scrolledTable, BorderLayout.CENTER);
        tablePanel.setMinimumSize(new Dimension(0, 0));

        // high level layout
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx=0;
        gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH;
        JPanel controlPanel = new JPanel(gbl);
        controlPanel.add(getMonitorControl(), gbc);
        gbc.weightx=1;
        gbl.setConstraints(fixedVariablesPanel, gbc);
        controlPanel.add(fixedVariablesPanel);
        gbc.weightx=0;
        gbc.weighty=0;
        gbl.setConstraints(independentVariablesPanel, gbc);
        controlPanel.add(independentVariablesPanel);
        independentVariablesPanel.setPreferredSize(new Dimension(0, 0));
        //gbl.setConstraints(featureSelectionControl, gbc);
        //controlPanel.add(featureSelectionControl);
        gbl.setConstraints(legend, gbc);
        controlPanel.add(legend);
        final JSplitPane chartPanel =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                           linePlotPanel, tablePanel);
        chartPanel.setOneTouchExpandable(true);
        chartPanel.setDividerLocation(Integer.MAX_VALUE);
        add(chartPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        if (usingJdk13orGreater())
        {
            try
            {
                // Without using reflection, the following line is:
                // chartPanel.setResizeWeight(1);
                // Will not compile under jdk1.2.2 (thus the use of reflection)
                chartPanel.getClass().
                    getMethod("setResizeWeight",
                              new Class[]{double.class}).
                        invoke(chartPanel, new Object[]{new Double(1)});
            }
            catch(Exception e) {e.printStackTrace();}
        }
        else
        {
            // jdk 1.2
            chartPanel.addComponentListener(new ComponentAdapter() {
                    public void componentResized(ComponentEvent e)
                    {
                        chartPanel.setDividerLocation(
                            chartPanel.getSize().height -
                            chartPanel.getDividerSize());
                    }
                });
        }
    }

    private RangeModel adjustedXScale = null;
    public void adjustXScale(RangeModel rm)
    {
        // will be used the next time the view is updated
        adjustedXScale = rm;
    }

    protected void populateMenuBar(JMenuBar mb)
    {
        //
        // View Menu
        //
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        final JMenuItem adjustXScale =
          new JCheckBoxMenuItem("Adjust X Scale", true);
        adjustXScale.setMnemonic('X');
        viewMenu.add(adjustXScale);
        chart.setShowXRangeScroller(adjustXScale.isSelected());
        adjustXScale.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    chart.setShowXRangeScroller(adjustXScale.isSelected());
                    if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                        chart.resetTotalRange();
                    }
                }
            });

        final JMenuItem adjustYScale = new JCheckBoxMenuItem("Adjust Y Scale");
        adjustYScale.setMnemonic('Y');
        viewMenu.add(adjustYScale);
        chart.setShowYRangeScroller(adjustYScale.isSelected());
        adjustYScale.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    chart.setShowYRangeScroller(adjustYScale.isSelected());
                    if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                        chart.resetYRangeScroller();
                    }
                }
            });

        viewMenu.add(new JSeparator());

        final JMenuItem showGrid = new JCheckBoxMenuItem("Show Grid", true);
        showGrid.setMnemonic('S');
        viewMenu.add(showGrid);
        chart.setShowGrid(showGrid.isSelected());
        showGrid.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    chart.setShowGrid(showGrid.isSelected());
                }
            });

        mb.add(viewMenu, 1);

        super.populateMenuBar(mb);
    }

    protected String getSelectedMetric()
    {
        return metricTreeButton.getSelectedItem().toString();
    }

    protected void updateMetricSelector()
    {
        metricTreeButton.setRoot(AssessmentDataSource.makeMetricTree());
        metricTreeButton.expandFirstLevel();
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
        itemTreeButton.setSelectedItem(DEFAULT_ITEM_NODE);
    }

    public void updateView()
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        final Runnable postQueryRunnable = new Runnable() {
            public void run()
            {
                chart.setTitle(variableManager.toString());
                repaint();
                setCursor(Cursor.getDefaultCursor());
                if (adjustedXScale != null)
                {
                    SwingUtilities.invokeLater( new Runnable() {
                            public void run()
                            {
                                chart.setXScrollerRange(adjustedXScale);
                                adjustedXScale = null;
                            }
                        });
                }
            }
        };

        generateQuery(variableManager, postQueryRunnable);
    }

    /**
     * Special case for metrics control, if control is not a
     * independent variable, it must be set to a leaf node.
     * (because we don't support aggregation across different metrics)
     */
    private void fixVariableValue(VariableModel vm)
    {
        if (vm.getName().equalsIgnoreCase("Metric") &&
            (vm.getState() == VariableModel.FIXED))
        {
            vm.setValue("Demand");
        }
    }

    /**
     * Needed for compatibility with jdk1.2.2
     */
    private boolean usingJdk13orGreater()
    {
        float versionNumber =
            Float.parseFloat(System.getProperty("java.class.version"));
        return (versionNumber >= 47.0);
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

    /**
     * Main for unit testing.
     *
     * @param args ignored
     */
    public static void main(String[] args)
    {
        CFrame frame = new CFrame(UIConstants.LINEPLOT_UI_NAME, false);
        LinePlotPanel lpp = new LinePlotPanel(false);
        VariableInterfaceManager vim = lpp.getVariableInterfaceManager();
        vim.getDescriptor("Org").setValue("23INBN");
        vim.getDescriptor("Item").setValue("Drug");
        vim.getDescriptor("Metric").setValue("Demand");
        vim.setYAxis("Item");

        frame.getContentPane().add(lpp);
        frame.setVisible(true);
    }
}