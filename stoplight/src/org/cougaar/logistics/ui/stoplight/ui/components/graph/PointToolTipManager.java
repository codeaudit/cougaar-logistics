package org.cougaar.logistics.ui.stoplight.ui.components.graph;

/**
 * This class is a modified version of the javax.swing.ToolTipManager class
 * to get the tool tip text from the registered component only when the
 * tool tip is to be drawn instead of ever time a mouse action occurs.
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

import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;

public class PointToolTipManager extends MouseAdapter
    implements MouseMotionListener
{
    protected class insideTimerAction
        implements ActionListener
    {

        public void actionPerformed(ActionEvent actionevent)
        {
            if(insideComponent != null && insideComponent.isShowing())
            {
                showImmediately = true;
                showTipWindow();
            }
        }

        protected insideTimerAction()
        {
        }
    }

    protected class outsideTimerAction
        implements ActionListener
    {

        public void actionPerformed(ActionEvent actionevent)
        {
            showImmediately = false;
        }

        protected outsideTimerAction()
        {
        }
    }

    protected class stillInsideTimerAction
        implements ActionListener
    {

        public void actionPerformed(ActionEvent actionevent)
        {
            hideTipWindow();
            enterTimer.stop();
            showImmediately = false;
        }

        protected stillInsideTimerAction()
        {
        }
    }

    private static interface ToolTipPopup
    {

        public abstract void addMouseListener(PointToolTipManager tooltipmanager);

        public abstract Rectangle getBounds();

        public abstract void hide();

        public abstract void removeMouseListener(PointToolTipManager tooltipmanager);

        public abstract void show(JComponent jcomponent, int i, int j);
    }

    class JPanelPopup extends JPanel
        implements ToolTipPopup
    {

        public void addMouseListener(PointToolTipManager tooltipmanager)
        {
            super.addMouseListener(tooltipmanager);
        }

        public Rectangle getBounds()
        {
            return super.getBounds();
        }

        public void hide()
        {
            Container container = getParent();
            Rectangle rectangle = getBounds();
            if(container != null)
            {
                container.remove(this);
                container.repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            }
        }

        public void removeMouseListener(PointToolTipManager tooltipmanager)
        {
            super.removeMouseListener(tooltipmanager);
        }

        public void show(JComponent jcomponent, int i, int j)
        {
            Point point = new Point(i, j);
            SwingUtilities.convertPointFromScreen(point, jcomponent.getRootPane().getLayeredPane());
            setBounds(point.x, point.y, getSize().width, getSize().height);
            jcomponent.getRootPane().getLayeredPane().add(this, JLayeredPane.POPUP_LAYER, 0);
        }

        public void update(Graphics g)
        {
            paint(g);
        }

        public JPanelPopup(JComponent jcomponent, Dimension dimension)
        {
            setLayout(new BorderLayout());
            setDoubleBuffered(true);
            setOpaque(true);
            add(jcomponent, "Center");
            setSize(dimension);
        }
    }

    class PanelPopup extends Panel
        implements ToolTipPopup
    {

        public void addMouseListener(PointToolTipManager tooltipmanager)
        {
            super.addMouseListener(tooltipmanager);
        }

        public Rectangle getBounds()
        {
            return super.getBounds();
        }

        public void hide()
        {
            Container container = getParent();
            Rectangle rectangle = getBounds();
            if(container != null)
            {
                container.remove(this);
                container.repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            }
        }

        public void removeMouseListener(PointToolTipManager tooltipmanager)
        {
            super.removeMouseListener(tooltipmanager);
        }

        public void show(JComponent jcomponent, int i, int j)
        {
            Point point = new Point(i, j);
            SwingUtilities.convertPointFromScreen(point, jcomponent.getRootPane().getLayeredPane());
            jcomponent.getRootPane().getLayeredPane().add(this, JLayeredPane.POPUP_LAYER, 0);
            setBounds(point.x, point.y, getSize().width, getSize().height);
        }

        public PanelPopup(JComponent jcomponent, Dimension dimension)
        {
            setLayout(new BorderLayout());
            add(jcomponent, "Center");
            setSize(dimension);
        }
    }

    class WindowPopup extends Window
        implements ToolTipPopup
    {

        boolean firstShow;
        JComponent tip;
        Frame frame;

        public void addMouseListener(PointToolTipManager tooltipmanager)
        {
            super.addMouseListener(tooltipmanager);
        }

        public Rectangle getBounds()
        {
            return super.getBounds();
        }

        public void hide()
        {
            super.hide();
            removeNotify();
        }

        public void removeMouseListener(PointToolTipManager tooltipmanager)
        {
            super.removeMouseListener(tooltipmanager);
        }

        public void show(JComponent jcomponent, int i, int j)
        {
            setLocation(i, j);
            setVisible(true);
            if(firstShow)
            {
                hide();
                setVisible(true);
                firstShow = false;
            }
        }

        public WindowPopup(Frame frame1, JComponent jcomponent, Dimension dimension)
        {
            super(frame1);
            firstShow = true;
            tip = jcomponent;
            frame = frame1;
            add(jcomponent, "Center");
            pack();
        }
    }


    Timer enterTimer;
    Timer exitTimer;
    Timer insideTimer;
    String toolTipText;
    Point preferredLocation;
    JComponent insideComponent;
    MouseEvent mouseEvent;
    boolean showImmediately;
    static final PointToolTipManager sharedInstance = new PointToolTipManager();
    ToolTipPopup tipWindow;
    JToolTip tip;
    private Rectangle popupRect;
    private Rectangle popupFrameRect;
    boolean enabled;
    boolean mouseAboveToolTip;
    private boolean tipShowing;
    private long timerEnter;
//    private KeyStroke postTip;
//    private KeyStroke hideTip;
//    private ActionListener postTipAction;
//    private ActionListener hideTipAction;
    private FocusListener focusChangeListener;
    protected boolean lightWeightPopupEnabled;
    protected boolean heavyWeightPopupEnabled;

    PointToolTipManager()
    {
        popupRect = null;
        popupFrameRect = null;
        enabled = true;
        mouseAboveToolTip = false;
        tipShowing = false;
        timerEnter = 0L;
        focusChangeListener = null;
        lightWeightPopupEnabled = true;
        heavyWeightPopupEnabled = false;
        enterTimer = new Timer(750, new insideTimerAction());
        enterTimer.setRepeats(false);
        exitTimer = new Timer(500, new outsideTimerAction());
        exitTimer.setRepeats(false);
        insideTimer = new Timer(4000, new stillInsideTimerAction());
        insideTimer.setRepeats(false);
/*        postTip = KeyStroke.getKeyStroke(112, 2);
        postTipAction = new ActionListener() {

            public void actionPerformed(ActionEvent actionevent)
            {
                if(tipWindow != null)
                {
                    hideTipWindow();
                }
                else
                {
                    hideTipWindow();
                    enterTimer.stop();
                    exitTimer.stop();
                    insideTimer.stop();
                    insideComponent = (JComponent)actionevent.getSource();
                    if(insideComponent != null)
                    {
                        toolTipText = insideComponent.getToolTipText();
                        preferredLocation = new Point(10, insideComponent.getHeight() + 10);
                        showTipWindow();
                        if(focusChangeListener == null)
                            focusChangeListener = createFocusChangeListener();
                        insideComponent.addFocusListener(focusChangeListener);
                    }
                }
            }

        };
        hideTip = KeyStroke.getKeyStroke(27, 0);
        hideTipAction = new ActionListener() {

            public void actionPerformed(ActionEvent actionevent)
            {
                hideTipWindow();
                JComponent jcomponent = (JComponent)actionevent.getSource();
                jcomponent.removeFocusListener(focusChangeListener);
                preferredLocation = null;
            }

        };*/
    }

    private FocusListener createFocusChangeListener()
    {
        return new FocusAdapter() {

            public void focusLost(FocusEvent focusevent)
            {
                hideTipWindow();
                JComponent jcomponent = (JComponent)focusevent.getSource();
                jcomponent.removeFocusListener(focusChangeListener);
            }

        };
    }

    static Frame frameForComponent(Component component)
    {
        for(; !(component instanceof Frame); component = component.getParent());
        return (Frame)component;
    }

  private JLabel tipLabel = null;

  public void setDataTipLabel(JLabel label)
  {
    tipLabel = label;
  }

    public int getDismissDelay()
    {
        return insideTimer.getInitialDelay();
    }

    private int getHeightAdjust(Rectangle rectangle, Rectangle rectangle1)
    {
        if(rectangle1.y >= rectangle.y && rectangle1.y + rectangle1.height <= rectangle.y + rectangle.height)
            return 0;
        else
            return ((rectangle1.y + rectangle1.height) - (rectangle.y + rectangle.height)) + 5;
    }

    public int getInitialDelay()
    {
        return enterTimer.getInitialDelay();
    }

    private int getPopupFitHeight(Rectangle rectangle, Component component)
    {
        if(component != null)
        {
            for(Container container = component.getParent(); container != null; container = container.getParent())
            {
                if((container instanceof JFrame) || (container instanceof JDialog) || (container instanceof JWindow))
                    return getHeightAdjust(container.getBounds(), rectangle);
                if((container instanceof JApplet) || (container instanceof JInternalFrame))
                {
                    if(popupFrameRect == null)
                        popupFrameRect = new Rectangle();
                    Point point = container.getLocationOnScreen();
                    popupFrameRect.setBounds(point.x, point.y, container.getBounds().width, container.getBounds().height);
                    return getHeightAdjust(popupFrameRect, rectangle);
                }
            }

        }
        return 0;
    }

    private int getPopupFitWidth(Rectangle rectangle, Component component)
    {
        if(component != null)
        {
            for(Container container = component.getParent(); container != null; container = container.getParent())
            {
                if((container instanceof JFrame) || (container instanceof JDialog) || (container instanceof JWindow))
                    return getWidthAdjust(container.getBounds(), rectangle);
                if((container instanceof JApplet) || (container instanceof JInternalFrame))
                {
                    if(popupFrameRect == null)
                        popupFrameRect = new Rectangle();
                    Point point = container.getLocationOnScreen();
                    popupFrameRect.setBounds(point.x, point.y, container.getBounds().width, container.getBounds().height);
                    return getWidthAdjust(popupFrameRect, rectangle);
                }
            }

        }
        return 0;
    }

    public int getReshowDelay()
    {
        return exitTimer.getInitialDelay();
    }

    private int getWidthAdjust(Rectangle rectangle, Rectangle rectangle1)
    {
        if(rectangle1.x >= rectangle.x && rectangle1.x + rectangle1.width <= rectangle.x + rectangle.width)
            return 0;
        else
            return ((rectangle1.x + rectangle1.width) - (rectangle.x + rectangle.width)) + 5;
    }

    void hideTipWindow()
    {
      if (tipLabel != null)
      {
        tipLabel.setText(" ");
        return;
      }

        if(tipWindow != null)
        {
            tipWindow.removeMouseListener(this);
            tipWindow.hide();
            tipWindow = null;
            tipShowing = false;
            timerEnter = 0L;
            tip.getUI().uninstallUI(tip);
            tip = null;
            insideTimer.stop();
        }

        if (insideComponent != null)
        {
          insideComponent.repaint();
        }
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean isLightWeightPopupEnabled()
    {
        return lightWeightPopupEnabled;
    }

    public void mouseDragged(MouseEvent mouseevent)
    {
    }

    public void mouseEntered(MouseEvent mouseevent)
    {
        if(tipShowing && !lightWeightPopupEnabled && System.currentTimeMillis() - timerEnter < 200L)
            return;
        if(mouseevent.getSource() == tipWindow)
            return;
        JComponent jcomponent = (JComponent)mouseevent.getSource();
//        toolTipText = jcomponent.getToolTipText(mouseevent);
        toolTipText = null;
        preferredLocation = jcomponent.getToolTipLocation(mouseevent);
        exitTimer.stop();
        Point point = mouseevent.getPoint();
        if(point.x < 0 || point.x >= jcomponent.getWidth() || point.y < 0 || point.y >= jcomponent.getHeight())
            return;
        if(insideComponent != null)
        {
            enterTimer.stop();
            insideComponent = null;
        }
        jcomponent.addMouseMotionListener(this);
        insideComponent = jcomponent;
        if(tipWindow != null)
        {
            if(heavyWeightPopupEnabled)
                return;
            mouseEvent = mouseevent;
            if(showImmediately)
                showTipWindow();
            else
                enterTimer.start();
        }
    }

    public void mouseExited(MouseEvent mouseevent)
    {
        if(tipShowing && !lightWeightPopupEnabled && System.currentTimeMillis() - timerEnter < 200L)
            return;
        boolean flag = true;
        if(mouseevent.getSource() == tipWindow)
        {
            Container container = insideComponent.getTopLevelAncestor();
            Rectangle rectangle = tipWindow.getBounds();
            Point point1 = mouseevent.getPoint();
            point1.x += rectangle.x;
            point1.y += rectangle.y;
            rectangle = container.getBounds();
            point1.x -= rectangle.x;
            point1.y -= rectangle.y;
            point1 = SwingUtilities.convertPoint(null, point1, insideComponent);
            if(point1.x >= 0 && point1.x < insideComponent.getWidth() && point1.y >= 0 && point1.y < insideComponent.getHeight())
                flag = false;
            else
                flag = true;
        }
        else
        if(mouseevent.getSource() == insideComponent && tipWindow != null)
        {
            Point point = SwingUtilities.convertPoint(insideComponent, mouseevent.getPoint(), null);
            Rectangle rectangle1 = insideComponent.getTopLevelAncestor().getBounds();
            point.x += rectangle1.x;
            point.y += rectangle1.y;
            rectangle1 = tipWindow.getBounds();
            if(point.x >= rectangle1.x && point.x < rectangle1.x + rectangle1.width && point.y >= rectangle1.y && point.y < rectangle1.y + rectangle1.height)
                flag = false;
            else
                flag = true;
        }
        if(flag)
        {
            enterTimer.stop();
            if(insideComponent != null)
                insideComponent.removeMouseMotionListener(this);
            insideComponent = null;
            toolTipText = null;
            mouseEvent = null;
            hideTipWindow();
            exitTimer.start();
        }
    }

    public void mouseMoved(MouseEvent mouseevent)
    {
        JComponent jcomponent = (JComponent)mouseevent.getSource();
//        String s = jcomponent.getToolTipText(mouseevent);
        String s = "";
        Point point = jcomponent.getToolTipLocation(mouseevent);
        if(s != null || point != null)
        {
            mouseEvent = mouseevent;
            if((s != null && s.equals(toolTipText) || s == null) && (point != null && point.equals(preferredLocation) || point == null))
            {
                if(tipWindow != null)
                    insideTimer.restart();
                else
                    enterTimer.restart();
            }
            else
            {
                toolTipText = s;
                preferredLocation = point;
                if(showImmediately)
                {
                    hideTipWindow();
                    showTipWindow();
                }
                else
                {
                    enterTimer.restart();
                }
            }
        }
        else
        {
            toolTipText = null;
            preferredLocation = null;
            mouseEvent = null;
            hideTipWindow();
            enterTimer.stop();
            exitTimer.start();
        }

      toolTipText = null;
    }

    public void mousePressed(MouseEvent mouseevent)
    {
        hideTipWindow();
        enterTimer.stop();
        showImmediately = false;
    }

    public void registerComponent(JComponent jcomponent)
    {
        jcomponent.removeMouseListener(this);
        jcomponent.addMouseListener(this);
//        jcomponent.registerKeyboardAction(postTipAction, postTip, 0);
//        jcomponent.registerKeyboardAction(hideTipAction, hideTip, 0);
    }

    public void setDismissDelay(int i)
    {
        insideTimer.setInitialDelay(i);
    }

    public void setEnabled(boolean flag)
    {
        enabled = flag;
        if(!flag)
            hideTipWindow();
    }

    public void setInitialDelay(int i)
    {
        enterTimer.setInitialDelay(i);
    }

    public void setLightWeightPopupEnabled(boolean flag)
    {
        lightWeightPopupEnabled = flag;
    }

    public void setReshowDelay(int i)
    {
        exitTimer.setInitialDelay(i);
    }

    public static PointToolTipManager sharedInstance()
    {
        return sharedInstance;
    }

    void showTipWindow()
    {
        if(insideComponent == null || !insideComponent.isShowing())
            return;
        if(enabled)
        {
            Point point = insideComponent.getLocationOnScreen();
            Dimension dimension1 = Toolkit.getDefaultToolkit().getScreenSize();
            Point point1 = new Point();
            hideTipWindow();
            tip = insideComponent.createToolTip();
//            tip.setTipText(toolTipText);
            toolTipText = insideComponent.getToolTipText(mouseEvent);
            if (toolTipText == null)
            {
              return;
            }

      if (tipLabel != null)
      {
        tipLabel.setText(toolTipText);
        return;
      }

            tip.setTipText(toolTipText);
            toolTipText = null;

            Dimension dimension = tip.getPreferredSize();
            if(insideComponent.getRootPane() == null)
            {
                tipWindow = new WindowPopup(frameForComponent(insideComponent), tip, dimension);
                heavyWeightPopupEnabled = true;
            }
            else
            if(lightWeightPopupEnabled)
            {
                heavyWeightPopupEnabled = false;
                tipWindow = new JPanelPopup(tip, dimension);
            }
            else
            {
                heavyWeightPopupEnabled = false;
                tipWindow = new PanelPopup(tip, dimension);
            }
            tipWindow.addMouseListener(this);
            if(preferredLocation != null)
            {
                point1.x = point.x + preferredLocation.x;
                point1.y = point.y + preferredLocation.y;
            }
            else
            {
                point1.x = point.x + mouseEvent.getX();
                point1.y = point.y + mouseEvent.getY() + 20;
                if(point1.x + dimension.width > dimension1.width)
                    point1.x -= dimension.width;
                if(point1.y + dimension.height > dimension1.height)
                    point1.y -= dimension.height + 20;
            }
            if(!heavyWeightPopupEnabled)
            {
                if(popupRect == null)
                    popupRect = new Rectangle();
                popupRect.setBounds(point1.x, point1.y, tipWindow.getBounds().width, tipWindow.getBounds().height);
                int i = getPopupFitHeight(popupRect, insideComponent);
                int j = getPopupFitWidth(popupRect, insideComponent);
                if(i > 0)
                    point1.y -= i;
                if(j > 0)
                    point1.x -= j;
            }
            tipWindow.show(insideComponent, point1.x, point1.y);
            insideTimer.start();
            timerEnter = System.currentTimeMillis();
            tipShowing = true;
        }
    }

    public void unregisterComponent(JComponent jcomponent)
    {
        jcomponent.removeMouseListener(this);
//        jcomponent.unregisterKeyboardAction(postTip);
//        jcomponent.unregisterKeyboardAction(hideTip);
    }




}
