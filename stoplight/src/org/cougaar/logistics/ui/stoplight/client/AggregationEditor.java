
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
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.event.*;

public class AggregationEditor extends JDialog
{
    private boolean useAssessmentAgent = true;
    private JList metricList;
    private JTextField unitSelector;
    private JComboBox orgSelector;
    private JPanel derivedMetricBox;
    private JComboBox timeSelector;
    private JComboBox itemSelector;
    private Hashtable localAggregationSchemes = null;
    private static final int spacing = 10;
    private int closeAction = JOptionPane.CANCEL_OPTION;

    private AggregationEditor(Frame owner, final String selectedMetric)
    {
        super(owner, true);
        setTitle("Aggregation Editor");
        setSize(600, 350);
        localAggregationSchemes =
            copyAggregationSchemes(AssessmentDataSource.aggregationSchemes);
        createComponents();
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
        AggregationEditor ae = new AggregationEditor(owner, selectedMetric);
        return ae.getCloseAction();
    }

    public int getCloseAction()
    {
        return closeAction;
    }

    private static Hashtable copyAggregationSchemes(Hashtable orgAggSchemes)
    {
        Hashtable newAggSchemes = new Hashtable();
        Enumeration keys = orgAggSchemes.keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            AggregationScheme orgAS = (AggregationScheme)orgAggSchemes.get(key);
            AggregationScheme newAS =
                new AggregationScheme(orgAS.orgAggregation,
                                      orgAS.timeAggregation,
                                      orgAS.itemAggregation);
            newAggSchemes.put(key, newAS);
        }

        return newAggSchemes;
    }

    private void setMetric(String selectedMetric)
    {
        metricList.setSelectedValue(selectedMetric, true);
    }

    private void createComponents()
    {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // Create JList of aggregation scheme keys
        metricList = new JList(AssessmentDataSource.aggregationSchemeLabels);
        metricList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sml = new JScrollPane(metricList);

        // Create panel to hold metric info
        JPanel metricInfoPanel = new JPanel();
        metricInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
               "General Metric Attributes"),
               BorderFactory.createEmptyBorder(0, spacing, spacing, spacing)));
        metricInfoPanel.setLayout(new BoxLayout(metricInfoPanel, BoxLayout.Y_AXIS));
        Box unitBox = new Box(BoxLayout.X_AXIS);
        JLabel unitLabel = new JLabel("Units: ", JLabel.RIGHT);
        unitBox.add(unitLabel);
        unitBox.add(unitSelector = new JTextField());
        unitSelector.setMaximumSize(new Dimension(
            Integer.MAX_VALUE, unitSelector.getPreferredSize().height));
        unitSelector.setEnabled(false);
        metricInfoPanel.add(Box.createGlue());
        metricInfoPanel.add(unitBox);
        metricInfoPanel.add(Box.createGlue());

        // Create aggregation scheme editor panel
        Object[] orgAggSelection =
            {AggregationScheme.getLabelString(AggregationScheme.SUM),
             AggregationScheme.getLabelString(AggregationScheme.MIN),
             AggregationScheme.getLabelString(AggregationScheme.MAX),
             AggregationScheme.getLabelString(AggregationScheme.AVG)};
        Object[] timeAggSelection =
            {AggregationScheme.getLabelString(AggregationScheme.SUM),
             AggregationScheme.getLabelString(AggregationScheme.MIN),
             AggregationScheme.getLabelString(AggregationScheme.MAX),
             AggregationScheme.getLabelString(AggregationScheme.AVG),
             AggregationScheme.getLabelString(AggregationScheme.FONE)};
        Object[] itemAggSelection=
            {AggregationScheme.getLabelString(AggregationScheme.NONE),
             AggregationScheme.getLabelString(AggregationScheme.SUM),
             AggregationScheme.getLabelString(AggregationScheme.MIN),
             AggregationScheme.getLabelString(AggregationScheme.MAX),
             AggregationScheme.getLabelString(AggregationScheme.AVG),
             AggregationScheme.getLabelString(AggregationScheme.FONE),
             AggregationScheme.getLabelString(AggregationScheme.WAVG)};
        JPanel aggPanel = new JPanel();
        aggPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
               "Metric's Ordered Aggregation Scheme"),
               BorderFactory.createEmptyBorder(0, spacing, spacing, spacing)));
        aggPanel.setLayout(new BoxLayout(aggPanel, BoxLayout.Y_AXIS));
        Box orgBox = new Box(BoxLayout.X_AXIS);
        JLabel orgLabel = new JLabel("Organization: ", JLabel.RIGHT);
        orgBox.add(orgLabel);
        orgBox.add(orgSelector = new JComboBox(orgAggSelection));
        derivedMetricBox = new JPanel();
        derivedMetricBox.
            setLayout(new BoxLayout(derivedMetricBox, BoxLayout.Y_AXIS));
        JLabel derivedMetricLabel =
            new JLabel("-- Derived Metric Calculation --", JLabel.CENTER);
        derivedMetricLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        derivedMetricBox.add(Box.createGlue());
        derivedMetricBox.add(derivedMetricLabel);
        Box timeBox = new Box(BoxLayout.X_AXIS);
        JLabel timeLabel = new JLabel("Time: ", JLabel.RIGHT);
        timeBox.add(timeLabel);
        timeBox.add(timeSelector = new JComboBox(timeAggSelection));
        Box itemBox = new Box(BoxLayout.X_AXIS);
        JLabel itemLabel = new JLabel("Item: ", JLabel.RIGHT);
        itemBox.add(itemLabel);
        itemBox.add(itemSelector = new JComboBox(itemAggSelection));

        // aggregation order is different when using aggregation agent
        if (useAssessmentAgent)
        {
          aggPanel.add(derivedMetricBox);
          aggPanel.add(Box.createGlue());
          aggPanel.add(timeBox);
          aggPanel.add(Box.createGlue());
          aggPanel.add(itemBox);
          aggPanel.add(Box.createGlue());
          aggPanel.add(orgBox);
        }
        else
        {
          aggPanel.add(Box.createGlue());
          aggPanel.add(orgBox);
          aggPanel.add(derivedMetricBox);
          aggPanel.add(Box.createGlue());
          aggPanel.add(timeBox);
          aggPanel.add(Box.createGlue());
          aggPanel.add(itemBox);
        }
        aggPanel.add(Box.createGlue());

        // Justify Label sizes
        Dimension labelSize = orgLabel.getPreferredSize();
        unitLabel.setPreferredSize(labelSize);
        timeLabel.setPreferredSize(labelSize);
        itemLabel.setPreferredSize(labelSize);

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
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(metricInfoPanel);
        rightPanel.add(Box.createGlue());
        rightPanel.add(aggPanel);
        c.add(sml, BorderLayout.CENTER);
        c.add(rightPanel, BorderLayout.EAST);
        c.add(buttonPanel, BorderLayout.SOUTH);

        // Event Handling
        metricList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e)
                {
                    updateAggregationScheme();
                }
            });

        orgSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    AggregationScheme as = getSelectedScheme();
                    as.orgAggregation = as.getAggregationMethod(
                        (String)orgSelector.getSelectedItem());
                }
            });

        timeSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    AggregationScheme as = getSelectedScheme();
                    as.timeAggregation = as.getAggregationMethod(
                        (String)timeSelector.getSelectedItem());
                }
            });

        itemSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    AggregationScheme as = getSelectedScheme();
                    as.itemAggregation = as.getAggregationMethod(
                        (String)itemSelector.getSelectedItem());
                }
            });

        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    AssessmentDataSource.aggregationSchemes =
                        copyAggregationSchemes(localAggregationSchemes);
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
                    localAggregationSchemes =
                        AssessmentDataSource.createDefaultAggSchemes();
                    updateAggregationScheme();
                }
            });
    }

    private AggregationScheme getSelectedScheme()
    {
        return (AggregationScheme)
            localAggregationSchemes.get(metricList.getSelectedValue());
    }

    private void updateAggregationScheme()
    {
        AggregationScheme as = getSelectedScheme();
        orgSelector.setSelectedItem(as.getLabelString(as.orgAggregation));
        timeSelector.setSelectedItem(as.getLabelString(as.timeAggregation));
        itemSelector.setSelectedItem(as.getLabelString(as.itemAggregation));

        String metric = (String)metricList.getSelectedValue();
        unitSelector.setText(MetricInfo.metricUnits.get(metric).toString());
        derivedMetricBox.setVisible(MetricInfo.isDerived(metric));
        boolean unitlessMetric =
            MetricInfo.metricUnits.get(metric).equals(MetricInfo.UNITLESS);
        itemSelector.setEnabled(unitlessMetric);
    }

    /**
     * main for unit testing
     */
    public static void main(String args[])
    {
        JFrame frame = new JFrame();
        if (AggregationEditor.showDialog(frame, "Demand") ==
            JOptionPane.OK_OPTION)
        {
            System.out.println("OK Selected");
        }
    }
}