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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory;

import java.util.Vector;
import java.util.Hashtable;

import java.awt.Event;
import java.awt.event.ActionEvent;


/** 
 * <pre>
 * 
 * The InventoryMenuEvent has all the menu commands and allows
 * mechanism to broadcast the given menu event to other parts
 * of the inventory UI.
 * 
 * 
 * @see InventoryUIFrame
 *
 **/

public class InventoryMenuEvent extends ActionEvent 
{
    public final static String MENU_Exit = "Exit";
    public final static String MENU_Connect = "Connect..";
    public final static String MENU_SaveXML = "Save XML..";

    public InventoryMenuEvent(Object source, String command) {
	super(source,Event.ACTION_EVENT,command);
    }

}


