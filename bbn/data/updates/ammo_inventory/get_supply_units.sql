select
  fu.org_id
from
  fdm_unit fu
where
fu.org_id like '%MSB%'
or
fu.org_id like '%FSB%'
or
fu.org_id like '%DASB%'
or
fu.org_id like '%MAINTCO%'
order by
  fu.org_id