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
import java.util.Vector;

/***********************************************************************************************************************
<b>Description</b>: This class stores objects hieratically according to their class types and builds sub-class
                    references to other objects of the same type that are lower in the inheritance tree.  This class is
                    designed to provide fast access to a group of objects of the same type.

***********************************************************************************************************************/
public class FamilyTree
{
//  private static final boolean debug = true;
  private static final boolean debug = false;
  
  private Hashtable tree = new Hashtable(1);
  private Vector roots = new Vector(0);
  
	/*********************************************************************************************************************
  <b>Description</b>: Adds an object to the tree.

  <br>
  @param os ObjectStorage instance that references the object to be added
	*********************************************************************************************************************/
  public void add(ObjectStorage os)
  {
    synchronized(this)
    {
      FamilyLink link = (FamilyLink)tree.get(os.object.getClass());
      if (link == null)
      {
        link = new FamilyLink();
        link.type = os.object.getClass();
if (debug) System.out.println("traverseParent() new Link: " + link.type);
        tree.put(link.type, link);
        
        traverseParent(link, link.type);
      }
      
      if (!link.instances.contains(os))
      {
        link.instances.add(os);
      }
    }

    if (debug)
    {
      for (int i=0, isize=roots.size(); i<isize; i++)
      {
        printTree((FamilyLink)roots.elementAt(i), 0);
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Prints the current hierarchy tree of classes of objects that have been added to the tree,
                      recursively.  This method is used as a debug tool.

  <br>
  @param parentLink FamilyLink to start recursion from
  @param level Starting level (recursion starts at level 0, or root level) (used for indentation)
	*********************************************************************************************************************/
  private void printTree(FamilyLink parentLink, int level)
  {
    synchronized(this)
    {
      for (int i=0; i<level; i++)
      {
        System.out.print("  ");
      }
      System.out.println(parentLink.type);
      Vector siblingLinks = parentLink.siblingLinks;
      for (int i=0, isize=siblingLinks.size(); i<isize; i++)
      {
        printTree((FamilyLink)siblingLinks.elementAt(i), level +1);
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Recursively ascends the object hierarchy to determine the relationships between classes.

  <br>
  @param childLink FamilyLink to start recursion from
  @param childType Class type of the link
	*********************************************************************************************************************/
  private void traverseParent(FamilyLink childLink, Class childType)
  {
    synchronized(this)
    {
      Class superClass = childType.getSuperclass();
      Class[] interfaces = childType.getInterfaces();
if (debug) System.out.println("traverseParent() child: " + childType + " super: " + superClass);
      
      if (superClass != null)
      {
        FamilyLink superLink = (FamilyLink)tree.get(superClass);
        
        if (superLink == null)
        {
          superLink = new FamilyLink();
          superLink.type = superClass;
if (debug) System.out.println("traverseParent() new Link: " + superClass);
          tree.put(superClass, superLink);
        }

        if (!superLink.siblingLinks.contains(childLink))
        {
          superLink.siblingLinks.add(childLink);
          traverseParent(superLink, superClass);
        }
      }
/*      else if (childType != Object.class)
      {
        FamilyLink superLink = (FamilyLink)tree.get(Object.class);
        if (!superLink.siblingLinks.contains(childLink))
        {
          superLink.siblingLinks.add(childLink);
        }
      }*/
      else
      {
        if (!roots.contains(childLink))
        {
          roots.add(childLink);
        }
      }

      for (int i=0; i<interfaces.length; i++)
      {
        FamilyLink superLink = (FamilyLink)tree.get(interfaces[i]);
        
        if (superLink == null)
        {
          superLink = new FamilyLink();
          superLink.type = interfaces[i];
if (debug) System.out.println("traverseParent() new Link: " + interfaces[i]);
          tree.put(interfaces[i], superLink);
        }
          
        if (!superLink.siblingLinks.contains(childLink))
        {
          superLink.siblingLinks.add(childLink);
          traverseParent(superLink, interfaces[i]);
        }
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes an object from the tree.

  <br>
  @param os ObjectStorage instance that references the object to be removed
	*********************************************************************************************************************/
  public void remove(ObjectStorage os)
  {
    synchronized(this)
    {
      FamilyLink link = (FamilyLink)tree.get(os.object.getClass());
      if (link != null)
      {
        link.instances.remove(os);
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Finds all objects that represent the specified class type.

  <br>
  @param objectType Class type to find
  @return Array of ObjectStorage instances that contain objects that match or are sub classes of the specified type
	*********************************************************************************************************************/
  public ObjectStorage[] find(Class objectType)
  {
if (debug) System.out.println("find(): " + objectType);
    synchronized(this)
    {
      Vector objectList = new Vector(0);
      FamilyLink link = (FamilyLink)tree.get(objectType);
      if (link != null)
      {
if (debug) System.out.println("find(): " + link);
        storeInstances(objectList, link);
      }

      return((ObjectStorage[])objectList.toArray(new ObjectStorage[objectList.size()]));
    }
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Recursively stores object instances into a Vector.

  <br>
  @param objectList Vector to store instances within
  @param parentLink Starting link
	*********************************************************************************************************************/
  private void storeInstances(Vector objectList, FamilyLink parentLink)
  {
    synchronized(this)
    {
      objectList.addAll(parentLink.instances);
      Vector siblingLinks = parentLink.siblingLinks;
      for (int i=0, isize=siblingLinks.size(); i<isize; i++)
      {
        storeInstances(objectList, (FamilyLink)siblingLinks.elementAt(i));
      }
    }
  }
}

/***********************************************************************************************************************
<b>Description</b>: This class holds the information on each node in the FamilyTree class.

***********************************************************************************************************************/
class FamilyLink
{
  public Class type;
  public Vector instances = new Vector(0);
  public Vector siblingLinks = new Vector(0);
  
	/*********************************************************************************************************************
  <b>Description</b>: Provides a text representation of the link information.

  <br>
  @return Text representation of the link information
	*********************************************************************************************************************/
  public String toString()
  {
    String string = "Class: " + type + " : " + instances.size();
    if (siblingLinks.size() > 0)
    {
      string += " Subclasses: " + siblingLinks;
    }

    return(string);
  }
}
