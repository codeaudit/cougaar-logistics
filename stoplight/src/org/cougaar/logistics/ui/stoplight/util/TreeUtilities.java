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
package org.cougaar.logistics.ui.stoplight.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.cougaar.lib.uiframework.transducer.XmlInterpreter;
import org.cougaar.lib.uiframework.transducer.elements.Attribute;
import org.cougaar.lib.uiframework.transducer.elements.Element;
import org.cougaar.lib.uiframework.transducer.elements.ListElement;
import org.cougaar.lib.uiframework.transducer.elements.Structure;
import org.cougaar.lib.uiframework.transducer.elements.ValElement;
import org.cougaar.lib.uiframework.ui.util.TreeInterpreter;

public class TreeUtilities
{
    /**
     * Returns a vector of all nodes under the given tree node.
     *
     * @param tn tree node to traverse
     * @param justLeaves if true, will only return leaf nodes
     * @return vector of all nodes under the given tree node
     */
    public static Vector getSubordinateList(TreeNode tn, boolean justLeaves)
    {
        Vector nodeList = new Vector();

        if (tn.isLeaf())
        {
            nodeList.add(tn);
        }
        else
        {
            Enumeration children = tn.children();
            while (children.hasMoreElements())
            {
                TreeNode node = (TreeNode)children.nextElement();
                nodeList.addAll(getSubordinateList(node, justLeaves));
            }
            if (!justLeaves)
            {
              nodeList.add(tn);
            }
        }
        return nodeList;
    }


    /**
     * Recreates a structure based on data from a file.
     *
     * @param fileName full filename from which to read.
     * @return structure based on data from the database.
     */
    public static Structure readFromFile (String fileName)
    {
        Structure s = null;
        try
        {
            XmlInterpreter xint = new XmlInterpreter();
            FileInputStream fin = new FileInputStream(fileName);
            s = xint.readXml(fin);
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * Saves a structure to a XML file.
     *
     * @param s The Structure to be saved
     * @param fileName the file to write the XMLized Structure to
     */
    public static void saveToFile(Structure s, String fileName)
    {
        try
        {
            XmlInterpreter xint = new XmlInterpreter();
            FileOutputStream fout = new FileOutputStream(fileName);
            xint.writeXml(s, fout);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
     }

    /**
     * Creates a tree model (viewable by a JTree) based on the contents of the
     * given structure.
     *
     * @param s structure on which to base contents of tree model
     * @return tree model based on the contents of the given structure.
     */
    public static DefaultMutableTreeNode createTree(Structure s)
    {
        TreeInterpreter ti = new TreeInterpreter();
        return ti.generate(s);
    }

    public static DefaultMutableTreeNode
        findNode(DefaultMutableTreeNode currentNode, String nodeString)
    {
        if (currentNode.getUserObject().toString().equals(nodeString))
        {
            return currentNode;
        }
        if (!currentNode.isLeaf())
        {
            for (int i = 0; i < currentNode.getChildCount(); i++)
            {
                DefaultMutableTreeNode foundNode =
                    findNode((DefaultMutableTreeNode)
                             currentNode.getChildAt(i), nodeString);
                if (foundNode != null) return foundNode;
            }
        }

        return null;
    }

    public static ListElement findListElement(String uid, ListElement le)
    {
        String elementUID = getFirstValForAttribute(le.getAttribute("UID"));
        if (elementUID.equals(uid))
            return le;

        for (Enumeration e = le.getChildren(); e.hasMoreElements();)
        {
            ListElement child = (ListElement)e.nextElement();
            ListElement foundElement = findListElement(uid, child);
            if (foundElement != null)
                return foundElement;
        }

        return null;
    }

    /**
     * Strip the given attribute from given list element and all subordinates
     */
    public static void stripTreeAttribute(ListElement le, String attributeName)
    {
      le.removeAttribute(attributeName);

      for (Enumeration e = le.getChildren(); e.hasMoreElements();)
      {
        ListElement child = (ListElement)e.nextElement();
        stripTreeAttribute(child, attributeName);
      }
    }

    private static String getFirstValForAttribute(Attribute atr)
    {
        String name="";
        Enumeration en1=null;
        ValElement val=null;

        if (atr != null) {
            en1=atr.getChildren();
        }
        if (en1!=null && en1.hasMoreElements()) {
            val=((Element)en1.nextElement()).getAsValue();
        }
        if (val!=null) {
            name = val.getValue();
        }
        return name;
    }
}