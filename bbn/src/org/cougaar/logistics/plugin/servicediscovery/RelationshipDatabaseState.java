/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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


package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.mlm.ui.newtpfdd.gui.view.SimpleGanttChartView;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseState;
import org.cougaar.mlm.ui.newtpfdd.gui.view.PopupDialogSupport;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.CarrierDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;

import java.util.Date;
import java.util.Iterator;


public class RelationshipDatabaseState
    implements DatabaseState,PopupDialogSupport{

    RelationshipDatabaseState() {
    }

    public DatabaseConfig getDBConfig () {return null;}
    public DatabaseRun    getRun () {return null;}

    public void showAssetDetailView(AssetDetailRequest adr) {
       //System.out.println("RASwitchDatabaseState: AssetDetailRequest: " + adr);
    }

    public void showCarrierDetailView(CarrierDetailRequest cdr){
       //System.out.println("RASwitchDatabaseState: CarrierDetailRequest: " + cdr);
    }

}
