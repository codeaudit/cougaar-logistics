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

import java.util.*;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.bbn.openmap.omGraphics.*;

import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.proj.LineType;

public class OMFixedText extends OMText
{

  private float fixedTextSize;
  public OMFixedText()
  {
    super();
  }

  public OMFixedText (float lat, float lon, float fixedSize, int latOffs, int lonOffs, String text, int just)
  {
    super (lat, lon, latOffs, lonOffs, text, just);

    fixedTextSize = fixedSize;
  }

  public OMFixedText (float lat, float lon, float fixedSize, int latOffs, int lonOffs, String text, Font font, int just)
  {
    super (lat, lon, latOffs, lonOffs, text, font, just);

    fixedTextSize = fixedSize;
  }

  // swiped from OMText and modified to keep text same size
  public synchronized boolean generate(Projection proj)
  {
        // HACK synchronized because of various race conditions that need to
        // be sorted out.

        if (proj == null) {
            //Debug.message("omgraphic", "OMText: null projection in generate!");
            return false;
        }

        // flush the cached information about the bounding box.
        polyBounds = null;

        // Although it most definately has bounds, OMText is considered a
        // point object by the projection code.  We need to check to make
        // sure the point is plot-able: if not then don't display it.  This
        // might occur, for instance, if we're using the Orthographic and the
        // point is on the other side of the world.
        switch (renderType) {
        case RENDERTYPE_XY:
            pt = point;
            break;
        case RENDERTYPE_OFFSET:
            if (!proj.isPlotable(lat, lon)) {
/*                if (Debug.debugging("omGraphics"))
                    System.err.println(
                        "OMText.generate(): offset point is not plotable!"); */
                setNeedToRegenerate(true);//so we don't render it!
                return false;
            }
            pt = proj.forward(lat, lon);
            pt.translate(point.x, point.y);
            adjustTextFont (proj);
            break;
        case RENDERTYPE_LATLON:
            if (!proj.isPlotable(lat, lon)) {
/*                if (Debug.debugging("omGraphics"))
                    System.err.println(
                        "OMText.generate(): llpoint is not plotable!"); */
                setNeedToRegenerate(true);//so we don't render it!
                return false;
            }
            pt = proj.forward(lat, lon);
            adjustTextFont (proj);
            if (getNeedToRegenerate())
              return false;
            else
              break;
              
        case RENDERTYPE_UNKNOWN:
            System.err.println(
                "OMText.render.generate(): invalid RenderType");
            return false;
        }

// commented out to compile with 3.6.2
/*
        if (f == null) {
            f = DEFAULT_FONT;
        }
*/
        setNeedToRegenerate(false);
        return true;
    }

  private void adjustTextFont (Projection proj)
  {
//    System.out.println ("adjusting text font");
    
    LatLonPoint llp1 = new LatLonPoint (lat, lon);
    LatLonPoint llp2 = new LatLonPoint (lat + fixedTextSize, lon + fixedTextSize);

    ArrayList xys = proj.forwardLine(llp1, llp2, LineType.Straight, -1);

//    int x[] = (int[]) xys.elementAt(0);
    int y[] = (int[]) xys.get(1);

//        System.out.println ("OMFixedText y range: " + (y[0] - y[1]) );
    int fontSize = (y[0] - y[1]);         // ( x[1] - x[0], y[0] - y[1]);
    if (fontSize > 1)
      f = new Font ( "SansSerif", Font.BOLD, fontSize / 2 );
    else
    {
      setNeedToRegenerate (true); // so we don't render it!
    }

  }


}
