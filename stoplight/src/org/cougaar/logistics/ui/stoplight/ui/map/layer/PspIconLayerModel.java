/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;
import java.text.SimpleDateFormat;

import com.bbn.openmap.LatLonPoint;

import com.bbn.openmap.Layer;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.event.ProjectionEvent;
import com.bbn.openmap.Environment;

import com.bbn.openmap.event.*;

// For Cougaar 11.0 broke dependency on datagrabber module
// Copied constants to local AssetTypeConstants file
//import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.Location;
import org.cougaar.glm.ldm.plan.GeolocLocationImpl;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;

import org.cougaar.planning.ldm.plan.ScheduleImpl;

import org.cougaar.logistics.ui.stoplight.ui.components.RangeSliderPanel;
import org.cougaar.logistics.ui.stoplight.ui.map.ScenarioMapBean;
import org.cougaar.logistics.ui.stoplight.ui.map.app.*;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.*;
import org.cougaar.logistics.ui.stoplight.ui.map.util.NamedLocationTime;

import org.cougaar.glm.map.MapLocationInfo;

public class PspIconLayerModel extends PspIconLayerModelBase
{

  OMGraphicList curTimeMarkers = new OMGraphicList();

  Hashtable dailyNLUnits = new Hashtable (300);
  Hashtable schedImplByUnit = new Hashtable (300);

  OMCGMIcons cgmicons;

  public Hashtable mostRecentlyLoaded = null;

  Collection nls = null;

  public PspIconLayer myOwningLayer;

  private PspIconKeepAlive pika = null;

  PspIconLayerModel(PspIconLayer myOwner)
  {
     myOwningLayer = myOwner;

     try
     {
//         System.out.println("load is loading cgm txt");
       cgmicons = new OMCGMIcons ("cgmload.txt");
     }
     catch (java.io.IOException ioex)
     {
       ioex.printStackTrace();
     }

      pika = new PspIconKeepAlive(this);
      pika.start();
  }

  OMGraphic findClosest(int x, int y, float limit)
  {
    return curTimeMarkers.findClosest(x, y, limit);
  }



  private OMGraphic makeIconGraphic ( float lat, float lon, Color color,
                                      String type )
  {
    return makeIconGraphic (lat, lon, color, type, 0);
  }


  private OMGraphic makeIconGraphic ( float lat, float lon, Color color,
                                      String type, int echelon )
  {

    OMGraphic ret=null;


    if (type!=null && !type.equals(""))
    {

//      System.out.println("Creating cgmicon of type "+type);
      try
      {
        ret = new CGMVecIcon ( (OMCGM) cgmicons.get (type.toLowerCase()), lat, lon );
      }

      catch (Exception ex)
      {

         // try an upper case match
         	try
        	{
         	  ret = new CGMVecIcon ( (OMCGM) cgmicons.get (type.toUpperCase()), lat, lon );
         	}

        	catch(Exception ex2)
         	{
            System.err.println ("\tNo such cgmvecicon " + type);
           // ex.printStackTrace();
           }

       }

       if (type.equalsIgnoreCase("Armored"))
       {
          ret = new CGMVecIcon ( (OMCGM) cgmicons.get ("armor"), lat, lon );
       }

       if (type.equalsIgnoreCase("Infantry"))
       {
         ret = new CGMVecIcon ( (OMCGM) cgmicons.get ("infantry"), lat, lon );
       }

     }

     if (ret==null)
     {
       ret=new VecIcon(lat, lon, color);
     }
     else
     {
       ( (CGMVecIcon) ret).setEchelon (echelon);
     }

     return ret;

   }


   private Unit makeUnit ( double lat, double lon, Color color, String type, String orgID, int echelon )
   {

     OMGraphic omgraphic;

//   System.out.println("PspIconLayerModelBase:makeUnit type: " + type + " label: " + msg );

     omgraphic = makeIconGraphic( (float) lat, (float) lon, color, type, echelon );
     ((VecIcon) omgraphic).addToMessage(type);
     ((VecIcon) omgraphic).setLabel(orgID);
     markers.add(omgraphic);

     Unit unit = new Unit( orgID, omgraphic, new Hashtable(1) );

     Unit u2 = (Unit) units.put(omgraphic, unit);

     return unit;

   }

   private Unit makeUnit ( float lat, float lon, Color color,
                           String label, String msg, Hashtable data )
   {

     OMGraphic omgraphic;

     // omgraphic = new VecIcon(lat, lon, color);
     String type="infantry";
     type=getUnitType(label);

//            System.out.println("-- TimedXlm.getUnitIconType("+label+") returns: ["+type+"]");

     omgraphic = makeIconGraphic(lat, lon, color, type);
     ((VecIcon) omgraphic).addToMessage(msg);
     ((VecIcon) omgraphic).setLabel(label);

     markers.add(omgraphic);

     // omList.add(omgraphic);

     Unit unit = new Unit(label, omgraphic, data);
     Unit u2 = (Unit) units.put(omgraphic, unit);
     if (u2 != null) {
       System.err.println("units.put returned non-null value-- inserted unit: "
                                   +unit+" key: "+omgraphic+" returned unit: "+u2);
      }

      return unit;

  }


  public void load (Hashtable loadMe)
  {

     mostRecentlyLoaded = loadMe;

//     System.err.println("PspIconLayerModel.load( Hashtable )");

     Vector hiddenUnitLabels = new Vector(0);
     Hashtable assetBarList = new Hashtable(1);
     NamedLocationTime nltm = null;

     synchronized (schedImplByUnit) // synched against PspIconLayerBase.setVisibleForAllNamedLocations
     {

       if (nls != null)
       {

         for (Iterator it=nls.iterator(); it.hasNext();)
         {

           nltm = (NamedLocationTime)it.next();
           if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
           {

              VecIcon graphic = (VecIcon)nltm.getUnit().getGraphic();
              String unitLabel = nltm.getUnit().getLabel();
              if (graphic.isVisible())
              {
                hiddenUnitLabels.add(unitLabel);
              }
              
              if (graphic.assetBarGraphic != null)
              {
                assetBarList.put(unitLabel, graphic.assetBarGraphic);
              }

           }
           
         }
         
       }

       schedImplByUnit.clear();

       Collection hashCol = loadMe.values();

//       System.out.println ("hashtable size is: " + loadMe.size() );

       // for the whole hashtable
       for (Iterator hashItr=hashCol.iterator(); hashItr.hasNext();)
       {

         MapLocationInfo mli = (MapLocationInfo) hashItr.next();

         ScheduleImpl unitSI = load (mli);

         // augment this with route waypoints from the leg/route database
         getRouteWayPoints ( mli.getUID(), unitSI );

         // change the last one to have an end time in perpetuity (i.e. MAX_LONG)
         NamedLocationTime lastNLT = (NamedLocationTime) unitSI.last();
         lastNLT.setEndTime(Long.MAX_VALUE);
         
         // keep the schedules organized by unit for quick build
         schedImplByUnit.put (mli.getUID(), unitSI);

       }

     }

    if (RangeSliderPanel.rangeSlider != null)
    {
      buildDailyNLUnits( (long)RangeSliderPanel.rangeSlider.getMinValue() * 1000L,
                         (long)RangeSliderPanel.rangeSlider.getMaxValue() * 1000L );
    }

    // Set all hidden icons
    for (int i=0, isize=hiddenUnitLabels.size(); i<isize; i++)
    {
      setVisibleForAllNamedLocations((String)hiddenUnitLabels.elementAt(i), false);
    }

    for (Enumeration e=assetBarList.keys(); e.hasMoreElements();)
    {
      String unitName = (String)e.nextElement();
      setAssetBarGraphic(unitName, (AssetBarGraphic)assetBarList.get(unitName));
    }

    System.out.println ("load of all units completed");

  }

  public void getRouteWayPoints (String uid, ScheduleImpl sI)
  {

    // just get the first one for a reference copy
    NamedLocationTime copyMeNlt = (NamedLocationTime) sI.get(0);

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    long startMillis = 0;
    long endMillis = 0;

    RoutingTable rt;

    try
    {
      // Get the database stuff
//      System.out.println ("\ngetting route way points for: " + uid);

      // try for ship routes first, if none get the air route
      //      rt = RouteJdbcConnector.findRoutes (uid, DGPSPConstants.CONV_TYPE_SHIP );
      rt = RouteJdbcConnector.findRoutes (uid, AssetTypeConstants.ASSET_TYPE_SHIP );
      if (rt == null || rt.elats.size() == 0)
      {
        rt = RouteJdbcConnector.findRoutes (uid, AssetTypeConstants.ASSET_TYPE_PLANE );
        if (rt == null || rt.elats.size() == 0)
        {
          return;  // apparently we found nothing
        }
        else
        {
           // if it's a plane we want to load up the GREAT CIRCLE points so we can follow those
          rt = followGreatCircleRT (rt, ScenarioMap.getMapBean (myOwningLayer.getRootPane() ));
        }

      }

      // for debugging just try the plane
//      rt = RouteJdbcConnector.findRoutes (uid, AssetTypeConstants.ASSET_TYPE_PLANE );
//     if (rt == null || rt.elats.size() == 0)
//      return;
//          rt = followGreatCircleRT (rt);

      // if it's a plane we want to load up the GREAT CIRCLE points

      try
      {

        Date dat = dbDateFormat.parse (rt.startTime);
        startMillis = dat.getTime();

        dat = dbDateFormat.parse (rt.endTime);
        endMillis = dat.getTime();

      }
      catch (NumberFormatException nfe)
      {
        System.err.println("Exception caught is: " + nfe.toString() );
        System.err.println ("PspIconLayerModel:getRouteWayPoints found database bad date string(s), either: ");
        System.err.print ( rt.startTime );
        System.err.print ("\t or \t");
        System.err.println ( rt.endTime );

        return; // can't do anything without a timeline
      }

    }
    catch (NullPointerException npe)
    {
      System.err.println ( "NullPointerExecption on " + uid + " stack trace: " );
      npe.printStackTrace();
      return;
    }

    catch (Exception exc)
    {
      System.err.println ("database not available or query failed for uid: " + uid );
      System.err.println ("query error is: " + exc.toString());
      return;
    }

    long totalTime = endMillis - startMillis;
    if (totalTime < 0)
    {
      System.err.println ("total leg times are negative: " + totalTime);
      totalTime *= (-1); // this shouldn't happen!
    }

//    System.out.println ("total time for all legs: " + totalTime);


    double totalDistance = getTotalDistance (rt);
//    System.out.println ("total distance is: " + totalDistance );

    if (rt.elats.size() > 1)
    {

      // save the home location, dump the rest because we'll use the database points
      NamedLocationTime homeboy = null;
      for (int iisi = 0; iisi < sI.size(); iisi ++ )
      {

        // we identify home location by the fact that it's at the "beginning of time"
        homeboy = (NamedLocationTime) sI.get(iisi);
        if (homeboy.getStartTime() < 1000) // if we're within one second of Dec. 31, 1969, that's close enough
          break;
      }

      sI.clear(); // as long as we have a reference to homeboy the (nefarious?) garbage collect won't touch it

      if (homeboy == null)
        System.err.println ("Unit " + uid + " has no home location.");
      else
        sI.add (homeboy);
    }
    
//    System.out.println ("totalDistance: " + totalDistance );

    // for each waypoint, set his time based on how far he is along the total distance
    // the take that as a percentage of total time
    for (int ii = 0; ii < rt.elats.size(); ii ++)
    {


      double partDist = getPartialDistance ( rt,
                                             (Double) rt.elats.get(ii),
                                             (Double) rt.elons.get(ii) );

//      System.out.println ("Partial distance is: " + partDist);

      // until we get these things in order
      if (partDist < 0.0)
      {
//        System.err.println ("Database waypoints are out of sequence for unit: " + uid );
        continue;
      }


      float percDist = (float) (partDist / totalDistance);
//      System.out.println ("percent Dist is: " + percDist );

      long timePiece = startMillis + (long) ( (float) totalTime * percDist + 0.5f);

//      System.out.println ("start time is: " + (new Date (timePiece)).toString() );
//      System.out.println ("lat  is: " + ( (Double) rt.elats.get(ii)).doubleValue() );
//      System.out.println ("lat  is: " + ( (Double) rt.elons.get(ii)).doubleValue() );

      // now we can create a namedLocationTime object at this location with this time
      NamedLocationTime iNlt = InterpolateIconPos.makeInterimNLT( copyMeNlt,
                                                                  (float) ( (Double) rt.elats.get(ii)).doubleValue(),
                                                                  (float) ( (Double) rt.elons.get(ii)).doubleValue(),
                                                                  timePiece,
                                                                  timePiece + 1000L);

      sI.add(iNlt);

    }

  }



  private static double getTotalDistance (RoutingTable rt)
  {

     double totDist = 0.0;
     for (int ii = 0; ii < rt.slats.size(); ii ++)
     {

       // the ol' c-squared = a-squared + b-squared, dist = square-root-of-c-squared trick (I fell for that one last week, chief)
       double asquared = Math.pow( ( ((Double) rt.elats.get(ii)).doubleValue() - ( (Double) rt.slats.get(ii)).doubleValue() ), 2.0);
       double bsquared = Math.pow( ( ((Double) rt.elons.get(ii)).doubleValue() - ( (Double) rt.slons.get(ii)).doubleValue() ), 2.0);
       totDist += Math.pow ((asquared + bsquared), 0.5);

     }

     return totDist;

  }

  public static double getPartialDistance ( RoutingTable rt,
                                      Double latD,
                                      Double lonD )
  {

    final double CLOSE_ENOUGH = 0.01f;
    double lat = latD.doubleValue();
    double lon = lonD.doubleValue();
    
    double partDist = -1.0;

    for (int jj = 0; jj < rt.slats.size(); jj ++)
    {
       double latDiff = ( (Double) rt.elats.get(jj)).doubleValue() - lat;
       if (latDiff < 0.0)
         latDiff *= (-1.0);

       double lonDiff = ( (Double) rt.elons.get(jj)).doubleValue() - lon;
       if (lonDiff < 0.0)
         lonDiff *= (-1.0);

       if ( latDiff < CLOSE_ENOUGH &&
            lonDiff < CLOSE_ENOUGH )
       {

         partDist = 0.0;  // if we're actually calculating a point we need to start our count at zero not -1

         // calculate the distance only up to this point
         for (int ii = 0; ii <=jj; ii ++ )
         {
           // the ol' c-squared = a-squared + b-squared, dist = square-root-of-c-squared trick (I fell for that one last week, chief)
           double asquared = Math.pow( ( ((Double) rt.elats.get(ii)).doubleValue() - ( (Double) rt.slats.get(ii)).doubleValue() ), 2.0);
           double bsquared = Math.pow( ( ((Double) rt.elons.get(ii)).doubleValue() - ( (Double) rt.slons.get(ii)).doubleValue() ), 2.0);
           partDist += Math.pow ((asquared + bsquared), 0.5);
         }

         break;
       }
    }

    return partDist;
  }

  void updateOneMli (MapLocationInfo mli)
  {

    ScheduleImpl sIRef = (ScheduleImpl) schedImplByUnit.get (mli.getUID() );

    // then we build the new one in the new one
    ScheduleImpl schImpl = load (mli);

    // augment this with route waypoints from the leg/route database
    getRouteWayPoints ( mli.getUID(), schImpl );

    // change the last one to have an end time in perpetuity (i.e. MAX_LONG)
    NamedLocationTime lastNLT = (NamedLocationTime) schImpl.last();
    lastNLT.setEndTime(Long.MAX_VALUE);


    long topVal = -1;
    boolean showMe = false;
    AssetBarGraphic assetG = null;
    if (sIRef != null)
    {
      NamedLocationTime nlt = (NamedLocationTime) sIRef.get(0);
      VecIcon vi = (VecIcon) nlt.getUnit().getGraphic();
      showMe = vi.isVisible();
      topVal = vi.locationNumber;
      assetG = vi.assetBarGraphic;
    }

    // keep the schedules organized by unit for quick build
    synchronized (schedImplByUnit)
    {
      schedImplByUnit.put (mli.getUID(), schImpl);
    }   // be sure not to include this synch block around the setTime call below, it'll deadlock

    setVisibleForAllNamedLocations (mli.getUID(), showMe);
    if (topVal != -1)
    {
      moveIconOnStack(schImpl, mli.getUID(), topVal);
    }

    if (assetG != null)
    {
      setAssetBarGraphic(mli.getUID(), assetG);
    }

    // then we rebuild the daily nlUnits
    if (RangeSliderPanel.rangeSlider != null)
    {
      buildDailyNLUnits( (long)RangeSliderPanel.rangeSlider.getMinValue() * 1000L,
                         (long)RangeSliderPanel.rangeSlider.getMaxValue() * 1000L );
    }

    // then we reset the time to redisplay the units position
    myOwningLayer.setTime(null);

  }



  ScheduleImpl load (MapLocationInfo mli)
  {

    Vector schedVec = mli.getScheduleElements();
    ScheduleImpl unitSI = new ScheduleImpl();
    // store the relationship stuff
    myOwningLayer.addRelation(mli.getUID(), mli.getRelationshipSchedule() );

    // for the whole schedule of a hashtable entry
    for (Iterator schedItr=schedVec.iterator(); schedItr.hasNext();)
    {

      LocationScheduleElement locSched = (LocationScheduleElement) schedItr.next();
      GeolocLocationImpl loc = (GeolocLocationImpl) locSched.getLocation();

      Latitude lat = loc.getLatitude();
      Longitude lon = loc.getLongitude();

      Unit myUnit = makeUnit ( lat.getDegrees(), lon.getDegrees(), Color.white,
                               mli.getSymbol(), mli.getUID(), mli.getEchelon() );

      NamedLocationTime nl = new NamedLocationTime ( mli.getSymbol(),
                                                     Double.toString(lat.getDegrees()),
                                                     Double.toString(lon.getDegrees()),
                                                     Long.toString(locSched.getStartTime()),
                                                     Long.toString(locSched.getEndTime()),
                                                     null );

      nl.setUnit(myUnit);

      unitSI.add(nl);

    }

    return unitSI;

  }

  void setTime(long time)
  {

//      System.out.println("pspiconLayerModel setTime, time is: " + time );

      synchronized (dailyNLUnits) // synched against load
      {

        curTimeMarkers.clear();

        Long dayKey = new Long (time / 1000 / 60 / 60 / 24);
//        System.out.println ("looking for day key: " + dayKey);
        ScheduleImpl todaysNLUnits = (ScheduleImpl) dailyNLUnits.get (dayKey);

/*  display all units in a daily bucket, for testing
        for (Iterator it=todaysNLUnits.iterator(); it.hasNext(); )
        {

          NamedLocationTime nltm=(NamedLocationTime)it.next();

          if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
          {
            curTimeMarkers.add(nltm.getUnit().getGraphic());
            Unit u2 = (Unit) units.put(nltm.getUnit().getGraphic(), nltm.getUnit());
          }
        }
*/

        if (todaysNLUnits == null)
        {
          System.out.println ("No units for this time");
          todaysNLUnits = new ScheduleImpl(); // go through the process with nothing to do
        }


        nls = NamedLocationTime.getNamedLocationsAtTimeInterpolate (todaysNLUnits, time);

        for (Iterator it=nls.iterator(); it.hasNext(); )
        {

          NamedLocationTime nltm=(NamedLocationTime)it.next();

          if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
          {
            curTimeMarkers.add(nltm.getUnit().getGraphic());
            Unit u2 = (Unit) units.put(nltm.getUnit().getGraphic(), nltm.getUnit());
          }
        }

      }

  }

  public void setVisibleForAllNamedLocations(String unitLabel, boolean visibility)
  {
    NamedLocationTime nltm = null;

    synchronized (schedImplByUnit)  // synched against load
    {

      ScheduleImpl mySched = (ScheduleImpl)schedImplByUnit.get(unitLabel);

      for (Enumeration enum=mySched.getAllScheduleElements(); enum.hasMoreElements();)
      {
        nltm = (NamedLocationTime)enum.nextElement();

//        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null && nltm.getUnit().getLabel().equals(unitLabel))
        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
        {
          nltm.getUnit().getGraphic().setVisible(visibility);
          nltm.getUnit().getGraphic().setNeedToRegenerate(true);
        }

      }

      for (Iterator it=nls.iterator(); it.hasNext();)
      {

        nltm = (NamedLocationTime)it.next();
        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null && nltm.getUnit().getLabel().equals(unitLabel))
//        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
        {
          nltm.getUnit().getGraphic().setVisible(visibility);
          nltm.getUnit().getGraphic().setNeedToRegenerate(true);
        }

      }
    }
  }

  public void moveIconToTop(Unit unit)
  {
    curTimeMarkers.moveIndexedToTop(curTimeMarkers.indexOf(unit.getGraphic()));

    synchronized (schedImplByUnit)  // synched against load
    {
      ScheduleImpl mySched = (ScheduleImpl)schedImplByUnit.get(unit.getLabel());
      VecIcon.maxNumber++;

      moveIconOnStack(mySched, unit.getLabel(), VecIcon.maxNumber);
    }
  }

  private void moveIconOnStack(ScheduleImpl mySched, String unitLabel, long location)
  {
    NamedLocationTime nltm = null;
    for (Enumeration enum=mySched.getAllScheduleElements(); enum.hasMoreElements();)
    {
      nltm = (NamedLocationTime)enum.nextElement();

      if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
      {
        ((VecIcon)nltm.getUnit().getGraphic()).locationNumber = location;
      }

    }

    for (Iterator it=nls.iterator(); it.hasNext();)
    {

      nltm = (NamedLocationTime)it.next();
      if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null && nltm.getUnit().getLabel().equals(unitLabel))
      {
        ((VecIcon)nltm.getUnit().getGraphic()).locationNumber = location;
      }
    }
  }

  public void setAssetBarGraphic(String unitLabel, AssetBarGraphic assetG)
  {
    synchronized (schedImplByUnit)  // synched against load
    {
      ScheduleImpl mySched = (ScheduleImpl)schedImplByUnit.get(unitLabel);
      NamedLocationTime nltm = null;
      for (Enumeration enum=mySched.getAllScheduleElements(); enum.hasMoreElements();)
      {
        nltm = (NamedLocationTime)enum.nextElement();
  
        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
        {
          ((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic = assetG;
        }
  
      }
  
      for (Iterator it=nls.iterator(); it.hasNext();)
      {
  
        nltm = (NamedLocationTime)it.next();
        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null && nltm.getUnit().getLabel().equals(unitLabel))
        {
          ((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic = assetG;
        }
      }
    }
  }

  final static Vector emptyVector=new Vector();

  Iterator markerIterator()
  {

    Iterator rit;

    if (curTimeMarkers!=null&&curTimeMarkers.getTargets()!=null)
    {
        rit=curTimeMarkers.getTargets().iterator();
    }
    else
    {
        rit=emptyVector.iterator();
    }

    return rit;
  }


  public void changeLocationInfoNode (String newUri)
  {

//    System.out.println ("changing location nodes");

    pika.enoughAlready = true;

//    System.out.println ("thread stopped");

    uriString = new String (newUri);

    pika = new PspIconKeepAlive(this);

    pika.start();

//    System.out.println ("thread started");

  }

  public synchronized void buildDailyNLUnits (long startMillis, long endMillis)
  {

     Collection dayGroup = null;

     //
     // The basic algorithm is to plot every units position for every day into a bin, a day-bin.
     // Any unit that is at a fixed position that entire day gets that NamedLocationTime object placed
     // into that day's bin. Any units that are not at a fixed position that day get multiple elements
     // placed into the search bin: the one with the closest start time to noon; the one with the closest
     // start time the beginning of the day; and the one with the closest end time to end of the day.
     // The idea being to supply enough information from which to interpolate
     // the position any millisecond of the day.
     //

     // we have to have an extra day so we can interpolate the very end (right) of the slider
     endMillis += (24 * 60 * 60 * 1000);

     // first we have to make a place to hold all these NamedLocationTime
     long numDays = (endMillis - startMillis) / (long) (1000L * 60L * 60L * 24L);
//     System.out.println ("buildDailyUnits day range is: " + numDays);

     ScheduleImpl newScheds[] = new ScheduleImpl [(int) numDays + 2];
     for (int ii = 0; ii <= numDays + 1; ii ++)
       newScheds[ii] = new ScheduleImpl();

     // for every unit, do every day
     long startDay = startMillis / 1000L / 60L / 60L / 24L;
     long endDay = endMillis / 1000L / 60L / 60L / 24L;

     for ( Enumeration keyE = schedImplByUnit.keys(); keyE.hasMoreElements();)
     {

         String keyId = (String) keyE.nextElement();
//         String keyId = new String ("3-FSB");
//         System.out.println ("\n building daily positions for this unit: " + keyId);

         ScheduleImpl unitSI = (ScheduleImpl) schedImplByUnit.get(keyId);   // we really should synch against this but it's high risk for deadlock
/*
         if ( keyId.equals("2-3-INFBN") )
         {
           System.out.println ("Searching NLTs of: ");
           for (int jj=0; jj < unitSI.size(); jj ++)
           {
             NamedLocationTime nlt = (NamedLocationTime) unitSI.get(jj);
             System.out.print ("start time: " + new Date(nlt.getStartTime()).toString() );
             System.out.println (" end time: " + new Date(nlt.getEndTime()).toString() );
           }
         }
*/

         int iivec=0; // schedule vector counter

         // for every day including the end day
         for ( long currDay = startDay; currDay <= endDay; currDay += 1, iivec ++ ) // (1000 * 60 * 60 * 24) )
         {

           ScheduleImpl addSched = (ScheduleImpl) newScheds[iivec];

           long noonSearchTime = ((currDay * 24L + 12L) * 60L * 60L * 1000L) + 1L; // 12:00:00.00 noon that day
//           System.out.println ("\nnoon search time is: " + new Date (noonSearchTime).toString() );

           long startSearchWindow = (currDay * 24L * 60L * 60L * 1000L) + 1L; // 00:00:00.01 that day
//           System.out.println ("start search window is: " + new Date (startSearchWindow).toString() );

           long endSearchWindow = ((currDay + 1) * 24L * 60L * 60L * 1000L) - 1L; // 23:59:59.999  that day
//           System.out.println ("end search window is: " + new Date (endSearchWindow).toString() );

           Vector dayBinNLTs = new Vector();

           recurseSearchSchedImpls ( unitSI,
                                     noonSearchTime,
                                     InterpolateIconPos.NEARESTSTARTTIME,
                                     startSearchWindow,
                                     dayBinNLTs );

//           System.out.println ("recursive search for start returned: " + dayBinNLTs.size() + " units" );
           boolean needToGetEnd = true;
           for ( int ii = 0; ii < dayBinNLTs.size(); ii ++)
           {

             // if no NLT ends after today, we need to get all the "end pieces"
             if ( ((NamedLocationTime) dayBinNLTs.get(ii)).getEndTime() > endSearchWindow )
             {
               needToGetEnd = false;
             }
           }

           if (needToGetEnd)
           {
             // get all the NLTs that end before our endSearchWindow time
             recurseSearchSchedImpls ( unitSI,
                                       noonSearchTime,
                                       InterpolateIconPos.NEARESTENDTIME,
                                       endSearchWindow,
                                       dayBinNLTs);

           }

           // now that we've got everything we can make the schedule for this unit
           for ( int ii = 0; ii < dayBinNLTs.size(); ii ++)
           {
             NamedLocationTime thisNlt = (NamedLocationTime) dayBinNLTs.get(ii);

             addSched.add ( thisNlt );
           }

         }

       }


     // now, finally, transfer everything over to the structure from which it will be displayed
     synchronized (dailyNLUnits)
     {

       dailyNLUnits.clear();

       // build our hashtable
       for (int ii = 0; ii < numDays; ii ++ )
       {
          ScheduleImpl addSched = (ScheduleImpl) newScheds[ii];
          long keyVal = startDay + ii;
//         System.out.println ("creating " + addSched.size() + " elements with day key of " + keyVal );
         dailyNLUnits.put(new Long (keyVal), addSched);
        }
     }
  }

  private NamedLocationTime findClosestTimeBeforeOrAfter ( long testTime, ScheduleImpl sched, int ba )
  {

    NamedLocationTime returnMe = null;

    long minTime = Long.MAX_VALUE;
    long testDiff;

    for ( ListIterator schedIter = sched.listIterator(); schedIter.hasNext(); )
    {

        NamedLocationTime testMe = (NamedLocationTime) schedIter.next();

        // note: we're testing for closest time elements in a gap between one end time and
        // it's following start time, as in endTime->testTime->startTime
        testDiff = Long.MAX_VALUE;

        // looking for a start time before the test time
        if (ba == InterpolateIconPos.NEARESTSTARTTIME )
        {
          if (testMe.getStartTime() < testTime)
          {
            testDiff = testTime - testMe.getStartTime();

//          System.out.print ("found an earlier start time: ");
//          System.out.println (new Date (testMe.getStartTime()).toString() );
//          System.out.print (" before our test time ");
//          System.out.println ( new Date (testTime).toString() );

          }
        }

        else  // must be looking for end time after the test time
        {

          if (testMe.getEndTime() > testTime )
          {

//          System.out.print ("found an end time: ");
//          System.out.println (new Date (testMe.getEndTime()).toString() );
//          System.out.print (" after our end time ");
//          System.out.println ( new Date (testTime).toString() );

            testDiff = testMe.getEndTime() - testTime;

          }
        }

        if (testDiff > 0 && testDiff < minTime)
        {
            minTime = testDiff;
            returnMe = testMe;
        }

    }

    return returnMe;

  }


  private void recurseSearchSchedImpls ( ScheduleImpl sched, long testTime, int sORe, long referenceTime, Vector retNLTs)
  {

//      System.out.println (" testTime is: " + new Date (testTime).toString() + " \treference time " + new Date (referenceTime).toString() );

      // find the closest start time before the test time
      NamedLocationTime testNLT = findClosestTimeBeforeOrAfter ( testTime,
                                                                 sched,
                                                                 sORe );

       if (testNLT != null)
       {
         retNLTs.add (testNLT);
       }

       else
       {
         // the only way we can get here is looking for an NLT with an end time greater than our final
         // temopral position, so this would be a great place to redefine the end time of that NLT to be MAX_LONG
         // we should only have to do this once since once it's reset the search will never return null again

         return;

       }

       if (sORe == InterpolateIconPos.NEARESTSTARTTIME)
       {

         //
         // if the NamedLocationTime doesn't cover entire day
         //

         if ( testNLT.getStartTime() > referenceTime )
         {
              // we have to find the next one with the even earlier start time
           recurseSearchSchedImpls (sched, testNLT.getStartTime(), sORe, referenceTime, retNLTs);
         }

       }

       else
       {
         // must be looking for NLTs with closest end times
         if ( testNLT.getEndTime() < referenceTime )
         {
           // keep looking, this one ends before today expires
           recurseSearchSchedImpls (sched, testNLT.getEndTime(), sORe, referenceTime, retNLTs);
         }

       }

    }

  private static RoutingTable followGreatCircleRT (RoutingTable baseRT, ScenarioMapBean mapBean)
  {

    Vector sLats = new Vector();
    Vector sLons = new Vector();
    Vector eLats = new Vector();
    Vector eLons = new Vector();
    Vector types = new Vector();


    for (int ii = 0; ii < baseRT.elats.size(); ii ++)
    {
      OMWideLine omwl = new OMWideLine ( (float) ( (Double) baseRT.slats.get(ii)).doubleValue(),
                                (float) ( (Double) baseRT.slons.get(ii)).doubleValue(),
                                (float) ( (Double) baseRT.elats.get(ii)).doubleValue(),
                                (float) ( (Double) baseRT.elons.get(ii)).doubleValue(),
                                OMWideLine.LINETYPE_GREATCIRCLE,
                                10 );

      Projection proj = mapBean.getProjection();
      if (proj == null)
      {
        System.err.println ("Map has no current projection, can't follow air route plot");
        return baseRT;
      }

      omwl.generate(proj);

      int[][] xptsD = omwl.getXpoints();
      int[][] yptsD = omwl.getYpoints();

      // start at one and do this point with the previous point to make a route segment
      for (int jj = 1; jj < xptsD[0].length; jj ++ )
      {
        LatLonPoint llOne = proj.inverse(xptsD[0][jj-1], yptsD[0][jj-1]);
        LatLonPoint llTwo = proj.inverse(xptsD[0][jj], yptsD[0][jj]);

        sLats.add( new Double ((double) llOne.getLatitude()) );
        sLons.add( new Double ((double) llOne.getLongitude()) );

        eLats.add( new Double ((double) llTwo.getLatitude()) );
        eLons.add( new Double ((double) llTwo.getLongitude()) );

	//        types.add ( new Integer (DGPSPConstants.CONV_TYPE_PLANE) );
        types.add ( new Integer (AssetTypeConstants.ASSET_TYPE_PLANE) );

      }
    }

    RoutingTable retRT = new RoutingTable( sLats, sLons,
                                           eLats, eLons,
                                           baseRT.orgName,
                                           baseRT.startTime,
                                           baseRT.endTime,
                                           types );

    return retRT;
  }

}





