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

package org.cougaar.logistics.ui.stoplight.ui.inventory;


public class TestUIChart
{

  public static void main(String[] args)
  {
     //InventoryChartUI icui = new InventoryChartUI("3-FSB",  "MEAL READY-TO-EAT:NSN/8970001491094", System.currentTimeMillis(), System.currentTimeMillis());
     //InventoryChartUI icui = new InventoryChartUI("3-FSB",  null, 994291200, 995155200);
     InventoryChartUI icui = new InventoryChartUI();
     //icui.startup("3-FSB",  null, 994291200, 995155200);
     //icui.startup(null,  null, 0, 0, null, null);
     icui.startup("3-FSB",  null, 994291200, 995155200, "localhost", "5555", 4);
     //icui.populate("3-FSB",  "MEAL READY-TO-EAT:NSN/8970001491094");
     //InventoryChartUI icui = new InventoryChartUI(null,  null, 0, 0);
     //InventoryChartUI icui = new InventoryChartUI(null,  null, 994291200, 995155200);

  }
}
