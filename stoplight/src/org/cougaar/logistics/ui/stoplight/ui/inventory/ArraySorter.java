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
package org.cougaar.logistics.ui.stoplight.ui.inventory;
public class ArraySorter {
   /*
   ** Sort in the same array
   */
   public static void sort(Object[] a, Comparer comparer) {
     sort(a, null, 0, a.length - 1, true, comparer);
   }

   /*
   ** Sort a and b, using a as the reference
   */
   public static void sort(Object[] a, Object[] b,
     int from, int to, boolean ascending, Comparer comparer) {
     // No sort
     if (a == null || a.length < 2) return;

     // sort using Quicksort
     int i = from, j = to;
     Object center = a[ (from + to) / 2 ];
     do {
       if (ascending) {
         while( (i < to) && (comparer.compare(  center, a[i]) > 0) ) i++;
         while( (j > from) && (comparer.compare(center, a[j]) < 0) ) j--;
         }
       else {
         // Decending sort
         while( (i < to) && (comparer.compare(  center, a[i]) < 0) ) i++;
         while( (j > from) && (comparer.compare(center, a[j]) > 0) ) j--;
         }
       if (i < j) {
         // Swap elements
         Object temp = a[i]; a[i] = a[j]; a[j] = temp;
         // Swap in b array if needed
         if (b != null) {
           temp = b[i]; b[i] = b[j]; b[j] = temp;
         }
       }
       if (i <= j) { i++; j--; }
       } while(i <= j);
     // Sort the rest
     if (from < j) sort(a, b, from, j, ascending, comparer);
     if (i < to) sort(a, b, i, to, ascending, comparer);
     }

 public static interface Comparer {
   /**
    * The interface implementation should compare the two
    * objects and return an int using these rules:
    * if (a > b)  return > 0;
    * if (a == b) return 0;
    * if (a < b)  return < 0;
    */
    public int compare(Object a, Object b);
    }
 }






