/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.util.Date;

import java.text.SimpleDateFormat;


// example: 
// OutputHandler h = new OutputHandler(new XYZProducer("name"), true);
//

public class OutputHandler 
{
    private static OutputHandler globalHandler = null;
    private Producer producer;
    private SimpleDateFormat formatter;

    public OutputHandler(Producer producer, boolean registerAsGlobal)
	throws NullPointerException
    {
	if ( producer == null )
	    throw new NullPointerException("OH:pM Error: null producer");
	this.producer = producer;

	if ( registerAsGlobal ) {
	    if ( globalHandler != null )
		System.err.println("Warning: replacing old globalHandler.");
	    globalHandler = this;
	}

	formatter = new SimpleDateFormat("HH:mm:ss.SS");
	addConsumer(new SimpleIOConsumer());
    }

    public void putMessage(String message, boolean preamble, boolean newLine)
    {
	if ( preamble )
	    message = "[" + formatter.format(new Date()) + "] [" + Thread.currentThread().getName() + "] " + message;
	if ( newLine )
	    message += "\n";
	if ( message.indexOf("Error") != -1 ) {
	    String origin = message.substring(0, message.indexOf(" "));
	    message += ExceptionTools.stackToString(origin);
	}
	producer.addNotify(message);
    }

    public static void out(String message, boolean preamble, boolean newLine)
    {
	if ( globalHandler != null ) {
	    globalHandler.putMessage(message, preamble, newLine);
	} else{
	    System.err.println("[OH:out: Warning: no global handler] " + message);
    }
    }

    public static void out(String message)
    {
	out(message, true, true);
    }

    public void addConsumer(Consumer consumer)
    {
	producer.addConsumer(consumer);
    }

    public void deleteConsumer(Consumer consumer)
    {
	producer.deleteConsumer(consumer);
    }
}
