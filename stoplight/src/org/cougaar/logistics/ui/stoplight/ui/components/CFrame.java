package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.cougaar.logistics.ui.stoplight.ui.themes.*;

/**
 * This class is a highly modified version of the frame used in Sun's SwingSet
 * example.  It provides print support and the option to dynamically switch
 * between different look and feels and/or themes.
 *
 * The parts of this component that are used for dynamically switching between
 * Look and Feels and Metal Themes are based on Sun's SwingSet example. So...
 *
 *  Copyright (c) 1997-1999 by Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */
public class CFrame extends JFrame implements Printable
{
    /**
     * Default constructor.  Create new CFrame without any contents
     * or title.
     */
    public CFrame()
    {
        super();

        init();
    }

    /**
     * Create a new JFrame with given title and pluggable look and feel support
     *
     * @param title frame title
     * @param plaf true if all components of frame will support pluggable look
     *             and feel
     */
    public CFrame(String title, boolean plaf)
    {
        super(title);

        init();
        setPlaf(plaf);
    }

    private void init()
    {
        setJMenuBar(createMenus());
        setSize(1000, 600);
    }

    // Possible Look & Feels
    private String mac      = "com.sun.java.swing.plaf.mac.MacLookAndFeel";
    private String metal    = "javax.swing.plaf.metal.MetalLookAndFeel";
    private String motif    = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
    private String windows  = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

    // The current Look & Feel
    private String currentLookAndFeel = metal;

    // Menus
    protected JMenuBar menuBar = null;
    protected JMenu fileMenu = null;
    protected JMenu lafMenu = null;
    protected JMenuItem metalMenuItem = null;
    private   JMenu themesMenu = null;
    private   ButtonGroup lafMenuGroup = new ButtonGroup();
    private   ButtonGroup themesMenuGroup = new ButtonGroup();
    private   Component printTarget;

   // Graphically print the component to the printer
    protected synchronized void printComponent(Component c)
    {
        printTarget = c;
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(this);
        pj.printDialog();
        try {
            pj.print();
        }catch (Exception printException) {
            printException.printStackTrace();
        }
    }

    /**
     * Gets the Look and Feel pulldown menu
     *
     * @return the Look and Feel pulldown menu
     */
    public JMenu getLookAndFeelPulldown()
    {
        return lafMenu;
    }

    /**
     * Gets the Themes pulldown menu
     *
     * @return the Themes pulldown menu
     */
    public JMenu getThemesPulldown()
    {
        return themesMenu;
    }

    /**
     * This method is required to implement the Printable interface
     */
    public int print(Graphics g, PageFormat pageFormat, int pageIndex)
        throws PrinterException
    {
        if (pageIndex>=1) {
            return NO_SUCH_PAGE;
        }
        Graphics2D  g2 = (Graphics2D) g;
        Dimension cs = printTarget.getSize();
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        double imageableWidth = pageFormat.getImageableWidth();
        double imageableHeight = pageFormat.getImageableHeight();
        double scale = 1;
        if (cs.width >= imageableWidth) {
            scale =  imageableWidth / cs.width;
        }
        g2.scale(scale, scale);
        //g2.translate((imageableWidth - cs.width)*scale/2,
        //             (imageableHeight - cs.height)*scale/2);
        printTarget.paintAll(g2);
        return Printable.PAGE_EXISTS;
    }

    /**
     * Set pluggable look and feel capable
     *
     * @param plaf true if all components of frame will support pluggable look
     *             and feel
     */
    public void setPlaf(boolean plaf)
    {
        lafMenu.setEnabled(plaf);
    }

    /**
     * Create menus
     */
    private JMenuBar createMenus()
    {
        JMenuItem mi;
        // ***** create the menubar ****
        menuBar = new JMenuBar();

        // ***** create File menu
        fileMenu = (JMenu) menuBar.add(new JMenu("File"));
        fileMenu.setMnemonic('F');
        createMenuItem(fileMenu, "Print", 'P', "", new PrintAction(this));
        createMenuItem(fileMenu, "Close", 'C', "", new CloseAction(this));
        fileMenu.add(new JSeparator());
        createMenuItem(fileMenu, "Exit", 'E', "", new ExitAction());

        // ***** create laf switcher menu
        lafMenu = (JMenu) menuBar.add(new JMenu("Look & Feel"));
        lafMenu.setMnemonic('L');

        mi = createLafMenuItem(lafMenu, "Java Look & Feel", 'J', "", metal);
        mi.setSelected(true); // this is the default l&f
        metalMenuItem = mi;

        createLafMenuItem(lafMenu, "Macintosh Look & Feel", 'M', "", mac);

        createLafMenuItem(lafMenu, "Motif Look & Feel", 'F', "", motif);

        createLafMenuItem(lafMenu, "Windows Look & Feel", 'W', "", windows);

        // ***** create themes menu
        themesMenu = (JMenu) menuBar.add(new JMenu("Themes"));
        themesMenu.setMnemonic('T');

        mi = createThemesMenuItem(themesMenu, "Default", 'D', "",
                                  new DefaultMetalTheme());
        mi.setSelected(true); // This is the default theme

        createThemesMenuItem(themesMenu, "Aqua", 'A', "", new AquaTheme());

        createThemesMenuItem(themesMenu, "Charcoal", 'C', "",
                             new CharcoalTheme());

        createThemesMenuItem(themesMenu, "High Contrast", 'H', "",
                             new ContrastTheme());

        createThemesMenuItem(themesMenu, "Emerald", 'E', "",
                             new EmeraldTheme());

        createThemesMenuItem(themesMenu, "Ruby", 'R', "", new RubyTheme());

        createThemesMenuItem(themesMenu, "Presentation", 'P', "",
                             new DemoMetalTheme());

        createThemesMenuItem(themesMenu, "Sandstone", 'S', "",
                             new KhakiMetalTheme());

        createThemesMenuItem(themesMenu, "Big High Contrast", 'I', "",
                             new BigContrastMetalTheme());

        createThemesMenuItem(themesMenu, "Blue", 'B', "", new BlueTheme());

        createThemesMenuItem(themesMenu, "Cougaar", 'O', "",
                             new CougaarTheme());

        createThemesMenuItem(themesMenu, "Cougaar Presentation", 'O', "",
                             new CougaarPresentationTheme());

        return menuBar;
    }

    /**
     * Creates a generic menu item
     */
    protected JMenuItem createMenuItem(JMenu menu, String label, char mnemonic,
             String accessibleDescription, Action action) {
        JMenuItem mi = (JMenuItem) menu.add(new JMenuItem(label));
        mi.setMnemonic(mnemonic);
        mi.getAccessibleContext().
            setAccessibleDescription(accessibleDescription);
        mi.addActionListener(action);
        if(action == null) {
            mi.setEnabled(false);
        }
        return mi;
    }

    /**
     * Creates a JRadioButtonMenuItem for the Themes menu
     */
    private JMenuItem createThemesMenuItem(JMenu menu, String label,
        char mnemonic, String accessibleDescription, DefaultMetalTheme theme) {
        JRadioButtonMenuItem mi =
            (JRadioButtonMenuItem) menu.add(new JRadioButtonMenuItem(label));
        themesMenuGroup.add(mi);
        mi.setMnemonic(mnemonic);
        mi.getAccessibleContext().
            setAccessibleDescription(accessibleDescription);
        mi.addActionListener(new ChangeThemeAction(this, theme));

        return mi;
    }

    /**
     * Creates a JRadioButtonMenuItem for the Look and Feel menu
     */
    private JMenuItem createLafMenuItem(JMenu menu, String label,
            char mnemonic, String accessibleDescription, String laf) {
        JMenuItem mi =
            (JRadioButtonMenuItem) menu.add(new JRadioButtonMenuItem(label));
        lafMenuGroup.add(mi);
        mi.setMnemonic(mnemonic);
        mi.getAccessibleContext().setAccessibleDescription(accessibleDescription);
        mi.addActionListener(new ChangeLookAndFeelAction(this, laf));
        mi.setEnabled(isAvailableLookAndFeel(laf));

        return mi;
    }

    /**
     * A utility function that layers on top of the LookAndFeel's
     * isSupportedLookAndFeel() method. Returns true if the LookAndFeel
     * is supported. Returns false if the LookAndFeel is not supported
     * and/or if there is any kind of error checking if the LookAndFeel
     * is supported.
     *
     * The L&F menu will use this method to detemine whether the various
     * L&F options should be active or inactive.
     *
     */
     protected boolean isAvailableLookAndFeel(String laf) {
         try {
             Class lnfClass = Class.forName(laf);
             LookAndFeel newLAF = (LookAndFeel)(lnfClass.newInstance());
             return newLAF.isSupportedLookAndFeel();
         } catch(Exception e) { // If ANYTHING weird happens, return false
             return false;
         }
     }

    /**
     * Stores the current L&F, and calls updateLookAndFeel, below
     */
    public void setLookAndFeel(String laf) {
        if(currentLookAndFeel != laf) {
            currentLookAndFeel = laf;
            themesMenu.setEnabled(laf == metal);
            updateLookAndFeel();
        }
    }

    /**
     * Sets the current L&F on each demo module
     */
    public void updateLookAndFeel() {
        try {
            UIManager.setLookAndFeel(currentLookAndFeel);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            System.out.println("Failed loading L&F: " + currentLookAndFeel);
            System.out.println(ex);
        }
    }

    private class ChangeLookAndFeelAction extends AbstractAction {
        CFrame swingset;
        String laf;
        protected ChangeLookAndFeelAction(CFrame swingset, String laf) {
            super("ChangeTheme");
            this.swingset = swingset;
            this.laf = laf;
        }

        public void actionPerformed(ActionEvent e) {
            swingset.setLookAndFeel(laf);
        }
    }

    protected static DefaultMetalTheme currentTheme = new DefaultMetalTheme();
    private class ChangeThemeAction extends AbstractAction {
        CFrame swingset;
        DefaultMetalTheme theme;
        protected ChangeThemeAction(CFrame swingset, DefaultMetalTheme theme) {
            super("ChangeTheme");
            this.swingset = swingset;
            this.theme = theme;
        }

        public void actionPerformed(ActionEvent e) {
            MetalLookAndFeel.setCurrentTheme(theme);
            currentTheme = theme;
            swingset.updateLookAndFeel();
        }
    }

    private class PrintAction extends AbstractAction
    {
        private Component comp;
        protected PrintAction(Component comp)
        {
            super("PrintAction");
            this.comp = comp;
        }

        public void actionPerformed(ActionEvent e)
        {
            (new Thread() {
                    public void run()
                    {
                        printComponent(comp);
                    }
                }).start();
        }
    }

    private class CloseAction extends AbstractAction {
        CFrame cframe;
        protected CloseAction(CFrame cframe) {
            super("CloseAction");
            this.cframe = cframe;
        }

        public void actionPerformed(ActionEvent e) {
            cframe.dispose();
        }
    }

    private class ExitAction extends AbstractAction {
        protected ExitAction() {
            super("ExitAction");
        }

        public void actionPerformed(ActionEvent e) {
            int selection = JOptionPane.showConfirmDialog(CFrame.this,
                        "Would you like to exit this application?");
            if (selection == JOptionPane.OK_OPTION)
            {
                System.exit(0);
            }
        }
    }
}