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
 * This class describes a theme using blue colors.
 */
public class BlueTheme extends DefaultMetalTheme {

    public String getName() { return "Blue"; }

    // Delta colors and fonts obtained from FgiPanel.java
    public static final ColorUIResource black = new ColorUIResource(0,0,0);
    public static final ColorUIResource white = new ColorUIResource(255,255,255);
    public static final ColorUIResource red = new ColorUIResource(255,0,0);
    public static final ColorUIResource green = new ColorUIResource(0, 255, 0);
    public static final ColorUIResource blue = new ColorUIResource(0,0,255);
    public static final ColorUIResource isiLightBlue = new ColorUIResource(202, 218, 220);
    public static final ColorUIResource isiDarkBlue = new ColorUIResource(150, 180, 185);
    public static final ColorUIResource isiBrown = new ColorUIResource(200,200,165);
    public static final FontUIResource titleFont = new FontUIResource("Arial", Font.BOLD, 16);
    public static final FontUIResource attributeNameFont = new FontUIResource("Arial", Font.BOLD, 14);
    public static final FontUIResource largeTitleFont = new FontUIResource("Arial", Font.BOLD, 20);
    public static final FontUIResource standardFont = new FontUIResource("Arial", Font.PLAIN, 13);
    public static final FontUIResource largeFont = new FontUIResource("Arial", Font.PLAIN, 14);
    public static final FontUIResource largeBoldFont = new FontUIResource("Arial", Font.BOLD, 14);
    public static final FontUIResource smallFont = new FontUIResource("Arial", Font.BOLD, 12);

    // color I made up
    public static final ColorUIResource foregroundBlue = new ColorUIResource(111, 149, 156);

    protected ColorUIResource getPrimary1() { return foregroundBlue; } // labels
    protected ColorUIResource getPrimary2() { return foregroundBlue; } // labels, handles highlight, shadows
    protected ColorUIResource getPrimary3() { return isiBrown; } // text selection

    protected ColorUIResource getSecondary1() { return isiBrown; } // Inactive window borders (control outlines)
    protected ColorUIResource getSecondary2() { return isiBrown; } // Secondary shadows
    protected ColorUIResource getSecondary3() { return isiLightBlue; } // background

    protected ColorUIResource getBlack() { return black; }
    protected ColorUIResource getWhite() { return white; }

    public FontUIResource getControlTextFont() { return attributeNameFont;}
    public FontUIResource getSystemTextFont() { return standardFont;}
    public FontUIResource getUserTextFont() { return standardFont;}
    public FontUIResource getMenuTextFont() { return standardFont;}
    public FontUIResource getWindowTitleFont() { return largeTitleFont;}
    public FontUIResource getSubTextFont() { return smallFont;}
    public void addCustomEntriesToTable(UIDefaults table)

    {
        table.put("Button.background", isiDarkBlue);
        table.put("Button.foreground", white);
        table.put("ComboBox.background", isiDarkBlue);
        table.put("ComboBox.foreground", white);
    }
}
