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

import javax.swing.table.AbstractTableModel;

import java.util.List;
import java.util.ArrayList;

/**
 * Shows a popup of the details about an carrier, or all the 
 * carriers carried on a leg.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 5/3/01
 **/
public class CarrierDetailTableModel extends AbstractTableModel{

  //Constants:
  ////////////

  public static final int FIELD = 0;
  public static final int VALUE = 1;

  protected static final String[] columnNames={"Field",
					       "Value"};  
  protected static final Class[] columnClasses={String.class,
						Object.class};

  //Variables:
  ////////////

  private CarrierDetails cd;
  
  private DatabaseConfig dbConfig;
  private int runID;

  //Constructors:
  ///////////////

  public CarrierDetailTableModel(DatabaseConfig dbConfig, int runID){
    this.dbConfig=dbConfig;
    this.runID=runID;
  }

  //Members:
  //////////
  public int getRowCount(){
    return cd.getFieldCount();
  }
  public int getColumnCount(){
    return columnClasses.length;
  }
  public Object getValueAt(int row, int column){
    switch(column){
    case FIELD:
      return cd.getFieldName(row);
    case VALUE:
      return cd.getValueAt(row);
    }    
    System.err.println("CarrierDetailTableModel: unknown column: "+column);
    return null;
  }

  public Class getColumnClass(int column){
    return columnClasses[column];
  }

  public String getColumnName(int column){
    return columnNames[column];
  }

  public void fillWithData(CarrierDetailRequest cdr){
    cd = cdr.getCarrierDetails(dbConfig, runID);
    fireTableDataChanged();
  }

  //InnerClasses:
  ///////////////
}
