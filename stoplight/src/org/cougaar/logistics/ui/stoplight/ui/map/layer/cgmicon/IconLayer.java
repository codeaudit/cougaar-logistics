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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.bbn.openmap.Layer;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.event.ProjectionEvent;

import org.cougaar.logistics.ui.stoplight.ui.map.layer.IntelVecIcon;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.NonMilSalesVecIcon;

//import org.cougaar.logistics.ui.stoplight.ui.map.layer.ArmoredVecIcon;
//import org.cougaar.logistics.ui.stoplight.ui.map.layer.InfantryVecIcon;


/**
 * Layer objects are components which can be added to the MapBean to
 * make a map.
 * <p>
 * Layers implement the ProjectionListener interface to listen for
 * ProjectionEvents.  When the projection changes, they may need to
 * refetch, regenerate their graphics, and then repaint themselves
 * into the new view.
 */
public class IconLayer extends Layer {

    protected OMGraphicList graphics;

    protected int red,green,blue;

    /**
     * Construct the layer.
     */
    public IconLayer () {
	super();
	graphics = new OMGraphicList(10);
	createGraphics(graphics);
    }

    /**
     * Sets the properties for the <code>Layer</code>.  This allows
     * <code>Layer</code>s to get a richer set of parameters than the
     * <code>setArgs</code> method.
     * @param prefix the token to prefix the property names
     * @param props the <code>Properties</code> object
     */
    public void setProperties(String prefix, java.util.Properties props) {
	super.setProperties(prefix, props);
    }

    /**
     * Invoked when the projection has changed or this Layer has been
     * added to the MapBean.
     * @param e ProjectionEvent
     */
    public void projectionChanged (ProjectionEvent e) {
	graphics.generate(e.getProjection());
	repaint();
    }

    /**
     * Paints the layer.
     * @param g the Graphics context for painting
     */
    public void paint (Graphics g) {
	graphics.render(g);
    }

    /**
     * Create graphics.
     */
    protected void createGraphics (OMGraphicList list)
    {
	    // NOTE: all this is very non-optimized...

      // Create the icons
      try
      {
        // Coded icons, i.e. objects that were written to display an icon
        IntelVecIcon intelIcon = new IntelVecIcon ( (float) 11.0, (float) 0.0, Color.lightGray );
        list.add(intelIcon);

        NonMilSalesVecIcon nonMilIcon = new NonMilSalesVecIcon ( (float) 11.0, (float) 0.3, Color.lightGray );
        list.add(nonMilIcon);

        // CGMIcons
        OMCGMIcons cgmicons = new OMCGMIcons ("../config/cgmload.txt");

        CGMVecIcon icon = new CGMVecIcon ( (OMCGM) cgmicons.get ("light infantry"), 10.0f, 0.0f );
        icon.cgmIcon.changeColor(new Color(128,224,255),createRandomColor());
        list.add(icon);
        icon = new CGMVecIcon ( (OMCGM) cgmicons.get ("armor"), 10.1f, 0.0f);
         icon.cgmIcon.changeColor(new Color(128,224,255),createRandomColor());
        list.add (icon);
        icon =new CGMVecIcon ( (OMCGM) cgmicons.get ("mechanized infantry"), 10.0f, 1.0f );
         icon.cgmIcon.changeColor(new Color(128,224,255),createRandomColor());
        list.add (icon);

        icon =new CGMVecIcon ( (OMCGM) cgmicons.get ("infantry"), 10.1f, 1.0f );
         icon.cgmIcon.changeColor(new Color(128,224,255),createRandomColor());
        list.add (icon );

        // second infantry
        icon = new CGMVecIcon ( (OMCGM) cgmicons.get ("infantry"), 10.1f, 0.8f );
         icon.cgmIcon.changeColor(new Color(128,224,255),createRandomColor());
        list.add (icon );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("armored cavalry"), 10.2f, 1.0f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("recon"), 10.3f, 1.0f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("airborne"), 10.4f, 0.0f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("air assault"), 10.5f, 1.0f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("field artillery"), 10.5f, 1.1f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("air defense"), 10.3f, 1.2f ) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("anti-armor"), 10.2f, 0.9f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("aviation"), 10.2f, -0.2f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("attack helicopter"), 10.0f, -0.1f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("air cavalry"), 10.3f, 0.1f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("engineer"), 10.2f, 0.3f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("chemical"), 10.3f, -0.3f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("signal"), 10.2f, -0.3f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("intelligence"), 10.4f, -0.3f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("transport"), 10.35f, -0.5f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("supply"), 10.25f, -0.5f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("maintenance"), 10.2f, -0.7f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("subsistence"), 10.4f, -1.0f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("clothing"), 10.2f, -2.0f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("petroleum"), 10.35f, -0.9f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("construction materials"), 10.2f, -0.9f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("ammunition"), 9.9f, -2.2f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("personal demand"), 10.1f, -2.2f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("medical"), 10.0f, -2.4f ) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("major end items"), 10.0f, -2.0f) );

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("medical supply"), 9.7f, 0.0f ));

        list.add (new CGMVecIcon ( (OMCGM) cgmicons.get ("nonmilitary supply"), 9.7f, 1.0f) );

    }

    catch (java.io.IOException ioexc)
    {
        System.err.println (ioexc.toString());
        ioexc.printStackTrace();
    }

    catch (Exception exc)
    {
        System.err.println (exc.toString());
        exc.printStackTrace();
    }


  }

  private Color createRandomColor()
  {
   red = (int)(Math.random()*255.0);
   green = (int)(Math.random()*255.0);
   blue = (int)(Math.random()*255.0);
   return (new Color(red,green,blue));
  }

}
