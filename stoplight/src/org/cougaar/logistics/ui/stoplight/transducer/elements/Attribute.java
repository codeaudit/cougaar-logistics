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

import java.util.*;

/**
 *  An Attribute is a named property associated with a ListElement.  Attributes
 *  are not ordered within their parent, but must have unique names so that
 *  one may be distinguished from another.
 */
public class Attribute extends ListElement {
  /**
   *  The "symbol" associated with the Attribute type, which happens to be "A".
   */
  public static final String SYMBOL = "A";

  /**
   *  The name of this attribute
   */
  protected String name = null;

  /**
   *  Construct a new Attribute with no name or contents, initially.
   */
  public Attribute () {
  }

  /**
   *  Construct a new Attribute with the specified name but no initial contents.
   *  @param n the name of the new Attribute
   */
  public Attribute (String n) {
    name = n;
  }

  /**
   *  Construct what is likely to be the most common form of Attribute, namely
   *  one whose sole child Element is a ValElement (q.v.) containing a String.
   *  @param n the name of the Attribute being created
   *  @param v the String value to be contained in the ValElement child
   */
  public Attribute (String n, String v) {
    name = n;
    addChild(new ValElement(v));
  }

  /**
   *  Construct an Attribute with a single child.
   *  @param n the name of the Attribute being created
   *  @param elt the Element being inserted as a child
   */
  public Attribute (String n, Element elt) {
    name = n;
    addChild(elt);
  }

  /**
   *  Since this Element is an instance of Attribute, return a reference as the
   *  more specific type.
   *  @return the reference to this as an Attribute
   */
  public Attribute getAsAttribute () {
    return this;
  }

  /**
   *  Set the name of this Attribute.  Warning:  this method should not be
   *  used while a ListElement has this Attribute tabulated under a previous
   *  name.
   *  @param n the new name of this Attribute
   */
  public void setName (String n) {
    name = n;
  }

  /**
   *  Report the name by which this Attribute is known.
   *  @return the Attribute name
   */
  public String getName () {
    return name;
  }

  /**
   *  Produce the XML representation of this Attribute and its substructures.
   *  @param pp the PrettyPrinter (text formatter) to which output is directed
   */
  public void generateXml (PrettyPrinter pp) {
    pp.print("<a name=\"" + name + "\">");
    generateSubTags(pp);
    pp.print("</a>");
  }

  /**
   *  Report the symbol associated with this type of Element.  In this case,
   *  that's "A".
   */
  public String getSymbol () {
    return SYMBOL;
  }
}