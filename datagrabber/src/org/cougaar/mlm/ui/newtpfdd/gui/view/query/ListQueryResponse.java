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

import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;

public class ListQueryResponse extends QueryResponse {
  Tree ctype, cinst, atype, ainst;
  
  public void setCarrierTypeTree (Tree tree) 
  {
	ctype = tree;
  }
  public Tree getCarrierTypeTree () 
  {
	return ctype;
  }
  
  public void setCarrierInstanceTree (Tree tree) 
  {
	cinst = tree;
  }
  public Tree getCarrierInstanceTree () 
  {
	return cinst;
  }

  public void setCargoTypeTree (Tree tree) 
  {
	atype = tree;
  }
  public Tree getCargoTypeTree () 
  {
	return atype;
  }
  public void setCargoInstanceTree (Tree tree) 
  {
	ainst = tree;
  }
  public Tree getCargoInstanceTree () 
  {
	return ainst;
  }
}
