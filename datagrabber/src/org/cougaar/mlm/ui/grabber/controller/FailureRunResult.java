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
