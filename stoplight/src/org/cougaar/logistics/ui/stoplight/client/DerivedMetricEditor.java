/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;

public class DerivedMetricEditor extends JDialog
{
    private Box metricBox = null;
    private Box unaryBox = null;
    private Box binaryBox = null;
    private Box unitBox = null;
    private JList metricList = null;
    private JList formulaList = null;
    private Hashtable localDerivedMetrics = null;
    private Hashtable localMetricUnits = null;
    private static final int spacing = 10;
    private int closeAction = JOptionPane.CANCEL_OPTION;
    private static final String NUMBER_FORMULA_ELEMENT = "Number";

    private DerivedMetricEditor(Frame owner, final String selectedMetric)
    {
        super(owner, true);
        setTitle("Derived Metric Editor");
        localDerivedMetrics = copyDerivedMetrics(MetricInfo.derivedMetrics);
        localMetricUnits = (Hashtable)MetricInfo.metricUnits.clone();
        createComponents();
        pack();
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    setMetric(selectedMetric);
                }
            });
        show();
    }

    public static int showDialog(Frame owner, String selectedMetric)
    {
        DerivedMetricEditor dme =
            new DerivedMetricEditor(owner, selectedMetric);
        return dme.getCloseAction();
    }

    public int getCloseAction()
    {
        return closeAction;
    }

    private void setMetric(String selectedMetric)
    {
        metricList.setSelectedValue(selectedMetric, true);
    }

    private static Hashtable copyDerivedMetrics(Hashtable orgDerivedMetrics)
    {
        Hashtable newDerivedMetrics = new Hashtable();
        Enumeration keys = orgDerivedMetrics.keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            Vector orgDMF = (Vector)orgDerivedMetrics.get(key);
            Vector newDMF = new Vector();
            for (int i = 0; i < orgDMF.size(); i++)
            {
                newDMF.add(new String((String)orgDMF.elementAt(i)));
            }
            newDerivedMetrics.put(key, newDMF);
        }

        return newDerivedMetrics;
    }

    private void createComponents()
    {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // Create JList of currently defined derived metrics
        JPanel metricPanel = new JPanel(new BorderLayout());
        metricList = new JList(localDerivedMetrics.keySet().toArray());
        metricList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sml = new JScrollPane(metricList);
        metricPanel.add(sml, BorderLayout.CENTER);
        JPanel metricButtonPanel =
            new JPanel(new GridLayout(2, 1, spacing, spacing));
        JButton addMetricButton = new JButton("Add New");
        metricButtonPanel.add(addMetricButton);
        final JButton removeMetricButton = new JButton("Remove Selected");
        removeMetricButton.setEnabled(false);
        metricButtonPanel.add(removeMetricButton);
        metricButtonPanel.setBorder(
          BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing));
        metricPanel.add(metricButtonPanel, BorderLayout.EAST);
        sml.setPreferredSize(new Dimension(0, 0));
        metricPanel.setBorder(
            BorderFactory.createTitledBorder("Derived Metrics"));

        // general attributes panel
        JPanel generalPanel = new JPanel();
        generalPanel.setBorder(
            BorderFactory.createTitledBorder("General Metric Attributes"));
        unitBox = new Box(BoxLayout.X_AXIS);
        unitBox.add(new JLabel("Metric's Units: "));
        final JComboBox unitSelector =
            new JComboBox(new Object[] {MetricInfo.ITEM_UNITS,
                                        MetricInfo.ITEM_DAY_UNITS,
                                        MetricInfo.UNITLESS});
        unitBox.add(unitSelector);
        generalPanel.add(unitBox);

        // Create JList of currently selected metric's formula
        JPanel formulaPanel = new JPanel(new BorderLayout());
        formulaPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Selected Metric's Formula (RPN)"));
        formulaList = new JList();
        formulaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sfl = new JScrollPane(formulaList);
        formulaPanel.add(sfl, BorderLayout.CENTER);
        JPanel removeButtonPanel = new JPanel();
        final JButton removeSelected = new JButton("Remove Selected");
        removeSelected.setEnabled(false);
        removeButtonPanel.add(removeSelected);
        formulaPanel.add(removeButtonPanel, BorderLayout.SOUTH);

        // Create Add Operation / Operator Panel
        JPanel addOpPanel = new JPanel();
        addOpPanel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createTitledBorder("Formula Components"),
          BorderFactory.createEmptyBorder(spacing, spacing, spacing,spacing)));
        addOpPanel.setLayout(new BoxLayout(addOpPanel, BoxLayout.Y_AXIS));
        metricBox = createAddBox("Metric: ",
                               AssessmentDataSource.getAllMetrics().toArray());
        addItemToAddBox(metricBox, NUMBER_FORMULA_ELEMENT);
        addOpPanel.add(Box.createVerticalStrut(spacing));
        addOpPanel.add(metricBox);
        unaryBox = createAddBox("Unary Operator: ", MetricInfo.UNARY_OPS);
        addOpPanel.add(Box.createVerticalStrut(spacing));
        addOpPanel.add(unaryBox);
        binaryBox = createAddBox("Binary Operator: ", MetricInfo.BINARY_OPS);
        addOpPanel.add(Box.createVerticalStrut(spacing));
        addOpPanel.add(binaryBox);
        enableAddOpPanel(false);

        // Line everything up
        Dimension labelSize = binaryBox.getComponent(0).getPreferredSize();
        ((JComponent)unaryBox.getComponent(0)).setPreferredSize(labelSize);
        ((JComponent)metricBox.getComponent(0)).setPreferredSize(labelSize);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(
          BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing));
        JButton resetButton = new JButton("Reset to defaults");
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createGlue());
        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(spacing));
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);

        // High Level Layout
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(metricPanel, BorderLayout.NORTH);
        northPanel.add(generalPanel, BorderLayout.SOUTH);
        c.add(northPanel, BorderLayout.NORTH);
        c.add(formulaPanel, BorderLayout.CENTER);
        c.add(addOpPanel, BorderLayout.EAST);
        c.add(buttonPanel, BorderLayout.SOUTH);

        // Event Handling
        metricList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e)
                {
                    Vector lsf = getLocalSelectedFormula();
                    if (lsf == null)
                    {
                        formulaList.setListData(new Object[] {});
                        removeMetricButton.setEnabled(false);
                        enableAddOpPanel(false);
                    }
                    else
                    {
                        unitSelector.setSelectedItem(localMetricUnits.
                            get(metricList.getSelectedValue()));
                        formulaList.setListData(lsf);
                        removeMetricButton.setEnabled(true);
                        enableAddOpPanel(true);
                    }
                }
            });

        unitSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    localMetricUnits.put(metricList.getSelectedValue(),
                                         unitSelector.getSelectedItem());
                }
            });

        formulaList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e)
                {
                    removeSelected.setEnabled(
                        formulaList.getSelectedIndex() != -1);
                }
            });

        addMetricButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    String newMetricString =
                        JOptionPane.showInputDialog(DerivedMetricEditor.this,
                        "New Derived Metric's Name:  ");
                    if (newMetricString != null)
                    {
                        localDerivedMetrics.put(newMetricString, new Vector());
                        localMetricUnits.put(newMetricString,
                                             MetricInfo.UNITLESS);
                        Object[] newMetricKeys =
                            localDerivedMetrics.keySet().toArray();
                        metricList.setListData(newMetricKeys);
                        metricList.setSelectedValue(newMetricString, true);
                        addItemToAddBox(metricBox, newMetricString);
                    }
                }
            });

        removeMetricButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    Object sv = metricList.getSelectedValue();
                    if (sv != null)
                    {
                        localDerivedMetrics.remove(sv);
                        Object[] newMetricKeys =
                            localDerivedMetrics.keySet().toArray();
                        metricList.setListData(newMetricKeys);
                    }
                }
            });

        removeSelected.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    int rIndex = formulaList.getSelectedIndex();
                    if (rIndex != -1)
                    {
                        getLocalSelectedFormula().removeElementAt(rIndex);
                        formulaList.setListData(getLocalSelectedFormula());
                    }
                }
            });

        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    MetricInfo.derivedMetrics = localDerivedMetrics;
                    MetricInfo.metricUnits = localMetricUnits;
                    AssessmentDataSource.updateAggSchemes();
                    closeAction = JOptionPane.OK_OPTION;
                    dispose();
                }
            });

        cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    closeAction = JOptionPane.CANCEL_OPTION;
                    dispose();
                }
            });

        resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    localDerivedMetrics = MetricInfo.createDerivedMetrics();
                    localMetricUnits = MetricInfo.createMetricUnits();
                    formulaList.setListData(getLocalSelectedFormula());
                }
            });
    }

    private void enableAddOpPanel(boolean enabled)
    {
        for (int i = 0; i < metricBox.getComponentCount(); i++)
        {
            metricBox.getComponent(i).setEnabled(enabled);
            unaryBox.getComponent(i).setEnabled(enabled);
            binaryBox.getComponent(i).setEnabled(enabled);
        }

        for (int i = 0; i < unitBox.getComponentCount(); i++)
        {
            unitBox.getComponent(i).setEnabled(enabled);
        }
    }

    private Box createAddBox(String labelString, Object[] selections)
    {
        final Box newBox = new Box(BoxLayout.X_AXIS);
        JLabel label = new JLabel(labelString, JLabel.RIGHT);
        newBox.add(label);
        final JComboBox cb = new JComboBox(selections);
        newBox.add(cb);
        JButton addButton = new JButton("Add");
        newBox.add(addButton);
        addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    String selectedItem = (String)cb.getSelectedItem();
                    if (selectedItem.equals(NUMBER_FORMULA_ELEMENT))
                    {
                      boolean integerEntered = false;
                      while (!integerEntered)
                      {
                        selectedItem =
                          JOptionPane.showInputDialog(newBox,
                                              "Enter Numeric Formula Element");
                        if (selectedItem == null)
                          return;
                        try {
                          Integer.valueOf(selectedItem);
                          integerEntered = true;
                        } catch (Exception exp) {}
                      }
                    }

                    Vector lsf = getLocalSelectedFormula();
                    if (lsf != null)
                    {
                        lsf.add(selectedItem);
                        formulaList.setListData(lsf);
                    }
                }
            });

        return newBox;
    }

    private static void addItemToAddBox(Box addBox, String item)
    {
      ((JComboBox)addBox.getComponent(1)).addItem(item);
    }

    private Vector getLocalSelectedFormula()
    {
        Vector selectedFormula = null;
        Object metric = metricList.getSelectedValue();
        if (metric != null)
        {
            selectedFormula = (Vector)localDerivedMetrics.get(metric);
        }
        return selectedFormula;
    }

    /**
     * main for unit testing
     */
    public static void main(String args[])
    {
        JFrame frame = new JFrame();
        if (DerivedMetricEditor.showDialog(frame, "Demand") ==
            JOptionPane.OK_OPTION)
        {
            System.out.println("OK Selected");
        }
    }
}