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

package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.oplan.TimeSpan;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.logistics.plugin.servicediscovery.RelationshipUILauncherFrame;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

public class RelationshipScheduleServlet
        extends HttpServlet {

    private SimpleServletSupport support;
    private LoggingService logger;


    public void setSimpleServletSupport(SimpleServletSupport support) {
        this.support = support;
    }

    public void setLoggingService(LoggingService loggingService) {
        this.logger = loggingService;
    }

    public void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        // create a new "RelationshipScheduleGetter" context per request
        RelationshipScheduleGetter ig = new RelationshipScheduleGetter(support, logger);
        ig.execute(request, response);
    }

    public void doPut(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        // create a new "RelationshipScheduleGetter" context per request
        RelationshipScheduleGetter ig = new RelationshipScheduleGetter(support, logger);
        try {
            //System.out.println("\n\n\n\n\n\n********* BEGIN PUT");
            ig.execute(request, response);
            //System.out.println("\n\n\n\n\n\n\n****** DID PUT");
        } catch (Exception e) {
            //System.out.println("\n\n\n\n\n********* FAILED PUT!!  Exception: "+e);
            e.printStackTrace();
        }
    }

    /**
     * This inner class does all the work.
     * <p>
     * A new class is created per request, to keep all the
     * instance fields separate.  If there was only one
     * instance then multiple simultaneous requests would
     * corrupt the instance fields (e.g. the "out" stream).
     * <p>
     * This acts as a <b>context</b> per request.
     */
    private static class RelationshipScheduleGetter {

        ServletOutputStream out;

        Date startCDay = null;
        Date endCDay = null;

        /* since "RelationshipScheduleGetter" is a static inner class, here
         * we hold onto the support API.
         *
         * this makes it clear that RelationshipScheduleGetter only uses
         * the "support" from the outer class.
         */
        SimpleServletSupport support;
        LoggingService logger;


        final public static String RELATIONSHIP_SCHEDULE_NO_OVERLAP = "RELATIONSHIP_SCHEDULE_NO_OVERLAP";

        final public static String RELATIONSHIP_SCHEDULE_W_OVERLAP = "RELATIONSHIP_SCHEDULE_W_OVERLAP";

        protected static UnaryPredicate orgsPredicate = new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof Organization) {
                    return ((Organization) o).isSelf();
                }
                return false;
            }
        };

        public RelationshipScheduleGetter(SimpleServletSupport aSupport,
                                          LoggingService aLoggingService) {
            this.support = aSupport;
            this.logger = aLoggingService;
        }

        /*
          Called when a request is received from a client.
          Either gets the command ASSET to return the names of all the assets
          that contain a ScheduledContentPG or
          gets the name of the asset to plot from the client request.
        */
        public void execute(
                HttpServletRequest req,
                HttpServletResponse res) throws IOException {

            this.out = res.getOutputStream();

            String relationshipScheduleType;

            int len = req.getContentLength();
            if (len > 0) {
                //System.out.println("READ from content-length[" + len + "]");
                InputStream in = req.getInputStream();
                BufferedReader bin = new BufferedReader(new InputStreamReader(in));
                relationshipScheduleType = bin.readLine();
                bin.close();
                relationshipScheduleType = relationshipScheduleType.trim();
                //System.out.println("POST DATA: " + relationshipScheduleType);
            } else {
                System.out.println("WARNING: No relationshipScheduleType");
                return;
            }


            Organization myOrg = getMyOrganization();

            getStartEndDay();

            String orgId = myOrg.getItemIdentificationPG().getItemIdentification();

            RelationshipScheduleData data = new RelationshipScheduleData(startCDay.getTime(),
                                                                         orgId);

            if (relationshipScheduleType.equals(RELATIONSHIP_SCHEDULE_NO_OVERLAP)) {
                getNonOverlappingRelationshipMap(myOrg, data);
            } else {
                getOverlappingRelationshipMap(myOrg, data);
            }


            ObjectOutputStream output = new ObjectOutputStream(out);

            output.writeObject(data);
            //System.out.println("Sent XML document");

        }

        protected HashSet getAllRelationshipRoles(Organization myOrg) {
            HashSet roles = new HashSet();


            RelationshipSchedule relSched = myOrg.getRelationshipSchedule();
            Collection relationships = relSched.getMatchingRelationships(TimeSpan.MIN_VALUE,
                                                                         TimeSpan.MAX_VALUE);

            Iterator rit = relationships.iterator();

            while (rit.hasNext()) {
                Relationship r = (Relationship) rit.next();
                HasRelationships hr = relSched.getOther(r);
                if (hr instanceof Organization) {
                    Role role = relSched.getOtherRole(r);
                    roles.add(role);
                }
            }
            return roles;
        }

        protected long getBoundedStartTime(Date startDate) {
            long relStartTime = startDate.getTime();
            if (relStartTime == TimeSpan.MIN_VALUE) {
                logger.debug("RelSchedServlet- encountered a starttime equal to the Timespan.MIN_VALUE.");
            }
            return Math.max(relStartTime, startCDay.getTime());
        }

        protected long getBoundedEndTime(Date endDate) {
            long relEndTime = endDate.getTime();
            if (relEndTime == TimeSpan.MAX_VALUE) {
                logger.debug("RelSchedServlet- encountered a endtime equal to the Timespan.MAX_VALUE.");
            }
            return Math.min(relEndTime, endCDay.getTime());
        }

        protected void getNonOverlappingRelationshipMap(Organization myOrg,
                                                        RelationshipScheduleData data) {
            RelationshipSchedule relSched = myOrg.getRelationshipSchedule();

            Collection roles = getAllRelationshipRoles(myOrg);

            Iterator roleIT = roles.iterator();

            int ctr = 1;
            int colorCtr = 0;

            while (roleIT.hasNext()) {
                Role role = (Role) roleIT.next();

                /** TODO: do you want to filter out the SUPERIOR/SUBORDINATE Relatioships?
                 if(!((role.getName().equals(Relationships.SUPERIOR)) ||
                 (role.getName().equals(Relationships.SUBORDINATE)))) {

                 **/

                Collection relationships = relSched.getMatchingRelationships(role,
                                                                             startCDay.getTime(),
                                                                             endCDay.getTime());

                Iterator relIT = relationships.iterator();

                colorCtr = 0;

                while (relIT.hasNext()) {
                    Relationship r = (Relationship) relIT.next();
                    HasRelationships hr = relSched.getOther(r);
                    if (hr instanceof Organization) {
                        Organization o = (Organization) hr;
                        String orgId = o.getItemIdentificationPG().getItemIdentification();
                        String color;
                        if (colorCtr == 0) {
                            color = "red";
                            colorCtr++;
                        } else {
                            color = "blue";
                            colorCtr = 0;
                        }

                        long relStartTime = getBoundedStartTime(r.getStartDate());
                        long relEndTime = getBoundedEndTime(r.getEndDate());

                        data.addRelationship(role.getName(),
                                             ctr++,
                                             relStartTime,
                                             relEndTime,
                                             color,
                                             orgId);
                    }

                }
            }

        }

        protected void getOverlappingRelationshipMap(Organization myOrg,
                                                     RelationshipScheduleData data) {
            RelationshipSchedule relSched = myOrg.getRelationshipSchedule();

            Collection roles = getAllRelationshipRoles(myOrg);

            Iterator roleIT = roles.iterator();

            int ctr = 1;

            while (roleIT.hasNext()) {
                Role role = (Role) roleIT.next();

                /** TODO: do you want to filter out the SUPERIOR/SUBORDINATE Relatioships?
                 if(!((role.getName().equals(Relationships.SUPERIOR)) ||
                 (role.getName().equals(Relationships.SUBORDINATE)))) {

                 **/

                Collection relationships = relSched.getMatchingRelationships(role,
                                                                             startCDay.getTime(),
                                                                             endCDay.getTime());

                Iterator relIT = relationships.iterator();

                boolean colorIsRed = true;

                HashMap orgHash = new HashMap();
                HashMap colorOrgMap = new HashMap();
                int orgCtr = 1;

                while (relIT.hasNext()) {
                    Relationship r = (Relationship) relIT.next();
                    HasRelationships hr = relSched.getOther(r);
                    if (hr instanceof Organization) {
                        Organization o = (Organization) hr;
                        String orgId = o.getItemIdentificationPG().getItemIdentification();
                        String color;


                        long relStartTime = getBoundedStartTime(r.getStartDate());
                        long relEndTime = getBoundedEndTime(r.getEndDate());

                        String roleName = role.getName();
                        if (relationships.size() > 1) {
                            if (orgHash.containsKey(orgId)) {
                                roleName = (String) orgHash.get(orgId);
                                colorIsRed = ((Boolean) colorOrgMap.get(orgId)).booleanValue();
                                colorOrgMap.put(orgId, new Boolean(!colorIsRed));
                            } else {
                                roleName = roleName + orgCtr;
                                orgHash.put(orgId, roleName);
                                colorOrgMap.put(orgId, new Boolean(false));
                                colorIsRed = true;
                                orgCtr++;
                            }
                        }

                        if (colorIsRed) {
                            color = "red";
                        } else {
                            color = "blue";
                        }

                        data.addRelationship(roleName,
                                             ctr++,
                                             relStartTime,
                                             relEndTime,
                                             color,
                                             orgId);
                    }

                }
            }

        }

        protected Organization getMyOrganization(Iterator orgs) {
            Organization myOrg = null;
            // look for this organization
            if (orgs.hasNext()) {
                myOrg = (Organization) orgs.next();
            }
            return myOrg;
        }

        public Organization getMyOrganization() {
            Collection orgsCollection = support.queryBlackboard(orgsPredicate);
            return getMyOrganization(orgsCollection.iterator());
        }


        protected void getStartEndDay() {
            startCDay = null;

            // get oplan

            Collection oplanCollection = support.queryBlackboard(oplanPredicate());

            /*Subscription oplanSubscription =
          psc.getServerPluginSupport().subscribe(this, oplanPredicate());
          Collection oplanCollection =
          ((CollectionSubscription)oplanSubscription).getCollection();
            */

            if (!(oplanCollection.isEmpty())) {
                Iterator iter = oplanCollection.iterator();
                Oplan plan = (Oplan) iter.next();
                //CollectionSubscription collectsub = new CollectionSubscription(oplanPredicate());
                //Oplan plan = (Oplan) collectsub.first();
                //Oplan plan = (Oplan) ((CollectionSubscription)oplanSubscription).first();
                startCDay = plan.getCday();
                endCDay = plan.getEndDay();
                //psc.getServerPluginSupport().unsubscribeForSubscriber(oplanSubscription);
            }

        }


        private static UnaryPredicate oplanPredicate() {
            return new UnaryPredicate() {
                public boolean execute(Object o) {
                    return (o instanceof Oplan);
                }
            };
        }
    }
}




