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
 * Interface used for logging status/errors
 *
 * @since 2/01/01
 **/
public interface Logger{

  //Constants:
  ////////////

  //Severities:
  public static final int FATAL = 0;
  public static final int ERROR = 1;
  public static final int WARNING = 2;
  public static final int IMPORTANT = 3;
  public static final int NORMAL = 4;
  public static final int MINOR = 5;
  public static final int TRIVIAL = 6;

  public static final String[] SEVERITIES={"Fatal",
					   "Error",
					   "Warning",
					   "Important",
					   "Normal",
					   "Minor",
					   "Trivial"};

  //Types:
  public static final int GENERIC = 0;
  public static final int STATE_CHANGE = 1;
  public static final int NET_IO = 2;
  public static final int FILE_IO = 3;
  public static final int WEB_IO = 4;
  public static final int DB_CONNECT = 5;
  public static final int DB_STRUCTURE = 6;
  public static final int DB_WRITE = 7;
  public static final int DB_QUERY = 8;
  public static final int PARSE = 9;
  public static final int REQUEST = 10;
  public static final int RESULT = 11;
  public static final int DATA_CONSISTENCY=12;

  public static final String[] TYPES={"Generic",
				      "State Change",
				      "Net IO",
				      "File IO",
				      "Web IO",
				      "DB Connect",
				      "DB Structure",
				      "DB Write",
				      "DB Query",
				      "Parse",
				      "Request",
				      "Result",
				      "Data Consistency"};

  //Members:
  //////////

  public void logMessage(int severity, int type, String message);

  public void logMessage(int severity, int type, String message, Exception e);

  boolean isWarningEnabled   ();
  boolean isImportantEnabled ();
  boolean isNormalEnabled    ();
  boolean isMinorEnabled     ();
  boolean isTrivialEnabled   ();
}
