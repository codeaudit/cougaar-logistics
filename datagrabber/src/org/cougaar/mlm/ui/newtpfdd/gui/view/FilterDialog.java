/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;

import java.awt.Font;

import org.cougaar.mlm.ui.newtpfdd.gui.view.query.Query;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.ListQueryResponse;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.QueryHandler;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

public class FilterDialog extends JDialog implements ActionListener, TreeSelectionListener
{
    private JCheckBox ivjincludeCargoCheckBox = null;
    private JCheckBox ivjincludeCarrierCheckBox = null;
    private JPanel ivjQueryContentPane = null;
    private JCheckBox ivjnewWindowCheckBox = null;
    private JCheckBox ivjrollupCheckBox = null;
    private JButton ivjincludeHierarchyButton = null;
    private JButton getManifestInfoButton = null;

    private JLabel ivjcargoTypeLabel = null;
    private JLabel ivjcarrierTypeLabel = null;
    private JLabel ivjcargoNameLabel = null;
    private JLabel ivjcarrierNameLabel = null;

    private JScrollPane ivjcargoTypeScroller = null;
    private JScrollPane ivjcarrierTypeScroller = null;
    private JScrollPane ivjcargoNameScroller = null;
    private JScrollPane ivjcarrierNameScroller = null;

    private JList ivjcargoTypeList = null;
    private JList ivjcarrierTypeList = null;
    private JList ivjcargoNameList = null;
    private JList ivjcarrierNameList = null;

    private JRadioButton ivjcargoByTypeRadio = null;
    private JRadioButton ivjcarrierByTypeRadio = null;
    private JRadioButton ivjcargoByNameRadio = null;
    private JRadioButton ivjcarrierByNameRadio = null;

    private UnitPanel ivjunitPanel = null;
    private JPanel ivjcargoSelectorPanel = null;
    private JPanel ivjcarrierSelectorPanel = null;
    private JPanel ivjunitSelectorPanel = null;
    private JButton ivjsendQueryButton = null;
    private JButton aggregateButton = null;
    private JButton unitAggregateButton = null;

    // user code members
    private ButtonGroup carrierRadioGroup;
    private ButtonGroup cargoRadioGroup;
    private Vector listeners;

  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.FilterDialog.debug", 
									   "false"));
  private int unitPanelWidth = Integer.getInteger ("org.cougaar.mlm.ui.newtpfdd.gui.view.FilterDialog.unitPanelWidth", 
												   280).intValue ();
  boolean showAggregateButton = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.FilterDialog.showAggregateButton", 
									   "true"));

  private NewTPFDDShell shell;
  Font myFont;
  DatabaseState dbState;
  
  /** The active worker. */
  private transient TPFDDSwingWorker worker;

  private DefaultListModel cargoTypeListModel = new DefaultListModel();
  private DefaultListModel carrierTypeListModel = new DefaultListModel();
  private DefaultListModel cargoNameListModel = new DefaultListModel();
  private DefaultListModel carrierNameListModel = new DefaultListModel();
  
    public FilterDialog(NewTPFDDShell shell, Font font)
    {
	super();
	//	if (debug)
	//	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterDialog ctor, run " + run);

	//	this.taskModel = new TaskModel (dbConfig);
	this.dbState = shell;
	listeners = new Vector();
	setName(NewTPFDDShell.TPFDDString + " Filter Dialog");
	setTitle(NewTPFDDShell.TPFDDString + " Filter Dialog");
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	setBounds(25, 50, 720, 725);
	this.shell = shell;
	myFont = font;
	setContentPane(getQueryContentPane());
	getRootPane().setDefaultButton(getsendQueryButton ());

    }

    /**
     * Return the includeCargoCheckBox property value.
     * @return JCheckBox
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JCheckBox getincludeCargoCheckBox() {
	if (ivjincludeCargoCheckBox == null) {
	    try {
		ivjincludeCargoCheckBox = new JCheckBox();
		ivjincludeCargoCheckBox.setName("includeCargoCheckBox");
		ivjincludeCargoCheckBox.setText("Use Cargo Criteria");
		ivjincludeCargoCheckBox.setSelected(true);
		ivjincludeCargoCheckBox.addActionListener(this);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjincludeCargoCheckBox;
    }

    /**
     * Return the JCheckBox1 property value.
     * @return JCheckBox
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JCheckBox getincludeCarrierCheckBox() {
	if (ivjincludeCarrierCheckBox == null) {
	    try {
		ivjincludeCarrierCheckBox = new JCheckBox();
		ivjincludeCarrierCheckBox.setName("includeCarrierCheckBox");
		ivjincludeCarrierCheckBox.setText("Use Carrier Criteria");
		ivjincludeCarrierCheckBox.setSelected(false);
		getcarrierByTypeRadio().setEnabled(false);
		getcarrierByNameRadio().setEnabled(false);
		getcarrierTypeList().setEnabled(false);
		getcarrierTypeScroller().setEnabled(false);
		getcarrierNameList().setEnabled(false);
		getcarrierNameScroller().setEnabled(false);
		ivjincludeCarrierCheckBox.addActionListener(this);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjincludeCarrierCheckBox;
    }
    
    /**
     * Return the includeHierarchyCheck property value.
     * @return JCheckBox
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JButton getincludeHierarchyButton() {
	if (ivjincludeHierarchyButton == null) {
	    try {
		ivjincludeHierarchyButton = new JButton();
		ivjincludeHierarchyButton.setName("includeHierarchyButton");
		ivjincludeHierarchyButton.setText("Select Subordinates");
		ivjincludeHierarchyButton.addActionListener(this);
		ivjincludeHierarchyButton.setActionCommand("include hierarchy");
		ivjincludeHierarchyButton.setEnabled(false);
		
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjincludeHierarchyButton;
    }
    
    private JButton makeButton(String label, String command) {
	  JButton button = new JButton();
	  button.setText(label);
	  button.setActionCommand (command);
	  button.addActionListener(this);
	  button.setEnabled (false);
	  return button;
    }

    private DefaultListSelectionModel getnewselectionModel() {
	DefaultListSelectionModel selectionModel = null;
	try {
	    selectionModel = new DefaultListSelectionModel();
	    selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	} catch (Exception ivjExc) {
	    handleException(ivjExc);
	}
	return selectionModel;
    }

    /**
     * Return the cargoTypeLabel property value.
     * @return JLabel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JLabel getcargoTypeLabel() {
	if (ivjcargoTypeLabel == null) {
	    try {
		ivjcargoTypeLabel = new JLabel();
		ivjcargoTypeLabel.setName("cargoTypeLabel");
		ivjcargoTypeLabel.setText("Type:");
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoTypeLabel;
    }

    /**
     * Return the carrierTypeLabel property value.
     * @return JLabel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JLabel getcarrierTypeLabel() {
	if (ivjcarrierTypeLabel == null) {
	    try {
		ivjcarrierTypeLabel = new JLabel();
		ivjcarrierTypeLabel.setName("carrierTypeLabel");
		ivjcarrierTypeLabel.setText("Type:");
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierTypeLabel;
    }

    /**
     * Return the cargoNameLabel property value.
     * @return JLabel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JLabel getcargoNameLabel() {
	if (ivjcargoNameLabel == null) {
	    try {
		ivjcargoNameLabel = new JLabel();
		ivjcargoNameLabel.setName("cargoNameLabel");
		ivjcargoNameLabel.setText("Name:");
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoNameLabel;
    }

    /**
     * Return the JLabel property value.
     * @return JLabel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JLabel getcarrierNameLabel() {
	if (ivjcarrierNameLabel == null) {
	    try {
		ivjcarrierNameLabel = new JLabel();
		ivjcarrierNameLabel.setName("carrierNameLabel");
		ivjcarrierNameLabel.setText("Name:");
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierNameLabel;
    }

    private JScrollPane getcargoTypeScroller() {
	if (ivjcargoTypeScroller == null) {
	    try {
		ivjcargoTypeScroller = new JScrollPane(getcargoTypeList());
		ivjcargoTypeScroller.setName("cargoTypeScroller");
		ivjcargoTypeScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoTypeScroller;
    }
 
    private JScrollPane getcarrierTypeScroller() {
	if (ivjcarrierTypeScroller == null) {
	    try {
		ivjcarrierTypeScroller = new JScrollPane(getcarrierTypeList());
		ivjcarrierTypeScroller.setName("carrierTypeScroller");
		ivjcarrierTypeScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierTypeScroller;
    }
 
    private JScrollPane getcargoNameScroller() {
	if (ivjcargoNameScroller == null) {
	    try {
		ivjcargoNameScroller = new JScrollPane(getcargoNameList());
		ivjcargoNameScroller.setName("cargoNameScroller");
		ivjcargoNameScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoNameScroller;
    }

    private JScrollPane getcarrierNameScroller() {
	if (ivjcarrierNameScroller == null) {
	    try {
		ivjcarrierNameScroller = new JScrollPane(getcarrierNameList());
		ivjcarrierNameScroller.setName("carrierNameScroller");
		ivjcarrierNameScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierNameScroller;
    }

    private JList getcargoTypeList() {
	if (ivjcargoTypeList == null) {
	    try {
		ivjcargoTypeList = new JList(cargoTypeListModel);
		ivjcargoTypeList.setName("cargoTypeList");
		ivjcargoTypeList.setSelectionModel(getnewselectionModel());
		ivjcargoTypeList.setCellRenderer (listRenderer);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoTypeList;
    }

    private JList getcarrierTypeList() {
	if (ivjcarrierTypeList == null) {
	    try {
		ivjcarrierTypeList = new JList(carrierTypeListModel);
		ivjcarrierTypeList.setName("carrierTypeList");
		ivjcarrierTypeList.setSelectionModel(getnewselectionModel());
		ivjcarrierTypeList.setCellRenderer (listRenderer);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierTypeList;
    }

    private JList getcargoNameList() {
	if (ivjcargoNameList == null) {
	    try {
		ivjcargoNameList = new JList(cargoNameListModel);
		ivjcargoNameList.setName("cargoNameList");
		ivjcargoNameList.setSelectionModel(getnewselectionModel());
		ivjcargoNameList.setCellRenderer (listRenderer);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoNameList;
    }

    private JList getcarrierNameList() {
	if (ivjcarrierNameList == null) {
	    try {
		ivjcarrierNameList = new JList(carrierNameListModel);
		ivjcarrierNameList.setName("carrierNameList");
		ivjcarrierNameList.setSelectionModel(getnewselectionModel());
		ivjcarrierNameList.setCellRenderer (listRenderer);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierNameList;
    }


 protected ListCellRenderer listRenderer = new DefaultListCellRenderer () {
     public Component getListCellRendererComponent(
         JList list,
         Object value,
         int index,
         boolean isSelected,
         boolean cellHasFocus)
     {
	   Component comp = 
		 super.getListCellRendererComponent (list, value, index, isSelected, cellHasFocus);
	   
	   setText(trim(((Node)value).getDisplayName()));
	   return comp;
     }

	 protected String trim (String str) {
	   int index = str.indexOf ('(');
	   return (index != -1) ? str.substring (0, index) : str;
	 }

   };
 
    /**
     * Return the cargoByTypeRadio property value.
     * @return JRadioButton
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JRadioButton getcargoByTypeRadio() {
	if (ivjcargoByTypeRadio == null) {
	    try {
		ivjcargoByTypeRadio = new JRadioButton();
		ivjcargoByTypeRadio.setName("cargoByTypeRadio");
		ivjcargoByTypeRadio.setText("Type");
		// user code begin {1}
		ivjcargoByTypeRadio.addActionListener(this);
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoByTypeRadio;
    }

    /**
     * Return the carrierByTypeRadio property value.
     * @return JRadioButton
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JRadioButton getcarrierByTypeRadio() {
	if (ivjcarrierByTypeRadio == null) {
	    try {
		ivjcarrierByTypeRadio = new JRadioButton();
		ivjcarrierByTypeRadio.setName("carrierByTypeRadio");
		ivjcarrierByTypeRadio.setText("Type");
		// user code begin {1}
		ivjcarrierByTypeRadio.addActionListener(this);
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierByTypeRadio;
    }

    /**
     * Return the cargoByNameRadio property value.
     * @return JRadioButton
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JRadioButton getcargoByNameRadio() {
	if (ivjcargoByNameRadio == null) {
	    try {
		ivjcargoByNameRadio = new JRadioButton();
		ivjcargoByNameRadio.setName("cargoByNameRadio");
		ivjcargoByNameRadio.setText("Name");
		// user code begin {1}
		ivjcargoByNameRadio.addActionListener(this);
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoByNameRadio;
    }

    /**
     * Return the carrierByNameRadio property value.
     * @return JRadioButton
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JRadioButton getcarrierByNameRadio() {
	if (ivjcarrierByNameRadio == null) {
	    try {
		ivjcarrierByNameRadio = new JRadioButton();
		ivjcarrierByNameRadio.setName("carrierByNameRadio");
		ivjcarrierByNameRadio.setText("Name");
		// user code begin {1}
		ivjcarrierByNameRadio.addActionListener(this);
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierByNameRadio;
    }

    /**
     * Return the newWindowCheckBox property value.
     * @return JCheckBox
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JCheckBox getnewWindowCheckBox() {
	if (ivjnewWindowCheckBox == null) {
	    try {
		ivjnewWindowCheckBox = new JCheckBox();
		ivjnewWindowCheckBox.setName("newWindowCheckBox");
		ivjnewWindowCheckBox.setText("Open views in new window");
		ivjnewWindowCheckBox.setSelected(true);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjnewWindowCheckBox;
    }

    private JCheckBox getrollupCheckBox() {
	if (ivjrollupCheckBox == null) {
	    try {
		ivjrollupCheckBox = new JCheckBox();
		ivjrollupCheckBox.setName("rollupCheckBox");
		ivjrollupCheckBox.setText("Aggregate by Cargo Type");
		ivjrollupCheckBox.setSelected(false);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjrollupCheckBox;
    }

    private JCheckBox getSortByNameCheckBox() {
	if (ivjrollupCheckBox == null) {
	    try {
		ivjrollupCheckBox = new JCheckBox();
		ivjrollupCheckBox.setName("sortByNameCheckBox");
		ivjrollupCheckBox.setText("Sort By Name");
		ivjrollupCheckBox.setSelected(false);
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjrollupCheckBox;
    }

    /**
       Return the sendQueryButton property value.
       Public so that external views which receive button events can check and act
       on this one.
       @return JButton
    */
    public JButton getsendQueryButton() {
	if ( ivjsendQueryButton == null) {
	    try {
		ivjsendQueryButton = new JButton();
		ivjsendQueryButton.setName("sendQueryButton");
		ivjsendQueryButton.setText("Display " + NewTPFDDShell.TPFDDString);
		ivjsendQueryButton.setActionCommand("Display TPFDD");
		ivjsendQueryButton.addActionListener(this);
		ivjsendQueryButton.setEnabled (false);
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjsendQueryButton;
    }

    /**
     * Return the QueryContentPane property value.
     * @return JPanel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JPanel getQueryContentPane() {
	if (ivjQueryContentPane == null) {
	    try {
		ivjQueryContentPane = new JPanel();
		ivjQueryContentPane.setName("QueryContentPane");
		ivjQueryContentPane.setLayout(new GridBagLayout());

		//GridBagConstraints constraintsnewWindowCheckBox = new GridBagConstraints();
		//constraintsnewWindowCheckBox.gridx = 1; constraintsnewWindowCheckBox.gridy = 0;
		//constraintsnewWindowCheckBox.anchor = GridBagConstraints.WEST;
		//constraintsnewWindowCheckBox.insets = new Insets(4, 4, 4, 4);
		//		getQueryContentPane().add(getnewWindowCheckBox(), constraintsnewWindowCheckBox);

		//		GridBagConstraints constraintsrollupCheckBox = new GridBagConstraints();
		//		constraintsrollupCheckBox.gridx = 2; constraintsrollupCheckBox.gridy = 0;
		//		constraintsrollupCheckBox.anchor = GridBagConstraints.WEST;
		//		constraintsrollupCheckBox.insets = new Insets(4, 4, 4, 4);
		//		getQueryContentPane().add(getrollupCheckBox(), constraintsrollupCheckBox);

		GridBagConstraints constraintsSortByNameCheckBox = new GridBagConstraints();
		constraintsSortByNameCheckBox.gridx = 2; constraintsSortByNameCheckBox.gridy = 0;
		constraintsSortByNameCheckBox.anchor = GridBagConstraints.WEST;
		constraintsSortByNameCheckBox.insets = new Insets(4, 4, 4, 4);
		getQueryContentPane().add(getSortByNameCheckBox(), constraintsSortByNameCheckBox);

		GridBagConstraints constraintsincludeCarrierCheckBox = new GridBagConstraints();
		constraintsincludeCarrierCheckBox.gridx = 1; constraintsincludeCarrierCheckBox.gridy = 1;
		constraintsincludeCarrierCheckBox.anchor = GridBagConstraints.WEST;
		constraintsincludeCarrierCheckBox.insets = new Insets(4, 4, 4, 4);
		constraintsincludeCarrierCheckBox.fill = GridBagConstraints.HORIZONTAL;
		constraintsincludeCarrierCheckBox.weightx = 1.0;
		getQueryContentPane().add(getincludeCarrierCheckBox(), constraintsincludeCarrierCheckBox);

		GridBagConstraints constraintsincludeCargoCheckBox = new GridBagConstraints();
		constraintsincludeCargoCheckBox.gridx = 3; constraintsincludeCargoCheckBox.gridy = 1;
		constraintsincludeCargoCheckBox.anchor = GridBagConstraints.WEST;
		constraintsincludeCargoCheckBox.insets = new Insets(4, 4, 4, 4);
		constraintsincludeCargoCheckBox.fill = GridBagConstraints.HORIZONTAL;
		constraintsincludeCargoCheckBox.weightx = 1.0;
		getQueryContentPane().add(getincludeCargoCheckBox(), constraintsincludeCargoCheckBox);


		GridBagConstraints constraintscarrierSelectorPanel = new GridBagConstraints();
		constraintscarrierSelectorPanel.gridx = 1; constraintscarrierSelectorPanel.gridy = 2;
		constraintscarrierSelectorPanel.gridwidth = 3;
		constraintscarrierSelectorPanel.fill = GridBagConstraints.BOTH;
		constraintscarrierSelectorPanel.weightx = 1.0;
		constraintscarrierSelectorPanel.weighty = 1.0;
		constraintscarrierSelectorPanel.insets = new Insets(4, 4, 4, 4);
		getQueryContentPane().add(getcarrierSelectorPanel(), constraintscarrierSelectorPanel);

		GridBagConstraints constraintscargoSelectorPanel = new GridBagConstraints();
		constraintscargoSelectorPanel.gridx = 1; constraintscargoSelectorPanel.gridy = 3;
		constraintscargoSelectorPanel.gridwidth = 3;
		constraintscargoSelectorPanel.fill = GridBagConstraints.BOTH;
		constraintscargoSelectorPanel.weightx = 1.0;
		constraintscargoSelectorPanel.weighty = 1.0;
		constraintscargoSelectorPanel.insets = new Insets(4, 4, 4, 4);
		getQueryContentPane().add(getcargoSelectorPanel(), constraintscargoSelectorPanel);

		GridBagConstraints constraintssendQueryButton = new GridBagConstraints();
		constraintssendQueryButton.gridx = 1; constraintssendQueryButton.gridy = 4;
		constraintssendQueryButton.anchor = GridBagConstraints.WEST;
		constraintssendQueryButton.gridwidth = 1;
		constraintssendQueryButton.weightx = 1.0;
		constraintssendQueryButton.insets = new Insets(4, 4, 4, 4);
		getQueryContentPane().add(getsendQueryButton(), constraintssendQueryButton);
		
		GridBagConstraints constraintsAggTPFDDButton = new GridBagConstraints();
		constraintsAggTPFDDButton.gridx = 2; constraintsAggTPFDDButton.gridy = 4;
		constraintsAggTPFDDButton.gridwidth = 1;
		constraintsAggTPFDDButton.weightx = 1.0;
		constraintsAggTPFDDButton.insets = new Insets(4, 4, 4, 4);
		aggregateButton = makeButton ("Aggregate TPFDD","Aggregate TPFDD");
		if (showAggregateButton)
		  getQueryContentPane().add(aggregateButton, constraintsAggTPFDDButton);
		
		GridBagConstraints constraintsUnitAggButton = new GridBagConstraints();
		constraintsUnitAggButton.gridx = 3; constraintsUnitAggButton.gridy = 4;
		constraintsUnitAggButton.gridwidth = 1;
		constraintsUnitAggButton.weightx = 1.0;
		constraintsUnitAggButton.insets = new Insets(4, 4, 4, 4);
		getQueryContentPane().add(unitAggregateButton = makeButton ("Unit Aggregate","Unit Aggregate"), 
								  constraintsUnitAggButton);
		
		GridBagConstraints constraintsunitSelectorPanel = new GridBagConstraints();
		constraintsunitSelectorPanel.gridx = 0; constraintsunitSelectorPanel.gridy = 0;
		constraintsunitSelectorPanel.gridheight = 5;
		constraintsunitSelectorPanel.fill = GridBagConstraints.BOTH;
		constraintsunitSelectorPanel.weightx = 0.0;
		constraintsunitSelectorPanel.weighty = 1.0;
		constraintsunitSelectorPanel.insets = new Insets(4, 4, 4, 4);
		getQueryContentPane().add(getunitSelectorPanel(), constraintsunitSelectorPanel);

		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjQueryContentPane;
    }

    /**
     * Return the cargoSelectorPanel property value.
     * @return JPanel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JPanel getcargoSelectorPanel() {
	if (ivjcargoSelectorPanel == null) {
	    try {
		ivjcargoSelectorPanel = new JPanel();
		ivjcargoSelectorPanel.setName("cargoSelectorPanel");
		ivjcargoSelectorPanel.setBorder(BorderFactory.createTitledBorder("Cargo Asset Selection"));
		ivjcargoSelectorPanel.setLayout(new GridBagLayout());

		/*
		  GridBagConstraints constraintscargoTypeLabel = new GridBagConstraints();
		  constraintscargoTypeLabel.gridx = 0; constraintscargoTypeLabel.gridy = 1;
		  constraintscargoTypeLabel.anchor = GridBagConstraints.WEST;
		  constraintscargoTypeLabel.insets = new Insets(4, 4, 4, 4);
		  getcargoSelectorPanel().add(getcargoTypeLabel(), constraintscargoTypeLabel);
		*/

		GridBagConstraints constraintscargoByTypeRadio = new GridBagConstraints();
		constraintscargoByTypeRadio.gridx = 0; constraintscargoByTypeRadio.gridy = 0;
		constraintscargoByTypeRadio.anchor = GridBagConstraints.WEST;
		constraintscargoByTypeRadio.insets = new Insets(4, 4, 4, 4);
		getcargoSelectorPanel().add(getcargoByTypeRadio(), constraintscargoByTypeRadio);
		
		/*
		  GridBagConstraints constraintscargoNameLabel = new GridBagConstraints();
		  constraintscargoNameLabel.gridx = 0; constraintscargoNameLabel.gridy = 2;
		  constraintscargoNameLabel.fill = GridBagConstraints.VERTICAL;
		  constraintscargoNameLabel.anchor = GridBagConstraints.WEST;
		  constraintscargoNameLabel.insets = new Insets(4, 4, 4, 4);
		  getcargoSelectorPanel().add(getcargoNameLabel(), constraintscargoNameLabel);
		*/

		GridBagConstraints constraintscargoByNameRadio = new GridBagConstraints();
		constraintscargoByNameRadio.gridx = 0; constraintscargoByNameRadio.gridy = 1;
		constraintscargoByNameRadio.anchor = GridBagConstraints.WEST;
		constraintscargoByNameRadio.insets = new Insets(4, 4, 4, 4);
		getcargoSelectorPanel().add(getcargoByNameRadio(), constraintscargoByNameRadio);

		GridBagConstraints constraintscargoTypeScroller = new GridBagConstraints();
		constraintscargoTypeScroller.gridx = 1; constraintscargoTypeScroller.gridy = 0;
		constraintscargoTypeScroller.fill = GridBagConstraints.BOTH;
		constraintscargoTypeScroller.weightx = 1.0;
		constraintscargoTypeScroller.weighty = 1.0;
		constraintscargoTypeScroller.insets = new Insets(4, 4, 4, 4);
		getcargoSelectorPanel().add(getcargoTypeScroller(), constraintscargoTypeScroller);


		GridBagConstraints constraintscargoNameScroller = new GridBagConstraints();
		constraintscargoNameScroller.gridx = 1; constraintscargoNameScroller.gridy = 1;
		constraintscargoNameScroller.fill = GridBagConstraints.BOTH;
		constraintscargoNameScroller.weightx = 1.0;
		constraintscargoNameScroller.weighty = 1.0;
		constraintscargoNameScroller.insets = new Insets(4, 4, 4, 4);
		getcargoSelectorPanel().add(getcargoNameScroller(), constraintscargoNameScroller);

		// user code begin {1}
		cargoRadioGroup = new ButtonGroup();
		cargoRadioGroup.add(getcargoByTypeRadio());
		cargoRadioGroup.add(getcargoByNameRadio());
		getcargoByTypeRadio().setSelected(true);
		getcargoNameScroller().setEnabled(false);
		getcargoNameList().setEnabled(false);
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcargoSelectorPanel;
    }

    /**
     * Return the carrierSelectorPanel property value.
     * @return JPanel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JPanel getcarrierSelectorPanel() {
	if (ivjcarrierSelectorPanel == null) {
	    try {
		ivjcarrierSelectorPanel = new JPanel();
		ivjcarrierSelectorPanel.setName("carrierSelectorPanel");
		ivjcarrierSelectorPanel.setBorder(BorderFactory.createTitledBorder("Carrier Asset Selection"));
		ivjcarrierSelectorPanel.setLayout(new GridBagLayout());

		/*
		  GridBagConstraints constraintscarrierTypeLabel = new GridBagConstraints();
		  constraintscarrierTypeLabel.gridx = 0; constraintscarrierTypeLabel.gridy = 1;
		  constraintscarrierTypeLabel.anchor = GridBagConstraints.WEST;
		  constraintscarrierTypeLabel.insets = new Insets(4, 4, 4, 4);
		  getcarrierSelectorPanel().add(getcarrierTypeLabel(), constraintscarrierTypeLabel);
		*/
		
		GridBagConstraints constraintscarrierByTypeRadio = new GridBagConstraints();
		constraintscarrierByTypeRadio.gridx = 0; constraintscarrierByTypeRadio.gridy = 0;
		constraintscarrierByTypeRadio.anchor = GridBagConstraints.WEST;
		constraintscarrierByTypeRadio.insets = new Insets(4, 4, 4, 4);
		getcarrierSelectorPanel().add(getcarrierByTypeRadio(), constraintscarrierByTypeRadio);

		/*
		  GridBagConstraints constraintscarrierNameLabel = new GridBagConstraints();
		  constraintscarrierNameLabel.gridx = 0; constraintscarrierNameLabel.gridy = 2;
		  constraintscarrierNameLabel.fill = GridBagConstraints.VERTICAL;
		  constraintscarrierNameLabel.anchor = GridBagConstraints.WEST;
		  constraintscarrierNameLabel.insets = new Insets(4, 4, 4, 4);
		  getcarrierSelectorPanel().add(getcarrierNameLabel(), constraintscarrierNameLabel);
		*/
		
		GridBagConstraints constraintscarrierByNameRadio = new GridBagConstraints();
		constraintscarrierByNameRadio.gridx = 0; constraintscarrierByNameRadio.gridy = 1;
		constraintscarrierByNameRadio.anchor = GridBagConstraints.WEST;
		constraintscarrierByNameRadio.insets = new Insets(4, 4, 4, 4);
		getcarrierSelectorPanel().add(getcarrierByNameRadio(), constraintscarrierByNameRadio);


		GridBagConstraints constraintscarrierTypeScroller = new GridBagConstraints();
		constraintscarrierTypeScroller.gridx = 1; constraintscarrierTypeScroller.gridy = 0;
		constraintscarrierTypeScroller.fill = GridBagConstraints.BOTH;
		constraintscarrierTypeScroller.weightx = 1.0;
		constraintscarrierTypeScroller.weighty = 1.0;
		constraintscarrierTypeScroller.insets = new Insets(4, 4, 4, 4);
		getcarrierSelectorPanel().add(getcarrierTypeScroller(), constraintscarrierTypeScroller);


		GridBagConstraints constraintscarrierNameScroller = new GridBagConstraints();
		constraintscarrierNameScroller.gridx = 1; constraintscarrierNameScroller.gridy = 1;
		constraintscarrierNameScroller.fill = GridBagConstraints.BOTH;
		constraintscarrierNameScroller.weightx = 1.0;
		constraintscarrierNameScroller.weighty = 0.6;
		constraintscarrierNameScroller.insets = new Insets(4, 4, 4, 4);
		getcarrierSelectorPanel().add(getcarrierNameScroller(), constraintscarrierNameScroller);

		// user code begin {1}
		carrierRadioGroup = new ButtonGroup();
		carrierRadioGroup.add(getcarrierByTypeRadio());
		carrierRadioGroup.add(getcarrierByNameRadio());
		getcarrierByTypeRadio().setSelected(true);
		getcarrierNameScroller().setEnabled(false);
		getcarrierNameList().setEnabled(false);
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjcarrierSelectorPanel;
    }

    /**
     * Return the unitPanel property value.
     * @return JLabel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    // JTreeTable popup menu needs to be able to preset this.
    public UnitPanel getunitPanel() {
	if (ivjunitPanel == null) {
	    try {
		ivjunitPanel = new UnitPanel(dbState, this, myFont);
		// user code begin {1}
		// user code end
	    } catch (RuntimeException ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjunitPanel;
    }
    /**
     * Return the unitSelectorPanel property value.
     * @return JPanel
     */
    /* WARNING: THIS METHOD WILL BE REGENERATED. */
    private JPanel getunitSelectorPanel() {
	if (ivjunitSelectorPanel == null) {
	    try {
		ivjunitSelectorPanel = new JPanel();
		ivjunitSelectorPanel.setName("unitSelectorPanel");
		ivjunitSelectorPanel.setBorder(BorderFactory.createTitledBorder("Unit Selection"));
		ivjunitSelectorPanel.setLayout(new BorderLayout());
		ivjunitSelectorPanel.setMinimumSize(new Dimension(unitPanelWidth, 200));
		ivjunitSelectorPanel.add(getunitPanel(), BorderLayout.CENTER);
		JPanel panel = new JPanel ();
		panel.setLayout (new BorderLayout ());
		
		ivjunitSelectorPanel.add(panel, BorderLayout.SOUTH);
		panel.add(getincludeHierarchyButton(), BorderLayout.NORTH);
		panel.add(getManifestInfoButton = 
				  makeButton ("Get Manifest Info", "Get Manifest Info"), BorderLayout.SOUTH);
		
		// user code begin {1}
		// user code end
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return ivjunitSelectorPanel;
    }

    /**
     * Called whenever the part throws an exception.
     * @param exception Exception
     */
    private void handleException(Exception e)
    {
	OutputHandler.out(ExceptionTools.toString("CQ:hE", e));
    }

    /*
     * Event Handling routines
     */
    public void actionPerformed(ActionEvent event)
    {
	String command = event.getActionCommand();
	Object source = event.getSource();

	Debug.out("CQ:aP command " + command);
	try {
	  if (command.equals ("Display TPFDD")) {
		shell.showTPFDDView (getFilterClausesFromGUI (), true, false);
		return;
	  }
	  if (command.equals ("Aggregate TPFDD")) {
		FilterClauses clauses = getFilterClausesFromGUI ();
		clauses.setRollup (true);
		shell.showTPFDDView (clauses, true, false);

		return;
	  }
	  if (command.equals ("Unit Aggregate")) {
		FilterClauses clauses = getFilterClausesFromGUI ();
		clauses.setRollup (true);
		clauses.setByUnit (true);
		shell.showTPFDDView (clauses, true, false);

		return;
	  }
	  if (command.equals ("Get Manifest Info")) {
		updateLists ();
		return;
	  }
	  if (command.equals ("include hierarchy")) {
		getunitPanel().selectAllChildrenOfSelected();
		return;
	  }
	  
	    if ( source instanceof JRadioButton ) {
		if ( source == getcargoByNameRadio() ) {
		getcargoTypeScroller().setEnabled(false);
		getcargoTypeList().setEnabled(false);
		getcargoNameScroller().setEnabled(true);
		getcargoNameList().setEnabled(true);
	    }
	    else if ( source == getcargoByTypeRadio() ) {
		getcargoNameScroller().setEnabled(false);
		getcargoNameList().setEnabled(false);
		getcargoTypeScroller().setEnabled(true);
		getcargoTypeList().setEnabled(true);
	    }
	    else if ( source == getcarrierByNameRadio() ) {
		getcarrierTypeScroller().setEnabled(false);
		getcarrierTypeList().setEnabled(false);
		getcarrierNameScroller().setEnabled(true);
		getcarrierNameList().setEnabled(true);
	    }
	    else if ( source == getcarrierByTypeRadio() ) {
		getcarrierNameScroller().setEnabled(false);
		getcarrierNameList().setEnabled(false);
		getcarrierTypeScroller().setEnabled(true);
		getcarrierTypeList().setEnabled(true);
	    }
	    else
		OutputHandler.out("CQ:aP Error: Unknown JRadioButton source: " + source);
	    }
	    else if ( source instanceof JButton ) {
		  //		if ( source == getsendQueryButton() ) {
		  //		    lastQuery = getQueryFromUI();
		  //		    for ( Iterator iter = listeners.iterator(); iter.hasNext(); )
		  //			((ActionListener)(iter.next())).actionPerformed(event);
		  //		}
		  //		else if ( source == getincludeHierarchyButton() ) {
		  //		    getunitPanel().selectAllChildrenOfSelected();
		  //	}
		  //		else 
		  //		    OutputHandler.out("CQ:aP Error: Unknown JButton source: " + source);
	    }
	    else if ( source instanceof JCheckBox ) {
		if ( source == getincludeCargoCheckBox() ) {
		    if ( getincludeCargoCheckBox().isSelected() ) {
			getcargoByTypeRadio().setEnabled(true);
			getcargoByNameRadio().setEnabled(true);
			if ( getcargoByTypeRadio().isSelected() ) {
			    getcargoTypeList().setEnabled(true);
			    getcargoTypeScroller().setEnabled(true);
			    getcargoNameList().setEnabled(false);
			    getcargoNameScroller().setEnabled(false);
			}
			else {
			    getcargoTypeList().setEnabled(false);
			    getcargoTypeScroller().setEnabled(false);
			    getcargoNameList().setEnabled(true);
			    getcargoNameScroller().setEnabled(true);
			}
		    }
		    else {
			getcargoByTypeRadio().setEnabled(false);
			getcargoByNameRadio().setEnabled(false);
			getcargoTypeList().setEnabled(false);
			getcargoTypeScroller().setEnabled(false);
			getcargoNameList().setEnabled(false);
			getcargoNameScroller().setEnabled(false);
		    }
		}
		else if ( source == getincludeCarrierCheckBox() ) {
		    if ( getincludeCarrierCheckBox().isSelected() ) {
			getcarrierByTypeRadio().setEnabled(true);
			getcarrierByNameRadio().setEnabled(true);
			if ( getcarrierByTypeRadio().isSelected() ) {
			    getcarrierTypeList().setEnabled(true);
			    getcarrierTypeScroller().setEnabled(true);
			    getcarrierNameList().setEnabled(false);
			    getcarrierNameScroller().setEnabled(false);
			}
			else {
			    getcarrierTypeList().setEnabled(false);
			    getcarrierTypeScroller().setEnabled(false);
			    getcarrierTypeList().setEnabled(true);
			    getcarrierTypeScroller().setEnabled(true);
			}
		    }
		    else {
			getcarrierTypeList().setEnabled(false);
			getcarrierTypeScroller().setEnabled(false);
			getcarrierNameList().setEnabled(false);
			getcarrierNameScroller().setEnabled(false);
			getcarrierByTypeRadio().setEnabled(false);
			getcarrierByNameRadio().setEnabled(false);
		    }
		}
		else
		    OutputHandler.out("CQ:aP Error: Unknown JCheckBox source: " + source);
	    }
	    else
		OutputHandler.out("CQ:aP Error: unknown bean source: " + source);
	}
	catch ( Exception e ) {
	    OutputHandler.out(ExceptionTools.toString("CQ:aP", e));
	}
	catch ( Error e ) {
	    OutputHandler.out(ExceptionTools.toString("CQ:aP", e));
	}
    }
    
    public void valueChanged(TreeSelectionEvent e)
    {
	  cargoTypeListModel.clear();
	  carrierTypeListModel.clear();
	  cargoNameListModel.clear();
	  carrierNameListModel.clear ();

	  if (!getunitPanel().anyUnitsSelected ()) {
		getsendQueryButton().setEnabled (false);
		getManifestInfoButton.setEnabled (false);
		getincludeHierarchyButton().setEnabled(false);
		aggregateButton.setEnabled(false);
		unitAggregateButton.setEnabled(false);
	  }
	  else {
		getsendQueryButton().setEnabled (true);
		getManifestInfoButton.setEnabled (true);
		getincludeHierarchyButton().setEnabled(true);
		aggregateButton.setEnabled(true);
		unitAggregateButton.setEnabled(true);
	  }

	  //	  updateLists();
    }


    private void updateLists() {
	  List unitNodes = getunitPanel().getSelectedUnitNodes();
	  FilterClauses filterClauses = new FilterClauses();
	
	  filterClauses.setUnitDBUIDs (getNodeDBUIDs (unitNodes.toArray()));

	  setFilterLists (filterClauses,  carrierTypeListModel, carrierNameListModel,
					  cargoTypeListModel, cargoNameListModel);
    }

  private FilterClauses getFilterClausesFromGUI()
  {
	FilterClauses clauses = new FilterClauses ();
	
	getunitPanel().getSelectedUnitNames(clauses);

	boolean cargoIncluded = getincludeCargoCheckBox().isSelected();
	boolean carrierIncluded = getincludeCarrierCheckBox().isSelected();
	boolean sortByName = getSortByNameCheckBox().isSelected();
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterDialog - sort By name " + sortByName);
	  
	clauses.setSortByName (sortByName);
	
	if (cargoIncluded) {
	  if ( getcargoByNameRadio().isSelected() ) {
		clauses.setCargoInstances (getNodeDBUIDs (getcargoNameList().getSelectedValues()));
	  } else {
		clauses.setCargoTypes     (getNodeDBUIDs (getcargoTypeList().getSelectedValues()));
      }
	}

	if (carrierIncluded) {
	  if ( getcarrierByNameRadio().isSelected() ) {
		clauses.setCarrierInstances (getNodeDBUIDs (getcarrierNameList().getSelectedValues()));
	  } else {
		clauses.setCarrierTypes (getNodeDBUIDs (getcarrierTypeList().getSelectedValues()));
      }
	}

	// Debug.out("CQ:gQFU Set up query: " + query);
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterDialog - clauses " + clauses);
  
	return clauses;
  }

  public List getNodeDBUIDs (Object [] nodes) {
	List dbuids = new ArrayList ();
	
	UIDGenerator generator = UIDGenerator.getGenerator();
	
	for (int i = 0; i < nodes.length; i++) {
	  Node node = (Node) nodes[i];
	  dbuids.add (generator.getDBUID (node.getUID()));
	}

	return dbuids;
  }

  /*
    public void setUnitNode(String nodeDBUID) {
	  getunitPanel ().setUnitNode (nodeDBUID);
    }
  */

  public void setFilterLists(FilterClauses filterClauses, 
							 DefaultListModel carrierType, DefaultListModel carrierInstance,
							 DefaultListModel cargoType, DefaultListModel cargoInstance) {
	//  Doesn't seem to do anything!
	/*
	if (worker != null) {
	  worker.interrupt ();
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "interrupting thread.");
	}
	*/
	
	worker = new ListQueryWorker (filterClauses, carrierType, carrierInstance, cargoType, cargoInstance, this);
	worker.startWorker();
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterDialog.setFilterLists - made worker " + worker);
  }

  private static Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
  private static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

  class ListQueryWorker extends TPFDDSwingWorker {
	FilterClauses filterClauses;
	Set forest = new HashSet();
	DefaultListModel carrierType;
	DefaultListModel carrierInstance;
	DefaultListModel cargoType;
	DefaultListModel cargoInstance;
	JDialog dialog;
	
	public ListQueryWorker (FilterClauses filterClauses, DefaultListModel carrierType, 
							DefaultListModel carrierInstance,
							DefaultListModel cargoType, DefaultListModel cargoInstance, JDialog dialog) {
	  this.filterClauses = filterClauses;
	  this.carrierType = carrierType;
	  this.carrierInstance = carrierInstance;
	  this.cargoType = cargoType;
	  this.cargoInstance = cargoInstance;
	  this.dialog = dialog;
	  
	  carrierType.clear ();
	  carrierInstance.clear ();
	  cargoType.clear ();
	  cargoInstance.clear ();
	}
	
	public void construct(){
	  dialog.setCursor (waitCursor);
	  doListQuery(filterClauses, forest);
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "setting cursor to " + waitCursor + " at " + new Date ());
	}

	public void finished() {
	  /* Set the worker to null,
		 but only if we are the active worker. */
	  if (worker == this) {
		worker = null;
	  }
	  handleResults ();
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "setting cursor to " + defaultCursor + " at " + new Date ());
	  
	  dialog.setCursor (defaultCursor);
	}

	protected void populateListModel (Tree tree, DefaultListModel listModel) {
	  for (int i = 0; i < tree.getRoot().getChildCount(); i++) {
		Node child = tree.getChild (tree.getRoot(), i);
		listModel.addElement (child);
	  }
	}
	
	protected void handleResults () {
	  // add children to node
	  for (Iterator iter = forest.iterator (); iter.hasNext();) {
		ListQueryResponse lqr = (ListQueryResponse) iter.next();
		populateListModel (lqr.getCarrierTypeTree(), carrierType);
		populateListModel (lqr.getCarrierInstanceTree(), carrierInstance);
		populateListModel (lqr.getCargoTypeTree(), cargoType);
		populateListModel (lqr.getCargoInstanceTree(), cargoInstance);
		if (debug)
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilteDialog lists updated.");
	  }
	}
  };

  protected void doListQuery (FilterClauses filterClauses, Set forest) {
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterDialog.doListQuery - list query is " + filterClauses);
	QueryHandler queryHandler = new QueryHandler ();
	
	Query listQuery = queryHandler.createListQuery (dbState, filterClauses);

	ListQueryResponse response = (ListQueryResponse)
	  listQuery.getResponse(dbState.getDBConfig().getConnection());

	forest.add (response); // hack
  }
}
