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
package org.cougaar.mlm.ui.newtpfdd.gui.view.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class LineQuery extends SqlQuery {
    
    private List unitIDList = null;
    private List carrierPrototypeIDList = null;
    private List cargoPrototypeIDList = null;
    private List carrierInstanceIDList = null;
    private List cargoInstanceIDList = null;
  //    private boolean fullLines = true;

  public LineQuery (/*QueryData oldQuery*/) {
	/*
	setUnitIDList             (toList(oldQuery.getUnitNames ()));
	setCarrierPrototypeIDList (toList(oldQuery.getCarrierTypes ()));
	setCargoPrototypeIDList   (toList(oldQuery.getCargoTypes ()));
	setCarrierInstanceIDList  (toList(oldQuery.getCarrierTypes ()));
	setCargoInstanceIDList    (toList(oldQuery.getCargoTypes ()));
	*/
  }
  
  protected List toList (String [] strings) {
	List ret = new ArrayList ();
	for (int i = 0; i < strings.length; i++)
	  ret.add (strings[i]);
	return ret;
  }
  
    public List getUnitIDList() { return unitIDList; }
    public void setUnitIDList(List unitIDList) { this.unitIDList = unitIDList; }

    public List getCarrierPrototypeIDList() { return carrierPrototypeIDList; };
    public void setCarrierPrototypeIDList(List carrierPrototypeIDList) { this.carrierPrototypeIDList = carrierPrototypeIDList; };

    public List getCargoPrototypeIDList() { return cargoPrototypeIDList; };
    public void setCargoPrototypeIDList(List cargoPrototypeIDList) { this.cargoPrototypeIDList = cargoPrototypeIDList; };

    public List getCarrierInstanceIDList() { return carrierInstanceIDList; };
    public void setCarrierInstanceIDList(List carrierInstanceIDList) { this.carrierInstanceIDList = carrierInstanceIDList; };

    public List getCargoInstanceIDList() { return cargoInstanceIDList; };
    public void setCargoInstanceIDList(List cargoInstanceIDList) { this.cargoInstanceIDList = cargoInstanceIDList; };

  //    public boolean getFullLines() { return fullLines; }
  //    public void setFullLines(boolean fullLines) { this.fullLines = fullLines; }

  /**
   * some sql here, probably built using a string buffer. ... 
   * don't forget to handle dates and doubles in a DB safe way and to use ' quotes.  
   * See /tops/src/org/cougaar/domain/mlm/ui/grabber/config/DBConfig 
   * for examples of functions for doing oracle/my sql syntax)
   */	
  protected String getSqlQuery () {
	StringBuffer sb = new StringBuffer ();
	
	sb.append ("");
	return sb.toString ();
  }
  
  protected void handleResult (ResultSet rs, QueryResponse response) {
	try {
	while(rs.next()){
	  //        process a row as you see fit.  use rs.getXXXX()
	}
	} catch (SQLException e) {e.printStackTrace();
	}
  }
}
