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
package org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider;

import javax.swing.*;
import javax.swing.plaf.*;

public class AssistantUIManager {
  
  /*
  ** changed so that CMThumbSlider can be subclassed from
  ** a different package. If the subclass overrides getUIClassID
  ** it must also provide the UI classes in its very package.
  */
  private static String guessComponentName(JComponent c) {
    if (c.getUIClassID().equals(CMThumbSlider.uiClassID))
      return CMThumbSlider.class.getName();
    return c.getClass().getName();
  }
  
  public static ComponentUI createUI(JComponent c)  {
    String componentName = guessComponentName(c);

    int index = componentName.lastIndexOf(".") +1;
    StringBuffer sb = new StringBuffer();
    sb.append( componentName.substring(0, index) );

    String lookAndFeelName = UIManager.getLookAndFeel().getName();
    if (lookAndFeelName.startsWith("CDE/")) {
      lookAndFeelName = lookAndFeelName.substring(4,lookAndFeelName.length());
    }
    sb.append( lookAndFeelName );
    sb.append( componentName.substring(index) );
    sb.append( "UI" );

    ComponentUI componentUI = getInstance(sb.toString());

    if (componentUI == null) {
      sb.setLength(0);
      sb.append( componentName.substring(0, index) );
      sb.append( "Basic");
      sb.append( componentName.substring(index) );
      sb.append( "UI" );
      componentUI = getInstance(sb.toString());
    }

    return componentUI;
  }

  private static ComponentUI getInstance(String name) {
    try {
      return (ComponentUI)Class.forName(name).newInstance();
    }
    catch (ClassNotFoundException ex) {
    }
    catch (IllegalAccessException ex) {
      ex.printStackTrace();
    }
    catch (InstantiationException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public static void setUIName(JComponent c) {
    String key = c.getUIClassID();
    String uiClassName = (String)UIManager.get(key);

    if (uiClassName == null) {
      String componentName = guessComponentName(c);
      int index = componentName.lastIndexOf(".") +1;
      StringBuffer sb = new StringBuffer();
      sb.append( componentName.substring(0, index) );
      String lookAndFeelName = UIManager.getLookAndFeel().getName();
      if (lookAndFeelName.startsWith("CDE/")) {
        lookAndFeelName = lookAndFeelName.substring(4,lookAndFeelName.length());
      }
      sb.append( lookAndFeelName );
      sb.append( key );
      UIManager.put(key, sb.toString());
    }
  }

  public AssistantUIManager() {}
}
