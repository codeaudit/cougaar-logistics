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
package org.cougaar.mlm.ui.newtpfdd.gui.view.query;

import java.util.Date;
import org.cougaar.mlm.ui.grabber.controller.Run;

/** run information from the point of view of the client */

public class DatabaseRun {
  String database;
  int runID;
  int condition;
  Date timestamp;

  boolean hasRollupTable=false;

  boolean hasCargoTypeTable=false;
  boolean hasCargoInstanceTable=false;
  boolean hasCargoLegTable=false;

  boolean hasCarrierTypeTable=false;
  boolean hasCarrierInstanceTable=false;

  boolean useDerivedTables = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun.useDerivedTables", 
				       "true"));


  public DatabaseRun (String database, int runID, int condition, Date timestamp) {
	this.database = database;
	this.runID = runID;
	this.condition = condition;
	this.timestamp = timestamp;
  }

  public int getRunID () { return runID; }
  public String getDatabase () { return database; }
  public int getCondition () { return condition; }
  public Date getTimestamp () { return timestamp; }

  public boolean equals (Object other) {
    if (other == null) return false;

    DatabaseRun otherRun = (DatabaseRun) other;
    return database.equals (otherRun.getDatabase()) &&
      runID == otherRun.getRunID ();
  }
  
  public String toString () {
	//	if (showCondition)
	String cond = (condition == Run.COND_WARNING) ? " w " : " OK";
	return "Run #" + runID + " " + timestamp + cond;
  }

  public void setHasRollupTable(boolean val){
    hasRollupTable=val;
  }

  public boolean hasRollupTable(){
    return useDerivedTables&&hasRollupTable;
  }


  public void setHasCargoTypeTable(boolean val){
    hasCargoTypeTable=val;
  }

  public boolean hasCargoTypeTable(){
    return useDerivedTables&&hasCargoTypeTable;
  }

  public void setHasCargoInstanceTable(boolean val){
    hasCargoInstanceTable=val;
  }

  public boolean hasCargoInstanceTable(){
    return useDerivedTables&&hasCargoInstanceTable;
  }

  public void setHasCargoLegTable(boolean val){
    hasCargoLegTable=val;
  }

  public boolean hasCargoLegTable(){
    return useDerivedTables&&hasCargoLegTable;
  }


  public void setHasCarrierTypeTable(boolean val){
    hasCarrierTypeTable=val;
  }

  public boolean hasCarrierTypeTable(){
    return useDerivedTables&&hasCarrierTypeTable;
  }

  public void setHasCarrierInstanceTable(boolean val){
    hasCarrierInstanceTable=val;
  }

  public boolean hasCarrierInstanceTable(){
    return useDerivedTables&&hasCarrierInstanceTable;
  }
}
