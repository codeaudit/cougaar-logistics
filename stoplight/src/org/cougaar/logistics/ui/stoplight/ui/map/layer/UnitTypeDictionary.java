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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;

import java.util.*;
import java.io.*;


public class UnitTypeDictionary {
    public UnitTypeDictionary() {
	initializeDefaults();
    }
    public void reset() {
	dictionary=new Properties();
    }
    public void reset(InputStream input) throws IOException {
	reset();
	load(input);
    }
    public void load(InputStream input) throws IOException {
	dictionary.load(input);
    }
    public String getUnitType(String unitName) { 
	String type = dictionary.getProperty(unitName);
	if (type==null) { 
	    type="unknown"; 
	}
	return type;
    }
    Properties dictionary=new Properties();
    
    private void initializeDefaults() {
	dictionary.setProperty("23INBN","infantry");
	dictionary.setProperty("30INBN","infantry");
	dictionary.setProperty("31INBN","infantry");
	dictionary.setProperty("3-7-INBN","infantry");
	dictionary.setProperty("3-69-ARBN","armored");
	dictionary.setProperty("3ID","other");
	dictionary.setProperty("1BDE","other");
    }
    
}
