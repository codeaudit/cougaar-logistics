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
package org.cougaar.mlm.ui.grabber.logger;

/**
 * Logs to Std streams.
 * @author Benjamin Lubin; last modified by: $Author: mthome $
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
