# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.44-nt

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0005FallingBehindConditionServlet','FallingBehindConditionServlet','org.cougaar.tools.csmart.recipe.SpecificInsertionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Class Name',2.000000000000000000000000000000,'org.cougaar.logistics.servlet.ConditionComponent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Component Name',6.000000000000000000000000000000,'FallingBehindConditionServlet');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Component Priority',8.000000000000000000000000000000,'COMPONENT');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Number of Arguments',0.000000000000000000000000000000,'3');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Target Component Selection Query',4.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Type of Insertion',7.000000000000000000000000000000,'plugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Value 1',5.000000000000000000000000000000,'org.cougaar.logistics.servlet.ConditionServlet');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Value 2',3.000000000000000000000000000000,'/CPUSettingServlet');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005FallingBehindConditionServlet','Value 3',1.000000000000000000000000000000,'conditionName=FallingBehind');
UNLOCK TABLES;

