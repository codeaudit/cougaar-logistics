Database = ${icis.database}
Username = ${icis.database.user}
Password = ${icis.database.password}
MIN_IN_POOL= 1
MAX_IN_POOL= 4
TIMEOUT= 1
NUMBER_OF_TRIES= 2

headerQuery=select commodity, nsn, nomenclature, ui, ssc, price, icc, alt, plt, pcm, boq, diq, iaq, nso, qfd, rop, owrmrp, weight, cube, aac, slq from header where NSN = :nsn
assetsQuery=select nsn, ric, purpose, condition, iaq from assets where NSN = :nsn
nomen=select nomenclature from header where NSN = :nsn	
cost=select price from header where NSN = :nsn
volume=select cube from header where NSN = :nsn
weight=select weight from header where NSN = :nsn
classIXData=select nomenclature, ui, price, cube, weight from header where NSN = :nsn 
classIIIPackagedData=select nomenclature, ui, price, cube, weight from header where NSN = :nsn 
classVData=select nomenclature, weight, ccc from ammo_characteristics where DODIC = :nsn
ui=select ui from header where NSN = :nsn
# MEI
#
meiQuery=select NOMENCLATURE from AGGREGATED_MEI_NOMENCLATURE where MEI = :nsn and SERVICE = :service
# ARMY
#
ConsumableArmyNSN=select MEI_NSN, PART_NSN, OPTEMPO, DCR from ARMY_SPARES_DCR_BY_OPTEMPO where MEI_NSN = :nsn and OPTEMPO = 'HIGH' order by DCR desc
PackagedPOLArmyNSN=select MEI_NSN, PACKAGED_NSN, OPTEMPO, DCR from ARMY_PACKAGED_DCR_BY_OPTEMPO where MEI_NSN = :nsn order by DCR desc
#BulkPOLArmyNSN=select NSN, FUEL_NSN, OPTEMPO, GALLONS_PER_DAY from ARMY_FUELS_DCR_BY_OPTEMPO where NSN = :nsn order by GALLONS_PER_DAY desc
BulkPOLArmyNSN=select NSN, FUEL_NSN, UPPER(OPTEMPO), GALLONS_PER_DAY from ALP_MEI_FUEL where NSN = :nsn order by GALLONS_PER_DAY desc
AmmunitionArmyNSN=select MEI_NSN, DODIC, UPPER(OPTEMPO), TONS_PER_DAY from alp_mei_dodic_2_view where MEI_NSN = :nsn order by TONS_PER_DAY desc
MeiConsumption=select CONSUME_AMMO, CONSUME_FUEL, CONSUME_PKG_POL, CONSUME_SPARES from MEI_CONSUMPTION where NSN = :nsn

# AirForce
#
ConsumableAirforceMDS=select MDS, NSN, OPTEMPO, DEMANDS_PER_DAY from airforce_spares_dcr_by_optempo where MDS = :nsn order by DEMANDS_PER_DAY
BulkPOLAirforceMDS=select MDS, FUEL_NSN, OPTEMPO, GALLONS_PER_DAY from AIRFORCE_FUELS_DCR_BY_OPTEMPO where MDS = :nsn order by GALLONS_PER_DAY
# Marine
# 
ConsumableMarineTAMCN=select TAMCN, PART_NSN, OPTEMPO, DCR from MCGRD_SPARES_DCR_BY_OPTEMPO where TAMCN = :nsn order by DCR
ConsumableMarineNSN=select MEI_NSN, PART_NSN, OPTEMPO, DCR from MCGRD_SPARES_DCR_BY_OPTEMPO where MEI_NSN = :nsn order by DCR
ConsumableMarineMDS=select MDS,NSN, OPTEMPO, DEMANDS_PER_DAY from USMCAIR_SPARES_DCR_BY_OPTEMPO where MDS = :nsn order by DEMANDS_PER_DAY
BulkPOLMarineTAMCN=select TAMCN, FUEL_NSN, OPTEMPO, GALLONS_PER_DAY from MARINE_GROUND_FUELS_DCR_BY_OP where TAMCN = :nsn order by GALLONS_PER_DAY
BulkPOLMarineMDS=select MDS, FUEL_NSN, OPTEMPO, GALLONS_PER_DAY from MARINE_AIR_FUELS_DCR_BY_OP where MDS = :nsn order by GALLONS_PER_DAY
# Navy
#
ConsumableNavyMEI=select MEI_ID, NSN, OPTEMPO, DCR from NAVY_SPARES_DCR_BY_OPTEMPO where MEI_ID = :nsn order by DCR
ConsumableNavyMDS=select MDS, NSN, OPTEMPO, DEMANDS_PER_DAY from NAVYAIR_SPARES_DCR_BY_OPTEMPO where MDS = :nsn order by DEMANDS_PER_DAY
# Prototype & Property Provider
#
#%org.cougaar.glm.ldm.GLMPrototypeProvider
#%org.cougaar.glm.ldm.GLMPropertyProvider
