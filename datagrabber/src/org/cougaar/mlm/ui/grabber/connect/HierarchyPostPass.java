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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.config.HierarchyPSPConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.*;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Handles creating the descendents table and forging roots if need be
 *
 * @since 2/01/01
 **/
public class HierarchyPostPass extends PrepareDBTables 
  implements HierarchyConstants{

  //Constants:
  ////////////

  //In HierarchyConstants.java

  //Variables:
  ////////////

  protected Set hConfigs;

  //Constructors:
  ///////////////

  /**pass in all the HierarchyPSPConfigs**/
  public HierarchyPostPass(int id, int runID,
			   Set hConfigs,
			   DBConfig dbConfig,
			   Connection c,
			   Logger l){
    super(id, runID, dbConfig, c,l);
    this.hConfigs=hConfigs;
  }

  //Actions:

  private void includeAuthenticRoot(Statement s, HierarchyPSPConfig hConfig,
				    String rootOrg)
    throws SQLException{
    HierarchyPrepareDBTables.addToRootTable(this,s,getRootTableName(),
					    rootOrg,
					    hConfig.getSociety());
  }

  /**Assumed that require table already exits.**/
  private boolean determineNeedForgeRoot(Statement s, 
					 HierarchyPSPConfig hConfig)
    throws SQLException{
    boolean ret=false;
    if(hConfig.forgeRoot()){
      ResultSet rs=s.executeQuery("SELECT * FROM "+getRootTableName()+
				  " WHERE "+COL_ORGID+"='"+
				  hConfig.getVirtualRoot()+"'");
      ret=!rs.next();
    }
    return ret;
  }
  
  /** just always update**/
  protected boolean needPrepareDB(Connection c){
    return true;
  }

  protected void forgeVirtualRoot(Statement s, HierarchyPSPConfig hConfig)
    throws SQLException{
    HierarchyPrepareDBTables.addToRootTable(this,s,getRootTableName(),
					    hConfig.getVirtualRoot(),
					    hConfig.getSociety());
    HierarchyPrepareDBTables.addToNamesTable(this, s, getNamesTableName(),
					     hConfig.getVirtualRoot(), 
					     hConfig.getVirtualRootName()); 
    //Add to org table:
    Iterator iter=hConfig.getClusterIterator();
    while(iter.hasNext()){
      String cluster=(String)iter.next();
      HierarchyPrepareDBTables.addToOrgTable(this, s, getOrgTableName(),
					     hConfig.getVirtualRoot(), 
					     cluster,ADMIN_SUBORDINATE);
    }
    logMessage(Logger.NORMAL,Logger.STATE_CHANGE,
	       "Forged virtual root "+hConfig.getVirtualRootName());
  }

  private List getDescendents(Statement s, String org)
    throws SQLException{
    ResultSet rs=s.executeQuery("SELECT "+COL_RELID+" FROM "+getOrgTableName()+
				" WHERE "+COL_ORGID+"='"+org+"' AND ("+
				COL_REL+"='"+SUBORDINATE+"' OR "+
				COL_REL+"='"+ADMIN_SUBORDINATE+"')");
    if(!rs.next()){
      return null;
    }
    List ret=new ArrayList(5);
    boolean hasMore=true;
    while(hasMore){
      ret.add(rs.getString(1));
      hasMore=rs.next();
    }
    rs.close();
    return ret;
  }

  /** add to descendtable a pair between everything in the stack,
   * and everything in orgs and below, and recurse below everything in orgs
   **/
  private void insertDescendRowsForNode(Statement s,
					StringStack stack, String cur)
    throws SQLException{
    //firt push myself on the stack:
    stack.push(cur);
    //Add all the pairs for everything on the stack and cur:
    Iterator iter=stack.iterator();
    while(iter.hasNext()){
      String parID=(String)iter.next();
      HierarchyPrepareDBTables.addToDescendTable(this,s,
						 getDescendTableName(),
						 parID,cur);
    }
    //Now try to recurse:
    List subs=getDescendents(s,cur);
    if(subs!=null){
      for(int i=0;i<subs.size();i++){
	String sub=(String)subs.get(i);
	insertDescendRowsForNode(s,stack,sub);
      }
    }   
    //pop the stack.
    stack.pop();
  }
  
  private void updateDescendTable(Statement s, String root)
    throws SQLException{
    StringStack stack=new StringStack();
    insertDescendRowsForNode(s, stack, root);
    logMessage(Logger.MINOR,Logger.DB_WRITE,"Updated table("+
	       getDescendTableName()+") for root: "+root); 
  }

  protected void prepareDB(Connection c){
    Statement s=null;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE,"Could not create/close Statement",e);
      return;
    }

    Iterator iter=hConfigs.iterator();
    while(iter.hasNext()){
      HierarchyPSPConfig hConfig=(HierarchyPSPConfig)iter.next();
      if(hConfig.forgeRoot()){
	try{
	  if(determineNeedForgeRoot(s,hConfig))
	    forgeVirtualRoot(s,hConfig);
	}catch(SQLException e){
	  haltForError(Logger.DB_WRITE,
		       "Could not determine/add virtual root",e);
	  break;
	}
      }else{
	try{
	  includeAuthenticRoot(s,hConfig,hConfig.getRoot());
	}catch(SQLException e){
	  haltForError(Logger.DB_WRITE,
		       "Could not include authentic root",e);
	  break;
	}
      }
      try{
	updateDescendTable(s,hConfig.getRoot());
      }catch(SQLException e){
	haltForError(Logger.DB_WRITE,"Could not updateDescendTable",e);
	break;
      }
    }
    try{
      s.close();
    }catch(SQLException e){
      logMessage(Logger.ERROR,Logger.DB_WRITE,"Could not close Statement",e);
    }
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
  /*
  private static class OrgNode{
    private List subList;
    public OrgNode(){
      subList=new ArrayList();
    }
    public Iterator getSubordinateIDIterator(){
      return subList.iterator();
    }
    public void addSubordinateID(String id){
      subList.add(id);
    }
  }

  protected static class OrgTree{
    private String root;
    private Map orgToNode;

    public OrgTree(){
      orgToNode=new HashMap(31);
    }

    public void setRoot(String r){
      root=r;
    }

    public String getRoot(){
      return root;
    }

    public Iterator getSubordinateIDIterator(String id){
      OrgNode o=getOrg(id);
      if(o==null)return null;
      return getOrg(id).getSubordinateIDIterator();
    }

    public void setHierarchy(Statement s, String orgTableName){
      orgToNode.clear();
      //For each Org:
      for(int i=0;i<hd.numOrgs();i++){
	Organization o=hd.getOrgDataAt(i);
	String uid=o.getUID();
	OrgNode on=new OrgNode();
	for(int j=0;j<o.getNumRelations();j++){
	  int rel=o.getRelationAt(j);
	  if(rel==Organization.SUBORDINATE ||
	     rel==Organization.ADMIN_SUBORDINATE){
	    on.addSubordinateID(o.getRelationUIDAt(j));
	  }
	}
	addOrgNode(uid,on);
      }
    }

    protected void addOrgNode(String id, OrgNode on){
      orgToNode.put(id,on);
    }

    protected OrgNode getOrg(String id){
      return (OrgNode)orgToNode.get(id);
    }
  }

  private void insertDescendRowsForNode(Statement s,
					OrgTree ot, 
					StringStack stack,
					String id)
    throws SQLException{
    stack.push(id);
    //Now, add the current node to everything in the stack:
    Iterator iter=stack.iterator();
    while(iter.hasNext()){
      String parID=(String)iter.next();
      HierarchyPrepareDBTables.addToDescendTable(this,s,getDescendTableName(),
						 parID,id);
    }
    iter = ot.getSubordinateIDIterator(id);
    if(iter==null){
      logMessage(Logger.WARNING,Logger.DATA_CONSISTENCY,
		 "Did not recive expected hierarchy node("+id+")");
    }else{
      while(iter.hasNext()){
	insertDescendRowsForNode(s,ot,stack,(String)iter.next());
      }
    }
    stack.pop();
  }
  */

  public static class StringStack{
    private List l;
    public StringStack(){
      l=new ArrayList();
    }
    public void push(String s){
      l.add(s);
    }
    public String pop(){
      if(l.size()>0){
	return (String) l.remove(l.size()-1);
      }
      return null;
    }
    public Iterator iterator(){
      return l.iterator();
    }
  }
}
