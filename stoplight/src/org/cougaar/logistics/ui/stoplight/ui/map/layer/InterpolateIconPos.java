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

import java.util.Collection;
import java.util.ListIterator;
import java.util.Hashtable;
import java.util.Vector;

import com.bbn.openmap.omGraphics.OMGraphic;

import org.cougaar.planning.ldm.plan.ScheduleImpl;


import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.*;
import org.cougaar.logistics.ui.stoplight.ui.map.util.NamedLocationTime;

public class InterpolateIconPos
{

  public static final float PERCENT_TRAVEL_TIME = 0.10f; // + %10
  static final long SAME_START_END_DELTA = (60 * 60 * 1000); // within an hour

  public static Hashtable reUse = new Hashtable();
  
  public InterpolateIconPos()
  {
  }

  public static NamedLocationTime findClosestNlt (String orgId, long testTime, ScheduleImpl sched, int sORe)
  {

      NamedLocationTime returnMe = null;

      long minTime = Long.MAX_VALUE;
      long testDiff;

      for ( ListIterator schedIter = sched.listIterator(); schedIter.hasNext(); )
      {

        NamedLocationTime testMe = (NamedLocationTime) schedIter.next();

        if (orgId.equals(testMe.getUnit().getLabel()))
        {
          // note: we're testing for closest time elements in a gap between one end time and
          // it's following start time, as in endTime->testTime->startTime;
          // so we're looking for closest end time BEFORE the testTime argument
          // and closest start time AFTER testTime
          if (sORe == NEARESTENDTIME)
          {
             testDiff = testTime - testMe.getEndTime();
          }
          else
          {
           testDiff = testMe.getStartTime() - testTime;
//           System.out.println ("testDiff is: " + testDiff);
          }
          if (testDiff > 0 && testDiff < minTime)
          {
            minTime = testDiff;
            returnMe = testMe;
//            System.out.println ("keeping: " + new Date (returnMe.getStartTime()).toString() );
          }
        }
      }

      return returnMe;

  }


  public static NamedLocationTime findOverlappingNlt ( String orgId,
                                                       long testTime,
                                                       ScheduleImpl sched )
  {

      NamedLocationTime returnMe = null;

      long minTime = Long.MAX_VALUE;
      long testEnd, testStart;

      for ( ListIterator schedIter = sched.listIterator(); schedIter.hasNext(); )
      {

        NamedLocationTime testMe = (NamedLocationTime) schedIter.next();

        if (orgId.equals(testMe.getUnit().getLabel()))
        {

          testEnd = testMe.getEndTime() - testTime; // ends after time mark
          testStart = testTime - testMe.getStartTime(); // starts before time mark

          if (testEnd > 0 && testStart > 0 )
          {
            // found it, there should be only one
            returnMe = testMe;
          }
        }
      }

      return returnMe;

    }

    public static Collection findTransitionNLTs (Collection origCol,
                                                  ScheduleImpl sched,
                                                  long targetTime)
    {

      Hashtable alreadyDone = new Hashtable(900);
      Vector ourVec = new Vector(origCol);

      // for the whole schedule, find the orgs that are not currently in the collection,
      // then pass them to interpolateEndStart to find the nearest start and end times

      for (ListIterator schedItr = sched.listIterator(); schedItr.hasNext(); )
      {

        NamedLocationTime nlt = (NamedLocationTime) schedItr.next();
        String schedOrgId = nlt.getUnit().getLabel();

        boolean foundIt = false;
        for (int ii = ourVec.size() - 1; ii >=0; ii --)
        {

          NamedLocationTime colNlt = (NamedLocationTime) ourVec.get (ii);

          if ( schedOrgId.equals(colNlt.getUnit().getLabel() ) )
          {

            // however, once we find one that is in the list we need to check to see if it has
            // an adjacent/abutting time such that the target time, where the slider stopped,
            // is within the PERCENT_TRAVEL_TIME window.
            // then also, once we check we don't have to do it again

            foundIt = true;

            // we don't have to check if this is done more than once (i.e.
            // use the alreadyDone hashtable) a unit can be in the time intersection,
            // aka the colNlt, only once
/*
            NamedLocationTime abutNlt = hasAbuttingEndTime (colNlt, targetTime, sched);
            if (abutNlt != null)
            {
              // System.out.println ("abutting end time");

              NamedLocationTime interimNlt = interpolateAbuttingTime (colNlt, targetTime, abutNlt);

              if (interimNlt != null) // no need to interpolate
              {
                  // remove the current one
                  ourVec.remove(ii);
                  ourVec.add (interimNlt);
              }

            }
*/
            break;
          }

        }

        if ( (! foundIt) && ( alreadyDone.get(schedOrgId) == null) )
        {
          alreadyDone.put (schedOrgId, new String() ); // only do each org once
          // System.out.println ("interpolating for " + schedOrgId );
          NamedLocationTime interimNlt = interpolateEndToStart (schedOrgId, targetTime, sched);
          if (interimNlt != null) // only null if it couldn't be interpolated
          {
            ourVec.add(interimNlt);
          }

        }

      }

      return (Collection) ourVec;

    }

    public static NamedLocationTime hasAbuttingEndTime (NamedLocationTime nlt, long targetTime, ScheduleImpl sched)
    {

      NamedLocationTime abutNlt = findClosestNlt  ( nlt.getUnit().getLabel(),
                                                    targetTime,
                                                    sched,
                                                    NEARESTENDTIME );

      if (abutNlt == null)
        return null;

      // if it's within a limited time window (like an hour)
      if (nlt.getStartTime() - abutNlt.getEndTime() < SAME_START_END_DELTA )
      {
        return abutNlt;
      }
      else
      {
        return null;
      }

    }

    public static NamedLocationTime interpolateAbuttingTime ( NamedLocationTime nlt,
                                                              long targetTime,
                                                              NamedLocationTime abutNlt )
    {

        NamedLocationTime interimNlt = null;

        //
        // if they are the same (within one hour)
        // for the original collection, find the start schedule elements that overlap specified time
        // the specified time and adjust their starting position based on a linear scaling
        //

        long timeDelta = nlt.getEndTime() - nlt.getStartTime();

        long adjustedStartTime = nlt.getStartTime() + (long) ((float) timeDelta * PERCENT_TRAVEL_TIME + 0.5f);  // add 10 percent of total occupation time as travel time
        if (targetTime > nlt.getStartTime() && targetTime < adjustedStartTime)
        {

          // we are in the travel time window
          String schedOrgId = nlt.getUnit().getLabel();

          interimNlt = interpolateEndToStart (nlt, abutNlt, adjustedStartTime, targetTime);
        }

        return interimNlt;

    }

    public static int NEARESTENDTIME = 0;
    public static int NEARESTSTARTTIME = 1;

    public static NamedLocationTime interpolateEndToStart ( NamedLocationTime iNlt,
                                                             NamedLocationTime startedNlt,
                                                             long adjustedStartTime,
                                                             long targetTime)
    {

      // part / whole, but not times 100
      float percentProg = (float) (targetTime - iNlt.getStartTime()) /
                          (float) (adjustedStartTime - iNlt.getStartTime());


      float newLat;
      float latDiff = iNlt.getLatitude() - startedNlt.getLatitude();
      latDiff *= percentProg;

      // it doesn't matter which Quadrant we're in, we add the latDiff (or lonDiff)
      newLat = startedNlt.getLatitude() + latDiff;

      float newLon;
      float lonDiff = iNlt.getLongitude() - startedNlt.getLongitude() ;
      lonDiff *= percentProg;
      newLon = startedNlt.getLongitude() + lonDiff;

/*
      System.out.print (" endLat is " + endNlt.getLatitude() );
      System.out.print (" newLat is " + newLat);
      System.out.println (" startLat is " + startNlt.getLatitude());

      System.out.print (" endLon is " + endNlt.getLongitude() );
      System.out.print (" newLon is " + newLon);
      System.out.println (" startlon is " + startNlt.getLongitude());
*/

//      NamedLocationTime retMe = makeInterimNLT ( iNlt, newLat, newLon, targetTime, adjustedStartTime);
      NamedLocationTime retMe;
      String orgKey = startedNlt.getUnit().getLabel();
      retMe = (NamedLocationTime) reUse.get ( orgKey );
      if (retMe == null)
      {
//        System.out.println ("making new interim NLT for " + orgKey );
        retMe = makeInterimNLT (iNlt, newLat, newLon, targetTime, adjustedStartTime);
        reUse.put (startedNlt.getUnit().getLabel(), retMe);
      }

      else
      {
//        System.out.println ("re-using interim NLT of " + orgKey );
        // just relocate the existing one
        retMe.setLatitude(newLat);
        retMe.setLongitude(newLon);
        retMe.setStartTime(targetTime);
        retMe.setEndTime(adjustedStartTime);
        OMGraphic omg = retMe.getUnit().getGraphic();
        if (omg instanceof CGMVecIcon)
          ( (CGMVecIcon) omg).setLocation(newLat, newLon, 0);
        if (omg instanceof VecIcon)
        {
          ( (VecIcon) omg).changeLocation (newLat, newLon );
        }

      }

      return retMe;

    }

    public static NamedLocationTime interpolateEndToStart (String orgId, long time, ScheduleImpl sched)
    {

      // find the closest end and start time
      NamedLocationTime endNlt = findClosestNlt (orgId, time, sched, NEARESTENDTIME);

      NamedLocationTime startNlt = findClosestNlt (orgId, time, sched, NEARESTSTARTTIME);
      if (endNlt == null || startNlt == null)
      {
//        System.out.println ("end or start NamedLocationTime was null on unit: " + orgId);
        // this "time" is at one or the other end of the range, can't be interpolated (extrapolated maybe but that's a different algorithm)

        // look for an overlapping NLT before we give up
        NamedLocationTime icoverit = findOverlappingNlt (orgId, time, sched);

        return icoverit;

      }

      // note: the time space we're working with here goes:
      // end of previous NamedLocationTime -> gap we're interpolating -> start of next NamedLocationTime
      // we code it thinking endNlt-> gap -> startNlt

      long timeDelta = startNlt.getStartTime() - endNlt.getEndTime();
      if (timeDelta <= 0)
      {
        // apparently this left before it arrived, hopefully the departure NLT is more accurate
        return startNlt;
      }

      NamedLocationTime abutNlt = hasAbuttingEndTime (startNlt, time, sched);
      if (abutNlt != null)
      {
        // System.out.println ("abutting end time");

        NamedLocationTime interimNlt = interpolateAbuttingTime (startNlt, time, abutNlt);

        if (interimNlt != null) // no need to interpolate
        {
          return (interimNlt);
        }

      }


      float percentProg = (float) (time - endNlt.getEndTime()) / (float) timeDelta; // part / whole, but not times 100

      float newLat;
      float latDiff = startNlt.getLatitude() - endNlt.getLatitude();
      latDiff *= percentProg;

      // it doesn't matter which Quadrant we're in, we add the latDiff (or lonDiff)
      newLat = endNlt.getLatitude() + latDiff;

      float newLon;
      float lonDiff = startNlt.getLongitude() - endNlt.getLongitude();
      lonDiff *= percentProg;
      newLon = endNlt.getLongitude() + lonDiff;

/*
      System.out.print (" endLat is " + endNlt.getLatitude() );
      System.out.print (" newLat is " + newLat);
      System.out.println (" startLat is " + startNlt.getLatitude());

      System.out.print (" endLon is " + endNlt.getLongitude() );
      System.out.print (" newLon is " + newLon);
      System.out.println (" startlon is " + startNlt.getLongitude());
*/

      NamedLocationTime retMe;
      String orgKey = startNlt.getUnit().getLabel();
      retMe = (NamedLocationTime) reUse.get ( orgKey );
      if (retMe == null)
      {
//        System.out.println ("making new interim NLT for " + orgKey );
        retMe = makeInterimNLT (startNlt, newLat, newLon, time, time + 100L );
        reUse.put (startNlt.getUnit().getLabel(), retMe);
      }
      else
      {
//        System.out.println ("re-using interim NLT of " + orgKey );
        // just relocate the existing one
        retMe.setLatitude(newLat);
        retMe.setLongitude(newLon);
        retMe.setStartTime(time);
        retMe.setEndTime(time + 100L);
        OMGraphic omg = retMe.getUnit().getGraphic();
        if (omg instanceof CGMVecIcon)
          ( (CGMVecIcon) omg).setLocation(newLat, newLon, 0);
        if (omg instanceof VecIcon)
        {
          ( (VecIcon) omg).changeLocation (newLat, newLon );
        }

      }

      return retMe;

    }

    public static NamedLocationTime makeInterimNLT ( NamedLocationTime baseNlt,
                                                     float latPos,
                                                     float lonPos,
                                                     long startTime,
                                                     long endTime )
    {


      String orgId = new String ( baseNlt.getUnit().getLabel() );

      // we set time range for this at current time until adjustedStartTime
      NamedLocationTime interimNlt = new NamedLocationTime ( orgId,
                                             Float.toString ( latPos ),
                                             Float.toString ( lonPos ),
                                             Long.toString (startTime ),
                                             Long.toString ( endTime ),
                                             null );
        OMGraphic omg = baseNlt.getUnit().getGraphic();

      if (omg instanceof CGMVecIcon)
      {
        CGMVecIcon cvi = (CGMVecIcon) omg;
        CGMVecIcon newCvi = cvi.makeAnother();

        newCvi.setLocation(latPos, lonPos, 0);

        // VecIcon functions for positioning label
        newCvi.initLocation(latPos, lonPos );
        newCvi.initLabel( orgId );

        interimNlt.setUnit( new Unit (orgId, newCvi, new Hashtable() ) );
      }

      else
      {
        VecIcon vIcon = new VecIcon( latPos, lonPos, java.awt.Color.white);
        vIcon.setLabel( ((VecIcon)omg).getLabel());
        vIcon.setMessage ( ((VecIcon) omg).getMessage() );
        vIcon.setVisible( ((VecIcon) omg).isVisible() );
        interimNlt.setUnit( new Unit (orgId, vIcon, new Hashtable() ) );
      }

      return interimNlt;
   }
   
}