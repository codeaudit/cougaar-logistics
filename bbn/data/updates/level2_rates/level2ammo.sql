select 
fue.org_id
, oa.optempo
, sum(oa.tons_per_day * fue.unit_equipment_qty) agg_tons_per_day
from 
fdm_unit_equipment fue
, oplog_ammorate oa
, fdm_transportable_item_detail ftid
where
fue.ti_id = ftid.ti_id
and
ftid.materiel_item_identifier = oa.mei_nsn
group by
fue.org_id
, oa.optempo
order by
fue.org_id
, oa.optempo