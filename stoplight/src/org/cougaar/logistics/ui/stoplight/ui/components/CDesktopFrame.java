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
package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

/**
 * This is a top level application frame used to launch stoplight, line plot,
 * and other Cougaar tools in inner frames.
 */
public class CDesktopFrame extends CFrame
{
    private JDesktopPane desktopPane;

    /**
     * Default constructor.  Create new CFrameContainer without any tools
     * (i.e. views) or title.
     */
    public CDesktopFrame()
    {
        super();
        init();
    }

    /**
     * Create new CFrameContainer without any tools (i.e. views).
     *
     * @param title of frame.
     */
    public CDesktopFrame(String title)
    {
        super(title, true);
        init();
    }

    private void init()
    {
        desktopPane = new CDesktopPane();
        getContentPane().add(desktopPane, BorderLayout.CENTER);
        addMenus();
    }

    // Menus
    private JMenu viewMenu = null;
    private JMenu windowMenu = null;
    private JMenuItem closeMenuItem = null;
    private JMenuItem printSelectedMenuItem = null;

    /**
     * Add a new tool to the view pulldown menu
     *
     * @param name              name to use for pulldown option
     * @param mnemonic          character to use for pulldown option mnemonic
     * @param cougaarUIClass    class that implements the CougaarUI interface.
     *                          Used to create new instances of UI in reaction
     *                          to user selections.
     * @param constParamClasses array of classes that describe constructor
     *                          parameters that will be used for creating new
     *                          instances of the CougaarUI.
     * @param config            creates array of objects that will be passed
     *                          into constructor when creating new instances
     *                          of the CougaarUI.
     */
     public void addTool(String name, char mnemonic, Class cougaarUIClass,
                         Class[] constParamClasses,
                         Configurator config)
     {
        createMenuItem(viewMenu, name, mnemonic, "",
                       new CreateViewAction(name, cougaarUIClass,
                                            constParamClasses, config));
     }

    /**
     * Creates a new inner frame to parent the given JPanel and adds it to the
     * desktop.
     *
     * @param title     title for new inner frame
     * @param cougaarUI cougaarUI to use in new inner frame
     */
    public void createInnerFrame(String title, CougaarUI cougaarUI)
    {
        final JInternalFrame f =
            new JInternalFrame(title, true, true, true, true);
        cougaarUI.install(f);
        f.putClientProperty("CougaarUI", cougaarUI);
        f.setSize(850, 500);
        desktopPane.add(f, JLayeredPane.PALETTE_LAYER);
        f.setVisible(true);
        try {f.setSelected(true);} catch(Exception e) {}

        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    // workaround for apparent Swing bug
                    f.setSize(801, 500);
                }
            });

        // add new window to window menu
        final JMenuItem mi = (JMenuItem)windowMenu.add(new JMenuItem(title));
        mi.addActionListener(new WindowAction(f));

        f.addInternalFrameListener(new InternalFrameAdapter() {
                public void internalFrameClosed(InternalFrameEvent e)
                {
                    // try to make another frame active
                    JInternalFrame[] ifs = desktopPane.getAllFrames();
                    int ai = usingJdk13orGreater() ? 0 : 1;
                    try {ifs[ai].setSelected(true);} catch(Exception ex){}

                    f.removeInternalFrameListener(this);
                    f.dispose();

                    // remove frame's menu item from window menu
                    windowMenu.remove(mi);
                }

                public void internalFrameDeiconified(InternalFrameEvent e)
                {
                    updateEnabledMenuOptions();
                }

                public void internalFrameIconified(InternalFrameEvent e)
                {
                    updateEnabledMenuOptions();
                }
            });
    }

    private class CDesktopPane extends JDesktopPane
    {
        /**
        * When look and feel or theme is changed, this method is called.  It
        * overrides a Cougaar theme color for the desktop.
        * (otherwise resulting color combo is extra harsh).
        */
        public void updateUI()
        {
            if ((UIManager.getLookAndFeel() instanceof MetalLookAndFeel) &&
                (currentTheme.getName().startsWith("Cougaar")))
            {
                CDesktopPane.this.setBackground(Color.gray);
            }
            else
            {
                CDesktopPane.this.setBackground(null);
            }
            super.updateUI();
        }
    }

     /**
     * Create new menus and menu items
     */
    private void addMenus() {
        //
        // ***** modify File Menu
        //
        Component[] comps = fileMenu.getMenuComponents();
        fileMenu.removeAll();

        JMenu printMenu = new JMenu("Print");
        printMenu.setMnemonic('P');
        fileMenu.add(printMenu);
        createMenuItem(printMenu, "Entire Desktop", 'D', "",
                       new PrintAction(this, true));
        printSelectedMenuItem =
            createMenuItem(printMenu, "Selected View", 'S', "",
                           new PrintAction(this, false));

        // close active inner frame with close (not whole window)
        closeMenuItem =
            createMenuItem(fileMenu, "Close", 'C', "", new CloseAction());

        // put the exit option back at the end of menu
        fileMenu.add(new JSeparator());
        fileMenu.add(comps[comps.length - 1]);

        // ***** create View Menu
        viewMenu = (JMenu)menuBar.add(new JMenu("View"), 1);
        viewMenu.setMnemonic('V');

        // ***** create Window Menu
        windowMenu = (JMenu)menuBar.add(new JMenu("Window"));
        windowMenu.setMnemonic('W');

        // disable window menu, close menu item, and print selected view menu
        // item when window menu is empty.
        updateEnabledMenuOptions();
        windowMenu.getPopupMenu().addContainerListener(new ContainerListener(){
                public void componentAdded(ContainerEvent e)
                {
                    updateEnabledMenuOptions();
                }
                public void componentRemoved(ContainerEvent e)
                {
                    updateEnabledMenuOptions();
                }
            });
    }

    /**
     * Set all menu items to the correct enabled state.
     */
    private void updateEnabledMenuOptions()
    {
        boolean plafSupported = true;
        JInternalFrame[] ifs = desktopPane.getAllFrames();
        for (int i = 0; i < ifs.length; i++)
        {
            CougaarUI cui = (CougaarUI)ifs[i].getClientProperty("CougaarUI");
            if (!cui.supportsPlaf())
            {
                plafSupported = false;
                break;
            }
        }
        boolean windowsOpen = (ifs.length > 0);
        boolean windowSelected =
            windowsOpen ? (getSelectedFrame() != null) : false;

        windowMenu.setEnabled(windowsOpen);
        closeMenuItem.setEnabled(windowSelected);
        printSelectedMenuItem.setEnabled(windowSelected);
        lafMenu.setEnabled(plafSupported);
    }

    private class PrintAction extends AbstractAction
    {
        // if false, just the active window
        private CDesktopFrame swingset;
        private boolean wholeDesktop;
        protected PrintAction(CDesktopFrame swingset, boolean wholeDesktop) {
            super("PrintAction");
            this.swingset = swingset;
            this.wholeDesktop = wholeDesktop;
        }

        public void actionPerformed(ActionEvent e)
        {
            (new Thread() {
                    public void run()
                    {
                        if (wholeDesktop)
                        {
                            printComponent(swingset);
                        }
                        else
                        {
                            JInternalFrame selectedFrame = getSelectedFrame();
                            //    desktopPane.getSelectedFrame() (jdk1.3)

                            if (selectedFrame != null)
                            {
                                printComponent(selectedFrame);
                            }
                        }
                    }
                }).start();
        }
    }

    /**
     * Needed for compatibility with jdk1.2.2
     */
    private boolean usingJdk13orGreater()
    {
        float versionNumber =
            Float.parseFloat(System.getProperty("java.class.version"));
        return (versionNumber >= 47.0);
    }

    private class CreateViewAction extends AbstractAction {
        private String title;
        private Class viewClass;
        private Class[] constParamClasses;
        private Configurator config;
        protected CreateViewAction(String title, Class viewClass,
                                   Class[] constParamClasses,
                                   Configurator config)
        {
            super("CreateViewAction");
            this.viewClass = viewClass;
            this.constParamClasses = constParamClasses;
            this.config = config;
            this.title = title;
        }

        public void actionPerformed(ActionEvent e)
        {
            desktopPane.
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            (new Thread() {
                    public void run()
                    {
                        try
                        {
                            Constructor c =
                                viewClass.getConstructor(constParamClasses);
                            CougaarUI cougaarUI = (CougaarUI)
                                c.newInstance((config !=  null) ?
                                              config.createConstParameters() :
                                              null);
                            createInnerFrame(title, cougaarUI);
                            if (config != null)
                            {
                                config.configure(cougaarUI);
                            }
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                        finally
                        {
                            desktopPane.setCursor(Cursor.getDefaultCursor());
                        }
                    }
                }).start();
        }
    }

    private class CloseAction extends AbstractAction {
        protected CloseAction() {
            super("CloseAction");
        }

        public void actionPerformed(ActionEvent e) {
            // jdk1.3: use desktopPane.getSelectedFrame()
            JInternalFrame frame = getSelectedFrame();

            if (frame != null)
            {
                try{frame.setClosed(true);}catch(Exception ex){}
            }
        }
    }

    private class WindowAction extends AbstractAction {
        private JInternalFrame frame;
        protected WindowAction(JInternalFrame frame) {
            super("WindowAction");
            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            // try to make frame active
            try
            {
                frame.setIcon(false);
                frame.setSelected(true);
            }
            catch(Exception ex){}
        }
    }

    /**
     * This method is only required when compiling/running under jdk1.2
     * In jdk1.3 you can use: desktopPane.getSelectedFrame()
     */
    private JInternalFrame getSelectedFrame()
    {
        JInternalFrame selectedFrame = null;
        JInternalFrame[] ifs = desktopPane.getAllFrames();
        for (int i=0; i < ifs.length; i++)
        {
            if (ifs[i].isSelected())
            {
                selectedFrame = ifs[i];
                break;
            }
        }
        return selectedFrame;
    }

    protected interface Configurator
    {
        public Object[] createConstParameters();
        public void configure(CougaarUI ui);
    }
}