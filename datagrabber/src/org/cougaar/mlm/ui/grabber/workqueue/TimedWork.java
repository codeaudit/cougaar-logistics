/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.grabber.workqueue;

/**
 * Defines a timed piece of work to do
 * @author Gordon Vidaver; last modified by: $Author: gvidaver $
 *
 * @since 01/11/02
 **/
public interface TimedWork extends Work {

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  //Members:
  //////////

  public long getStart ();

  public void setDuration (long dur);
  public long getDuration ();
  
  /** how long has the work taken, in wall-clock time */
  public long timeSpent();

  /** returns true if spent more than the allotted time */
  public boolean isOverdue();
  
  /** 
   * return result when the work times out.
   *
   * may also want to attempt to clean up here, e.g. 
   * attempt to close open connections, etc.
   *
   * This is the work's opportunity to communicate to whoever 
   * is waiting for it and tell them information, like that
   * the work timed out.
   */
  public Result getTimedOutResult ();
  //InnerClasses:
  ///////////////
}
