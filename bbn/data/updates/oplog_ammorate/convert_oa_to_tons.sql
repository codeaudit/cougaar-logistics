select
    oat.echelon
, ftid.materiel_item_identifier mei_nsn
, oat.lin
, oat.dodic
, oat.optempo
, (oat.rounds_per_day*ac.weight)/2000 tons_per_day
, "ARMY" service
from
    oplog_ammorate_tmp oat
, ammo_characteristics ac
, fdm_transportable_item_detail ftid
where
oat.dodic = ac.dodic 
and oat.lin = ftid.ti_id
order by
  mei_nsn
, oat.dodic
, oat.echelon
, oat.optempo
, service