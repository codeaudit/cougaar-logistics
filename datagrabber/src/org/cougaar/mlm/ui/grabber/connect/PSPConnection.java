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

import org.cougaar.planning.servlet.data.xml.DeXMLizer;
import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.planning.servlet.data.Failure;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Work;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.controller.FailureRunResult;
import org.cougaar.mlm.ui.grabber.controller.RunResult;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

import java.sql.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

/**
 * Basic infrastructure to talk to a PSP that uses the 
 * XMLable/serializable interfaces
 * 
 *
 * @since 2/01/01
 **/
public abstract class PSPConnection extends PSPWork{

  //Constants:
  ////////////

  protected int[] BACKOFF_TIMES={250,1000,5000};
  
  //Variables:
  ////////////

  protected URLConnectionData urlConnectData;
  
  //Constructors:
  ///////////////

  public PSPConnection(int id, int runID, 
		       URLConnectionData ucd, 
		       DBConfig dbConfig,
		       Connection dbConnection,
		       Logger logger){
    super(id,runID,dbConfig,dbConnection,logger);
    this.urlConnectData=ucd;
  }

  //Members:
  //////////

  //Abstract members:

  /**update the database table based on the recieved object**/
  protected abstract void updateDB(Connection c, DeXMLable obj);

  /**Prepare the result object based on the recieved object**/
  protected abstract RunResult prepResult(DeXMLable obj);

  /**return the DeXMLableFactory specific to this URL connection**/
  protected abstract DeXMLableFactory getDeXMLableFactory();

  //Gets:


  protected String getClusterName(){
    return urlConnectData.getClusterName();
  }

  public String getName(){
    return super.getName () + " for " + getClusterName();
  }

  /**Determine if we should use XML**/
  protected boolean transferByXML(){
    return urlConnectData.transferByXML();
  }

  /**Determine if we should use PSP format or servlet format for URLs**/
  protected boolean usingPSPs(){
    return urlConnectData.usingPSPs();
  }
  
  /**Determine if we are loading from a file**/
  protected boolean sourceIsFile(){
    return urlConnectData.sourceIsFile();
  }

  protected String getQueryString(){
    return "";
  }

  protected String getFileName(){
    return urlConnectData.getFileName();
  }

  public String getURL(){
    String baseURL=urlConnectData.getURL();
    String queryString = getQueryString();
    if (!usingPSPs ())
      queryString = queryString.replace ('?', '&');

    return baseURL+(transferByXML()?"?format=xml":"?format=data")+
      queryString;
  }

  /**Null if there is no post data.  
   * Expect that this function MAY be called more than once if there
   * is an IO Error in writing the first time.
   **/
  protected InputStream getPostData(){
    return null;
  }

  //Sets:

  //Overrides for increased funcationality:

  /**helper that halts, sets status and logs a message at once**/
  public void haltForError(int type, String status){
    logMessage(Logger.ERROR,type,getClusterName()+":"+status);
    haltForError(status);
  }

  /**helper that halts, sets status and logs a message at once**/
  public void haltForError(int type, String status, Exception e){
    logMessage(Logger.ERROR,type,getClusterName()+":"+status,e);
    haltForError(status);
  }

  //Actions:

  //Steps in perform()

  protected InputStream connectFile(String file){
    if (logger.isMinorEnabled())
      logMessage(Logger.MINOR,Logger.FILE_IO,"Connecting to file: " + file);

    File f = new File(file);
    InputStream inStr=null;
    try{
      inStr = new FileInputStream(f);
    }catch(FileNotFoundException e){
      haltForError(Logger.FILE_IO,"File not found ("+file+")");
      return null;
    }
    return inStr;
  }

  /**connect to the URL and post any data returned by getPostData()**/
  protected InputStream connectURL(String urlStr)
    throws UnableToConnectException{
    if (logger.isMinorEnabled())
      logMessage(Logger.MINOR,Logger.NET_IO,"Connecting to URL: " + urlStr);

    URL url=null;
    InputStream inStream=null;
    OutputStream outStream=null;
    try{
      url = new URL(urlStr);
    }catch(MalformedURLException e){
      haltForError(Logger.NET_IO,"Malformed URL: "+urlStr,e);
      return null;
    }
    URLConnection uc;
    try{
      uc = url.openConnection();
    }catch(IOException e){
      throw new UnableToConnectException("Unable to connect to URL: "+url,e);
    } 
    InputStream pd = getPostData();
    if(pd!=null){
      try{
	outStream = new BufferedOutputStream(uc.getOutputStream());
      }catch(IOException e){
	throw new UnableToConnectException
	  ("Unable to open output stream (post) to URL",e);
      }
      pd= new BufferedInputStream(pd);
      setEpoch(POST_QUERY);
      try{
	byte buf[]=new byte[1024];
	while(true){//Expect an EOF eventually...
	  int size =pd.read(buf);
	  outStream.write(buf,0,size);
	}
      }catch(EOFException e){e.printStackTrace();
	//Expected.
      }catch(IOException e){e.printStackTrace();
	throw new UnableToConnectException("Unable to post data to URL",e);
      }finally{
	if(outStream!=null)
	  try{
	    outStream.close();
	  }catch(IOException e){e.printStackTrace();}
      }
    }
    try{
      inStream = uc.getInputStream();
    }catch(IOException e){
      throw new UnableToConnectException
	("Unable to get input stream from URL",e);
    }
    return inStream;
  }

  /**Get the object from the psp**/
  protected DeXMLable stream(InputStream s) throws UnableToStreamException{
    InputStream inStream = new BufferedInputStream(s);
    DeXMLable ret=null;
    setStatus("Starting reading object");
    if(transferByXML()){
      try{
	DeXMLizer dXML = new LoggingDeXMLizer(getDeXMLableFactory());
	ret = dXML.parseObject(inStream);
      }catch(Exception e){
	throw new UnableToStreamException("Error reading XML object",e);
      }
    }else{
      try{
	//	ByteArrayInputStream bytes = new ByteArrayInputStream (s);
	//	BufferedInputStream buffered = new BufferedInputStream (bytes);
	ObjectInputStream objInStream = new ObjectInputStream(inStream);
	long millis = System.currentTimeMillis ();
	ret = (DeXMLable)objInStream.readObject();
	logMessage (Logger.MINOR, Logger.NET_IO, "Took " + (System.currentTimeMillis ()-millis) + " to read object");
      }catch(Exception e){
	e.printStackTrace();
	throw new UnableToStreamException("Error reading serialized object",e);
      }
    }
    setStatus("Done reading object");
    if (logger.isMinorEnabled())
      logMessage(Logger.MINOR,Logger.NET_IO,
		 "Object Read");
    return ret;
  }

  /**
   * Do the operation and return a hint for preparing the result<BR>
   *
   * Note that in this code we are explicitly testing for a halt condition
   * rather than using exception processing so that some other thread
   * could halt us.  We must leave state consistent after a halt is called
   * and return null as quickly as possible.
   **/
  protected Object perform(){
    DeXMLable obj=null;
    //We are going to back off and try again on thrown exceptions until
    //we run out of attempts:
    int backoff=0;
    boolean success=false;
    boolean giveup=false;
    while(!success && !giveup){
      try{
	setEpoch(CONNECTING);
	InputStream inStr=null;
	if(sourceIsFile()){
	  inStr=connectFile(getFileName());
	}
	else{
	  inStr=connectURL(getURL());
	}
	if(halt){
	  if(inStr!=null)
	    try{
	      inStr.close();
	    }catch(IOException e){e.printStackTrace();}
	  return null;
	}    
	setEpoch(STREAMING);
	obj = stream(inStr);
	if(inStr!=null)
	  try{
	    inStr.close();
	  }catch(IOException e){e.printStackTrace();}
	success=true;
	if(backoff>0){
	  logMessage(Logger.WARNING,Logger.NET_IO,
		     "Successfully read from PSP on the "+
		     (backoff+1) + " try.");
	}
      }catch(PSPException e){
	if(backoff<BACKOFF_TIMES.length){
	  try{
	    String message=e.getMessage()+" -- Backing off for "+
	      BACKOFF_TIMES[backoff]+" millis.";
	    logMessage(Logger.WARNING, Logger.NET_IO,
		       message,
		       e.getNestedException());
	    setStatus(message);
	    Thread.sleep(BACKOFF_TIMES[backoff]);
	  }catch(InterruptedException ie){ie.printStackTrace();}
	  backoff++;
	}else{
	  haltForError(Logger.NET_IO, "Giving up after "+
		       (backoff+1)+" tries: "+e.getMessage(), 
		       e.getNestedException());
	  giveup=true;
	}
      }
    }
    if(halt)return null;

    if(obj==null){
      haltForError(Logger.NET_IO,"PSP returned null");
    }
    if(halt)return null;

    //Check to see if the obj is a Failure:
    if(obj instanceof Failure){
      Failure f=(Failure)obj;
      haltForError(Logger.NET_IO,"PSP returned Failure: "+f.toString());
    }
    if(halt)return null;
    
    setEpoch(UPDATINGDB);
    Connection c=getDBConnection();

    logMessage(Logger.NORMAL,Logger.NET_IO,"updating DB with connection " + c);

    updateDB(c,obj);

    logTiming ();

    if(halt)return null;
    
    return obj;
  }

  /** logs at normal level timing info about how long this work took to do */
  protected void logTiming () {
    long endOfWork = System.currentTimeMillis ();

    float total     = (float) (endOfWork      - startURLReadTime);
    float readTime  = (float) (endURLReadTime - startURLReadTime);
    float pct1      = (readTime/total);
    float pct2      = (1-pct1);        

    String p1 = format.format(pct1*100.0f);
    String p2 = format.format(pct2*100.0f);

    if (total > 0)
      logMessage(Logger.NORMAL,Logger.NET_IO,
		 urlConnectData.getClusterName() + 
		 " : Read from URL in " + (endURLReadTime - startURLReadTime) + 
		 " (" + p1 +"%), " +  
		 " stored data in " + (endOfWork - endURLReadTime) + 
		 " (" + p2 +"%) millis.");
  }

  protected NumberFormat format = new DecimalFormat("##");
  public static SimpleDateFormat sqlDate=
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /** records time stamp info on epoch transitions */
  protected void setEpoch(int e){
    super.setEpoch (e);
    
    if (logger.isTrivialEnabled())
      logMessage(Logger.TRIVIAL,Logger.NET_IO, "set Epoch to be " + e);

    if (e == CONNECTING) {
      startURLReadTime = System.currentTimeMillis ();
      if (logger.isMinorEnabled())
	logMessage(Logger.MINOR,Logger.NET_IO, " starting POST_QUERY at " + 
		   sqlDate.format(new Date(startURLReadTime)));
    }
    else if (e == UPDATINGDB) {
      endURLReadTime = System.currentTimeMillis ();
      if (logger.isMinorEnabled())
	logMessage(Logger.MINOR,Logger.NET_IO, " starting UPDATINGDB at " + 
		   sqlDate.format(new Date(endURLReadTime)));
    }
  }

  /**Prepare the result object based on the hint**/
  protected RunResult prepResult(Object hint){
    return prepResult((DeXMLable)hint);
  }

  //InnerClasses:
  ///////////////

  public class LoggingDeXMLizer extends DeXMLizer{
    public LoggingDeXMLizer(DeXMLableFactory dXMLFactory){
      super(dXMLFactory);
    }

    protected void reportMessage(String m){
      logMessage(Logger.ERROR,Logger.PARSE,m);
    }
    
    protected void reportMessage(String m, Exception e){
      logMessage(Logger.ERROR,Logger.PARSE,m,e);
    }    
  }

  public static class PSPException extends Exception{
    private Exception nested;
    public PSPException(String message, Exception nested){
      super(message);
      this.nested=nested;
    }
    public Exception getNestedException(){
      return nested;
    }
  }

  public static class UnableToConnectException extends PSPException{
    public UnableToConnectException(String message, Exception nested){
      super(message,nested);
    }
  }

  public static class UnableToStreamException extends PSPException{
    public UnableToStreamException(String message, Exception nested){
      super(message,nested);
    }
  }

  protected long startURLReadTime, endURLReadTime;
}
