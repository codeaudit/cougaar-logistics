/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.logistics.ldm.asset;

import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.policy.RangeRuleParameterEntry;
import org.cougaar.planning.ldm.policy.KeyRuleParameterEntry;
import org.cougaar.planning.ldm.policy.KeyRuleParameter;
import org.cougaar.planning.ldm.policy.RangeRuleParameter;
import org.cougaar.glm.ldm.asset.MilitaryPerson;
import org.cougaar.glm.ldm.asset.PackagePG;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;


import org.cougaar.logistics.plugin.inventory.LogisticsOPlan;
import org.cougaar.logistics.plugin.utils.*;
import org.cougaar.logistics.ldm.ClassIConsumerPrototypeProvider;
import org.cougaar.logistics.ldm.policy.FeedingPolicy;

import java.math.BigDecimal;
import java.util.*;

public class SubsistenceConsumerBG extends ConsumerBG {

  protected SubsistenceConsumerPG myPG;
  transient ClassIConsumerPrototypeProvider parentPlugin;
  String supplyType = "Subsistence";
  private transient LoggingService logger;
  private LogisticsOPlan logOPlan = null;
  private List consumedItems = new ArrayList();
  private FeedingPolicy feedingPolicy = null;
  private final static String BOTTLED_WATER = "NSN/8960013687383";  
  private final static String FRESH_FRUITS = "NSN/891501F768439";
  private final static String FRESH_VEGETABLES = "NSN/891500V264926";

  public SubsistenceConsumerBG(SubsistenceConsumerPG pg) {
    myPG = pg;
  }

  public void initialize(ClassIConsumerPrototypeProvider plugin) {
    parentPlugin = plugin;
    logger = parentPlugin.getLoggingService(this);
   }

  public List getPredicates() {
    ArrayList predList = new ArrayList();
    String typeId = myPG.getMei().getTypeIdentificationPG().getTypeIdentification();
    predList.add(new MilitaryPersonPred());
    predList.add(new OrgActivityPred());
    predList.add(new LogisticsOPlanPredicate());
    predList.add(new FeedingPolicyPred(myPG.getService()));
    return predList;
  }

  public Schedule getParameterSchedule(Collection col, TimeSpan span) {
    Schedule paramSchedule = null;
    Vector params = new Vector();
    Iterator predList = col.iterator();
    UnaryPredicate predicate;
    // DEBUG
//      String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
//     if (myOrgName.indexOf("35-ARBN") >= 0) {
//       System.out.println("getParamSched() Asset is "+
//                       myPG.getMei().getTypeIdentificationPG().getTypeIdentification());
//     }
    while (predList.hasNext()) {
      Iterator list = ((Collection)predList.next()).iterator();
      predicate = (UnaryPredicate)list.next();
      if (predicate instanceof MilitaryPersonPred) {
        Schedule consumerSched = 
          parentPlugin.getScheduleUtils().createConsumerSchedule((Collection)list.next());
//      if (myOrgName.indexOf("35-ARBN") >= 0) {
//        System.out.println("getParamSched() ConsumerSched "+consumerSched);
//      }
        params.add(parentPlugin.getScheduleUtils().convertQuantitySchedule(consumerSched));
      } else if (predicate instanceof OrgActivityPred) {
        Schedule orgActSched = 
          parentPlugin.getScheduleUtils().createOrgActivitySchedule((Collection)list.next());
        params.add(orgActSched);
//      if (myOrgName.indexOf("35-ARBN") >= 0) {
//        System.out.println("getParamSched() OrgActSched "+orgActSched);
//      }
      } else if (predicate instanceof LogisticsOPlanPredicate) {
        Collection oplanColl = (Collection)list.next();
        Iterator it = oplanColl.iterator();
        if (it.hasNext()) {
          logOPlan = (LogisticsOPlan)it.next();
        }
      } else if (predicate instanceof FeedingPolicyPred) {
        Collection fpColl = (Collection) list.next();
        Iterator it = fpColl.iterator();
        if (it.hasNext()) {
          feedingPolicy = (FeedingPolicy)it.next();
          params.add(getFeedingPolicySchedule(feedingPolicy));
          params.add(getEnhancementPolicySchedule(feedingPolicy));
          params.add(getWaterPolicySchedule(feedingPolicy));
        }
      } else {
        logger.error("getParameterSchedule: unknown predicate "+predicate);
      }
    }
    paramSchedule = parentPlugin.getScheduleUtils().getMergedSchedule(params);
//     if (myOrgName.indexOf("35-ARBN") >= 0) {
//       System.out.println("getParamSched() MERGED "+paramSchedule);
//     }
    return paramSchedule;
  }

  private HashMap addWater(HashMap water, String key, Double value) {
    water.put(key, value);
    return water;
  }    

  private HashMap addEnhancements(HashMap enhance, String []nsn_keys, int j, FeedingPolicy fp) {
    KeyRuleParameterEntry [] keys = fp.getEnhancementsKeys(j);
    for ( int i = 0; i < keys.length; i++) {
      enhance.put(nsn_keys[i], new Double(keys[i].getValue()));
    }    
    return enhance;
  }      

  protected Schedule getFeedingPolicySchedule(FeedingPolicy fp) {
    Vector sched_els = new Vector();
    ObjectScheduleElement ose;
    long start, end;
    ObjectScheduleElement element;
    Vector mealsSched = createMealAndSupplementSchedule(fp);
    for (int i = 0; i < mealsSched.size(); i++) {
      ose = (ObjectScheduleElement)mealsSched.elementAt(i);
      // e.g., min = 0 then start is C + 0 or C - Day of the operation
      // e.g., min = 1 then start is C + 1 or 2nd day of the operation
      start =  parentPlugin.getTimeUtils().addNDays(logOPlan.getStartTime(), (int)ose.getStartTime());
      end = parentPlugin.getTimeUtils().addNDays(start,((int)ose.getEndTime() - 
                                                        (int)ose.getStartTime() + 1));
      element = new ObjectScheduleElement(start, end, ose.getObject());
      sched_els.addElement(element);
      start = end;
    }
    return parentPlugin.getScheduleUtils().newObjectSchedule(sched_els.elements());
  } 
 
  protected Schedule getWaterPolicySchedule(FeedingPolicy fp) {
    Vector sched_els = new Vector(0);
    if (fp != null) {
      RangeRuleParameterEntry[] waterRanges = fp.getWaterPolicyRanges();
      ObjectScheduleElement element;
      long start, end;
      // e.g., min = 0 then start is C + 0 or C - Day of the operation
      // e.g., min = 1 then start is C + 1 or 2nd day of the operation
      if (waterRanges.length > 0) {
        start =  parentPlugin.getTimeUtils().addNDays(logOPlan.getStartTime(), waterRanges[0].getRangeMin());
        for (int i = 0; i < waterRanges.length; i++) {
          end = parentPlugin.getTimeUtils().addNDays(start,(waterRanges[i].getRangeMax() - 
                                                            waterRanges[i].getRangeMin() + 1));
          KeyRuleParameterEntry [] keys = fp.getRangeKeys(waterRanges[i]);
          element = new ObjectScheduleElement(start, end, 
                                              addWater(new HashMap(), BOTTLED_WATER, new Double(keys[0].getValue())));
          sched_els.addElement(element);
          start = end;
        }
      }
    }// else feeding policy is null return empty schedule
    return parentPlugin.getScheduleUtils().newObjectSchedule(sched_els.elements());
  }        

  protected Schedule getEnhancementPolicySchedule(FeedingPolicy fp) {
    Vector sched_els = new Vector(0);
    String [] nsns = {FRESH_FRUITS, FRESH_VEGETABLES};
    if (fp != null) {
      RangeRuleParameterEntry[] eRanges = fp.getEnhancementsPolicyRanges();
      ObjectScheduleElement element;
      long start, end;
      // e.g., min = 0 then start is C + 0 or C - Day of the operation
      // e.g., min = 1 then start is C + 1 or 2nd day of the operation
      if (eRanges.length > 0) {
        start =  parentPlugin.getTimeUtils().addNDays(logOPlan.getStartTime(), eRanges[0].getRangeMin());
        for (int i = 0; i < eRanges.length; i++) {
          end = parentPlugin.getTimeUtils().addNDays(start,(eRanges[i].getRangeMax() - 
                                                            eRanges[i].getRangeMin() + 1));
          element = new ObjectScheduleElement(start, end, addEnhancements(new HashMap(), nsns, i, fp));
          sched_els.addElement(element);
          start = end;
        }
      }
    }// else feeding policy is null return empty schedule
    return parentPlugin.getScheduleUtils().newObjectSchedule(sched_els.elements());
  } 

  public Rate getRate(Asset asset, List params) {

    double quantity = 0; // people
    // CDW
    if (params.size() < 1) {
      return null;
    } // if
    Object obj = params.get(0);
    if (obj instanceof Double) {
      quantity = ((Double) obj).doubleValue();
    } else {
      if (obj != null) {
        logger.debug ( "Bad param - expected quantity got " + obj); 
      } // if
      return null;
    } // if
    OrgActivity act = null;
    obj = params.get(1);
    if (obj instanceof OrgActivity) {
      act = (OrgActivity) obj;
    } else {
      if (obj != null) {
        logger.debug ( "Bad param - expected OrgActivity got " + obj); 
      } // if
      return null;
    } // if

    KeyRuleParameterEntry[] keys = 
      getActionPolicy(act.getActivityType(), act.getOpTempo()); 
    if ((keys != null) && (keys.length == 0)) {
      return null;
    } // if

    Rate result = null;
    String identifier = parentPlugin.getAssetUtils().getAssetIdentifier (asset);

    PackagePG ppg = (PackagePG) asset.searchForPropertyGroup(PackagePG.class);
    if (ppg == null) {
      logger.error("No PackagePG on "+identifier);
    }
    //String type = null;
    double resource_count = 0;

    if (keys != null) {
      for (int j = 0; j < keys.length; j++) {
        if ((keys[j].getKey().equalsIgnoreCase("Breakfast")
             && (keys[j].getValue().equals(identifier)))
            || (keys[j].getKey().equalsIgnoreCase("Lunch")
                && (keys[j].getValue().equals(identifier)))
            || (keys[j].getKey().equalsIgnoreCase("Dinner")
                && (keys[j].getValue().equals(identifier)))) {
          resource_count += 1.0;
        }
      }
    } else {
      // Optempo does not over rule
      logger.debug( " meal params is " +
                    params.get(2) + " resource is " + identifier);
      if (params.size() < 3) {
        logger.error("Class I ose array in getRate() is missing element "+2+" (meal)");
      } else if (params.get(2) != null) {
          if (((HashMap) params.get(2)).containsKey(identifier)) {
            // Meals
            resource_count += ((Double) ((HashMap)
                                         params.get(2)).get(identifier)).doubleValue(); 
            logger.debug(identifier+" rate is "+resource_count);
          } // if
          // DEBUG
          else {
            logger.debug("No meal rates for "+identifier);
          }
      } // if

      // Enhancements policy
      if (params.get(3) != null) {
        if (((HashMap) params.get(3)).containsKey(identifier)) {
          // Meals
          resource_count += ((Double) ((HashMap)
                                       params.get(3)).get(identifier)).doubleValue(); 
          logger.debug ( " enhance params is " + ((Double)
                                                  ((HashMap) params.get(3)).get (identifier)).doubleValue());
        }
      }
    }

    // Water
    if (params.size() < 5) {
      logger.error("Class I ose array in getRate() is missing element "+5+" water");
    } else {
      if (params.get(4) != null) {
        if (((HashMap) params.get(4)).containsKey(identifier)) {
          // water
          resource_count += ((Double) ((HashMap)
                                       params.get (4)).get(identifier)).doubleValue(); 
          logger.debug ( " water params is " + ((Double) ((HashMap)
                                                         params.get(4)).get (identifier)).doubleValue());
        } // if
      } // if
    } // if

    if (resource_count > 0) {
      double total = 
        Math.ceil (resource_count * (1.0 / ppg.getCountPerPack()) * quantity); 
      result = CountRate.newEachesPerDay (total);
      RationPG rpg = (RationPG)
        asset.searchForPropertyGroup(RationPG.class);
      logger.debug ("\n THE rate is " +
                   CountRate.newEachesPerDay (total) + " for asset " +
                   identifier + " the ration type is " + rpg.getRationType());
      logger.debug ( " Unit of Issue  is " + ppg.getUnitOfIssue()
                    + " count per pack" + ppg.getCountPerPack());
    } // if
    return result;
  } // getRate


  public Collection getConsumed() {
    if (consumedItems.isEmpty()) {
      Vector result = parentPlugin.generateRationList();

      if (result == null) {
        logger.debug("getConsumed(): Database query returned EMPTY result set for "+
                     myPG.getMei()+", "+supplyType);
      } else {
        parseResults(result);
      }
    }
    return consumedItems;
  }

  public Collection getConsumed(int x) {
    return getConsumed();
  }

  public Collection getConsumed(int x, int y) {
    return getConsumed();
  }

  protected void parseResults (Vector result) {
    logger.debug ("parseACRResult() for consumer "+ myPG.getMei() + " asset type " +supplyType);
    //String nsn, typeid, optempo;
    Asset newAsset;
    //String typeIDPrefix = "NSN/";
    Enumeration results = result.elements();
    //Object row[];
    //Vector part_types = new Vector();
    while (results.hasMoreElements()) {
      newAsset = (Asset)results.nextElement();
      consumedItems.add(newAsset);
    }
  }

  public PGDelegate copy(PropertyGroup pg) {
    return null;
  }

  private KeyRuleParameterEntry[] getActionPolicy(String activity, String optempo) {
    RangeRuleParameterEntry[] rules = feedingPolicy.getRules();
    RangeRuleParameter theRule = new RangeRuleParameter();
    KeyRuleParameterEntry [] keys;
    boolean found = false;
    boolean flag = true;
    int j = 0;
    int i = 0;
    while ((!found) && (i < rules.length)) {
      theRule = (RangeRuleParameter)rules[i].getValue();
      keys = feedingPolicy.getConditionKeys(theRule);
      j = 0;
      flag = true;
      while (flag && (j < keys.length)) {
	if ((!keys[j].getKey().equalsIgnoreCase("OrgActivity")) && (!keys[j].getKey().equalsIgnoreCase("Optempo"))) {
	  flag = false;
	} else {
	  if ((!keys[j].getValue().equalsIgnoreCase(activity)) && (!keys[j].getValue().equalsIgnoreCase(optempo))) {
	    flag = false;
	  } 
	} 
	j++;
      }
      if (flag) {
	found = true;
      } else {
	i++;
      }
    } 
    if (found) {
      return feedingPolicy.getActionKeys(theRule);	
    } 
    return null;
  }

  /**
   * Get the mandatory supplements from the property group and create a map
   * that contains the daily consumption rate for both the supplements
   * and the meals for a given range.
   * @param LogClassIPolicy
   * @return Vector of ObjectScheduleElements
   */
  private Vector createMealAndSupplementSchedule (FeedingPolicy policy) {
    Vector sched_els = new Vector();
    if (policy != null) {
      RangeRuleParameterEntry[] mealRanges = policy.getMealPolicyRanges();
      ObjectScheduleElement element;
      for (int i = 0; i < mealRanges.length; i++) {
	element = new ObjectScheduleElement( mealRanges[i].getRangeMin(),
					     mealRanges[i].getRangeMax(), 
					     addMeals(new HashMap(), i, policy));
	sched_els.addElement(element);
      } // for
    } // if
    return sched_els;
  } // createMealAndSupplementSchedule

		
  private HashMap addMeals (HashMap map, int j, FeedingPolicy p) {
    RangeRuleParameterEntry[] meals; // bRanges, lRanges, dRanges;
    meals = p.getMealPolicyRanges();
    if (meals == null) {
      return map;
    } // if
    KeyRuleParameterEntry[] keys = p.getRangeKeys(meals[j]);
    String nsn;
    for (int i = 0; i < keys.length; i++) {
      nsn = keys[i].getValue().toString();
      if (nsn != null && nsn.length() > 0) {
	calculateConsumptionRate(map, nsn);
	map = addSupplementRate(map, nsn);
      } // if
    } // for
    return map;
  } // addMeals


  private HashMap calculateConsumptionRate (HashMap m, String key) {
    double rate = 0;
    if (m.containsKey(key)) {
      rate = ((Double)m.get(key)).doubleValue();
      ++rate;
      m.put(key, new Double(rate));
    } else { // else first time in 
      m.put(key, new Double(++rate));
    } // if
    return m;
  }	 // calculateConsumptionRate


  private HashMap addSupplementRate (HashMap m, String n) {
    Asset item = parentPlugin.getPrototype(n);
    if (item != null) {
      RationPG rpg = (RationPG)item.searchForPropertyGroup(RationPG.class);
      HashMap supplements = rpg.getMandatorySupplement();
      //	System.JTEST.out.println (" the supplements list is "  + supplements);
      for (Iterator i = supplements.keySet().iterator(); i.hasNext();) { 
	String nsn = (String)i.next();
	m = calculateSupplementRate(m, nsn, ((BigDecimal)supplements.get(nsn)).doubleValue());
      } // for
    } // if
    return m;
  } // addSupplementRate


  private HashMap calculateSupplementRate (HashMap m, String key, double value) {
    if (m.containsKey(key)) {
      value = value + ((Double)m.get(key)).doubleValue();
      m.put(key, new Double(value));
    } else {  
      m.put(key, new Double(value));
    } 
    return m;
  }	


}




