/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.details;

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * Shows a popup of the details about an asset, or all the 
 * assets carried on a leg.
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/27/01
 **/
public class AssetDetailView extends JFrame{

  //Constants:
  ////////////

  //Variables:
  ////////////
  protected DatabaseConfig dbConfig;
  protected int runID;

  //Constructors:
  ///////////////

  public AssetDetailView(DatabaseConfig dbConfig, int runID,
			 AssetDetailRequest adr){
    super("Asset Details");
    this.dbConfig=dbConfig;
    this.runID=runID;
    setupGUI(adr);
  }

  //Members:
  //////////
  
  protected void setupGUI(AssetDetailRequest adr){
    JPanel content=new JPanel(new BorderLayout());
    AssetDetailTableModel tableModel=
      new AssetDetailTableModel(dbConfig,runID);
    tableModel.fillWithData(adr);
    JTable table=new JTable(tableModel);
    JScrollPane sp =new JScrollPane(table);
    content.add(sp, BorderLayout.CENTER);

    JButton close=new JButton("Dismiss");
    close.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
	  dispose();
	}
      });
    content.add(close,BorderLayout.SOUTH);

    setContentPane(content);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(900,600);
    show();
  }

  //InnerClasses:
  ///////////////
}
