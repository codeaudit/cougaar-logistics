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
import org.cougaar.logistics.ui.inventory.dialog.ColorTab;
import org.cougaar.logistics.ui.inventory.InventorySelectionListener;
import org.cougaar.logistics.ui.inventory.InventorySelectionEvent;
import org.cougaar.logistics.ui.inventory.InventoryUIFrame;
import org.cougaar.logistics.ui.inventory.InventoryMenuEvent;

/**
 * <pre>
 *
 * The InventoryPreferenceDialog is the Dialog used to configure the options for the
 * the Inventory GUI.    You can configure Labels, Colors, Start Up Preferences etc.
 * The data is persisted from session to session so that the user can customize
 * his/her Inventory GUI to run they want.
 *
 * @see org.cougaar.logistics.ui.inventory.InventoryUIFrame
 *
 **/


public class InventoryPreferenceDialog extends JDialog
    implements ActionListener, InventorySelectionListener {


  public static final String PREFERENCE_DIALOG_TITLE = "Preferences";
  public static final String OK_LABEL = "OK";
  public static final String CANCEL_LABEL = "Cancel";
  public static final String APPLY_LABEL = "Apply";

  static final Cursor waitCursor =
      Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
  static final Cursor defaultCursor =
      Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

  private Container contentPane;

  protected JTextArea editPane;
  protected JFileChooser fileChooser;

  protected InventoryUIFrame mainFrame;
  protected InventoryPreferenceData origPrefData;
  protected InventoryPreferenceData newPrefData;
  protected Logger logger;

  protected JTabbedPane prefTabs;

  String cip;
  String helpFileStr;
  String defaultSavePrefPath;


  public InventoryPreferenceDialog(InventoryUIFrame invUIFrame,
                                   InventoryPreferenceData data) {
    super(invUIFrame, PREFERENCE_DIALOG_TITLE, true);
    mainFrame = invUIFrame;
    origPrefData = data;
    newPrefData = (InventoryPreferenceData) data.clone();
    initializePrefDialog();
  }


  protected void initializePrefDialog() {

    logger = Logging.getLogger(this);

    contentPane = getRootPane().getContentPane();

    cip = System.getProperty("org.cougaar.install.path");

    if ((cip == null) ||
        (cip.trim().equals(""))) {
      logger.error("org.cougaar.install.path is not defined in Command line");
    } else {
      helpFileStr = cip + File.separator + "albbn" + File.separator + "doc" + File.separator + "alinvgui" + File.separator + "index.htm";
    }

    String baseDir = System.getProperty("org.cougaar.workspace");
    if ((baseDir == null) ||
        (baseDir.trim().equals(""))) {
      baseDir = cip + File.separator + "workspace";
    }
    defaultSavePrefPath = baseDir + File.separator + "INVGUICSV";
    File pathDirs = new File(defaultSavePrefPath);

    fileChooser = new JFileChooser(pathDirs);
// fills frame
    doMyLayout();

    pack();
    setSize(450, 350);

    Point mainLoc = mainFrame.getLocation();

    setLocation((int) mainLoc.getX() + (int) this.getSize().getWidth() / 2,
                (int) mainLoc.getY() + (int) this.getSize().getHeight() / 2);

    //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
// set location to middle of the screen
    //setLocation(screenSize.width / 2 - (int) this.getSize().getWidth() / 2,
    //            screenSize.height / 2 - (int) this.getSize().getHeight() / 2);
    //setVisible(true);
  }

  protected void doMyLayout() {
    //getRootPane().setJMenuBar(makeMenus());

    prefTabs = new JTabbedPane();

    contentPane.add(prefTabs, BorderLayout.CENTER);
    prefTabs.setPreferredSize(new Dimension(450, 350));

    JPanel editPanel = new JPanel();
    editPanel.setLayout(new BorderLayout());

    JButton okButton = new JButton("OK");
    JButton applyButton = new JButton("Apply");
    JButton cancelButton = new JButton("Cancel");
    okButton.addActionListener(this);
    applyButton.addActionListener(this);
    cancelButton.addActionListener(this);


    JPanel bottomPanel = new JPanel();
    bottomPanel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(3, 3, 3, 3),
            bottomPanel.getBorder()));
    bottomPanel.setLayout(new BorderLayout());
    bottomPanel.add(Box.createHorizontalStrut(100), BorderLayout.WEST);
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(okButton);
    buttonPanel.add(applyButton);
    buttonPanel.add(cancelButton);
    bottomPanel.add(buttonPanel, BorderLayout.EAST);

    prefTabs.add("Startup Prefs", new StartUpTab(mainFrame, this, newPrefData));
    prefTabs.add("Color Prefs", new ColorTab(mainFrame, this, newPrefData));
    prefTabs.add("Unit Prefs", new UnitPrefTab(mainFrame, this, newPrefData));

    //Handle window closing correctly.
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    contentPane.add(bottomPanel, BorderLayout.SOUTH);


  }

  protected JMenuBar makeMenus() {
    JMenuBar retval = new JMenuBar();

    JMenu file = new JMenu("File");
    JMenuItem quit = new JMenuItem(InventoryMenuEvent.MENU_Exit);
    JMenuItem save = new JMenuItem(InventoryMenuEvent.MENU_SaveXML);
    JMenuItem open = new JMenuItem(InventoryMenuEvent.MENU_OpenXML);
    quit.addActionListener(this);
    save.addActionListener(this);
    open.addActionListener(this);
    file.add(quit);
    file.add(open);
    file.add(save);
    retval.add(file);

    JMenu connection = new JMenu("Connection");
    JMenuItem connect = new JMenuItem(InventoryMenuEvent.MENU_Connect);
    connect.addActionListener(this);
    connection.add(connect);
    retval.add(connection);

    JMenu helpMenu = new JMenu("Help");
    JMenuItem helpItem = new JMenuItem(InventoryMenuEvent.MENU_Help);
    helpItem.addActionListener(this);
    helpMenu.add(helpItem);
//Help popup is not done yet.
//retval.add(helpMenu);

    return retval;
  }

  public boolean notifyMainFrameOfChanges() {
    boolean changes = false;
    for (int i = 0; i < prefTabs.getTabCount(); i++) {
      PreferenceDialogTab tab = (PreferenceDialogTab) prefTabs.getComponentAt(i);
      if (tab.prefsChanged()) {
        tab.flushValuesToData();
        changes = true;
      }
    }
    if (changes) {
      mainFrame.prefDataChanged(origPrefData, newPrefData);
      origPrefData = (InventoryPreferenceData) newPrefData.clone();
    }

    return changes;
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(OK_LABEL)) {
      notifyMainFrameOfChanges();
      mainFrame.savePrefData(newPrefData);
      setVisible(false);
    } else if (e.getActionCommand().equals(CANCEL_LABEL)) {
      setVisible(false);
    } else if (e.getActionCommand().equals(APPLY_LABEL)) {
      notifyMainFrameOfChanges();
    }
  }

  protected void popupHelpPage() {
  }

  protected void saveXML() {
    File pathDirs = new File(defaultSavePrefPath);
    try {
      if (!pathDirs.exists()) {
        pathDirs.mkdirs();
      }
    } catch (Exception ex) {
      logger.error("Error creating default directory " + defaultSavePrefPath, ex);
    }
    if (newPrefData != null) {
      String fileID = defaultSavePrefPath + "InventoryUIPrefs.txt";
      System.out.println("Save to file: " + fileID);
      fileChooser.setSelectedFile(new File(fileID));
    }
    int retval = fileChooser.showSaveDialog(this);
    if (retval == fileChooser.APPROVE_OPTION) {
      File saveFile = fileChooser.getSelectedFile();
      try {
        FileWriter fw = new FileWriter(saveFile);
        fw.write(editPane.getText());
        fw.flush();
        fw.close();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

  }


  public static void timeConsumingTaskStart(Component c) {
    JFrame frame = null;
    if (c != null)
      frame = (JFrame) SwingUtilities.getRoot(c);
    if (frame == null)
      return;
    timeConsumingTaskStart(frame);
  }

  public static void timeConsumingTaskStart(JFrame frame) {
    Component glass = frame.getGlassPane();
    glass.setCursor(waitCursor);
    glass.addMouseListener(new DoNothingMouseListener());
    glass.setVisible(true);
  }

  public static void timeConsumingTaskEnd(Component c) {
    JFrame frame = null;
    if (c != null)
      frame = (JFrame) SwingUtilities.getRoot(c);
    if (frame == null)
      return;
    timeConsumingTaskEnd(frame);
  }

  public static void timeConsumingTaskEnd(JFrame frame) {
    Component glass = frame.getGlassPane();
    MouseListener[] mls =
        (MouseListener[]) glass.getListeners(MouseListener.class);
    for (int i = 0; i < mls.length; i++)
      if (mls[i] instanceof DoNothingMouseListener)
        glass.removeMouseListener(mls[i]);
    glass.setCursor(defaultCursor);
    glass.setVisible(false);
  }


  private static void displayErrorString(String reply) {
    JOptionPane.showMessageDialog(null, reply, reply,
                                  JOptionPane.ERROR_MESSAGE);
  }

  public void selectionChanged(InventorySelectionEvent e) {

  }


  protected class HelpDialog extends JDialog {

    protected String helpFileURLStr;

    public HelpDialog(JFrame owner,
                      String aHelpFileURLStr) {
      super(owner, "Inventory GUI Help", false);
      helpFileURLStr = aHelpFileURLStr;
      setupAndLayout();
    }

    protected void setupAndLayout() {

    }

  }


  protected class TaskStartRunnable implements Runnable {

    InventoryUIFrame frame;

    public TaskStartRunnable(InventoryUIFrame aFrame) {
      frame = aFrame;
    }

    public void run() {
      timeConsumingTaskStart(frame);
    }
  }

  static class DoNothingMouseListener extends MouseInputAdapter {
    public void mouseClicked(MouseEvent e) {
      e.consume();
    }

    public void mouseDragged(MouseEvent e) {
      e.consume();
    }

    public void mouseEntered(MouseEvent e) {
      e.consume();
    }

    public void mouseExited(MouseEvent e) {
      e.consume();
    }

    public void mouseMoved(MouseEvent e) {
      e.consume();
    }

    public void mousePressed(MouseEvent e) {
      e.consume();
    }

    public void mouseReleased(MouseEvent e) {
      e.consume();
    }
  }
}


