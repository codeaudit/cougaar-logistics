select
 fti.ti_id
, fti.ti_nm
, oa.dodic
, ac.nomenclature
from
    fdm_transportable_item fti
, oplog_ammorate oa
, ammo_characteristics ac
where
   fti.ti_id = oa.lin
and
    oa.dodic = ac.dodic
group by
  fti.ti_id
, oa.dodic
order by
  fti.ti_id
, oa.dodic