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
import java.awt.Container;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;

import java.io.Serializable;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.net.URLConnection;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.JButton;

import java.util.Vector;
import java.util.EventObject;
import java.util.Enumeration;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.*;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;
import org.cougaar.util.OptionPane;

public class RemoteFileNodeGUI extends org.cougaar.logistics.ui.stoplight.ui.components.desktop.ComponentFactory 
                               implements org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI
{
  
  private String rootPath = null;
  JScrollPane scrollPane = null;
  private JTextArea textArea = new JTextArea();
  private Color background = textArea.getBackground();
  protected static String ls = System.getProperty("line.separator");
  public DnDJTree tree = null;
  static Object messageString = null;
  String url = "http://localhost:5555/";
  Container frame = null;
   
  public void install(CDesktopFrame f)
  {
  	System.out.println("install with " + rootPath );
  	String urlPrefix = null;
  	
  	frame = f;
  	f.setJMenuBar(new JMenuBar());
  	
  	urlPrefix = GetConnectionData(f);
  	System.out.println("%%%% remotefilenodegui connection string = " + urlPrefix);
  	url = urlPrefix;
  	f.setTitle(url);
  	f.setSize(200, 400);
  	tree = new DnDJTree(createRemoteTreeModel("sendHelp"), f, urlPrefix);
  	           
    if(tree == null)
      return;
    
    tree.setRootVisible(false);
    scrollPane = new JScrollPane(tree);
    f.getContentPane().add(scrollPane, BorderLayout.CENTER);
   
    tree.addTreeExpansionListener(new TreeExpansionListener()
    {
    	public void treeCollapsed(TreeExpansionEvent e)
    	{
    		
    	}
    	public void treeExpanded(TreeExpansionEvent e)
    	{
    		System.out.println("%%%% expansion - path = " + e.getPath());
    		TreePath path = e.getPath();
    		
    	  FileNode lastNode = (FileNode)path.getLastPathComponent();
    	 
    	  System.out.println("%%%% node is " + lastNode);
    	  if(!lastNode.isExplored())
    	  {
	    	  String filePath = ((FileNode)lastNode).getFile().getAbsolutePath();
	    	  System.out.println("%%%% expansion path string " + filePath);
	    	  
	    	  FileNode node = createRemoteTreeModel(filePath);
	    	  System.out.println("%%%% explore remote node  " + node);
	    	  //node.explore();
	    	  
	    	  DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
	    	  //model.nodeStructureChanged(lastNode);
	    	  
	    	  FileNode[] nodes = new FileNode[path.getPathCount()];
	    	  for(int i = 0; i < path.getPathCount(); i++)
	    	  {
	    	  	nodes[i] = (FileNode)path.getPathComponent(i);
	    	  }
	    	  int childCount = model.getChildCount(nodes[path.getPathCount() - 2]);
	    	  int childIndex = 0;
	    	  for(int i = 0; i < childCount; i++)
	    	  {
	    	  	FileNode childNode = (FileNode) model.getChild(nodes[path.getPathCount() - 2], i);
	    	  	if(childNode.getFile().getName().equals(lastNode.getFile().getName()))
	    	  	{
	    	  	  childIndex = i;
	    	  	}
	    	  }
	    	  System.out.println("%%%% parent node " + nodes[path.getPathCount() - 2]); 
	    	  model.removeNodeFromParent(lastNode);
	    	  model.insertNodeInto(node, nodes[path.getPathCount() - 2], childIndex);
	    	  model.nodeStructureChanged(node);
    	  }
    	 
    	}
    });
    
    //f.show();
   
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
  
  
  // ------------------- DragSource Interface ----------------------------  

  public Vector getTargetComponents()
  {
    Vector components = new Vector(1);
    components.add(textArea);
    //components.add(tree);
    
    return(components);
  }

  public boolean dropToSubComponents()
  {
    return(true);
  }

  public boolean readyForDrop(Point location)
  {
    return(true);
  }

  
  
  
  public static void sendToMover(FileInfo file, String urlPrefix)
  {
  	Vector preps = new Vector();
  	String parameters = "sendHelp";
  	String urlString = urlPrefix;
    //System.out.println("%%%% url is " + urlString);
    
    try
    {
      String uriString = urlString + "/FILEMOVER.PSP";
      URL url = new URL(uriString);
      URLConnection urlCon = url.openConnection();

      // Setup the communication parameters with the specifed URL
      urlCon.setDoOutput(true);
      urlCon.setDoInput(true);
      urlCon.setAllowUserInteraction(false);
      
      ObjectOutputStream server = new ObjectOutputStream(urlCon.getOutputStream());
      server.writeObject(file);
           
      System.out.println("%%%% sent file");
      server.flush();
      server.close();
      InputStream is = urlCon.getInputStream();
    
    }
  
  catch (Exception e)
  {
      System.err.println ("FileMover sending exception: " + e.toString());
      //e.printStackTrace();
  }

  }

  public Vector getSupportedDataFlavors()
  {
    Vector flavors = new Vector(1);
    flavors.add(ObjectTransferable.getDataFlavor(FileInfo.class));
    
    
    return(flavors);
  }
  
  
  

	public String getToolDisplayName()
	{
	  return("Remote Node Test UI");
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
  	return(url + " Remote Files ");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(100, 100));
  }

  public boolean isResizable()
  {
    return(true);
  }
  
  //public Task createHeartBeatTask(PlanningFactory theLDMF, String org, String verb)
  //{
  private FileNode createRemoteTreeModel(String parameters)
  {
  	Vector preps = new Vector();
  	FileNode fileNode = null;
  	String urlString = url;
    //System.out.println("%%%% url is " + urlString);
    
    try
    {
      String uriString = urlString + "/FINDNODE.PSP";
      URL url = new URL(uriString);
      URLConnection urlCon = url.openConnection();

      // Setup the communication parameters with the specifed URL
      urlCon.setDoOutput(true);
      urlCon.setDoInput(true);
      urlCon.setAllowUserInteraction(false);
      
      
      
      // Send the parameter (GET/POST data) to the specified URL
      
      
      DataOutputStream server = new DataOutputStream(urlCon.getOutputStream());
			server.writeBytes(parameters);
			server.close();
      System.out.println("%%%% sent request");
      
      InputStream is = urlCon.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(is);
      fileNode = (FileNode) ois.readObject();
      System.out.println("got FileNode,  " + fileNode);
    }
  
  catch (Exception e)
  {
      System.err.println ("RemoteFileNode sending exception: " + e.toString());
      //e.printStackTrace();
  }

	  return fileNode;
	  
  }
  
   
  private JFileChooser getFileSystemView()
  {
  	Vector preps = new Vector();
  	String parameters = "sendHelp";
  	JFileChooser fileview  = null;
  	String urlString = url;
    System.out.println("%%%% url is " + urlString);   
    try
    {
      String uriString = urlString + "/FINDFILESYSTEM.PSP";
      URL url = new URL(uriString);
      URLConnection urlCon = url.openConnection();

      // Setup the communication parameters with the specifed URL
      urlCon.setDoOutput(true);
      urlCon.setDoInput(true);
      urlCon.setAllowUserInteraction(false);
           
      
      // Send the parameter (GET/POST data) to the specified URL
      
      
      DataOutputStream server = new DataOutputStream(urlCon.getOutputStream());
			server.writeBytes(parameters);
			server.close();
      System.out.println("%%%% sent request");
      
      InputStream is = urlCon.getInputStream();
      
      ObjectInputStream ois = new ObjectInputStream(is);
      
      fileview = (JFileChooser) ois.readObject();
      
      System.out.println("got FileSystemView,  ");
    }
  
  catch (Exception e)
  {
      System.err.println ("RemoteFileNode sending exception: " + e.toString());
      //e.printStackTrace();
  }

	  return fileview;
	  
  }
  
  
  public String GetConnectionData(CDesktopFrame frame) 
   {
     
        String msg = "Enter cluster Log Plan Server location as host:port";

        String host = "localhost";
        String port = "5555";
        String defaultString = host + ":" + port;
        if ((messageString = OptionPane.showInputDialog(frame, msg, "Cluster Location", 3, null, null, defaultString)) == null)
        {
          return null;
        }
        
        String s = (String)messageString;
        s = s.trim();
        if (s.length() != 0)
        {
          int i = s.indexOf(":");
          if (i != -1)
          {
            host = s.substring(0, i);
            port = s.substring(i+1);
          }
        }
        String clusterHost = host;
        String clusterPort = port;
        String hostAndPort = "http://" + clusterHost + ":" + clusterPort + "/";
        System.out.println("%%%% hostandport " + hostAndPort);
        return hostAndPort;
     
   }
   
   
   
   
 
  
}

