/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.client.aggregator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.lib.uiframework.ui.components.CFrame;
import org.cougaar.lib.uiframework.ui.components.CRadioButtonSelectionControl;

public class QueryEditor extends JPanel
{
  private Collection availableClusters;
  private CRadioButtonSelectionControl queryType;
  private JPanel persistentPanel;
  private JTextField name;
  private JComboBox updateMethod;
  private JLabel pullRateLabel;
  private JTextField pullRate;
  private JList sourceClusters;
  private ScriptEditor unaryPredEditor;
  private ScriptEditor xmlEncoderEditor;
  private JCheckBox fullFormat;
  private AggScriptEditor aggregatorEditor;

  private final static int spacing = 5;

  public QueryEditor(Collection availableClusters)
  {
    super(new GridBagLayout());
    this.availableClusters = availableClusters;
    createComponents();
  }

  /**
   * Set all fields based on passed in query
   */
  public void setFromQuery(AggregationQuery q)
  {
    queryType.setSelectedItem(q.getType().toString());
    name.setText(q.getName());
    updateMethod.setSelectedItem(q.getUpdateMethod());
    pullRate.setText(String.valueOf(q.getPullRate()));

    Enumeration ce = q.getSourceClusters();
    Vector cv = new Vector();
    while(ce.hasMoreElements())
      cv.add(ce.nextElement());
    sourceClusters.setListData(cv);

    ScriptSpec ps = q.getPredicateSpec();
    ScriptSpec fs = q.getFormatSpec();

    unaryPredEditor.setLanguage(ps.getLanguage());
    unaryPredEditor.setScript(ps.getText());
    xmlEncoderEditor.setLanguage(fs.getLanguage());
    xmlEncoderEditor.setScript(fs.getText());
    fullFormat.setSelected(fs.getFormat() == XmlFormat.INCREMENT);

    aggregatorEditor.setScriptSpec(q.getAggSpec());
  }

  /**
   * Create new query object based on contents of form
   */
  public AggregationQuery createQuery()
  {
    AggregationQuery aq = new AggregationQuery(
      QueryType.fromString(queryType.getSelectedItem().toString()));
    if (aq.getType() == QueryType.PERSISTENT)
    {
      aq.setName(name.getText());
      aq.setUpdateMethod((UpdateMethod)updateMethod.getSelectedItem());
      if (aq.getUpdateMethod() == UpdateMethod.PULL)
      {
        aq.setPullRate(Integer.parseInt(pullRate.getText()));
      }
    }

    ListModel lm = sourceClusters.getModel();
    for (int i = 0; i < lm.getSize(); i++)
    {
      aq.addSourceCluster(lm.getElementAt(i).toString());
    }

    aq.setPredicateSpec(new ScriptSpec(ScriptType.UNARY_PREDICATE,
      unaryPredEditor.getLanguage(), unaryPredEditor.getScript()));
    aq.setFormatSpec(new ScriptSpec(xmlEncoderEditor.getLanguage(),
      getXmlFormat(), xmlEncoderEditor.getScript()));

    // aq.setPredicateSpec(unaryPredEditor.getScriptSpec());
    // aq.setFormatSpec(xmlEncoderEditor.getScriptSpec());
    aq.setAggSpec(aggregatorEditor.getScriptSpec());

    return aq;
  }

  public XmlFormat getXmlFormat () {
    if (fullFormat.isSelected())
      return XmlFormat.INCREMENT;
    return XmlFormat.XMLENCODER;
  }

  private void createComponents()
  {
    // query type
    JPanel typePanel = new JPanel(new BorderLayout());
    typePanel.add(new JLabel("Query Type: ", JLabel.RIGHT), BorderLayout.WEST);
    String[] selections = collectionToStringArray(QueryType.getValidValues());
    queryType = new CRadioButtonSelectionControl(selections, BoxLayout.X_AXIS);
    queryType.setSelectedItem(QueryType.TRANSIENT.toString());
    typePanel.add(queryType, BorderLayout.CENTER);

    // source clusters
    JPanel sourcePanel = new JPanel(new BorderLayout(spacing, spacing));
    sourcePanel.setBorder(BorderFactory.createTitledBorder("Source Clusters"));
    sourceClusters = new JList();
    sourceClusters.setEnabled(false);
    JScrollPane scrollSourceClusters = new JScrollPane(sourceClusters);
    scrollSourceClusters.setPreferredSize(new Dimension(0, 0));
    sourcePanel.add(scrollSourceClusters, BorderLayout.CENTER);
    JPanel editPanel = new JPanel(new GridBagLayout());
    final JButton editClusters = new JButton("Edit List");
    editPanel.add(editClusters);
    editPanel.add(Box.createHorizontalStrut(spacing));
    sourcePanel.add(editPanel, BorderLayout.EAST);

    // persistent handling
    GridBagConstraints pGbc = new GridBagConstraints();
    persistentPanel = new JPanel(new GridBagLayout());
    persistentPanel.setBorder(
      BorderFactory.createTitledBorder("Persistent Handling"));
    pGbc.insets = new Insets(spacing/2, spacing/2, spacing/2, spacing/2);
    pGbc.weighty = 1;
    pGbc.weightx = 0;
    pGbc.fill = GridBagConstraints.HORIZONTAL;
    persistentPanel.add(new JLabel("Query Name: ", JLabel.RIGHT), pGbc);
    name = new JTextField();
    pGbc.weightx = 1;
    persistentPanel.add(name, pGbc);
    pGbc.gridy = 1;
    pGbc.weightx = 0;
    JLabel updateLabel = new JLabel("Update Method: ", JLabel.RIGHT);
    persistentPanel.add(updateLabel, pGbc);
    Object[] updateMethods = UpdateMethod.getValidValues().toArray();
    updateMethod = new JComboBox(updateMethods);
    persistentPanel.add(updateMethod, pGbc);
    String updateMethodTooltip = "<HTML>" +
      "<B>Push:</B> Source Clusters push updates to aggregation agent,<BR>" +
      "<B>Pull:</B> Aggregation Agent pulls updates from Source Clusters" +
      "</HTML>";
    updateLabel.setToolTipText(updateMethodTooltip);
    updateMethod.setToolTipText(updateMethodTooltip);
    pGbc.gridy++;
    pullRateLabel = new JLabel("Pull Wait Period (sec.): ", JLabel.RIGHT);
    persistentPanel.add(pullRateLabel, pGbc);
    pullRate = new JTextField(3);
    persistentPanel.add(pullRate, pGbc);
    String pullRateToolTip = "Enter negative pull period for no auto-pull";
    pullRateLabel.setToolTipText(pullRateToolTip);
    pullRate.setToolTipText(pullRateToolTip);

    // unary predicate
    unaryPredEditor = new ScriptEditor();

    // xml encoder
    xmlEncoderEditor = new ScriptEditor();
    fullFormat = new JCheckBox("Full Format");
    xmlEncoderEditor.addAuxControl(fullFormat);

    // aggregator
    aggregatorEditor = new AggScriptEditor();

    // fix selector sizes
    // -- this may be obsolete now
    unaryPredEditor.setControlSize(xmlEncoderEditor.getControlSize());

    // high level layout
    JTabbedPane scriptPanel = new JTabbedPane();
    scriptPanel.setBorder(BorderFactory.createTitledBorder("Scripts"));
    scriptPanel.addTab("Unary Predicate", unaryPredEditor);
    scriptPanel.addTab("XML Encoding", xmlEncoderEditor);
    scriptPanel.addTab("Aggregation", aggregatorEditor);

    JPanel topGrid = new JPanel(new GridLayout(1, 2, spacing, spacing));
    topGrid.add(sourcePanel);
    topGrid.add(persistentPanel);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    add(typePanel, gbc);
    gbc.insets = new Insets(spacing, 0, 0, 0);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.gridy = 1;
    add(topGrid, gbc);
    gbc.gridy++;
    gbc.weighty = 1;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    add(scriptPanel, gbc);

    // initialize to correct enabled state
    setEnabledFields();

    // event handling
    queryType.addPropertyChangeListener("selectedItem",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e)
        {
          setEnabledFields();
        }
      });
    updateMethod.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          setEnabledFields();
        }
      });
    editClusters.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          LinkedList userList = new LinkedList();
          for (int i = 0; i < sourceClusters.getModel().getSize(); i++)
          {
            userList.add(sourceClusters.getModel().getElementAt(i));
          }
          Collection newSourceClusters = SublistEditorDialog.
            showSublistEditorDialog(editClusters, userList, availableClusters);
          sourceClusters.setListData(newSourceClusters.toArray());
        }
      });
  }

  private void setEnabledFields()
  {
    QueryType qt =QueryType.fromString(queryType.getSelectedItem().toString());
    if (qt == QueryType.PERSISTENT)
    {
      setEnabled(persistentPanel, true);
      boolean pullSelected = updateMethod.getSelectedItem()==UpdateMethod.PULL;
      pullRateLabel.setEnabled(pullSelected);
      pullRate.setEnabled(pullSelected);
    }
    else
    {
      setEnabled(persistentPanel, false);
    }

  }

  private void setEnabled(Container c, boolean enabled)
  {
    for (int i=0; i < c.getComponentCount(); i++)
    {
      c.getComponent(i).setEnabled(enabled);
    }
  }

  private String[] collectionToStringArray(Collection col)
  {
    String[] sa = new String[col.size()];
    int count = 0;
    for (Iterator i = col.iterator(); i.hasNext();)
    {
      sa[count++] = i.next().toString();
    }
    return sa;
  }

  /**
   * For unit testing
   */
  public static void main(String[] args)
  {
    LinkedList masterList = new LinkedList();
    masterList.add("one");
    masterList.add("two");
    masterList.add("three");
    CFrame testFrame = new CFrame("Query", true);
    QueryEditor qe = new QueryEditor(masterList);
    testFrame.getContentPane().add(qe, BorderLayout.CENTER);
    testFrame.show();
  }

  /**
   * Sublist editor for selecting the source clusters from a list of all
   * available clusters.
   */
  private static class SublistEditorDialog extends JDialog
  {
    private SublistEditor se;

    private SublistEditorDialog(Component source, Frame owner,
                                Collection orgUserList,
                                Collection orgMasterList)
    {
      super(owner, "Source Cluster Editor", true);
      se = new SublistEditor("Source Clusters", orgUserList,
                             "Remaining Clusters", orgMasterList);
      createComponents();
      setSize(500, 300);
      setLocationRelativeTo(source);
      show();
    }

    public SublistEditor getSublistEditor()
    {
      return se;
    }

    public static
      Collection showSublistEditorDialog(Component source,
                                         Collection orgUserList,
                                         Collection orgMasterList)
    {
      Frame owner = (Frame)SwingUtilities.getWindowAncestor(source);
      SublistEditorDialog sed =
        new SublistEditorDialog(source, owner, orgUserList, orgMasterList);
      return sed.getSublistEditor().getUserList();
    }

    public void createComponents()
    {
      se.setBorder(BorderFactory.createEtchedBorder());

      JPanel buttonPanel = new JPanel();
      buttonPanel.setBorder(
        BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing));
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
      buttonPanel.add(Box.createGlue());
      JButton okButton = new JButton("OK");
      buttonPanel.add(okButton);
      buttonPanel.add(Box.createGlue());

      getContentPane().add(se, BorderLayout.CENTER);
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      // event handling
      okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)
          {
            dispose();
          }
        });
    }
  }
}