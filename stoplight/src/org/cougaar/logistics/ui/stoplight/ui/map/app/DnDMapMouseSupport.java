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

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureEvent;

import com.bbn.openmap.*;
import com.bbn.openmap.event.*;

import com.bbn.openmap.util.Debug;

public class DnDMapMouseSupport extends com.bbn.openmap.event.MapMouseSupport
{
    /**
     * Construct a default MapMouseSupport.
     * The default value of consumeEvents is set to true.
     */
    public DnDMapMouseSupport()
    {
    	this(true);
    }

    /**
     * Construct a MapMouseSupport.
     * @param shouldConsumeEvents if true, events are propagated to
     * the first MapMouseListener that successfully processes the
     * event, if false, events are propagated to all MapMouseListeners
     */
    public DnDMapMouseSupport(boolean shouldConsumeEvents)
    {
      super(shouldConsumeEvents);
    }

    /**
     * Handle a mousePressed MouseListener event.
     * @param evt MouseEvent to be handled
     */
    public boolean fireDragGestureRecognized(DragGestureEvent evt)
    {
      if (Debug.debugging("gestures"))
      {
        System.out.println("MapMouseSupport.fireDragGestureRecognized()");
      }

      java.util.Vector targets = getTargets();
      if (targets == null) return false;
      
      for (int i = 0; i < targets.size(); i++)
      {
        // Not quite the way it should be, but you get what you pay for
        MapMouseListener target = (MapMouseListener)targets.elementAt(i);
        if (target instanceof DragGestureListener)
        {
          ((DragGestureListener)target).dragGestureRecognized(evt);
          if (consumeEvents)
          {
            priorityListener = target;
            return true;
          }
        }
      }

      return false;
    }
}
