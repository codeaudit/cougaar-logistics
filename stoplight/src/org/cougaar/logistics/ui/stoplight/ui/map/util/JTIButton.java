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
import javax.swing.tree.*;
import java.awt.BorderLayout;

import org.cougaar.logistics.ui.stoplight.transducer.*;

import java.awt.event.*;
import javax.swing.event.*;

public class JTIButton extends JButton implements Serializable {

    JTIFrame jtif;
    public JTIButton() {}
    String fname="";
    public void setFilename(String name) { fname=name; }
    public String getFilename() { return fname; }
    
    public JFrame getMenuFrame() { return jtif; }
    public JTIButton(String filename, final JTIButtonAction jtiba) {
    final JTIButton selfButton=this;
    // System.out.println("Jf: " +filename);
	fname=filename;
	jtif=new JTIFrame(fname);
	jtif.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

	jtif.addTreeSelectionListener(new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
		    String selected = jtif.getSelectedString();
		    jtiba.act(selected);
		    jtif.hide();
        selfButton.setLabel(jtif.getSelectedString());
		    System.out.println("Painting " +jtif.getSelectedString());

		}		
	    });

	addActionListener( new ActionListener(){
	    public void actionPerformed(ActionEvent e) {
 		    jtif.show();
		    System.out.println("jtib up " +e);
	    }
	});

	setLabel(jtif.getTitle());
	setBorder(BorderFactory
		  .createTitledBorder(BorderFactory
				      .createEtchedBorder(), jtif.getTitle()));

    }

    /*
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                                   tree.getLastSelectedPathComponent();

                if (node == null) return;

                Object nodeInfo = node.getUserObject();
 		String displayText="Cannot Display Tasks for cluster:  "+nodeInfo+"\n\n";
		// 		displayText+=SocietyProxy.query(new TasksForOrgQuery(nodeInfo));
// 		htmlPane.setText(displayText);
		
		displayURL(SocietyProxy.query(new TasksForOrgQuery(nodeInfo.toString())), displayText); 
                if (DEBUG) {
                    System.out.println(nodeInfo.toString());
                }
            }
        });

     */

    public static void main(String[] argv) {
	JTIButton jtib;
	JTIButtonAction jtibaction = new JTIButtonAction() {
		public void act(String sel) {
		    System.err.println("jtibaction for selection: "+sel);
		}
	    };
	if (argv.length < 1) {
	    System.out.println("Please specify JTree file.");
	    
	    jtib=new JTIButton("\\dev\\ui\\menus\\jtmetrics.xml", 
					 jtibaction);
	} else {
	     jtib=new JTIButton(argv[0], jtibaction);
	}
	    JFrame frame=new JFrame();
	    frame.getContentPane().add(jtib);
	    frame.setSize(400, 600);
	    // frame.pack();
	    System.out.println("Pop up...");
	    frame.setVisible(true);

	    frame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			System.exit(0);
		    }
		});

    }
}
