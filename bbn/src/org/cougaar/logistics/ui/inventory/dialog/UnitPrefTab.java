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


public class UnitPrefTab extends PreferenceDialogTab {

  public static final String LITERS_LABEL = InventoryPreferenceData.LITERS_LABEL;
  public static final String CASES_LABEL = InventoryPreferenceData.CASES_LABEL;
  public static final String BOTTLES_LABEL = InventoryPreferenceData.BOTTLES_LABEL;
  public static final String TONS_LABEL = InventoryPreferenceData.TONS_LABEL;
  public static final String ROUNDS_LABEL = InventoryPreferenceData.ROUNDS_LABEL;

  public static final String ROUND_UNITS_CHECK_LABEL = "Round Units to the nearest whole number";

  protected ButtonGroup ammoUnitGroup;
  protected ButtonGroup waterUnitGroup;

  protected JCheckBox roundUnitsCheck;

  public UnitPrefTab(InventoryUIFrame invUIFrame,
                     InventoryPreferenceDialog invUIDialog,
                     InventoryPreferenceData data) {
    super(invUIFrame, invUIDialog, data);
  }


  protected void doTabLayout() {

    setLayout(new GridBagLayout());

    JLabel ammoUnitLabel = new JLabel("Preferred Ammo Unit: ", JLabel.LEFT);
    ammoUnitGroup = new ButtonGroup();
    JRadioButton tonsRadio = new JRadioButton(TONS_LABEL, prefData.ammoUnit == prefData.getUnit(TONS_LABEL));
    tonsRadio.setActionCommand(TONS_LABEL);
    tonsRadio.addItemListener(this);
    JRadioButton roundsRadio = new JRadioButton(ROUNDS_LABEL, prefData.ammoUnit == prefData.getUnit(ROUNDS_LABEL));
    roundsRadio.setActionCommand(ROUNDS_LABEL);
    roundsRadio.addItemListener(this);

    ammoUnitGroup.add(tonsRadio);
    ammoUnitGroup.add(roundsRadio);

    JPanel ammoUnitPanel = new JPanel();
    ammoUnitPanel.setLayout(new GridLayout(2, 1));
    ammoUnitPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    ammoUnitPanel.add(ammoUnitLabel);
    JPanel radioPanel = new JPanel();
    radioPanel.setLayout(new GridLayout(1, 2));
    radioPanel.add(tonsRadio);
    radioPanel.add(roundsRadio);
    ammoUnitPanel.add(radioPanel);


    JLabel waterUnitLabel = new JLabel("Preferred Water Unit: ", JLabel.LEFT);
    waterUnitGroup = new ButtonGroup();
    JRadioButton casesRadio = new JRadioButton(CASES_LABEL, prefData.waterUnit == prefData.getUnit(CASES_LABEL));
    casesRadio.setActionCommand(CASES_LABEL);
    casesRadio.addItemListener(this);
    JRadioButton bottlesRadio = new JRadioButton(BOTTLES_LABEL, prefData.waterUnit == prefData.getUnit(BOTTLES_LABEL));
    bottlesRadio.setActionCommand(BOTTLES_LABEL);
    bottlesRadio.addItemListener(this);
    JRadioButton litersRadio = new JRadioButton(LITERS_LABEL, prefData.waterUnit == prefData.getUnit(LITERS_LABEL));
    litersRadio.setActionCommand(LITERS_LABEL);
    litersRadio.addItemListener(this);

    waterUnitGroup.add(casesRadio);
    waterUnitGroup.add(bottlesRadio);
    waterUnitGroup.add(litersRadio);

    JPanel waterUnitPanel = new JPanel();
    waterUnitPanel.setLayout(new GridLayout(2, 1));
    waterUnitPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    waterUnitPanel.add(waterUnitLabel);
    radioPanel = new JPanel();
    radioPanel.setLayout(new GridLayout(1, 3));
    radioPanel.add(casesRadio);
    radioPanel.add(bottlesRadio);
    radioPanel.add(litersRadio);
    waterUnitPanel.add(radioPanel);


    roundUnitsCheck = new JCheckBox(ROUND_UNITS_CHECK_LABEL, prefData.roundToWholes);
    roundUnitsCheck.setActionCommand(ROUND_UNITS_CHECK_LABEL);
    roundUnitsCheck.addItemListener(this);


    JPanel roundUnitsPanel = new JPanel();
    roundUnitsPanel.setLayout(new GridLayout(1, 1));
    roundUnitsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

    roundUnitsPanel.add(roundUnitsCheck);


    int gridx = 0;
    int gridy = 0;
    Insets blankInsets = new Insets(5, 10, 5, 10);

    int height = 1;
    gridy += height;

    add(ammoUnitPanel, new GridBagConstraints(gridx,
                                              gridy,
                                              1, height, 1.0, 0.9,
                                              GridBagConstraints.WEST,
                                              GridBagConstraints.HORIZONTAL,
                                              blankInsets, 0, 0));


    height = 1;
    gridy += height;

    add(waterUnitPanel, new GridBagConstraints(gridx,
                                               gridy,
                                               1, height, 1.0, 0.8,
                                               GridBagConstraints.WEST,
                                               GridBagConstraints.HORIZONTAL,
                                               blankInsets, 0, 0));

    height = 1;
    gridy += height;

    add(roundUnitsPanel, new GridBagConstraints(gridx,
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
      prefData.ammoUnit = prefData.getUnit(ammoUnitGroup.getSelection().getActionCommand());
      prefData.waterUnit = prefData.getUnit(waterUnitGroup.getSelection().getActionCommand());
      prefData.roundToWholes = roundUnitsCheck.isSelected();
    }
    super.flushValuesToData();
  }


}


