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
package org.cougaar.logistics.ui.stoplight.util;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.cougaar.lib.uiframework.transducer.*;
import org.cougaar.lib.uiframework.transducer.elements.*;

public class OrgXMLGenerator
{
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: java mil.darpa.log.alpine.blackjack." +
                               "assessui.util.OrgXMLGenerator <config zip>");
            return;
        }

        try
        {
            // create vector of org descriptors
            Vector orgs = new Vector();

            ZipInputStream zis = new ZipInputStream(
                new FileInputStream(args[0]));

            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null)
            {
                if (ze.getName().endsWith("-relationships.ini"))
                {
                    String thisOrg = ze.getName();
                    int start = thisOrg.lastIndexOf('/') + 1;
                    int end = thisOrg.indexOf("-relationships.ini");
                    thisOrg = thisOrg.substring(start, end);
                    String supOrg = null;

                    byte[] l = new byte[80];
                    zis.read(l);
                    String line = new String(l);
                    if (line.startsWith("Superior"))
                    {
                        int qi = line.indexOf('"') + 1;
                        supOrg = line.substring(qi, line.indexOf('"', qi));
                    }

                    if ((supOrg == null ) || supOrg.equals(""))
                        supOrg = "All Units";
                    orgs.add(new OrgDesc(thisOrg, supOrg));
                }
            }

            /*
            File directory = new File("d:\\Alpine\\bj-config-12");
            File[] relFiles = directory.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith("-relationships.ini");
                    }
                });

            for (int i = 0; i < relFiles.length; i++)
            {
                String thisOrg = relFiles[i].getName();
                int start = thisOrg.lastIndexOf('\\') + 1;
                int end = thisOrg.indexOf("-relationships.ini");
                thisOrg = thisOrg.substring(start, end);
                String supOrg = null;
                BufferedReader br =
                    new BufferedReader(new FileReader(relFiles[i]));
                while (br.ready())
                {
                    String line = br.readLine();
                    if (line.startsWith("Superior"))
                    {
                        int qi = line.indexOf('"') + 1;
                        supOrg = line.substring(qi, line.indexOf('"', qi));
                        break;
                    }
                }

                if ((supOrg == null ) || supOrg.equals(""))
                    supOrg = "All Units";
                orgs.add(new OrgDesc(thisOrg, supOrg));
            }
            */

            // Create org structure
            Structure orgStruc = new Structure();
            orgStruc.addAttribute(new Attribute("hierarchy", "Organization"));
            ListElement root = new ListElement();
            orgStruc.addChild(root);
            root.addAttribute(new Attribute("UID", "All Units"));

            while (orgs.size() > 0)
            {
                for (int i=0; i < orgs.size(); i++)
                {
                    OrgDesc od = (OrgDesc)orgs.elementAt(i);
                    boolean inserted = insertOrgIfPossible(od, root);
                    if (inserted)
                    {
                        orgs.remove(od);
                        i--;
                    }
                }
            }

            // Save structure to xml file
            XmlInterpreter xint = new XmlInterpreter();
            FileOutputStream fout = new FileOutputStream("orgTree.xml");
            xint.writeXml(orgStruc, fout);
            fout.close();
            System.out.println("\n** orgTree.xml successfully generated **");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static boolean insertOrgIfPossible(OrgDesc od, ListElement le)
    {
        Attribute a = le.getAttribute("UID");
        Enumeration vs = a.getChildren();
        String parentUID = ((Element)vs.nextElement()).getAsValue().getValue();

        if (parentUID.equals(od.parent))
        {
            ListElement newListElement = new ListElement();
            newListElement.addAttribute(new Attribute("UID", od.name));
            le.addChild(newListElement);
            return true;
        }

        Enumeration children = le.getChildren();
        while(children.hasMoreElements())
        {
            ListElement child = (ListElement)children.nextElement();
            if (insertOrgIfPossible(od, child))
            {
                return true;
            }
        }

        return false;
    }

    private static class OrgDesc
    {
        public String name;
        public String parent;

        public OrgDesc(String name, String parent)
        {
            this.name = name;
            this.parent = parent;
        }

        public String toString()
        {
            return parent + "->" + name;
        }
    }
}