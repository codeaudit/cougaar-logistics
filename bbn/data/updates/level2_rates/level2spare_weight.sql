select 
fue.org_id
, asdbo.optempo
, sum(fue.unit_equipment_qty * asdbo.dcr * header.weight / 2000000) agg_tons_per_day
from 
fdm_unit_equipment fue
, army_spares_dcr_by_optempo asdbo
, fdm_transportable_item_detail ftid
, header
where
fue.ti_id = ftid.ti_id
and
ftid.materiel_item_identifier = asdbo.mei_nsn
and
asdbo.part_nsn = header.nsn
group by
fue.org_id
, asdbo.optempo
order by
fue.org_id
, asdbo.optempo