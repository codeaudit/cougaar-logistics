/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/view/ScrollInfoDialog.java,v 1.2 2003-02-03 22:28:00 mthome Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;


public class ScrollInfoDialog extends JDialog implements ActionListener
{
    private JScrollPane messageScroller;
    private JPanel containerPanel;
    private JButton OKButton;
    private String info;

    private JPanel getContainerPanel(String info)
    {
        if ( containerPanel == null )
            try {
                containerPanel = new JPanel();
                containerPanel.setName("containerPanel");
                containerPanel.setLayout(new BorderLayout());
                containerPanel.add(getMessageScroller(info), BorderLayout.CENTER);
		containerPanel.add(getOKButton(), BorderLayout.SOUTH);
            }
            catch(Exception e) {
                handleException(e);
            }
        return containerPanel;
    }

    private JScrollPane getMessageScroller(String info)
    {
        if ( messageScroller == null )
            try {
                MessageArea messages = new MessageArea("Textual Representation:\n" + info, 20, 60);
                messages.setEditable(false);
                messageScroller = new JScrollPane(messages);
                messages.setScrollBar(messageScroller.getVerticalScrollBar());
                messageScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            }
            catch ( Exception e ) {
                handleException(e);
            }
        return messageScroller;
    }

    public JButton getOKButton()
    {
	if ( OKButton == null) {
	    try {
		OKButton = new JButton();
		OKButton.setName("OKButton");
		OKButton.setText("OK");
		OKButton.addActionListener(this);
	    } catch (Exception ivjExc) {
		// user code begin {2}
		// user code end
		handleException(ivjExc);
	    }
	}
	return OKButton;
    }

    private void handleException(Exception e)
    {
        OutputHandler.out(ExceptionTools.toString("Server:hE", e));
    }

    public void actionPerformed(ActionEvent event)
    {
	dispose();
    }

    public ScrollInfoDialog(String info)
    {
	super();
	setName("ScrollInfoDialog");
	setTitle("Detailed Information");
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	setSize(350, 450);
	setContentPane(getContainerPanel(info));
	setVisible(true);
    }
}
