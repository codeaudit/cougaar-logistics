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
package org.cougaar.mlm.ui.grabber.workqueue;

import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

/**
 * Defines a queue of work to do, and manages threads to get it
 * done
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 2/01/01
 **/
public class WorkQueue{

  //Constants:
  ////////////

  //Variables:
  ////////////
  protected Logger logger;
  protected ResultHandler resultHandler;

  protected LinkedList activeThreads;
  protected LinkedList inactiveThreads;

  protected LinkedList workQ;

  protected int keepAlive = 10000;
  protected int maxThreads = 20;
  protected int threadsCreated=0;

  protected int assignedID=1;

  public static final int NO_CURRENT_WORK = -1;
  
  //Constructors:
  ///////////////

  public WorkQueue(Logger l, ResultHandler r){
    logger=l;
    resultHandler=r;
    activeThreads=new LinkedList();
    inactiveThreads=new LinkedList();
    workQ=new LinkedList();
  }

  //Members:
  //////////

  public void setMaxThreads(int num){
    maxThreads=num;
  }

  //Query:

  public synchronized Map getWorkIDToStatusMap(){
    Map ret=new HashMap(31);
    Iterator iter=workQ.iterator();
    while(iter.hasNext()){
      Work w=(Work)iter.next();
      ret.put(new Integer(w.getID()),w.getStatus());
    }
    iter=activeThreads.iterator();
    while(iter.hasNext()){
      WorkThread wt=(WorkThread)iter.next();
      ret.put(new Integer(wt.getWorkID()),wt.getWorkStatus());
    }
    return ret;
  }

  //public

  public int getValidID(){
    return assignedID++;
  }

  /** 
   * Uses createWorkThread to create new threads, so is
   * safe even if momentarily run out of threads.
   */
  public synchronized void enque(Work w){
    if (logger.isTrivialEnabled())
      logger.logMessage(Logger.TRIVIAL,Logger.STATE_CHANGE,
			"Work "+w.getID()+" enqued");
    if(numInactiveThreads()==0 && numThreads()<maxThreads){
	  putWorkOnNewThread (w);
    }else{
	  putWorkOnQueue (w);
    }
  }

  protected void putWorkOnNewThread (Work w) {
	WorkThread wt = createWorkThread (w);
	addActiveThread(wt);
	wt.start();
  }

  protected void putWorkOnQueue (Work w) {
	workQ.addLast(w);
	if(numInactiveThreads()>0){
	  WorkThread wt=(WorkThread)inactiveThreads.getFirst();
	  if(wt!=null){
		wt.awaken();
	  }
	}
  }

  /** how many threads are active at this moment */
  public synchronized int getNumActiveWork () {
	return activeThreads.size ();
  }

  public synchronized boolean isBusy () {
	return (!activeThreads.isEmpty ());
  }

  /** 
   * Safe thread creation.
   * 
   * Will remain blocked here if we can never create a new thread, 
   * but this isn't that big a deal.
   *
   * Normally won't have to try more than once to create a thread.
   * @param w work to do on thread
   */
  protected WorkThread createWorkThread (Work w) {
    WorkThread wt = null;
	
    while (wt == null) {
      try {
	wt =new WorkThread(Integer.toString(threadsCreated++),w);

	if (wt == null) {
	  // wait, hoping we'll get one of the threads next time
	  logger.logMessage(Logger.IMPORTANT,Logger.GENERIC,
			    "WorkQueue could not create a thread, waiting and trying again. "+
			    "(VM has temporarily run out of threads.)");
	  threadsCreated--;
	  Thread.sleep (1000);
	  // try again!
	}

	if (logger.isMinorEnabled())
	  logger.logMessage(Logger.MINOR,Logger.GENERIC,
			    Thread.currentThread () + 
			    " - WorkQueue.createWorkThread - create new thread " +
			    wt);
		
      } catch (InterruptedException e) {e.printStackTrace();}
    }

    return wt;
  }

  /**haults current work -- but enque still accepts new work...**/
  public synchronized boolean haltAllWork(){
    Iterator iter = activeThreads.iterator();
    while(iter.hasNext()){
      WorkThread wt = (WorkThread)iter.next();
      wt.haltWork();
    }
    return true;
  }
  
  public synchronized boolean haltWork(Work w){
    return haltWork(w.getID());
  }

  public synchronized boolean haltWork(int id){
    Iterator iter = activeThreads.iterator();
    while(iter.hasNext()){
      WorkThread wt = (WorkThread)iter.next();
      if(wt.getWorkID()==id){
	wt.haltWork();
	logger.logMessage(Logger.IMPORTANT,Logger.STATE_CHANGE,
			  "Halted formerly active work: "+id);
	return true;
      }
    }
    ListIterator listIter=workQ.listIterator();
    while(listIter.hasNext()){
      Work w=(Work)listIter.next();
      if(w.getID()==id){
		w.halt();
		listIter.remove();
		logger.logMessage(Logger.IMPORTANT,Logger.STATE_CHANGE,
						  "Halted and dequeued formerly pending work: "+id);
      }
    }
    return false;
  }

  public synchronized int numActiveThreads(){
    return activeThreads.size();
  }

  public synchronized int numInactiveThreads(){
    return inactiveThreads.size();
  }

  public synchronized int numThreads(){
    return activeThreads.size()+inactiveThreads.size();
  }

  //Internal

  protected synchronized boolean isWork(){
    return workQ.size()>0;
  }

  protected synchronized Work dequeWork(){
    if(workQ.size()>0){
      return (Work)workQ.removeFirst();
    }
    return null;
  }

  protected synchronized void addActiveThread(WorkThread wt){
    activeThreads.addLast(wt);
  }

  protected synchronized void addInactiveThread(WorkThread wt){
    inactiveThreads.addLast(wt);
  }

  protected synchronized void moveThreadToInactive(WorkThread wt){
    activeThreads.remove(wt);
    inactiveThreads.addLast(wt);
  }
  
  protected synchronized void moveThreadToActive(WorkThread wt){
    if(inactiveThreads.contains(wt)){
      inactiveThreads.remove(wt);
      activeThreads.addLast(wt);
    }
  }

  protected synchronized void removeThread(WorkThread wt){
    inactiveThreads.remove(wt);
  }

  protected void handleResult(Result r){
    resultHandler.handleResult(r);
  }

  private WorkQueue getWorkQ(){
    return this;
  }

  //InnerClasses:
  ///////////////

  public class WorkThread extends Thread{

    //Constants:
    ////////////

    public int myKeepAlive=keepAlive;

    //Variables:
    ////////////

    private Work work;
    private boolean haltThread=false;
    private boolean wokenUp=false;

    /** this is so we don't get into a race between the thread that is returning
     * from perform and the thread calling timeOut with timedWork -
     * see TimedWorkQueue.haltTimedOutWork 
     */
    protected Object workLock = new Object ();
    protected boolean timedOut = false;

    //Constructors:
    ///////////////

    public WorkThread(String name){
      super(name);
    }

    public WorkThread(String name, Work w){
      super(name);
      work=w;
      awaken();
    }


    //Members:
    //////////

    public void haltThread(){
      logger.logMessage(Logger.NORMAL, Logger.STATE_CHANGE,
			"Thread("+getName()+") halting.");
      haltThread=true;
      interrupt();
    }

    public void haltWork(){
      if(work!=null){
	if (logger.isMinorEnabled())
	  logger.logMessage(Logger.MINOR, Logger.STATE_CHANGE,
			    "Thread("+getName()+") " + 
			    "halting activity on work: "+work.getID());
	work.halt();
      }
    }

    /** Get the current work id**/
    public int getWorkID(){
      if(work==null)
		return NO_CURRENT_WORK;
      return work.getID();
    }

    /** Get current status **/
    public String getWorkStatus(){
      if(work==null) {
	return "";
    } else {
        return work.getStatus();
    }
    } 

    public Work getCurrentWork(){
	  return work;
	}

    public Object getWorkLock () { return workLock; }

    public synchronized void awaken(){
      getWork();
      wokenUp=true;
      this.notify();
    }

    protected synchronized void getWork(){
      synchronized(getWorkQ()){
	if(work==null)
	  work=dequeWork();
	if(work!=null)
	  moveThreadToActive(this);
      }
    }

    protected void workDone(Result r){
      if(r != null)
	handleResult(r);
      if(r!=null){
	String workID = (work != null) ? ("" + work.getID()) : "";
	if (logger.isTrivialEnabled())
	  logger.logMessage(Logger.TRIVIAL, Logger.STATE_CHANGE,
			    "Thread("+getName()+") Work("+workID+
			    ") done");
      }else
	logger.logMessage(Logger.ERROR, Logger.STATE_CHANGE,
			  "Thread("+getName()+") Work("+work.getID()+
			  ") produced null result");
      work=null;
      moveThreadToInactive(this);
    }

    /** just like work done, except the thread is removed  */
    protected void timeOut (Result r) {
      if(r!=null)
	handleResult(r);
      if(r!=null){
	String workID = (work != null) ? ("" + work.getID()) : "";
	if (logger.isMinorEnabled())
	  logger.logMessage(Logger.MINOR, Logger.STATE_CHANGE,
			    "Thread("+getName()+") Work("+workID+
			    ") done because timed out.");
      }else
	logger.logMessage(Logger.ERROR, Logger.STATE_CHANGE,
			  "Thread("+getName()+") Work("+work.getID()+
			  ") produced null result after time out.");
      work = null;
      timedOut = true;
      removeThread(this);
    }

    public void run(){
      if (logger.isMinorEnabled())
	logger.logMessage(Logger.MINOR, Logger.STATE_CHANGE,
			  "Thread("+getName()+") starting");

      while(!haltThread){
	getWork();
	if(work!=null){
	  Result r=null;
	  try{
	    r = work.perform(logger);
	  }catch(Exception e){
	    logger.logMessage(Logger.ERROR, Logger.GENERIC,
			      "Unhandled exception while performing work",e);
	    e.printStackTrace();
	  }
	  // other holder could be TimedWorkQueue.haltTimedOutWork
	  synchronized(workLock) {
	    // if work timed out, don't call work done 
	    if (!timedOut) {
	      workDone(r);
	    } else {
	      if (logger.isMinorEnabled())
		logger.logMessage(Logger.MINOR, Logger.STATE_CHANGE,
				  "Thread " + Thread.currentThread () + 
				  " timed out, so dying.");
	      return; // thread timed out!
	    }
	  }
	}
	if(!isWork() && !timedOut){
	  synchronized(this){
	    try{
	      this.wait(myKeepAlive);
	    }catch(InterruptedException ie){
	      if (logger.isTrivialEnabled())
		logger.logMessage(Logger.TRIVIAL, Logger.STATE_CHANGE,
				  "Thread interrupted");
	      removeThread(this);
	      haltThread=true;
	      break;
	    }
	    if(wokenUp)
	      wokenUp=false;
	    else{
	      synchronized(getWorkQ()){
		if(!isWork()&&work==null){
		  removeThread(this);
		  haltThread=true;
		  break;
		}
	      }
	    }
	  }
	}
      }
      if(work != null){
	logger.logMessage(Logger.WARNING, Logger.STATE_CHANGE,
			  "Ending thread that still has a work object");
      }
      work=null;

      if (logger.isMinorEnabled())
	logger.logMessage(Logger.MINOR,Logger.STATE_CHANGE,
			  "Thread("+getName()+") dying");
    }
  }
}
