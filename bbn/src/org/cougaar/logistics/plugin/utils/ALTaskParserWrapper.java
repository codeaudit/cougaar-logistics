// 
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

package org.cougaar.logistics.plugin.utils;

import java.util.Date;
import java.util.Vector;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.xml.parser.TaskParser;
import org.cougaar.glm.parser.GLMTaskParser;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.lib.xml.parser.DateParser;
import org.cougaar.lib.xml.parser.DirectObjectParser;
import org.cougaar.lib.xml.parser.PreferencesParser;
import org.cougaar.lib.xml.parser.VerbParser;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.util.log.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Understands "maintaining" tag in xml task input file.
 *
 * An example would be:
 *
 *   <maintaining>
 *     <class>class org.cougaar.logistics.plugin.inventory.MaintainedItem</class>
 *     <itemID>FCS-C2V-0.MCG1.1-UA.ARMY.MIL</itemID>
 *     <type>Asset</type>
 *     <nomen>Command and Control Vehicle</nomen>
 *     <typeID>NSN/FCS-C2V</typeID>
 *   </maintaining>
 *
 * 
 * Copyright (c) 1999-2002 BBN Technologies 
 */
public class ALTaskParserWrapper extends GLMTaskParser {

  protected TaskParser makeTaskParser () { 
    return new ALTaskParser (logger);
  }

  public class ALTaskParser extends TaskParser {
    public ALTaskParser (Logger logger) { super (logger); }

    protected boolean handleTag (NewTask task, Vector prep_phrases,
                                 LDMServesPlugin ldm,
                                 PlanningFactory ldmf,
                                 Node child) {
      boolean didSomethingWithTag = false;
      String childname = child.getNodeName();
      if (childname.equals("maintaining")){
        // same as with
        NewPrepositionalPhrase newpp = ldmf.newPrepositionalPhrase();
        newpp.setPreposition(Constants.Preposition.MAINTAINING);
        newpp.setIndirectObject(getMaintainingObject(ldm, child));
        prep_phrases.addElement(newpp);
        didSomethingWithTag = true;
      } 
      else {
        return super.handleTag (task, prep_phrases, ldm, ldmf, child);
      }

      return didSomethingWithTag;
    }

    protected Object getMaintainingObject(LDMServesPlugin ldm, Node node){
      String type = null, typeID = null, itemID = null, nomen = null;

      NodeList  nlist    = node.getChildNodes();      
      int       nlength  = nlist.getLength();

      for(int i = 0; i < nlength; i++){
        Node    child       = nlist.item(i);
        String  childname   = child.getNodeName();
        if(child.getNodeType() == Node.ELEMENT_NODE){
          if(childname.equals("type"))
            type = getTagContents (child);
          else if (childname.equals("typeID"))
            typeID = getTagContents (child);
          else if (childname.equals("nomen"))
            nomen = getTagContents (child);
          else if (childname.equals("itemID"))
            itemID = getTagContents (child);
        }
      }

      return new MaintainedItem (type, typeID, itemID, nomen, null);
    }
  }
}
