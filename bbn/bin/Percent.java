/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
import java.util.*;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.*;

public class Percent {
  public static void main (String []arg) {
    if (arg.length < 5) {
      System.out.println ("Usage : Percent mysql_host mysql_database user password runid\n");
      return;
    }
    String host = arg[0];
    String database = arg[1];
    String user = arg[2];
    String password = arg[3];
    String runid = arg[4];
    boolean debug = false;

    try {
      try {
	Class.forName("org.gjt.mm.mysql.Driver");
      } catch (Exception e) { System.err.println("Got exception " + e);}
      Connection conn = DriverManager.getConnection("jdbc:mysql://"+host+"/" + database, 
						    user, password);
      Statement s;
      PreparedStatement ps;
      try{
	s = conn.createStatement();
      }catch(SQLException e){
	System.err.println("Could not create Statement "+e);
	return;
      }
      ResultSet prototypes = s.executeQuery("select prototypeid from conveyanceprototype_" + runid+
					    "\nwhere prototypeid not like 'NSN%' and not 'NullTypeId'");
      List protos = new ArrayList();
      while(prototypes.next()){
	protos.add (prototypes.getString(1));
      }
      prototypes.close();
    
      String sqlTemplate = "select cl.convid, cl.starttime, ccd.weight,\n"+
	"ccd.area, cp.weightcapacity, cp.areacapacity\n" + 
	"from assetinstance_221 a,\n"+
	"assetprototype_221 p,\n" + 
	"conveyanceprototype_221 cp,\n" + 
	"conveyanceinstance_221 ci,\n" +
	"conveyedleg_221 cl,\n" + 
	"assetitinerary_221 ai,\n" +
	"cargocatcodedim_221 ccd\n"+
	"where\n"+
	"a.assetid = ai.assetid and\n"+
	"ai.legid  = cl.legid and\n"+
	"cl.convid = ci.convid and\n"+
	"ci.prototypeid = cp.prototypeid and\n"+
	"cp.alptypeid   = ? and\n"+
	"a.prototypeid  = p.prototypeid and\n"+
	"p.prototypeid  = ccd.prototypeid \n"+
	"order by cl.convid, cl.starttime;";

      if (debug)
	System.out.println ("template sql was " + sqlTemplate);
    
      System.out.println ("Type \t\t# missions\tAvg. % of wt cap\tAvg. % of area cap\n");

      for (Iterator iter = protos.iterator(); iter.hasNext(); ) {
	String proto = (String) iter.next();
	// walk over result set prototypes
	// run prepared statement for each
	String sql = sqlTemplate.replaceAll ("221",runid);
	if (debug)
	  System.out.println ("sql was " + sql);
	ps = conn.prepareStatement (sql);
	ps.setString(1, proto);

	ResultSet rs = ps.executeQuery ();
	String last = null;
	Date lastDate = null;
	float wtTotal = 0.0f, areaTotal = 0.0f, areaAvgTotal = 0.0f, wtAvgTotal = 0.0f;
	float totalMissions = 0;
	float  wt_cap = 0.0f;
	float  area_cap = 0.0f;
	String convid = null;
	Date start = null;
	while(rs.next()) {
	  convid = rs.getString(1);
	  start  = rs.getTimestamp(2);
	  float  wt  = rs.getFloat(3);
	  float  area  = rs.getFloat(4);
	  wt_cap = rs.getFloat(5);
	  area_cap = rs.getFloat(6);

	  if ((last     != null && !convid.equals(last)) || 
	      (lastDate != null && !lastDate.equals(start))) {
	    last = convid;
	    lastDate = start;
	    totalMissions++;

	    wtAvgTotal   += wtTotal/wt_cap;
	    areaAvgTotal += areaTotal/area_cap;
	    
	    if (debug)
	      System.out.println (convid + " - " + start + " - wt % " + wtTotal/wt_cap + "(cap = " + wt_cap +
				  ") area % " + areaTotal/area_cap + " (cap = " + area_cap + ")");

	    wtTotal   = 0.0f;
	    areaTotal = 0.0f;
	  }
	  wtTotal   += wt;
	  areaTotal += area;

	  if (debug)
	    System.out.println (convid + " - " + start + " - wt " + wtTotal + " area " + areaTotal);

	  if (last == null) {
	    last = convid;
	    lastDate = start;
	  }
	  //	  System.out.println (proto + " - " + rs.getFloat(1)*100.0f + "%");
	}
	//	totalMissions++;

	wtAvgTotal   += wtTotal/wt_cap;
	areaAvgTotal += areaTotal/area_cap;

	if (debug)
	  System.out.println (convid + " - " + start + " - wt % " + wtTotal/wt_cap + "(cap = " + wt_cap +
			      ") area % " + areaTotal/area_cap + " (cap = " + area_cap + ")");

	if (totalMissions>0)
	  System.out.println (proto + (proto.length() > 7 ? "\t" : "\t\t") + ((int)totalMissions) + 
			      " missions - wt % " + wtAvgTotal/totalMissions + 
			      "\tarea % " + areaAvgTotal/totalMissions);
	rs.close();
      }
    
      s.close();
      conn.close();
    }catch(SQLException e){
      System.err.println("Got sql exception "+e);
      return;
    }
  }
}
