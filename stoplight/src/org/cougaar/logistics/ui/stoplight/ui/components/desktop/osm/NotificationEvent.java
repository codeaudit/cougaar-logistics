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

/***********************************************************************************************************************
<b>Description</b>: This class provides information of a notification event.

***********************************************************************************************************************/
public class NotificationEvent extends java.util.EventObject
{
	/*********************************************************************************************************************
  <b>Description</b>: Represents a storage event.  This event signals that a new object has been stored.
	*********************************************************************************************************************/
  public static final long STORE             = 0x01L;
	/*********************************************************************************************************************
  <b>Description</b>: Represents an update event.  This event signals that an object has been updated.
	*********************************************************************************************************************/
  public static final long UPDATE            = 0x02L;
	/*********************************************************************************************************************
  <b>Description</b>: Represents an object updated event.  This event signals that an object has been replaced by a
                      new, updated object.
	*********************************************************************************************************************/
  public static final long OBJECT_UPDATE     = 0x04L;
	/*********************************************************************************************************************
  <b>Description</b>: Represents an expiration update event.  This event signals that a new expiration time has been
                      assigned to an object.
	*********************************************************************************************************************/
  public static final long EXPIRATION_UPDATE = 0x08L;
	/*********************************************************************************************************************
  <b>Description</b>: Represents a deletion event.  This event signals that an object has been deleted.
	*********************************************************************************************************************/
  public static final long DELETE            = 0x10L;
	/*********************************************************************************************************************
  <b>Description</b>: Represents all events.  This event is the ORed combination of all events:
                        STORE | UPDATE | OBJECT_UPDATE | EXPIRATION_UPDATE | DELETE
	*********************************************************************************************************************/
  public static final long ANY               = STORE | UPDATE | OBJECT_UPDATE | EXPIRATION_UPDATE | DELETE;
  
  private long notificationType = 0;
  
  private ObjectID oid;
  
  private Object previousObject;

  private long expiration;
  private boolean expirationSet;
  
	/*********************************************************************************************************************
  <b>Description</b>: Constructs a NotificationEvent instance based on the supplied parameters.

  <br><b>Notes</b>:<br>
	                  - The notificationType parameter can be any ORed combination of any of the event types, i.e. one
	                    notification can contain multiple events
	                  - The expiration parameter is only valid if the expirationSet parameter is true

  <br>
  @param oid Object ID of the object the event occurred on
  @param source Actual object referenced by the object ID
  @param previousObject Previous object, if event is of OBJECT_UPDATE type, referenced by the object ID
  @param notificationType Type(s) of event(s)
  @param expiration Expiration time of the object in the form of milliseconds since 1970
  @param expirationSet Indication of validity of expiration time, if true the time is valid and the object has an
           expiration time
	*********************************************************************************************************************/
  public NotificationEvent(ObjectID oid, Object source, Object previousObject, long notificationType, long expiration, boolean expirationSet)
  {
    super(source);
    this.oid = oid;

    this.notificationType = notificationType;

    this.previousObject = previousObject;

    this.expiration = expiration;
    this.expirationSet = expirationSet;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the type(s) of event(s) contained by this notification ORed together.

  <br>
  @return Type(s) of event(s)
	*********************************************************************************************************************/
  public long getNotificationType()
  {
    return(notificationType);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the notification represents a store event.

  <br>
  @return True if this notification represents a store event, false otherwise
	*********************************************************************************************************************/
  public boolean isStore()
  {
    return((notificationType & STORE) != 0x00L);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the notification represents an update event.

  <br>
  @return True if this notification represents an update event, false otherwise
	*********************************************************************************************************************/
  public boolean isUpdate()
  {
    return((notificationType & UPDATE) != 0x00L);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the notification represents an object updated event.

  <br>
  @return True if this notification represents an object updated event, false otherwise
	*********************************************************************************************************************/
  public boolean isObjectUpdate()
  {
    return((notificationType & OBJECT_UPDATE) != 0x00L);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the notification represents an expiration update event.

  <br>
  @return True if this notification represents an expiration update event, false otherwise
	*********************************************************************************************************************/
  public boolean isExpirationUpdate()
  {
    return((notificationType & EXPIRATION_UPDATE) != 0x00L);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the notification represents a deletion event.

  <br>
  @return True if this notification represents a deletion event, false otherwise
	*********************************************************************************************************************/
  public boolean isDelete()
  {
    return((notificationType & DELETE) != 0x00L);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the object the event is for has an expiration time.

  <br>
  @return True if this notification event's object has an expiration time
	*********************************************************************************************************************/
  public boolean isExpirationSet()
  {
    return(expirationSet);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the expiration time of the event's object.

  <br><b>Notes</b>:<br>
	                  - The expiration time returned is only valid if the method NotificationEvent.isExpirationSet()
	                    returns true

  <br>
  @return Expiration time of the event's object in the form of milliseconds since 1970
	*********************************************************************************************************************/
  public long getExpiration()
  {
    return(expiration);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the object ID of the object the event references.

  <br>
  @return Object ID of the object the event references
	*********************************************************************************************************************/
  public ObjectID getObjectID()
  {
    return(oid);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the previously referenced object if the event type is an object updated event.

  <br>
  @return Previous value of the object the event references
	*********************************************************************************************************************/
  public Object getPreviousObject()
  {
    return(previousObject);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Retruns the text representation of the event object.

  <br>
  @return Text representation of the event object
	*********************************************************************************************************************/
  public String toString()
  {
    return(oid + ":" + getSource() + ":" + previousObject + ":" + notificationType + ":" + expiration + ":" + expirationSet);
  }
}
