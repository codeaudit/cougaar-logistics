/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/StatusLine.java,v 1.2 2003-02-03 22:27:59 mthome Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;

public class StatusLine extends Component
implements MessageListener
{
	private static final Dimension myPreferredSize = new Dimension(400,25);
	private boolean myAmInitialized = false;
	private String myMessage = null;

	StatusLine()
	{
		setBackground( Color.black );
		setForeground( Color.white );
		enableEvents( AWTEvent.COMPONENT_EVENT_MASK );
	}
	public Dimension getPreferredSize() { return myPreferredSize; }
	public void paint( Graphics g )
	{
		if( !myAmInitialized )
		{
			reconfigure();
			myAmInitialized = true;
		}

		g.setColor( getBackground() );
		g.fillRect( 0,0, getSize().width, getSize().height );

		g.setColor( getForeground() );
		g.drawRect( 0,0, getSize().width-1, getSize().height-1 );

		if( myMessage != null )
		{
			g.setColor( getForeground() );
			g.setFont( getFont() );
			g.drawString( myMessage, 4, getSize().height*3/4 );
		}
	}
	protected void processComponentEvent( ComponentEvent e )
	{
		super.processComponentEvent(e);

		switch( e.getID() )
		{
		case ComponentEvent.COMPONENT_RESIZED:
			reconfigure();
			break;
		}
	}
	private void reconfigure()
	{
			Font oldFont = getFont();
			Font newFont = new Font( oldFont.getName(),
									 Font.BOLD,
									 getSize().height*7/10 );
			setFont( newFont );
	}
	public void setMessage( String msg )
	{
		myMessage = msg;
		repaint();
	}
}
