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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.datatransfer.*;

import java.awt.event.*;
import java.io.*;
import java.net.URLConnection;
import java.net.URL;

import java.util.Vector;
import java.util.Enumeration;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;

public class DnDJTree extends JTree
                  implements TreeSelectionListener 
{


  /** Stores the parent Frame of the component */
  private CDesktopFrame Parent = null;

  /** Stores the selected node info */
  protected TreePath SelectedTreePath = null;
  protected FileNode SelectedNode = null;
  private JButton button = new JButton("File Copy In Progress");
  
  private Point cursorLocation = null;
  private String urlPrefix = null;
  
  /** Constructor 
  @param root The root node of the tree
  @param parent Parent JFrame of the JTree */
  public DnDJTree(FileNode root, CDesktopFrame parent, String url) 
  {
    super(root);
    Parent = parent;
    Parent.getContentPane().add(button, BorderLayout.WEST);	
    button.setVisible(false);
    System.out.println("%%%% dndjtree new one ");
    addTreeSelectionListener(this);
    urlPrefix = url;
    new DragSourceDropTarget(this);
    
    
    
    
    //unnecessary, but gives FileManager look
    putClientProperty("JTree.lineStyle", "Angled");
    MetalTreeUI ui = (MetalTreeUI) getUI();
  }

  /** Returns The selected node */
  public FileNode getSelectedNode() 
  {
    return SelectedNode;
  }
  
  ///////////////////////// Interface stuff ////////////////////

  /** TreeSelectionListener - sets selected node */
  public void valueChanged(TreeSelectionEvent evt) 
  {
    SelectedTreePath = evt.getNewLeadSelectionPath();
    if (SelectedTreePath == null) 
    {
      SelectedNode = null;
      System.out.println("%%%% selectedtreepath == null");
      return;
    }
    
    SelectedNode = 
      (FileNode)SelectedTreePath.getLastPathComponent();
    System.out.println("%%%% selectedtreepath != null " + SelectedNode);
  }
  
  
  public boolean isChild(TreeNode newParent, TreeNode selection)
  {
  	Enumeration trees = newParent.children();
  	while(trees.hasMoreElements())
  	{
  		FileNode fn = (FileNode) trees.nextElement();
  		FileNode sel = (FileNode) selection;
  		if(fn.getFile().getName().equals(sel.getFile().getName()))
  		  return true;
  	}
  	return false;
  }
  public void showMessage()
  {
  	//Parent.getContentPane().add(button, BorderLayout.WEST);	
  	button.setVisible(true);
	 	Parent.show();
	}
  public void unshowMessage()
  {
  	button.setVisible(false);
  	
  	//Parent.getContentPane().remove(button);
  	Parent.show();
  }
  private class DragSourceDropTarget implements DragSource, DropTarget
  {
  		// Drag & Drop supporting class
    private DragAndDropSupport dndSupport = new DragAndDropSupport();
    private Color background = null;
  	DnDJTree jTree = null;
  	Point toLocation = null;
  	public DragSourceDropTarget(DnDJTree tree)
  	{
  		jTree = tree;
  		background = jTree.getBackground();
  		// Add the drag source
      dndSupport.addDragSource(this);
     // Add the drop target
      dndSupport.addDropTarget(this);
  	} 
 // ------------------- DragSource Interface ----------------------------  

	  public Vector getSourceComponents()
	  {
	    Vector components = new Vector(1);
	    components.add(jTree);
	    
	    return(components);
	  }
	
	  public boolean dragFromSubComponents()
	  {
	    return(true);
	  }
	
	  public Object getData(Component componentAt, Point location)
	  {
	  	//Get the selected node
      FileInfo dragObject = null;
      Vector textArea = new Vector(5);
      FileNode dragNode = getSelectedNode();
      if(dragNode == null)
        return null;
	  	TreePath treePath = getPathForLocation(location.x, location.y);
		  System.out.println("%%%%at getData url is " + urlPrefix);	
	    
	    // Create the drag object (FileInfo)
	    
	    if (dragNode != null && !dragNode.isDirectory()) 
	    {
      	dragObject = new FileInfo(dragNode.getUserObject().toString(), true);
      	dragObject.setTextArea(textArea);
      	dragObject.setLength(dragNode.length());
      	System.out.println("%%% setting prefix into filenode -> " + urlPrefix);
      	dragObject.setUrl(urlPrefix);
      	dragObject.setDropFile(dragNode.getFile());
	    }   
	    return(dragObject);
	  }
	
	  public void dragDropEnd(boolean success)
	  {
  	}
	
		// ------------------- DropTarget Interface ----------------------------  
	
	  public Vector getTargetComponents()
	  {
	    Vector components = new Vector(1);
	    components.add(jTree);
	    
	    return(components);
	  }
	
	  public boolean dropToSubComponents()
	  {
	    return(true);
	  }
	
	  public boolean readyForDrop(Component componentAt, Point location, DataFlavor flavor)
	  {
	  	//System.out.println("%%% setting point loc " + location);
	  	toLocation = location;
	    return(true);
	  }
	
	  public void showAsDroppable(Component componentAt, Point location, DataFlavor flavor, boolean show, boolean droppable)
	  {
			if(show)
			{
			  if (droppable)
			  {
				  jTree.setBackground(Color.green);
			  }
			  else
			  {
				  jTree.setBackground(Color.red);
				}
			}
			else
			{
				jTree.setBackground(background);
			}
	  }
	
	  public void dropData(Component componentAt, Point location, DataFlavor flavor, final Object data)
	  {
	  	//System.out.println("%%%% tree drop entered " + urlPrefix);
	  	
	  	//  Run the drag n drop in a seperate thread to allow access during long file movements
	  	
     (new Thread()
			{
				public void run()
				{
					
					//JOptionPane.showMessageDialog(null,"Info", "Copy In Progress", JOptionPane.ERROR_MESSAGE);		
					showMessage();
			    try 
			    {
			    	        
			      FileInfo childInfo = (FileInfo) data;
			      //get new parent node
			      
			      Point loc = toLocation;
			      TreePath destinationPath = getPathForLocation(loc.x, loc.y);
			      //System.out.println("%%%% dndjtree destination is " + (FileNode) destinationPath.getLastPathComponent());
			      FileNode newParent =
			        (FileNode) destinationPath.getLastPathComponent();
			      			    
			      //get old parent node
			      
			      //System.out.println("%%%% dndjtree selectred node is " + getSelectedNode());
			      
			      //  set the path in the fileNode to the new path
			      
			      File dest = newParent.getFile();  // destination 
			      childInfo.setDestination(dest.getPath());
			      String newFilename = dest.getPath() + "\\" + childInfo.getDropFile().getName();
			      //System.out.println("%%%% destination path = " + newFilename);
			      File newFile = new File(newFilename);
			      FileNode newChild = new FileNode(newFile);
			      newChild.isDir = false;
			      try 
			      { 
			        if(!isChild(newParent, newChild))
			        {
			          newParent.add(newChild);
			          System.out.println("%%%% child not in node");
			        }
			      }
			      catch (java.lang.IllegalStateException ils) 
			      {
			        
			      }
						      	      
			      //expand nodes appropriately - this probably isnt the best way...
			      
			      DefaultTreeModel model = (DefaultTreeModel) getModel();
			      
			      model.reload(newParent);
			      TreePath parentPath = new TreePath(newParent.getPath());
			      expandPath(parentPath);
			      
			      //  prepare to send to copy file
			      
			      File xferFile = childInfo.getDropFile();  // source
			      
			      if(childInfo.getUrl() == null && urlPrefix == null)  // it's drag from local machine to local
			      {
			        copyLocal(childInfo);
			      }
			      else  if(childInfo.getUrl() != null && urlPrefix != null) // it's drag from remote machine to remote
			      {
			      	copyRemote(childInfo.getUrl(), childInfo);
			      }
			      else  if(childInfo.getUrl() != null && urlPrefix == null) // it's drag from remote machine to local
			      {
			      	copyRemoteToLocal(childInfo.getUrl(), childInfo);
			      }
			      else  if(childInfo.getUrl() == null && urlPrefix != null) // it's drag from local machine to remote
			      {
			      	copyLocalToRemote(urlPrefix, childInfo);
			      }
			    }
			    catch (Exception io) 
			    { 
			    	System.out.println("%%%% caught file io exception");
			    	io.printStackTrace(); 
			    }
	        unshowMessage();
	    }
			}).start(); 
	    
  }
		
  public Vector getSupportedDataFlavors(Component componentAt, Point location)
  {
    Vector flavors = new Vector(1);
    flavors.add(ObjectTransferable.getDataFlavor(FileInfo.class));
    
    return(flavors);
  } 
  	
 }
  
  
  public FileOutputStream getOutputStream(FileInfo childInfo)
  {
  	File xferFile = childInfo.getDropFile();  // source
  	File file = xferFile.getAbsoluteFile();
  	System.out.println("%%%% file is directory " + file.isDirectory());
  	String destination = childInfo.getDestination();
  	String newFilename = destination + "\\" + file.getName();
  	FileOutputStream fos = null;
  	try
  	{
    	fos = new FileOutputStream(newFilename);
    }
    catch(Exception e)
    {
    	e.printStackTrace();
    }
    
		System.out.println("%%%% new file name "  + newFilename );
		return fos;
  }
  
  
  
  public URLConnection getPSPConnection(String urlString, String psp)
  {
  	URLConnection urlCon = null;
  	try
    {
    	String uriString = urlString + psp;
      URL url = new URL(uriString);
      urlCon = url.openConnection();

      // Setup the communication parameters with the specifed URL
      urlCon.setDoOutput(true);
      urlCon.setDoInput(true);
      urlCon.setAllowUserInteraction(false);
    }
    catch(Exception e)
    {
    	e.printStackTrace();
    }
    return urlCon;
  }
  
  public void copyLocal(FileInfo childInfo)
  {
  	try
  	{
  		File xferFile = childInfo.getDropFile();  // source
  		int fileLength = (int)xferFile.length();
  		int xferLength = 0;
  		FileInputStream is = new FileInputStream(xferFile);
  		File file = xferFile.getAbsoluteFile();
  		System.out.println("%%%% abs file path " + xferFile.getAbsolutePath());
		  FileOutputStream fos = getOutputStream(childInfo);
		  
  		
  		if(fileLength <= 10000000)  //  xfer in one pass if 10mb or less
	  	{
	  		xferLength = fileLength;
	  		byte[] byteArray = new byte[xferLength];
		  	is.read(byteArray);
			  fos.write(byteArray);
	    }
	    else
	    {
  		  xferLength = 10000000;
  		  int offset = 0;
  		  
  		  byte[] byteArray = new byte[xferLength];
  		  int someLength = 0;
  		  while((someLength = is.read(byteArray)) > 0)
  		  {
  		  	fos.write(byteArray, offset, someLength);
			  }
  		}
	    fos.close();
	  }
		catch (Exception io)
	  {
		  	io.printStackTrace();
		}
	  System.out.println("%%%% local move");
  }
  
  public void copyRemoteToLocal(String remoteUrl, FileInfo childInfo)
  {
  	File xferFile = childInfo.getDropFile();  // source
  	String path = xferFile.getPath();
  	File file = xferFile.getAbsoluteFile();
  	//int fileLength = (int)xferFile.length();
  	int fileLength = (int) childInfo.getLength();
  	FileOutputStream fos = getOutputStream(childInfo);
  	String urlString = remoteUrl;
    //System.out.println("%%%% get remote file url is " + urlString);
    byte[] myByte = null;
    URLConnection urlConOut = getPSPConnection(urlString, "/FINDFILE.PSP");
    try
    {
      // Send the parameter (GET/POST data) to the specified URL
      
      
      DataOutputStream server = new DataOutputStream(urlConOut.getOutputStream());
			server.writeBytes(path);
			//server.close();
      System.out.println("%%%% sent request to read " + path + " of length " + fileLength);
      
      InputStream is = urlConOut.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(is);
      int fileTotal = 0;
      while(fileTotal < fileLength)
      {
      	System.out.println("%%%% read object from psp");
      	myByte = (byte[]) ois.readObject();
	      fos.write(myByte);
	      fileTotal += myByte.length;
	      System.out.println("got byteArray,  " + fileTotal);
      }
      
    }
  
	  catch (Exception e)
	  {
	      System.err.println ("jTree sending exception: " + e.toString());
	      e.printStackTrace();
	  }

	  
  }
  
  public void copyLocalToRemote(String remoteUrl, FileInfo childInfo)
  {
	  	File xferFile = childInfo.getDropFile();  // source
	  	String path = xferFile.getPath();
	  	File file = xferFile.getAbsoluteFile();
	  	int fileLength = (int)xferFile.length();
	  	int chunkSize = 5000000;
	  	String urlString = remoteUrl;
	    System.out.println("%%%% get remote file url is " + urlString);
	    byte[] myByte = null;
	    URLConnection urlConOut = null;
	    try
	    {
	    	urlConOut = getPSPConnection(urlString, "/FILEMOVER.PSP");
	    	FileInputStream is = new FileInputStream(xferFile);
	    	ObjectOutputStream server = null;
	    	if(fileLength <= chunkSize)  //  xfer in one pass if 10mb or less
	    	{
	    		server = new ObjectOutputStream(urlConOut.getOutputStream());
		  		byte[] byteArray = new byte[fileLength];
			  	is.read(byteArray);
			  	childInfo.setByteArray(byteArray);
				  server.writeObject(childInfo);
          System.out.println("%%%% sent file");
	        server.flush();
	        server.close();
	        InputStream isDumb = urlConOut.getInputStream();
		    }   
		    else
		    {
		    	int xferLength = chunkSize;
	  		  int offset = 0;
	  		  
	  		  byte[] byteArray = new byte[xferLength];
	  		  byte[] writeArray;
	  		  int someLength = 0;
	  		  childInfo.addFile = false;
	  		  while((someLength = is.read(byteArray)) > 0)
	  		  {
	  		  	writeArray = new byte[someLength];
	  		  	System.arraycopy(byteArray, 0, writeArray, 0, someLength);
	  		  	fileXferToRemote(writeArray, childInfo, urlString);
	  		  	childInfo.addFile = true;
				  }
		    	
		    }   
	    }
	  
		  catch (Exception e)
		  {
		      System.err.println ("jTree sending exception: " + e.toString());
		      //e.printStackTrace();
		  }
  }
  
  
  public void copyRemote(String remoteUrl, FileInfo childInfo)
  {
	  	File xferFile = childInfo.getDropFile();  // source
	  	String path = xferFile.getPath();
	  	File file = xferFile.getAbsoluteFile();
	  	int fileLength = (int) childInfo.getLength();
	  	String toUrlString = urlPrefix;
	  	System.out.println("%%%% to remote file url is " + toUrlString);
	  	String fromUrlString = remoteUrl;  // from url
	    System.out.println("%%%% from remote file url is " + fromUrlString);
	    byte[] myByte = null;
	    URLConnection urlConIn = getPSPConnection(fromUrlString, "/FINDFILE.PSP");
	    try
	    {
	    	
	      // Send the path to finder and get back file
	      	      
	      DataOutputStream inServer = new DataOutputStream(urlConIn.getOutputStream());
				inServer.writeBytes(path);
				inServer.close();
							
	      System.out.println("%%%% sent requestn " + path);
	      
	      InputStream is = urlConIn.getInputStream();
	      ObjectInputStream ois = new ObjectInputStream(is);
	      int fileTotal = 0;
	      childInfo.addFile = false;
	      while(fileTotal < fileLength)
	      {
	      	System.out.println("%%%% read object from psp");
	      	myByte = (byte[]) ois.readObject();
		      fileXferToRemote(myByte, childInfo, toUrlString);
		      fileTotal += myByte.length;
		      childInfo.addFile = true;
		      System.out.println("got byteArray,  " + fileTotal);
	      }
	      
	      
	    }
	  
		  catch (Exception e)
		  {
		      System.err.println ("jTree sending exception: " + e.toString());
		      //e.printStackTrace();
		  }
  }
  
  public void fileXferToRemote(byte[] byteArray, FileInfo childInfo, String urlString)
  {
  	byte[] writeArray;
	  URLConnection urlConOut = null;
	  ObjectOutputStream server = null;
	  System.out.println("%%%% begin send of fileInfo object");
  	try
  	{
	  	urlConOut = getPSPConnection(urlString, "/FILEMOVER.PSP");
	  	childInfo.setByteArray(byteArray);
	  	server = new ObjectOutputStream(urlConOut.getOutputStream());
	  	server.writeObject(childInfo);
	  	server.flush();
	    server.close();
	    InputStream ois = urlConOut.getInputStream();
	    ObjectInputStream receiver = new ObjectInputStream(ois);
	    String response = (String) receiver.readObject();
	    System.out.println("%%%% sent fileInfo object " + byteArray.length);
  }
  catch(Exception e)
  {
  	e.printStackTrace();
  }
	  
  }
 

} //end of DnDJTree
