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
 *  Element is the abstract base class of all Java classes which take part in
 *  the formation of such Structures as may be manipulated by the classes in
 *  the transducer package.
 */
public abstract class Element {
  /**
   *  The list of characters which should automatically be encoded when being
   *  written as part of an XML file.  The encoding ensures that an Object
   *  restored from the file has the same contents as the one that was written
   *  into it.
   */
  public static final String escapedChars = "<>&\r\n\t";

  /**
   *  Generate an XML representation of this Element and any subordinates it
   *  might have.
   *  @param pp a PrettyPrinter (text formatter) to receive the output.
   */
  public abstract void generateXml (PrettyPrinter pp);

  /**
   *  Report the "symbol" associated with the type of this Element.  The value
   *  is used in support of some database formats.  At present, the return is
   *  always a String consisting of a single capital letter.
   *  @return the subclass type's "symbol"
   */
  public abstract String getSymbol ();

  /**
   *  If this Element is a NullElement instance, provide a reference of the
   *  more specific type.  If not, then indicate as much by giving no reference.
   *  @return this Element as a NullElement, if appropriate.
   */
  public NullElement getAsNull () {
    return null;
  }
     
  /**
   *  If this Element is a ValElement instance, provide a reference of the
   *  more specific type.  If not, then indicate as much by giving no reference.
   *  @return this Element as a ValElement, if appropriate.
   */
  public ValElement getAsValue () {
    return null;
  }

  /**
   *  If this Element is a ListElement instance, provide a reference of the
   *  more specific type.  If not, then indicate as much by giving no reference.
   *  @return this Element as a ListElement, if appropriate.
   */
  public ListElement getAsList () {
    return null;
  }

  /**
   *  If this Element is an Attribute instance, provide a reference of the more
   *  specific type.  If not, then indicate as much by giving no reference.
   *  @return this Element as a Attribute, if appropriate.
   */
  public Attribute getAsAttribute () {
    return null;
  }

  /**
   *  If this Element is a Structure instance, provide a reference of the more
   *  specific type.  If not, then indicate as much by giving no reference.
   *  @return this Element as a Structure, if appropriate.
   */
  public Structure getAsStructure () {
    return null;
  }

  /**
   *  Convert a String to a form more suitable for writing to an XML file by
   *  encoding the characters that would conflict with markup syntax of XML.
   *  @param s the String to be encoded
   *  @return the properly coded String
   */
  public static String xmlize (String s) {
    if (s == null)
      return "";

    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (escapedChars.indexOf(c) != -1) {
        buf.append("&#");
        buf.append((int) c);
        buf.append(';');
      }
      else {
        buf.append(c);
      }
    }

    return buf.toString();
  }
}