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
import java.awt.dnd.*;
import java.awt.dnd.peer.*;
import java.awt.event.*;
import java.io.*;



public class FileNode extends DefaultMutableTreeNode implements Serializable
{
	private boolean explored = false;
	public boolean isDir = true;
	private long length = 0;
	public FileNode(File file)
	{
		setUserObject(file);
		isFileDirectory();
		length = file.length();
	}
	public FileNode()
	{
		
	}
	
	public boolean getAllowedChildren(){return isDirectory();}
	public boolean isLeaf(){ return !isDirectory();}
	public File getFile() {return (File)getUserObject();}
	
	public boolean isExplored() {return explored;}
	public boolean isFileDirectory()
	{
		File file  = getFile();
		
		if(file == null)
		{
			return true;
		}
		else
		{
			return file.isDirectory();
		}
		
	}
	public boolean isDirectory()
	{
		/*File file  = getFile();
		if(file == null)
		  return true;
		return file.isDirectory();*/
		return isDir;
	}
  public void setDirectory(boolean isdir)
  {
  	isDir = isdir;
  }
  
  public long length()
  {
  	return length;
  }
  public String toString()
  {
  	File file = (File) getUserObject();
		String filename = null;
		if(file == null)
		  filename = "Empty";
		else
		  filename = file.toString();
		int index = filename.lastIndexOf("\\");
		
		return (index != -1 && index != filename.length() - 1) ? filename.substring(index + 1) : filename;
	}
	
	public void explore()
	{
		if(!isExplored())
		{
			File file = getFile();
			isDir = isFileDirectory();
			File[] children = file.listFiles();
			if(children != null)
			for(int i = 0; i < children.length; i++)
			{
				FileNode next = new FileNode(children[i]);
				next.isDir = next.isFileDirectory();
				//System.out.println("%%%% file " + children[i].getName());
				//System.out.println("%%%% file " + children[i].length());
				//System.out.println("%%%% next is dir " + next.isDirectory());
				add(next);
				
			}
			explored = true;
		}
	}
}
		
