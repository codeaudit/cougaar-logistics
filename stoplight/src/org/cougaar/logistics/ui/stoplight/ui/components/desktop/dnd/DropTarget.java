/***********************************************************************************************************************
  Clark Software Engineering, Ltd. Copyright 2001
***********************************************************************************************************************/

//package com.clarksweng.dnd;
package org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd;

import java.awt.*;
import java.util.Vector;
import java.awt.datatransfer.*;

/***********************************************************************************************************************
<b>Description</b>: The drop target interface.  Implemented by classes which wish to receive/control drag and drop
                    actions for a component or group of components.

***********************************************************************************************************************/
public interface DropTarget
{
	/*********************************************************************************************************************
  <b>Description</b>: Gets the components to register drop events on.

  <br><b>Notes</b>:<br>
	                  - The vector must only contain java.awt.Component objects
	                  - This method is called only once when the drop target is added via DragAndDropSupport

  <br>
  @return Vector of java.awt.Component objects to register as targets
	*********************************************************************************************************************/
  public Vector getTargetComponents();

	/*********************************************************************************************************************
  <b>Description</b>: Determines if the sub-components of the target components (java.awt.Container objects only)
                      returned from the getTargetComponents() method should be registered for drop events.  This is
                      a convenience method to allow returning a single java.awt.Container object that contains all of
                      the java.awt.Component objects that should receive drop events.

  <br><b>Notes</b>:<br>
	                  - This method is called only once when the drop target is added via DragAndDropSupport

  <br>
  @return True if the sub-components of the target components should be registered for drop events, false if not
	*********************************************************************************************************************/
  public boolean dropToSubComponents();

	/*********************************************************************************************************************
  <b>Description</b>: Determines if the drop target is ready to accept a drop action.

  <br><b>Notes</b>:<br>
	                  - This method is called any time a drag enter/over action or drop action occurs and the data flavor
	                    of the dragged object matches a data flavor of the current drop target
	                  - The showAsDroppable() method should be used to display user feedback for droppable/not droppable

  <br>
  @param componentAt Component the drag/drop operation is on
  @param location Current location of the mouse pointer
  @param flavor Data flavor the operation is using
  @return True if the current specified conditions are acceptable for a drop operation, false if not
  
  @see #showAsDroppable(Component, Point, DataFlavor, boolean, boolean)
	*********************************************************************************************************************/
  public boolean readyForDrop(Component componentAt, Point location, DataFlavor flavor);

	/*********************************************************************************************************************
  <b>Description</b>: Called when a drag/drop operation warrants changing user display/feedback to indicate the status
                      of the drag/drop operation.

  <br><b>Notes</b>:<br>
	                  - This method is called any time a drag enter/over action or drop action occurs
	                  - This method may be called even if readyForDrop() was not called
	                  - The location paramter may be null if it is not relevant (such as on a drag exit event)
	                  - The flavor paramter may be null if it is not relevant (such as when the operation is rejected)

  <br>
  @param componentAt Component the drag/drop operation is on
  @param location Current location of the mouse pointer, null if the mouse location is over the component
  @param flavor Data flavor the operation is using, null if the data flavor is not relevant
  @param show True if the target should indicate a Drag & Drop operation is underway, false if not
  @param droppable True if the current drag can be droped on the component
  
  @see #readyForDrop(Component, Point, DataFlavor)
	*********************************************************************************************************************/
  public void showAsDroppable(Component componentAt, Point location, DataFlavor flavor, boolean show, boolean droppable);

	/*********************************************************************************************************************
  <b>Description</b>: Called when data is droped onto one of the components specified by the current DropTarget object.

  <br><b>Notes</b>:<br>
	                  - The actual (or serialized copy of, if dragged to another JVM) data object is passed to this
	                    method

  <br>
  @param componentAt Component the drag/drop operation is on
  @param location Current location of the mouse pointer
  @param flavor Data flavor the operation is using
  @param data Instance of the data object
	*********************************************************************************************************************/
  public void dropData(Component componentAt, Point location, DataFlavor flavor, Object data);

	/*********************************************************************************************************************
  <b>Description</b>: Gets the data flavors to trigger on for the current DropTarget.  Data flavor preferences should
                      be organized from most desirable to least desirable within the return Vector.

  <br><b>Notes</b>:<br>
	                  - The vector must only contain java.awt.datatransfer.DataFlavor objects
	                  - This method is called any time a drag enter/over action or drop action occurs

  <br>
  @param componentAt Component the drag/drop operation is on
  @param location Current location of the mouse pointer
  @return Vector of java.awt.datatransfer.DataFlavor objects to trigger on
	*********************************************************************************************************************/
  public Vector getSupportedDataFlavors(Component componentAt, Point location);

//  public boolean canDropToSelf();

//  public void showAsActive(boolean active);

//	public void dragEnter(DropTargetDragEvent e)
//	public void dragOver(DropTargetDragEvent e)
//	public void dropActionChanged(DropTargetDragEvent e)
//	public void dragExit(DropTargetEvent e)

}
