/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 
package org.cougaar.logistics.ui.inventory;

import java.util.Date;

import java.awt.Event;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.cougaar.logistics.ui.inventory.data.InventoryData;

/** 
 * <pre>
 * 
 * The MultiChartPanel contains the three InventoryCharts
 * to be displayed: the levels, refill, and demand charts.
 * Its just a container that measures lays out the 
 * charts appropriatly.
 *
 *
 * @see InventoryLevelChart;
 * @see InventoryRefillChart;
 * @see InventoryDemandChart;
 * 
 *
 **/

public class MultiChartPanel extends JPanel {

    protected InventoryLevelChart levelChart;
    protected InventoryRefillChart refillChart;
    protected InventoryDemandChart demandChart;

    public MultiChartPanel() {
	super();
	initializeCharts();
    }

    public void initializeCharts() {
	int gridx = 0;
	int gridy = 0;

	Insets blankInsets = new Insets(0, 0, 0, 0);
	levelChart = new InventoryLevelChart();
	refillChart = new InventoryRefillChart();
	demandChart = new InventoryDemandChart();

	// set header and legend to black
	// set beveled borders around plot area and chart
	//((JLabel)chart.getHeader()).setForeground(Color.black);
	//chart.setBorder(BorderFactory.createLoweredBevelBorder());
	//chart.getChartArea().setBorder(BorderFactory.createLoweredBevelBorder());

	// add chart to panel
	setLayout(new GridBagLayout());
	add(levelChart, new GridBagConstraints(gridx, gridy++, 1, 1, 1.0, 1.0,
					       GridBagConstraints.CENTER, 
					       GridBagConstraints.BOTH, 
					       blankInsets, 0, 0));

	add(refillChart, new GridBagConstraints(gridx, gridy++, 1, 1, 1.0, 1.0,
					       GridBagConstraints.CENTER, 
					       GridBagConstraints.BOTH, 
					       blankInsets, 0, 0));

	add(demandChart, new GridBagConstraints(gridx, gridy++, 1, 1, 1.0, 1.0,
					       GridBagConstraints.CENTER, 
					       GridBagConstraints.BOTH, 
					       blankInsets, 0, 0));
    }
   
    public void setData(InventoryData data) {
	levelChart.setData(data);
	refillChart.setData(data);
	demandChart.setData(data);
    }
}
