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
import org.cougaar.mlm.ui.grabber.logger.StdLogger;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;

/**
 * A ResultHandler that queues up results until they can be
 * dealt with.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 02/01/01
 **/
public class ResultQueue implements ResultHandler{

  //Constants:
  ////////////

  //Variables:
  ////////////

  protected Logger logger;
  /**Compromise, as sometimes we might want by id, othertimes like a Q**/
  protected SortedMap resultQ;

  protected Object notifyObject;

  //Constructors:
  ///////////////

  public ResultQueue(){
    logger=makeLogger();
    notifyObject=this;
    resultQ=new TreeMap();
  }

  /** makes it easier to subclass */
  public Logger makeLogger () { return new StdLogger (); }

  public ResultQueue(Logger l){
    logger=l;
    notifyObject=this;
    resultQ=new TreeMap();
  }

  public void setNotifyObject(Object o){
    notifyObject=o;
  }

  //Members:
  //////////

  public synchronized void setLogger(Logger l){
    logger=l;
  }

  public synchronized void handleResult(Result r){
    resultQ.put(new Integer(r.getID()),r);
    if(notifyObject!=null){
      synchronized(notifyObject){
	notifyObject.notify();
      }
    }
  }

  public synchronized boolean hasResult(){
    return resultQ.size()>0;
  }

  public synchronized Result getResult(){
    if(resultQ.size()>0)
      return (Result)resultQ.get(resultQ.firstKey());
    return null;
  }

  public synchronized Result getResult(int id){
    return (Result)resultQ.get(new Integer(id));
  }

  //InnerClasses:
  ///////////////
}


