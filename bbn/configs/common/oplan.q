# Used by glm/src/org/cougaar/mlm/plugin/ldm/OrgActivityQueryHandler
# Note that the word select must be lowercase cause of bad plugin
# Note that lowercase database is necessary because of different plugins access

Database=${org.cougaar.oplan.database}
Username=${org.cougaar.oplan.user}
Password=${org.cougaar.oplan.password}
# do both capitalizations
database=${org.cougaar.oplan.database}
username=${org.cougaar.oplan.user}
password=${org.cougaar.oplan.password}

activity = ACTIVITY_TYPE
opTempo = OPTEMPO
location = LOCATION

# The following defs allow us to find the relevant query handler when
# individual queries are to be performed

locationQuery.handlers=NewAlpLocQueryHandler,NewGeoLocQueryHandler
oplanInfoQuery.handler=NewOplanQueryHandler
activeStagesQuery.handler=NewActiveStagesQueryHandler
orgActivityQuery.handler=NewOrgActivityQueryHandler

# get Oplan Timeframe info
OplanTimeframeQuery = select OPERATION_NAME, min_planning_offset, start_offset, end_offset from oplan where oplan_id = ':oplanid:'

# get Oplan Stage info
OplanStageQuery = select stage_name, stage_num from oplan_stage where oplan_id = ':oplanid:'

# get AlpLoc info
AlpLocQuery = select alploc_code, location_name, latitude, longitude from alploc

# get GeoLoc info
GeoLocQuery = \
select DISTINCT \
    GEOLOC_CODE, \
    LOCATION_NAME, \
    INSTALLATION_TYPE_CODE, \
    CIVIL_AVIATION_CODE, \
    LATITUDE, \
    LONGITUDE, \
    COUNTRY_STATE_CODE, \
    COUNTRY_STATE_LONG_NAME \
FROM geoloc, \
     oplan_agent_attr \
WHERE ATTRIBUTE_NAME = 'LOCATION' \
    AND GEOLOC_CODE=ATTRIBUTE_VALUE

# get Oplan info
OplanInfoQuery = \
select OPERATION_NAME, \
   PRIORITY \
FROM oplan OPLAN \
WHERE OPLAN_ID = ':oplanid:'

#Get Orgactivities

# FORCE is a MySQL4.0 keyword so avoid using it
OrgActivityQuery.mysql = \
select DISTINCT ATTRIBUTE_NAME, \
    ORG_ID, \
    ATTRIBUTE_VALUE, \
    START_CDAY, \
    END_CDAY, \
    STAGE_NUM \
 FROM oplan_agent_attr ATTR, \
      oplan OP \
 WHERE \
  ATTR.ORG_ID = ':agent:' \
  AND ATTR.OPLAN_ID = ':oplanid:' \
  AND OP.OPLAN_ID = ':oplanid:' \
  AND ATTR.STAGE_NUM = :oplanStage: \
  AND ATTRIBUTE_NAME IN ('ACTIVITY_TYPE','OPTEMPO','LOCATION')

# Query to determine the active oplan stages of an organization

ActiveStagesQuery = \
SELECT MIN(STAGE_NUM) FROM OPLAN_ACTIVE_STAGE \
  WHERE OPLAN_ID = ':oplanid:' \
    AND ORG_ID = ':agent:'
