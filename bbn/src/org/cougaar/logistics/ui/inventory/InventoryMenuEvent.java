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
    public final static String MENU_OpenXML = "Open XML File..";
    public final static String MENU_Help = "Help..";
    public final static String MENU_Pref = "Preferences";
    public final static String MENU_ShowXML = "Show XML Debug..";

    public final static String MENU_DemandChart = "Demand Chart";
    public final static String MENU_RefillChart = "Refill Chart";
    public final static String MENU_InventoryChart = "Inventory Chart";

    public InventoryMenuEvent(Object source, String command) {
	super(source,Event.ACTION_EVENT,command);
    }

}


