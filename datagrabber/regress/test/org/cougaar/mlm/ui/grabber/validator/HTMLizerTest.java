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

    public void test1() {
        HTMLizer h = new HTMLizer(new MyPrintStream());
        h.br();
        MyPrintStream ps = (MyPrintStream)h.getStream();
        assertTrue(ps.get().indexOf("<BR>")!= -1);
    }
}
