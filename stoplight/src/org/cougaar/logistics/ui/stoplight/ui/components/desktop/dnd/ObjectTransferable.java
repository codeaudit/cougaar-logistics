/***********************************************************************************************************************
  Clark Software Engineering, Ltd. Copyright 2001
***********************************************************************************************************************/

//package com.clarksweng.dnd;
package org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

/***********************************************************************************************************************
<b>Description</b>: Object to hold drag and drop data objects.

***********************************************************************************************************************/
public class ObjectTransferable implements Transferable
{
  private static DataFlavor javaLocalObjectFlavor = null;
  private static Vector flavorList = new Vector(1);

  static
  {
    try
    {
      javaLocalObjectFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
    }
    catch (ClassNotFoundException e)
    {
      // This should never happen
      e.printStackTrace();
    }
  }
  
  public DataFlavor[] dataFlavorList = null;
  private Object data = null;

	/*********************************************************************************************************************
  <b>Description</b>: Contructs the DnD transport device with the object specified.  All possible java class data
                      flavors are generated for the specified object (i.e. all inherited classes/interfaces, including
                      java.lang.Object are used to determine data flavors).

  <br><b>Notes</b>:<br>
	                  - Both local JVM and serializable (for transport across JVMs) data flavors are created

  <br>
  @param object Object to transfer when the drag and drop operation is completed successfully
	*********************************************************************************************************************/
  public ObjectTransferable(Object object)
  {
  	data = object;

    dataFlavorList = getAllDataFlavors(data.getClass());
  }

  private static final synchronized DataFlavor[] getAllDataFlavors(Class type)
  {
    flavorList.clear();

    // For some reason, we need this flavor to present in the list for this to work
    flavorList.add(javaLocalObjectFlavor);

    addDataFlavors(type);

    return((DataFlavor[])flavorList.toArray(new DataFlavor[flavorList.size()]));
  }

  private static final synchronized void addDataFlavors(Class type)
  {
    Class[] interfaces = type.getInterfaces();
    for (int i=0; i<interfaces.length; i++)
    {
      addDataFlavors(interfaces[i]);
    }
    
    if (type.getSuperclass() != null)
    {
      addDataFlavors(type.getSuperclass());
    }

    // Set the local data flavor
    flavorList.add(getDataFlavor(type, true));
    // Set the serializable data flovor, if the type is Serializable
    if (type.isAssignableFrom(Serializable.class))
    {
      flavorList.add(getDataFlavor(type, false));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Implementation of for the Transferable.getTransferDataFlavors() interface method.  Returns all
                      possible java class data flavors generated from the data object.

  <br>
  @return All data flavors representing the data object type
	*********************************************************************************************************************/
  public synchronized DataFlavor[] getTransferDataFlavors()
  {
    return(dataFlavorList);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Implementation of for the Transferable.isDataFlavorSupported() interface method.  Determines if
                      the specified data flavor matches one of the data flavors of the data object.

  <br>
  @param flavor Data flavor to compare
  @return True if the data flavor matches at least one of the data flavors derived from the data object
	*********************************************************************************************************************/
  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    for (int i=0; i<dataFlavorList.length; i++)
    {
      if (flavor.equals(dataFlavorList[i]))
      {
        return(true);
      }
    }

    return(false);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Implementation of for the Transferable.getTransferData() interface method.  Will return the
                      instance of the data object.

  <br><b>Notes</b>:<br>
	                  - The actual (or serialized copy of, if dragged to another JVM) data object is returned

  <br>
  @param flavor Data flavor to compare
  @return Instance of the data object

  @throws UnsupportedFlavorException If the specified data flavor is not compatible with the data object
	*********************************************************************************************************************/
  public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
	{
		if (isDataFlavorSupported(flavor))
		{
		  return(data);
		}

	  throw(new UnsupportedFlavorException (flavor));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets the most precise local JVM data flavor of the specified class.

  <br>
  @param classType Class type to build the data flavor from
  @return Local JVM data flavor of the specified class
  
  @see #getDataFlavor(Class, boolean)
	*********************************************************************************************************************/
  public static final DataFlavor getDataFlavor(Class classType)
  {
    return(getDataFlavor(classType, true));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets the most precise local JVM or serializable data flavor of the specified class, depending on
                      the local boolean.

  <br>
  @param classType Class type to build the data flavor from
  @param local If true, build a local JVM data flavor, otherwise build a serializable data flavor
  @return Local JVM data flavor of the specified class if the local parameter is true, serializable data flavor of the
          specified class if the local parameter is true
  
  @see #getDataFlavor(Class)
	*********************************************************************************************************************/
  public static final DataFlavor getDataFlavor(Class classType, boolean local)
  {
    if (local)
    {
      return(new DataFlavor("application/x-java-jvm-local-objectref; class=" + classType.getName(), classType.getName()));
    }

  	return(new DataFlavor(classType, classType.getName()));

/*
    DataFlavor flav = null;

    try
    {
      /*System.out.println(DataFlavor.javaJVMLocalObjectMimeType);
      System.out.println(DataFlavor.javaRemoteObjectMimeType);
      System.out.println(DataFlavor.javaRemoteObjectMimeType);
      System.out.println(new DataFlavor(classType, classType.getName()).getMimeType());*/

      // application/x-java-jvm-local-objectref
      // application/x-java-remote-object
      // application/x-java-remote-object
      // application/x-java-serialized-object; class=java.lang.String
//      flav = new DataFlavor("application/x-java-serialized-object; class=" + classType.getName(), classType.getName());
//      flav = new DataFlavor(classType.getName(), classType.getName());

//      flav = new DataFlavor("application/x-java-jvm-local-objectref; class=" + classType.getName(), classType.getName());

/*    }
    catch (ClassNotFoundException e)
    {
      // This shouldn't happen since we already have the class object
      e.printStackTrace();
    }
  	return(flav);
    */

  }
}
