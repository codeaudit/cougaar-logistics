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
package org.cougaar.mlm.ui.grabber.controller;
import org.cougaar.mlm.ui.grabber.workqueue.Result;

/**
 * Defines a RunResult that represents a failure
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/01/01
 **/
public class FailureRunResult implements RunResult{

  //Constants:
  ////////////

  //Variables:
  ////////////

  private String reason;
  private int id;
  private int runID;
  /** 
   * A hint weather this error represents a Warning or a critical Error
   * (may be ignored by the run)
   **/
  private boolean error;

  //Constructors:
  ///////////////
  
  public FailureRunResult(int id, int runID, String reason, boolean error){
    this.id=id;
    this.runID=runID;
    this.reason=reason;
    this.error=error;
  }

  //Members:
  //////////

  public int getID(){
    return id;
  }

  public int getRunID(){
    return runID;
  }

  public String getReason(){
    return reason;
  }

  public boolean getError(){
    return error;
  }

  public String toString(){
    return (error?"Error: ":"")+reason;
  }

  //InnerClasses:
  ///////////////

}
