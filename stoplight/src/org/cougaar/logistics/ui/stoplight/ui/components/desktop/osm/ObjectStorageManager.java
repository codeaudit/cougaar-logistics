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

import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.reflect.Array;

/***********************************************************************************************************************
<b>Description</b>: The main object storage manager class used to store and retrieve objects.

<br><br><b>Notes</b>:<br>
									- An instance of this class is used in the CougaarDesktop to store/share application data

***********************************************************************************************************************/
public class ObjectStorageManager
{
//  private static final boolean debug = true;
  private static final boolean debug = false;
  
  private Hashtable oidToOSTable = new Hashtable(1);
  private InstanceHashtable objectToOSTable = new InstanceHashtable(1);
  
  private FamilyTree familyTree = new FamilyTree();

  private Hashtable oidKeyListeners = new Hashtable(1);
  private Hashtable classListeners = new Hashtable(1);

  // Store access ---------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Stores an object in the manager.  This method will attempt to store an object and return an object
                      ID that references the object

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param object Object to store
  @return Object ID associated with object instance

  @throws RuntimeException If the object has already been stored in the manager
	*********************************************************************************************************************/
  public ObjectID store(Object object)
  {
    return(store(new ObjectStorage(object)));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Stores an object in the manager.  This method will attempt to store an object and return an object
                      ID that references the object

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param object Object to store
  @param expiration Expiration time of the object instance in the form of milliseconds since 1970
  @return Object ID associated with object instance

  @throws RuntimeException If the object has already been stored in the manager
	*********************************************************************************************************************/
  public ObjectID store(Object object, long expiration)
  {
    return(store(new ObjectStorage(object, expiration)));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Stores an object in the manager.  This method will attempt to store an object and return an object
                      ID that references the object

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param os Object data and information internalized in a ObjectStorage instance
  @return Object ID associated with object instance
  
  @throws RuntimeException If the object has already been stored in the manager
	*********************************************************************************************************************/
  private ObjectID store(ObjectStorage os)
  {
    synchronized(this)
    {
      Object objToStore = objectToOSTable.get(os.object);
      if (objToStore != null)
      {
        throw(new RuntimeException("Object already stored: " + os.object));
      }

      ObjectID oid = os.oid;

      oidToOSTable.put(oid, os);
      objectToOSTable.put(os.object, os);
      familyTree.add(os);

      return(oid);
    }
  }

  // Update access ---------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Updates an object in the manager.  This method will attempt to mark an object stored in the
                      manager as being updated, this will trigger any listeners with criteria that the object meets
                      to be notified of the update.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param oid Object ID of the object to be updated

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void update(ObjectID oid)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }

      os.actionType = NotificationEvent.UPDATE;
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Updates an object in the manager.  This method will attempt to mark an object stored in the
                      manager as being updated, this will trigger any listeners with criteria that the object meets
                      to be notified of the update.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param object Object to be updated

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void update(Object object)
  {
    synchronized(this)
    {
      update(getObjectID(object));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Updates an object in the manager.  This method will attempt to mark an object stored in the
                      manager as being updated with a new instance of the object type, this will trigger any listeners
                      with criteria that the object meets to be notified of the update.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param oid Object ID of the object to be updated
  @param object Object to replace current object

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void update(ObjectID oid, Object object)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      
      if (os.object.getClass() != object.getClass())
      {
        throw(new IllegalArgumentException("Wrong class type, required: " + os.object.getClass() + " found: " + object.getClass()));
      }
      
      os.actionType = NotificationEvent.OBJECT_UPDATE;
      os.previousObject = os.object;
      os.object = object;

      objectToOSTable.remove(os.previousObject);
      objectToOSTable.put(object, os);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Updates an object in the manager.  This method will attempt to mark an object stored in the
                      manager as being updated with a new expiration time, this will trigger any listeners
                      with criteria that the object meets to be notified of the update.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param oid Object ID of the object to be updated
  @param expiration Object expiration time in the form of milliseconds since 1970

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void update(ObjectID oid, long expiration)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }

      os.actionType = NotificationEvent.EXPIRATION_UPDATE;
      os.expiration = expiration;
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Updates an object in the manager.  This method will attempt to mark an object stored in the
                      manager as being updated with a new expiration time, this will trigger any listeners
                      with criteria that the object meets to be notified of the update.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param object Object to be updated
  @param expiration Object expiration time in the form of milliseconds since 1970

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void update(Object object, long expiration)
  {
    synchronized(this)
    {
      update(getObjectID(object), expiration);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Updates an object in the manager.  This method will attempt to mark an object stored in the
                      manager as being updated with a new expiration time, this will trigger any listeners
                      with criteria that the object meets to be notified of the update.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param oid Object ID of the object to be updated
  @param object Object to replace current object
  @param expiration Object expiration time in the form of milliseconds since 1970

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void update(ObjectID oid, Object object, long expiration)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
  
      if (os.object.getClass() != object.getClass())
      {
        throw(new IllegalArgumentException("Wrong class type, required: " + os.object.getClass() + " found: " + object.getClass()));
      }
  
      os.actionType = NotificationEvent.OBJECT_UPDATE | NotificationEvent.EXPIRATION_UPDATE;
      os.previousObject = os.object;
      os.object = object;
      os.expiration = expiration;
      os.expirationSet = true;

      objectToOSTable.remove(os.previousObject);
      objectToOSTable.put(object, os);
    }
  }

  // Delete access ---------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Deletes an object in the manager.  This method will attempt to delete an object stored in the
                      manager, this will trigger any listeners with criteria that the object meets to be notified of
                      the deletion.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param oid Object ID of the object to be updated

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void delete(ObjectID oid)
  {
if (debug) System.out.println("Delete: " + oid);
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }

      os.actionType = NotificationEvent.DELETE;
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Deletes an object in the manager.  This method will attempt to delete an object stored in the
                      manager, this will trigger any listeners with criteria that the object meets to be notified of
                      the deletion.

  <br><b>Notes</b>:<br>
	                  - Commit must be invoked to complete this transaction

  <br>
  @param object Object to replace current object

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void delete(Object object)
  {
    synchronized(this)
    {
      delete(getObjectID(object));
    }
  }

  // Commit access ---------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Commits any pending transactions on the object with the specified object ID.

  <br>
  @param oid Object ID of the object

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void commit(ObjectID oid)
  {
if (debug) System.out.println("Commit: " + oid);
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }

      NotificationEvent event = os.commit();

if (debug) System.out.println("Commit event: " + event);

      if (event != null)
      {
        if (event.isDelete())
        {
          objectToOSTable.remove(os.object);
          oidToOSTable.remove(oid);
          familyTree.remove(os);
        }

        fireEventNotifications(event);

        if (event.isDelete())
        {
          oidKeyListeners.remove(oid);
        }
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Commits any pending transactions on the specified object.

  <br>
  @param object Object to commit

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void commit(Object object)
  {
    synchronized(this)
    {
      commit(getObjectID(object));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Commits any pending transactions on all objects stored in the manager.
  <br>
	*********************************************************************************************************************/
  public void commit()
  {
    synchronized(this)
    {
      for (Enumeration elements = oidToOSTable.elements(); elements.hasMoreElements(); )
      {
        ObjectStorage os = (ObjectStorage)elements.nextElement();

if (debug) System.out.println("Commit: " + os.oid);

        NotificationEvent event = os.commit();

if (debug) System.out.println("Commit event: " + event);

        if (event != null)
        {
          if (event.isDelete())
          {
            objectToOSTable.remove(os.object);
            oidToOSTable.remove(os.oid);
            familyTree.remove(os);
          }
  
          fireEventNotifications(event);
  
          if (event.isDelete())
          {
            oidKeyListeners.remove(os.oid);
          }
        }
      }
    }
  }

  // Operations with Commit access ---------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Calls store(Object) to store the specified object and then calls commit(ObjectID) to commit the
                      changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param object Object to store and commit
  @return Object ID associated with object instance
	*********************************************************************************************************************/
  public ObjectID storeCommit(Object object)
  {
    synchronized(this)
    {
      ObjectID oid = store(object);
      commit(oid);
  
      return(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls store(Object, long) to store the specified object and then calls commit(ObjectID) to commit
                      the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param object Object to store and commit
  @param expiration Object expiration time in the form of milliseconds since 1970
  @return Object ID associated with object instance

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public ObjectID storeCommit(Object object, long expiration)
  {
    synchronized(this)
    {
      ObjectID oid = store(object, expiration);
      commit(oid);
  
      return(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls update(ObjectID) to update the specified object and then calls commit(ObjectID) to commit
                      the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param oid Object ID of the object to update

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void updateCommit(ObjectID oid)
  {
    synchronized(this)
    {
      update(oid);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls update(ObjectID) to update the specified object and then calls commit(ObjectID) to commit
                      the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param object Object to update

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void updateCommit(Object object)
  {
    synchronized(this)
    {
      ObjectID oid = getObjectID(object);
      update(oid);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls update(ObjectID, Object) to update the specified object and then calls commit(ObjectID) to
                      commit the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param oid Object ID of the object to update
  @param object Object to replace current object

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void updateCommit(ObjectID oid, Object object)
  {
    synchronized(this)
    {
      update(oid, object);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls update(ObjectID, long) to update the specified object and then calls commit(ObjectID) to
                      commit the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param oid Object ID of the object to update
  @param expiration Object expiration time in the form of milliseconds since 1970

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void updateCommit(ObjectID oid, long expiration)
  {
    synchronized(this)
    {
      update(oid, expiration);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls update(ObjectID, long) to update the specified object and then calls commit(ObjectID) to
                      commit the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param object Object to update
  @param expiration Object expiration time in the form of milliseconds since 1970

  @throws IllegalArgumentException If the object is not stored in the manager

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void updateCommit(Object object, long expiration)
  {
    synchronized(this)
    {
      ObjectID oid = getObjectID(object);
      update(oid, expiration);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls update(ObjectID, Object, long) to update the specified object and then calls
                      commit(ObjectID) to commit the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param oid Object ID of the object to update
  @param object Object to replace current object
  @param expiration Object expiration time in the form of milliseconds since 1970

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void updateCommit(ObjectID oid, Object object, long expiration)
  {
    synchronized(this)
    {
      update(oid, object, expiration);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls delete(ObjectID) to delete the specified object and then calls commit(ObjectID) to commit
                      the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param oid Object ID of the object to update

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void deleteCommit(ObjectID oid)
  {
    synchronized(this)
    {
      delete(oid);
      commit(oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Calls delete(ObjectID) to delete the specified object and then calls commit(ObjectID) to commit
                      the changes.

  <br><b>Notes</b>:<br>
	                  - This is a synchronized atomic transaction

  <br>
  @param object Object to replace current object

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void deleteCommit(Object object)
  {
    synchronized(this)
    {
      ObjectID oid = getObjectID(object);
      delete(oid);
      commit(oid);
    }
  }

  // Outgoing queries ---------------------------------------------------
  
	/*********************************************************************************************************************
  <b>Description</b>: Gets the object ID associated with the specified object.

  <br>
  @param object Object to find ID of
  @return Object ID associated with object instance

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public ObjectID getObjectID(Object object)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)objectToOSTable.get(object);
      if (os == null)
      {
        throw(new IllegalArgumentException("Object not stored: " + object));
      }

      return(os.oid);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Determines if an object has an expiration date based on its object ID.

  <br>
  @param oid Object ID of the object to check expiration
  @return True if the instance has an expiration date, false otherwise

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public boolean isExpirationSet(ObjectID oid)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }
  
      return(os.expirationSet);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Determines if an object has an expiration date based on its object instance.

  <br>
  @param object Object to check expiration
  @return True if the instance has an expiration date, false otherwise

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public boolean isExpirationSet(Object object)
  {
    synchronized(this)
    {
      return(isExpirationSet(getObjectID(object)));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets the expiration time of and object specified by its object ID.

  <br>
  @param oid Object ID of the object to get expiration of
  @return Time, in the form of milliseconds since 1970, of when the object expires

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public long getExpiration(ObjectID oid)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }
  
      return(os.expiration);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets the expiration time of an object.

  <br>
  @param object Object to get expiration of
  @return Time, in the form of milliseconds since 1970, of when the object expires

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public long getExpiration(Object object)
  {
    synchronized(this)
    {
      return(getExpiration(getObjectID(object)));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Finds an object based on its object ID.

  <br>
  @param oid Object ID of the object to find
  @return The object found

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public Object find(ObjectID oid)
  {
    synchronized(this)
    {
      ObjectStorage os = (ObjectStorage)oidToOSTable.get(oid);
      if (os == null)
      {
        throw(new IllegalArgumentException("Invalid Object ID: " + oid));
      }
  
      return(os.object);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Finds all objects of the specified class type.

  <br>
  @param objectType Class type of the objects to find
  @return The objects found
	*********************************************************************************************************************/
  public Object[] find(Class objectType)
  {
    synchronized(this)
    {
      ObjectStorage[] os = familyTree.find(objectType);
      Object[] objects = (Object[])Array.newInstance(objectType, os.length);
      
      for (int i=0; i<os.length; i++)
      {
        objects[i] = os[i].object;
      }
      
      return(objects);
    }
  }

  // Listener access ---------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Sends event notification to all listeners registered for matching events.

  <br>
  @param event Event description
	*********************************************************************************************************************/
  private void fireEventNotifications(NotificationEvent event)
  {
    synchronized(this)
    {
      NotificationSupport listenerList = (NotificationSupport)oidKeyListeners.get(event.getObjectID());

if (debug) System.out.println("fireEventNotifications(): " + listenerList);

      if (listenerList != null)
      {
        listenerList.fireNotificationEvent(event);
      }

      fireEventNotificationsForClass(event, event.getSource().getClass());
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sends event notification to all listeners registered for matching events and class types.

  <br>
  @param event Event description
  @param childType Class type
	*********************************************************************************************************************/
  private void fireEventNotificationsForClass(NotificationEvent event, Class childType)
  {
    synchronized(this)
    {
      Class superClass = childType.getSuperclass();
      Class[] interfaces = childType.getInterfaces();
      if (superClass != null)
      {
        fireEventNotificationsForClass(event, superClass);
      }
      for (int i=0; i<interfaces.length; i++)
      {
        fireEventNotificationsForClass(event, interfaces[i]);
      }

      NotificationSupport listenerList = (NotificationSupport)classListeners.get(childType);

if (debug) System.out.println("fireEventNotificationsForClass(" + childType + "): " + listenerList);

      if (listenerList != null)
      {
        listenerList.fireNotificationEvent(event);
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a notification listener for the specified object ID.

  <br>
  @param oid Object ID to listen for events on
  @param listener Listener instance
	*********************************************************************************************************************/
  public void addNotificationListener(ObjectID oid, NotificationListener listener)
  {
    addNotificationListener(oid, listener, NotificationEvent.ANY);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a notification listener for the specified object.

  <br>
  @param object Object to listen for events on
  @param listener Listener instance

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void addNotificationListener(Object object, NotificationListener listener)
  {
    synchronized(this)
    {
      addNotificationListener(getObjectID(object), listener, NotificationEvent.ANY);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a notification listener for the specified object with specific events.

  <br>
  @param object Object to listen for events on
  @param listener Listener instance
  @param notificationType Types of events

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void addNotificationListener(Object object, NotificationListener listener, long notificationType)
  {
    synchronized(this)
    {
      addNotificationListener(getObjectID(object), listener, notificationType);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a notification listener for the specified object ID with specific events.

  <br>
  @param oid Object ID to listen for events on
  @param listener Listener instance
  @param notificationType Types of events
	*********************************************************************************************************************/
  public void addNotificationListener(ObjectID oid, NotificationListener listener, long notificationType)
  {
    synchronized(this)
    {
      NotificationSupport listenerList = (NotificationSupport)oidKeyListeners.get(oid);
      if (listenerList == null)
      {
        listenerList = new NotificationSupport();
        oidKeyListeners.put(oid, listenerList);
      }

      listenerList.addNotificationListener(listener, notificationType);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a notification listener for the specified object.

  <br>
  @param object Object associated with listener
  @param listener Listener instance

  @throws IllegalArgumentException If the object is not stored in the manager
	*********************************************************************************************************************/
  public void removeNotificationListener(Object object, NotificationListener listener)
  {
    synchronized(this)
    {
      removeNotificationListener(getObjectID(object), listener);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a notification listener for the specified object ID.

  <br>
  @param oid Object ID associated with listener
  @param listener Listener instance
	*********************************************************************************************************************/
  public void removeNotificationListener(ObjectID oid, NotificationListener listener)
  {
    synchronized(this)
    {
      NotificationSupport listenerList = (NotificationSupport)oidKeyListeners.get(oid);
      if (listenerList != null)
      {
        listenerList.removeNotificationListener(listener);
        if (listenerList.getListenerCount() == 0)
        {
          oidKeyListeners.remove(oid);
        }
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a notification listener for the specified class type..

  <br>
  @param objectType Class type to listen for events on
  @param listener Listener instance
	*********************************************************************************************************************/
  public void addNotificationListener(Class objectType, NotificationListener listener)
  {
    addNotificationListener(objectType, listener, NotificationEvent.ANY);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a notification listener for the specified class type with specific events.

  <br>
  @param objectType Class type to listen for events on
  @param listener Listener instance
  @param notificationType Types of events
	*********************************************************************************************************************/
  public void addNotificationListener(Class objectType, NotificationListener listener, long notificationType)
  {
    synchronized(this)
    {
      NotificationSupport listenerList = (NotificationSupport)classListeners.get(objectType);
      if (listenerList == null)
      {
        listenerList = new NotificationSupport();
        classListeners.put(objectType, listenerList);
      }

      listenerList.addNotificationListener(listener, notificationType);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a notification listener for the specified class type.

  <br>
  @param objectType Class type associated with listener
  @param listener Listener instance
	*********************************************************************************************************************/
  public void removeNotificationListener(Class objectType, NotificationListener listener)
  {
    synchronized(this)
    {
      NotificationSupport listenerList = (NotificationSupport)classListeners.get(objectType);
      if (listenerList != null)
      {
        listenerList.removeNotificationListener(listener);
        if (listenerList.getListenerCount() == 0)
        {
          classListeners.remove(objectType);
        }
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a notification listener for all objects, object IDs, events and class types.

  <br>
  @param listener Listener instance
	*********************************************************************************************************************/
  public void removeNotificationListener(NotificationListener listener)
  {
    synchronized(this)
    {
      for (Enumeration keys=classListeners.keys(); keys.hasMoreElements(); )
      {
        Object key = keys.nextElement();
        NotificationSupport listenerList = (NotificationSupport)classListeners.get(key);
        listenerList.removeNotificationListener(listener);
        if (listenerList.getListenerCount() == 0)
        {
          classListeners.remove(key);
        }
      }

      for (Enumeration keys=oidKeyListeners.keys(); keys.hasMoreElements(); )
      {
        Object key = keys.nextElement();
        NotificationSupport listenerList = (NotificationSupport)oidKeyListeners.get(key);
        listenerList.removeNotificationListener(listener);
        if (listenerList.getListenerCount() == 0)
        {
          oidKeyListeners.remove(key);
        }
      }
    }
  }
}
