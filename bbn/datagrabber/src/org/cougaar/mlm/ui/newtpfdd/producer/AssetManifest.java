/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/producer/Attic/AssetManifest.java,v 1.1 2002-05-14 20:41:07 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.producer;


import java.io.Serializable;

import java.util.Vector;


public class AssetManifest implements Serializable
{
    private Vector carrierNames;
    private Vector carrierNameTypes;
    private Vector carrierTypes;
    private Vector cargoNames;
    private Vector cargoNameTypes;
    private Vector cargoTypes;

    public AssetManifest()
    {
	carrierNames = new Vector();
	carrierTypes = new Vector();
	cargoNames = new Vector();
	cargoTypes = new Vector();
	carrierNameTypes = new Vector();
	cargoNameTypes = new Vector();
    }

    public void addCarrierName(String carrierName, String type)
    {
	if ( !carrierNames.contains(carrierName) ) {
	    carrierNames.add(carrierName);
	    carrierNameTypes.add(type);
	}
    }

    public void addCarrierType(String carrierType)
    {
	if ( !carrierTypes.contains(carrierType) )
	    carrierTypes.add(carrierType);
    }

    public void addCargoName(String cargoName, String type)
    {
	if ( !cargoNames.contains(cargoName) ) {
	    cargoNames.add(cargoName);
	    cargoNameTypes.add(type);
	}
    }

    public void addCargoType(String cargoType)
    {
	if ( !cargoTypes.contains(cargoType) )
	    cargoTypes.add(cargoType);
    }

    public Vector getCarrierNames()
    {
	return carrierNames;
    }

    public Vector getCarrierNameTypes()
    {
	return carrierNameTypes;
    }

    public Vector getCarrierTypes()
    {
	return carrierTypes;
    }

    public Vector getCargoNames()
    {
	return cargoNames;
    }

    public Vector getCargoTypes()
    {
	return cargoTypes;
    }

    public Vector getCargoNameTypes()
    {
	return cargoNameTypes;
    }
}
