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
 * Date: Aug 20, 2002
 * Time: 2:09:46 PM
 */
package test.org.cougaar.mlm.ui.grabber.validator;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.validator.HTMLizer;

import java.io.PrintStream;
import java.io.OutputStream;
import java.io.IOException;

public class HTMLizerTest extends TestCase {

    private static final String SEP = System.getProperty("line.separator");

    private static class MyOutputStream extends OutputStream {
        public void write(int b) throws IOException {
        }
    }

    private class MyPrintStream extends PrintStream {

        public MyPrintStream() {
            super(new MyOutputStream());
        }

        private StringBuffer sb = new StringBuffer();

        public void print(String s) {
            sb.append(s);
        }

        public void append(Object o) {
            sb.append(o.toString());
        }

        public String get() {
            return sb.toString();
        }

    }

    public HTMLizerTest(String name) {
        super(name);
    }

    public void testBR() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.br();
        assertEquals("<BR>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testHR() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.hr();
        assertEquals("<HR>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testH1() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.h1("foo");
        assertEquals("<H1>foo</H1>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testH2() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.h2("foo");
        assertEquals("<H2>foo</H2>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testH3() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.h3("foo");
        assertEquals("<H3>foo</H3>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testTD() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.tData("foo");
        assertEquals("<TD>foo</TD>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testTH() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.tHead("foo");
        assertEquals("<TH>foo</TH>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testLI() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.li("foo");
        assertEquals("<LI>foo", ((MyPrintStream)h.getStream()).get());
    }

    public void testA() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.a("http://google.com/", "Google");
        assertEquals("<A HREF=\"http://google.com/\">Google</A>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testP() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.p("foo");
        assertEquals("<p>foo</p>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testCenter() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.sCenter();
        h.eCenter();
        assertEquals("<CENTER>\n</CENTER>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testFont() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.sFont("blue");
        h.eFont();
        assertEquals("<FONT COLOR=blue>\n</FONT>\n", ((MyPrintStream)h.getStream()).get());
    }

    public void testRow() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.sRow();
        h.eRow();
        assertEquals("<TR>\n</TR>\n", ((MyPrintStream)h.getStream()).get());
    }
}


