/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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

import java.awt.Container;
import javax.swing.JMenu;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextChild;
import com.bbn.openmap.PropertyConsumer;

import org.cougaar.logistics.ui.stoplight.ui.components.CFrame;

// interaface imports
import java.util.Properties;

import com.bbn.openmap.gui.OpenMapFrame;

public class ScenarioFrame extends OpenMapCFrame
{

  private static ScenarioFrame me;

  public ScenarioFrame()
  {
    super();
    me = this;
  }

  public static Container getOpenMapContentPane()
  {
    if (me != null)
      return me.getContentPane();
    else
      return null;
  }

  public static Container getOpenMapJMenuBar()
  {
    if (me != null)
      return me.getJMenuBar();
    else
      return null;
  }

  public static void setVisibleFlag (boolean vis)
  {
     if (me != null)
       me.setVisible(vis);
  }

  public static JMenu getCFrameLookAndFeelPulldown ()
  {
    if (me != null)
      return me.getLookAndFeelPulldown();
    else
      return null;

  }

  public static JMenu getCFrameThemesPulldown ()
  {
    if (me != null)
       return me.getThemesPulldown();
    else
       return null;

  }

  //
  // pass through functions to satisfy the interfaces
  //

  // PropertyConsumer interface
/*
  public Properties getProperties( Properties getList)
  {
    return super.getProperties (getList);
  }

  public Properties getPropertyInfo ( Properties list)
  {
    return super.getPropertyInfo (list);
  }

  public String getPropertyPrefix ()
  {
    return super.getPropertyPrefix();
  }

  public void setPropertyPrefix (String prefix)
  {
    super.setPropertyPrefix(prefix);
  }

  public void setProperties( Properties setList)
  {
    super.setProperties (setList);
  }
  public void setProperties( String prefix, Properties setList)
  {
    super.setProperties (prefix, setList);
  }
*/

}
