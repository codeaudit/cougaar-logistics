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
package org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon;

import java.util.*;

import java.awt.Point;

import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.proj.LineType;

public class OMCGMbyVisio extends OMCGM
{

  private final double VISIO_SCALE_FACTOR=0.8;
  private final float VISIO_OFFSET=(float)0.0002;

  public OMCGMbyVisio()
  {
  }

  public OMCGMbyVisio(OMCGM copyMe)
  {

    if (copyMe.unitSize != null)
         this.unitSize = new String (copyMe.unitSize);

    this.cgmFileName = new String (copyMe.cgmFileName);
    this.omcgmdisp = (OpenMapCGMDisplay) (copyMe.omcgmdisp.makeAnother());

  }

  public boolean generate (Projection proj)
  {

    oldProjection = proj;  // I'm sure this is a strict OpenMap no no

    /**
     * Establish new x and y origin based on projection.
     *
     * @param proj Projection
     * @return true if generate was successful
     */

    int i, npts;

    if (proj == null)
    {
      System.err.println ("omgraphic: OMCGMbyVisio: null projection in generate!");
      return false;
    }

    // forward project the radian point
    Point origin = proj.forward( latOrigin + VISIO_OFFSET, lonOrigin + VISIO_OFFSET, new Point(0,0), true );//radians

    xOrigin = origin.x;
    yOrigin = origin.y;

    LatLonPoint llp1 = new LatLonPoint (latOrigin, lonOrigin);
    LatLonPoint llp2 = new LatLonPoint (latOrigin + 0.1, lonOrigin + 0.1);

    ArrayList xys = proj.forwardLine(llp1, llp2, LineType.Straight, -1);

    int x[] = (int[]) xys.get(0);
    int y[] = (int[]) xys.get(1);

//        System.out.println ("x range: " + (x[1] - x[0]) );
//        System.out.println ("y range: " + (y[0] - y[1]) );

    omcgmdisp.setOrigin(xOrigin, yOrigin);

    // Visio makes the icons too big scale them down further
    double adjuster;

    adjuster = (double) x[1] - (double) x[0];
    int adjustedX = (int) ((adjuster * VISIO_SCALE_FACTOR) + 0.5);

    adjuster = (double) y[0] - (double) y[1];
    int adjustedY = (int) ((adjuster * VISIO_SCALE_FACTOR) + 0.5);

//        System.out.println ("adjusted x range: " + adjustedX );
//        System.out.println ("adjusted y range: " + adjustedY );

    omcgmdisp.scale( adjustedX, adjustedY);

//    if (unitSize != null)
//    {
//      drawUnitSizeDesignation (unitSize, latOrigin, lonOrigin);
//      ogl.generate(proj);
//    }

    setNeedToRegenerate(false);

    return true;


  }


   public void updateScale ()
   {

     // set our new origin so that as we scale we don't appear to move
//     System.out.println ("\npermScale is: " + permScale);

//     System.out.print ( "changing degLat from: " + degLat );
//     System.out.println ( " \tchanging degLon from: " + degLon);

     // degL.. + degL..Offset will give us the original position of the icon,
     // from there we offset appropriately
     float origLonPos = degLon + degLonOffset;
     float origLatPos = degLat + degLatOffset;

     float newLat = 0.0f, newLon = 0.0f;
     if (permScale > 1.0f)
     {
       // get the new positions
       newLat = origLatPos - (LAT_OFFSET * permScale);
       newLon = origLonPos - (LON_OFFSET * permScale);
     }

     else if (permScale < 1.0)
     {
       // get the new positions
       newLat = origLatPos + (LAT_OFFSET * permScale);
       newLon = origLonPos + (LON_OFFSET * permScale);
     }

     else
     {
       // permScale = 1.0f exactly
       newLat = origLatPos;
       newLon = origLonPos;
     }


     // openmap works from -180.0 to +180.0
     if (newLon < -180.0f)
       newLon = 360.0f + newLon; // put it on the other side of the 180 degree line
     else if (newLon > 180.0f)
       newLon = newLon - 360.0f;

     if (newLat < -180.0f)
       newLat = 360.0f + newLat;
     else if (newLat > 180.0f)
       newLat = newLat - 360.0f;

     // prepare for next round of calculations
     degLatOffset = origLatPos - newLat;
     degLonOffset = origLonPos - newLon;

     degLat = newLat;
     degLon = newLon;

//     System.out.print ("\tto: " + degLat);
//     System.out.println (" \t\tto: " + degLon);

     setLocation (degLat, degLon, DECIMAL_DEGREES);

     generate (oldProjection);

     LatLonPoint llp1 = new LatLonPoint (latOrigin, lonOrigin);
     float toLat = latOrigin + (permScale * 0.3f);
     if (toLat > 180.0f)
       toLat -= 360.0f;
     else if (toLat < -180.0f)
       toLat += 360.0f;

     float toLon = lonOrigin + (permScale * 0.3f);
     if (toLon > 180.0f)
       toLon -= 360.0f;
     else if (toLon < -180.0f)
       toLon += 360.0f;

     LatLonPoint llp2 = new LatLonPoint (toLat, toLon);

     ArrayList xys = oldProjection.forwardLine(llp1, llp2, LineType.Straight, -1);

      int x[] = (int[]) xys.get(0);
      int y[] = (int[]) xys.get(1);

//        System.out.println ("x range: " + (x[1] - x[0]) );
//        System.out.println ("y range: " + (y[0] - y[1]) );
/*
      xrange = x[1] - x[0];
      yrange = y[0] - y[1];

      omcgmdisp.scale( xrange, yrange);
*/

   // Visio makes the icons too big scale them down further
    double adjuster;

    adjuster = (double) x[1] - (double) x[0];
    int adjustedX = (int) ((adjuster * VISIO_SCALE_FACTOR) + 0.5);

    adjuster = (double) y[0] - (double) y[1];
    int adjustedY = (int) ((adjuster * VISIO_SCALE_FACTOR) + 0.5);

//        System.out.println ("adjusted x range: " + adjustedX );
//        System.out.println ("adjusted y range: " + adjustedY );

    omcgmdisp.scale( adjustedX, adjustedY);

//      if (unitSize != null)
//      {
//        drawUnitSizeDesignation (unitSize, latOrigin, lonOrigin);
//        ogl.generate(oldProjection);
//      }

   }



  public OMCGM makeAnother()
  {

     try
     {

       OMCGMbyVisio mycopy = new OMCGMbyVisio();
       if (this.unitSize != null)
         mycopy.unitSize = new String (this.unitSize);
       mycopy.cgmFileName = new String (this.cgmFileName);
       mycopy.omcgmdisp = (OpenMapCGMDisplay) (this.omcgmdisp.makeAnother());

       return mycopy;

     }

     catch (Throwable ioexc)
     {
       System.err.println (ioexc.toString());
       ioexc.printStackTrace();
     }

     return null;

   }

} 