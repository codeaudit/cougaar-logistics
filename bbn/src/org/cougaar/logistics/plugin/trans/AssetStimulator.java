/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.logistics.plugin.trans;

import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;

import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.Filters;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.TimeSpan;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.lib.callback.UTILAssetCallback;
import org.cougaar.lib.callback.UTILAssetListener;
import org.cougaar.glm.parser.GLMTaskParser;
import org.cougaar.glm.util.AssetUtil;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.glm.ldm.asset.PhysicalAsset;
import org.cougaar.planning.ldm.plan.Schedule;

import org.cougaar.lib.filter.UTILPluginAdapter;

import org.cougaar.lib.util.UTILAllocate;
import org.cougaar.lib.util.UTILPreference;
import org.cougaar.lib.util.UTILAsset;

import java.util.Date;

/**
 * <pre>
 * Parses an XML file defining tasks to send to other clusters.
 * Creates tasks defined in the XML file and sends them 
 * to any subordinate, supporting, or provider clusters.
 *
 * Pops up a dialog box which provides a way to specify the 
 * XML file.  This defaults to whatever is in the ClusterInputFile 
 * parameter.
 *
 * In addition to sending tasks, they can also be rescinded.  
 * Tasks that have been sent are rescinded one at a time, with
 * each button press.
 * 
 * Implements the org listener interface so it can get all
 * reported orgs.
 *
 * (This code evolved from a version in the COUGAAR tree.)
 * </pre>
 */
public class AssetStimulator extends UTILPluginAdapter
  implements UTILAssetListener {

  /** Add the filter for all organizations... */
  public void setupFilters () {
    super.setupFilters ();

    if (isInfoEnabled())
      info (getName () + " : Filtering for Physical Assets.");

    addFilter (myAssetCallback = new UTILAssetCallback (this, logger));
  }

  public boolean interestingAsset (Asset a) {
    return (a instanceof PhysicalAsset);
  }
  
  /**
   * needed to implement the Listener interface.
   * Does nothing.
   *
   */
  public void handleChangedAssets (Enumeration e) {}

  /**
   * needed to implement the Listener interface.
   * Does nothing.
   *
   */
  public void handleNewAssets (Enumeration e) {}

  /**
   * Actually create the GUI window.  Two buttons, a text input box,
   * and a status line.
   * 
   * @param infile - default input file. From param 
   *                 "ClusterInputFile" defined in <ClusterName>.env.xml.
   */
  private void createGUI(String infile) {
    frame = new JFrame(getClusterName () + " - AssetStimulator");
    frame.getContentPane().setLayout(new BorderLayout());

    JPanel panel = new JPanel();
    JButton button = new JButton("Add Asset");
    JButton button2 = new JButton("Change Asset");
    JLabel label = new JLabel("                                             ");
    JLabel label2 = new JLabel("Click add to add a new truck. Click change to change the item id of the first physical asset.");

    ActionListener myGLMListener = new ActionListener () {
	public void actionPerformed(ActionEvent e) {
	  String lnfName = e.getActionCommand();

	  if (lnfName.equals("Add Asset")) {
	    addAsset ();
	  }
	  else {
	    changeAsset ();
	  }
	}
      };
	
    button.addActionListener(myGLMListener);
    button2.addActionListener(myGLMListener);

    panel.add(button);
    panel.add(button2);
    frame.getRootPane().setDefaultButton(button); // hitting return sends the tasks

    frame.getContentPane().add("Center", panel);
    frame.getContentPane().add("South", label);
    frame.getContentPane().add("North", label2);
    frame.pack();
    frame.setVisible(true);
  }

  void addAsset () {
    try {
      blackboard.openTransaction();
      publishAdd (makeNewAsset (getLDMService().getLDM(), "RAILCAR_68DODX", "brandNewAsset"));
    } catch (Exception exc) {
      error("Could not make asset.");
      error(exc.getMessage());
      exc.printStackTrace();
    }
    finally{
      blackboard.closeTransactionDontReset();
    }
  }

  void changeAsset () {
    try {
      blackboard.openTransaction();

      Collection collection = myAssetCallback.getSubscription ().getCollection();
		
      Asset firstAsset = (Asset) collection.iterator ().next ();
      ((NewItemIdentificationPG)firstAsset.getItemIdentificationPG()).setItemIdentification (
											     firstAsset.getItemIdentificationPG ().getItemIdentification () + 
											     "_changed");
      publishChange (firstAsset);
    } catch (Exception exc) {
      error("Could not change asset.");
      error(exc.getMessage());
      exc.printStackTrace();
    }
    finally{
      blackboard.closeTransactionDontReset();
    }
  }

  protected Asset makeNewAsset (LDMServesPlugin ldm, String prototype, String id) {
    // Create the instance
    UTILAsset assetHelper = new UTILAsset (logger);
    Asset newAsset = assetHelper.createInstance(ldm, prototype, id );

    Date now  = new Date ();
    Date then = new Date (now.getTime () - 24*60*60*1000);
	
    Schedule copySchedule = ldm.getFactory().newSimpleSchedule(then, now);
    // Set the Schedule
    ((NewRoleSchedule)newAsset.getRoleSchedule()).setAvailableSchedule(copySchedule);

    info ("Making new asset " + newAsset + " with UID " + newAsset.getUID());
    return newAsset;
  }
  
  /**
   * Reads the ClusterInputFile parameter, brings up the GUI.
   */
  public void localSetup () {
    String infile = null;
    try{infile=getMyParams ().getStringParam("ClusterInputFile");}
    catch(Exception e){infile = "                          ";}
    createGUI(infile);
  }

  /** frame for 2-button UI */
  JFrame frame;

  /** The callback mediating the org subscription */
  protected UTILAssetCallback myAssetCallback;
}






