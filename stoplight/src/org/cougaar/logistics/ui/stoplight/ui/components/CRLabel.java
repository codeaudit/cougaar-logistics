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
import java.awt.geom.Rectangle2D;
import javax.swing.*;

/**
 * Label that can be rotated such that it is oriented
 * LEFT_RIGHT (normal), UP_DOWN, RIGHT_LEFT, or DOWN_UP
 */
public class CRLabel extends JComponent
{
    /** left to right orientation */
    public static final int LEFT_RIGHT = 0;

    /** up to down orientation */
    public static final int UP_DOWN = 1;

    /** right to left orientation (upside down) */
    public static final int RIGHT_LEFT = 2;

    /** down to up orientation */
    public static final int DOWN_UP = 3;

    private String text = null;
    private int orientation = 0;
    private int startx = 0;
    private int starty = 0;

    /**
     * Create a new rotatable label with no text
     */
    public CRLabel()
    {
        updateUI();
    }

    /**
     * Create a new rotatable label with the given text
     *
     * @param text new label text
     */
    public CRLabel(String text)
    {
        this.text = text;
        updateUI();
    }

    /**
     * Create a new rotatable label with the given text and orientation.
     *
     * @param text new label text
     * @param orientation label orientation
     *                   (LEFT_RIGHT (normal), UP_DOWN, RIGHT_LEFT, or DOWN_UP)
     */
    public CRLabel(String text, int orientation)
    {
        this.text = text;
        this.orientation = orientation;
        updateUI();
    }

    /**
     * Called when look and feel changes.
     */
    public void updateUI()
    {
        super.updateUI();

        setFont(UIManager.getFont("Label.font"));
        setForeground(UIManager.getColor("Label.foreground"));
        resetProperties();
    }

    /**
     * Set new label orientation
     *
     * @param orientation label orientation
     *                   (LEFT_RIGHT (normal), UP_DOWN, RIGHT_LEFT, or DOWN_UP)
     */
    public void setOrientation(int orientation)
    {
        this.orientation = orientation;
        resetProperties();
    }

    /**
     * Get current label orientation
     *
     * @return current label orientation
     *                 (LEFT_RIGHT (normal), UP_DOWN, RIGHT_LEFT, or DOWN_UP)
     */
    public int getOrientation()
    {
        return orientation;
    }

    /**
     * Set label text
     *
     * @param text new label text
     */
    public void setText(String text)
    {
        this.text = text;
        resetProperties();
    }

    /**
     * Get label text
     *
     * @return current label text
     */
    public String getText()
    {
        return text;
    }

    private void resetProperties()
    {
        int padding = 2;
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.stringWidth(text) + padding;
        int height = fm.getHeight() + padding;
        int pwidth = 0;
        int pheight = 0;

        switch (orientation)
        {
            case LEFT_RIGHT:
                startx = 0;
                starty = height - padding;
                pwidth = width;
                pheight = height;
                break;
            case UP_DOWN:
                startx = padding;
                starty = 0;
                pwidth = height;
                pheight = width;
                break;
            case RIGHT_LEFT:
                startx = width;
                starty = padding;
                pwidth = width;
                pheight = height;
                break;
            case DOWN_UP:
                // This might be needed to further differentiate java versions
                // for special case code below.
                // String vmVersion = System.getProperty("java.vm.version");
                float versionNumber =
                    Float.parseFloat(System.getProperty("java.class.version"));
                int diff = height - padding;
                startx = (versionNumber > 46) ? diff : diff/2;
                starty = width;
                pwidth = height;
                pheight = width;
                break;
        }

        setPreferredSize(new Dimension(pwidth, pheight));
        revalidate();
        repaint();
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.rotate(orientation * Math.PI / 2, startx, starty);
        g2d.drawString(text, startx, starty);
    }

    /**
     * main for unit test
     *
     * @param args ignored
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Test");
        Box c = new Box(BoxLayout.Y_AXIS);
        frame.getContentPane().add(c);
        for (int rot = 0; rot < 4; rot ++)
        {
            CRLabel crLabel = new CRLabel("########## Long Label ##########");
            crLabel.setOrientation(rot);
            c.add(crLabel);
        }
        frame.pack();
        frame.setVisible(true);
    }
}