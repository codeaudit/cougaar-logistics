/*
 * User: tom
 * Date: Aug 22, 2002
 * Time: 2:18:34 PM
 */
package org.cougaar.mlm.ui.grabber.logger;

public class AbstractLogger {
    protected int verbosityLevel=Logger.NORMAL;

    public boolean isWarningEnabled   () { return verbosityLevel <= Logger.WARNING; }

    public boolean isImportantEnabled () { return verbosityLevel <= Logger.IMPORTANT; }

    public boolean isNormalEnabled    () { return verbosityLevel <= Logger.NORMAL; }

    public boolean isMinorEnabled     () { return verbosityLevel <= Logger.MINOR; }

    public boolean isTrivialEnabled   () { return verbosityLevel <= Logger.TRIVIAL; }
}
