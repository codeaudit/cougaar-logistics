/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
