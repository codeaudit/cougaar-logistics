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

import java.text.*;

import  org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

/**
 * Contains the fields of data about a given carrier.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 5/3/01
 **/
public class CarrierDetails{
  //Constants:
  ////////////
  public static final int OWNER = 0;
  /*Not yet filled by PSP
  public static final int HOMEBASE = 1; 
  */
  public static final int CONVTYPE = 1; //Form
  public static final int PROTOTYPE = 2;
  public static final int PROTONAME = 3;
  public static final int BUMPERNO = 4;
  public static final int SELFPROP = 5;
  public static final int AVESPEED = 6;
  public static final int WEIGHTCAP = 7;
  public static final int AREACAP = 8;
  public static final int VOLCAP = 9;

  private static double KGS_TO_STONS = 0.0011022927689594355d;
  private static double METERS_TO_FEET = 3.28084d;
  private static double SQUARE_METERS_TO_SQUARE_FEET = 10.763867d;
  //  private static double CUBIC_METERS_TO_CUBIC_FEET = 35.314667d;
  private static double LITERS_TO_CUBIC_FEET = 0.035315d;

  protected static final String[] fieldNames={"Owner",
					      //"Home base",
					      "Form",
					      "Type",
					      "Type Name",
					      "Carrier Name",
					      "Self Prop Asset",
					      "Ave Speed (mph)",
					      "Weight Cap (stons)",
					      "Area Cap (ft^2)",
					      "Vol Cap (ft^3)"};
  
  protected static final Class[] fieldClasses={String.class,
					       //String.class,
					       String.class,
					       String.class,
					       String.class,
					       String.class,
					       String.class,
					       String.class,
					       String.class,
					       String.class,
					       String.class};

  //Variables:
  ////////////
  private String owner;
  private String homeBase;
  private int convType;
  private String prototype;
  private String protoName;
  private String bumperno;
  private int selfProp;
  private String aveSpeed;
  private String weightCap;
  private String areaCap;
  private String volCap;

  private DecimalFormat format = new DecimalFormat("#.#");

  //Constructors:
  ///////////////

  public CarrierDetails(){
  }

  //Members:
  //////////
  public void setValueAt(Object o, int field){
    switch(field){
    case OWNER:
      owner=(String)o;break;
    /*
    case HOMEBASE:
      homeBase=(String)o;break;
    */
    case CONVTYPE:
      convType=((Integer)o).intValue();break;
    case PROTOTYPE:
      prototype=(String)o;break;
    case PROTONAME: 
      protoName=(String)o;break;
    case BUMPERNO: 
      bumperno=(String)o;break;
    case SELFPROP: 
      selfProp=((Integer)o).intValue();break;
    case AVESPEED: 
      aveSpeed=format.format(((Double)o).doubleValue());break;
    case WEIGHTCAP: 
      weightCap=format.format((((Double)o).doubleValue()/1000)*KGS_TO_STONS);break;
    case AREACAP: 
      System.out.println("area value " + ((Double)o).doubleValue());
      areaCap=format.format(((Double)o).doubleValue()*SQUARE_METERS_TO_SQUARE_FEET);break;
    case VOLCAP: 
      System.out.println("vol value " + ((Double)o).doubleValue());
      volCap=format.format(((Double)o).doubleValue()*LITERS_TO_CUBIC_FEET);break;
    }
  }

  public Object getValueAt(int field){
    switch(field){
    case OWNER:
      return owner;
    /*
    case HOMEBASE:
      return homeBase.equals("null")?"":homeBase;
    */
    case CONVTYPE:
      return convTypeToString(convType);
    case PROTOTYPE:
      return prototype;
    case PROTONAME: 
      return protoName;
    case BUMPERNO: 
      return bumperno;
    case SELFPROP: 
      return selfPropToString(selfProp);
    case AVESPEED: 
      return aveSpeed;
    case WEIGHTCAP: 
      return weightCap;
    case AREACAP: 
      return areaCap;
    case VOLCAP: 
      return volCap;
    default:
      return null;
    }
  }
  
  protected static String selfPropToString(int val){
    switch(val){
    case 0:
      return "N";
    case 1:
      return "Y";
    default:
      return "Unknown";
    }
  }

  protected static String convTypeToString(int type){
    if(type<0||type>=DGPSPConstants.CONVEYANCE_TYPES.length)
      return "Unknown";
    return DGPSPConstants.CONVEYANCE_TYPES[type];
  }

  //Statics:
  //////////

  public static int getFieldCount(){
    return fieldClasses.length;
  }

  public static String getFieldName(int field){
    return fieldNames[field];
  }

  public static Class getFieldClass(int field){
    return fieldClasses[field];
  }
}
