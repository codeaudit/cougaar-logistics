select 
fue.org_id
, asdbo.optempo
, sum(fue.unit_equipment_qty * asdbo.dcr) agg_dcr
from 
fdm_unit_equipment fue
, army_spares_dcr_by_optempo asdbo
, fdm_transportable_item_detail ftid
where
fue.ti_id = ftid.ti_id
and
ftid.materiel_item_identifier = asdbo.mei_nsn
group by
fue.org_id
, asdbo.optempo
order by
fue.org_id
, asdbo.optempo