select
fue.org_id
, if(sum((oa.dodic is not null)) > 0, 1, 0) consume_ammo
, if(sum((amf.fuel_nsn is not null)) > 0, 1, 0) consume_fuel
, if(sum((apdbo.packaged_nsn is not null)) > 0, 1, 0) consume_pkg_pol
, if(sum((asdbo.part_nsn is not null)) > 0, 1, 0) consume_spares
from
  fdm_transportable_item fti
, fdm_unit_equipment fue
, fdm_transportable_item_detail ftid
left join oplog_ammorate oa on (oa.mei_nsn = ftid.materiel_item_identifier)
left join alp_mei_fuel amf on (amf.nsn = ftid.materiel_item_identifier)
left join army_packaged_dcr_by_optempo apdbo on (apdbo.mei_nsn = ftid.materiel_item_identifier)
left join army_spares_dcr_by_optempo asdbo on (asdbo.mei_nsn = ftid.materiel_item_identifier)
where
    fti.ti_id = ftid.ti_id
and
   fti.ti_id = fue.ti_id
group by
fue.org_id
order by
fue.org_id