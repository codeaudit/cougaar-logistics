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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.*;

import java.io.Serializable;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.util.Vector;
import java.util.EventObject;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CDesktopFrame;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI;

public class DnDSourceFileGUI extends org.cougaar.logistics.ui.stoplight.ui.components.desktop.ComponentFactory implements org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI/*, DragSource*/
{
  
  private String rootPath = null;
  JScrollPane scrollPane = null;
  
  public void install(CDesktopFrame f)
  {
  	System.out.println("install with " + rootPath);

    FileNode rootNode = new FileNode();
    File[] childArray = File.listRoots();
    for(int i = 1; i < childArray.length; i++)
    {
    	FileNode nextNode = createTreeModel(childArray[i].getAbsolutePath());
    	rootNode.add(nextNode);
    	System.out.println("%%%% node " + childArray[i].getAbsolutePath());
    }
    final DnDJTree tree = new DnDJTree(rootNode, f, null);
    f.setTitle("Local Files");
try
{
    createTreeModel(rootNode);
}
catch (Exception e)
{
System.out.println("Throwing exception");
e.printStackTrace();
  throw(new RuntimeException(e.toString()));
}
tree.setRootVisible(false);


System.out.println("Here0");

    if (rootNode.getChildCount() > 0)
    {
    	System.out.println("make child visible");
      tree.makeVisible(new TreePath(((DefaultMutableTreeNode)rootNode.getFirstChild()).getPath()));
    }

    scrollPane = new JScrollPane(tree);
    f.getContentPane().add(scrollPane, BorderLayout.CENTER);
    
    tree.addTreeExpansionListener(new TreeExpansionListener()
    {
    	public void treeCollapsed(TreeExpansionEvent e)
    	{
    		
    	}
    
    
    public void treeExpanded(TreeExpansionEvent e)
    	{
    		System.out.println("%%%% local expansion - path = " + e.getPath());
    		TreePath path = e.getPath();
    		
    	  FileNode lastNode = (FileNode)path.getLastPathComponent();
    	  System.out.println("%%%% local lastnode is " + lastNode);
    	  if(!lastNode.isExplored())
    	  {
	    	  String filePath = ((FileNode)lastNode).getFile().getAbsolutePath();
	    	  System.out.println("%%%% local expansion path string " + filePath);
	    	  
	    	  FileNode node = createTreeModel(filePath);
	    	  System.out.println("%%%% local node is " + node);
	    	  node.explore();
	    	  
	    	  DefaultTreeModel model = (DefaultTreeModel) tree.getModel();  //  original tree
	    	  
	    	  
	    	  FileNode[] nodes = new FileNode[path.getPathCount()];
	    	  int childIndex = 0;
	    	  for(int i = 0; i < path.getPathCount(); i++)
	    	  {
	    	  	nodes[i] = (FileNode)path.getPathComponent(i);
	    	  }
	    	  int childCount = model.getChildCount(nodes[path.getPathCount() - 2]);
	    	  for(int i = 0; i < childCount; i++)
	    	  {
	    	  	FileNode childNode = (FileNode) model.getChild(nodes[path.getPathCount() - 2], i);
	    	  	if(childNode.getFile().getName().equals(lastNode.getFile().getName()))
	    	  	{
	    	  	  childIndex = i;
	    	  	}
	    	  }
	    	  System.out.println("%%%% parent node " + nodes[path.getPathCount() - 2]); 
	    	  System.out.println("%%%% child index = " + childIndex);
	    	  model.removeNodeFromParent(lastNode);
	    	  model.insertNodeInto(node, nodes[path.getPathCount() - 2], childIndex);
	    	  model.nodeStructureChanged(node);
    	  }
    	  
    	}
    });
    
    
  }
  
  public static void moveFile(FileInfo childInfo)
  {
  	try
  	{
	  	File xferfile = childInfo.getDropFile();
	   	String destination = childInfo.getDestination();
	   	
	   	System.out.println("%%%% abs file path " + xferfile.getAbsolutePath());
	   	File file = xferfile.getAbsoluteFile();
	   	
	   	System.out.println("%%%% destination = " + destination);
	    byte[] byteArray = childInfo.getByteArray();
	    String newFilename = destination + "\\" + file.getName();
	    System.out.println("%%%% new file name "  + newFilename );
	    FileOutputStream fos = new FileOutputStream(newFilename);
	    fos.write(byteArray);
	    fos.close();
    }
	  catch(Exception ioe)
	  {
	  	ioe.printStackTrace();
	  }
	    
  }
  
  
  private void createTreeModel(DefaultMutableTreeNode rootNode)
  {
    File[] roots = File.listRoots();

    for (int i=0; i<roots.length; i++)
    {
     try
     {	
      if (!roots[i].getAbsolutePath().equalsIgnoreCase("A:\\"))
      {
        FileNode node = new FileNode(roots[i]);
        node.explore();
        rootNode.add(node);
      }
     }
     catch(Exception e)
     {
     	System.out.println("%%%% tree model error");
     }
    }
  }

  private FileNode createTreeModel(String rootPath)
  {
  	File root = new File(rootPath);
  	FileNode rootNode = new FileNode(root);
  	rootNode.explore();
  	
  	return rootNode;
  }

  public void setRootPath(String rPath)
  {
  	rootPath = rPath;
  	System.out.println("root path set " + rPath);
  }
  
  public void removeOldHierarchy(CDesktopFrame f)
  {
  	f.remove(scrollPane);
  	f.repaint();
  }


	public String getToolDisplayName()
	{
	  return("DnD Source Test UI");
	}

	public CougaarDesktopUI create()
	{
	  return(this);
	}

  public boolean supportsPlaf()
  {
    return(true);
  }

  public void install(JFrame f)
  {
    throw(new RuntimeException("install(JFrame f) not supported"));
  }

  public void install(JInternalFrame f)
  {
  	System.out.println("internalframe");
    throw(new RuntimeException("install(JInternalFrame f) not supported"));
  }

  public boolean isPersistable()
  {
    return(false);
  }

  public Serializable getPersistedData()
  {
    return(null);
  }

  public void setPersistedData(Serializable data)
  {
  }

  public String getTitle()
  {
    return("DnD Source Test UI");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(100, 100));
  }

  public boolean isResizable()
  {
    return(true);
  }
}

