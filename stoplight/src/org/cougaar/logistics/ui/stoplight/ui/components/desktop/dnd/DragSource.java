/***********************************************************************************************************************
  Clark Software Engineering, Ltd. Copyright 2001
***********************************************************************************************************************/

//package com.clarksweng.dnd;
package org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd;

import java.awt.*;
import java.util.Vector;
import java.awt.datatransfer.*;

/***********************************************************************************************************************
<b>Description</b>: The drag source interface.  Implemented by classes which wish to generate/control drag and drop
                    actions for a component or group of components.

***********************************************************************************************************************/
public interface DragSource
{
	/*********************************************************************************************************************
  <b>Description</b>: Gets the components to register drag events on.

  <br><b>Notes</b>:<br>
	                  - The vector must only contain java.awt.Component objects
	                  - This method is called only once when the drag source is added via DragAndDropSupport

  <br>
  @return Vector of java.awt.Component objects to register as sources
	*********************************************************************************************************************/
  public Vector getSourceComponents();

	/*********************************************************************************************************************
  <b>Description</b>: Determines if the sub-components of the source components (java.awt.Container objects only)
                      returned from the getSourceComponents() method should be registered for drag events.  This is
                      a convenience method to allow returning a single java.awt.Container object that contains all of
                      the java.awt.Component objects that should receive drag events.

  <br><b>Notes</b>:<br>
	                  - This method is called only once when the drag source is added via DragAndDropSupport

  <br>
  @return True if the sub-components of the source components should be registered for drag events, false if not
	*********************************************************************************************************************/
  public boolean dragFromSubComponents();

	/*********************************************************************************************************************
  <b>Description</b>: Retrieves the data object to be dragged.  This method should return the actual instance of the
                      data object to be drop on a drop target, or a custom java.awt.datatransfer.Transferable object.

  <br><b>Notes</b>:<br>
	                  - This method is called once at the begining of each drag operation
	                  - The data flavor is determined directly from the returned data object (if not a Transferable)
	                  - The data object returned DOES NOT have to be, but usually is, the same class type between drags
	                  - If a Transferable object is returned, this object is used (along with its defined data flavors)
	                    instead of constructing a ObjectTransferable object

  <br>
  @param componentAt Component the drag operation is on
  @param location Current location of the mouse pointer
  @return Data object instance to be dragged, or a java.awt.datatransfer.Transferable object
	*********************************************************************************************************************/
  public Object getData(Component componentAt, Point location);
  
	/*********************************************************************************************************************
  <b>Description</b>: Called when the drag and drop operation has ended.

  <br>
  @param success True if the drag and drop operation completed successfully, false if not
	*********************************************************************************************************************/
  public void dragDropEnd(boolean success);

//  	public void dragDropEnd(DragSourceDropEvent e)
//  	public void dragEnter(DragSourceDragEvent e)  ???
//  	public void dragOver(DragSourceDragEvent e)
//  	public void dragExit(DragSourceEvent e)
//  	public void dropActionChanged (DragSourceDragEvent e)  ???

// public void showAsActive(boolean active);

// public Cursor getDragCursor()  ???

    /* Eliminates right mouse clicks as valid actions - useful especially
     * if you implement a JPopupMenu for the JTree
     */
//    dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);   ???
}
