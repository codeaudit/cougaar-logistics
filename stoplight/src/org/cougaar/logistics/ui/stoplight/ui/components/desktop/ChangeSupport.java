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

import java.util.Vector;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/***********************************************************************************************************************
<b>Description</b>: Support class for classes that need to implement listner notification services for ChangeListeners.

***********************************************************************************************************************/
public class ChangeSupport
{
  private Vector listenerList = new Vector(0);
  
	/*********************************************************************************************************************
  <b>Description</b>: Adds a change listener to the list of listeners.

  <br>
  @param listener Listener to add
	*********************************************************************************************************************/
  public synchronized void addChangeListener(ChangeListener listener)
  {
    if (!listenerList.contains(listener))
    {
      listenerList.add(listener);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a change listener from the list of listeners.

  <br>
  @param listener Listener to remove
	*********************************************************************************************************************/
  public synchronized void removeChangeListener(ChangeListener listener)
  {
    listenerList.remove(listener);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Notifies a listeners that have been added of a change event.

  <br>
  @param source Source of event
	*********************************************************************************************************************/
  public synchronized void fireChangeEvent(Object source)
  {
    ChangeEvent e = new ChangeEvent(source);
    
    for (int i=0, isize=listenerList.size(); i<isize; i++)
    {
      ((ChangeListener)listenerList.elementAt(i)).stateChanged(e);
    }
  }
}