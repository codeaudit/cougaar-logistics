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
 *  A ValElement is an Element whose purpose is to encapsulate raw data values
 *  (i.e., Strings).
 */
public class ValElement extends Element {
  /**
   *  The symbol associated with the ValElement type, namely "V".
   */
  public static final String SYMBOL = "V";

  /**
   *  The value encapsulated by this ValElement
   */
  protected String value = null;

  /**
   *  Construct a new ValElement with no initial contents.
   */
  public ValElement () {
  }

  /**
   *  Construct a new ValElement with the provided value as its contents.
   *  @param v the data to be encapsulated
   */
  public ValElement (String v) {
    value = v;
  }

  /**
   *  Since this Element is a ValElement instance, return a reference to it as
   *  the more specific type.
   *  @return a reference to this as a ValElement
   */
  public ValElement getAsValue () {
    return this;
  }

  /**
   *  Install a new String value as the contents of this ValElement.
   *  @param v the new content String
   */
  public void setValue (String v) {
    value = v;
  }

  /**
   *  Retrieve the value contained within this ValElement
   *  @return the encapsulated value.
   */
  public String getValue () {
    return value;
  }

  /**
   *  Generate an XML representation of this ValElement.
   *  @param pp the PrettyPrinter (text formatter) to which output is directed
   */
  public void generateXml (PrettyPrinter pp) {
    pp.print("<val>" + xmlize(value) + "</val>");
  }

  /**
   *  Report the symbol associated with the ValElement type, namely "V".
   *  @return the symbol of a ValElement, "V"
   */
  public String getSymbol () {
    return SYMBOL;
  }
}