/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
 * Shows a popup of the details about an asset, or all the 
 * assets carried on a leg.
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/27/01
 **/
public class AssetDetailTableModel extends AbstractTableModel{

  //Constants:
  ////////////

  //Variables:
  ////////////

  private List rows;

  private DatabaseConfig dbConfig;
  private int runID;

  //Constructors:
  ///////////////

  public AssetDetailTableModel(DatabaseConfig dbConfig, int runID){
    rows=new ArrayList();
    this.dbConfig=dbConfig;
    this.runID=runID;
  }

  //Members:
  //////////
  public int getRowCount(){
    return rows.size();
  }
  public int getColumnCount(){
    return AssetDetails.getColumnCount();
  }
  public Object getValueAt(int row, int column){
    AssetDetails ad=(AssetDetails)rows.get(row);
    return ad.getValueAt(column);
  }

  public Class getColumnClass(int column){
    return AssetDetails.getColumnClass(column);
  }

  public String getColumnName(int column){
    return AssetDetails.getColumnName(column);
  }

  public void fillWithData(AssetDetailRequest adr){
    List newRows=adr.getAssetDetails(dbConfig, runID);
    int size=rows.size();
    rows.addAll(newRows);
    fireTableRowsInserted(size,rows.size()-1);
  }

  //InnerClasses:
  ///////////////
}
