/* **********************************************************************
 * 
 *  BBNT Solutions LLC, A part of GTE
 *  10 Moulton St.
 *  Cambridge, MA 02138
 *  (617) 873-2000
 * 
 *  Copyright (C) 1998, 2000
 *  This software is subject to copyright protection under the laws of 
 *  the United States and other countries.
 * 
 * **********************************************************************
 * 
 * 
 * 
 * 
 * 
 * 
 * **********************************************************************
 */

package org.cougaar.logistics.ui.stoplight.ui.map.app;

import java.awt.event.*;

import java.awt.dnd.DragGestureEvent;

/**
 * The DragAndDropMouseMode delegates handling of mouse events to the
 * listeners.  This MouseMode type is ideal for Layers that want to
 * receive MouseEvents.  The simplest way to set this up is for the
 * Layer to implement the MapMouseListener interface, and indicate
 * that it wants to receive events when the mouse mode is the
 * DragAndDropMouseMode.  Here's a code snippet for a Layer that would do
 * this:
 * <code><pre>
 *  public MapMouseListener getMapMouseListener() {
 *	return this;
 *  }
 *  public String[] getMouseModeServiceList() {
 *	return new String[] {
 *	    DragAndDropMouseMode.modeID
 *	};
 *  }
 * </pre></code>
 * <p>
 * This class is functionally the same as the AbstractMouseMode,
 * except that it actually calls the fire methods in the
 * MapMouseSupport object to propagate the events.
 */
public class DragAndDropMouseMode extends com.bbn.openmap.event.SelectMouseMode implements java.awt.dnd.DragGestureListener
{
    /**
     * Construct a DragAndDropMouseMode.
     * Default constructor.  Sets the ID to the modeID, and the
     * consume mode to true. 
     */
    public DragAndDropMouseMode()
    {
    	this(true);
    }

    /**
     * Construct a DragAndDropMouseMode.
     * The constructor that lets you set the consume mode. 
     * @param consumeEvents the consume mode setting.
     */
    public DragAndDropMouseMode(boolean consumeEvents)
    {
    	super(consumeEvents);
    	mouseSupport = new DnDMapMouseSupport(consumeEvents);
    }

    public void dragGestureRecognized(DragGestureEvent e)
    {
      ((DnDMapMouseSupport)mouseSupport).fireDragGestureRecognized(e);
    }
}
