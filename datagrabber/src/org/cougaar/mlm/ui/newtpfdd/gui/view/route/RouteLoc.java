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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A location on the route
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/19/01
 **/
public class RouteLoc{
  //Constants:
  ////////////

  //Variables:
  ////////////
  private String dbUID;
  private String geoloc;
  private String prettyName;
  private float lat;
  private float lon;
  
  //Constructors:
  ///////////////
  public RouteLoc(String dbUID, String geoloc, String prettyName, float lat, float lon){
    this.dbUID=dbUID;
    this.geoloc=geoloc;
    this.prettyName=prettyName;
    this.lat=lat;
    this.lon=lon;
  }
  
  //Members:
  //////////
  public String getDBUID(){return dbUID;}
  public String getGeoLoc(){return geoloc;}
  public String getPrettyName(){return prettyName;}
  public float getLat(){return lat;}
  public float getLon(){return lon;}
  public int hashCode(){
    if (geoloc != null) {
      return geoloc.hashCode();
    } else {
      return super.hashCode ();
    }
  }
  public boolean equals(Object o){
    if(this==o)
      return true;
    if(o instanceof RouteLoc){
      if(dbUID!=null&&((RouteLoc)o).dbUID!=null)
	return dbUID.equals(((RouteLoc)o).dbUID);
    }
    return false;
  }
}
