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

package org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm;

import java.util.Vector;

/***********************************************************************************************************************
<b>Description</b>: This class provides event listener support utilities to the Object Storage Manager.

***********************************************************************************************************************/
public class NotificationSupport
{
  private Vector listenerTypeList = new Vector(0);
  private Vector listenerList = new Vector(0);

	/*********************************************************************************************************************
  <b>Description</b>: Returns a count of the number of listeners this support instance contains.

  <br>
  @return Number of listeners contained by this support instance
	*********************************************************************************************************************/
  public int getListenerCount()
  {
    return(listenerList.size());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a listener to the support instance.

  <br>
  @param listener Listener to add
  @param notificationType Type of events the listener is to be notified of
	*********************************************************************************************************************/
  public void addNotificationListener(NotificationListener listener, long notificationType)
  {
    synchronized(this)
    {
      if (!listenerList.contains(listener))
      {
        listenerList.add(listener);
        listenerTypeList.add(new ListenerType(listener, notificationType));
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a listener from the support instance.

  <br><b>Notes</b>:<br>
	                  - Any notes about the method goes here

  <br>
  @param listener Listener to remove
	*********************************************************************************************************************/
  public void removeNotificationListener(NotificationListener listener)
  {
    synchronized(this)
    {
      int index = listenerList.indexOf(listener);
      
      if (index != -1)
      {
        listenerList.remove(index);
        listenerTypeList.remove(index);
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sedns out an event notification to all listerners registered for the specified type of event.

  <br>
  @param event Event to notify listeners with
	*********************************************************************************************************************/
  public void fireNotificationEvent(NotificationEvent event)
  {
    synchronized(this)
    {
      long notificationType = event.getNotificationType();
      for (int i=0, isize=listenerTypeList.size(); i<isize; i++)
      {
        ListenerType lt = (ListenerType)listenerTypeList.elementAt(i);
        if (lt.ofType(notificationType))
        {
          lt.listener.notify(event);
        }
      }
    }
  }
}

/***********************************************************************************************************************
<b>Description</b>: Support class for notification support.  This class stores an individual listener and information
                    about the types of events it is interested in.

***********************************************************************************************************************/
class ListenerType
{
  private long type;
  
  NotificationListener listener;
  
	/*********************************************************************************************************************
  <b>Description</b>: Constructs a ListenerType instance.

  <br>
  @param listener Listener instance
  @param type Type of events the listener is interested in
	*********************************************************************************************************************/
  public ListenerType(NotificationListener listener, long type)
  {
    this.type = type;
    this.listener = listener;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Checks the specified event types against this listener's types.

  <br>
  @param type Event types
  @return True if the listener is interested is all specified events, false otherwise
	*********************************************************************************************************************/
  public boolean ofType(long type)
  {
    return((this.type & type) != 0x00L);
  }
}
