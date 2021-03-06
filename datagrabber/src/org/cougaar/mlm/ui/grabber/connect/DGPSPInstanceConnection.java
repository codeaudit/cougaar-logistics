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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.core.util.UID;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.mlm.ui.psp.transit.data.instances.Instance;
import org.cougaar.mlm.ui.psp.transit.data.instances.InstancesData;
import org.cougaar.mlm.ui.psp.transit.data.instances.InstancesDataFactory;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DataGathererPSPConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

import java.sql.*;

import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.text.DecimalFormat;

import org.cougaar.planning.ldm.measure.Mass;

/**
 * Handles getting instance data from DataGatherer PSP
 *
 * @since 2/19/01
 **/
public class DGPSPInstanceConnection extends DGPSPConnection 
  implements DGPSPConstants{

  public DGPSPInstanceConnection(int id, int runID,
				 DataGathererPSPConfig pspConfig, 
				 DBConfig dbConfig,
				 Connection c,
				 Logger l){
    super(id, runID, pspConfig, dbConfig, c,l);
  }

  //Members:
  //////////

  //Gets:

  /**return the DeXMLableFactory specific to this URL connection**/
  protected DeXMLableFactory getDeXMLableFactory(){
    return new InstancesDataFactory();
  }

  //Actions:

  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    InstancesData data=(InstancesData)obj;
    PreparedStatement instancePS, manifestPS;

    try{
      instancePS = getInstancePreparedStatement (c);
      manifestPS = getManifestPreparedStatement (c);
      if (isTrivialEnabled()) {
	logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
		   getClusterName()+" instance created prepared statements.");
      }
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getInstancesIterator();
    setStatus("Creating batch updates for instances...");
    while(iter.hasNext()){
      num++;
      Instance part=(Instance)iter.next();
      if(!updateAssetInstance(instancePS,part))
	unsuccessful++;
      if(part.hasManifest) {
	if(!updateManifest(manifestPS,part))
	  unsuccessful++;
      }

      if ((num +1) % 1000 == 0) {
	try{
	  if (isMinorEnabled()) {
	    logMessage(Logger.MINOR,Logger.DB_WRITE,
		       getClusterName()+" executing a prepared batch of a thousand instances, " + num + " so far ");
	  }
	  instancePS.executeBatch();
	} catch(SQLException e){
	  if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){ 
	    // this well happen, since the same asset UID may appear in multiple agents
	    logMessage(Logger.WARNING,Logger.DB_WRITE,"While executing batch, got SQL Error - " + e);
	    e.printStackTrace ();
	  }
	}

	// we don't want an SQLException in instancePS to interfere with the manifest prepared statement
	// so we put them in separate try-catch blocks

	try{
	  if (isMinorEnabled()) {
	    logMessage(Logger.MINOR,Logger.DB_WRITE,
		       getClusterName()+" executing a prepared batch of a thousand instances, " + num + " so far ");
	  }
	  manifestPS.executeBatch();
	}catch(SQLException e){
	  if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){ 
	    // this well happen, since the same asset UID may appear in multiple agents
	    logMessage(Logger.WARNING,Logger.DB_WRITE,"While executing batch, got SQL Error - " + e);
	    e.printStackTrace ();
	  }
	}
      }

      if(halt)return;
    }

    try{
      if (isMinorEnabled()) {
	logMessage(Logger.MINOR,Logger.DB_WRITE,
		   getClusterName()+" executing the last prepared batch, " + num + " total instances done.");
      }

      instancePS.executeBatch();
    }catch(SQLException e){
      if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){ 
	// this well happen, since the same asset UID may appear in multiple agents
	logMessage(Logger.WARNING,Logger.DB_WRITE,"SQL Error - " + e);
	e.printStackTrace ();
      }
    }

    try{
      manifestPS.executeBatch();
    }catch(SQLException e){
      if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){ 
	// this well happen, since the same asset UID may appear in multiple agents
	logMessage(Logger.WARNING,Logger.DB_WRITE,"SQL Error - " + e);
	e.printStackTrace ();
      }
    }

    if(unsuccessful>0) {
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " instances(s)");
    }

    try { 
      instancePS.close();
      manifestPS.close(); 
    } catch(Exception e){
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" got exception closing prepared statement " + instancePS + 
		 " or " + manifestPS +
		 " - exception was : " + e);
      e.printStackTrace ();
    }

    setStatus("Done");
  }

  protected PreparedStatement getInstancePreparedStatement (Connection con) throws SQLException {
    StringBuffer sb=new StringBuffer();

    sb.append("INSERT INTO ");
    sb.append(getTableName(ASSET_INSTANCE_TABLE));
    sb.append(" (");
    sb.append(COL_ASSETID);sb.append(",");
    sb.append(COL_AGGREGATE);sb.append(",");
    sb.append(COL_OWNER);sb.append(",");
    sb.append(COL_PROTOTYPEID);sb.append(",");
    sb.append(COL_NAME);sb.append(",");
    sb.append(COL_ALP_ITEM_NOMEN);
    sb.append(") VALUES(");
    appendQueryParams(sb, 5);
    sb.append("?");sb.append(")");

    return con.prepareStatement(sb.toString());
  }

  protected PreparedStatement getManifestPreparedStatement (Connection con) throws SQLException {
    StringBuffer sb=new StringBuffer();

    sb.append("INSERT INTO ");
    sb.append(getTableName(MANIFEST_TABLE));
    sb.append(" (");
    sb.append (COL_MANIFEST_ITEM_ID);sb.append(",");
    sb.append (COL_ASSETID);sb.append(",");
    sb.append (COL_NAME);sb.append(",");
    sb.append (COL_ALP_TYPEID);sb.append(",");
    sb.append (COL_ALP_NOMENCLATURE);sb.append(",");
    sb.append (COL_WEIGHT);

    sb.append(") VALUES(");
      appendQueryParams(sb, 5);
      sb.append("?");sb.append(")");

    return con.prepareStatement(sb.toString());
  }
    Map uidToString = new HashMap ();

  protected boolean updateAssetInstance(PreparedStatement s, Instance part){
    try{
      int col = 0;
      s.setString(++col,getUID(part.UID, uidToString));
      s.setLong  (++col,part.aggregateNumber);
      s.setString(++col,part.ownerID==null?"":part.ownerID);
      s.setString(++col,part.prototypeUID);
      s.setString(++col,part.name==null?"":part.name);
      s.setString(++col,part.itemNomen);
      s.addBatch();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not add batch to table("+
		   getTableName(CONVEYED_LEG_TABLE)+")"+
		   "["+e.getSQLState()+"]",e);
      return false;
    }
    return true;
  }

  protected String getUID (UID uid, Map uidToString) {
    String returnedUID = (String) uidToString.get (uid);
    if (returnedUID == null) {
      uidToString.put (uid, (returnedUID = uid.toString()));
    }
    return returnedUID;
  }

  DecimalFormat noExponentNoFractionDoubleFormat=new DecimalFormat ("#");

  protected boolean updateManifest(PreparedStatement s, Instance part){
    try {
      int i = 0;
      Iterator typeIter = part.typeIdentifications.iterator();
      Iterator weightsIter = part.weights.iterator();
      Iterator receiverIter = part.receivers.iterator();

      for (Iterator nomenIter = part.nomenclatures.iterator (); nomenIter.hasNext ();) {
	i++;
	String nomen = (String) nomenIter.next ();
	String type  = (String) typeIter.next ();
	double weight = ((Mass) weightsIter.next ()).getGrams();
	String receiver = (String) receiverIter.next ();
	if (logger.isTrivialEnabled()) {
	  logMessage(Logger.TRIVIAL,Logger.RESULT," Found receiver " + receiver);
	}

	int col = 0;
	s.setString(++col,part.UID + "-item-" + i);
	s.setString(++col,part.UID.toString());
	s.setString(++col,receiver +":"+part.name+"-item-"+nomen+"-"+i);
	s.setString(++col,type);
	s.setString(++col,nomen+" "+type);
	s.setDouble(++col,weight);
	s.addBatch ();
      }

    }catch(SQLException e){
      if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){
	haltForError(Logger.DB_WRITE,"Could not add batch to table("+
		     getTableName(MANIFEST_TABLE)+")"+
		     "["+e.getSQLState()+"]",e);
	return false;
      }
    }

    return true;
  }

  protected RunResult prepResult(DeXMLable obj){
    setStatus("Starting");
    InstancesData data=(InstancesData)obj;
    InstanceRunResult irr = new InstanceRunResult(getID(),getRunID());
    Iterator iter=data.getInstancesIterator();
    while(iter.hasNext()){
      Instance part=(Instance)iter.next();
      UID muid=part.manifestUID;
      /**if a Container, MilVan, or Pallet**/
      if(!(muid==null||muid.equals(""))){
	irr.addManifestUID(muid);
      }
    }
    setStatus("Done");
    if (isMinorEnabled()) {
      logMessage(Logger.MINOR,Logger.RESULT,"Produced Result");
    }
    return irr;
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
  public class InstanceRunResult extends SuccessRunResult{
    private Set manifestUIDs;
    public InstanceRunResult(int id, int runID){
      super(id,runID,getSimpleName(),getClusterName());
      this.manifestUIDs=new HashSet(89);
    }
    public void addManifestUID(UID uid){
      manifestUIDs.add(uid);
    }
    public Iterator getManifestUIDIterator(){
      return manifestUIDs.iterator();
    }
  }
}
