/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
/*
 * User: tom
 * Date: Aug 22, 2002
 * Time: 11:35:01 AM
 */
package test.org.cougaar.mlm.ui.grabber.config;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.config.CougaarRCParams;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;

public class CougaarRCParamsTest extends TestCase {

    public class MyCougaarRCParamsChild extends CougaarRCParams {

        // override this to do nothing
        // since we really just want to test the parsing
        // for now, that is
        public void findFile() {}

        public void parseParameterStream(String sname, InputStream in) throws IOException {
            super.parseParameterStream(sname, in);
        }

        public Map getParams() {
            return super.parameterMap;
        }

    }

    private static final String SEP = System.getProperty("line.separator");

    public CougaarRCParamsTest(String name) {
        super(name);
    }

    public void testPoundSignComment() throws Throwable {
        String TEST_POUND_COMMENT = "# howdy" + SEP;
        MyCougaarRCParamsChild p = new MyCougaarRCParamsChild();
        p.parseParameterStream("foo", new ByteArrayInputStream(TEST_POUND_COMMENT.getBytes()));
        assertTrue(p.getParams().isEmpty());
    }

    public void testSemicolonComment() throws Throwable {
        String TEST_SEMICOLON_COMMENT = "; howdy" + SEP;
        MyCougaarRCParamsChild p = new MyCougaarRCParamsChild();
        p.parseParameterStream("foo", new ByteArrayInputStream(TEST_SEMICOLON_COMMENT.getBytes()));
        assertTrue(p.getParams().isEmpty());
    }

    public void testOneParam() throws Throwable {
        String TEST_ONE_PARAM = "foo=bar" + SEP;
        MyCougaarRCParamsChild p = new MyCougaarRCParamsChild();
        p.parseParameterStream("foo", new ByteArrayInputStream(TEST_ONE_PARAM.getBytes()));
        assertEquals("bar", p.getParams().get("foo"));
    }

    public void testMultipleParams() throws Throwable {
        String TEST_MULTIPLE_PARAMS = "foo=bar" + SEP +
                                      "baz=bif" + SEP +
                                      "buz=biz" + SEP;
        MyCougaarRCParamsChild p = new MyCougaarRCParamsChild();
        p.parseParameterStream("foo", new ByteArrayInputStream(TEST_MULTIPLE_PARAMS.getBytes()));
        assertEquals("bar", p.getParams().get("foo"));
        assertEquals("bif", p.getParams().get("baz"));
        assertEquals("biz", p.getParams().get("buz"));
    }

    public void testRedundantKeys() throws Throwable {
        String TEST_REDUNDANT_KEYS = "foo=bar" + SEP +
                                       "foo=baz" + SEP;
        MyCougaarRCParamsChild p = new MyCougaarRCParamsChild();
        p.parseParameterStream("foo", new ByteArrayInputStream(TEST_REDUNDANT_KEYS.getBytes()));
        assertEquals("bar", p.getParams().get("foo"));
    }
}
