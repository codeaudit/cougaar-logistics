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
 * Logs to Std streams.
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 2/01/01
 **/
public class StdLogger extends AbstractLogger implements IDLogger{

  //Constructors:
  ///////////////

  public StdLogger(){}

  public StdLogger(int verbosityLevel){
    this.verbosityLevel=verbosityLevel;
  }

  //Members:
  //////////

  public void setVerbosityLevel(int level){
    verbosityLevel=level;
  }

  private static String pad(String s, int l){
    StringBuffer sb = new StringBuffer(s);
    while(sb.length()<l)
      sb.append(' ');
    return sb.toString();
  }

  public void logMessage(int severity, int type, String message){
    if(severity>verbosityLevel)
      return;
    StringBuffer sb = new StringBuffer();
    sb.append('[');
    sb.append(pad(SEVERITIES[severity],9));
    sb.append("][");
    sb.append(pad(TYPES[type],12));
    sb.append("] ");
    sb.append(message);
    chooseAStream(severity, sb);
  }

  public void logMessage(int severity, int type, String message, Exception e){
    if(message==null){
      logMessage(severity,type,e.toString());
    }else{
      logMessage(severity,type,message+": "+e.toString());
    }
  }

  public void logMessage(int id, int severity, int type, String message){
    if(severity>verbosityLevel)
      return;
    StringBuffer sb = new StringBuffer();
    sb.append('[');
    sb.append(pad(SEVERITIES[severity],9));
    sb.append("][");
    sb.append(pad(TYPES[type],12));
    sb.append("][");
    sb.append(id);
    sb.append("] ");
    sb.append(message);
    chooseAStream(severity, sb);
  }

  public void logMessage(int id, int severity, int type, String message, 
			 Exception e){
    if(message==null){
      logMessage(id,severity,type,e.toString());
    }else{
      logMessage(id,severity,type,message+": "+e.toString());
    }
  }

    private void chooseAStream(int severity, StringBuffer sb) {
        if(severity<NORMAL){
          System.err.println(sb.toString());
          System.err.flush();
        }
        else{
          System.out.println(sb.toString());
          System.out.flush();
        }
    }

  //Static Members:
  /////////////////

  public static int getSeverity(String severity){
    for(int i=0;i<SEVERITIES.length;i++){
      if(severity.equals(SEVERITIES[i]))
	return i;
    }
    return NORMAL;
  }
}
