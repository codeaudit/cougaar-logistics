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

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.io.Serializable;

import java.awt.dnd.*;

import javax.swing.*;
import javax.swing.event.*;



import org.cougaar.util.OptionPane;

import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.inventory.MenuUtility;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

public class DnDUI implements CougaarUI
{
	
	Container frame = null;
	private String rootPath = null;
	DnDSourceFileGUI srctest = null;
	
  public DnDUI()
  {
  
  }
  
  public void install(CDesktopFrame installFrame)
  {
  	frame = installFrame;
  	installFrame.setJMenuBar(new JMenuBar());
  	//rootPath = System.getProperty("user.dir");
  	rootPath = null;
  	buildTree(installFrame.getContentPane(), installFrame.getJMenuBar());
  
  
   /* installFrame.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        
        if (frame instanceof JFrame)
        {
          System.exit(0);
        }
        else if (frame instanceof JInternalFrame)
        {
      }
      
      }
    });	*/
  
      
          
  	installFrame.show();
    installFrame.validate();
 }
 
  public void install(JFrame installFrame)
	{
		frame = installFrame;
		installFrame.setJMenuBar(new JMenuBar());
		//rootPath = System.getProperty("user.dir");
		rootPath = null;
		buildTree(installFrame.getContentPane(), installFrame.getJMenuBar());
		
		
		installFrame.show();
	  installFrame.validate();
	}
	
	public void install(JInternalFrame installFrame)
	{
		frame = installFrame;
		//installFrame.setJMenuBar(new JMenuBar());
		//rootPath = System.getProperty("user.dir");
		rootPath = null;
		buildTree(installFrame.getContentPane(), installFrame.getJMenuBar());
		
		
		installFrame.show();
	  installFrame.validate();
	}
	
	public void buildTree(Container contentPane, JMenuBar menuBar)
  {
  	//createMenuAndDialogs(contentPane, menuBar);
  	srctest = new DnDSourceFileGUI();
  	srctest.setRootPath(rootPath);
  	srctest.install((CDesktopFrame)frame);
  	
  	
  }
  
  private void createMenuAndDialogs(final Container contentPane, JMenuBar menuBar)
  {
  	JMenu            fileMenu = new JMenu("File"); 
  	JMenuItem        menuItem;

	menuItem = new JMenuItem("Open");
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		JFileChooser      fc = new JFileChooser();

		//fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int               result = fc.showOpenDialog(frame);

		if (result == JFileChooser.APPROVE_OPTION) {
		    String      newPath = fc.getSelectedFile().getPath();
        System.out.println("file is " + newPath);
		    //new TreeTableExample2(newPath);
		    rootPath = newPath;
		    //srctest = new DnDSourceFileGUI();
		    srctest.removeOldHierarchy((CDesktopFrame)frame);
  	    srctest.setRootPath(newPath);
      	srctest.install((CDesktopFrame)frame);
		    
		}
	    }
	});
	fileMenu.add(menuItem);
	fileMenu.addSeparator();
	menuBar.add(fileMenu);
  }
  public boolean supportsPlaf()
  {
    return(true);
  }

}