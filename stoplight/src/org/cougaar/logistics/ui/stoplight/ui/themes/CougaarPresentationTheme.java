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
package org.cougaar.logistics.ui.stoplight.ui.themes;

import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * This class describes a theme using offical Cougaar colors.
 *
 * 1.3 12/21/00
 */
public class CougaarPresentationTheme extends CougaarTheme
{
    public String getName() { return "Cougaar Presentation"; }

    private final FontUIResource controlFont = new FontUIResource("Trebuchet MS", Font.PLAIN, 20);
    private final FontUIResource systemFont = new FontUIResource("Trebuchet MS", Font.PLAIN, 18); // JTree Font
    private final FontUIResource windowTitleFont = new FontUIResource("Trebuchet MS", Font.BOLD, 32); // nothing automatic
    private final FontUIResource userFont = new FontUIResource("Trebuchet MS", Font.PLAIN, 18); // table data, input data
    private final FontUIResource smallFont = new FontUIResource("Trebuchet MS", Font.PLAIN, 16); // 10nothing automatic

    public FontUIResource getControlTextFont() { return controlFont;}
    public FontUIResource getSystemTextFont() { return systemFont;}
    public FontUIResource getUserTextFont() { return userFont;}
    public FontUIResource getMenuTextFont() { return controlFont;}
    public FontUIResource getWindowTitleFont() { return windowTitleFont;}
    public FontUIResource getSubTextFont() { return smallFont;}
}
