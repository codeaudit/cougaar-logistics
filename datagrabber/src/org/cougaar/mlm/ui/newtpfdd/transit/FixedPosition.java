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
package org.cougaar.mlm.ui.newtpfdd.transit;
import java.io.Serializable;

/** Position of an object at a specific geoloc/lat/lon
 * @author Benjamin Lubin; last modified by $Author: mthome $
 *
 * @since 11/14/00
 */
public class FixedPosition implements Position{
  protected String name;
  protected float lat;
  protected float lon;
  
  public FixedPosition(String name, double lat, double lon){
    if(name != null) {
      this.name=name.intern();
    } else {
      this.name=null;
    }
    this.lat=(float)lat;
    this.lon=(float)lon;
  }
  public FixedPosition(String name, float lat, float lon){
    if(name != null) {
      this.name=name.intern();
    } else {
      this.name=null;
    }
    this.lat=lat;
    this.lon=lon;
  }
  
  public String getName(){return name;}
  public float getLat(){return lat;}
  public float getLon(){return lon;}
  
  public int hashCode(){
    if(name==null)
      return super.hashCode();
    return name.hashCode();
  }
  
  public boolean equals(Object o){
    return (o instanceof FixedPosition &&
	    name.equals( ((FixedPosition)o).name));
  }
  public String toString(){
    return name + "("+lat+","+lon+")";
  }
}
