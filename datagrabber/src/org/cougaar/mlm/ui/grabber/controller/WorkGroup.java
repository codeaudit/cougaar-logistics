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
package org.cougaar.mlm.ui.grabber.controller;

import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.workqueue.ResultQueue;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Represents a collection of workIDs and has functions for
 * manipulating them.
 *
 * @since 2/17/01
 **/
public class WorkGroup{

  //Variables:
  ////////////
  
  private Map pendingWork;//maps work ids to a descriptive string
  private ResultQueue resultQ;
  private WorkQueue workQ;
  
  //Constructors:
  ///////////////
  
  public WorkGroup(WorkQueue workQ, ResultQueue resultQ){
    pendingWork=new HashMap(31);
    this.workQ=workQ;
    this.resultQ=resultQ;
  }
  
  //Members:
  //////////
  
  public void clear(){
    pendingWork.clear();
  }
  
  public void add(int workID){
    pendingWork.put(new Integer(workID),"");
  }
  
  public void add(int workID,String desc){
    pendingWork.put(new Integer(workID),desc);
  }
  
  public void remove(int workID){
    pendingWork.remove(new Integer(workID));
  }
  
  public String reportPendingWork () {
    return pendingWork.toString ();
  }

  /**check to see if any of the group's results are ready and
   * return one if they are.
   **/
  public Result getResult(){
    Iterator iter=pendingWork.keySet().iterator();
    while(iter.hasNext()){
      Integer workIDInt=(Integer)iter.next();
      int workID=(workIDInt).intValue();
      Result r=resultQ.getResult(workID);
      if(r!=null){
	return r;
      }
    }
    return null;
  }
  
  public String getDesc(int workID){
    String desc=(String)pendingWork.get(new Integer(workID));
    if(desc==null)
      return "";
    return desc;
  }
  
  public int size(){
    return pendingWork.size();
  }
  
  public boolean isEmpty(){
    return pendingWork.size()==0;
  } 
  
  public int haltAllInGroup(){
    int haltNum=0;
    Iterator iter=pendingWork.keySet().iterator();
    while(iter.hasNext()){
      Integer workIDInt=(Integer)iter.next();
      int workID=workIDInt.intValue();
      workQ.haltWork(workID);
      haltNum++;
    }
    return haltNum;
  }
}
