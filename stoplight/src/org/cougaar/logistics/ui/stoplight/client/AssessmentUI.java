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
package org.cougaar.logistics.ui.stoplight.client;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;

import org.cougaar.logistics.ui.stoplight.ui.components.CRadioButtonSelectionControl;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;
import org.cougaar.logistics.ui.stoplight.ui.util.VariableInterfaceManager;

public abstract class AssessmentUI extends JPanel implements CougaarUI
{
    protected static final String DEFAULT_ORG_NODE =
        System.getProperty("DEFAULTORG");

    private static Vector assessmentUIs = new Vector();

    private boolean useAssessmentAgent = true;
    private boolean ignoreQueries = false;
    private static final String descriptionString =  "Show Description";
    private static final String codeString = "Show Code";
    private MonitorControl monitorControl = new MonitorControl();
    private JCheckBox includeSubordinatesControl =
        new JCheckBox("Include Subordinates", true);
    private CRadioButtonSelectionControl itemDisplayPanel = null;
    private QueryGenerator queryGenerator = null;

    public AssessmentUI()
    {
        super(new BorderLayout());

        // create a new query generator to maintain an updated table model
        // based on (and triggered by) changes to variable controls.
        if (useAssessmentAgent)
        {
            queryGenerator = new AssessmentQueryGenerator();

            monitorControl.setBorder(
              BorderFactory.createTitledBorder("Monitor"));

            monitorControl.addPropertyChangeListener("monitoring",
              new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    Boolean ignore =
                      (Boolean)monitorControl.getClientProperty("IGNORE");
                    if ((ignore != null) && (ignore.booleanValue()))
                    {
                        monitorControl.putClientProperty("IGNORE",
                                                         Boolean.FALSE);
                    }
                    else
                    {
                        monitorControl.
                          putClientProperty("BUTTON_PRESSED", Boolean.TRUE);
                        updateView();
                    }
                }
              });

            includeSubordinatesControl.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    ((AssessmentQueryGenerator)queryGenerator).
                      setIncludeSubordinates(
                        includeSubordinatesControl.isSelected());
                    updateView();
                }
              });
        }
        else
        {
            queryGenerator = new DatabaseQueryGenerator();
            monitorControl.setVisible(false);
        }

        assessmentUIs.add(this);
    }

    public void setIgnoreQueries(boolean ignoreQueries)
    {
        this.ignoreQueries = ignoreQueries;
    }

    public abstract void updateView();
    protected abstract String getSelectedMetric();
    protected abstract void updateMetricSelector();
    protected abstract void updateOrgSelector();
    protected abstract void updateItemSelector();

    protected TableModel getTableModel()
    {
        return queryGenerator.getTableModel();
    }

    protected void
      generateQuery(final VariableInterfaceManager variableManager,
                    final Runnable postQueryRunnable)
    {
        if (ignoreQueries)
        {
            return;
        }

        if (useAssessmentAgent)
        {
            Boolean buttonPressed =
              (Boolean)monitorControl.getClientProperty("BUTTON_PRESSED");
            if ((buttonPressed != null) && (buttonPressed.booleanValue()))
            {
                monitorControl.putClientProperty("BUTTON_PRESSED",
                                                 Boolean.FALSE);
            }
            else
            {
                monitorControl.putClientProperty("IGNORE", Boolean.TRUE);
                monitorControl.setMonitoring(false);
            }
        }

        // Don't run query in event thread
        (new Thread() {
                public void run()
                {
                    if (useAssessmentAgent)
                    {
                        String errorStr = 
                        ((AssessmentQueryGenerator)queryGenerator).
                            generateQuery(variableManager, postQueryRunnable,
                                          AssessmentDataSource.timeout, monitorControl.isMonitoring());
                        
                        if (errorStr != null) {
                          JOptionPane.showMessageDialog(AssessmentUI.this,
                                    errorStr,
                                    "Warnings during query...",
                                    JOptionPane.WARNING_MESSAGE);
                        }

                    }
                    else
                    {
                        generateQuery(variableManager, postQueryRunnable);
                    }
                }
            }).start();
    }

    protected JComponent getMonitorControl()
    {
        return monitorControl;
    }

    protected JComponent getIncludeSubordinatesControl()
    {
        return includeSubordinatesControl;
    }

    protected void populateMenuBar(JMenuBar menuBar)
    {
        //
        // File Menu
        //
        if (useAssessmentAgent)
        {
            JMenu fileMenu = menuBar.getMenu(0);

            // save org tree
            JMenuItem saveOrg = new JMenuItem("Save Organization Tree");
            saveOrg.setMnemonic('S');
            fileMenu.add(new JSeparator(), 0);
            fileMenu.add(saveOrg, 0);
            saveOrg.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        AssessmentDataSource.saveOrgTree();
                    }
                });

            // connect to aggregation agent
            JMenuItem connect = new JMenuItem("Connect");
            connect.setMnemonic('o');
            fileMenu.add(new JSeparator(), 0);
            fileMenu.add(connect, 0);
            connect.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        nameServerUrlDialog();
                    }
                });
        }

        //
        // View Menu
        //
        JMenu viewMenu = menuBar.getMenu(1);

        // item pullright menu
        JMenu itemMenu = new JMenu("Items");
        itemMenu.setMnemonic('I');

        // item labels
        itemDisplayPanel =
            new CRadioButtonSelectionControl(new String[]{descriptionString,
                                                          codeString},
                                             BoxLayout.Y_AXIS);
        String currentShowProp =
          AssessmentDataSource.getShowProperty(AssessmentDataSource.itemTree);
        itemDisplayPanel.setSelectedItem(currentShowProp.equals("ITEM_ID") ?
                                         descriptionString : codeString);
        itemDisplayPanel.addPropertyChangeListener("selectedItem",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    String newValue = e.getNewValue().toString();
                    updateItemShowProperty(newValue);
                }
            });
        ButtonGroup bg = itemDisplayPanel.convertToMenuItems();
        Enumeration mis = bg.getElements();
        while (mis.hasMoreElements())
        {
            itemMenu.add((JMenuItem)mis.nextElement());
        }

        // when the assessment agent is used, the client doesn't need to know
        // about item weights.
        if (!useAssessmentAgent)
        {
            itemMenu.add(new JSeparator());

            // refresh item weights
            final JMenuItem refreshItemWeights =
                new JMenuItem("Refresh Item Weights");
            refreshItemWeights.setMnemonic('R');
            itemMenu.add(refreshItemWeights);
            refreshItemWeights.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        AssessmentDataSource.createItemTrees(); //i.e. recreate
                        updateItemSelector();
                    }
                });
        }

        viewMenu.add(new JSeparator());
        viewMenu.add(itemMenu);

        if (useAssessmentAgent)
        {
            // org menu
            JMenu orgMenu = new JMenu("Organizations");
            orgMenu.setMnemonic('O');

            final JCheckBoxMenuItem aggOrgs =
              new JCheckBoxMenuItem("Disable Org Aggregation When On Axis",
                                    false);
            orgMenu.add(aggOrgs);
            viewMenu.add(orgMenu);
            aggOrgs.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e)
                    {
                        ((AssessmentQueryGenerator)queryGenerator).
                            setDisableOrgAxisAgg(aggOrgs.isSelected());
                        updateView();
                    }
                });
        }

        // refresh view
        viewMenu.add(new JSeparator());
        JMenuItem refreshView = new JMenuItem("Refresh");
        refreshView.setMnemonic('R');
        viewMenu.add(refreshView);
        refreshView.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    updateView();
                }
            });

        //
        // Edit Menu
        //

        //
        // These three preference dialogs will probably be aggregated into
        // a single preferences dialog with tabbed panes.
        //
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');

        JMenuItem aggregation = new JMenuItem("Aggregation", 'A');
        editMenu.add(aggregation);
        aggregation.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    int optionSelected =
                        AggregationEditor.showDialog(findFrame(),
                                                     getSelectedMetric());
                    if (optionSelected == JOptionPane.OK_OPTION)
                    {
                        updateView();
                    }
                }
            });

        JMenuItem derivedMetrics = new JMenuItem("Derived Metrics", 'D');
        editMenu.add(derivedMetrics);
        derivedMetrics.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    int optionSelected =
                        DerivedMetricEditor.showDialog(findFrame(),
                                                       getSelectedMetric());
                    if (optionSelected == JOptionPane.OK_OPTION)
                    {
                        updateMetricSelector();
                    }
                }
            });

        if (useAssessmentAgent)
        {
            JMenuItem viewUpdate = new JMenuItem("View Update Method", 'V');
            editMenu.add(viewUpdate);
            viewUpdate.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int optionSelected =
                          UpdateEditor.showDialog(findFrame());
                        if (optionSelected == JOptionPane.OK_OPTION)
                        {
                            updateView();
                        }
                    }
                });

            JMenuItem setTimeout = new JMenuItem("Set Agg Agent Timeout", 'T');
            editMenu.add(setTimeout);
            setTimeout.addActionListener(new SetTimeoutAction());
        }

        menuBar.add(editMenu, 1);
    }

    private class SetTimeoutAction implements ActionListener {
        public void actionPerformed (ActionEvent ae) {
            String timeoutStr = (String) JOptionPane.showInputDialog(
              AssessmentUI.this, "Set Agg Agent Query Timeout in milliseconds :", "Timeout...",
              JOptionPane.QUESTION_MESSAGE, null, null, new Long(AssessmentDataSource.timeout));
            if (timeoutStr == null) {
                return;
            }
            else if (timeoutStr.length() == 0) {
                showErrorMessage("No timeout was specified.", "Error in Set Timeout ...");
            }
            else {
                try {
                    updateTimeout(Long.parseLong(timeoutStr));
                } catch (NumberFormatException nfe) {
                    showErrorMessage("Invalid number: " + timeoutStr + ".", "Error in Set Timeout ...");
                }
            }
        }
    }

    protected void showErrorMessage (String msg, String title) {
        JOptionPane.showMessageDialog(AssessmentUI.this, msg,
            title, JOptionPane.ERROR_MESSAGE);
    }

    protected void updateTimeout(long timeout) {
        AssessmentDataSource.timeout = timeout;
    }
    
    /**
     * Add this panel to the passed in JFrame.  This method is required to
     * implement the CougaarUI interface.
     *
     * @param frame frame to which the panel should be added
     */
    public void install(JFrame frame)
    {
        frame.getContentPane().add(this);
        populateMenuBar(frame.getJMenuBar());

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e)
            {
              if (queryGenerator != null)
              {
                queryGenerator.finalize();
              }

              assessmentUIs.remove(AssessmentUI.this);
            }
          });
    }

    /**
     * Add this panel to the passed in JInternalFrame.  This method is required
     * to implement the CougaarUI interface.
     *
     * @param frame frame to which the panel should be added
     */
    public void install(JInternalFrame frame)
    {
        frame.getContentPane().add(this);
        JMenuBar mb = new JMenuBar();
        mb.add(new JMenu());
        populateMenuBar(mb);
        frame.setJMenuBar(mb);

        frame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e)
            {
              if (queryGenerator != null)
              {
                queryGenerator.finalize();
              }

              assessmentUIs.remove(AssessmentUI.this);
            }
          });
    }

    /**
     * Returns true if this CougaarUI supports pluggable look and feel.  This
     * method is required to implement the CougaarUI interface.
     *
     * @return true if UI supports pluggable look and feel
     */
    public boolean supportsPlaf()
    {
        return true;
    }

    private Frame findFrame()
    {
        Container parent = getParent();
        while (!(parent instanceof Frame))
        {
            parent = parent.getParent();
        }
        return (Frame)parent;
    }

    private void nameServerUrlDialog()
    {
        String inputURL = (String)
          JOptionPane.showInputDialog(findFrame(),
                                      "Enter Name Server URL:",
                                      "Name Server Connection Editor",
                                      JOptionPane.QUESTION_MESSAGE, null, null,
                                      AssessmentDataSource.getNameServerUrl());

        // check for cancel
        if (inputURL == null)
            return;

        // before switching to new psp, cancel and monitors on
        // old psp
        for (int i = 0; i < assessmentUIs.size(); i++)
        {
            AssessmentUI ui = (AssessmentUI)assessmentUIs.elementAt(i);
            ((AssessmentQueryGenerator)ui.queryGenerator).cancelQueryMonitor();
        }

        AssessmentDataSource.setNameServerUrl(inputURL);
        AssessmentQueryGenerator.newPSPInterface();
        boolean aggregatorFound = (AssessmentDataSource.pspInterface != null);

        // this change effects all assessment UIs
        for (int i = 0; i < assessmentUIs.size(); i++)
        {
            AssessmentUI ui = (AssessmentUI)assessmentUIs.elementAt(i);
            ui.updateOrgSelector();

            if (!aggregatorFound)
                ui.updateView();
        }

        if (!aggregatorFound)
        {
            JOptionPane.showMessageDialog(findFrame(),
                                          "Aggregation Agent not found.\n" +
                                          "Check name server URL.", null,
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateItemShowProperty(String newValue)
    {
        // this is a global property, it must change on all UIs
        for (int i = 0; i < assessmentUIs.size(); i++)
        {
            AssessmentUI ui = (AssessmentUI)assessmentUIs.elementAt(i);
            ui.itemDisplayPanel.setSelectedItem(newValue);
        }

        String newShowProperty =
            newValue.equals(descriptionString) ? "ITEM_ID" : "UID";
        if (!AssessmentDataSource.
            getShowProperty(AssessmentDataSource.itemTree).
            equals(newShowProperty))
        {
            AssessmentDataSource.setShowProperty(
                AssessmentDataSource.itemTree, newShowProperty);
        }
        updateView();
    }
}