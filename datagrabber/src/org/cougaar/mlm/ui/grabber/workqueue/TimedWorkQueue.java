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
package org.cougaar.mlm.ui.grabber.workqueue;

import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

/**
 * <pre>
 * Defines a queue of timed work.  Has a watcher thread that 
 * periodically (every 10 seconds) wakes up and checks to see
 * if any work has exceeded its maximum duration.  If it has,
 * it tells the thread that is performing the work to time it out
 * and takes it off the list of available threads.
 *
 * </pre>
 * @author Gordon Vidaver; last modified by: $Author: mthome $
 *
 * @since 2/01/01
 **/
public class TimedWorkQueue extends WorkQueue{
  /** by default, checks for timed out work every 10 seconds */
  public static long TIMEOUT_POLL_PERIOD = 10000l;
  public static boolean VERBOSE = false;

  protected Thread watcher;

  public TimedWorkQueue(Logger l, ResultHandler r){ super (l,r);  }
  
  protected void putWorkOnNewThread (Work w) {
    super.putWorkOnNewThread (w);
    startWatcher();
  }

  protected void putWorkOnQueue (Work w) {
    super.putWorkOnQueue (w);
    startWatcher();
  }

  protected void startWatcher () {
    if (watcher == null || !watcher.isAlive ()) {
      watcher = new Thread (new WorkQueueWatcher());
      watcher.start();
    }
  }

  /** 
   * <pre>
   * Walks active threads looking for timed work 
   *
   * If the thread has spent more than the allotted time, times it out.
   * The work is not "halted", but a special result is returned by the 
   * TimedWork and its thread is removed from the active
   * thread list.  Basically, this thread could block forever, and so
   * we should just put it aside, not use it, and hope it stops by itself.
   *
   * Note that if the thread is blocking on i/o, the Work should
   * implement a getTimedOutResult method that attempts to close 
   * the underlying i/o -- 
   *   BUT BE WARNED : If this is a 1.3.1 URLConnection blocked in a 
   * read, the calling thread will block on the stream.close, 
   * so be careful!
   *
   * Calls TimedWork.getTimedOutResult to create the special result.
   * </pre>
   * @see org.cougaar.mlm.ui.grabber.workqueue.TimedWork#getTimedOutResult
   */
  public synchronized void haltTimedOutWork () {
    if (!activeThreads.isEmpty ())
      logger.logMessage(Logger.MINOR,Logger.GENERIC,
			"TimedWorkQueue - Checking " + activeThreads.size () + " active threads.");
    for(Iterator iter=activeThreads.iterator(); iter.hasNext();){
      WorkThread wt=(WorkThread)iter.next();
      Work currentWork = wt.getCurrentWork ();
      if (currentWork instanceof TimedWork) {
	TimedWork timedWork = (TimedWork) currentWork;
	if (timedWork.isOverdue ()) {
	  logger.logMessage(Logger.IMPORTANT,Logger.STATE_CHANGE,
			    "Time exceeded for active work: "+timedWork.getID());
	  // this thread is done
	  // other holder could be WorkThread.run, right after perform returns
	  synchronized (wt.getWorkLock ()) {
	    wt.timeOut (timedWork.getTimedOutResult ());
	  }
	}
      }
      else {
	logger.logMessage(Logger.MINOR,Logger.GENERIC,
			  "Skipping non-TimedWork work " + currentWork.getID());
      }
    }
    logger.logMessage(Logger.MINOR,Logger.GENERIC,
		      "TimedWorkQueue - finished checking active threads.");
  }

  private class WorkQueueWatcher implements Runnable {
    public void run () {
      try {
	while (isBusy()) {
	  // periodically wake up and check for timed out work
	  if (VERBOSE)
	    System.out.println (this + " - " + Thread.currentThread () + 
				" is busy, now sleeping.");
	  Thread.currentThread().sleep (TIMEOUT_POLL_PERIOD);
	  if (VERBOSE)
	    System.out.println (this + " - " + Thread.currentThread () + 
				" checking for timed out work. ");
	  haltTimedOutWork ();
	}
	if (VERBOSE)
	  System.out.println (this + " - " + Thread.currentThread () + " will die now.");
      } catch (Exception e) {e.printStackTrace();};
    }
  };
}
