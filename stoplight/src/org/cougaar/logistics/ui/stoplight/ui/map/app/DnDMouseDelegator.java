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
package org.cougaar.logistics.ui.stoplight.ui.map.app;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.util.Vector;
import java.awt.dnd.*;

import com.bbn.openmap.util.Debug;
import com.bbn.openmap.event.*;
import com.bbn.openmap.*;

public class DnDMouseDelegator extends com.bbn.openmap.MouseDelegator
{
  private DragGestureRecognizer dragRec = null;

  /**
  * Construct a MouseDelegator 
  */
  public DnDMouseDelegator()
  {
  }

  /**
  * Construct a MouseDelegator with an associated MapBean.
  * @param map MapBean
  */
  public DnDMouseDelegator(MapBean map)
  {
    super(map);
    dragRec = DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(map, DnDConstants.ACTION_MOVE, null);
  }

  public void setMap(MapBean map)
  {
    super.setMap(map);
    dragRec = DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(map, DnDConstants.ACTION_MOVE, null);
  }

  /**
  * Sets the three default OpenMap mouse modes.
  * These modes are: NavMouseMode (Map Navigation), the
  * SelectMouseMode (MouseEvents go to Layers), and NullMouseMode
  * (MouseEvents are ignored).
  */
/*  public void setDefaultMouseModes()
  {
    MapMouseMode[] modes = new MapMouseMode[3];
    modes[0] = new NavMouseMode(true);
    modes[1] = new DragAndDropMouseMode(true);
    modes[2] = new NullMouseMode();

    setMouseModes(modes);
  }*/

  /**
  * Set the active MapMouseMode.
  * This sets the MapMouseMode of the associated MapBean.
  * @param mm MapMouseMode
  */
  public void setActive(MapMouseMode mm)
  {
    super.setActive(mm);

    if (map != null)
    {
      if (mm instanceof DragGestureListener)
      {
        try
        {
          dragRec.addDragGestureListener((DragGestureListener)mm);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }

  /**
  * Deactivate the MapMouseMode.
  * @param mm MapMouseMode.
  */
  public void setInactive(MapMouseMode mm)
  {
    super.setInactive(mm);

    if (map != null)
    {
      if (mm instanceof DragGestureListener)
      {
        dragRec.removeDragGestureListener((DragGestureListener)mm);
      }
    }
  }
}
