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
 * @author Benjamin Lubin; last modified by: $Author: mthome $
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
