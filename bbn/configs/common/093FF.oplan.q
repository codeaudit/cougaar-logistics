# Used by glm/src/org/cougaar/mlm/plugin/ldm/OrgActivityQueryHandler
# Referenced by the glm/src/org/cougaar/mlm/plugin/GLSInitServlet
# Note that the word select must be lowercase cause of bad plugin
# Note that lowercase database is necessary because of different plugins access

Database=${org.cougaar.refconfig.database}
Username=${org.cougaar.refconfig.user}
Password=${org.cougaar.refconfig.password}
database=${org.cougaar.refconfig.database}
oplanid = 093FF
activity = ACTIVITY_TYPE
opTempo = OPTEMPO
location = LOCATION

# get Oplan Timeframe info
OplanTimeframeQuery = select OPERATION_NAME, min_planning_offset, start_offset, end_offset from oplan where oplan_id = ':oplan_id:'

# get AlpLoc info
%AlpLocQueryHandler
AlpLocQuery = select alploc_code, location_name, latitude, longitude from alploc

# get GeoLoc info
%GeoLocQueryHandler
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
%OplanQueryHandler
OplanInfoQuery = \
select OPERATION_NAME, \
   PRIORITY \
FROM oplan OPLAN \
WHERE OPLAN_ID = '093FF'

#Get Orgactivities
%OrgActivityQueryHandler

# FORCE is a MySQL4.0 keyword so avoid using it
OrgActivityQuery.mysql = \
select DISTINCT ATTRIBUTE_NAME AS RELATION_NAME, \
    ORG_ID, \
    'FORCE_TYPE', \
    ATTRIBUTE_VALUE AS RELATES_TO, \
    'RELATES_TO_TYPE', \
    START_CDAY AS START_DAY, \
    END_CDAY AS END_DAY \
 FROM oplan_agent_attr ATTR, \
      oplan OP \
 WHERE \
  ATTR.ORG_ID = :agent \
  AND ATTR.OPLAN_ID = '093FF' \
  AND OP.OPLAN_ID = '093FF' \
  AND ATTRIBUTE_NAME IN ('ACTIVITY_TYPE','OPTEMPO','LOCATION')

