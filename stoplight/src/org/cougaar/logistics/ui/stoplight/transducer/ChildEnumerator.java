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

package org.cougaar.logistics.ui.stoplight.transducer;
import org.w3c.dom.*;

/**
 *  This class provides an iterator for the child nodes of a given node in a
 *  DOM tree.  Those that contain only text are presumed to be white space and
 *  are summarily ignored.
 */
public class ChildEnumerator {
  Node[] nodes;
  int k = -1;

  /**
   *  Construct this enumerator with the non-text, non-null children of the
   *  given node, if any.  If any such children exist, the enumerator starts
   *  with the "cursor" resting <i>on</i> the first node in the sequence.
   *  @param n The node whose children are to be enumerated
   */
  public ChildEnumerator (Node n) {
    NodeList nl = n.getChildNodes();
    int length = nl.getLength();
    int nodeCount = 0;
    int i;
    for (i = 0; i < length; i++) {
      Node child = nl.item(i);
      if (child != null && !(child instanceof Text) && !(child instanceof Comment))
        nodeCount++;
    }
    // System.out.println("ChildEnumerator:  summary-- " + length + " = " +
    //   nodeCount + " (Real) + " + (length - nodeCount) + " (White Space)");
      
    nodes = new Node[nodeCount];
    int j;
    for (i = 0, j = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if (child != null && !(child instanceof Text) && !(child instanceof Comment))
        nodes[j++] = nl.item(i);
    }
  }

  /**
   *  Retrieve the node currently resting under the "cursor" in this enumerator
   *  or null if the cursor is out-of-bounds.
   *  @return the current node in the sequence
   */
  public Node current () {
    if (k < 0)
      k = 0;
    if (k < nodes.length)
      return nodes[k];
    else
      return null;
  }

  /**
   *  Retrieve the node <i>after</i> the one currently under the "cursor" or
   *  null if there is no such node.  Also move the cursor to the latter node
   *  if it exists.
   *  @return the next node in the sequence
   */
  public Node next () {
    if (k < nodes.length) {
      if (k >= 0)
        k++;
      else
        k = 0;
      return current();
    }
    else {
      return null;
    }
  }

  /**
   *  Retrieve the node <i>before</i> the one currently under the "cursor" or
   *  null if there is no such node.  Also move the cursor to that node, if it
   *  exists.
   *  @return the previous node in the sequence
   */
  public Node previous () {
    if (hasPriorNodes()) {
      k--;
      return current();
    }
    else {
      return null;
    }
  }

  /**
   *  Discover whether there are any nodes after the current one.
   *  @return true if there is a node after the current one, false otherwise.
   */
  public boolean hasMoreNodes () {
    return (k < nodes.length - 1);
  }

  /**
   *  Discover whether there are any nodes before the current one.
   *  @return true if there is a node before the current one, false otherwise.
   */
  public boolean hasPriorNodes () {
    return (k > 0);
  }

  /**
   *  Discover whether there are any nodes at all in the sequence.  This method
   *  was added purely for the sake of completeness.
   *  @return true if the enumeration is empty, false otherwise
   */
  public boolean isEmpty () {
    return (nodes.length == 0);
  }

  // In case we decide to keep the non-white-space Text nodes (for whatever
  // reason), here is a test for its being purely white-space
  private static boolean isWhiteSpace (String s) {
    java.util.StringTokenizer tok = new java.util.StringTokenizer(s, " \t\n\r");
    return !tok.hasMoreTokens();
  }
}
