/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
/*
 * User: tom
 * Date: Aug 15, 2002
 * Time: 5:13:52 PM
 */
package test.org.cougaar.mlm.ui.grabber.webserver;

import java.net.Socket;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

public class MockSocket extends Socket {
    private InputStream stream;

    public MockSocket(InputStream stream) {
        this.stream = stream;
    }

    public InputStream getInputStream() {
        return stream;
    }

    public static MockSocket createSocketWith(String data) {
        return new MockSocket(new ByteArrayInputStream((data + getCRLF()+ getCRLF()).getBytes()));
    }


    public static String getCRLF() {
        return System.getProperty("line.separator");
    }
}
