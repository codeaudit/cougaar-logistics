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
package org.cougaar.mlm.ui.grabber.logger;

/**
 * Logs to both logs specified.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/01/01
 **/
public class PairIDLogger implements IDLogger{

  //Constants:
  ////////////

  //Variables:
  ////////////
  IDLogger a;
  IDLogger b;

  //Constructors:
  ///////////////
  public PairIDLogger(IDLogger a, IDLogger b){
    this.a=a;
    this.b=b;
  }

  //Members:
  //////////
  
  public boolean isWarningEnabled   () { return true; }
  public boolean isImportantEnabled () { return true; }
  public boolean isNormalEnabled    () { return true; }
  public boolean isMinorEnabled     () { return true; }
  public boolean isTrivialEnabled   () { return true; }

  public void logMessage(int severity, int type, String message){
    a.logMessage(severity,type,message);
    b.logMessage(severity,type,message);
  }

  public void logMessage(int severity, int type, String message, Exception e){
    a.logMessage(severity,type,message,e);
    b.logMessage(severity,type,message,e);
  }

  public void logMessage(int id, int severity, int type, String message){
    a.logMessage(id,severity,type,message);
    b.logMessage(id,severity,type,message);
  }

  public void logMessage(int id, int severity, int type, String message, 
			 Exception e){
    a.logMessage(id,severity,type,message,e);
    b.logMessage(id,severity,type,message,e);
  }
}
