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


import java.util.*;


import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;
import org.cougaar.logistics.ui.inventory.InventoryUIFrame;
import org.cougaar.logistics.ui.inventory.InventoryMenuEvent;

/**
 * <pre>
 *
 * The InventoryPreferenceTab is an almost abstract superclass that all the tabs
 * for the InventoryPreferenceDialog inherit from.  It basically provides back
 * pointers and common functionality to all the tabs going into the dialog
 *
 * @see InventoryPreferenceDialog
 * @see InventoryPreferenceData
 *
 **/


public class StartUpTab extends PreferenceDialogTab {

  public static String CDAY_TIME_SCALE = "C-Days";
  public static String CALENDAR_TIME_SCALE = "Calendar Days";
  public static String DISPLAY_SHORTFALL = "Display Shortfall Plot Initially";
  public static String DONT_DISPLAY_SHORTFALL = "Hide Shortfall Plot Initially";

  public static String SHOW_INVENTORY_CHART = InventoryMenuEvent.MENU_InventoryChart;
  public static String SHOW_REFILL_CHART = InventoryMenuEvent.MENU_RefillChart;
  public static String SHOW_DEMAND_CHART = InventoryMenuEvent.MENU_DemandChart;


  protected ButtonGroup timeScaleGroup;
  protected ButtonGroup showShortfallGroup;

  protected JCheckBox showInventoryCheck;
  protected JCheckBox showDemandCheck;
  protected JCheckBox showRefillCheck;

  public StartUpTab(InventoryUIFrame invUIFrame,
                    InventoryPreferenceDialog invUIDialog,
                    InventoryPreferenceData data) {
    super(invUIFrame, invUIDialog, data);
  }


  protected void doTabLayout() {

    setLayout(new GridBagLayout());

    JLabel timeScaleLabel = new JLabel("X-Axis Time Scale: ", JLabel.LEFT);
    timeScaleGroup = new ButtonGroup();
    JRadioButton cdayRadio = new JRadioButton(CDAY_TIME_SCALE, prefData.startupWCDay);
    cdayRadio.setActionCommand(CDAY_TIME_SCALE);
    cdayRadio.addItemListener(this);
    JRadioButton calendarRadio = new JRadioButton(CALENDAR_TIME_SCALE, !prefData.startupWCDay);
    calendarRadio.setActionCommand(CALENDAR_TIME_SCALE);
    calendarRadio.addItemListener(this);

    timeScaleGroup.add(cdayRadio);
    timeScaleGroup.add(calendarRadio);

    JPanel timeScalePanel = new JPanel();
    timeScalePanel.setLayout(new GridLayout(2, 1));
    timeScalePanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    timeScalePanel.add(timeScaleLabel);
    JPanel radioPanel = new JPanel();
    radioPanel.setLayout(new GridLayout(1, 2));
    radioPanel.add(cdayRadio);
    radioPanel.add(calendarRadio);
    timeScalePanel.add(radioPanel);


    JLabel initShortfallLabel = new JLabel("Display shortfall when detected, or only if selected: ", JLabel.LEFT);
    showShortfallGroup = new ButtonGroup();
    JRadioButton showShortfallRadio = new JRadioButton(DISPLAY_SHORTFALL, prefData.displayShortfall);
    showShortfallRadio.setActionCommand(DISPLAY_SHORTFALL);
    showShortfallRadio.addItemListener(this);
    JRadioButton noshowShortfallRadio = new JRadioButton(DONT_DISPLAY_SHORTFALL, !prefData.displayShortfall);
    noshowShortfallRadio.setActionCommand(DONT_DISPLAY_SHORTFALL);
    noshowShortfallRadio.addItemListener(this);

    showShortfallGroup.add(showShortfallRadio);
    showShortfallGroup.add(noshowShortfallRadio);

    JPanel showShortfallPanel = new JPanel();
    showShortfallPanel.setLayout(new GridLayout(2, 1));
    showShortfallPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    showShortfallPanel.add(initShortfallLabel);
    radioPanel = new JPanel();
    radioPanel.setLayout(new GridLayout(1, 2));
    radioPanel.add(showShortfallRadio);
    radioPanel.add(noshowShortfallRadio);
    showShortfallPanel.add(radioPanel);

    JLabel startupChartsLabel = new JLabel("Select Charts to display:", JLabel.LEFT);
    showInventoryCheck = new JCheckBox(SHOW_INVENTORY_CHART, prefData.showInventoryChart);
    showInventoryCheck.setActionCommand(SHOW_INVENTORY_CHART);
    showInventoryCheck.addItemListener(this);
    showRefillCheck = new JCheckBox(SHOW_REFILL_CHART, prefData.showRefillChart);
    showRefillCheck.setActionCommand(SHOW_REFILL_CHART);
    showRefillCheck.addItemListener(this);
    showDemandCheck = new JCheckBox(SHOW_DEMAND_CHART, prefData.showDemandChart);
    showDemandCheck.setActionCommand(SHOW_DEMAND_CHART);
    showDemandCheck.addItemListener(this);

    JPanel showChartsPanel = new JPanel();
    showChartsPanel.setLayout(new GridLayout(2, 1));
    showChartsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    showChartsPanel.add(startupChartsLabel);
    JPanel checkPanel = new JPanel();
    checkPanel.setLayout(new GridLayout(1, 3));
    checkPanel.add(showInventoryCheck);
    checkPanel.add(showRefillCheck);
    checkPanel.add(showDemandCheck);
    showChartsPanel.add(checkPanel);

    int gridx = 0;
    int gridy = 0;
    Insets blankInsets = new Insets(5, 10, 5, 10);

    int height = 1;
    gridy += height;

    add(timeScalePanel, new GridBagConstraints(gridx,
                                               gridy,
                                               1, height, 1.0, 0.8,
                                               GridBagConstraints.WEST,
                                               GridBagConstraints.HORIZONTAL,
                                               blankInsets, 0, 0));

    height = 1;
    gridy += height;

    add(showShortfallPanel, new GridBagConstraints(gridx,
                                                   gridy,
                                                   1, height, 1.0, 0.8,
                                                   GridBagConstraints.WEST,
                                                   GridBagConstraints.HORIZONTAL,
                                                   blankInsets, 0, 0));

    height = 1;
    gridy += height;

    add(showChartsPanel, new GridBagConstraints(gridx,
                                                gridy,
                                                1, height, 1.0, 0.8,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.HORIZONTAL,
                                                blankInsets, 0, 0));


    height = 2;
    gridy += height;

    JPanel spaceFiller = new JPanel();
    spaceFiller.add(Box.createVerticalStrut(30));

    add(spaceFiller, new GridBagConstraints(gridx,
                                            gridy,
                                            1, height, 1.0, 1.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.BOTH,
                                            blankInsets, 0, 0));


  }

  public void flushValuesToData() {
    if (prefsChanged()) {
      prefData.startupWCDay = timeScaleGroup.getSelection().getActionCommand().equals(CDAY_TIME_SCALE);
      prefData.displayShortfall = showShortfallGroup.getSelection().getActionCommand().equals(DISPLAY_SHORTFALL);
      prefData.showInventoryChart = showInventoryCheck.isSelected();
      prefData.showRefillChart = showRefillCheck.isSelected();
      prefData.showDemandChart = showDemandCheck.isSelected();
    }
    super.flushValuesToData();
  }


}


