/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;

/***********************************************************************************************************************
<b>Description</b>: An extension of the swing JSplitPane class that attempts to correct divider location problems
                    when switching look and feel.

***********************************************************************************************************************/
public class CSplitPane extends javax.swing.JSplitPane
{
	/*********************************************************************************************************************
  <b>Description</b>: Constructs a split pane with the specified orientation.  Possible orientation values are the
                      same as for the javax.swing.JSplitPane class.

  <br>
  @param orientation Split orientation
	*********************************************************************************************************************/
  public CSplitPane(int orientation)
  {
    super(orientation);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Overridden method to attempt to correct display problems when switching look and feel.
	*********************************************************************************************************************/
  public void updateUI()
  {
    int location = getDividerLocation();
    super.updateUI();
    setDividerLocation(location);
  }
}
