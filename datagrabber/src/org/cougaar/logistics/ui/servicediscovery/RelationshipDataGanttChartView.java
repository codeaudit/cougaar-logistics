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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */


package org.cougaar.logistics.ui.servicediscovery;

import org.cougaar.mlm.ui.newtpfdd.gui.view.SimpleGanttChartView;
import org.cougaar.mlm.ui.newtpfdd.gui.view.GanttChartView;
import org.cougaar.mlm.ui.newtpfdd.gui.view.TaskModel;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseState;

import org.cougaar.util.log.Logger;

import java.util.Date;
import java.util.Iterator;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;


import java.text.SimpleDateFormat;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


/**
 * The file GanttChartView reads a CSV file and displays
 * the chart.
 * <p>
 * Blank lines and line starting with "#" are ignored.
 * <p>
 * A blank start date tells the reader to copy the end date
 * from the prior line.  This is handy if your input file
 * only records the end time of the event.
 * <p>
 * A blank end date tells the reader to copy the start date
 * from the current line.  This is handy if the event is
 * instantaneous.
 * <p>
 * Example file:
 * <pre>
 * # comment
 * A, 2002-10-08 01:03:00, 2002-10-08 02:50:00, grey, foo
 * B, 2002-10-08 01:00:00, 2002-10-08 01:30:00, red, bar
 * B, , 2002-10-08 03:05:00, blue, baz
 * </pre>
 */
public class RelationshipDataGanttChartView extends SimpleGanttChartView implements ActionListener {

    public final static String MENU_SaveXML = "Save..";
    public final static String MENU_OpenXML = "Open..";
    public final static String MENU_Exit = "Exit";

    protected String xmlString=null;

    protected JFrame ganttChartFrame=null;
    protected GanttChartView ganttChartView=null;
    protected JFileChooser fileChooser=null;

    Logger logger;
    RelationshipXMLParser parser;
    

    public void launch() {
	//super.launch(getStartDate(), getTitle());
        this.launch(getStartDate(), getTitle());
    }

    protected RelationshipScheduleData data;


    public RelationshipDataGanttChartView(String xml, Logger logger) {
	parser = new RelationshipXMLParser();
	this.xmlString = xml;
	this.logger = logger;
	this.data = parser.parseString(xml);
    }

    public RelationshipDataGanttChartView(RelationshipScheduleData rsData, String xml, Logger logger) {
	parser = new RelationshipXMLParser();
	this.xmlString = xml;
	this.logger = logger;
	this.data = rsData;
    }

    protected Iterator getInstances() {
        return data.getInstances();
    }

    protected Iterator getLegs() {
        return data.getRelationships();
    }

    protected String getTitle() {
        return "Relationships of " + data.getSourceAgent();
    }

    protected Date getStartDate() {
        return new Date(data.getStartCDay());
    }

    /**
     * Create the task model.
     */
    protected TaskModel createTaskModel() {
        return new SimpleTaskModel((DatabaseState) new RelationshipDatabaseState());
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Title:  " + getTitle() + "\n");
        sb.append(data.toString());
        return sb.toString();
    }
    
    public void launch(Date baseDate, String title) {
	// disable begin/end lozenges
	System.setProperty("originLozengeSize", "0");
	System.setProperty("destinationLozengeSize", "0");
	// launch UI
	TaskModel model = createTaskModel();
	String now = (new SimpleDateFormat("MM/dd/yyyy")).format(
		      baseDate != null ? baseDate : (new Date()));
	ganttChartView = 
	    new GanttChartView(model, 
			       now);
	JPanel ganttChartPanel = new JPanel();
	ganttChartPanel.setLayout(new BorderLayout());
	ganttChartPanel.add(ganttChartView, BorderLayout.CENTER);
	ganttChartFrame = new JFrame();
	ganttChartFrame.getContentPane().add(ganttChartPanel, BorderLayout.CENTER);
	ganttChartFrame.setTitle(title);
	ganttChartFrame.setSize(640, 480);
	ganttChartFrame.setVisible(true);
	ganttChartFrame.setJMenuBar(makeMenus());
	ganttChartView.showTPFDDLines(null);
	initializeChooser();
    }


   protected void initializeChooser() {
	File pathDirs = new File(RelationshipUILauncherFrame.getDefaultOpenPath());
	fileChooser = new JFileChooser(pathDirs);
   }

  protected JMenuBar makeMenus() {
    JMenuBar retval = new JMenuBar();

    JMenu file = new JMenu("File");
    JMenuItem quit = new JMenuItem(MENU_Exit);
    JMenuItem save = new JMenuItem(MENU_SaveXML);
    JMenuItem open = new JMenuItem(MENU_OpenXML);
    quit.addActionListener(this);
    save.addActionListener(this);
    open.addActionListener(this);
    file.add(quit);
    //Right now if you open into a view when you scroll to the end you get an exception
    //file.add(open);
    file.add(save);
    retval.add(file);

    return retval;
  }




    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(MENU_Exit)) {
	    ganttChartFrame.dispose();  
        } else if (e.getActionCommand().equals(MENU_SaveXML)) {
	    saveXML();
        } else if (e.getActionCommand().equals(MENU_OpenXML)) {
	    openXML();
        } else {
            System.out.println("RelationshipDataGanntChartView: Unknown Action");
        }
    }


  protected void saveXML() {
    String defaultSavePath = RelationshipUILauncherFrame.getDefaultSavePath();
    File pathDirs = new File(defaultSavePath);
    try {
      if (!pathDirs.exists()) {
        pathDirs.mkdirs();
      }
    } catch (Exception ex) {
      logger.error("Error creating default directory " + defaultSavePath, ex);
    }
    if (xmlString != null) {
      String fileID = defaultSavePath + "Relationship-" + data.getSourceAgent() + "-" + RelationshipUILauncherFrame.formatTimeStamp(new Date(), false) + ".csv";
      System.out.println("Save to file: " + fileID);
      fileChooser.setSelectedFile(new File(fileID));
    }
    int retval = fileChooser.showSaveDialog(ganttChartFrame);
    if (retval == fileChooser.APPROVE_OPTION) {
      File saveFile = fileChooser.getSelectedFile();
      try {
        FileWriter fw = new FileWriter(saveFile);
        fw.write(xmlString);
        fw.flush();
        fw.close();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }
    
  protected void openXML() {
      
    fileChooser.setMultiSelectionEnabled(false);
    int retval = fileChooser.showOpenDialog(ganttChartFrame);
    if (retval == fileChooser.APPROVE_OPTION) {
	File openFile = fileChooser.getSelectedFile();
	xmlString = "";
	try {
	    BufferedReader br = new BufferedReader(new FileReader(openFile));
	      
	    String nextLine = br.readLine();
	    while (nextLine != null) {
		xmlString = xmlString + nextLine + "\n";
		nextLine = br.readLine();
	    }
	    br.close();
	} catch (IOException ioe) {
	    throw new RuntimeException(ioe);
	}
	
	data = parser.parseString(xmlString);
	ganttChartView.showTPFDDLines(null);
	ganttChartFrame.setTitle(getTitle());
	ganttChartView.invalidate();
	ganttChartFrame.invalidate();
	ganttChartFrame.repaint();
    }

  }
}
