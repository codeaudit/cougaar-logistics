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

package org.cougaar.logistics.ui.inventory.dialog;

import org.cougaar.logistics.ui.inventory.InventoryUIFrame;
import org.cougaar.logistics.ui.inventory.InventoryColorTable;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.util.Hashtable;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;

/**
 * <pre>
 *
 * The InventoryPreferenceTab is an almost abstract superclass that all the tabs
 * for the InventoryPreferenceDialog inherit from.  It basically provides back
 * pointers and common functionality to all the tabs going into the dialog
 *
 * @see InventoryPreferenceDialog
 * @see org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData
 *
 **/

public class ColorTab extends PreferenceDialogTab {
  InventoryColorTable colorTable;
  ButtonGroup buttonGroup;

  public ColorTab(InventoryUIFrame invUIFrame,
                  InventoryPreferenceDialog invUIDialog,
                  InventoryPreferenceData prefData) {
    super(invUIFrame, invUIDialog, prefData);
  }


  protected void doTabLayout() {
    colorTable = prefData.getColorTable();
    buttonGroup = new ButtonGroup();
    setLayout(new GridBagLayout());
    int y = 0;
    String colorScheme = prefData.getColorScheme();
    for (int i = 0; i < InventoryPreferenceData.colorSchemes.length; i++) {
      JRadioButton button = new JRadioButton(InventoryPreferenceData.colorSchemes[i]);
      if (colorScheme.equals(InventoryPreferenceData.colorSchemes[i]))
        button.setSelected(true);
      button.addItemListener(this);
      buttonGroup.add(button);
      add(button, new GridBagConstraints(0, y++, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                         new Insets(10, 0, 0, 0), 0, 0));
    }
//	  int y = 0;
//    for (int i = 0; i < InventoryPreferenceData.colorSchemes.length; i++) {
//      JPanel p = doColorPanelLayout(InventoryPreferenceData.colorSchemes[i]);
//      add(p, new GridBagConstraints(0, y++, 1, 1, 0.0, 0.0,
//                                    GridBagConstraints.WEST, GridBagConstraints.BOTH,
//                                    new Insets(10, 0, 0, 0), 0, 0));
  }


//  private JPanel doColorPanelLayout(String colorScheme) {
//    JPanel colorPanel = new JPanel(new GridBagLayout());
//    JRadioButton button = new JRadioButton(colorScheme);
//    button.addItemListener(this); // marks preference data as changed
//    buttonGroup.add(button);
//    colorPanel.add(button, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
//				                                         GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//                                                 new Insets(0, 0, 0, 0), 0, 0));
//    colorPanel.setBorder(BorderFactory.createLineBorder(Color.red));
//    JPanel colorSchemePanel = createColorSchemePanel(colorScheme);
//    colorPanel.add(colorSchemePanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
//                                                   GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//                                                   new Insets(0, 20, 0, 0), 0, 0));
//    return colorPanel;
//  }

  private JPanel createColorSchemePanel(String colorScheme) {
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    String[] labels = colorTable.getColorLabels();
    int rows = (labels.length + 1) / 2;
    panel.setLayout(new GridLayout(rows, 2, 2, 2));
    for (int i = 0; i < labels.length; i++) {
      Color color = colorTable.get(colorScheme, labels[i]);
      Icon icon = new ColoredBox(color, 10);
      panel.add(new JLabel(labels[i], icon, SwingConstants.LEFT));
    }
    return panel;
  }

  public void flushValuesToData() {
    if (prefsChanged()) {
      Enumeration buttons = buttonGroup.getElements();
      while (buttons.hasMoreElements()) {
        JRadioButton button = (JRadioButton) buttons.nextElement();
        if (button.isSelected()) {
          prefData.colorScheme = button.getText();
          return;
        }
      }
    }
    super.flushValuesToData();
  }


}
