/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.servlet.data.shortfall;


import org.cougaar.logistics.plugin.inventory.ShortfallInventory;


public class ShortfallInventoryRule {

  //Variables:
  ////////////

    
  public String agentMatch;
  public String invItemMatch;
  public String verbTaskType;
  public String demandOrResupply;
  public String action;

  public final String DISCOUNT_ACTION = "DISCOUNT";
  public final String WILD_CARD = "*";
  public final String PROJECTION = "ProjectSupply";
  public final String ACTUAL = "Supply";
  public final String DEMAND = "Demand";
  public final String RESUPPLY = "Resupply";
  

  //Constructors:
  ///////////////


  public ShortfallInventoryRule(String strToMatchAgent,
				String invItemMatchStr,
				String verb,
				String aDemandOrResupply,
				String anAction) {

      agentMatch = strToMatchAgent;
      invItemMatch = invItemMatchStr;
      verbTaskType = verb;
      demandOrResupply = aDemandOrResupply;
      action = anAction;
  }

 
  public String getAction() { return action; }


  public ShortfallInventory apply(String agentName,
				  ShortfallInventory si) {

      boolean matches = (((agentMatch.equals(WILD_CARD)) ||
			  (agentName.matches(agentMatch))) &&
			 ((invItemMatch.equals(WILD_CARD)) ||
			  (si.getInvID().matches(invItemMatch))));

      if(matches) {

	  ShortfallInventory ruleAppliedInv = new ShortfallInventory(si.getInvID());

	  if(action.equals(DISCOUNT_ACTION)) {
	      
	      if(!(((demandOrResupply.equals(DEMAND))  ||
		    (demandOrResupply.equals(WILD_CARD))) &&
		   ((verbTaskType.equals(ACTUAL)) ||
		    (verbTaskType.equals(WILD_CARD))))) {
		  ruleAppliedInv.setNumDemandSupply(si.getNumDemandSupply());
	      }

	      if(!(((demandOrResupply.equals(DEMAND))  ||
		    (demandOrResupply.equals(WILD_CARD))) &&
		   ((verbTaskType.equals(PROJECTION)) ||
		    (verbTaskType.equals(WILD_CARD))))) {
		  ruleAppliedInv.setNumDemandProj(si.getNumDemandProj());
	      }

	      if(!(((demandOrResupply.equals(RESUPPLY))  ||
		    (demandOrResupply.equals(WILD_CARD))) &&
		   ((verbTaskType.equals(PROJECTION)) ||
		    (verbTaskType.equals(WILD_CARD))))) {
		  ruleAppliedInv.setNumResupplyProj(si.getNumResupplyProj());
	      }

	      if(!(((demandOrResupply.equals(RESUPPLY))  ||
		    (demandOrResupply.equals(WILD_CARD))) &&
		   ((verbTaskType.equals(ACTUAL)) ||
		    (verbTaskType.equals(WILD_CARD))))) {
		  ruleAppliedInv.setNumResupplySupply(si.getNumResupplySupply());
	      }

								     
	  }

	  if(si.equals(ruleAppliedInv)) {
	      return null;
	  }
	  else {
	      return ruleAppliedInv;
	  }
      }
      return null;
  }


  public static ShortfallInventoryRule parseConfigFileLine(String configFileLine) {
      ShortfallInventoryRule rule = null;
      String[] parts = configFileLine.split(",");
      if((parts==null) || (parts.length != 5)) {
	  throw new RuntimeException("Could not parse ShortfallInventoryRule.");
      }
      return new ShortfallInventoryRule(parts[0],parts[1],parts[2],parts[3],parts[4]);      
  }

}










