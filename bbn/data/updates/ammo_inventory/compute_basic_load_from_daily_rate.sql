select
mblt.lin
, mblt.dodic
, oa.tons_per_day * 2000 * 5 / ac.weight
from
    missing_basic_load_tmp mblt
, oplog_ammorate oa
, ammo_characteristics ac
where
   mblt.lin = oa.lin
and
    oa.dodic = ac.dodic
and
    mblt.dodic = ac.dodic
and
    oa.optempo = "HIGH"
and 
    oa.echelon = "BATTALION"
order by
  mblt.lin
, mblt.dodic