/* **********************************************************************
 *
 * adapted with permission from:
 *  BBNT Solutions LLC, A part of GTE
 *  10 Moulton St.
 *  Cambridge, MA 02138
 *  (617) 873-2000
 *
 * by:
 *   Clark Software Engineering
 *   5100 Springfield St. #308
 *   Dayton, OH 45431-1263
 *
 *  Copyright (C) 2001
 *  This software is subject to copyright protection under the laws of
 *  the United States and other countries.
 *
 * **********************************************************************
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
import com.bbn.openmap.gui.OMToolSet;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.event.ProjectionEvent;
import com.bbn.openmap.Environment;

import com.bbn.openmap.event.*;

import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.map.app.*;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;
import org.cougaar.logistics.ui.stoplight.ui.util.*;

/**
 * Layer objects are components which can be added to the MapBean to
 * make a map.
 * <p>
 * Layers implement the ProjectionListener interface to listen for
 * ProjectionEvents.  When the projection changes, they may need to
 * refetch, regenerate their graphics, and then repaint themselves
 * into the new view.
 */

public class PspIconLayerBase extends Layer implements MapMouseListener {

    private Class uiLaunchClass = null;
    private Object uiLaunchPopup = null;
    protected OMGraphicList graphics;

        ColorCodeUnit ccuA=new ColorCodeUnit();
        ColorCodeUnit ccuB=new ColorCodeUnit();
        ColorCodeUnit curCcu=ccuB;

    /**
     * Construct the layer.
     */
    public PspIconLayerBase ()
    {
        super();

        ccuA.add(Color.green, 60);
        // ccuA.add(new ColorCodeMaxValue(Color.gray, 110));
        ccuA.add(Color.yellow, 160);
        ccuA.add(Color.red, 260);

        ccuB.add(Color.green, 360);
        ccuB.add(Color.gray, 160);
        ccuB.add(Color.blue, 110);
        ccuB.add(Color.red, 60);


        graphics = new OMGraphicList(40);

        // try to create blackjack uilaunch popup during runtime
        // (but don't require it for compilation)
        try {
          uiLaunchClass =
            Class.forName(
              "mil.darpa.log.alpine.blackjack.assessui.client.UILaunchPopup");
          uiLaunchPopup = uiLaunchClass.getConstructor(null).newInstance(null);
        } catch (Exception e) {/* no biggie */}
    }

    /**
     * Sets the properties for the <code>Layer</code>.  This allows
     * <code>Layer</code>s to get a richer set of parameters than the
     * <code>setArgs</code> method.
     * @param prefix the token to prefix the property names
     * @param props the <code>Properties</code> object
     */
    public void setProperties(String prefix, java.util.Properties props)
    {
        super.setProperties(prefix, props);
    }

    Projection projection=null;
    /**
     * Invoked when the projection has changed or this Layer has been
     * added to the MapBean.
     * @param e ProjectionEvent
     */
    public void projectionChanged (ProjectionEvent e) {
        projection = e.getProjection();
        repaintLayer();
    }

    public void repaintLayer() {
        if (projection != null) {
            graphics.generate(projection);
        } else {
            System.err.println("PspIconLayerBase: null projection after in repaint -- skipping graphics.generate ...");
        }
    }

    /**
     * Paints the layer.
     * @param g the Graphics context for painting
     */
    public void paint (Graphics g) {
        graphics.render(g);
    }

    Iterator markerIterator() {
      return myState.markerIterator();
    }

    /**
     * Create graphics.
     */

     PspIconLayerModelBase  myState;


    /**
     * Note: A layer interested in receiving mouse events should
     * implement this function.  Otherwise, return null (the default).
     */
    public synchronized MapMouseListener getMapMouseListener(){
        return this;
    }

    /**
     * Return a list of the modes that are interesting to the
     * MapMouseListener.  You MUST override this with the modes you're
     * interested in.
     */
    public String[] getMouseModeServiceList(){
//        String[] services = {"Gestures" };
//        String[] services = {SelectMouseMode.modeID, DragAndDropMouseMode.modeID };
        String[] services = {SelectMouseMode.modeID};
        return services;
    }

    OMGraphic findClosest(int x, int y, float limit) {
        return myState.findClosest(x,y,limit);
    }

    private CDateLabeledSlider timeSlider = null;
    public void setTimeSlider(CDateLabeledSlider timeSlider)
    {
        this.timeSlider = timeSlider;
    }

    public boolean mouseClicked(MouseEvent e){
        return false;
    }

    Unit getUnit(OMGraphic omgr) {
        return myState.getUnit(omgr);
    }

    public void colorCodeUnits(String metric) {
        // for each unit
        //for (Iterator it=myState.markerIterator();it.hasNext();)
        for (Iterator it=markerIterator();it.hasNext();)
        {

            OMGraphic omgr=(OMGraphic)it.next();
            Unit unit=getUnit(omgr);
            if (unit!=null)
            {
              ccuA.setColor(unit,metric);
            }

            Float fl2=(unit!=null)?unit.getData(metric):null;
            String addon= (fl2==null) ?  "null" : fl2.toString();
            String msg="Value for "+metric+": "+addon;
            omgr=unit.getGraphic();
            if (omgr instanceof VecIcon)
            {
              ((VecIcon)omgr).setMessageAddon(msg);
            }
//            System.err.println("colorCodeUnits: "+msg);

        }
        repaintLayer();
    }

    private String getOrgName(OMGraphic omgr) {
        String name="";
        if(omgr != null) {
            if (omgr instanceof VecIcon) {
                name=((VecIcon)omgr).getLabel();
            }
        }
        return name;
    }

    public boolean mouseMoved(MouseEvent e){

     OMGraphic omgr = findClosest(e.getX(),e.getY(),4);

            if(omgr != null) {

                // this is for the case of a non-VecIcon
                String msg;

                if (omgr instanceof VecIcon) {
                    msg=((VecIcon)omgr).getFullMessage();
                } else {
                    msg=getOrgName(omgr)
                        +"  Double-Clicking on icon brings up a chart.  ";
                    String metric="metric1";
                    //Unit unit=myState.getUnit(omgr);
                    Unit unit=getUnit(omgr);
                    Float fl2=(unit!=null)?unit.getData(metric):null;
                    String addon= (fl2==null) ?  "null" : fl2.toString();
                    msg+="Value for "+metric+": "+addon;
                    // curCcu.setColor(unit,metric);

                }

                fireRequestInfoLine(msg);
            } else {
                fireRequestInfoLine("");
                //             if(lastSelected != null){
                //                 lastSelected.deselect();
                //                 lastSelected.generate(oldProjection);
                //                 lastSelected = null;
//                 repaint();
                //                 //System.out.println("MouseMove Kicking repaint");
                //             }
            }

            //         if(omgr instanceof OMBitmap){
            //             omgr.select();
            //             omgr.generate(oldProjection);
            //             lastSelected = omgr;
            //             //System.out.println("MouseMove Kicking repaint");
            //             repaint();
//         }

        return true;
        }

    //// just here because mouse i/f requires

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param e MouseEvent
     * @return false
     */
    public boolean mousePressed(MouseEvent e){
        return false;
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param e MouseEvent
     * @return false
     */
    public boolean mouseReleased(MouseEvent e){
        final OMGraphic omgr = findClosest(e.getX(),e.getY(),4);
        if ((omgr != null) && (uiLaunchPopup != null)) {
            if (e.isPopupTrigger())
            {
                try {
                  // calling blackjack UILaunchPopup methods using reflection
                  // so that class is not required at compile time.
                  //uiLaunchPopup.setConfigProperty("Org", getOrgName(omgr));
                  uiLaunchClass.getMethod("setConfigProperty",
                    new Class[] {Object.class, Object.class}).
                      invoke(uiLaunchPopup,
                             new Object[] {"Org", getOrgName(omgr)});
                  //uiLaunchPopup.show(
                  //  PspIconLayerBase.this.getParent(), e.getX(), e.getY());
                  uiLaunchClass.getMethod("show",
                    new Class[] {Component.class, Integer.TYPE, Integer.TYPE}).
                      invoke(uiLaunchPopup,
                             new Object[] {PspIconLayerBase.this.getParent(),
                                           new Integer(e.getX()),
                                           new Integer(e.getY())});
                } catch (Exception exp) {/* no biggie */}
            }
        } else {
            return false;
        }
        return true;
    }
    /**
     * Invoked when the mouse enters a component.
     * @param e MouseEvent
     */
    public void mouseEntered(MouseEvent e){
        return;
    }

    /**
     * Invoked when the mouse exits a component.
     * @param e MouseEvent
     */
    public void mouseExited(MouseEvent e){
        return;
    }
    /**
     * Invoked when a mouse button is pressed on a component and then
     * dragged.  The listener will receive these events if it
     * @param e MouseEvent
     * @return false
     */
    public boolean mouseDragged(MouseEvent e){
        return false;
    }
    /**
     * Handle a mouse cursor moving without the button being pressed.
     * Another layer has consumed the event.
     */
    public void mouseMoved(){
        //System.out.println("mouseMoved2 event consumed by other layer  Called");
    }

}


