/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */


package org.cougaar.logistics.ui.servicediscovery;


import java.util.*;


import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.*;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.logistics.ui.inventory.ConnectionHelper;


/**
 * <pre>
 *
 * The RelationshipUILauncherFrame is the root object that maintains the primary
 * window where the inventory charts are displayed.
 *
 * @see RelationshipScheduleData
 *
 **/


public class RelationshipUILauncherFrame extends JFrame
        implements ActionListener {


    static final Cursor waitCursor =
            Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    static final Cursor defaultCursor =
            Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    protected static final String CONNECT_BUTTON_LABEL = "Connect";
    protected static final String CONNECTION_BORDER_TITLE = "Connection";
    protected static final String LAUNCH_BUTTON_LABEL = "Launch";
    protected static final String LAUNCH_BORDER_TITLE = "Agent Selection";
    protected static final String AGENT_COMBO_LABEL = "Agent:";

    protected static final String MAIN_BORDER_TITLE = "Relationship Viewer Launcher";

    protected static final String SERV_ID = "relationship_schedule";

    final protected static String RELATIONSHIP_NO_OVERLAP_QUERY = "RELATIONSHIP_SCHEDULE_NO_OVERLAP";

    final protected static String RELATIONSHIP_W_OVERLAP_QUERY = "RELATIONSHIP_SCHEDULE_W_OVERLAP";

    JButton connectButton;
    JButton launchButton;
    JComboBox agentsBox;

    private Container contentPane;

    String servProt;
    String servHost;
    String servPort;

    Hashtable agentURLs;

    protected Logger logger;

    protected String currAgent;


    public RelationshipUILauncherFrame() {
        super("Relationship Launcher GUI");
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        logger = Logging.getLogger(this);
        contentPane = getRootPane().getContentPane();

        servHost = "localhost";
        servProt = "http";
        servPort = "8800";

        // fills frame
        doMyLayout();
        pack();
        setSize(300, 220);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // set location to middle of the screen
        setLocation(screenSize.width / 2 - (int) this.getSize().getWidth() / 2,
                    screenSize.height / 2 - (int) this.getSize().getHeight() / 2);
        setVisible(true);
    }

    protected JPanel createConnectPanel() {
        JPanel connectPanel = new JPanel();
        connectButton = new JButton(CONNECT_BUTTON_LABEL);
        connectButton.addActionListener(this);
        connectButton.setFocusPainted(true);
        connectPanel.setLayout(new GridBagLayout());
        connectPanel.add(connectButton,
                         new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER,
                                                GridBagConstraints.NONE,
                                                new Insets(5, 5, 5, 5),
                                                0, 0));
        connectPanel.setBorder(BorderFactory.createTitledBorder(CONNECTION_BORDER_TITLE));
        return connectPanel;
    }


    protected JPanel createAgentLaunchPanel() {
        int x = 1;
        int y = 1;
        JPanel launchPanel = new JPanel();
        JPanel agentPanel = new JPanel();
        agentPanel.setLayout(new FlowLayout());
        agentsBox = new JComboBox();
        agentsBox.setPreferredSize(new Dimension(200, 25));
        JLabel agentBoxLabel = new JLabel(AGENT_COMBO_LABEL);
        agentPanel.add(agentBoxLabel);
        agentPanel.add(agentsBox);
        launchButton = new JButton(LAUNCH_BUTTON_LABEL);
        launchButton.addActionListener(this);
        launchButton.setFocusPainted(false);
        launchPanel.setLayout(new GridBagLayout());
        launchPanel.add(agentPanel,
                        new GridBagConstraints(x, y++, 1, 1, 0.0, 0.0,
                                               GridBagConstraints.CENTER,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(5, 5, 5, 5),
                                               0, 0));
        launchPanel.add(launchButton,
                        new GridBagConstraints(x, y++, 1, 1, 0.0, 0.0,
                                               GridBagConstraints.CENTER,
                                               GridBagConstraints.NONE,
                                               new Insets(5, 5, 5, 5),
                                               0, 0));
        launchPanel.setBorder(BorderFactory.createTitledBorder(LAUNCH_BORDER_TITLE));
        return launchPanel;
    }


    /** creates an y_axis oriented panel containing two components */
    protected JPanel createYPanel(JComponent comp1, JComponent comp2) {
        JPanel panel = new JPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(comp1);
        panel.add(comp2);
        return panel;
    }

    protected void doMyLayout() {
        int x = 0;
        int y = 0;
        //getRootPane().setJMenuBar(makeMenus());
        JPanel mainPanel = new JPanel();
        JPanel connectPanel = createConnectPanel();
        JPanel launchPanel = createAgentLaunchPanel();


        mainPanel.setLayout(new GridBagLayout());
        mainPanel.add(connectPanel,
                      new GridBagConstraints(x, y++, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.CENTER,
                                             GridBagConstraints.BOTH,
                                             new Insets(5, 5, 5, 5),
                                             0, 0));
        mainPanel.add(launchPanel,
                      new GridBagConstraints(x, y++, 1, 2, 0.0, 0.0,
                                             GridBagConstraints.CENTER,
                                             GridBagConstraints.BOTH,
                                             new Insets(5, 5, 5, 5),
                                             0, 0));

        //mainPanel.setPreferredSize(new Dimension(300,300));
        /**
         mainPanel.setBorder(
         BorderFactory.createCompoundBorder(
         BorderFactory.createCompoundBorder(
         BorderFactory.createTitledBorder(MAIN_BORDER_TITLE),
         BorderFactory.createEmptyBorder(5, 5, 5, 5)),
         mainPanel.getBorder()));
         ***/

        contentPane.add(mainPanel);
    }

    protected JMenuBar makeMenus() {
        JMenuBar retval = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem quit = new JMenuItem("Exit");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem open = new JMenuItem("Open");
        quit.addActionListener(this);
        save.addActionListener(this);
        open.addActionListener(this);
        file.add(quit);
        file.add(open);
        file.add(save);
        retval.add(file);

        JMenu connection = new JMenu("Connection");
        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(this);
        connection.add(connect);
        retval.add(connection);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(this);
        helpMenu.add(helpItem);
        //Help popup is not done yet.
        //retval.add(helpMenu);

        return retval;
    }


    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(CONNECT_BUTTON_LABEL)) {
            connectToServlet();
        } else if (e.getActionCommand().equals(LAUNCH_BUTTON_LABEL)) {
            launchRelationshipViewer();
        } else {
            System.out.println("RelationshipUILauncherFrame: Unknown Action");
        }
    }


    public static void timeConsumingTaskStart(Component c) {
        JFrame frame = null;
        if (c != null)
            frame = (JFrame) SwingUtilities.getRoot(c);
        if (frame == null)
            return;
        timeConsumingTaskStart(frame);
    }

    public static void timeConsumingTaskStart(JFrame frame) {
        Component glass = frame.getGlassPane();
        glass.setCursor(waitCursor);
        glass.addMouseListener(new DoNothingMouseListener());
        glass.setVisible(true);
    }

    public static void timeConsumingTaskEnd(Component c) {
        JFrame frame = null;
        if (c != null)
            frame = (JFrame) SwingUtilities.getRoot(c);
        if (frame == null)
            return;
        timeConsumingTaskEnd(frame);
    }

    public static void timeConsumingTaskEnd(JFrame frame) {
        Component glass = frame.getGlassPane();
        MouseListener[] mls =
                (MouseListener[]) glass.getListeners(MouseListener.class);
        for (int i = 0; i < mls.length; i++)
            if (mls[i] instanceof DoNothingMouseListener)
                glass.removeMouseListener(mls[i]);
        glass.setCursor(defaultCursor);
        glass.setVisible(false);
    }

    protected void connectToServlet() {
        if (getAgentHostAndPort()) {
            Vector agents = getAgentNames();
            if (agents == null) {
                displayErrorString("Error. Was unable to retrieve agents.");
            }
            initializeComboBoxes(agents);
        }
    }

    private static void displayErrorString(String reply) {
        JOptionPane.showMessageDialog(null, reply, reply,
                                      JOptionPane.ERROR_MESSAGE);
    }

    public boolean getAgentHostAndPort() {

        String msg = "Enter cluster Log Plan Server location as host:port";
        String s = ConnectionHelper.getClusterHostPort(null, msg, servHost, servPort);
        if ((s == null) || (s.trim().equals(""))) {
            displayErrorString("Entered Nothing. Cannot get connection.");
            return false;
        }
        s = s.trim();
        if (s.length() != 0) {
            String[] substrings = s.split("://");
            if (substrings.length >= 2) {
                servProt = substrings[0];
                String hostAndPort = substrings[1];
                int i = hostAndPort.indexOf(":");
                if (i != -1) {
                    servHost = hostAndPort.substring(0, i);
                    servPort = hostAndPort.substring(i + 1);
                    System.out.println("getAgentHostAndPort url = " + getURLString());
                    return true;
                }
            }
        }
        displayErrorString("Improper Format for url.  Cannot get connection.");
        return false;
    }


    public String getDefaultAgentName() {
        Vector agentNames = getSortedAgentNames();
        if (agentNames == null) return null;
        return (String) agentNames.elementAt(1);
    }

    public Vector getAgentNames() {
        logger.debug("Getting Agent List");
        ConnectionHelper connection = null;
        try {
            connection = new ConnectionHelper(getURLString());
            agentURLs = connection.getClusterIdsAndURLs();
            connection.closeConnection();
            connection = null;
            if (agentURLs == null) {
                logger.warn("No AGENT/Agents");
                return null;
            }
        } catch (Exception e) {
            logger.error(e.toString());
            return null;
        }
        return getSortedAgentNames();
    }

    private Vector getSortedAgentNames() {
        if (agentURLs != null) {
            Enumeration names = agentURLs.keys();
            Vector vNames = new Vector();
            while (names.hasMoreElements())
                vNames.addElement(names.nextElement());
            Collections.sort(vNames);
            return vNames;
        }
        return null;
    }

    public String getURLString() {
        return servProt + "://" + servHost + ":" + servPort + "/";
    }

    public RelationshipScheduleData getRelationshipData(String myAgentName) {
        String agentURL = (String) agentURLs.get(myAgentName);
        //When querying for all just ASSET
        String queryStr = RELATIONSHIP_W_OVERLAP_QUERY;


        //logger.debug("Submitting: " + queryStr + " to: " + agentURL +
        //                   " for: " + SERV_ID);
        ConnectionHelper connection = null;
        InputStream is = null;
        try {
            connection =
                    new ConnectionHelper(agentURL, SERV_ID);

            connection.sendData(queryStr);

            is = connection.getInputStream();
        } catch (Exception e) {
            displayErrorString(e.toString());
            return null;
        }
        RelationshipScheduleData rsData = null;
        try {
            ObjectInputStream p = new ObjectInputStream(is);
            rsData = (RelationshipScheduleData) p.readObject();
            p.close();
            connection.closeConnection();
        } catch (Exception e) {
            displayErrorString("Object read exception: " + "_2_" + e.toString());
            return null;
        }

        return rsData;
    }

    protected void launchRelationshipViewer() {
        currAgent = (String) agentsBox.getSelectedItem();
        RelationshipScheduleData rsData = getRelationshipData(currAgent);
        if (rsData != null) {
            printRelationshipData(rsData);
            new RelationshipDataGanttChartView(rsData).launch();
        }
    }

    protected void printRelationshipData(RelationshipScheduleData data) {
        System.out.println(data);
    }

    protected void initializeComboBoxes(Vector agents) {
        setAgents(agents);
    }

    protected void setAgents(Vector agents) {
        agentsBox.removeAllItems();
        currAgent = null;
        if (agents != null) {
            for (int i = 0; i < agents.size(); i++) {
                String agentName = (String) agents.elementAt(i);
                agentsBox.addItem(agentName);
            }
            if (agents.size() > 1) {
                currAgent = (String) agentsBox.getItemAt(0);
            }
        }
    }

    public static String formatTimeStamp(Date dateToFormat,
                                         boolean includeSeconds) {

        String datestamp;
        TimeZone gmt = TimeZone.getDefault();
        GregorianCalendar now = new GregorianCalendar(gmt);
        now.setTime(dateToFormat);
        datestamp = "" + now.get(Calendar.YEAR);
        datestamp += prependSingle(now.get(Calendar.MONTH) + 1);
        datestamp += prependSingle(now.get(Calendar.DAY_OF_MONTH));
        datestamp += prependSingle(now.get(Calendar.HOUR_OF_DAY));
        datestamp += prependSingle(now.get(Calendar.MINUTE));
        if (includeSeconds) {
            datestamp += prependSingle(now.get(Calendar.SECOND));
        }
        return datestamp;
    }

    private static String prependSingle(int digit) {
        if (digit < 10) {
            return "0" + digit;
        } else {
            return "" + digit;
        }
    }

    protected class HelpDialog extends JDialog {

        protected String helpFileURLStr;

        public HelpDialog(JFrame owner,
                          String aHelpFileURLStr) {
            super(owner, "Inventory GUI Help", false);
            helpFileURLStr = aHelpFileURLStr;
            setupAndLayout();
        }

        protected void setupAndLayout() {

        }

    }


    protected class TaskStartRunnable implements Runnable {

        RelationshipUILauncherFrame frame;

        public TaskStartRunnable(RelationshipUILauncherFrame aFrame) {
            frame = aFrame;
        }

        public void run() {
            timeConsumingTaskStart(frame);
        }
    }


    static class DoNothingMouseListener extends MouseInputAdapter {
        public void mouseClicked(MouseEvent e) {
            e.consume();
        }

        public void mouseDragged(MouseEvent e) {
            e.consume();
        }

        public void mouseEntered(MouseEvent e) {
            e.consume();
        }

        public void mouseExited(MouseEvent e) {
            e.consume();
        }

        public void mouseMoved(MouseEvent e) {
            e.consume();
        }

        public void mousePressed(MouseEvent e) {
            e.consume();
        }

        public void mouseReleased(MouseEvent e) {
            e.consume();
        }
    }

    public static void main(String[] args) {
        new RelationshipUILauncherFrame();
    }
}


