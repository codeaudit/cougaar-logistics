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
package org.cougaar.mlm.ui.newtpfdd.gui.view.details;

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * Shows a popup of the details about an carrier, or all the 
 * carriers carried on a leg.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 4/27/01
 **/
public class CarrierDetailView extends JFrame{

  //Constants:
  ////////////

  //Variables:
  ////////////
  protected DatabaseConfig dbConfig;
  protected int runID;

  //Constructors:
  ///////////////

  public CarrierDetailView(DatabaseConfig dbConfig, int runID,
			 CarrierDetailRequest cdr){
    super("Carrier Details");
    this.dbConfig=dbConfig;
    this.runID=runID;
    setupGUI(cdr);
  }

  //Members:
  //////////
  
  protected void setupGUI(CarrierDetailRequest cdr){
    JPanel content=new JPanel(new BorderLayout());
    CarrierDetailTableModel tableModel=
      new CarrierDetailTableModel(dbConfig,runID);
    tableModel.fillWithData(cdr);
    JTable table=new JTable(tableModel);
    JScrollPane sp =new JScrollPane(table);
    content.add(sp, BorderLayout.CENTER);

    JButton close=new JButton("Dismiss");
    close.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
	  dispose();
	}
      });
    content.add(close,BorderLayout.SOUTH);

    setContentPane(content);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(400,400);
    show();
  }

  //InnerClasses:
  ///////////////
}