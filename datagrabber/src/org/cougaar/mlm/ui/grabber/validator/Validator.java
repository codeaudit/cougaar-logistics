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
package org.cougaar.mlm.ui.grabber.validator;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.DBConnectionProvider;
import org.cougaar.mlm.ui.grabber.controller.FailureRunResult;

import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.workqueue.ResultHandler;
import org.cougaar.mlm.ui.grabber.workqueue.Work;
import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all the tests and knows how to kick them off
 *
 * @since 2/27/01
 **/
public class Validator{

  //Constants:
  ////////////

  public static final int ALL_TESTS=0;
  public static final int CORE_TESTS=-1;
  public static final int INFO_TESTS=-2;
  public static final int WARNING_TESTS=-3;
  public static final int ERROR_TESTS=-4;

  public static final int MIN_TEST_CATEGORY=ERROR_TESTS;

  protected Test[] tests;
  private WorkQueue workQ;

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public Validator(DBConfig dbConfig){
    ArrayList t=new ArrayList();

    /*empty owner*/
    t.add(new EmptyOwnerTest(dbConfig));
    /*Test types */
    t.add(new BadConveyanceTypeTest(dbConfig));
    t.add(new BadLegTypeTest(dbConfig));
    t.add(new BadAssetTypeTest(dbConfig));
    t.add(new BadAssetClassTest(dbConfig));
    t.add(new MissingPeopleTest(dbConfig));
    t.add(new MissingCargoTest(dbConfig));
    t.add(new TotalPeopleTest(dbConfig,true));
    t.add(new UnitCargoAmountTest(dbConfig,true));
    t.add(new TotalPeopleTest(dbConfig,false));
    t.add(new UnitCargoAmountTest(dbConfig,false));
    t.add(new UnitCargoByClassTest(dbConfig,false, false));
    t.add(new UnitCargoByClassTest(dbConfig,false, true));
    t.add(new UnitCargoByClassTest(dbConfig,true,  false));
    t.add(new UnitCargoByClassTest(dbConfig,true,  true));
    t.add(new CargoDimensionTest(dbConfig));
    t.add(new CargoDensityTest(dbConfig));
    t.add(new CargoSizeTest(dbConfig, CargoSizeTest.BIGGER_THAN_MILVAN));
    t.add(new CargoSizeTest(dbConfig, CargoSizeTest.BIGGER_THAN_C5)); 
    t.add(new UnitMilvansTest(dbConfig,false));
    t.add(new UnitMilvansTest(dbConfig,true));
    t.add(new UnitPalletTest(dbConfig,false));
    t.add(new UnitPalletTest(dbConfig,true));
    t.add(new UnitCargoBySeaOrAirTest(dbConfig,false));
    t.add(new UnitCargoBySeaOrAirTest(dbConfig,true));
    t.add(new AssetOriginationTest(dbConfig,true));
    t.add(new AssetOriginationTest(dbConfig,false));
    t.add(new RouteEndpointTest(dbConfig));
    t.add(new RepeatNomenclatureTest(dbConfig));
    t.add(new TonnageInfo(dbConfig,false,false,false,false));
    t.add(new TonnageInfo(dbConfig,false,true,false,false));
    t.add(new TonnageInfo(dbConfig,false,true,true,false));
    //    t.add(new TonnageInfo(dbConfig,false,true,true,true));
    t.add(new TonnageInfo(dbConfig,true,false,false,false));
    t.add(new TonnageInfo(dbConfig,true,true,false,false));
    t.add(new TonnageInfo(dbConfig,true,true,true,false));
    t.add(new ManifestInfo(dbConfig));
    //    t.add(new TonnageInfo(dbConfig,true,true,true,true));
    //    t.add(new TonnageInfo(dbConfig,false,false,false,true));
    //    t.add(new TonnageInfo(dbConfig,false,true,false,true));

    /*
    t.add(new AvgTonnageInfo(dbConfig,false,false,false,false));
    t.add(new AvgTonnageInfo(dbConfig,false,true,false,false));
    t.add(new AvgTonnageInfo(dbConfig,false,true,true,false));
    t.add(new AvgTonnageInfo(dbConfig,false,true,true,true));
    t.add(new AvgTonnageInfo(dbConfig,true,false,false,false));
    t.add(new AvgTonnageInfo(dbConfig,true,true,false,false));
    t.add(new AvgTonnageInfo(dbConfig,true,true,true,false));
    t.add(new AvgTonnageInfo(dbConfig,true,true,true,true));
    t.add(new AvgTonnageInfo(dbConfig,false,false,false,true));
    t.add(new AvgTonnageInfo(dbConfig,false,true,false,true));
    */
    t.add(new PeopleInfo(dbConfig,true));
    t.add(new MissionInfo(dbConfig,true));
    /*Test Missing references:*/
    /*
      MissingReferenceTest(DBConfig dbConfig,
      String baseName,
      int failureLevel,
      String tableContain,
      String columnContain,
      String tableMissing,
      String columnMissing,
      String header,
      int type,
      String description)
    */    
    t.add(new MissingReferenceTest(dbConfig,
				   "m_convoyid",
				   Test.RESULT_ERROR,
				   "convoymember",
				   "convoyid",
				   "convoys",
				   "convoyid",
				   "Convoy ID",
				   Test.TYPE_CONVOY,
				   "Missing convoy references in convoy members"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_convoymemberid",
				   Test.RESULT_ERROR,
				   "convoymember",
				   "convid",
				   "conveyanceinstance",
				   "convid",
				   "Convoy Member ID",
				   Test.TYPE_CONV,
				   "Missing conveyance id references in convoy members"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_routeinfoid",
				   Test.RESULT_ERROR,
				   "conveyedleg",
				   "routeid",
				   "route_elements",
				   "routeid",
				   "Route ID",
				   Test.TYPE_ROUTE,
				   "Missing route id references in legs",
				   "and contain.routeid <> null"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_assetitin",
				   Test.RESULT_ERROR,
				   "assetitinerary",
				   "assetid",
				   "assetinstance",
				   "assetid",
				   "Asset ID",
				   Test.TYPE_ASSET,
				   "Missing asset references in itinerary"));
    
    t.add(new MissingReferenceTest(dbConfig,
				   "m_legitin",
				   Test.RESULT_ERROR,
				   "assetitinerary",
				   "legid",
				   "conveyedleg",
				   "legid",
				   "Leg ID",
				   Test.TYPE_LEG,
				   "Missing leg references in itinerary"));
    
    t.add(new MissingReferenceTest(dbConfig,
				   "m_slocleg",
				   Test.RESULT_ERROR,
				   "conveyedleg",
				   "startlocid",
				   "locations",
				   "locid",
				   "Loc ID",
				   Test.TYPE_LOC,
				   "Missing startLoc references in leg"));
    
    t.add(new MissingReferenceTest(dbConfig,
				   "m_elocleg",
				   Test.RESULT_ERROR,
				   "conveyedleg",
				   "endlocid",
				   "locations",
				   "locid",
				   "Loc ID",
				   Test.TYPE_LOC,
				   "Missing endLoc references in leg"));
        
    t.add(new MissingReferenceTest(dbConfig,
				   "m_slocroute",
				   Test.RESULT_ERROR,
				   "route_elements",
				   "startlocid",
				   "locations",
				   "locid",
				   "Loc ID",
				   Test.TYPE_LOC,
				   "Missing startLoc references in route"));
    
    t.add(new MissingReferenceTest(dbConfig,
				   "m_elocroute",
				   Test.RESULT_ERROR,
				   "route_elements",
				   "endlocid",
				   "locations",
				   "locid",
				   "Loc ID",
				   Test.TYPE_LOC,
				   "Missing endLoc references in route"));
    
    t.add(new MissingReferenceTest(dbConfig,
				   "m_convleg",
				   Test.RESULT_ERROR,
				   "conveyedleg",
				   "convid",
				   "conveyanceinstance",
				   "convid",
				   "Conveyance ID",
				   Test.TYPE_CONV,
				   "Missing conveyance references in leg"));
        
    t.add(new MissingReferenceTest(dbConfig,
				   "m_owninst",
				   Test.RESULT_ERROR,
				   "assetinstance",
				   "ownerid",
				   "org",
				   "related_id",
				   "Owner ID",
				   Test.TYPE_ORG,
				   "Missing owner references in instance",
				   "and missing.relation_type=0"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_protoinst",
				   Test.RESULT_ERROR,
				   "assetinstance",
				   "prototypeid",
				   "assetprototype",
				   "prototypeid",
				   "Prototype ID",
				   Test.TYPE_ASSET,
				   "Missing prototype references in instance"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_parentproto",
				   Test.RESULT_ERROR,
				   "assetprototype",
				   "parentprototypeid",
				   "assetprototype",
				   "prototypeid",
				   "Parent Prototype ID",
				   Test.TYPE_ASSET_PROTO,
				   "Missing parent prototype "+
				   "references in prototype",
				   " and contain.parentprototypeid "+
				   "is not null"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_homeconv",
				   Test.RESULT_WARNING,
				   "conveyanceinstance",
				   "baselocid",
				   "locations",
				   "locid",
				   "Location ID",
				   Test.TYPE_LOC,
				   "Missing homeLoc references in conveyance"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_ownerconv",
				   Test.RESULT_WARNING,
				   "conveyanceinstance",
				   "ownerid",
				   "org",
				   "related_id",
				   "Owner ID",
				   Test.TYPE_LOC,
				   "Missing owner references in conveyance"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_convprotoconv",
				   Test.RESULT_ERROR,
				   "conveyanceinstance",
				   "prototypeid",
				   "conveyanceprototype",
				   "prototypeid",
				   "Conveyance PrototypeID",
				   Test.TYPE_CONV_PROTO,
				   "Missing conveyance prototype references "+
				   "in conveyance"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_orgname",
				   Test.RESULT_ERROR,
				   "org",
				   "org_id",
				   "orgnames",
				   "org_id",
				   "Org ID",
				   Test.TYPE_ORG,
				   "Missing org name references in org table"));

    t.add(new MissingReferenceTest(dbConfig,
				   "m_orgroot",
				   Test.RESULT_WARNING,
				   "orgroots",
				   "org_id",
				   "org",
				   "org_id",
				   "Org ID",
				   Test.TYPE_ORG,
				   "Missing root org references in org table "+
				   "(Expected Condition if and only if the "+
				   "root has no descendents)"));
        
    /* MISC*/
    t.add(new DisconnectedHierarchyTest(dbConfig));
    t.add(new StartEndTimeTest(dbConfig));
    // t.add(new ArrivalTimeTest(dbConfig));
    t.add(new ArrivalTimePrecisionTest(dbConfig));
    t.add(new UnitArrivalTimePrecisionTest(dbConfig));
    t.add(new OverlappingLegsTest(dbConfig));
    // t.add(new MissingLegTest(dbConfig,MissingLegTest.BY_CARRIER));
    t.add(new MissingLegTest(dbConfig,MissingLegTest.BY_ASSET));
    tests=(Test[])t.toArray(new Test[0]);
  }

  //Members:
  //////////

  public Test getTest(int test){
    return tests[test-1];
  }

  public int getNumTests(){
    return tests.length;
  }

  public String getDescription(int test){
    return getTest(test).getDescription();
  }

  public int getTestResult(Logger l, Statement s, int run, int test){
    if (isTestCategory(test))
	return Test.RESULT_OK;
	
    return getTest(test).getResult(l, s, run);
  }

  public String getTestResultString(Logger l, Statement s, int run, int test){
    return Test.RESULTS[getTestResult(l, s,run,test)];
  }

  public int getTestPriority(int test){
    return getTest(test).getPriorityLevel();
  }

  public String getTestPriorityString(int test){
    return Test.PRIORITIES[getTestPriority(test)];
  }

  public String getTestTypeString(int test){
    switch(test) {
    case ALL_TESTS:
      return "All Tests";
    case CORE_TESTS:
      return "Core Tests";
    case INFO_TESTS:
      return "Information Tests";
    case WARNING_TESTS:
      return "Warning Tests";
    case ERROR_TESTS:
      return "Error Tests";
    }

    return getDescription(test);
  }

  public boolean isTestCategory(int test){
    return (test <= ALL_TESTS);
  }

  /*
   * Return a list, containing all of the indices of Tests
   * for a given test type.  
   **/
  public List getTestIndicesForTestType(int testtype){
    switch (testtype) {
    case ALL_TESTS:
      return getTestIndicesForAllTests();
    case CORE_TESTS:
      return getTestIndicesForPriorityLevel(Test.PRIORITY_CORE);
    case INFO_TESTS:
      return getTestIndicesForFailureLevel(Test.RESULT_INFO);
    case WARNING_TESTS:
      return getTestIndicesForFailureLevel(Test.RESULT_WARNING);
    case ERROR_TESTS:
      return getTestIndicesForFailureLevel(Test.RESULT_ERROR);
    }
    List l = new ArrayList();
    l.add(new Integer(testtype));

    return l;    
  }

  /*
   * Return a list, containing the indices of all Tests
   **/
  public List getTestIndicesForAllTests(){
    List ret=new ArrayList();
    for(int i=1;i<=getNumTests();i++){
      ret.add(new Integer(i));
    }
    return ret;
  }

  /*
   * Return a list, containing all of the indices of Tests
   * that produce a given failure level.
   **/
  public List getTestIndicesForFailureLevel(int level){
    List ret=new ArrayList();
    for(int i=1;i<=getNumTests();i++){
      if(getTest(i).failureLevel()==level)
	ret.add(new Integer(i));
    }
    return ret;
  }

  /*
   * Return a list, containing all of the indices of Tests
   * that produce a given priority level.
   **/
  public List getTestIndicesForPriorityLevel(int level){
    List ret=new ArrayList();
    for(int i=1;i<=getNumTests();i++){
      if(getTest(i).getPriorityLevel()==level)
	ret.add(new Integer(i));
    }
    return ret;
  }

  /** test has a ONE based index, 0 is reserved for 'all tests'**/
  public void runTest(final HTMLizer h, 
		      DBConnectionProvider connectionProvider, 
		      int run, int test){
    // keep track of whether this category has been run
    try {
      Statement s = connectionProvider.getDBConnection ().createStatement();
      if (isTestCategory(test)) {
	ResultTable.updateStatus(h,s,this,run,test);
      }
      s.close();

      ValidatorResultHandler resultHandler = new ValidatorResultHandler (h);
      workQ = new WorkQueue (h, resultHandler);
      resultHandler.setWorkQ (workQ);

      List l = getTestIndicesForTestType(test);
      for(int i=0;i<l.size();i++){
	int idx=((Integer)l.get(i)).intValue();
	Work work = new TestWork (100+i, connectionProvider.getDBConnection(), 
				  idx, getDescription (idx), h, run, this);
	if (h.isMinorEnabled()) {
	  h.logMessage(Logger.MINOR,Logger.GENERIC, "Queueing work (" + work + ")");
	}
	workQ.enque (work);
      }

      while (workQ.isBusy ()) {
	synchronized (this) {
	  try {wait(5000);} catch (Exception e) {e.printStackTrace();}
	}
      }
    }
    catch (SQLException sqle) {
      h.logMessage (Logger.WARNING, Logger.GENERIC, "Got SQL error : " + sqle);
      sqle.printStackTrace();
    }

    h.p("<BR><B>Finished Validating</B>");
  }

  class ValidatorResultHandler implements ResultHandler {
    WorkQueue workQ;
    HTMLizer htmlizer;

    public ValidatorResultHandler (HTMLizer htmlizer) {
      this.htmlizer = htmlizer;
    }
    
    public void setWorkQ (WorkQueue workQ) {
      this.workQ = workQ;
    }

    public void handleResult(Result r) {
      if (htmlizer.isTrivialEnabled ()) {
	htmlizer.logMessage (Logger.TRIVIAL, Logger.GENERIC, "Returned " + r);
      }
      if (htmlizer.isMinorEnabled ()) {
	htmlizer.logMessage (Logger.MINOR, Logger.GENERIC, 
		      "Now " + workQ.getNumActiveWork () + 
		      " active tests, " + workQ.getNumWorkWaitingOnQueue () + 
		      " waiting to be worked on.");
      }
      if (r instanceof FailureRunResult) {
	if (htmlizer.isWarningEnabled ()) {
	  htmlizer.logMessage (Logger.WARNING, Logger.GENERIC, "Stopping all work because of failure : " + r);
	}
	workQ.haltAllWork (); // stop on any failure
      }
    }      
  }

  public class TestWork implements Work {
    int id;
    boolean halt = false;
    Connection connection;
    String testName;
    int testIndex;
    HTMLizer htmlizer;
    int run;
    Validator validator;

    public TestWork (int id, Connection connection, 
		     int testIndex, String testName, 
		     HTMLizer htmlizer, int run,
		     Validator validator) {
      this.id = id; 
      this.connection = connection;
      this.testName = testName;
      this.testIndex = testIndex;
      this.htmlizer = htmlizer;
      this.run = run;
      this.validator = validator;

      if (htmlizer.isMinorEnabled()) {
	htmlizer.logMessage (Logger.MINOR, Logger.GENERIC, 
			     "Using connection " + connection + 
			     " for test " + this);
      }
    }

    public int getID() { return id; }
    public String getStatus() { return "doing test " + this; }
    public void halt() { halt = true;}
    public Result perform(Logger l) { 
      Statement statement = null;
      Result result = null;
      try {
	if (doTest (statement = connection.createStatement())) {
	  result = new Result () { 
	      public int getID () { return id; }
	      public String toString () { return "Successful run of test \"" + testName + "\" on run #" + run; }
	    };
	} else {
	  result = new FailureRunResult (id, run, "failed running test " + this, false);
	}
      } catch (SQLException sqle){
	htmlizer.logMessage (Logger.ERROR, Logger.GENERIC, "Got sql exception " + sqle);
	sqle.printStackTrace ();
	result = new FailureRunResult (id, run, "sql error - failed running test " + this, false);
      } finally {
	if (statement != null) {
	  try { statement.close(); } catch (Exception e) {}
	}
      }

      return result;
    }

    protected boolean doTest (Statement statement) {
      try {
	if (!halt) {
	  getTest(testIndex).prepare(htmlizer,statement,run);
	}
      } catch (Exception e) {
	htmlizer.logMessage(Logger.WARNING, Logger.GENERIC,
			    "Problem running test - could not prepare test "+testName +
			    ". Exception was " + e);
	e.printStackTrace();
	return false;
      }
      try {
	synchronized (validator) { // don't update tables at same time
	  if (!halt) {
	    ResultTable.updateStatus(htmlizer,statement,validator,run,testIndex);
	  }
	}
      } catch (RuntimeException e) {
	htmlizer.logMessage(Logger.WARNING, Logger.GENERIC,
			    "Problem running test - could not update status for test "+testName);
	return false;
      }
      return true;
    }

    public String toString () { return "Test Work : " + testName;  }
  }

  /** test has a ONE based index, 0 is reserved for 'all tests'**/
  public void displayTest(Statement s, HTMLizer h, int run, int test){
    List l = getTestIndicesForTestType(test);

    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();
      getTest(idx).produceHTML(s,run,h);
    }
  }
  
  /** test has a ONE based index, 0 is reserved for 'all tests'**/
  public void clearTest(Statement s, HTMLizer h, int run, int test){
    // keep track of whether this category has been run
    if (isTestCategory(test))
      ResultTable.removeTest(h,s,run,test);

    List l = getTestIndicesForTestType(test);
    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();
      getTest(idx).dropTable(h,s,run);
      ResultTable.removeTest(h,s,run,idx);
    }
    h.p("<BR><B>Cleared</B>");
  }

  //InnerClasses:
  ///////////////
}




