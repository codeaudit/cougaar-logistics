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


import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.FlowLayout;
import java.awt.Cursor;
import java.awt.Component;
import java.awt.event.*;

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


public abstract class PreferenceDialogTab extends JPanel implements ItemListener {


  InventoryPreferenceData   prefData;
  InventoryPreferenceDialog parentDialog;
  InventoryUIFrame          parentFrame;

  Logger                    logger;

  boolean itemChange;




  public PreferenceDialogTab(InventoryUIFrame invUIFrame,
				InventoryPreferenceDialog invUIDialog,
				InventoryPreferenceData data) {
      super();
      parentFrame = invUIFrame;
      parentDialog = invUIDialog;
      prefData = data;
      itemChange = false;
      initializePrefTab();
  }



  protected void initializePrefTab() {

    logger = Logging.getLogger(this);
    doTabLayout();
    setVisible(true);
  }

  protected abstract void doTabLayout();

  public void flushValuesToData() {
    itemChange = false;
  }

  public void itemStateChanged(ItemEvent e) {
     itemChange = true;
  }

  public boolean prefsChanged() { return itemChange; }
}


