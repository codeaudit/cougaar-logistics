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
 * Interface used for logging status/errors
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
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
}
