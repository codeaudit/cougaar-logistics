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


package org.cougaar.logistics.ui.servicediscovery;

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
