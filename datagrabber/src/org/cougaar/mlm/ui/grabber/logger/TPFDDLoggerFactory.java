/*
 * User: tom
 * Date: Aug 16, 2002
 * Time: 9:49:25 AM
 */
package org.cougaar.mlm.ui.grabber.logger;

public class TPFDDLoggerFactory {

    private static IDLogger singleton;

    public static IDLogger createLogger() {
        if (singleton == null) {
            singleton = new StdLogger();
        }
        return singleton;
    }
}
