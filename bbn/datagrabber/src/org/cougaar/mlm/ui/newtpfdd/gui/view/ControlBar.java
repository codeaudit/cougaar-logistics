/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/view/Attic/ControlBar.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  Creation date: Mon Jan 24 12:13:54 2000
  @author Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JCheckBox;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;

public class ControlBar extends JPanel implements ActionListener
{
    private TaskGanttChart gc;
    private TaskModel taskModel;

    private JButton plusButton = null;
    private JButton minusButton = null;
    private JButton plusFiveButton = null;
    private JButton minusFiveButton = null;
    private JButton fitToViewButton = null;
    private JButton earliestButton = null;
    private JButton latestButton = null;
    private JCheckBox showPrefs = null;
    private JLabel countLabel = null;

    public JLabel getcountLabel()
    {
	try {
	    if ( countLabel == null ) {
		countLabel = new JLabel();
		countLabel.setName("countLabel");
		countLabel.setText("0");
		countLabel.setBackground(Color.white);
		countLabel.setHorizontalAlignment(JLabel.RIGHT);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return countLabel;
    }

    private JCheckBox getshowPrefs()
    {
	try {
	    if ( showPrefs == null ) {
		showPrefs = new JCheckBox();
		showPrefs.setName("showPrefsCheckBox");
		showPrefs.setText("Show Planning Time Bounds");
		showPrefs.setSelected(false);
		showPrefs.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return showPrefs;
    }

    private JButton getplusButton()
    {
	try {
	    if ( plusButton == null ) {
		plusButton = new JButton();
		plusButton.setText("+1 row");
		plusButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return plusButton;
    }
    
    private JButton getminusButton()
    {
	try {
	    if ( minusButton == null ) {
		minusButton = new JButton();
		minusButton.setText("-1");
		minusButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return minusButton;
    }

    private JButton getplusFiveButton()
    {
	try {
	    if ( plusFiveButton == null ) {
		plusFiveButton = new JButton();
		plusFiveButton.setText("+5");
		plusFiveButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return plusFiveButton;
    }
    
    private JButton getminusFiveButton()
    {
	try {
	    if ( minusFiveButton == null ) {
		minusFiveButton = new JButton();
		minusFiveButton.setText("-5");
		minusFiveButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return minusFiveButton;
    }

    private JButton getfitToViewButton()
    {
	try {
	    if ( fitToViewButton == null ) {
		fitToViewButton = new JButton();
		fitToViewButton.setText("Fit To View");
		fitToViewButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return fitToViewButton;
    }

    private JButton getearliestButton()
    {
	try {
	    if ( earliestButton == null ) {
		earliestButton = new JButton();
		earliestButton.setText("Earliest C-Date");
		earliestButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
		handleException(e);
	}
	return earliestButton;
    }
    
    private JButton getlatestButton()
    {
	try {
	    if ( latestButton == null ) {
		latestButton = new JButton();
		latestButton.setText("Latest C-Date");
		latestButton.addActionListener(this);
	    }
	}
	catch ( Exception e ) {
	    handleException(e);
	}
	return latestButton;
    }

    private JButton makeButton(String label, String command) {
	  JButton button = new JButton();
	  button.setText(label);
	  button.setActionCommand (command);
	  button.addActionListener(this);
	  return button;
    }

  public ControlBar(TaskGanttChart gc, TaskModel taskModel)
    {
	this.gc = gc;
	this.taskModel = taskModel;
	
	setName("ganttChartControlBar");
	setLayout(new BorderLayout ());
	JPanel upper = new JPanel ();
	JPanel lower = new JPanel ();
	
	upper.setLayout(new FlowLayout());
	// upper.add(getcountLabel());
	upper.add(getplusButton());
	upper.add(getplusFiveButton());
	upper.add(getminusButton());
	upper.add(getminusFiveButton());
	upper.add(getfitToViewButton());
	upper.add(makeButton("Zoom In",  "Zoom In"));
	upper.add(makeButton("Zoom Out", "Zoom Out"));
	upper.add(getearliestButton());
	upper.add(getlatestButton());
	add (upper, BorderLayout.NORTH);

	lower.setLayout(new FlowLayout());
	lower.add (getcountLabel());
	add (lower, BorderLayout.SOUTH);
    }
    
    private void handleException(Exception e)
    {
	OutputHandler.out(ExceptionTools.toString("CB:hE", e));
    }

    public void actionPerformed(ActionEvent event)
    {
	String command = event.getActionCommand();
	Object source = event.getSource();
	
	Debug.out("CB:aP command " + command);
	if ( !(source instanceof JButton || source instanceof JCheckBox) ) {
	    OutputHandler.out("CB:aP Error: unknown bean source: " + source);
	    return;
	}
	  if (command.equals ("Zoom In")) {
		gc.zoomIn ();
		return;
	  }
	  if (command.equals ("Zoom Out")) {
		gc.zoomOut ();
		return;
	  }
	try {
	    if ( source == getplusButton() )
		gc.setVisibleAmount(gc.getVisibleAmount() + 1);
	    else if ( source == getminusButton() )
		gc.setVisibleAmount(gc.getVisibleAmount() - 1);
	    else if ( source == getplusFiveButton() )
		gc.setVisibleAmount(gc.getVisibleAmount() + 5);
	    else if ( source == getminusFiveButton() )
		gc.setVisibleAmount(gc.getVisibleAmount() - 5);
	    else if ( source == getfitToViewButton() )
		gc.fitToView();
	    else if ( source == getearliestButton() )
		gc.setVirtualXLocation(taskModel.getMinTaskStart() - TaskGanttChart.DAYLEN);
	    else if ( source == getlatestButton() )
		gc.setVirtualXLocation(taskModel.getMaxTaskEnd() - gc.getVirtualXSize() + TaskGanttChart.DAYLEN);
	    else if ( source == getshowPrefs() )
		gc.getLozengePanel().setLayerActive(0, getshowPrefs().isSelected());
	    else
		OutputHandler.out("CB:aP Error: unknown JButton source: " + source);
	}
	catch ( Exception e ) {
	    OutputHandler.out(ExceptionTools.toString("CQ:aP", e));
	}
	catch ( Error e ) {
	    OutputHandler.out(ExceptionTools.toString("CQ:aP", e));
	}
    }
}
