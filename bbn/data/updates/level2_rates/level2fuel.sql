select 
fue.org_id
, amf.optempo
, sum(amf.gallons_per_day * fue.unit_equipment_qty) agg_gallons_per_day
from 
fdm_unit_equipment fue
, alp_mei_fuel amf
where
fue.ti_id = amf.lin
group by
fue.org_id
, amf.optempo
order by
fue.org_id
, amf.optempo