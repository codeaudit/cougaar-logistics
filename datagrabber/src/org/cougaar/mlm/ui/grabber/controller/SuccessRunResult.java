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
package org.cougaar.mlm.ui.grabber.controller;
import org.cougaar.mlm.ui.grabber.workqueue.Result;

/**
 * Defines a RunResult that represents a success
 *
 * @since 2/01/01
 **/
public class SuccessRunResult implements RunResult{

  //Constants:
  ////////////

  //Variables:
  ////////////

  private String reason;
  private int id;
  private int runID;
  private boolean warning;
  protected String creator, cluster;

  //Constructors:
  ///////////////
  
  public SuccessRunResult(int id, int runID){
    this.id=id;
    this.runID=runID;
    warning=false;
    reason="";
  }
  
  public SuccessRunResult(int id, int runID, String creator, String cluster) {
    this (id, runID);
    this.creator = creator;
    this.cluster = cluster;
  }

  public SuccessRunResult(int id, int runID, boolean warning, String reason, String creator, String cluster){
    this.id=id;
    this.runID=runID;
    this.reason=reason;
    this.warning=warning;
    this.creator = creator;
    this.cluster = cluster;
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

  public boolean getWarning(){
    return warning;
  }

  public String getCreator(){
    return creator;
  }

  public String getCluster(){
    return cluster;
  }

  public String toString(){
    return "Successful Result" + (warning?" Warning: ":"")+reason+" created by " + creator;
  }

  //InnerClasses:
  ///////////////

}
