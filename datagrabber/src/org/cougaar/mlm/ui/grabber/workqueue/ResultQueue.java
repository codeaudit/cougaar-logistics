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
package org.cougaar.mlm.ui.grabber.workqueue;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;

/**
 * A ResultHandler that queues up results until they can be
 * dealt with.
 * @author Benjamin Lubin; last modified by: $Author: mthome $
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


