# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.43-nt

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-StimServlet','StimulatorServletAddition','org.cougaar.tools.csmart.recipe.SpecificInsertionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Class Name',4.000000000000000000000000000000,'org.cougaar.core.servlet.BlackboardServletComponent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Component Name',0.000000000000000000000000000000,'org.cougaar.core.servlet.BlackboardServletComponent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Component Priority',2.000000000000000000000000000000,'COMPONENT');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Number of Arguments',3.000000000000000000000000000000,'2');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Target Component Selection Query',5.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Type of Insertion',1.000000000000000000000000000000,'plugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Value 1',6.000000000000000000000000000000,'org.cougaar.glm.servlet.GLMStimulatorServlet');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-StimServlet','Value 2',7.000000000000000000000000000000,'/stimulator');
UNLOCK TABLES;

