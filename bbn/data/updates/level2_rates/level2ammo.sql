select 
fue.org_id
, oa.optempo
, sum(oa.tons_per_day * fue.unit_equipment_qty) agg_tons_per_day
from 
fdm_unit_equipment fue
, oplog_ammorate oa
, org_pg_attr opa
where
fue.ti_id = oa.lin
and
fue.org_id = opa.org_id
and 
opa.pg_attribute_lib_id = "MilitaryOrgPG|Echelon"
and
opa.attribute_value = oa.echelon
group by
fue.org_id
, oa.optempo
order by
fue.org_id
, oa.optempo