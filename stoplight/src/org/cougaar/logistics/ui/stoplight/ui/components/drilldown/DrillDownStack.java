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

package org.cougaar.logistics.ui.stoplight.ui.components.drilldown;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JButton;

import java.util.Vector;
import java.util.Stack;

/***********************************************************************************************************************
<b>Description</b>: This class provided support for drill down capabilities.  The theory of a drill down is to have
                    a set of UI components that are "stacked" on top of each other such that a child covers all of its
                    ancestors.  When the child is double clicked on, it creates a new child UI component to be displayed
                    (the new child covers the current component).  Forward and backward controls are provided to
                    navigate the stack of UI components.  The IU components may be drill downs them selves or they may
                    be created from other object types that implement the DrillDown interface.  As components are
                    navigated, their drill down counterparts are inserted and removed from the stack.

***********************************************************************************************************************/
public class DrillDownStack extends JPanel
{
  private Stack drillDownStack = new Stack();
  private Stack historyStack = new Stack();

  private Vector currentElementList = null;
  private DrillDownSet currentDrillDownSet = null;

	/*********************************************************************************************************************
  <b>Description</b>: Constructor for a DrillDownStack.

  <br>
  @param component Initial component to display
	*********************************************************************************************************************/
  public DrillDownStack(Component component)
  {
    currentDrillDownSet = new DrillDownSet(null, component, new Vector(0));
    currentElementList = currentDrillDownSet.elementList;
    this.setLayout(new BorderLayout());

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(1, 2));
    
    JButton button = new JButton("Back");
    button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          pop();
        }
      });
    buttonPanel.add(button);

    button = new JButton("Forward");
    button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          push();
        }
      });
    buttonPanel.add(button);

    this.add(buttonPanel, BorderLayout.NORTH);
    this.add(component, BorderLayout.CENTER);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a drill down object to be triggered when the specified area on the specified component is
                      double clicked.

  <br><b>Notes</b>:<br>
	                  - The area parameter can be null to indicate a double click is acceptable anywhere on the component

  <br>
  @param drillDown DrillDown object to active when the specified component is triggered
  @param component UI Component to listen for double click to activate the specified DrillDown object
  @param area Area of the component to consider double clicking a valid trigger 
	*********************************************************************************************************************/
  public void addDrillDown(DrillDown drillDown, Component component, Rectangle area)
  {
    currentElementList.add(new DrillDownElement(drillDown, component, area));
  }

  private void pop()
  {
    if (!drillDownStack.empty())
    {
      currentDrillDownSet.removeAllListeners();
      this.remove(currentDrillDownSet.displayComponent);
      historyStack.push(currentDrillDownSet);

      currentDrillDownSet = (DrillDownSet)drillDownStack.pop();
      currentElementList = currentDrillDownSet.elementList;

      currentDrillDownSet.addAllListeners();
      this.add(currentDrillDownSet.displayComponent, BorderLayout.CENTER);

      this.revalidate();
      this.repaint();
    }
  }

  private void push()
  {
    if (!historyStack.empty())
    {
      currentDrillDownSet.removeAllListeners();
      this.remove(currentDrillDownSet.displayComponent);
      drillDownStack.push(currentDrillDownSet);

      currentDrillDownSet = (DrillDownSet)historyStack.pop();
      currentElementList = currentDrillDownSet.elementList;

      currentDrillDownSet.addAllListeners();
      this.add(currentDrillDownSet.displayComponent, BorderLayout.CENTER);

      this.revalidate();
      this.repaint();
    }
  }

  private void drill(DrillDown drillDown, Component displayComponent, Vector elementList)
  {
    currentDrillDownSet.removeAllListeners();
    this.remove(currentDrillDownSet.displayComponent);
    historyStack.clear();

    drillDownStack.push(currentDrillDownSet);

    currentDrillDownSet = new DrillDownSet(drillDown, displayComponent, elementList);
    currentElementList = elementList;
    this.add(displayComponent, BorderLayout.CENTER);

    this.revalidate();
    this.repaint();
  }

  private class DrillDownSet
  {
    public DrillDown drillDown = null;
    public Component displayComponent = null;
    public Vector elementList = null;
    
    public DrillDownSet(DrillDown drillDown, Component displayComponent, Vector elementList)
    {
      this.drillDown = drillDown;
      this.displayComponent = displayComponent;
      this.elementList = elementList;
    }
    
    public void addAllListeners()
    {
      for (int i=0, isize=elementList.size(); i<isize; i++)
      {
        DrillDownElement element = (DrillDownElement)elementList.elementAt(i);
//        element.componentToListen.addMouseListener(element);
        element.setListeners();
      }
    }

    public void removeAllListeners()
    {
      for (int i=0, isize=elementList.size(); i<isize; i++)
      {
        DrillDownElement element = (DrillDownElement)elementList.elementAt(i);
//        element.componentToListen.removeMouseListener(element);
        element.removeListeners();
      }
    }

    public String toString()
    {
      return(this.getClass().getName() + "@" + this.hashCode() + " " + drillDown);
    }
  }

  private class DrillDownElement implements MouseListener, MouseMotionListener
  {
    public DrillDown drillDown = null;
    public Component componentToListen = null;
    public Rectangle triggerArea = null;
    
    public DrillDownElement(DrillDown drillDown, Component componentToListen, Rectangle triggerArea)
    {
      this.drillDown = drillDown;
      this.componentToListen = componentToListen;
      this.triggerArea = triggerArea;
      
//      componentToListen.addMouseListener(this);
      setListeners(componentToListen);
//      componentToListen.addMouseMotionListener(this);
    }

    public void setListeners()
    {
      setListeners(componentToListen);
    }

    private void setListeners(Component parent)
    {
    	if (parent instanceof Container)
    	{
    		Component[] componentList = ((Container)parent).getComponents();
    		for (int i=0; i<componentList.length; i++)
    		{
    			setListeners(componentList[i]);
    		}
    	}
    	
      parent.addMouseListener(this);
    }
    
    public void removeListeners()
    {
      removeListeners(componentToListen);
    }

    private void removeListeners(Component parent)
    {
    	if (parent instanceof Container)
    	{
    		Component[] componentList = ((Container)parent).getComponents();
    		for (int i=0; i<componentList.length; i++)
    		{
    			removeListeners(componentList[i]);
    		}
    	}
    	
      parent.removeMouseListener(this);
    }
    
    public void mouseClicked(MouseEvent e)
    {
      if ((e.getClickCount() > 1) && ((triggerArea == null) || (triggerArea.contains(e.getPoint()))))
      {
        DrillDown newDrillDown = drillDown.getNextDrillDown(e);
        if (newDrillDown != null)
        {
          DrillDownStack.this.currentElementList = new Vector(0);
          Component displayComponent = newDrillDown.activate(DrillDownStack.this);
          if (displayComponent != null)
          {
            DrillDownStack.this.drill(newDrillDown, displayComponent, DrillDownStack.this.currentElementList);
          }
          else
          {
            DrillDownStack.this.currentElementList = DrillDownStack.this.currentDrillDownSet.elementList;
          }
        }
      }
    }

    public void mouseEntered(MouseEvent e)
    {
//System.out.println("mouseEntered!" + e);
    }

    public void mouseExited(MouseEvent e)
    {
//System.out.println("mouseExited!" + e);
    }

    public void mousePressed(MouseEvent e)
    {
//System.out.println("mousePressed!" + e);
    }

    public void mouseReleased(MouseEvent e)
    {
//System.out.println("mouseReleased!" + e);
    }

    public void mouseDragged(MouseEvent e)
    {
//System.out.println("mouseDragged!" + e);
    }

    public void mouseMoved(MouseEvent e)
    {
//System.out.println("mouseMoved!" + e);
    }
  }
}
