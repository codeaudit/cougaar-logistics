/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/producer/Attic/PSPClientConfig.java,v 1.1 2002-05-14 20:41:07 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

/*
  All the static info for the XML user interface is here, 
  so it can be easily configured.
*/


package org.cougaar.mlm.ui.newtpfdd.producer;


public class PSPClientConfig
{
    // the package and id are used in the URL specified by the client
    // to connect to the PSP

    public static final String PSP_package = "alpine/demo";

    public static final String PSP_id = "DEBUG.PSP";

    public static final String UIDataPSP_id = "UIDATA.PSP";

    public static final String ItineraryPSP_id = "ITINERARY.PSP";

    public static final String SubordinatesPSP_id = "SUBORDINATES.PSP";

    public static final String AssetPopulationPSP_id = "ASSET_POPULATION.PSP";

    public static final String HierarchyPSP_id = "HIERARCHY.PSP";
}
