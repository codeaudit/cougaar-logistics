/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.ui.stoplight.ui.map.util;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.BorderLayout;
import org.cougaar.logistics.ui.stoplight.transducer.*;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

import java.awt.event.*;

public class JTreeInterpreter  {

    String title="";
    public String getTitle() { return title; }
  public JTreeInterpreter () {
  }


    public JTree generate(Structure str) {
	DefaultMutableTreeNode top;
	JTree tree=null;

	Attribute atr=str.getAttribute("hierarchy");
	title= getFirstValForAttribute(atr);

	// get first list in structure
	ListElement me = str.getContentList();
	if (me != null) {
	    tree = new JTree(generateBranch(me));
	}
	return tree;
    }

    String getFirstValForAttribute(Attribute atr) {
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

    private String getNodeUID(ListElement le) {
	Attribute atr=le.getAttribute("UID");
	return getFirstValForAttribute(atr);
// 	String name="NO_NAME";
// 	Enumeration en1=null;
// 	ValElement val=null;
	
// 	if (atr != null) {
// 	    en1=atr.getChildren();
// 	}
// 	if (en1!=null && en1.hasMoreElements()) {
// 	    val=((Element)en1.nextElement()).getAsValue();
// 	}
// 	if (val!=null) {
// 	    name = val.getValue();
// 	}
// 	return name;
    }

    private DefaultMutableTreeNode generateBranch(ListElement le) {
	DefaultMutableTreeNode branch;
	String name = getNodeUID(le);
	branch = new DefaultMutableTreeNode(name);

	for (Enumeration en=le.getChildren(); en.hasMoreElements(); ) {
	// for each child of le that is a list 
	    ListElement child=((Element)en.nextElement()).getAsList();
	    if (child != null) {
		branch.add(generateBranch(child));
	    }
	}
	return branch;
    }

    public static void main(String[] argv) {
	JFrame frame;
	JTreeInterpreter jti = new JTreeInterpreter();
	
	if (argv.length < 1)
	    System.out.println("Please specify JTree file.");
	else {
	    try {
		XmlInterpreter xint = new XmlInterpreter();
		
		FileInputStream fin = new FileInputStream(argv[0]);
		Structure s = xint.readXml(fin);
		fin.close();
		
		JTree jtree=jti.generate(s);
		if (jtree==null) {
		    System.err.println("NULL JTree.  Make sure that your file parses correctly.");
		} else {
		    frame=new JFrame();

		    frame.getContentPane().add(jtree, BorderLayout.CENTER);
		    frame.setTitle("Cluster Command Structure");
		    frame.pack();
		    frame.setSize(200, 400);
		    frame.setVisible(true);

		    frame.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent e) {
				System.exit(0);
			    }
			});
		}
	    } catch (Exception ex) {
		System.err.println("Exception: "+ex.getMessage());
		ex.printStackTrace();
	    }
	}
    }
}
