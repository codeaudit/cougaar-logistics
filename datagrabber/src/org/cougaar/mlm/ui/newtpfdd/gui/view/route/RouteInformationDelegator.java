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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import java.awt.Dimension;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

import com.bbn.openmap.InformationDelegator;

/**
 * Displays Status information, with customizations for Route Display
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




