/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import java.awt.Dimension;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

import com.bbn.openmap.InformationDelegator;

/**
 * Displays Status information, with customizations for Route Display
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 4/24/01
 **/
public class RouteInformationDelegator extends InformationDelegator
  implements ComponentListener{

  //Constants:
  ////////////

  //Variables:
  ////////////

  protected int maxHeight=0;

  //Constructors:
  ///////////////
  public RouteInformationDelegator(){
    super();
  }

  //Members:
  //////////

  public Dimension getPreferredSize(){
    Dimension d=super.getPreferredSize();
    int newHeight=(int)d.getHeight();
    if(newHeight>maxHeight)
      maxHeight=newHeight;
    Dimension newD=new Dimension((int)d.getWidth(),maxHeight);
    return newD;
  }

  //From ComponentListener;
  public void componentResized(ComponentEvent e){
    maxHeight=0;
  }
  public void componentMoved(ComponentEvent e){}
  public void componentShown(ComponentEvent e){}
  public void componentHidden(ComponentEvent e){}

  //InnerClasses:
  ///////////////
}




