/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.util;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;

/**
 * This interface must be implemented by all User Interface tools that are
 * included in the Cougaar Desktop Frame (CDesktopFrame).
 *
 * @see org.cougaar.logistics.ui.stoplight.ui.components.CDesktopFrame
 */
public interface CougaarUI
{
    /**
     * Install this user interface in the passed in JInternalFrame.
     *
     * @param f internal frame to which this user interface should be added
     */
    public void install(JFrame f);

    /**
     * Install this user interface in the passed in JFrame.
     *
     * @param f frame to which this user interface should be added
     */
    public void install(JInternalFrame f);

    /**
     * Returns true if this UI supports pluggable look and feel.  Otherwise,
     * only Metal look and feel support is assumed.
     *
     * @return true if UI supports pluggable look and feel.
     */
    public boolean supportsPlaf();
}