/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.grabber.workqueue;

/**
 * Defines a timed piece of work to do
 *
 * @since 01/11/02
 **/
public interface TimedWork extends Work {

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  //Members:
  //////////

  public long getStart ();

  public void setDuration (long dur);
  public long getDuration ();
  
  /** how long has the work taken, in wall-clock time */
  public long timeSpent();

  /** returns true if spent more than the allotted time */
  public boolean isOverdue();
  
  /** 
   * return result when the work times out.
   *
   * may also want to attempt to clean up here, e.g. 
   * attempt to close open connections, etc.
   *
   * This is the work's opportunity to communicate to whoever 
   * is waiting for it and tell them information, like that
   * the work timed out.
   */
  public Result getTimedOutResult ();
  //InnerClasses:
  ///////////////
}
