# Find all agents that are SecurityMnRManagers
recipeQueryALManagers=\
SELECT DISTINCT AC.COMPONENT_ALIB_ID FROM \
   alib_component AC, \
   community_attribute CA, \
   community_entity_attribute CEA, \
   asb_component_hierarchy ACH, \
   expt_trial ET, \
   expt_trial_assembly ETA, \
   asb_assembly AA \
WHERE \
    ACH.ASSEMBLY_ID :assembly_match: \
    AND (ACH.COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID OR \
    ACH.PARENT_COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID) \
    AND AC.COMPONENT_NAME = CEA.ENTITY_ID \
    AND CEA.COMMUNITY_ID = CA.COMMUNITY_ID \
    AND ET.TRIAL_ID = ':trial_id:' \
    AND ET.TRIAL_ID = ETA.TRIAL_ID \
    AND AA.ASSEMBLY_TYPE = 'COMM' \
    AND AA.ASSEMBLY_ID = ETA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CEA.ASSEMBLY_ID \
    AND AC.COMPONENT_TYPE = 'agent'\
    AND CA.ATTRIBUTE_ID = 'CommunityType' \
    AND CA.ATTRIBUTE_VALUE = 'AdaptiveLogistics' \
    AND CEA.ATTRIBUTE_ID = 'Role' \
    AND CEA.ATTRIBUTE_VALUE = 'AdaptiveLogisticsManager'

recipeQueryALMembers=\
SELECT DISTINCT AC.COMPONENT_ALIB_ID FROM \
   alib_component AC, \
   community_attribute CA, \
   community_entity_attribute CEA, \
   asb_component_hierarchy ACH, \
   expt_trial ET, \
   expt_trial_assembly ETA, \
   asb_assembly AA \
WHERE \
    ACH.ASSEMBLY_ID :assembly_match: \
    AND (ACH.COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID OR \
    ACH.PARENT_COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID) \
    AND AC.COMPONENT_NAME = CEA.ENTITY_ID \
    AND CEA.COMMUNITY_ID = CA.COMMUNITY_ID \
    AND ET.TRIAL_ID = ':trial_id:' \
    AND ET.TRIAL_ID = ETA.TRIAL_ID \
    AND AA.ASSEMBLY_TYPE = 'COMM' \
    AND AA.ASSEMBLY_ID = ETA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CEA.ASSEMBLY_ID \
    AND AC.COMPONENT_TYPE = 'agent'\
    AND CA.ATTRIBUTE_ID = 'CommunityType' \
    AND CA.ATTRIBUTE_VALUE = 'AdaptiveLogistics' \
    AND CEA.ATTRIBUTE_ID = 'Role' \
    AND CEA.ATTRIBUTE_VALUE = 'Member'

recipeQueryALTransportMembers=\
SELECT DISTINCT AC.COMPONENT_ALIB_ID FROM \
   alib_component AC, \
   community_attribute CA, \
   community_entity_attribute CEA, \
   asb_component_hierarchy ACH, \
   expt_trial ET, \
   expt_trial_assembly ETA, \
   asb_assembly AA \
WHERE \
    ACH.ASSEMBLY_ID :assembly_match: \
    AND (ACH.COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID OR \
    ACH.PARENT_COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID) \
    AND AC.COMPONENT_NAME = CEA.ENTITY_ID \
    AND CEA.COMMUNITY_ID = CA.COMMUNITY_ID \
    AND ET.TRIAL_ID = ':trial_id:' \
    AND ET.TRIAL_ID = ETA.TRIAL_ID \
    AND AA.ASSEMBLY_TYPE = 'COMM' \
    AND AA.ASSEMBLY_ID = ETA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CEA.ASSEMBLY_ID \
    AND AC.COMPONENT_TYPE = 'agent'\
    AND CA.ATTRIBUTE_ID = 'CommunityType' \
    AND CA.ATTRIBUTE_VALUE = 'AdaptiveLogistics' \
    AND CEA.ATTRIBUTE_ID = 'Role' \
    AND CEA.ATTRIBUTE_VALUE = 'Member' \
    AND CA.COMMUNITY_ID LIKE '%TRANSPORT-COMM'

recipeQueryALSupplyMembers=\
SELECT DISTINCT AC.COMPONENT_ALIB_ID FROM \
   alib_component AC, \
   community_attribute CA, \
   community_entity_attribute CEA, \
   asb_component_hierarchy ACH, \
   expt_trial ET, \
   expt_trial_assembly ETA, \
   asb_assembly AA \
WHERE \
    ACH.ASSEMBLY_ID :assembly_match: \
    AND (ACH.COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID OR \
    ACH.PARENT_COMPONENT_ALIB_ID = AC.COMPONENT_ALIB_ID) \
    AND AC.COMPONENT_NAME = CEA.ENTITY_ID \
    AND CEA.COMMUNITY_ID = CA.COMMUNITY_ID \
    AND ET.TRIAL_ID = ':trial_id:' \
    AND ET.TRIAL_ID = ETA.TRIAL_ID \
    AND AA.ASSEMBLY_TYPE = 'COMM' \
    AND AA.ASSEMBLY_ID = ETA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CA.ASSEMBLY_ID \
    AND ETA.ASSEMBLY_ID = CEA.ASSEMBLY_ID \
    AND AC.COMPONENT_TYPE = 'agent'\
    AND CA.ATTRIBUTE_ID = 'CommunityType' \
    AND CA.ATTRIBUTE_VALUE = 'AdaptiveLogistics' \
    AND CEA.ATTRIBUTE_ID = 'Role' \
    AND CEA.ATTRIBUTE_VALUE = 'Member' \
    AND CA.COMMUNITY_ID LIKE '%SUPPLY-COMM'







