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

package org.cougaar.logistics.ui.stoplight.transducer.elements;

/**
 *  A NullElement is an Element used as a placeholder for content that isn't
 *  there.
 */
public class NullElement extends Element {
  /**
   *  The symbol associated with the NullElement type.  That is, "N".
   */
  public static final String SYMBOL = "N";

  /**
   *  Since this Element is a NullElement instance, supply a reference to it
   *  as the more specific type.
   */
  public NullElement getAsNull () {
    return this;
  }

  /**
   *  Generate the XML representation of this Element.
   *  @param pp the PrettyPrinter (text formatter) to which output is directed
   */
  public void generateXml (PrettyPrinter pp) {
    pp.print("<null/>");
  }

  /**
   *  Report the symbol of this type of Element, which happens to be "N".
   *  @return the symbol of a NullElement, "N"
   */
  public String getSymbol () {
    return SYMBOL;
  }
}