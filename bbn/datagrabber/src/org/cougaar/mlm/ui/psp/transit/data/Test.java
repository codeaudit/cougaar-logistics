/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.psp.transit.data;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.planning.servlet.data.hierarchy.*;
import org.cougaar.mlm.ui.psp.transit.data.population.*;
import org.cougaar.mlm.ui.psp.transit.data.prototypes.*;
import org.cougaar.mlm.ui.psp.transit.data.instances.*;
import org.cougaar.mlm.ui.psp.transit.data.legs.*;
import org.cougaar.mlm.ui.psp.transit.data.locations.*;
import org.cougaar.mlm.ui.psp.transit.data.registration.*;

import org.xml.sax.SAXException;

import java.io.Writer;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.OutputStreamWriter;

import java.util.List;

/**
 * Test the PSP data XMLization
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/24/01
 **/
public class Test{

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  //Members:
  //////////

  public static void testHierarchy() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      HierarchyData hd1 = new HierarchyData();
      hd1.setRootOrgID("3ID");
      Organization od1A = 
	new Organization();
      od1A.setUID("3ID");
      od1A.setPrettyName("3rd Infantry Division");
      od1A.addRelation("6ALFSQ",
		       Organization.ADMIN_SUBORDINATE);
      hd1.addOrgData(od1A);

      hd1.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new HierarchyDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void testPopulation() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      PopulationData pd1 = new PopulationData();

      ConveyancePrototype cp1 = new ConveyancePrototype();
      cp1.UID="UID1";
      cp1.conveyanceType=ConveyancePrototype.ASSET_TYPE_SHIP;
      cp1.volCap=1000;
      cp1.weightCap=2000;
      cp1.aveSpeed=2;
      cp1.alpTypeID="RORO";
      cp1.nomenclature="Boat";
      pd1.addPrototype(cp1);

      ConveyanceInstance ci1 = new ConveyanceInstance();
      ci1.UID="UID2";
      ci1.prototypeUID="UID1";
      ci1.bumperNo="B1";
      ci1.itemNomen="ItNom";
      ci1.homeLocID="BOSP";
      ci1.ownerID="STSHIP";
      ci1.selfPropelled=true;

      pd1.addInstance(ci1);
      
      pd1.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new PopulationDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void testPrototypes() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      PrototypesData pd1 = new PrototypesData();
      Prototype p1 = new Prototype();
      p1.UID="UID3";
      p1.assetClass=Prototype.ASSET_CLASS_1;
      p1.assetType=Prototype.ASSET_TYPE_ASSET;
      p1.weight=2;
      p1.width=3;
      p1.height=4;
      p1.depth=5;
      p1.alpTypeID="NSN/asdf";
      p1.nomenclature="Box of nails";
      pd1.addPrototype(p1);

      pd1.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new PrototypesDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void testInstances() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      InstancesData id1 = new InstancesData();
      Instance i1 = new Instance();
      i1.UID="UID1";
      i1.itemNomen="INomen";
      i1.aggregateNumber=4;
      i1.prototypeUID="UID2";
      i1.ownerID="3ID";
      id1.addInstance(i1);

      id1.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new InstancesDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void testLegs() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      LegsData ld1 = new LegsData();
      Leg l1 = new Leg();
      l1.UID="UID1";
      l1.startTime=123121;
      l1.endTime=2134123;
      l1.startLoc="gsds";
      l1.endLoc="fdss";
      l1.legType=Leg.LEG_TYPE_TRANSPORTING;
      l1.conveyanceUID="UID35463";
      l1.addCarriedAsset("UID234");

      ld1.addLeg(l1);

      ld1.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new LegsDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void testLocations() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      LocationsData ld1 = new LocationsData();
      Location l1 = new Location();
      l1.UID="UID456";
      l1.lat=3.34;
      l1.lon=5.43;
      l1.geoLoc="sfsd";
      l1.icao="3453534";
      l1.prettyName="Boston";
      ld1.addLocation(l1);

      ld1.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new LocationsDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void testRegistration() throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      Registration r = new Registration("Session1", 23542342);

      r.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new RegistrationFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  private static void testHierarchyError()throws SAXException, IOException{
      XMLWriter stdOut = new XMLWriter(new BufferedWriter
	(new OutputStreamWriter(System.out)), true);

      StringWriter strOut = new StringWriter();
      XMLWriter out = new XMLWriter(strOut);
      out.writeHeader();
      
      Failure f = new Failure(new Exception("Hierarchy Failure"));

      f.toXML(out);
      out.flush();
      out.close();

      /*
      stdOut.write("\nObj->XML:\n");
      stdOut.write(strOut.getBuffer().toString());
      stdOut.flush();
      */

      DeXMLizer dx = new DeXMLizer(new HierarchyDataFactory());

      Reader strIn = new StringReader(strOut.getBuffer().toString());
      DeXMLable obj = dx.parseObject(strIn);

      stdOut.write("\nObj->XML->OBJ->XML:\n");
      
      ((XMLable)obj).toXML(stdOut);

      stdOut.flush();
  }

  public static void main(String s[]) {
    try{
      testHierarchy();
      testPopulation();
      testPrototypes();
      testInstances();
      testLegs();
      testLocations();
      testRegistration();
      testHierarchyError();
    }catch(Exception e){
      System.err.println(e);
    }
  }
}
