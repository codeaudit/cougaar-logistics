select
  fue.org_id
, fue.ti_id
, fue.unit_equipment_qty
, oa.dodic
, ac.weight
from
  fdm_unit_equipment fue
, oplog_ammorate oa
, combat_loads cl
, ammo_characteristics ac
where
fue.ti_id = oa.lin
and
oa.dodic = cl.dodic
and 
oa.dodic = ac.dodic
group by
  fue.org_id
, fue.ti_id
, oa.dodic
order by
  fue.org_id
, oa.dodic
, fue.ti_id