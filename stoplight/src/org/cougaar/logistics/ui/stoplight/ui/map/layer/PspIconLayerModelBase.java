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
import java.net.*;


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

class PspIconLayerModelBase
{
    OMGraphicList markers = new OMGraphicList();

    Hashtable units=new Hashtable();
    Hashtable allIcons = new Hashtable();

    void resetMarkers() {
      markers=new OMGraphicList();   // do not clear()
      units=new Hashtable();         // do not clear()
   }


  OMGraphic findClosest(int x, int y, float limit) {
    return markers.findClosest(x, y, limit);
  }

    UnitTypeDictionary unitTypeDictionary=new UnitTypeDictionary();

/*
        private OMGraphic makeIconGraphic(float lat, float lon, Color color,
                                          String type)
        {
            OMGraphic ret= new VecIcon(lat, lon, color);
            return ret;
        }

*/        
    
         protected String getUnitType(String unitName)
         {
            return unitTypeDictionary.getUnitType(unitName);
         }


        Unit getUnit(OMGraphic g) {
            return (Unit) units.get(g);
        }

        Unit setUnit(Unit unit) { return (Unit) units.put(unit.getGraphic(), unit); }


    String uriString=null;

    PspIconLayerModelBase()
    {
        OMGraphic omgraphic;

        uriString = Environment.get("pspicon.locations.url");

//        System.out.println("PspIconLayerModelBase *** uriString from environment is "+uriString);
        if (uriString==null)
        {
          uriString = new String();

            System.err.println("No PSP url provided in properties file, icons not displayed");
        }

    }



   Iterator markerIterator() {
      java.util.List vec=markers.getTargets();
      if (vec==null) {
        vec=new Vector();
      }
            return vec.iterator();
    }
  }




