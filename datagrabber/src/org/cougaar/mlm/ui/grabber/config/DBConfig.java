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
package org.cougaar.mlm.ui.grabber.config;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.xml.sax.Attributes;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.*;

/**
 * Parameters for comunication with the database
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/05/01
 **/
public class DBConfig implements XMLable, DeXMLable{

  //Constants:
  ////////////

  public static final String NAME_TAG = "DBConfig";
  public static final String USER_TAG = "User";
  public static final String PASS_TAG = "Password";
  public static final String DRIVER_TAG = "DriverClass";
  public static final String URL_TAG = "ConnectionURL";
  public static final String SYNTAX_TAG = "Syntax";

  public static final int ORACLE=0;
  public static final int MYSQL=1;

  public static final String[] DBTYPES={"Oracle",
					"MySQL"};

  public static SimpleDateFormat sqlDate=
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  //Variables:
  ////////////

  protected String user;
  protected String password;

  protected String driverClassName;
  protected String connectionURL;

  protected int syntax=MYSQL;

  //Constructors:
  ///////////////

  public DBConfig(){
    CougaarRCParams params = new CougaarRCParams ();

    /** fill in values from the cougaar rc file */
    String value = null;
    if ((value = params.getParam ("driver.mysql")) != null)
      driverClassName = value;

    /** Expected to be like : jdbc:mysql://localhost/grabber **/
      if ((value = params.getParam ("org.cougaar.mlm.ui.grabber.config.DBConfig.connectionURL")) != null)
	connectionURL = value;
      if ((value = params.getParam ("org.cougaar.mlm.ui.grabber.config.DBConfig.user")) != null)
	user = value;
      if ((value = params.getParam ("org.cougaar.mlm.ui.grabber.config.DBConfig.password")) != null)
	password = value;
      if ((value = params.getParam ("org.cougaar.mlm.ui.grabber.config.DBConfig.syntax")) != null)
	syntax=stringToType(value);

      if (debug) {
	System.out.println ("DBConfig settings : ");
	try{
	  XMLWriter w=new XMLWriter(new OutputStreamWriter(System.out));
	  toXML(w);
	  w.flush();
	}catch(Exception e){e.printStackTrace();}
      }
  }

  public DBConfig(String driverClassName,
		  String connectionURL,
		  String user,
		  String password,
		  int syntax){
    this.driverClassName=driverClassName;
    this.connectionURL=connectionURL;
    this.user=user;
    this.password=password;
    this.syntax=syntax;
  }

  //Members:
  //////////

  public String getDriverClassName(){
    return driverClassName;
  }

  public String getConnectionURL(){
    return connectionURL;
  }

  public String getUser(){
    return user;
  }

  public String getPassword(){
    return password;
  }

  public String getHostName(){
    if(connectionURL==null || connectionURL.equals(""))
      return "";
    int idx=connectionURL.indexOf("//");
    if(idx==-1)
      return "";
    String looseProto=connectionURL.substring(idx+2);
    idx=looseProto.indexOf("/");
    String hostOnly=looseProto.substring(0,idx);
    return hostOnly;
  }


  public String getDatabaseName(){
    if(connectionURL==null || connectionURL.equals(""))
      return "";
    int idx=connectionURL.lastIndexOf("/");
    if(idx==-1)
      return "";
    return connectionURL.substring(idx+1);
  }

  public String getConnectionURLNoDatabase () {
    if(connectionURL==null || connectionURL.equals(""))
      return "";
    int idx=connectionURL.lastIndexOf("/");
    if(idx==-1)
      return "";
    return connectionURL.substring(0,idx+1);
  }
  
  //Functions for dealing with database specific SQL:
  ///////////////////////////////////////////////////

  public int createTableSelect(Statement s, String newTableName, String sql)
    throws SQLException{
    switch(syntax){
    case MYSQL:
      return s.executeUpdate("CREATE TABLE "+newTableName+" "+sql);	     
    default:
    case ORACLE:
      System.err.println("DBConfig.createTableSelect not yet implemented "+
			 "for Oracle -- should be easy though");
      return -1;
    }
  }

  public int selectIntoTable(Statement s, String destTable, String destColumns, String selectSql)
    throws SQLException{
    switch(syntax){
    case MYSQL:
      return s.executeUpdate("INSERT INTO "+destTable+" ("+destColumns+") "+selectSql);
    default:
    case ORACLE:
      System.err.println("DBConfig.selectIntoTable not yet implemented "+
			 "for Oracle -- should be easy though");
      return -1;
    }
  }

  public String getUniqueViolatedErrorCode(){
    return "S1009";
  }

  public String getDBDouble(double d){
    String str=Double.toString(d);
    int idx=str.indexOf('E');
    if(idx==-1)
      return str;
    else{
      if(str.charAt(idx+1)=='-')
	return str;
      else{
	return str.substring(0,idx+1)+"+"+str.substring(idx+1);
      }      
    }      
  }

  public String getDBTableName(String name){
    switch(syntax){
    default:
    case ORACLE:
      return name.toUpperCase();
    case MYSQL:
      return name.toLowerCase();
    }
  }

  public String getDateTimeType(){
    switch(syntax){
    default:
    case ORACLE:
      return "DATE";
    case MYSQL:
      return "DATETIME";
    }
  }

  public String dateToSQL(long time){
    StringBuffer sb= new StringBuffer();
    switch(syntax){
    default:
    case ORACLE:
      sb.append("TO_DATE('");
      synchronized(sqlDate){
	sb.append(sqlDate.format(new Date(time)));
      }
      sb.append("','YYYY-MM-DD HH24:MI:SS')");
    case MYSQL:
      sb.append('\'');
      synchronized(sqlDate){
	sb.append(sqlDate.format(new Date(time)));
      }
      sb.append('\'');
    }
    return sb.toString();
  }

  public String getSyntaxString(){
    return DBTYPES[syntax];
  }

  public int getSyntax () {
	return syntax;
  }
  
  //Statics:

  public static String typeToString(int type){
    return DBTYPES[type];
  }
  
  public static int stringToType(String str){
    for(int i=0;i<DBTYPES.length;i++){
      if(str.equals(DBTYPES[i]))
	return i;
    }
    System.err.println("Unknown database type: "+str);
    return MYSQL;
  }


  //XMLable:

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);
    w.tagln(USER_TAG,user);
    w.tagln(PASS_TAG,password);
    w.tagln(DRIVER_TAG,driverClassName);
    w.tagln(URL_TAG,connectionURL);
    w.tagln(SYNTAX_TAG,typeToString(syntax));
    w.cltagln(NAME_TAG);
  }

  //DeXMLable:

  /**
   * Report a startElement that pertains to THIS object, not any
   * sub objects.  Call also provides the elements Attributes and data.  
   * Note, that  unlike in a SAX parser, data is guaranteed to contain 
   * ALL of this tag's data, not just a 'chunk' of it.
   * @param name startElement tag
   * @param attr attributes for this tag
   * @param data data for this tag
   **/
  public void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException{
    try{
      if(name.equals(NAME_TAG)){
      }else if(name.equals(USER_TAG)){
	user=data;
      }else if(name.equals(PASS_TAG)){
	password=data;
      }else if(name.equals(DRIVER_TAG)){
	driverClassName=data;
      }else if(name.equals(URL_TAG)){
	connectionURL=data;
      }else if(name.equals(SYNTAX_TAG)){
	syntax=stringToType(data);
      }else
	throw new UnexpectedXMLException("Unexpected open tag:"+name);
    }catch(NumberFormatException e){
      throw new UnexpectedXMLException("Could not parse as number("+
				       data+")"+e);
    }
  }

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being deXMLized
   **/
  public boolean closeTag(String name)
    throws UnexpectedXMLException{
    return name.equals(NAME_TAG);
  }

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException{
    throw new UnexpectedXMLException("Unexpected subobject:"+obj);
  }

  boolean debug = "true".equals (System.getProperty("org.cougaar.mlm.ui.grabber.config.DBConfig.debug","false"));

  //InnerClasses:
  ///////////////
}
