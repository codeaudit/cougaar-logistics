/* **********************************************************************
 *
 *  Clark Software Engineering, Ltd.
 *  5100 Springfield St. Ste 308
 *  Dayton, OH 45431-1263
 *  (937) 256-7848
 *
 *  Copyright (C) 2001
 *  This software is subject to copyright protection under the laws of
 *  the United States and other countries.
 *
 */

package org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon;

import java.util.*;

import java.io.DataInputStream;
import java.io.FileInputStream;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Color;

import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.cgm.*;

import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMCircle;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.omGraphics.OMPoly;

import com.bbn.openmap.proj.Projection;

import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.proj.ProjMath;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.proj.LineType;


public class OMCGM extends OMGraphic
{

  public static final String SQUAD_SIZE="squad";
  public static final String SECTION_SIZE="section";
  public static final String PLATOON_SIZE="platoon";
  public static final String COMPANY_SIZE="company";
  public static final String BATTALION_SIZE="battalion";
  public static final String REGIMENT_SIZE="regiment";
  public static final String BRIGADE_SIZE="brigade";
  public static final String DIVISION_SIZE="division";
  public static final String CORPS_SIZE="corps";
  public static final String ARMY_SIZE="army";

     // (4 - Company, 5- Battalion, 6-Regiment/Group, 7-Brigade, 8 - Division, 9- Corps, 10 - Army.
  public static final int COMPANY_FLAG = 4;
  public static final int BATTALION_FLAG = 5;
  public static final int REGIMENT_FLAG = 6;
  public static final int GROUP_FLAG = 6;
  public static final int BRIGADE_FLAG = 7;
  public static final int DIVISION_FLAG = 8;
  public static final int CORPS_FLAG = 9;
  public static final int ARMY_FLAG = 10;

  protected OpenMapCGMDisplay omcgmdisp;

  protected float latOrigin, lonOrigin;  // in radians
  protected float degLat, degLon; // in decimal degrees

  protected int xOrigin, yOrigin;
  private int xrange = 0, yrange = 0;

  protected static float permScale = 1.0f;

  protected final float LON_OFFSET = 0.03f; // adjustment to keep icon from apearing to move away
  protected final float LAT_OFFSET = 0.03f; // adjustment to keep icon from apearing to move away

  protected float degLonOffset = 0.0f; // adjustment to keep icon from apearing to move away
  protected float degLatOffset = 0.0f; // adjustment to keep icon from apearing to move away

  // if we need to rescale we need this
  protected Projection oldProjection = null;

  // keep track of the decorations on the CGM display
  // like unit strength symbology, labels, and HUD-type clutter
  protected OMGraphicList ogl = new OMGraphicList();

  protected String cgmFileName;
  protected String unitSize = null;

  public OMCGM ()
  {
    super(RENDERTYPE_LATLON, LINETYPE_UNKNOWN, DECLUTTERTYPE_NONE);
  }

  public OMCGM( String cgmFile) throws java.io.IOException
  {

    super(RENDERTYPE_LATLON, LINETYPE_UNKNOWN, DECLUTTERTYPE_NONE);

    cgmFileName = new String (cgmFile);

    CGM cgm = new CGM ();
    cgm.read ( new DataInputStream ( new FileInputStream (cgmFile) ) );
    omcgmdisp = new OpenMapCGMDisplay (cgm);

  }


  public OMCGM( String cgmFile, String unitSizeDesignation) throws java.io.IOException
  {
    super(RENDERTYPE_LATLON, LINETYPE_UNKNOWN, DECLUTTERTYPE_NONE);

    cgmFileName = new String (cgmFile);
    unitSize = new String (unitSizeDesignation);

    CGM cgm = new CGM ();

    cgm.read ( new DataInputStream ( new FileInputStream (cgmFile) ) );
    omcgmdisp = new OpenMapCGMDisplay (cgm);

  }

    //
    // Swiped from OMPoly.java ... heh-heh-heh
    //

    /**
     * Set the location based on a latitude, longitude, and some xy
     * points.
     * The coordinate mode and the polygon setting are the same as in
     * the constructor used.  This is for RENDERTYPE_OFFSET polys.
     *
     * @param latPoint latitude in decimal degrees
     * @param lonPoint longitude in decimal degrees
     * @param units radians or decimal degrees.  Use OMGraphic.RADIANS
     * or OMGraphic.DECIMAL_DEGREES
     */
  public void setLocation(float latPoint, float lonPoint, int units)
  {

    if (units == OMGraphic.DECIMAL_DEGREES)
    {

        degLat = latPoint;
        degLon = lonPoint;

        latOrigin = ProjMath.degToRad(latPoint);
        lonOrigin = ProjMath.degToRad(lonPoint);
    }

    else // already in radians
    {
        latOrigin = latPoint;
        lonOrigin = lonPoint;

        degLat = ProjMath.radToDeg(latPoint);
        degLon = ProjMath.radToDeg(lonPoint);
    }

      setNeedToRegenerate(true);
      setRenderType(RENDERTYPE_LATLON);

  }


  public boolean generate (Projection proj)
  {
      oldProjection = proj;  // I'm sure this is a strict OpenMap no no

//   System.out.println ("generate, degLon is: " + degLon);

    /**
     * Establish new x and y origin based on projection.
     *
     * @param proj Projection
     * @return true if generate was successful
     */

    int i, npts;

    if (proj == null)
    {
      System.err.println ("omgraphic: OMCGM: null projection in generate!");
      return false;
    }

    // forward project the radian point
    Point origin = proj.forward( latOrigin, lonOrigin, new Point(0,0), true );//radians

    xOrigin = origin.x;
    yOrigin = origin.y;

    LatLonPoint llp1 = new LatLonPoint (latOrigin, lonOrigin);
    // LatLonPoint llp2 = new LatLonPoint (latOrigin + 0.1, lonOrigin + 0.1);
    LatLonPoint llp2 = new LatLonPoint (latOrigin + (permScale * 0.3f), lonOrigin + (permScale * 0.3f));

    ArrayList xys = proj.forwardLine(llp1, llp2, LineType.Straight, -1);

    int x[] = (int[]) xys.get(0);
    int y[] = (int[]) xys.get(1);

//        System.out.println ("x range: " + (x[1] - x[0]) );
//        System.out.println ("y range: " + (y[0] - y[1]) );

    omcgmdisp.setOrigin(xOrigin, yOrigin);

//    System.out.println ("in generate, permScale is " + permScale);
    xrange = x[1] - x[0];
    yrange = y[0] - y[1];

//    int xscale = (int) (((float) xrange) * permScale + 0.05f);
//    int yscale = (int) (((float) xrange) *  permScale + 0.05f);
//    System.out.println ("xscale is: " + xscale);
//    System.out.println ("yscale is: " + yscale);
//    omcgmdisp.scale( xrange, yscale);
    omcgmdisp.scale( xrange, yrange);

//    if (unitSize != null)
//    {
//      drawUnitSizeDesignation (unitSize, latOrigin, lonOrigin);
//      ogl.generate(proj);
//    }

    setNeedToRegenerate(false);

    return true;


  }


  public void render (Graphics g)
  {

//     System.out.println ("render, degLon is: " + degLon);

     if (getNeedToRegenerate() || !isVisible())
       return;

     if (unitSize != null)
     {
       ogl.render(g);
     }

     omcgmdisp.paint(g);

  }

   public float distance (int x, int y)
   {
     double asquared = Math.pow( (double) (xOrigin - x), 2.0);
     double bsquared = Math.pow( (double) (yOrigin - y), 2.0);
     double dist = Math.pow ((asquared + bsquared), 0.5);

     return (float) dist;
   }


   public static void changePercentScale ( float percChange )
   {

//     System.out.println ("percChange is: " + percChange);

     if (percChange > 0.0f)
     {
       if (percChange > 1.0f)
         permScale *= percChange;
       else
         permScale += (permScale * percChange);
     }

     else
     {
       percChange *= -1.0;
       if (percChange > 1.0f)
         permScale /= percChange;
       else
         permScale -= (permScale * percChange);
     }

//     System.out.println ("permScale is: " + permScale);
     if (permScale < 0.1f)
       permScale = 0.1f;  // never go below zero

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

      xrange = x[1] - x[0];
      yrange = y[0] - y[1];

      omcgmdisp.scale( xrange, yrange);

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

       OMCGM mycopy = new OMCGM();
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

   protected void drawUnitSizeDesignationNot (String unitSize, float xCoord, float yCoord)
   {

     final float LOW_LAT = 0.14f;
     final float HIGH_LAT = 0.16f; // 0.23f;
     final float CIRCLE_START_LAT = LOW_LAT + 0.02f; // 0.23f Circles start slightly higher

     final float BGBOXLATLOW = 0.13f;   // 0.20f original
     final float BGBOXLATHIGH = 0.17f; // 0.24f
     final float BGBOXLONLEFT = 0.05f;
     final float BGBOXLONRIGHT = 0.25f;

     // (4 - Company, 5- Battalion, 6-Regiment/Group, 7-Brigade, 8 - Division, 9- Corps, 10 - Army.

     ogl.clear(); // always start fresh

//     System.out.println ( "unit size is: " + unitSize);
     if (xCoord < 0.0)
       xCoord = (float) 360.0 - xCoord;

     if (yCoord < 0.0)
       yCoord = (float) 360.0 - yCoord;


     if ( unitSize.equals( SQUAD_SIZE) )
     {

       OMCircle dot1= new OMCircle ( degLat + (CIRCLE_START_LAT * permScale),
                                     degLon + (0.15f * permScale),
                                     (0.02f * permScale));

 /*
       try {
       dot1.setlinepaint ( Color.black);
       } catch (Exception ex) {
	   ex.printStackTrace();
       }
*/
       //dot1.setfillpaint(Color.black);

       ogl.add(dot1);


     }

     else if ( unitSize.equals( SECTION_SIZE) )
     {

       OMCircle dot1= new OMCircle ( degLat + (CIRCLE_START_LAT * permScale),
                                     degLon + (0.12f * permScale),
                                     (0.02f * permScale) );
       OMCircle dot2= new OMCircle ( degLat + (CIRCLE_START_LAT * permScale),
                                     degLon + (0.19f * permScale),
                                     (0.02f * permScale) );
/* openmap 4.0 specific
       try {
	   dot1.setlinepaint ( Color.black);
	   dot2.setlinepaint ( Color.black);
       } catch (Exception ex) {
	   ex.printStackTrace();
       }
*/
       //dot1.setfillpaint(Color.black);
       //dot2.setfillpaint(Color.black);

       ogl.add(dot1);
       ogl.add(dot2);

     }

     else if ( unitSize.equals( PLATOON_SIZE) )
     {

       OMCircle dot1= new OMCircle ( degLat + (CIRCLE_START_LAT * permScale),
                                     degLon + (0.08f * permScale),
                                     (0.02f * permScale) );
       OMCircle dot2= new OMCircle ( degLat + (CIRCLE_START_LAT * permScale),
                                     degLon + (0.15f * permScale),
                                     (0.02f * permScale) );
       OMCircle dot3= new OMCircle ( degLat + (CIRCLE_START_LAT * permScale),
                                     degLon + (0.22f * permScale),
                                     (0.02f * permScale) );

/* openmap 4.0 specific
       try {
       dot1.setlinepaint ( Color.black);
       dot2.setlinepaint ( Color.black);
       dot3.setlinepaint ( Color.black);
       } catch (Exception ex) {
	   ex.printStackTrace();
       }

       dot1.setfillpaint(Color.black);
       dot2.setfillpaint(Color.black);
       dot3.setfillpaint(Color.black);
*/
       ogl.add(dot1);
       ogl.add(dot2);
       ogl.add(dot3);

     }

     else if ( unitSize.equals (COMPANY_SIZE) )
     {

       float lineLon = degLon + (0.15f * permScale);
       OMLine line1 = new OMLine ( degLat + (LOW_LAT * permScale),
                                   lineLon,
                                   degLat + (HIGH_LAT * permScale),
                                   lineLon,
                                   OMLine.LINETYPE_STRAIGHT );

       ogl.add(line1);

     }

     else if ( unitSize.equals (BATTALION_SIZE) )
     {

       float lineLon = degLon + (0.12f * permScale);
//       OMLine line1 = new OMLine (degLat + ((float) 0.0675 * permScale), lineLon, degLat + ((float) 0.085 * permScale), lineLon, OMLine.LINETYPE_STRAIGHT );
       OMLine line1 = new OMLine ( degLat + (LOW_LAT * permScale),
                                   lineLon,
                                   degLat + (permScale * HIGH_LAT),
                                   lineLon,
                                   OMLine.LINETYPE_STRAIGHT );

       lineLon = degLon + (0.18f * permScale);
       OMLine line2 = new OMLine ( degLat + (LOW_LAT * permScale),
                                   lineLon,
                                   degLat + (HIGH_LAT * permScale),
                                   lineLon,
                                   OMLine.LINETYPE_STRAIGHT );

       ogl.add (line1);
       ogl.add (line2);

     }

     else if ( unitSize.equals (REGIMENT_SIZE) )
     {

       float lineLon = degLon + (0.10f * permScale);
       OMLine line1 = new OMLine (degLat + (LOW_LAT * permScale),
                                  lineLon,
                                  degLat + (permScale * HIGH_LAT),
                                  lineLon,
                                  OMLine.LINETYPE_STRAIGHT );

       lineLon = degLon + (0.145f * permScale);
       OMLine line2 = new OMLine ( degLat + (LOW_LAT * permScale),
                                   lineLon,
                                   degLat + (permScale * HIGH_LAT),
                                   lineLon,
                                   OMLine.LINETYPE_STRAIGHT );

       lineLon = degLon + (0.19f * permScale);
       OMLine line3 = new OMLine ( degLat + (LOW_LAT * permScale),
                                   lineLon,
                                   degLat + (permScale * HIGH_LAT),
                                   lineLon,
                                   OMLine.LINETYPE_STRAIGHT );

       ogl.add(line1);
       ogl.add(line2);
       ogl.add(line3);

     }

     else if ( unitSize.equals (BRIGADE_SIZE) )
     {

       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.15f * permScale) );

     }
     else if ( unitSize.equals (DIVISION_SIZE) )
     {

       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.12f * permScale) );
       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.18f * permScale) );

     }

     else if ( unitSize.equals (CORPS_SIZE) )
     {

       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.10f * permScale) );
       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.15f * permScale) );
       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.20f * permScale) );

     }

     else if ( unitSize.equals (ARMY_SIZE) )
     {

       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.06f * permScale) );
       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.12f * permScale) );
       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.18f * permScale) );
       drawX ( degLat + (LOW_LAT * permScale), degLon + (0.24f * permScale) );

     }


     // last we want to draw a white background box so we can see the echelon markers
     float polypoints[] = { degLat + (BGBOXLATLOW * permScale), degLon + (BGBOXLONLEFT * permScale),
                            degLat + (BGBOXLATLOW * permScale), degLon + (BGBOXLONRIGHT * permScale),
                            degLat + (BGBOXLATHIGH * permScale), degLon + (BGBOXLONRIGHT * permScale),
                            degLat + (BGBOXLATHIGH * permScale), degLon + (BGBOXLONLEFT * permScale),
                            degLat + (BGBOXLATLOW * permScale), degLon + (BGBOXLONLEFT * permScale)
                            };
     OMPoly bgbox = new OMPoly (polypoints, OMGraphic.DECIMAL_DEGREES, OMGraphic.LINETYPE_STRAIGHT );
     bgbox.setFillPaint(Color.white);

     ogl.add (bgbox);

   }

   private void drawX (float lat, float lon)
   {

      float llat = lat;
      float ulat = lat + (0.02f * permScale);

      float llon = lon - (0.01f * permScale);
      float rlon = lon + (0.01f * permScale);

      OMLine line1 = new OMLine (llat, llon, ulat, rlon, OMLine.LINETYPE_STRAIGHT );

      OMLine line2 = new OMLine (llat, rlon, ulat, llon, OMLine.LINETYPE_STRAIGHT );

      ogl.add (line1);
      ogl.add (line2);

   }

   public void showCGMCommands()
   {
    omcgmdisp.showCGMCommands();
    }

    public void changeColor(Color oldc, Color newc)
    {
      omcgmdisp.changeColor (oldc, newc);
    }

    public void setEchelon (int echelon)
    {

     // (4 - Company, 5- Battalion, 6-Regiment/Group, 7-Brigade, 8 - Division, 9- Corps, 10 - Army.

      switch (echelon)
      {
        case COMPANY_FLAG:
          unitSize = COMPANY_SIZE;
          break;

        case BATTALION_FLAG:
          unitSize = BATTALION_SIZE;
          break;

        case REGIMENT_FLAG: // same number as GROUP_FLAG
          unitSize = REGIMENT_SIZE;
          break;

        case BRIGADE_FLAG:
          unitSize = BRIGADE_SIZE;
          break;

        case DIVISION_FLAG:
          unitSize = DIVISION_SIZE;
          break;

        case CORPS_FLAG:
          unitSize = CORPS_SIZE;
          break;

        default:
        // case ARMY_FLAG:
          unitSize = ARMY_SIZE;
          break;

        }
     }

}
