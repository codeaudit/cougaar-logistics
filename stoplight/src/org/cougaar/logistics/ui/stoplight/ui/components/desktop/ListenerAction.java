/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.awt.event.AWTEventListener;
import java.awt.AWTEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerListener;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;

import java.util.EventObject;

import java.lang.reflect.Method;

/***********************************************************************************************************************
<b>Description</b>: This is a utility class designed to limit code/class bloat due to anonymous class listener
                    callbacks and improve code readability.  Instead of creating a anonymous class that implements a
                    specific listener each time a listener is needed, an instance of this class is created specifying
                    the type of listener to use, an object, method name to call on the object (via reflection), and
                    parameters to pass (including the original event object).  This allows less class files to be
                    generated (good for applets and distribution) and extra information to be passed along with the
                    event.

***********************************************************************************************************************/
public class ListenerAction implements ActionListener, WindowListener, ComponentListener, ContainerListener, ListSelectionListener, CaretListener, KeyListener, MouseListener, MouseMotionListener, AWTEventListener, InternalFrameListener, PropertyChangeListener
{
	public static final int actionPerformed		      	= 1;

	public static final int windowOpened			      	= 2;
	public static final int windowClosing		      		= 3;
	public static final int windowClosed			      	= 4;
	public static final int windowIconified		      	= 5;
	public static final int windowDeiconified	      	= 6;
	public static final int windowActivated		      	= 7;
	public static final int windowDeactivated	      	= 8;

	public static final int componentResized	      	= 9;
	public static final int componentMoved	      		= 10;
	public static final int componentShown	      		= 11;
	public static final int componentHidden	      		= 12;

	public static final int componentAdded	      		= 13;
	public static final int componentRemoved	      	= 14;

	public static final int valueChanged		      		= 15;

	public static final int caretUpdate		      			= 16;

	public static final int keyTyped				      		= 17;
	public static final int keyPressed			      		= 18;
	public static final int keyReleased			      		= 19;

	public static final int mouseClicked		      		= 20;
	public static final int mousePressed	      			= 21;
	public static final int mouseReleased	      			= 22;
	public static final int mouseEntered	      			= 23;
	public static final int mouseExited		      			= 24;

	public static final int mouseDragged			      	= 25;
	public static final int mouseMoved	      				= 26;

	public static final int eventDispatched	      		= 27;

	public static final int internalFrameOpened				= 28;
	public static final int internalFrameClosing			= 29;
	public static final int internalFrameClosed				= 30;
	public static final int internalFrameIconified		= 31;
	public static final int internalFrameDeiconified	= 32;
	public static final int internalFrameActivated		= 33;
	public static final int internalFrameDeactivated	= 34;

	public static final int propertyChange          	= 35;


	private Object targetObject = null;
	private Method targetMethod = null;
	private Object[] targetParameters = null;

	private int listenerType = 0;
	private boolean sendEvent = false;

	public ListenerAction(Object target, String method, Object[] parameters, int type) throws Throwable
	{
		this(target, method, parameters, type, false);
	}

	public ListenerAction(Object target, String method, Object[] parameters, int type, boolean sendEventObject) throws Throwable
	{
		targetObject = target;
		listenerType = type;
		sendEvent = sendEventObject;

    Class[] types = new Class[parameters.length + (sendEvent ? 1 : 0)];
    targetParameters = new Object[types.length];
    
    for (int i=0; i<parameters.length; i++)
    {
      types[i] = parameters[i].getClass();
      targetParameters[i] = parameters[i];
    }

    if (sendEvent)
    {
    	types[types.length -1] = EventObject.class;
    }

		targetMethod = targetObject.getClass().getMethod(method, types);
	}

	private void invokeListener(int type, EventObject e)
	{
		if (listenerType == type)
		{
	    if (sendEvent)
	    {
	    	targetParameters[targetParameters.length -1] = e;
	    }

			try
			{
				targetMethod.invoke(targetObject, targetParameters);
			}
	  	catch (Throwable t)
	  	{
	  		t.printStackTrace();
	  	}
	  }
	}

	// ActionListener -----------------------

	public void actionPerformed(ActionEvent e)
	{
		invokeListener(actionPerformed, e);
	}

	// WindowListener -----------------------

	public void windowOpened(WindowEvent e)
	{
		invokeListener(windowOpened, e);
	}

	public void windowClosing(WindowEvent e)
	{
		invokeListener(windowClosing, e);
	}
	
	public void windowClosed(WindowEvent e)
	{
		invokeListener(windowClosed, e);
	}

	public void windowIconified(WindowEvent e)
	{
		invokeListener(windowIconified, e);
	}

	public void windowDeiconified(WindowEvent e)
	{
		invokeListener(windowDeiconified, e);
	}

	public void windowActivated(WindowEvent e)
	{
		invokeListener(windowActivated, e);
	}

	public void windowDeactivated(WindowEvent e)
	{
		invokeListener(windowDeactivated, e);
	}

	// ComponentListener -----------------------

	public void componentResized(ComponentEvent e)
	{
		invokeListener(componentResized, e);
	}

	public void componentMoved(ComponentEvent e)
	{
		invokeListener(componentMoved, e);
	}

	public void componentShown(ComponentEvent e)
	{
		invokeListener(componentShown, e);
	}

	public void componentHidden(ComponentEvent e)
	{
		invokeListener(componentHidden, e);
	}

	// ContainerListener -----------------------

	public void componentAdded(ContainerEvent e)
	{
		invokeListener(componentAdded, e);
	}

	public void componentRemoved(ContainerEvent e)
	{
		invokeListener(componentRemoved, e);
	}

	// ListSelectionListener -----------------------

	public void valueChanged(ListSelectionEvent e)
	{
		invokeListener(valueChanged, e);
	}

	// CaretListener -----------------------

	public void caretUpdate(CaretEvent e)
	{
		invokeListener(caretUpdate, e);
	}

	// KeyListener -----------------------

	public void keyTyped(KeyEvent e)
	{
		invokeListener(keyTyped, e);
	}

	public void keyPressed(KeyEvent e)
	{
		invokeListener(keyPressed, e);
	}

	public void keyReleased(KeyEvent e)
	{
		invokeListener(keyReleased, e);
	}

	// MouseListener -----------------------

	public void mouseClicked(MouseEvent e)
	{
		invokeListener(mouseClicked, e);
	}

	public void mousePressed(MouseEvent e)
	{
		invokeListener(mousePressed, e);
	}

	public void mouseReleased(MouseEvent e)
	{
		invokeListener(mouseReleased, e);
	}

	public void mouseEntered(MouseEvent e)
	{
		invokeListener(mouseEntered, e);
	}

	public void mouseExited(MouseEvent e)
	{
		invokeListener(mouseExited, e);
	}

	// MouseMotionListener -----------------------

	public void mouseDragged(MouseEvent e)
	{
		invokeListener(mouseDragged, e);
	}
	
	public void mouseMoved(MouseEvent e)
	{
		invokeListener(mouseMoved, e);
	}

	// AWTEventListener -----------------------

	public void eventDispatched(AWTEvent e)
	{
		invokeListener(eventDispatched, e);
	}
	
	// InternalFrameListener -----------------------

	public void internalFrameOpened(InternalFrameEvent e)
	{
		invokeListener(internalFrameOpened, e);
	}

	public void internalFrameClosing(InternalFrameEvent e)
	{
		invokeListener(internalFrameClosing, e);
	}

	public void internalFrameClosed(InternalFrameEvent e)
	{
		invokeListener(internalFrameClosed, e);
	}

	public void internalFrameIconified(InternalFrameEvent e)
	{
		invokeListener(internalFrameIconified, e);
	}

	public void internalFrameDeiconified(InternalFrameEvent e)
	{
		invokeListener(internalFrameDeiconified, e);
	}

	public void internalFrameActivated(InternalFrameEvent e)
	{
		invokeListener(internalFrameActivated, e);
	}

	public void internalFrameDeactivated(InternalFrameEvent e)
	{
		invokeListener(internalFrameDeactivated, e);
	}

	// PropertyChangeListener -----------------------

	public void propertyChange(PropertyChangeEvent e)
	{
		invokeListener(propertyChange, e);
	}
}
