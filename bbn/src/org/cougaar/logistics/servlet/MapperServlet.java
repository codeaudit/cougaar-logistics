/*
    <copyright>
     Copyright 2002-2003 BBNT Solutions, LLC
     under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
     and the Defense Logistics Agency (DLA).
    
     This program is free software; you can redistribute it and/or modify
     it under the terms of the Cougaar Open Source License as published by
     DARPA on the Cougaar Open Source Website (www.cougaar.org).
    
     THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
     PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
     IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
     MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
     ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
     HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
     DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
     TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
     PERFORMANCE OF THE COUGAAR SOFTWARE.
    </copyright>
  */
package org.cougaar.logistics.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.Servlet;

import java.io.PrintWriter;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;


import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AlarmService;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.logging.LoggingServiceWithPrefix;

import org.cougaar.glm.ldm.asset.ClassIXRepairPart;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.CommunityPGImpl;
import org.cougaar.planning.ldm.asset.NewClusterPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;


import org.cougaar.logistics.ldm.Constants;

import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;



/**
 * Servlet allocates prints out demand totals
 *
 *@author    MDavis
 */
public class MapperServlet extends BaseServletComponent implements BlackboardClient, UtilsProvider {

	private BlackboardService blackboard;
        private AgentIdentificationService agentService;
        private AlarmService alarmService;
	private PrintWriter out;
	private DomainService domainService = null;
        private PlanningFactory planFactory;

        private LoggingService logger;

        protected TimeUtils timeUtils;
        protected TaskUtils taskUtils;
        protected ScheduleUtils scheduleUtils;
        protected AssetUtils assetUtils;

        protected MapperPrinter printer;

	

	public void setDomainService(DomainService aDomainService) {
		domainService = aDomainService;
	}

	public DomainService getDomainService() {
		return domainService;
	}

	protected String getPath() {
		return "/mapperServlet";
	}

	protected Servlet createServlet() {
		blackboard = (BlackboardService) serviceBroker.getService(this,
			BlackboardService.class, null);
		if(blackboard == null) {
			throw new RuntimeException(
				"Unable to obtain blackboard service");
		}

		agentService = (AgentIdentificationService) serviceBroker.getService(this,
			AgentIdentificationService.class, null);
		if(agentService == null) {
			throw new RuntimeException(
				"Unable to obtain agent service");
		}

		alarmService = (AlarmService) serviceBroker.getService(this,
			AlarmService.class, null);
		if(alarmService == null) {
			throw new RuntimeException(
				"Unable to obtain alarm service");
		}

		planFactory = (PlanningFactory) getDomainService().getFactory("planning");
		

		// get the logging service
		logger = (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
		if( logger == null ) {
		    throw new RuntimeException("Cannot find logging service!");
		}

		timeUtils = new TimeUtils(this);
		assetUtils = new AssetUtils(this);
		taskUtils = new TaskUtils(this);
		scheduleUtils = new ScheduleUtils(this);

		printer = new MapperPrinter(this);

		return new MyServlet();
	}


        public LoggingService getLoggingService(Object requestor) {
	    LoggingService ls = (LoggingService)
		serviceBroker.getService(requestor,
					 LoggingService.class,
					 null);
	    return LoggingServiceWithPrefix.add(ls, getAgentIdentifier() + ": ");
	}

        public String getAgentIdentifier() {
	    if(agentService != null) {return agentService.getName();}
	    return "Dont know Agent";
	}

	public void setBlackboardService(BlackboardService blackboard) {
		this.blackboard = blackboard;
	}

	public String getBlackboardClientName() {
		return toString();
	}

	public long currentTimeMillis() {
		return alarmService.currentTimeMillis();
	}

	public boolean triggerEvent(Object event) {
		return false;
	}

	public void unload() {
		super.unload();
		//Release blackboard service
		if(blackboard != null) {
			serviceBroker.releaseService(
				this, BlackboardService.class, servletService);
			blackboard = null;
		}
	}

        public TaskUtils getTaskUtils() {
	    return taskUtils;
	}

        public TimeUtils getTimeUtils() {
	    return timeUtils;
	}
    
        public AssetUtils getAssetUtils() {
	    return assetUtils;
	}

        public ScheduleUtils getScheduleUtils() {
	    return scheduleUtils;
	}



	private class MyServlet extends HttpServlet {

	    String supplyType = "Ammunition";

	    public String getSupplyType() {
		return supplyType;
	    }

	    /** More pirate garbage

		public void doGet(HttpServletRequest req, HttpServletResponse res)
			 throws IOException {

			out = res.getWriter();
			out.println(headWithTitle("Publish Task") + "<BODY>");

			String action = "mapperServlet";

			out.println("<FORM ACTION=" + action + " METHOD=POST>\n");
			out.println("<table><tr><td>NSN:</td><td>" + getChoices()
				 + "</td></tr>");
			out.println("<tr><td>PROVIDER:</td><td><INPUT TYPE=TEXT NAME="
				 + "\"provider\"></td></tr></table>");
			out.println("<CENTER><INPUT TYPE=SUBMIT VALUE=\"PUBLISH\"></CENTER>"
				 + "</FORM></BODY>");
			out.flush();
		}

		public void doPost(HttpServletRequest req, HttpServletResponse res)
			 throws IOException {

			String nsn = req.getParameter("nsn");
			String provider = req.getParameter("provider");

			out = res.getWriter();
			out.println(headWithTitle("Task published") + "<BODY>");

			try {
				blackboard.openTransaction();
				publishTask(nsn, provider);
			} finally {
				blackboard.closeTransactionDontReset();
				out.println("<a href=taskServlet>back</a></body>");
				out.flush();
			}
			}  **/
	

	

	    public void doGet(
			      HttpServletRequest req,
			      HttpServletResponse res) throws IOException {
	
		Collection myOrgs = blackboard.query(orgsPredicate);
		Iterator orgs = myOrgs.iterator();
		Organization myOrg=null;
		String orgName = "";
		if(orgs.hasNext()) {
		    myOrg = (Organization) orgs.next();
		    orgName = myOrg.getItemIdentificationPG().getItemIdentification();
		}
		
		Collection supplys = blackboard.query(new SupplyTaskPredicate(getSupplyType(),
									      orgName,
									      getTaskUtils()));
		Collection level2s = blackboard.query(new Level2TaskPredicate(getSupplyType(),
									      orgName,
									      getTaskUtils()));
		Collection level6s = blackboard.query(new ProjectionTaskPredicate(getSupplyType(),
										  orgName,
										  getTaskUtils()));


		Worker worker = new Worker(res,
					   getSupplyType(),
					   level2s,
					   level6s,
					   supplys,
					   orgName,
					   printer);
		worker.execute();
		}

		}

  protected static class SupplyTaskPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;
    String orgName;

    public SupplyTaskPredicate(String type, String orgname, TaskUtils aTaskUtils) {
      supplyType = type;
      orgName = orgname;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.SUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
	      if(!taskUtils.isInternal(task)) {
		if (!taskUtils.isMyRefillTask(task, orgName)) {
		  //if (taskUtils.isMyDemandForecastProjection(task, orgName)){
		    return true;
		}
	      }
	    }
	  }
	}
      return false;
    }
  }



  protected static class ProjectionTaskPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;
    String orgName;

    public ProjectionTaskPredicate(String type, String orgname, TaskUtils aTaskUtils) {
      supplyType = type;
      orgName = orgname;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (!taskUtils.isLevel2(task)) {
	      if (taskUtils.isDirectObjectOfType(task, supplyType)) {
		  //OSC only
		  //Should this ever be ready for Transport on level 6? - doesn't hurt but inneficient
		  if(!(taskUtils.isReadyForTransport(task))) {
		  //if (!taskUtils.isMyRefillTask(task, orgName)) {
		  // if (taskUtils.isMyDemandForecastProjection(task, orgName)) {
		  return true;
	      }
	    }
	  }
	}
      }
      return false;
      }
  }


  protected static class Level2TaskPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;
    String orgName;

    public Level2TaskPredicate(String type, String orgname, TaskUtils aTaskUtils) {
      supplyType = type;
      taskUtils = aTaskUtils;
      orgName = orgname;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isLevel2(task)) {
            if (taskUtils.isDirectObjectOfType(task, supplyType)) {
	      // OSC only
              if(!(taskUtils.isReadyForTransport(task))) {
		// everything below me
		if (!taskUtils.isMyRefillTask(task, orgName)) {
		  // just the demand from my agent
		  //if (taskUtils.isMyDemandForecastProjection(task, orgName)) {
		  return true;
		}
	      }
	    }
	  }
	}
      }
      return false;	
    }
  }

    private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
	public boolean execute(Object o) {
	    if (o instanceof Organization) {
		return ((Organization) o).isSelf();
	    }
	    return false;
	}
    };

    private static class Worker {
	
	private HttpServletResponse response;
	private String supplyType;
	private String orgName;
	private Collection level2Tasks;
	private Collection level6Tasks;
	private Collection supplyTasks;
	private MapperPrinter printer;
	
	public Worker(HttpServletResponse response,
		      String aSupplyType,
		      Collection level2,
		      Collection level6,
		      Collection supply,
		      String myOrg,
		      MapperPrinter myPrinter) {
	    this.response = response;
	    supplyType = aSupplyType;
	    level2Tasks = level2;
	    level6Tasks = level6;
	    supplyTasks = supply;
	    orgName = myOrg;
	    printer = myPrinter;
	}
	
	public void execute() throws IOException {
	    writeResponse();
	}
	
	private void writeResponse() throws IOException {
	    response.setContentType("text/html");
	    PrintWriter out = response.getWriter();
	    
	    //printer call
	    //out.println(file);
	    printer.printDemandStats(level2Tasks,
				     level6Tasks,
				     supplyTasks,
				     supplyType,
				     orgName,
				     out);
	}
    }

}

    /*** Remove Pirated software **

	private String headWithTitle(String title) {
		return "<HEAD><TITLE>" + title + "</TITLE></HEAD>";
	}

	private void publishTask(String nsnS, String providerName) {
		Asset nsn = makePrototype(nsnS);

		NewTask task = planFactory.newTask();
		task.setDirectObject(nsn);
		task.setVerb(org.cougaar.glm.ldm.Constants.Verb.Supply);
    Plan realityPlan = planFactory.getRealityPlan();
		task.setPlan(realityPlan);

		AspectValue endTAV = TimeAspectValue.create(AspectType.END_TIME, DEFAULT_END_TIME);
		ScoringFunction endScoreFunc =
			ScoringFunction.createStrictlyAtValue(endTAV);
		Preference endPreference = planFactory.newPreference(AspectType.END_TIME, endScoreFunc);

		Vector preferenceVector = new Vector(1);
		preferenceVector.addElement(endPreference);
		task.setPreferences(preferenceVector.elements());
    blackboard.publishAdd(task);
    out.println("Published task : " + true);
		Organization org = createOrganization(providerName);

		boolean isSuccess = true;
    AllocationResult estAR = PluginHelper.createEstimatedAllocationResult(task, planFactory, 0.5, isSuccess);
		Allocation allocation = planFactory.createAllocation(task.getPlan(), task,
			org, estAR, Role.ASSIGNED);

		out.println("Allocating task to: " + providerName + "<BR>");
		out.println("Direct Object:" +
			allocation.getTask().getDirectObject().getTypeIdentificationPG().getTypeIdentification() + "<BR>");
    blackboard.publishAdd(allocation);
		out.println("Published allocation: " + true + "<BR>");
	}

	private Asset makePrototype(String itemID) {

		Asset cix = planFactory.getPrototype(itemID);
		if(cix == null) {
			cix = planFactory.createPrototype(ClassIXRepairPart.class, itemID);
		}

		return cix;
	}

	private Organization createOrganization(String orgStr) {
		final String uic = orgStr;

		Organization org = (Organization) planFactory.createAsset("Organization");
		org.initRelationshipSchedule();
		org.setLocal(false);

		((NewTypeIdentificationPG) org.getTypeIdentificationPG()).setTypeIdentification(UTC);
		NewItemIdentificationPG itemIdProp =
			(NewItemIdentificationPG) org.getItemIdentificationPG();
		itemIdProp.setItemIdentification(uic);
		itemIdProp.setNomenclature(orgStr);
		itemIdProp.setAlternateItemIdentification(orgStr);

		NewClusterPG cpg = (NewClusterPG) org.getClusterPG();
		cpg.setMessageAddress(MessageAddress.getMessageAddress(orgStr));

		CommunityPGImpl communityPG =
			(CommunityPGImpl) planFactory.createPropertyGroup(CommunityPGImpl.class);
		PropertyGroupSchedule schedule = new PropertyGroupSchedule();

		ArrayList communities = new ArrayList(1);
		communities.add("COUGAAR");
		communityPG.setCommunities(communities);
		communityPG.setTimeSpan(DEFAULT_START_TIME, DEFAULT_END_TIME);
		schedule.add(communityPG);
		org.setCommunityPGSchedule(schedule);

		return org;
	}

	private String getChoices() {
		return "<select name=\"nsn\">" +
			"<option value=\"NSN/4710007606205\" selected>"
			 + "WUC1:NSN1:4710007606205 Metal tube assembly" +
			"<option value=\"NSN/4320012017527\">"
			 + "WUC1:NSN2:4320012017527 Rotary pump" +
			"<option value=\"NSN/5930008432366\">"
			 + "WUC1:NSN3:5930008432366 Pressure switch" +
			"<option value=\"NSN/1730007603370\">"
			 + "WUC2:NSN1:1730007603370 Ground safety pin" +
			"<option value=\"NSN/5945002010273\">"
			 + "WUC2:NSN2:5945002010273 Solid state switch" +
			"<option value=\"NSN/5930011951836\">"
			 + "WUC2:NSN3:5930011951836 Sensitive switch" +
			"<option value=\"NSN/4310004145989\">"
			 + "WUC3:NSN1:4310004145989 Reciprocating compressor" +
			"<option value=\"NSN/6105007262754\">"
			 + "WUC3:NSN2:6105007262754 AC Motor" +
			"<option value=\"NSN/3110005656233\">"
			 + "WUC3:NSN3:3110005656233 Rod end roller bearing" +
			"</select>";
	}


    ***/

