/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.statistics;

import org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.validator.Validator;
import org.cougaar.mlm.ui.grabber.validator.Test;
import org.cougaar.mlm.ui.grabber.validator.HTMLizer;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.EditorKit;
import javax.swing.text.Document;
import javax.swing.JLabel;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import java.io.*;

import com.ibm.eou.swingchart.UniversalChart; // from chart.jar - thanks AlphaWorks!

import org.cougaar.mlm.ui.grabber.validator.Graphable;

/**
 * Displays statistics information based on grabber Validation tests
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 5/3/01
 **/
public class GraphPane extends StatisticsPane {

  boolean debug = true;
  
  private final Font    iInformalFontBig = new Font("SansSerif", Font.ITALIC, 20);
  private final Font    iFormalFontBig   = new Font("Serif", Font.BOLD, 20);
  private final Font    iNegativeFontBig   = new Font("Courier", Font.PLAIN, 20);

  private final Font    iInformalFont    = new Font("SansSerif", Font.ITALIC, 12);
  private final Font    iFormalFont      = new Font("Serif", Font.BOLD, 12);
  private final Font    iNegativeFont      = new Font("Courier", Font.PLAIN, 12);

  private final Font [] iFonts    =  {iInformalFont,    iFormalFont,    iNegativeFont};
  private final Font [] iBigFonts =  {iInformalFontBig, iFormalFontBig, iNegativeFontBig};

  // Colors

  private final Color [] iBackgroundColors =  {new Color(255, 255, 200), new Color(204, 204, 204), Color.black};
  private final Color [] iForegroundColors =  {Color.black, new Color(0,0,128), Color.white};

  //Constants:
  ////////////

  public String getName () { return "Run Charts"; }

  //Variables:
  ////////////

  protected UniversalChart ucChart;
  protected JLabel statusLabel;

  protected Color[] possibleColors = new Color [] { 
	Color.blue,    Color.cyan,     Color.green,
	Color.magenta, Color.red,      Color.yellow, 
	Color.black,   Color.orange,   Color.gray, 
	Color.pink,    Color.darkGray, Color.lightGray 
  };
  
  //Constructors:
  ///////////////

  public GraphPane(NewTPFDDShell shell){
	super (shell);
  }

  //Members:
  //////////

  protected void setupGUI(){
	super.setupGUI();
	statusLabel = new JLabel ();
    add(statusLabel,BorderLayout.SOUTH);
  }

  protected boolean addTest (Test test) {	
	return test instanceof Graphable;  
  }
  
  protected JComponent createContent () {
	//	ucChart = new UniversalChart();
	ucChart = new MyChart();
	((MyChart)ucChart).setDim(new Dimension(400,400));
    ucChart.setShowGridLines( false );
	// alphaworks Chart variables
	Color[] cuc = new Color [] { 
	  Color.blue
	};
	ucChart.setColors( cuc );
	
	int style = 0;
	
	//    ucChart.setFont(new Font("SansSerif", Font.ITALIC, 20));//iFonts[style]);
	//	ucChart.setForeground(Color.black);//iForegroundColors[style]);
	//    ucChart.setBackground(new Color (255,255,200));//iBackgroundColors[style]);
	return ucChart;
  }

  public void displayResult(Statement s, int idx){
    final int fin_idx=idx;
    final int fin_runID=getRunID();
    final Statement fin_s=s;
	final Test fin_whichTest = getValidator().getTest(idx);    
	
    final SwingWorker worker = new SwingWorker(){
		int idx=fin_idx;
		int runID=fin_runID;
		Statement s=fin_s;
		Test whichTest = fin_whichTest;

		String [] legends;
		String [] stringLabels;
		double [][] valueMatrix;

        public Object construct() {
		  try{
			List legendList = new ArrayList ();
			int numThirdDim = 1;
			Graphable graphableTest = (Graphable) whichTest;
			boolean hasThirdDim = graphableTest.hasThirdDimension();
			boolean showMultiple = graphableTest.showMultipleGraphs();
			
			if (!hasThirdDim) {
			  legends = new String [1];
			  String[] headers=whichTest.getHeaders();
			  legends[0]=headers[graphableTest.getYAxisColumn ()-1];
			  legendList.add (legends[0]);
			}

			List labels = new ArrayList ();
			ResultSet rs=s.executeQuery("SELECT * FROM "+whichTest.getTableName(runID));

			int zCol = graphableTest.getZAxisColumn ();
			
			int rows=0;
			Set uniqueLegendNames = new HashSet();
			while (rs.next()) {
			  if (hasThirdDim)
				uniqueLegendNames.add(rs.getString(zCol));
			  rows++;
			}
			if (debug)
			  System.out.println ("displayResult.worker.construct - JDBC 1.0 - rows: "+rows);

			if (rows == 0) // nothing to show
			  return new MyChart();

			if (hasThirdDim) {
			  legendList.addAll (uniqueLegendNames);
			  Collections.sort (legendList);
			  legends = (String []) legendList.toArray (new String[] {});
			  numThirdDim = legends.length;
			}
			  
			// need to get a new copy of the result set
			rs=s.executeQuery("SELECT * FROM "+whichTest.getTableName(runID));

			valueMatrix = new double[numThirdDim][rows];

			int numColumns = populateValues (rs, graphableTest, legendList, labels, valueMatrix);
			if (numColumns == 0) 
			  numColumns = 1;
			
			rs.close();

			// chop off parts of matrix that are not filled in
			double [][] valueMatrix2  = new double[numThirdDim][numColumns];
			for (int i = 0; i < numThirdDim; i++)
			  for (int j = 0; j < numColumns; j++)
				valueMatrix2[i][j] = valueMatrix[i][j];
			valueMatrix = valueMatrix2;
			
			if (debug)
			  System.out.println ("displayResult.worker.construct - total rows: "+rows);

			stringLabels = (String []) labels.toArray(new String []{});

			if (debug) {
			  System.out.println ("displayResult.worker.construct - legends :");
			  for (int i = 0; i < legends.length; i++)
				System.out.println (i + " " + legends[i]);
			  System.out.println ("displayResult.worker.construct - labels :");
			  for (int i = 0; i < stringLabels.length; i++)
				System.out.println (i + " " + stringLabels[i]);
			  System.out.println ("displayResult.worker.construct - values :");
			  for (int i = 0; i < numThirdDim; i++) {
				System.out.print ("[");
				
				for (int j = 0; j < valueMatrix[i].length; j++)
				  System.out.print ("" + valueMatrix[i][j] + ((j == valueMatrix[i].length-1) ? "" : ","));
				System.out.println ("]");
			  }
			}
		  } catch(SQLException e){
			logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
							  "Could not produce HTML from validation table",e);
		  }
		  return ucChart;
		}

		protected int populateValues (ResultSet rs, Graphable graphableTest,
									   List legendList, 
									   List labels, double [][] valueMatrix) {
		  boolean hasThirdDim = graphableTest.hasThirdDimension();
		  int xCol = graphableTest.getXAxisColumn ();
		  int yCol = graphableTest.getYAxisColumn ();
		  int zCol = graphableTest.getZAxisColumn ();
		  int types[]=((Test)graphableTest).getTypes();

		  String lastX = "";
		  int currentColumn=-1; // values for each z axis of data
		  try {
			while(rs.next()){
			  // X axis labels
			  String label = "?";
			  if (types[xCol-1] == Test.TYPE_STRING)
				label = rs.getString(xCol);
			  else if (types[xCol-1] == Test.TYPE_DATETIME)
				label = rs.getTimestamp(xCol).toString();

			  if (!label.equals(lastX)) {
				labels.add (label);
				currentColumn++;
			  } 

			  lastX = label;
			  
			  // Z axis index
			  int zAxisIndex = (hasThirdDim) ?
				legendList.indexOf(rs.getString(zCol)) : 0;
			  
			  if (zAxisIndex == -1)
				System.out.println ("GraphPane.construct - huh? " + 
									"zAxisIndex is undefined. legend List " + legendList + 
									" does not contain z col " + rs.getString(zCol));
			  // Y axis values
			  // if (debug)
			  //System.out.println ("[" + zAxisIndex + "]["+currentColumn+"] = " + rs.getInt(yCol));
			  
			  valueMatrix[zAxisIndex][currentColumn] = getDoubleValue(rs, yCol);
			}
		  } catch(SQLException e){
			logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
							  "Could not produce HTML from validation table",e);
		  }
		  return currentColumn;
		}
		
		public double getDoubleValue (ResultSet rs, int yCol) {
		  double doubleValue;
			  
		  try {
			//				intValue = rs.getInt(yCol);
			doubleValue = rs.getDouble(yCol);
		  } catch (SQLException e) {
			try {
			  String strDoubleValue = rs.getString (yCol);
			  try {
				doubleValue = Double.parseDouble (strDoubleValue);
			  }
			  catch (NumberFormatException nfe) {
				doubleValue = 0;
			  }
			} catch (SQLException se) {
			  doubleValue = 0;
			}
		  }
		  return doubleValue;
		}
		
        //Runs on the event-dispatching thread.
        public void finished(){
		  if (stringLabels == null) return;
		  
		  ucChart.setLegends(legends);
		  ucChart.setLabels (stringLabels);

		  if (legends.length > 1) {
			if (debug)
			  System.out.println ("GraphPane.finished - num legends " + legends.length);
			
			Color [] colorTable = new Color [legends.length];
			
			for (int i = 0; i < legends.length; i++)
			  colorTable[i] = possibleColors[i%possibleColors.length];

			ucChart.setColors(colorTable);
		  }
		  else 
			ucChart.setColors(new Color [] {Color.blue});

		  Rectangle bounds = ucChart.getBounds ();
		  int height = getPixelHeight(ucChart.getFontMetrics(ucChart.getFont()),legends);
		  
		  ucChart.setBounds ((int)bounds.getX(), (int)bounds.getY(),
		  					 getPixelWidth(ucChart.getFontMetrics(ucChart.getFont()),stringLabels,legends),
		  					 height);

		  ((MyChart)ucChart).setDim(new Dimension((int)ucChart.getBounds().getWidth(),
												  height-10));

		  ucChart.setMinimumSize(ucChart.getPreferredSize());

		  ucChart.invalidate ();
		  scrollPane.invalidate();
		  scrollPane.repaint();
			
		  if (debug)
			System.out.println ("displayResult.worker.construct - bounds " + //before:\n"+bounds + 
								" after\n" + ucChart.getBounds());
			
		  ucChart.setValues (valueMatrix);
		  //			ucChart.setChartType( UniversalChart.PLOT );
		  ucChart.setChartType( UniversalChart.PARALLEL );

		  if (!ucChart.isVisible())
			ucChart.setVisible(true);
		  setText("<HTML><CENTER><H1></H1></CENTER></HTML>");
        }

		protected int getPixelWidth (FontMetrics fontMetrics, String [] labels,
									 String [] legends) {
		  int width = 0;
		  
		  for (int i = 0; i < labels.length; i++)
			width += fontMetrics.stringWidth(labels[i]);
		  
		  if (legends.length > 1)
			width = Math.max (width, legends.length*15);
		  
		  return width;
		}

		protected int getPixelHeight (FontMetrics fontMetrics, String [] legends) {
		  return legends.length*(fontMetrics.getHeight()+2);
		}

      };
	worker.start();  
  }

  protected void setText(String text){
    statusLabel.setText(text);
  }

  //InnerClasses:
  ///////////////

  private static class MyChart extends UniversalChart 
  {
	Dimension d;
	
	public void setDim (Dimension d) 
	{
	  this.d = d;
	}
	
	public Dimension getPreferredSize () 
	{
	  //		System.out.println ("super.getPreferredSize " + super.getPreferredSize () + 
	  //							" min " + getMinimumSize() +
	  //							" max " + getMaximumSize());
	  return d;
	}
	
  }
}
