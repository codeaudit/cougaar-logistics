/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/view/Attic/GanttChartView.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Jason Leatherman, Daniel Bromberg
*/

/**
   Simple wrapper to set up a Task-based GanttChart view.
*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import java.util.Date;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import javax.swing.*;
import javax.swing.filechooser.*;

import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.SwingQueue;

import org.cougaar.mlm.ui.newtpfdd.gui.component.GanttChart;
import org.cougaar.mlm.ui.newtpfdd.gui.component.LongXRuler;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell;

import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoInstance;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LegNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DimensionNode;
import org.cougaar.mlm.ui.grabber.validator.CargoDimensionTest;

public class GanttChartView extends JPanel implements ActionListener, WorkListener {
    private static final int DAYLEN = 1000*3600*24;
    private TaskGanttChart gc;
    private ControlBar cb;
    private long last;
  protected TaskModel taskModel;
  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.GanttChartView.debug", 
									   "false"));

  public GanttChartView(TaskModel taskModel, String startDate)
    {
	  this.taskModel = taskModel;
	  
	gc = new TaskGanttChart(taskModel);
      
	gc.setVirtualXLocation(0L);
	gc.setVirtualXSize(20 * DAYLEN);
	gc.setTicInterval(DAYLEN);
	gc.setTicLabelInterval(DAYLEN);
	SimpleDateFormat dFormat = new SimpleDateFormat("MM/dd/yy");
	Date CDayZeroDate = new Date();
	try {
	    CDayZeroDate = dFormat.parse(startDate);
	}
	catch ( ParseException e ) {
	    OutputHandler.out("GCV:GCV Bad date string: " + startDate + ": " + e);
	}
	Date now = new Date();
	Debug.out("GCV:GCV startDate: " + startDate + " time: " + CDayZeroDate.getTime() + " now: " + now.getTime());
	gc.setCDayZeroTime(CDayZeroDate.getTime());
	gc.setVisibleAmount(15);
	gc.setLabelUnitsMode(LongXRuler.PLAINDAYS_UNITS);
	
	cb = new ControlBar(gc, taskModel);
	
	setBackground(Color.black);
	setLayout(new BorderLayout());
	
	add(gc, BorderLayout.CENTER);
	add(cb, BorderLayout.NORTH);
	
	setVisible(true);
	UpdateThread updateThread = new UpdateThread();
	updateThread.start();
    }

  public void showTPFDDLines (FilterClauses filterClauses) {
	if (debug)
	  System.out.println ("GanttChartView.showTPFDDLines - " + filterClauses);

	taskModel.showTPFDDLines (filterClauses, gc, this, this);
  }

  public void showTPFDDFilterLines (FilterClauses filterClauses) {
	if (debug)
	  System.out.println ("GanttChartView.showTPFDDFilterLines - " + filterClauses);
	
	taskModel.showFilterTPFDDLines (filterClauses, gc, this, this);
  }

   protected long duration = 0l;
   protected boolean workIsDone = false;
 
   public void workTook (long duration) {
     this.duration = duration;
     workIsDone = true;
   }

    public GanttChart getWidget()
    {
	return gc;
    }

  private class UpdateThread extends Thread {
    private Runnable updateCountRunnable = new Runnable() {
	public void run() {
	  cb.getcountLabel().setText(String.valueOf(gc.getNumRows()) + " items");
	}
      };
    private Runnable calculateTonsRunnable = new Runnable() {
	public void run() {
	  double [] results = getTotalWeightAndVolume (taskModel.getTree());
	  double tons   = results [0];
	  double volume = results [1];
	  DecimalFormat form = new DecimalFormat ("#.#");
	  String tonsString = form.format (tons);
	  String volumeString = form.format (volume);
	  String durString = form.format(((double)duration)/1000.0d);
	  cb.getcountLabel().setText(String.valueOf(gc.getNumRows()) + " items (" +
				     tonsString + " tons, " + 
				     volumeString + " m^3). Created in " + 
 				     durString + " seconds.");
	}
      };
    
    public void run()
    {
      try {
	runloop();
      }
      catch ( Exception e ) {
	OutputHandler.out(ExceptionTools.toString("GCV:uT:run", e));
      }
      catch ( Error e ) {
	OutputHandler.out(ExceptionTools.toString("GCV:uT:run", e));
      }	    
    }
    
    private void runloop() {
      int i = 0;
      while ( !workIsDone ) {
	try {
	  synchronized (this) {
	    this.wait(500);
	  }
	} catch ( Exception e ) {}

	SwingQueue.invokeLater(updateCountRunnable);
	SwingQueue.invokeLater(calculateTonsRunnable);
      }
    }
  }

  public double [] getTotalWeightAndVolume (Tree tree) {
    return getTotalWeightAndVolume (tree.getRoot(), tree);
  }

  public double [] getTotalWeightAndVolume (Node node, Tree tree) {
    double weight = 0;
    double volume = 0;
    
    if (node instanceof DimensionNode) {
      DimensionNode dimNode = (DimensionNode) node;
      // weight is in grams, we want short tons = 2000 pounds
      weight = dimNode.getQuantity()*(dimNode.getWeight ()/1000000.0)*CargoDimensionTest.METRIC_TO_SHORT_TON;
      volume = dimNode.getQuantity()*dimNode.getVolume ();
      if (weight < 0.000001)
 	System.err.println ("TotalWeightAndVolume - Weight was zero for Node " + node);
      if (volume < 0.000001)
 	System.err.println ("TotalWeightAndVolume - Volume was zero for Node " + node);
    }

    for (int i = 0; i < node.getChildCount (); i++) {
      Node childNode = tree.getNode (node.getChildUID (i));
      double [] results = getTotalWeightAndVolume (childNode, tree);
      weight += results[0];
      volume += results[1];
    }

    return new double [] { weight, volume };
  }

  public void paint(Graphics g)
  {
    int numRows = gc.getNumRows();
    if ( numRows % 10 == 0 )
      cb.getcountLabel().setText(String.valueOf(numRows) + " items");
    super.paint(g);
  }

  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();
    Object source = event.getSource();

    // for now, there is only one command
    boolean allLegs = command.equals ("Export CSV file");

    JFileChooser chooser = new JFileChooser ();
    chooser.setDialogTitle ("Save TPFDD data to a CSV file."); // doesn't seem to work
    chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
    chooser.addChoosableFileFilter(new CSVFilter());

    int returnVal = chooser.showSaveDialog(null);
    String fileWithExtension = "";
    if(returnVal == JFileChooser.APPROVE_OPTION) {
      File realFile = chooser.getSelectedFile();
      if (realFile == null) {
	System.out.println("No file selected.");
	return;
      }
	
      System.out.println("You chose to save to this file: " + realFile);
      String fileName = realFile.getName();
      if (!fileName.endsWith (".csv")) 
	fileName += ".csv";
      try {
	fileWithExtension = realFile.getParent()+File.separator+fileName;
	PrintWriter out
	  = new 
	    PrintWriter(new 
	      BufferedWriter(new 
		FileWriter(fileWithExtension)));
	writeTreeToFile (taskModel.getTree(), out, allLegs);
	out.close ();
	File test = new File (fileWithExtension);
	if (!test.exists ())
	  System.err.println ("--->>> Hey! file " + test + " doesn't exist!");
      } catch (IOException ioe) {
	JOptionPane.showMessageDialog(null, "Couldn't write to " + fileWithExtension, 
				      "File IO Error", JOptionPane.ERROR_MESSAGE); 
      }
     }
   }

  private class CSVFilter extends FileFilter {
    // Accept all directories and all csv files.
    public boolean accept(File f) {
      if (f.isDirectory()) {
	return true;
      }

      String extension = getExtension(f);
      if (extension != null) {
	if (extension.equals(CSV)) {
	  return true;
	} else {
	  return false;
	}
      }

      return false;
    }

    /*
     * Get the extension of a file.
     */  
    public String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    public final static String CSV = "csv";

    // The description of this filter
    public String getDescription() {
      return "Just CSV files";
    }
  }

  protected void writeTreeToFile (Tree tree, PrintWriter out, boolean allLegs) {
    SimpleDateFormat dFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
    Node root = tree.getRoot ();
    out.print ("Unit,");
    out.print ("Cargo Name,");
    out.print ("Container,");
    out.print ("Type,");
    out.print ("Nomenclature,");
    out.print ("Quantity,");
    out.print ("Tons,");
    out.print ("Volume (m^3),");
    out.print ("From Geoloc,");
    out.print ("From Name,");
    out.print ("To Geoloc,");
    out.print ("To Name,");
    out.print ("Start Time,");
    out.print ("End Time,");
    out.print ("Carrier Type,");
    out.println ("Name");
    for (int i = 0; i < root.getChildCount (); i++) {
      CargoInstance instance = (CargoInstance) tree.getChild (root, i);
      for (int j = 0; j < instance.getChildCount (); j++) {
	LegNode leg = (LegNode) tree.getChild (instance, j);
	if (!allLegs && leg.getMode () != LegNode.MODE_SEA)
	  continue; // skip non-sea legs 

	out.print (instance.getUnitName() +",");
	out.print ("\"" + instance.getDisplayName() +"\",");
	out.print ("\"" + instance.getContainer() +"\",");
	out.print ("\"" + instance.getALPType() +"\",");
	out.print ("\"" + instance.getNomen() +"\",");
	out.print (instance.getQuantity() +",");
	out.print (((instance.getWeight()/1000000.0)*CargoDimensionTest.METRIC_TO_SHORT_TON) +",");
	out.print (instance.getVolume() +",");
	out.print (leg.getFromCode () +",");
	out.print ("\"" + leg.getFrom () + "\",");
	out.print (leg.getToCode () +",");
	out.print ("\"" + leg.getTo () + "\",");
	out.print (dFormat.format(leg.getActualStart ()) +",");
	out.print (dFormat.format(leg.getActualEnd ()) +",");
	out.print (leg.getCarrierType () +",");
	out.println (leg.getCarrierName ());
      }
    }
    out.flush ();
  }
  
  protected JMenuBar getMenuBar () {
    JMenuBar menuBar = new JMenuBar();
    menuBar.setName("TPFDDMenuBar");
    JMenu menu = new JMenu();
    menu.setName("tpfddMenu");
    menu.setText("Export");
    menu.add(makeMenuItem("Export CSV file", this));
    menu.add(makeMenuItem("Export Sea Legs CSV file", this));
    menuBar.add(menu);

    return menuBar;
  }

  protected JMenuItem makeMenuItem (String label, ActionListener listener) {
    JMenuItem item = new JMenuItem();

    item.setName(label);
    item.setToolTipText(label);
    item.setText(label);
    item.setActionCommand(label);
    item.addActionListener(listener);
	
    return item;
  }
}
