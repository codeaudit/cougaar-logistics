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
     * @param
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