/* Author: Benjamin Lubin; last modified by $Author: gvidaver $
 * Created: 2/26/01; last modified on $Date: 2002-05-14 20:41:05 $
 *
 * Except as otherwise noted, if any of the below return non-empty
 * tables, it is an ERROR or WARNING as indicated
 */

/*WARNING Invalid/unknown conveyancetype:*/
select distinct prototypeid, alptypeid, alpnomenclature, conveyancetype 
from conveyanceprototype_1
where conveyancetype not between 1 and 7;

/*ERROR Invalid/unknown legtype:*/
select distinct legid, starttime, endtime, legtype, convid
from conveyedleg_1
where legtype not between 1 and 5;

/*WARNING Invalid/unknown asset type*/
select distinct prototypeid, alptypeid, alpnomenclature, assettype
from assetprototype_1
where assettype not between 0 and 1;

/*WARNING Invalid/unknown asset class*/
select distinct prototypeid, alptypeid, alpnomenclature, assetclass
from assetprototype_1
where assetclass not between 1 and 12;

/*ERROR superior is subordinate to no one and not root*/
select distinct sup.org_id 
from org_1 sup 
left outer join org_1 sub
on sub.related_id=sup.org_id
left outer join orgroots_1 root
on sup.org_id = root.org_id
where sub.related_id is null 
and root.org_id is null;

/*Check for missing cross in refrences*/

/*ERROR assets in itinerary*/
select distinct itin.assetid 
from assetitinerary_1 itin
left outer join assetinstance_1 inst
on itin.assetid=inst.assetid
where inst.assetid is null;

/*ERROR legs int itinerary*/
select distinct itin.legid
from assetitinerary_1 itin
left outer join conveyedleg_1 leg
on itin.legid = leg.legid
where leg.legid is null;

/*ERROR start loc in leg*/
select distinct leg.startlocid
from conveyedleg_1 leg
left outer join locations_1 loc
on leg.startlocid = loc.locid
where loc.locid is null;

/*ERROR end loc in leg*/
select distinct leg.endlocid
from conveyedleg_1 leg
left outer join locations_1 loc
on leg.endlocid = loc.locid
where loc.locid is null;

/*ERROR conveyance in leg*/
select distinct leg.convid
from conveyedleg_1 leg
left outer join conveyanceinstance_1 conv
on leg.convid=conv.convid
where conv.convid is null;

/*ERROR owner from cargo instance*/
select distinct inst.assetid, inst.prototypeid, inst.ownerid
from assetinstance_1 inst
left outer join org_1 org
on inst.ownerid=org.related_id 
and org.relation_type=0
where org.related_id is null;

/*ERROR prototype from cargo instance*/
select distinct inst.prototypeid
from assetinstance_1 inst
left outer join assetprototype_1 proto
on inst.prototypeid=proto.prototypeid
where proto.prototypeid is null;

/*ERROR parent prototype doesn't exist*/
select distinct q.parentprototypeid
from assetprototype_1 q
left outer join assetprototype_1 s
on q.parentprototypeid = s.prototypeid 
where s.prototypeid is null
and q.parentprototypeid is not null;

/*WARNING conveyance home base in conveyanceinstance*/
select distinct conv.baselocid
from conveyanceinstance_1 conv
left outer join locations_1 loc
on conv.baselocid=loc.locid
where loc.locid is null;

/*WARNING conveyance owner in conveyanceinstance */
select distinct conv.ownerid
from conveyanceinstance_1 conv
left outer join org_1 org
on conv.ownerid=org.org_id
where org.org_id is null;

/*ERROR conveyance prototype in conveyanceinstance*/
select distinct conv.prototypeid
from conveyanceinstance_1 conv
left outer join conveyanceprototype_1 proto
on conv.prototypeid=proto.prototypeid
where proto.prototypeid is null;

/*ERROR org with no pretty name*/
select distinct org.org_id
from org_1 org
left outer join orgnames_1 names
on names.org_id=org.org_id
where names.org_id is null;

/**ERROR roots not in org table*/
select distinct root.org_id
from orgroots_1 root
left outer join org_1 org
on root.org_id=org.org_id
where org.org_id is null;

/*WARNING (as facilities are expected) All overlapping legs:*/
select distinct q.legid, q.starttime, q.endtime, q.convid
from conveyedleg_1 q, conveyedleg_1 s 
where
   (q.convid = s.convid)
and(q.legid != s.legid)
and(((q.starttime between s.starttime and s.endtime)
     and q.starttime != s.starttime and q.starttime != s.endtime)
    or
    ((q.endtime between s.starttime and s.endtime)
     and q.endtime != s.starttime and q.endtime != s.endtime))
order by q.convid;

/*ERROR Start before end*/
select distinct q.legid, q.starttime, q.endtime, 
q.startlocid, q.endlocid, q.legtype, q.convid
from conveyedleg_1 q 
where q.starttime > q.endtime 
order by q.convid;

/*ERROR Missing leg by carrier*/
/*Use the following query, and then walk the results in java looking for*/
/*startlocid for a given conv that is different from the last endlocid for*/
/*that convid.  Should be possible in one pass*/
select legid, starttime, endtime, startlocid, endlocid, convid 
from conveyedleg_1 
order by convid, starttime;


/*ERROR Missing leg by asset*/
/*Use the following query, and then walk the results in java looking for*/
/*startlocid for a given asset that is different from the last endlocid for*/
/*that asset.  Should be possible in one pass*/
select distinct c.legid, c.starttime, c.endtime, c.startlocid, c.endlocid, 
i.assetid 
from conveyedleg_1 c, assetitinerary_1 i 
where c.legid = i.legid 
and c.legtype !=4
and c.legtype !=5
order by i.assetid, c.starttime;
