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
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

import java.util.List;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Gets data about carrier details
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 5/3/01
 **/
public class CarrierDetailRequest{

  //Constants:
  ////////////

  //Variables:
  ////////////

  private String convID;

  //Constructors:
  ///////////////
  public CarrierDetailRequest(String convID){
    this.convID=convID;
  }

  //Members:
  //////////

  protected String getTableName(String baseName, int runID){
    return Controller.getTableName(baseName,runID);
  }

  public CarrierDetails getCarrierDetails(DatabaseConfig dbConfig, int runID){
    CarrierDetails ret = new CarrierDetails();
    try{
      Statement s = dbConfig.getConnection().createStatement();
      ResultSet rs= s.executeQuery(getSql(dbConfig,runID));
      if(rs.next()){
	addCarrierFields(ret,rs);
      }
      if(s!=null)
	s.close();
    }catch(SQLException e){
      System.err.println("SQL Exception getting Carrier Details: "+e);
    }
    return ret;
  }

  protected void addCarrierFields(CarrierDetails cd, 
				  ResultSet rs)
    throws SQLException{
    cd.setValueAt(new String(rs.getString(1)),CarrierDetails.OWNER);
    //cd.setValueAt(new String(rs.getString(2)),CarrierDetails.HOMEBASE);
    cd.setValueAt(new Integer(rs.getInt(3)),CarrierDetails.CONVTYPE);
    cd.setValueAt(new String(rs.getString(4)),CarrierDetails.PROTOTYPE);
    cd.setValueAt(new String(rs.getString(5)),CarrierDetails.PROTONAME);
    cd.setValueAt(new String(rs.getString(6)),CarrierDetails.BUMPERNO);
    cd.setValueAt(new Integer(rs.getInt(7)),CarrierDetails.SELFPROP);
    cd.setValueAt(new Double(rs.getDouble(8)),CarrierDetails.AVESPEED);
    cd.setValueAt(new Double(rs.getDouble(9)),CarrierDetails.WEIGHTCAP);
    cd.setValueAt(new Double(rs.getDouble(10)),CarrierDetails.AREACAP);
    cd.setValueAt(new Double(rs.getDouble(11)),CarrierDetails.VOLCAP);
  }

  protected String getSql(DatabaseConfig dbConfig, int runID){
    String ciTable=getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,runID);
    String cpTable=getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,runID);

    //For select:
    String ciOwner="ci."+DGPSPConstants.COL_OWNER;
    String ciHomeBase="ci."+DGPSPConstants.COL_BASELOC;
    String cpConvType="cp."+DGPSPConstants.COL_CONVEYANCE_TYPE;
    String cpAlpType="cp."+DGPSPConstants.COL_ALP_TYPEID;
    String cpAlpNomen="cp."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String ciBumperno="ci."+DGPSPConstants.COL_BUMPERNO;
    String ciSelfProp="ci."+DGPSPConstants.COL_SELFPROP;
    String cpAveSpeed="cp."+DGPSPConstants.COL_AVE_SPEED;
    String cpWeightCap="cp."+DGPSPConstants.COL_WEIGHT_CAP;
    String cpVolCap="cp."+DGPSPConstants.COL_VOL_CAP;
    String cpAreaCap="cp."+DGPSPConstants.COL_AREA_CAP;

    //For Where:
    String ciConvID="ci."+DGPSPConstants.COL_CONVEYANCEID;
    String ciProtoID="ci."+DGPSPConstants.COL_PROTOTYPEID;
    String cpProtoID="cp."+DGPSPConstants.COL_PROTOTYPEID;

    String sql="select "+
      ciOwner+", "+
      ciHomeBase+", "+
      cpConvType+", "+
      cpAlpType+", "+
      cpAlpNomen+", "+
      ciBumperno+", "+
      ciSelfProp+", "+
      cpAveSpeed+", "+
      cpWeightCap+", "+
      cpAreaCap+", "+
      cpVolCap+
      " from "+
      ciTable+" ci, "+   
      cpTable+" cp "+
      " where "+
      ciConvID+"='"+convID+"'\nand "+
      ciProtoID+"="+cpProtoID;

    return sql;
  }

  //InnerClasses:
  ///////////////
}
