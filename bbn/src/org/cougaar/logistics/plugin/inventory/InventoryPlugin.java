/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.glm.ldm.asset.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.glm.plugins.FileUtils;
import org.cougaar.glm.plugins.ScheduleUtils;

import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.service.DomainService;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Enumerator;
import org.cougaar.core.plugin.util.PluginHelper;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

/** The InventoryPlugin is the Glue of inventory management.
 *  It handles all blackboard services for its modules, 
 *  facilitates inter-module communication and manages the
 *  subscriptions.  The InventoryPlugin also creates inventories.
 *  All modules are called from the InventoryPlugin.
 **/

public class InventoryPlugin extends ComponentPlugin {

  private DomainService domainService;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils AssetUtils;
  private HashMap pluginParams;
  private HashMap inventoryHash;
  private HashMap inventoryInitHash;
  private ArrayList touchedInventories;
  private boolean touchedProjections;
  private String supplyType;
  private String inventoryFile;
  private UnaryPredicate dueOutPredicate;
  private ProjectionWeight projectionWeight;
  private boolean fillToCapacity;
  private boolean maintainAtCapacity;
  private LDMService ldmService = null;
  private DetReqAggHandler detReqHandler;
  private boolean configured = false;
  private Organization myOrganization;
  private String myOrgName;
  private SupplyExpander supplyExpander;
  private ExternalAllocator externalAllocator;
  private RefillGenerator refillGenerator;
  private RefillProjectionGenerator refillProjGenerator;
  private RefillComparator refillComparator;
  private AllocationAssessor allocationAssessor;
  private long startTime;
  private long cycleStamp;
  private boolean logToCSV=false;

  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String INVENTORY_FILE = "INVENTORY_FILE";
  public final String ENABLE_CSV_LOGGING = "ENABLE_CSV_LOGGING";
  // Policy variables
  private InventoryPolicy inventoryPolicy = null;
  private int criticalLevel = 3;
  private int reorderPeriod = 3;
  private int handlingTime = 0;
  private int transportTime = 1;
  private int bucketSize = 1;
  //TODO replace these with real VTH Knob values
  private long endOfLevelSix;
  private long endOfLevelTwo;

  public void load() {
    super.load();
    logger = getLoggingService(this);
    taskUtils = new TaskUtils(this);
    timeUtils = new TimeUtils(this);
    AssetUtils = new AssetUtils(this);
    detReqHandler = new DetReqAggHandler(this);
    // readParameters() initializes supplyType and inventoryFile
    pluginParams = readParameters();
    supplyExpander = new SupplyExpander(this);
    externalAllocator = new ExternalAllocator(this,getRole(supplyType));
    refillGenerator = new RefillGenerator(this);
    refillProjGenerator = new RefillProjectionGenerator(this);
    refillComparator = new RefillComparator(this);
    allocationAssessor = new AllocationAssessor(this,getRole(supplyType));
    inventoryHash = new HashMap();
    inventoryInitHash = new HashMap();
    touchedInventories = new ArrayList();
    touchedProjections = false;
    startTime = currentTimeMillis();
    // TODO replace with real VTH Knob
    endOfLevelSix = timeUtils.addNDays(startTime, 40);
    endOfLevelTwo = timeUtils.addNDays(startTime, 80);
    domainService = (DomainService) 
	getServiceBroker().getService(this,
				      DomainService.class,
      new ServiceRevokedListener() {
	  public void serviceRevoked(ServiceRevokedEvent re) {
	      if (DomainService.class.equals(re.getService()))
	      domainService  = null;
	  }
      });
  }

  public TaskUtils      getTaskUtils() {return taskUtils;}
  public TimeUtils      getTimeUtils() {return timeUtils;}
  public AssetUtils     getAssetUtils() {return AssetUtils;}  
  public String         getSupplyType() {return supplyType; }
  private String getInventoryFileName() {return inventoryFile; }
  public Organization   getMyOrganization() {return myOrganization;}
    public long getCurrentTimeMillis() { return currentTimeMillis(); }

    public boolean publishAdd(Object o) {
	return getBlackboardService().publishAdd(o);
    }

  public void publishAddExpansion(Expansion expansion) {
    PluginHelper.publishAddExpansion(getBlackboardService(), expansion);
  }

    public boolean publishChange(Object o) {
	return getBlackboardService().publishChange(o);
    }

    public boolean publishRemove(Object o) {
	return getBlackboardService().publishRemove(o);
    }

    public RootFactory getRootFactory() { 
	RootFactory rootFactory=null;
	if(domainService != null) {
		rootFactory = domainService.getFactory();
	}
	return rootFactory; 
    }

  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService) 
      getServiceBroker().getService(requestor,
				    LoggingService.class,
				    null);
  }
  
  protected void execute() {
    updateInventoryPolicy(inventoryPolicySubscription.getAddedCollection());
    updateInventoryPolicy(inventoryPolicySubscription.getChangedCollection());
    cycleStamp = (new Date()).getTime();
    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if ((myOrganization != null) && (supplyTaskSubscription == null)) {
	myOrgName = myOrganization.getItemIdentificationPG().getItemIdentification();
	inventoryFile = getInventoryFile(supplyType);
	getInventoryData();
	supplyExpander.initialize(myOrganization);
	setupSubscriptions2();
      }
    }
    if (detReqHandler.getDetermineRequirementsTask(detReqSubscription, aggMILSubscription) != null) {
      expandIncomingRequisitions(supplyTaskSubscription.getAddedCollection());
      touchedProjections = expandIncomingProjections(projectionTaskSubscription.getAddedCollection());
      // call the Refill Generators if we have new demand
      if (! getTouchedInventories().isEmpty()) {
	  //check to see if we have new projections
	  if (touchedProjections) {
	      refillProjGenerator.calculateRefillProjections(getTouchedInventories(), 
							     criticalLevel, 
							     endOfLevelSix, 
							     endOfLevelTwo, 
							     refillComparator);
	  }
	  refillGenerator.calculateRefills(getTouchedInventories(), inventoryPolicy,
					   refillComparator);
      } else { // Backwards Flow
	externalAllocator.updateAllocationResult(refillAllocationSubscription);
        allocationAssessor.reconcileInventoryLevels(getTouchedInventories());
	supplyExpander.updateAllocationResult(expansionSubscription);
      }
						       
      takeInventorySnapshot(getTouchedInventories());

      // touchedInventories should not be cleared until the end of transaction
      touchedInventories.clear();
      touchedProjections = false;
      //testBG();
    }
  }

  /** Subscription for aggregatable support requests. **/
  private IncrementalSubscription detReqSubscription;
  
  /** Subscription for the aggregated support request **/
  private CollectionSubscription aggMILSubscription;
  
  /** Subscription for the MIL tasks **/
  private IncrementalSubscription milSubscription;

  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;

  /** Subscription for incoming Supply tasks **/
  private IncrementalSubscription supplyTaskSubscription;

  /** Subscription for incoming Projection tasks **/
  private IncrementalSubscription projectionTaskSubscription;

  /** Subscription for Allocations on outgoing Refill (Supply & ProjectSupply) tasks **/
  private IncrementalSubscription refillAllocationSubscription;

  /** Subscription for Supply/ProjectSupply Expansions **/
  private IncrementalSubscription expansionSubscription;

  /** Subscription for InventoryPolicy **/
  private IncrementalSubscription inventoryPolicySubscription;


  protected void setupSubscriptions() {
    detReqSubscription = (IncrementalSubscription) blackboard.subscribe(new DetInvReqPredicate(taskUtils));
    aggMILSubscription = (CollectionSubscription) blackboard.subscribe(new AggMILPredicate(), false);
    milSubscription = (IncrementalSubscription) blackboard.subscribe(new MILPredicate());
    addInventories(blackboard.query(new InventoryPredicate(supplyType)));
    detReqHandler.addMILTasks(milSubscription.elements());
    selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);
    inventoryPolicySubscription = (IncrementalSubscription) blackboard.subscribe(new InventoryPolicyPredicate(supplyType));
  }

  protected void setupSubscriptions2() {
    supplyTaskSubscription = (IncrementalSubscription) blackboard.subscribe(new SupplyTaskPredicate(supplyType, myOrgName, taskUtils));
    projectionTaskSubscription = (IncrementalSubscription) blackboard.subscribe( new ProjectionTaskPredicate(supplyType, myOrgName, taskUtils));
    refillAllocationSubscription = (IncrementalSubscription) blackboard.subscribe(new RefillAllocPredicate(supplyType, myOrgName, taskUtils));
    expansionSubscription = (IncrementalSubscription)blackboard.subscribe(new ExpansionPredicate(supplyType, myOrgName, taskUtils));
  }

  private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
      public boolean execute(Object o) {
	if (o instanceof Organization) {
	  return ((Organization)o).isSelf();
	}
	return false;
      }
    };  	

  private static class SupplyTaskPredicate implements UnaryPredicate
  {
    String supplyType;
    String orgName;
    TaskUtils taskUtils;
    
    public SupplyTaskPredicate(String type, String myOrg, TaskUtils aTaskUtils) {
      supplyType = type;
      orgName = myOrg;
      taskUtils = aTaskUtils;
    }
    
    public boolean execute(Object o) {
      if (o instanceof Task ) {
	Task task = (Task)o;
	if (task.getVerb().equals(Constants.Verb.SUPPLY)) {
	  if (taskUtils.isDirectObjectOfType(task, supplyType)) {
	    if (!taskUtils.isMyRefillTask(task, orgName)) {
	      if (taskUtils.getQuantity(task) > 0) {
		return true;
	      }
	    }
	  }
	}	
      }
      return false;
    }
  }
  

  private static class ProjectionTaskPredicate implements UnaryPredicate
    {
	String supplyType;
	String orgName;
	TaskUtils taskUtils;

	public ProjectionTaskPredicate(String type, String orgname,TaskUtils aTaskUtils) {
	    supplyType = type;
	    orgName = orgname;
	    taskUtils = aTaskUtils;
	}

	public boolean execute(Object o) {
	    if (o instanceof Task ) {
		Task task = (Task)o;
		if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
		    if (taskUtils.isDirectObjectOfType(task, supplyType)) {
			if (!taskUtils.isMyInventoryProjection(task, orgName)) {
			    return true;
			}
		    }
		}	
	    }
	    return false;
	}
    }

  /**
     Passes DetermineRequirements tasks of type MaintainInventory.
  **/
  private static class DetInvReqPredicate implements UnaryPredicate {

      private TaskUtils taskUtils;

      public DetInvReqPredicate(TaskUtils aTaskUtils) {
	  taskUtils = aTaskUtils;
      }

    public boolean execute(Object o) {
      if (o instanceof Task) {
	Task t = (Task) o;
	if (t.getVerb().equals(Constants.Verb.DETERMINEREQUIREMENTS)) {
	  return taskUtils.isTaskOfType(t, "MaintainInventory");
	}
      }
      return false;
    }
  }
  
  /**
     Selects the per-inventory MaintainInventory tasks.
  **/
  private static class MILPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Task) {
	Task t = (Task) o;
	if (t.getVerb().equals(Constants.Verb.MAINTAININVENTORY)) {
	  return t.getDirectObject() != null; // true if this is the agg task
	}
      }
      return false;
    }
  }
  
  /**
     Selects the aggregate MaintainInventory task
  **/
  private static class AggMILPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Task) {
	Task t = (Task) o;
	if (t.getVerb().equals(Constants.Verb.MAINTAININVENTORY)) {
	  return t.getDirectObject() == null; // true if this is not the agg task
	}
      }
      return false;
    }
  }
  
  /**
     Passes Inventory assets that have a valid LogisticsInventoryPG
  **/
  private static class InventoryPredicate implements UnaryPredicate {
    String supplyType;

    public InventoryPredicate(String type){ 
      supplyType = type; 
    }

    public boolean execute(Object o) {
      if (o instanceof Inventory) {
	LogisticsInventoryPG logInvpg = 
	  (LogisticsInventoryPG) ((Inventory) o).searchForPropertyGroup(LogisticsInventoryPG.class);
	if (logInvpg != null) {
	  String type = getAssetType((Inventory)o);
	  if (supplyType.equals(type)) {
	    return true;    
	  }
	}
      }
      return false;
    }

    private String getAssetType(Inventory inventory) {
      InventoryPG invpg = (InventoryPG)inventory.getInventoryPG();
      if (invpg == null ) return null;
      Asset a = invpg.getResource();
      if (a == null) return null;
      SupplyClassPG pg = (SupplyClassPG)a.searchForPropertyGroup(SupplyClassPG.class);
      return pg.getSupplyType();
    }
  }
    
  private class ProjectionExpansionPredicate implements UnaryPredicate {
	String supplyType_;
        UnaryPredicate taskPredicate;

      public ProjectionExpansionPredicate(String type, String orgname, TaskUtils aTaskUtils) {
	    supplyType_ = type;
            taskPredicate = new ProjectionTaskPredicate(type, orgname, aTaskUtils);
	}

	public boolean execute(Object o) {
	    if (o instanceof PlanElement ) {
		Task task = ((PlanElement) o).getTask();
                return taskPredicate.execute(task);
	    }
	    return false;
	}
    }
    
    private class SupplyExpansionPredicate implements UnaryPredicate
    {
	String supplyType_;
        UnaryPredicate taskPredicate;

	public SupplyExpansionPredicate(String type,String orgname,TaskUtils aTaskUtils) {
	    supplyType_ = type;
            taskPredicate = new SupplyTaskPredicate(type, orgname, aTaskUtils);
	}

	public boolean execute(Object o) {
	    if (o instanceof Expansion) {
		Task task = ((Expansion) o).getTask();
                return taskPredicate.execute(task);
	    }
	    return false;
	}
    }

    //Allocation of refill tasks
  static class RefillAllocPredicate implements UnaryPredicate {
    String type_;
    String orgName_;
    TaskUtils taskUtils;
    
    public RefillAllocPredicate(String type, String orgName, TaskUtils aTaskUtils) {
      type_ = type;
      orgName_ = orgName;
      taskUtils = aTaskUtils;
    }
    
    public boolean execute(Object o) {
      if (o instanceof Allocation ) {
	Task task = ((Allocation)o).getTask();
	if (task.getVerb().equals(Constants.Verb.SUPPLY) ||
	    task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
	  if (taskUtils.isDirectObjectOfType(task, type_)) {
	    // need to check if externally allocated
	    if(((Allocation)o).getAsset() instanceof Organization) {
	      if (taskUtils.isMyRefillTask(task, orgName_)){
		return true;
	      }
	    }
	  }
	}
      }
      return false;
    }
  }

  private class ExpansionPredicate implements UnaryPredicate {
    String supplyType;
    String orgName;
    TaskUtils taskUtils;

    public ExpansionPredicate(String type, String orgname, TaskUtils taskUtils) {
      supplyType = type;
      orgName = orgname;
      this.taskUtils = taskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Expansion) {
	Task task = ((Expansion) o).getTask();
	if (task.getVerb().equals(Constants.Verb.SUPPLY) ||
	    task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
	  if (taskUtils.isDirectObjectOfType(task, supplyType)) {
	    if (!taskUtils.isMyRefillTask(task, orgName)) {
	      if (taskUtils.getQuantity(task) > 0) {
		return true;
	      }
	    }
	  }
	}	
      }
      return false;
    }
  }

  private class InventoryPolicyPredicate implements UnaryPredicate {
    String type;
    public InventoryPolicyPredicate(String type) {
      this.type = type;
    }
    public boolean execute(Object o) {
      if (o instanceof org.cougaar.logistics.plugin.inventory.InventoryPolicy) {
	String type = ((InventoryPolicy)o).getResourceType();
	if (type.equals(this.type)) {
	  logger.debug("Found an inventory policy for "+this.type);
	  return true;
	}
      }
      return false;
    }
  } 

  // Determines which tasks should be expanded and which should be
  // re-allocated to a supplier
  private Collection sortIncomingSupplyTasks(Collection tasks) {
    ArrayList expandList = new ArrayList();
    ArrayList passThruList = new ArrayList();
    Task t;
    Inventory inventory;
    Asset asset;
    for (Iterator i = tasks.iterator(); i.hasNext();) {
      t = (Task)i.next();
      asset = (Asset)t.getDirectObject();
      inventory = findOrMakeInventory(asset);
      if (inventory != null) {
	expandList.add(t);
      } else {  // allocate tasks to supplier?
	passThruList.add(t);
      }
    }
    externalAllocator.forwardSupplyTasks(passThruList, myOrganization);
    return expandList;
  }

  private void expandIncomingRequisitions(Collection tasks) {
    Collection tasksToExpand = sortIncomingSupplyTasks(tasks);
    supplyExpander.expandAndDistributeRequisitions(tasksToExpand);
  }

  private boolean expandIncomingProjections(Collection tasks) {
    Collection tasksToExpand = sortIncomingSupplyTasks(tasks);
    return supplyExpander.expandAndDistributeProjections(tasksToExpand);
  }
    
  /**
     Add some inventories to the inventoryHash.
     Method called during rehydration to populate inventory hash
  **/
  private void addInventories(Collection inventories) {
    for (Iterator i = inventories.iterator(); i.hasNext(); ) {
      addInventory((Inventory) i.next());
    }
  }
  
  private void addInventory(Inventory inventory) {
    String item = getInventoryType(inventory);
    inventoryHash.put(item, inventory);
  }

  private void removeInventories(Enumeration inventories) {
    while (inventories.hasMoreElements()) {
      removeInventory((Inventory) inventories.nextElement());
    }
  }

  private void removeInventory(Inventory inventory) {
    String item = getInventoryType(inventory);
    inventoryHash.remove(item);
  }

  public String getInventoryType(Inventory inventory) {
    ScheduledContentPG scp = inventory.getScheduledContentPG();
    Asset proto = scp.getAsset();
    if (proto == null) {
      logger.error("getInventoryType failed to get asset for "+
		   inventory.getScheduledContentPG().getAsset().getTypeIdentificationPG());
      return "";
    }
    return proto.getTypeIdentificationPG().getTypeIdentification();
  }

  public Inventory findOrMakeInventory(Asset resource) {
    Inventory inventory = null;
    String item = resource.getTypeIdentificationPG().getTypeIdentification();
    inventory = (Inventory) inventoryHash.get(item);
    if (inventory == null) {
      inventory = createInventory(resource, item);
      if (inventory != null) {
	addInventory(inventory);
	publishAdd(inventory);
	detReqHandler.findOrMakeMILTask(inventory, detReqSubscription, 
					aggMILSubscription);
      }
    }
    if (inventory == null) logger.debug("Inventory is null for "+item);
    else logger.debug("findOrMakeInventory(), CREATED inventory bin for: "+
		      AssetUtils.assetDesc(inventory.getScheduledContentPG().getAsset()));
    return inventory;
  }
  
  protected Inventory createInventory(Asset resource, String item) {
    double levels[] = null;
    Inventory inventory = null;
    levels = (double[])inventoryInitHash.get(item);
    if (levels != null) {
      inventory=(Inventory)getRootFactory().createAsset("Inventory");
      ((NewItemIdentificationPG)inventory.getItemIdentificationPG()).setItemIdentification("Inventory:" + item);
      NewLogisticsInventoryPG logInvPG = 
	(NewLogisticsInventoryPG)PropertyGroupFactory.newLogisticsInventoryPG();
      inventory.addOtherPropertyGroup(logInvPG);

      logInvPG.setIsLevel2(false); // will need to key off asset to identify level2 item
      logInvPG.setCapacity(levels[0]);
      logInvPG.setInitialLevel(levels[1]);
      logInvPG.setResource(resource);
      logInvPG.setOrg(getMyOrganization());
      logInvPG.setLogInvBG(new LogisticsInventoryBG(logInvPG));
      logInvPG.initialize(startTime, criticalLevel, reorderPeriod, bucketSize, getCurrentTimeMillis(), logToCSV, this);

      NewTypeIdentificationPG ti = 
	(NewTypeIdentificationPG)inventory.getTypeIdentificationPG();
      ti.setTypeIdentification("InventoryAsset");
      ti.setNomenclature("Inventory Asset");

      NewScheduledContentPG scp;
      scp = (NewScheduledContentPG)inventory.getScheduledContentPG();
      scp.setAsset(resource);
      scp.setSchedule(ScheduleUtils.buildSimpleQuantitySchedule(levels[1], startTime, 
								startTime+(TimeUtils.MSEC_PER_DAY*10)));
    }
    return inventory;
  }

  public void touchInventory(Inventory inventory) {
    if (!touchedInventories.contains(inventory)) {
      touchedInventories.add(inventory);
    }
  }

  public Collection getTouchedInventories() {
    return touchedInventories;
  }

  public Collection getInventories() {
    return inventoryHash.values();
  }

  public void takeInventorySnapshot(Collection inventories) {
    Inventory inv;
    Iterator inv_it = inventories.iterator();
    LogisticsInventoryPG logInvPG = null;
    while (inv_it.hasNext()) {
      inv = (Inventory)inv_it.next();
      logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      logInvPG.takeSnapshot(inv);
      if(logToCSV) {
	  logInvPG.logAllToCSVFile(cycleStamp);
      }
    }
  }

  /**
     Read the Plugin parameters(Accepts key/value pairs)
     Initializes supplyType and inventoryFile
  **/
  private HashMap readParameters() {
    final String errorString = "InventoryPlugin requires 1 parameter, Supply Type.  Additional parameter for csv logging, default is disabled.   e.g. org.cougaar.logistics.plugin.inventory.InventoryPlugin("+SUPPLY_TYPE+"=BulkPOL, ENABLE_CSV_LOGGING=true);";
    Collection p = getParameters();
    
    if (p.isEmpty()) {
      logger.error(errorString);
      return null;
    }
    HashMap map = new HashMap();
    int idx;
 
    for (Iterator i = p.iterator();i.hasNext();) {
      String s = (String)i.next();
      if ((idx=s.indexOf('=')) != -1) {
	String key = new String(s.substring(0, idx));
	String value = new String(s.substring(idx+1, s.length()));
	map.put(key.trim(), value.trim());
      }
    }
    supplyType = (String)map.get(SUPPLY_TYPE);
//      inventoryFile = (String)map.get(INVENTORY_FILE);
    if (supplyType == null)
      logger.error(errorString);
    String loggingEnabled = (String)map.get(ENABLE_CSV_LOGGING);
    if((loggingEnabled != null) &&
       (loggingEnabled.trim().equals("true"))) {
	logToCSV = true;
    }
    return map;
  }

  private String getInventoryFile(String type) {
    String result = null;
    // if defined in plugin argument list
    String inv_file = null;
    if ((inv_file = (String)pluginParams.get(INVENTORY_FILE)) != null) {
      result = inv_file;
    } 
    else {
      result = getClusterSuffix(myOrganization.getClusterPG().getClusterIdentifier().toString()) +
	"_"+type.toLowerCase()+".inv";
    }
    return result;
  }
  
  private String getClusterSuffix(String clusterId) {
    String result = null;
    int i = clusterId.lastIndexOf("-");
    if (i == -1) {
      result = clusterId;
    } 
    else {
      result = clusterId.substring(i+1);
    }
    return result;
  }
  
  public void publishAddToExpansion(Task parent, Task subtask) {
    //attach the subtask to its parent and the parent's workflow
    PlanElement pe = parent.getPlanElement();
    Expansion expansion;
    NewWorkflow wf;
    ((NewTask) subtask).setParentTask(parent);
    ((NewTask) subtask).setPlan(parent.getPlan());
    // Task has not been expanded, create an expansion
    if (pe == null) {
      RootFactory factory = getRootFactory();
      // Create workflow
      wf = (NewWorkflow)factory.newWorkflow();
      wf.setParentTask(parent);
      wf.setIsPropagatingToSubtasks(true);
      wf.addTask(subtask);
      ((NewTask) subtask).setWorkflow(wf);
      // Build Expansion
      expansion = factory.createExpansion(parent.getPlan(), parent, wf, null);
      // Publish Expansion
      publishAdd(expansion);
    }
    // Task already has expansion, add task to the workflow and publish the change
    else if (pe instanceof Expansion) {
      expansion =(Expansion)pe;
      wf = (NewWorkflow)expansion.getWorkflow();
      wf.addTask(subtask);
      ((NewTask) subtask).setWorkflow(wf);
      publishChange(expansion);
    }
    else {
      logger.error("publishAddToExpansion: problem pe not Expansion? "+pe);	    
    }
 
   // Publish new task
    if (!publishAdd(subtask)) {
      logger.error("publishAddToExpansion fail to publish task "+taskUtils.taskDesc(subtask));
    }
  }

  // called by the RefillGenerator to hook up the refill task to the maintain
  // inventory parent task and workflow.
  public void publishRefillTask(Task task, Inventory inventory) {
    Task milTask = detReqHandler.findOrMakeMILTask(inventory,
						   detReqSubscription, aggMILSubscription);
    publishAddToExpansion(milTask, task);
  }

  private Organization getMyOrganization(Enumeration orgs) {
    Organization myOrg = null;
    // look for this organization
    if (orgs.hasMoreElements()) {
      myOrg = (Organization) orgs.nextElement();
    }
    return myOrg;
  }

  public void getInventoryData() {
    String invFile = getInventoryFileName();
    if (invFile != null) {
      Enumeration initialInv = FileUtils.readConfigFile(invFile, getConfigFinder());
      if (initialInv != null) {
	stashInventoryInformation(initialInv);
      }
    }
  }

  private void stashInventoryInformation(Enumeration initInv){
    String line;
    String item = null;
    double capacity, level;

    while(initInv.hasMoreElements()) {
      line = (String) initInv.nextElement();
      // Find the fields in the line, values seperated by ','
      Vector fields = FileUtils.findFields(line, ',');
      if (fields.size() < 3)
	continue;
      item = (String)fields.elementAt(0);
      capacity = Double.valueOf((String)fields.elementAt(1)).doubleValue();
      level = Double.valueOf((String)fields.elementAt(2)).doubleValue();
      double[] levels = {capacity,level};
      inventoryInitHash.put(item, levels);
    }
  }

  private Role getRole(String supply_type) {
    if (supply_type.equals("Ammunition"))
      return Constants.Role.AMMUNITIONPROVIDER;
    if (supply_type.equals("BulkPOL"))
      return Constants.Role.FUELSUPPLYPROVIDER;
    if (supply_type.equals("Consumable"))
      return Constants.Role.SPAREPARTSPROVIDER;
    if (supply_type.equals("PackagedPOL"))
      return Constants.Role.PACKAGEDPOLSUPPLYPROVIDER;
    logger.error("Unsupported Supply Type");
    return null;
  }

  protected boolean updateInventoryPolicy(Collection policies) {
    InventoryPolicy pol;
    boolean changed = false;
    Iterator policy_iterator = policies.iterator();
    while (policy_iterator.hasNext()) {
      pol = (InventoryPolicy)policy_iterator.next();
      inventoryPolicy = pol;
      int cl = pol.getCriticalLevel();
      if ((cl >= 0) && (cl != criticalLevel)) {
	criticalLevel = cl;
	changed = true;
      }
      int rp = pol.getReorderPeriod();
      if ((rp >= 0) && (rp != reorderPeriod)) {
	reorderPeriod = rp;
	changed = true;
      }
    }
    return changed;
  }

  /**
     Self-Test
  **/
  public void automatedSelfTest() {
    if (supplyType == null) logger.error("No SupplyType Plugin parameter.");
    if (inventoryFile == null) logger.error("No Inventory File Plugin parameter.");
    if (inventoryInitHash.isEmpty()) {
	logger.error("No initial inventory information.  Inventory File is empty or non-existant.");
	logger.error("Could not find Inventory file : "+inventoryFile);
    }
    if (detReqHandler.getDetermineRequirementsTask(detReqSubscription, aggMILSubscription) == null)
      logger.error("Missing DetermineRequirements for MaintainInventory task.");
    System.out.println("Critical Level is "+criticalLevel);
    System.out.println("Reorder Period is "+reorderPeriod);
    System.out.println("Days per bucket is "+bucketSize);
  }

  private void testBG() {
    Iterator inv_it = inventoryHash.values().iterator();
    Inventory inv;
    LogisticsInventoryPG logInvPG = null;
    cycleStamp = (new Date()).getTime();
    while (inv_it.hasNext()) {
      inv = (Inventory)inv_it.next();
      System.out.println("***"+inv.getItemIdentificationPG().getItemIdentification());
      logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      logInvPG.takeSnapshot(inv);
      if(logToCSV) {
	  logInvPG.logAllToCSVFile(cycleStamp);
      }
      logInvPG.Test();
    }
  }
}

