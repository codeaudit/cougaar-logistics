CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
#$:.unshift File.join(CIP, 'csmart', 'assessment', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
#require 'assessment/scripting'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file


Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("SocA-MOAS-AL-SD").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ua/full-160a237v.plugins.rb"
  do_action "LayoutSociety", "#{CIP}/operator/Full-UA-19H20N-2BDE-NoG-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
#    "#{RULES}/assessment",
    "#{RULES}/logistics"

  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "StartJabberCommunications"

  do_action "CleanupSociety"

  do_action "VerifyHosts"

  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"

#  do_action "CreateNewCnCBaselineRun", "#{experiment.name}"

  do_action "InstallCompletionMonitor"

  do_action "KeepSocietySynchronized"

  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds

  # Start the Network Shaping
#  do_action "EnableNetworkShaping"
#these are for socVA
#  do_action "DefineWANLink", "link 2-4","BW-router" , 102, 104
#  do_action "DefineWANLink", "link 4-2","BW-router" , 104, 102
#these are for socVB
#  do_action "DefineWANLink", "link 2-4","BW-router" , 202, 204
#  do_action "DefineWANLink", "link 4-2","BW-router" , 204, 202

  do_action "Sleep", 30.seconds

  do_action "PublishNextStage"

  do_action "InfoMessage", "########  Starting Initial Planning Phase  Stage-1#########"

  wait_for  "SocietyQuiesced"

  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage1#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage1"
  do_action "Sleep", 1.minutes

  do_action "RelationshipServlet", "1-35-ARBN", "RELATIONSHIP_SCHEDULE_W_OVERLAP", "RELATIONSHIPS-InitPlan-1-35-ARBN.xml"
                                                                  
#  wait_for "Command", "ok"
  do_action "InfoMessage", "##### Killing Nodes REAR-B-NODE, 1-AD-NODE, 2-BDE-1-AD-NODE #####"
  do_action "KillNodes", "REAR-B-NODE", "1-AD-NODE", "2-BDE-1-AD-NODE"
  do_action "Sleep", 1.minutes
  do_action "RestartNodes", "REAR-B-NODE", "1-AD-NODE", "2-BDE-1-AD-NODE"
  do_action "Sleep", 1.minutes
    wait_for  "SocietyQuiesced"

  do_action "SaveSocietyCompletion", "comp_stage1_AFTA_Restart#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage1_AFTA_Restart"
  do_action "Sleep", 1.minutes

  do_action "InfoMessage", "##### Moving AGENTS to other  NODES ######"
  do_action "MoveAgent", "1-35-ARBN", "AVN-DET-NODE"
  do_action "MoveAgent", "1-6-INFBN", "1-3-BDE-1-AD-NODE"
#  do_action "MoveAgent", "2-6-INFBN", "2-BDE-1-AD-NODE"
#  do_action "MoveAgent", "2-BDE-1-AD", "3-BDE-1-AD-NODE"
#  do_action "MoveAgent", "4-27-FABN", "AVNBDE-1-AD-NODE"
#  do_action "MoveAgent", "40-ENGBN", "1-BDE-1-AD-NODE"
  do_action "MoveAgent", "47-FSB", "REAR-A-NODE"
  do_action "MoveAgent", "240-SSCO", "REAR-C-NODE"
  do_action "Sleep", 3.minutes
#    wait_for  "SocietyQuiesced"

  do_action "SaveSocietyCompletion", "comp_stage1_AFTA_Move1#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage1_AFTA_Move1"
  do_action "Sleep", 1.minutes

  do_action "RelationshipServlet", "1-35-ARBN", "RELATIONSHIP_SCHEDULE_W_OVERLAP", "RELATIONSHIPS-AFTA-REHYD-B4-1-35-ARBN.xml"

  do_action "InfoMessage", "Advancing time to AUG 14 (C-1), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 4 * 24.hours * 1000

  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase  Stage-2  ########"
  do_action "InfoMessage", "########  OPlan Deployment Date Change for 2-BDE #######"
  wait_for "SocietyQuiesced"
                                                                  
  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage2#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage2"
  do_action "Sleep", 1.minutes

  do_action "InfoMessage", "##### Moving AGENTS back to Original NODES ######"
  do_action "MoveAgent", "1-35-ARBN", "2-BDE-1-AD-NODE"
  do_action "MoveAgent", "1-6-INFBN", "2-BDE-1-AD-NODE"
#  do_action "MoveAgent", "2-6-INFBN", "2-6-INFBN-NODE"
#  do_action "MoveAgent", "2-BDE-1-AD", "2-BDE-1-AD-NODE"
#  do_action "MoveAgent", "4-27-FABN", "4-27-FABN-NODE"
#  do_action "MoveAgent", "40-ENGBN", "40-ENGBN-NODE"
  do_action "MoveAgent", "47-FSB", "2-BDE-1-AD-NODE"
  do_action "MoveAgent", "240-SSCO", "REAR-A-NODE"
  do_action "Sleep", 3.minutes
#  wait_for "SocietyQuiesced"

  do_action "SaveSocietyCompletion", "comp_stage2_AFTA_Move2#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage2_AFTA_Move2"
  do_action "Sleep", 1.minutes
                                                                  
#  wait_for "Command", "ok"
                                                                  
  do_action "InfoMessage", "Advancing time to AUG 30 (C+15), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 16 * 24.hours * 1000
                                                                  
  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase  Stage-3 #########"
  do_action "InfoMessage", "########  OPlan OPTEMPO Change for 2-BDE on C+17 #########"
                                                                  
  wait_for  "NextOPlanStage"
                                                                  
  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase Stage-4 #########"
  do_action "InfoMessage", "########  UA OPlan deployment + day 1.         #########"
  wait_for "SocietyQuiesced"
                                                                  
  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage3_4#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage3_4"
  do_action "Sleep", 1.minutes
                                                                  
#  wait_for "Command", "ok"
                                                                  
  do_action "InfoMessage", "Advancing time to Sep 4 (C+20), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 5 * 24.hours * 1000
                                                                  
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "47-FSB", "FuelSupplyProvider", "10/25/2005"
#  do_action "DynamicSD", "47-FSB", "AmmunitionProvider", "10/25/2005"
#  do_action "DynamicSD", "47-FSB", "SubsistenceSupplyProvider", "10/25/2005"
#  do_action "DynamicSD", "47-FSB", "PackagedPOLSupplyProvider", "10/25/2005"
#  do_action "DynamicSD", "47-FSB", "SparePartsProvider", "10/25/2005"

  wait_for "SocietyQuiesced"

  do_action "RelationshipServlet", "1-35-ARBN", "RELATIONSHIP_SCHEDULE_W_OVERLAP", "RELATIONSHIPS-AFTA-SD1-1-35-ARBN.xml"
  do_action "SaveSocietyCompletion", "comp_stageSD1#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage_SD1"
  do_action "Sleep", 1.minutes
  
  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase Stage5  ########"
  do_action "InfoMessage", "########  UA OPlan change for Pursuit and Urban Assault #######"
                                                                  
  wait_for  "NextOPlanStage"

  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase Stage-6 #########"
  do_action "InfoMessage", "########  1-BDE OPTEMPO changes from Medium to High  #########"
  wait_for "SocietyQuiesced"
                                                                  
  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage5_6#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage5_6"
  do_action "Sleep", 1.minutes
                                                                  
#  wait_for "Command", "ok"
                                                                  
  do_action "InfoMessage", "Advancing time to Sep 5 (C+21), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 24.hours * 1000
                                                                  
  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase Stage-7 #########"
  do_action "InfoMessage", "########  UA begins Air Assault   #########"
  wait_for "SocietyQuiesced"
                                                                  
  do_action "SaveSocietyCompletion", "comp_stage7#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage7"
  do_action "Sleep", 1.minutes

  do_action "InfoMessage", "Advancing time to Sep 10 (C+26), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 5 * 24.hours * 1000
                                                                  
  do_action "InfoMessage", "########  MADE IT TO C+26   #########"                                                                  
#  do_action "FreezeSociety"
                                                                  
  do_action "SaveSocietyCompletion", "comp_stage7_C26#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage7_C26"
  do_action "Sleep", 1.minutes
                                                                  
  do_action "InfoMessage", "### Starting Dynamic SD TWO perturbation ###"
  do_action "DynamicSD", "125-FSB", "FuelSupplyProvider", "11/19/2005"
#  do_action "DynamicSD", "125-FSB", "AmmunitionProvider", "11/19/2005"
#  do_action "DynamicSD", "125-FSB", "SubsistenceSupplyProvider", "11/19/2005"
#  do_action "DynamicSD", "125-FSB", "PackagedPOLSupplyProvider", "11/19/2005"
#  do_action "DynamicSD", "125-FSB", "SparePartsProvider", "11/19/2005"

  wait_for "SocietyQuiesced"

  do_action "RelationshipServlet", "1-35-ARBN", "RELATIONSHIP_SCHEDULE_W_OVERLAP", "RELATIONSHIPS-AFTA-SD2-1-35-ARBN.xml"
  do_action "SaveSocietyCompletion", "comp_stageSD2#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage_SD2"
  do_action "Sleep", 1.minutes
  
#  do_action "DisableNetworkShaping"

  do_action "Sleep", 30.seconds

#  do_action "FreezeSociety"

  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  include "inventory.inc", "#{experiment.name}"

#  do_action "LogCnCData"
#  wait_for "CnCLoggingComplete" do
#    do_action "GenericAction" do
#      puts "CnCLoggingComplete was not received"
#    end
#    continue
#  end

#  do_action "StartDatagrabberService"
#  do_action "ConnectToDatagrabber", "localhost" do |datagrabber|
#    run = datagrabber.new_run
#    run.wait_for_completion
#  end
#  do_action "StopDatagrabberService"

#  do_action "AggAgentQueryBasic", "agg_basic.xml"
#  do_action "AggAgentQueryDemand", "agg_demand.xml"
#  do_action "AggAgentQueryDemand", "agg_demand_for_comp.xml"
#  do_action "AggAgentQueryShortfall", "agg_shortfall.xml"
#  do_action "AggAgentQueryJP8", "agg_jp8.xml"

#  wait_for "Command", "shutdown"

  do_action "Sleep", 30.seconds
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "CleanupSociety"
  do_action "StopCommunications"
}
