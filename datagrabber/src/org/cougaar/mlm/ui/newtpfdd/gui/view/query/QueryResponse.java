/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.query;

import java.util.Set;
import java.util.HashSet;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;

public class  QueryResponse {
    
    public static final int QR_FAILURE = 0;
    public static final int QR_SUCCESS = 1;
    public static final int QR_WARNING = 2;

    public static final String NORMAL_OP = "Normal Operations";

    protected Set forest;
    private String message;
    private int condition;

    public QueryResponse() {
	condition = QR_SUCCESS;
	message = NORMAL_OP;
	forest = new HashSet();
    }

    public void addTree(Tree tree) {
	  forest.add(tree);
    }
    public Set getTrees() {
	  return forest;
    }
  public void addForest (Set otherForest) {
	forest.addAll (otherForest);
  }
  
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message=message; }

    public int getCondition() { return condition; }
    public void setCondition(int condition) { this.condition=condition; }
    
}
