Database=${org.cougaar.database}
Username=${org.cougaar.database.user}
Password=${org.cougaar.database.password}

%SQLFDMAggregateAssetCreator
query= select \
    FUE.UNIT_IDENTIFIER AS UIC, \
    FUE.UNIT_EQUIPMENT_QTY AS QTY, \
    FTED.MATERIEL_ITEM_IDENTIFIER AS NSN, \
    FUE.TI_ID AS TI_ID, \
    FTED.TID_ID AS TID_ID, \
    FTED.SHPPNG_CNFGRTN_CD, \
    SUBSTR(FTE.TI_NM,1,30) AS TI_NM, \
    TID_LG_DM AS LENGTH, \
    TID_WDTH_DM AS WIDTH, \
    TID_HT_DM AS HEIGHT, \
    TID_WT AS WEIGHT, \
    TID_VL AS VOLUME, \
    FTED.CGO_TP_CD AS CGO_TP_CD, \
    FTED.CGO_XTNT_CD AS CGO_XTNT_CD, \
    FTED.CGO_CNTZN_CD AS CGO_CNTZN_CD, \
    FTED.MATERIEL_ITEM_IDENTIFIER AS MATERIEL_ITEM_IDENTIFIER, \
    FTED.TYPE_PACK_CODE AS TYPE_PACK_CODE, \
    FTED.TID_EQ_TY_CD AS TID_EQ_TY_CD, \
    TID_FTPRNT_AR AS FOOTPRINT \
FROM \
    fdm_unit_equipment FUE, \
    fdm_transportable_item FTE, \
    fdm_transportable_item_detail FTED \
WHERE \
FUE.ORG_ID=:agent \
AND FUE.TI_ID = FTE.TI_ID \
AND FUE.TI_ID = FTED.TI_ID \
AND FTED.TID_ID = '01' \
AND FTED.CGO_TP_CD IN ('A', 'B', 'C', 'K', 'R') \
AND FTED.MATERIEL_ITEM_IDENTIFIER <> '0000000000000' \
ORDER BY \
    FUE.UNIT_IDENTIFIER, \
    FUE.UNIT_EQUIPMENT_QTY ASC


query.mysql= select \
    FUE.UNIT_IDENTIFIER AS UIC, \
    FUE.UNIT_EQUIPMENT_QTY AS QTY, \
    FTED.MATERIEL_ITEM_IDENTIFIER AS NSN, \
    FUE.TI_ID AS TI_ID, \
    FTED.TID_ID AS TID_ID, \
    FTED.SHPPNG_CNFGRTN_CD, \
    SUBSTRING(FTE.TI_NM,1,30) AS TI_NM, \
    TID_LG_DM AS LENGTH, \
    TID_WDTH_DM AS WIDTH, \
    TID_HT_DM AS HEIGHT, \
    TID_WT AS WEIGHT, \
    TID_VL AS VOLUME, \
    FTED.CGO_TP_CD AS CGO_TP_CD, \
    FTED.CGO_XTNT_CD AS CGO_XTNT_CD, \
    FTED.CGO_CNTZN_CD AS CGO_CNTZN_CD, \
    FTED.MATERIEL_ITEM_IDENTIFIER AS MATERIEL_ITEM_IDENTIFIER, \
    FTED.TYPE_PACK_CODE AS TYPE_PACK_CODE, \
    FTED.TID_EQ_TY_CD AS TID_EQ_TY_CD, \
    TID_FTPRNT_AR AS FOOTPRINT \
FROM \
    fdm_unit_equipment FUE, \
    fdm_transportable_item FTE, \
    fdm_transportable_item_detail FTED \
WHERE \
FUE.ORG_ID=:agent \
AND FUE.TI_ID = FTE.TI_ID \
AND FUE.TI_ID = FTED.TI_ID \
AND FTED.TID_ID = '01' \
AND FTED.CGO_TP_CD IN ('A', 'B', 'C', 'K', 'R') \
AND FTED.MATERIEL_ITEM_IDENTIFIER <> '0000000000000' \
ORDER BY \
    FUE.UNIT_IDENTIFIER, \
    FUE.UNIT_EQUIPMENT_QTY ASC

%SQLMOSAggregateAssetCreator
#query = select ('MOS/' || billet.unfrmd_srvc_occptn_cd) AS MOS_LEVEL, sum(to_strength) AS MOS_QTY, unfrmd_srvc_occptn_tx  \
#	from \
#	fdm_unit_billet billet, \
#	fdm_unfrmd_srvc_occptn occ, \
#	lib_organization liborg \
#	where \
#	liborg.org_id=:agent \
#	and billet.unfrmd_srvc_occptn_cd=occ.unfrmd_srvc_occptn_cd \
#	and occ.rank_subcategory_code(+)='E' \
#	and billet.unit_identifier = liborg.uic \
#	group by billet.unfrmd_srvc_occptn_cd, unfrmd_srvc_occptn_tx

query = select 'MOS/11B' AS MOS_LEVEL, NVL(SUM(TO_STRENGTH),0) AS MOS_QTY, 'MOS/11B/INFANTRYMAN'  \
	FROM \
	fdm_unit_billet BILLET, \
	fdm_unfrmd_srvc_occptn OCC \
	WHERE \
	BILLET.ORG_ID=:agent \
	AND BILLET.UNFRMD_SRVC_OCCPTN_CD=OCC.UNFRMD_SRVC_OCCPTN_CD \
	AND OCC.RANK_SUBCATEGORY_CODE(+)='E'


#query.mysql = \
#select concat('MOS/',billet.unfrmd_srvc_occptn_cd) AS MOS_LEVEL, sum(to_strength) AS MOS_QTY, unfrmd_srvc_occptn_tx  \
#  from \
#   fdm_unit_billet billet, \
#   lib_organization liborg \
#   left join fdm_unfrmd_srvc_occptn occ on \
#     (billet.unfrmd_srvc_occptn_cd=occ.unfrmd_srvc_occptn_cd \
#       and occ.rank_subcategory_code='E') \
#  where \
#   liborg.org_id=:agent \
#   and billet.unit_identifier = liborg.uic \
#   group by billet.unfrmd_srvc_occptn_cd, unfrmd_srvc_occptn_tx
#

query.mysql = \
select 'MOS/11B', IFNULL(SUM(TO_STRENGTH),0) AS MOS_QTY,  'MOS/11B/INFANTRYMAN'  \
  FROM \
   fdm_unit_billet BILLET \
   LEFT JOIN fdm_unfrmd_srvc_occptn OCC ON \
     (BILLET.UNFRMD_SRVC_OCCPTN_CD=OCC.UNFRMD_SRVC_OCCPTN_CD \
       AND OCC.RANK_SUBCATEGORY_CODE='E') \
  WHERE \
   BILLET.ORG_ID=:agent

# Then, get the containers and generate an aggregate asset
%SQLAggregateAssetCreator
#query = select '8115001682275' AS NSN, DECODE(CONTAINER_20_FT_QTY,NULL,30,CONTAINER_20_FT_QTY) AS QTY_OH, 'CONTAINER' AS \
#	NOMENCLATURE \
#	FROM ue_summary_mtmc UES \
#	WHERE \
#	LIBORG.ORG_ID=:agent \
#	AND UES.UIC(+) = LIBORG.UIC


query.mysql = select DISTINCT '8115001682275', \
        30, \
	'CONTAINER' \


