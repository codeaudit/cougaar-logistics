select
  fti.ti_id
, fti.ti_nm
--, ftid.materiel_item_identifier nsn
, ftid.cgo_tp_cd
, (oa.dodic is not null) consume_ammo
, (amf.fuel_nsn is not null) consume_fuel
, (apdbo.packaged_nsn is not null) consume_pkg_pol
, (asdbo.part_nsn is not null) consume_spares
from
  fdm_transportable_item fti
, fdm_transportable_item_detail ftid
left join oplog_ammorate oa on (oa.mei_nsn = ftid.materiel_item_identifier)
left join alp_mei_fuel amf on (amf.nsn = ftid.materiel_item_identifier)
left join army_packaged_dcr_by_optempo apdbo on (apdbo.mei_nsn = ftid.materiel_item_identifier)
left join army_spares_dcr_by_optempo asdbo on (asdbo.mei_nsn = ftid.materiel_item_identifier)
where
    fti.ti_id = ftid.ti_id
group by
  fti.ti_id
, fti.ti_nm
, ftid.materiel_item_identifier
order by
  fti.ti_id