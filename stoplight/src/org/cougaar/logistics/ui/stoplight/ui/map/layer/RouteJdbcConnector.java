/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;
 
import java.sql.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.proj.Projection;

// datagrabber module (not OSS) is the true source for some constants
// Instead, these are now copied in the local AssetTypeConstants file
//import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.OMWideLine;


/***********************************************************************************************************************
<b>Description</b>: AssetJdbcPlugin is a plugin which connects to the payment asset database
                                     to update the customer info.

<br><br><b>Notes</b>:<br>
									- This plugIn is attached to the payment cluster.

***********************************************************************************************************************/
public class RouteJdbcConnector 
{
   
   public static boolean connectionFailed = false;
   public static final String DBRUNID = "_15";
   private static Hashtable rtsByUID = null;

  // ---------------------------------------------------------------------------------------------------------------------
  // Public Member Methods
  // ---------------------------------------------------------------------------------------------------------------------



  /*********************************************************************************************************************
  <b>Description</b>: createAssets

  <br><b>Notes</b>:<br>

  @param owner String organization name
  @return Vector routes                  -

  <br>

	*********************************************************************************************************************/
  public static RoutingTable findRoutes ( String owner, int conveyType  )
	{

 		if(connectionFailed && rtsByUID == null)
		  return null;

    if (rtsByUID == null)
    {
      System.out.println ("Result set empty, querying database");
      queryDB();
    }

    Vector bothRTs = (Vector) rtsByUID.get(owner);

    if (bothRTs == null)
      return null;       // there's nothing for this unit

    //    if (conveyType == DGPSPConstants.CONV_TYPE_SHIP)
    if (conveyType == AssetTypeConstants.ASSET_TYPE_SHIP)
    {
      return (RoutingTable) bothRTs.get(0);
    }
    //    else if (conveyType == DGPSPConstants.CONV_TYPE_PLANE)
    else if (conveyType == AssetTypeConstants.ASSET_TYPE_PLANE)
    {
      return (RoutingTable) bothRTs.get(1);
    }
    else
      return null;

  }

  public static void queryDB ()
	{
    java.sql.Connection conn = null;
    rtsByUID = new Hashtable();

    try
    {
       Driver myDriver = (Driver) Class.forName("org.gjt.mm.mysql.Driver()").newInstance();
       DriverManager.registerDriver(myDriver);

       System.out.println("Connecting to the datasource : " );
       conn = DriverManager.getConnection("jdbc:mysql://localhost/RouteDB");
       System.out.println("^^^^connected");


       java.sql.ResultSet rset = doTheQuery (conn);

       if (rset != null)
         processResults (rset, conn);

       conn.close();

    }

    catch (Exception e)
    {
       System.err.println("Database connection failed, defaulting to LocationInfoNode positions.");
//     e.printStackTrace();
       connectionFailed = true;

       return;

    }

  }



  private static java.sql.ResultSet doTheQuery (java.sql.Connection conn)
  {

    java.sql.ResultSet rset = null;

    try
    {
      	 //String owner = "3-FSB";        // has up to 11 legs, good for testing

      	 String assetitinerary = "assetitinerary";
      	 String assetinstance = "assetinstance";
      	 String conveyedleg = "conveyedleg";
      	 String locations = "locations";
      	 String route_elements = "route_elements";

         String cProto = "conveyanceprototype";
  	     String cInstance = "conveyanceinstance";

//         java.util.Properties prop = new java.util.Properties();


	       // main query for routeids
         String query =
         "select distinct " +
         "cl.starttime, " +      // 1
         "ai.assetid, " +        // 2
         "s.lat, s.lon, " +      // 3, 4
         "cl.endtime, " +         // 5
         "cp.conveyancetype, " + // 6
         "e.lat, e.lon, " +      // 7, 8
         "cl.routeid, " +        // 9
         "ai.ownerid " +         // 10
         "from " + assetinstance + DBRUNID + " ai, " +
         assetitinerary + DBRUNID + " itin, " +
         conveyedleg + DBRUNID + " cl, " +
         locations + DBRUNID + " s, " +
         locations + DBRUNID + " e, " +
         cProto + DBRUNID + " cp, " +
         cInstance + DBRUNID + " ci " +
         "where ai.assetid = itin.assetid " +
//         "and ai.ownerid = '2-3-INFBN' " + // debug test
         "and itin.legid = cl.legid " +
         "and cl.legtype = 1 " +   // we only want outbound/forward deployment routes
         "and cl.startlocid= s.locid " +
         "and cl.endlocid= e.locid " +
         "and cl.convid = ci.convid " +
         "and ci.prototypeid = cp.prototypeid " +
         "order by ai.ownerid, ai.assetid, cl.starttime";

//         System.out.println ("select statement is:\n \t" + query);

         java.sql.PreparedStatement ps = conn.prepareStatement(query);
//         ps.setString(1, owner);

         System.out.println ("executing query");
         rset = ps.executeQuery();
         System.out.println ("query execution complete");

         ps.close();

      }

      catch (SQLException sqle)
      {
        System.err.println ("SQL Exception");
        sqle.printStackTrace();
      }

    return rset;
  }

  private static void processResults ( java.sql.ResultSet rset, java.sql.Connection conn) throws SQLException
  {

    Vector routeLats = new Vector();
    Vector routeLons = new Vector();
    Vector routeLate = new Vector();
    Vector routeLone = new Vector();
    Vector types = new Vector();

    RoutingTable rt = null;

    String owneruid = null;
    String starttime = null;
    String endTime = null;
    String routeid = null;
    String assetId = null;

    Vector bothRTvec = new Vector (2);
    bothRTvec.setSize(2);

    boolean isAkeeperShip = false, doneAkeeperShip = false;
    boolean isAkeeperPlane = false, doneAkeeperPlane = false;
    int cvType;

    synchronized (rtsByUID)
    {

    while (rset.next())
    {

      if (owneruid == null)
      {
        owneruid = new String (rset.getString(10));
//        System.out.println ("first owner uid is: " + owneruid );
//        System.out.println (" ");
      }

      if (! owneruid.equals(rset.getString(10) ) )
      {

        // put our entry in the hashtable whether it's complete or not, we've
        // simply run out of legs for this owner
        rtsByUID.put(owneruid, bothRTvec);

        bothRTvec = new Vector (2); // the next owner should have his own vector
        bothRTvec.setSize(2);

        // reset everything for this owner's fresh start
        assetId = null;
        starttime = null;

        isAkeeperShip = false;
        isAkeeperPlane = false;

         routeLats = new Vector();
         routeLons = new Vector();
         routeLate = new Vector();
         routeLone = new Vector();

         types = new Vector();


        owneruid = new String (rset.getString(10));
//        System.out.println ("next owner uid is: " + owneruid );
//        System.out.println (" ");

      }

      if (assetId == null)
        assetId = new String (rset.getString (2));

      if ( ! assetId.equals(rset.getString (2) ) )
      {

         if (isAkeeperShip)
         {
             // check if this is before our current one, if so we still want to
             // keep it, if not pretend it's not a keeper
             if (bothRTvec.get(0) != null)
             {
               String oldStart = ( (RoutingTable) bothRTvec.get(0)).startTime;

               // if it's not earlier, pretend we didn't find it
               isAkeeperShip = startTimeBefore (oldStart, starttime);

             }

         }

         if (isAkeeperPlane)
         {
             // check if this is before our current one, if so we still want to
             // keep it, if not pretend it's not a keeper
             if (bothRTvec.get(1) != null)
             {
               String oldStart = ( (RoutingTable) bothRTvec.get(1)).startTime;

               // if it's not earlier, pretend we didn't find it
               isAkeeperPlane = startTimeBefore (oldStart, starttime);

             }

         }

        // now that we've moved on to the next asset, if we found a keeper we should, uh, keep it
        if (isAkeeperShip || isAkeeperPlane)
        {

//           System.out.println ("Creating RoutingTable spanning starttime: " + starttime + " endtime: " + endTime );
           rt = new RoutingTable(routeLats, routeLons, routeLate, routeLone, owneruid, starttime, endTime, types);

           if (isAkeeperShip)
           {
             System.out.println ("found " + rt.elats.size() + " segments with a sea leg for unit " + owneruid + " at " + starttime);
             isAkeeperShip = false;
             bothRTvec.setElementAt(rt, 0);
           }

           if (isAkeeperPlane)
           {
             // must be a keeper plane rt
             System.out.println ("found " + rt.elats.size() + " segments with an air leg for unit " + owneruid + " at " + starttime );
             isAkeeperPlane = false;

             bothRTvec.setElementAt(rt, 1);
           }

           // since we just processed a keeper, we need new memory locations for the next route positions,
           // otherwise we'll just be modifying the RoutingTable object we just created.

         }

         //  if we haven't just processed a keeper then we want to start fresh, if we haven't ...
         // ... then we STILL want to start fresh since we changed assets and are still
         // looking for a keeper
         routeLats = new Vector();
         routeLons = new Vector();
         routeLate = new Vector();
         routeLone = new Vector();

         types = new Vector();

         starttime = null; // start over leg timing too

         assetId = new String (rset.getString(2));

      }  // end if changed asset id

      cvType = rset.getInt(6);

      // For Cougaar 11.0, broke dependency on datagrabber module.
      // Constants were copied into local file.
      //      if (cvType == DGPSPConstants.CONV_TYPE_SHIP)
      if (cvType == AssetTypeConstants.ASSET_TYPE_SHIP)
         isAkeeperShip = true;

      //      if (cvType == DGPSPConstants.CONV_TYPE_PLANE)
      if (cvType == AssetTypeConstants.ASSET_TYPE_PLANE)
        isAkeeperPlane = true;

      // only get the first starttime for the legs of this asset
      // because we want the first legs start time and the last legs end time
      if (starttime == null)
        starttime = new String (rset.getString(1));

      endTime = new String (rset.getString(5));

      /* this is how we expect the query, above, to look
         "cl.starttime, " +      // 1
         "ai.assetid, " +        // 2
         "s.lat, s.lon, " +      // 3, 4
         "cl.endtime, " +         // 5
         "cp.conveyancetype, " + // 6
         "e.lat, e.lon, " +      // 7, 8
         "cl.routeid, " +        // 9
         "ai.ownerid " +         // 10
      */

      routeid = rset.getString(9);
      if (routeid == null)
      {
            // use the leg start and end as our start and end positions
             String startLat = rset.getString(3);
             routeLats.add(new Double(startLat));

             String startLon = rset.getString(4);
             routeLons.add(new Double(startLon));

             String endLat = rset.getString(7);
             routeLate.add(new Double(endLat));

             String endLon = rset.getString(8);
             routeLone.add(new Double(endLon));

             types.add (new Integer (cvType));

      }

      else
      {
        // get the individual route pieces that make up this leg, we assume
        // they start and end at the leg start and end positions
        findRouteElements ( routeid,
                            routeLats,
                            routeLons,
                            routeLate,
                            routeLone,
                            types,
                            new Integer (cvType),
                            conn );
      }

    }  // end while rset has more

    //
    // and don't forget the last one
    //

    // put our entry in the hashtable whether it's complete or not, we've
    // simply run out of legs for this owner
    rtsByUID.put(owneruid, bothRTvec);

    } // end synchronized

  }

  private static boolean startTimeBefore (String first, String second)
  {

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    try
    {

      Date fDate = dbDateFormat.parse (first);
      Date sDate = dbDateFormat.parse (second);

      // "before" returns true if the argument date, fDate, is BEFORE the object date, sDate - this function is clearly backwards
      return sDate.before(fDate);

    }

    catch (Exception nfe)
    {
      return false; // if we can't read the date we don't want it
    }

  }

  public static void setRtsHash (Hashtable oldHash)
  {
    // if there is none, we can't walk on someone else
    if (rtsByUID == null)
    {
      rtsByUID = oldHash;
    }

    else
    {
      // don't interfere with processResults()
      synchronized (rtsByUID)
      {
        rtsByUID = oldHash;
      }
    }

  }

  public static Hashtable getRtsHash ()
  {
      return rtsByUID;
  }


   public static void main(String[] args)
   {
   	RouteJdbcConnector rjet = new RouteJdbcConnector();
	//   	rjet.findRoutes("3ID-HHC", DGPSPConstants.CONV_TYPE_PLANE);
   	rjet.findRoutes("3ID-HHC", AssetTypeConstants.ASSET_TYPE_PLANE);
   }





  public static void findRouteElements ( String routeId,
                                         Vector routeLats,
                                         Vector routeLons,
                                         Vector routeLate,
                                         Vector routeLone,
                                         Vector types,
                                         Integer legConvType,
                                         java.sql.Connection conn )
  {


    try
    {

      	 String locations = "locations";
      	 String route_elements = "route_elements";

	       // main query for routeids
         String query =
         "select distinct " +
         "res.lat, res.lon, " +      // 1, 2
         "ree.lat, ree.lon " +      // 3, 4
         "from " +
         route_elements + DBRUNID + " re, " +
         locations + DBRUNID + " res, " +
         locations + DBRUNID + " ree " +
         "where re.routeid = (?) " +
         "and re.startlocid= res.locid " +
         "and re.endlocid= ree.locid " +
         "order by re.elem_num";

//         System.out.println ("Route element select statement is:\n \t" + query);

    	   //"select routeid, startlocid, endlocid, elem_num from route_elements_49 where routeid like (?)";
    	   //"select routeid, startlocid, endlocid, elem_num from " + route_elements + runid + " where routeid like (?)";
         java.sql.PreparedStatement ps = conn.prepareStatement(query);
         ps.setString(1, routeId);

         java.sql.ResultSet rset = ps.executeQuery();
         ps.close();

         // Process the results
         String starttime = null;
         String endTime = null;
         String routeid = null;
         String assetId = null;
         boolean isAkeeper = false;

         while (rset.next())
         {

           String startLat = rset.getString(1);
           routeLats.add(new Double(startLat));

           String startLon = rset.getString(2);
           routeLons.add(new Double(startLon));

           String endLat = rset.getString(3);
           routeLate.add(new Double(endLat));

           String endLon = rset.getString(4);
           routeLone.add(new Double(endLon));

           types.add (legConvType);

        }
    }

      //catch (SQLException sqle)
    catch (Exception sqle)
    {
      	connectionFailed = true;
       	sqle.printStackTrace();
    }

  }



}

