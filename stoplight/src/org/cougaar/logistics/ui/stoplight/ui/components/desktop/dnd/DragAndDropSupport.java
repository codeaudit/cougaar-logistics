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
//package com.clarksweng.dnd;
package org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd;

import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DnDConstants;

import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureRecognizer;

import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceEvent;

import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetEvent;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;

import java.util.Hashtable;
import java.util.Vector;

public class DragAndDropSupport
{
  private Hashtable targets = new Hashtable(1);
  private Hashtable sources = new Hashtable(1);
  
	/*********************************************************************************************************************
  <b>Description</b>: Registers a drag source to be triggered on drag events.

  <br>
  @param source DragSource object to register for drag events
	*********************************************************************************************************************/
  public void addDragSource(DragSource source)
  {
    sources.put(source, new DGListener(source));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Registers a drop target to be triggered on drop events.

  <br>
  @param target DropTarget object to register for drop events
	*********************************************************************************************************************/
  public void addDropTarget(DropTarget target)
  {
    targets.put(targets, new DTListener(target));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Unregisters a drag source for drag events.

  <br><b>Notes</b>:<br>
	                  - The drag source must be registered again to enable the DragSource object to be triggered on drag
	                    events

  <br>
  @param source DragSource object to unregister for drag events
	*********************************************************************************************************************/
  public void removeDragSource(DragSource source)
  {
    ((DGListener)sources.remove(source)).remove();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Unregisters a drop target for drop events.

  <br><b>Notes</b>:<br>
	                  - The drop target must be registered again to enable the DropTarget object to be triggered on drop
	                    events

  <br>
  @param target DropTarget object to unregister for drop events
	*********************************************************************************************************************/
  public void removeDropTarget(DropTarget target)
  {
    ((DTListener)targets.remove(target)).remove();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables a drag source for triggering on drag events.

  <br>
  @param source DragSource object to enable/disable event trigger
  @param active True if the drag source should be active, false if not
	*********************************************************************************************************************/
  public void enableDragSource(DragSource source, boolean active)
  {
    ((DGListener)sources.get(source)).setSourceActive(active);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables a drop target for triggering on drop events.

  <br>
  @param target DropTarget object to enable/disable event trigger
  @param active True if the drop target should be active, false if not
	*********************************************************************************************************************/
  public void enableDropTarget(DropTarget target, boolean active)
  {
    ((DTListener)targets.get(target)).setTargetActive(active);
  }
}

// ------------------  Drop Target operations  -----------------

class DTListener implements DropTargetListener
{
  private DropTarget target = null;
  private Vector dropTargetList = new Vector(1);
  
  public DTListener(DropTarget target)
  {
    this.target = target;
    
    boolean dropToSubComponents = target.dropToSubComponents();
    Vector components = target.getTargetComponents();
    for (int i=0, isize=components.size(); i<isize; i++)
    {
      if (dropToSubComponents)
      {
        setDropTargets((Component)components.elementAt(i));
      }
      else
      {
        dropTargetList.add(new java.awt.dnd.DropTarget((Component)components.elementAt(i), DnDConstants.ACTION_MOVE, this, true));
      }
    }
  }

  private void setDropTargets(Component parent)
  {
  	if (parent instanceof Container)
  	{
  		Component[] componentList = ((Container)parent).getComponents();
  		for (int i=0; i<componentList.length; i++)
  		{
  			setDropTargets(componentList[i]);
  		}
  	}
  	
    dropTargetList.add(new java.awt.dnd.DropTarget(parent, DnDConstants.ACTION_MOVE, this, true));
  }

  public void remove()
  {
    java.awt.dnd.DropTarget dropTarget = null;
    for (int i=0, isize=dropTargetList.size(); i<isize; i++)
    {
      dropTarget = (java.awt.dnd.DropTarget)dropTargetList.elementAt(i);
      dropTarget.removeDropTargetListener(this);
      dropTarget.setComponent(null);
    }
  }

  public void setTargetActive(boolean active)
  {
    for (int i=0, isize=dropTargetList.size(); i<isize; i++)
    {
      ((java.awt.dnd.DropTarget)dropTargetList.elementAt(i)).setActive(active);
    }
  }

	public void dragEnter(DropTargetDragEvent e)
	{
    try
    {
      checkDroppable(e);
    }
    catch (NullPointerException ex)
    {
// EBM: Bug???  For some reason when we drag exit one drop target and drag enter another drop target immediately (i.e.
//              the drop target components are right next to each other in the layout), a null pointer exception
//              is generated some where inside the Sun windows DnD classes.  But it does not appear to effect us if
//              we just ignore this error!
//      System.out.println("We have error");
//      ex.printStackTrace();
    }
	}

	public void dragOver(DropTargetDragEvent e)
	{
    checkDroppable(e);
	}

	public void dropActionChanged(DropTargetDragEvent e)
	{
    checkDroppable(e);
	}
  
	public void dragExit(DropTargetEvent e)
	{
		target.showAsDroppable(e.getDropTargetContext().getComponent(), null, null, false, false);
	}

	private DataFlavor getDropFlavor(DropTargetDragEvent e)
	{
		if ((e.getDropAction() & DnDConstants.ACTION_MOVE) != 0)
		{
		  Vector supportedFlavors = (Vector)target.getSupportedDataFlavors(e.getDropTargetContext().getComponent(), e.getLocation()).clone();
		  for (int i=0, isize=supportedFlavors.size(); i<isize; i++)
		  {
		    DataFlavor flavor = (DataFlavor)supportedFlavors.elementAt(i);
    		if(e.isDataFlavorSupported(flavor) && target.readyForDrop(e.getDropTargetContext().getComponent(), e.getLocation(), flavor))
    		{
    			return(flavor);
    		}
    	}
		}

		return(null);
	}

  private void checkDroppable(DropTargetDragEvent e)
  {
    DataFlavor flavor = getDropFlavor(e);

		if(flavor != null)
		{
  		target.showAsDroppable(e.getDropTargetContext().getComponent(), e.getLocation(), flavor, true, true);
  		e.acceptDrag(e.getDropAction()); 
		}
		else
		{
			target.showAsDroppable(e.getDropTargetContext().getComponent(), e.getLocation(), null, true, false);
			e.rejectDrag();
		}
  }

	private DataFlavor getDropFlavor(DropTargetDropEvent e)
	{
		if ((e.getDropAction() & DnDConstants.ACTION_MOVE) != 0)
		{
		  Vector supportedFlavors = (Vector)target.getSupportedDataFlavors(e.getDropTargetContext().getComponent(), e.getLocation()).clone();
		  for (int i=0, isize=supportedFlavors.size(); i<isize; i++)
		  {
		    DataFlavor flavor = (DataFlavor)supportedFlavors.elementAt(i);
    		if(e.isDataFlavorSupported(flavor) && target.readyForDrop(e.getDropTargetContext().getComponent(), e.getLocation(), flavor))
    		{
    			return(flavor);
    		}
    	}
		}

		return(null);
	}

	public void drop(DropTargetDropEvent e)
	{
    DataFlavor flavor = getDropFlavor(e);
		target.showAsDroppable(e.getDropTargetContext().getComponent(), null, null, false, false);

		if(flavor != null)
		{
		  try
		  {
  			e.acceptDrop(DnDConstants.ACTION_MOVE);
  			target.dropData(e.getDropTargetContext().getComponent(), e.getLocation(), flavor, e.getTransferable().getTransferData(flavor));
  			e.dropComplete(true);
  		}
  		catch (Throwable t)
  		{
  		  t.printStackTrace();
  		}
		}
		else
		{
			e.rejectDrop();      		
		}
	}
}



// ------------------  Drag Source operations  -----------------

class DGListener implements DragGestureListener
{
  // Should make this static
	private DragSourceListener dsListener = new DSListener();
	private DragSource source = null;

  private Vector recognizerList = new Vector(1);

  private boolean active = true;

	public DGListener(DragSource source)
	{
	  this.source = source;

    boolean dragFromSubComponents = source.dragFromSubComponents();
    Vector components = source.getSourceComponents();
    for (int i=0, isize=components.size(); i<isize; i++)
    {
      if (dragFromSubComponents)
      {
        setDragSource((Component)components.elementAt(i));
      }
      else
      {
        recognizerList.add(java.awt.dnd.DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer((Component)components.elementAt(i), DnDConstants.ACTION_MOVE, this));
      }
    }
	}

	private void setDragSource(Component parent)
	{
		if (parent instanceof Container)
		{
			Component[] componentList = ((Container)parent).getComponents();
			for (int i=0; i<componentList.length; i++)
			{
				setDragSource(componentList[i]);
			}
		}

    recognizerList.add(java.awt.dnd.DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(parent, DnDConstants.ACTION_MOVE, this));
	}

  public void remove()
  {
    DragGestureRecognizer recognizer = null;
    for (int i=0, isize=recognizerList.size(); i<isize; i++)
    {
      recognizer = (DragGestureRecognizer)recognizerList.elementAt(i);
      recognizer.removeDragGestureListener(this);
      recognizer.setComponent(null);
    }
  }

  public void setSourceActive(boolean active)
  {
    this.active = active;
  }

	public void dragGestureRecognized(DragGestureEvent e)
	{
		if ((active) && ((e.getDragAction() & DnDConstants.ACTION_MOVE) != 0))
		{
  		try
  		{
  		  Object data = source.getData(e.getComponent(), e.getDragOrigin());
  		  if (data != null)
  		  {
  		    if (data instanceof Transferable)
  		    {
  			    e.startDrag(java.awt.dnd.DragSource.DefaultMoveNoDrop, (Transferable)data, dsListener);
  		    }
  		    else
  		    {
  			    e.startDrag(java.awt.dnd.DragSource.DefaultMoveNoDrop, new ObjectTransferable(data), dsListener);
  			  }
  			}
  		}
  		catch (InvalidDnDOperationException ex)
  		{
  			ex.printStackTrace();
  		}
    }
	}

  private class DSListener implements DragSourceListener
  {
  	public void dragEnter(DragSourceDragEvent e)
  	{
  	  setDragCursor(e);
  	}
  
  	public void dragOver(DragSourceDragEvent e)
  	{
  	  setDragCursor(e);
  	}
  
  	public void dropActionChanged (DragSourceDragEvent e)
  	{
  	  setDragCursor(e);
  	}

  	public void dragExit(DragSourceEvent e)
  	{
      DragSourceContext context = e.getDragSourceContext();
      
      // there is a bug in DragSourceContext.
      // here is the work around: set the cursor to null first!
      context.setCursor(null); 
      context.setCursor(java.awt.dnd.DragSource.DefaultMoveNoDrop); 
  	}
  
  	public void dragDropEnd(DragSourceDropEvent e)
  	{
  	  source.dragDropEnd(e.getDropSuccess());
  	}
  
    private void setDragCursor(DragSourceDragEvent e)
    {
    	DragSourceContext context = e.getDragSourceContext();
    	int dropAction = e.getDropAction();
    	int targetAction = e.getTargetActions();
    	Cursor c = java.awt.dnd.DragSource.DefaultMoveNoDrop;

	    if (((dropAction & targetAction) & DnDConstants.ACTION_MOVE) == DnDConstants.ACTION_MOVE)
	    {
	      c = java.awt.dnd.DragSource.DefaultMoveDrop;
      }

    	// there is a bug in DragSourceContext.
    	// here is the work around: set the cursor to null first!
      context.setCursor(null); 
    	context.setCursor(c); 
  	}
  }
}
