CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')

require 'cougaar/scripting'
require 'ultralog/scripting'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file


Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("SAVE-PostStage5").run(10) {
  set_archive_path "#{CIP}/Logs"

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ua/small-tc20-17a12v.plugins.rb"
  do_action "LayoutSociety", "#{CIP}/operator/T20-SMALL-UA-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics"
  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "StartJabberCommunications"
  do_action "CleanupSociety"
  do_action "VerifyHosts"


  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"
  do_action "InstallCompletionMonitor"

  do_action "MarkForArchive", "#{CIP}/workspace/log4jlogs", "*.log", "Log4j node log"

  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds

  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Initial Planning Phase  Stage-1#########"
  wait_for "SocietyQuiesced"

  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage1#{experiment.name}.xml"
  do_action "Sleep", 10.seconds
  do_action "FullInventory", "Stage1"
  do_action "Sleep", 10.seconds

#  wait_for "Command", "ok"

  do_action "InfoMessage", "Advancing time to AUG 14 (C-1), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 4 * 24.hours

  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase  Stage-2  ########"
  do_action "InfoMessage", "########  OPlan Deployment Date Change for 2-BDE #######"
  wait_for "SocietyQuiesced"

  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage2#{experiment.name}.xml"
  do_action "Sleep", 10.seconds
  do_action "FullInventory", "Stage2"
  do_action "Sleep", 10.seconds

#  wait_for "Command", "ok"

  do_action "InfoMessage", "Advancing time to AUG 30 (C+15), 1 day steps, quiescing between s
teps"
  do_action "AdvanceTime", 16 * 24.hours

  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase  Stage-3 #########"
  do_action "InfoMessage", "########  OPlan OPTEMPO Change for 2-BDE on C+17 #########"

  wait_for  "NextOPlanStage"
=begin
  do_action "SaveSocietyCompletion", "comp_stage3#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
=end

  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Next Planning Phase Stage-4 #########"
  do_action "InfoMessage", "########  UA OPlan deployment + day 1.         #########"
  wait_for "SocietyQuiesced"

  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage3_4#{experiment.name}.xml"
  do_action "Sleep", 10.seconds
  do_action "FullInventory", "Stage3_4"
  do_action "Sleep", 10.seconds
  do_action "AggAgentQueryShortfall", "agg_shortfall34.xml"
  do_action "Sleep", 10.seconds

#  wait_for "Command", "ok"

  do_action "InfoMessage", "Advancing time to Sep 4 (C+20), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 5 * 24.hours

  do_action "Sleep", 5.minutes
  do_action "SavePersistenceSnapshot", "/OperatorArchive/SAVE-P-PreStage5#{experiment.name}.tgz"
  do_action "Sleep", 5.minutes

  do_action "InfoMessage", "Advancing time to Sep 7 (C+20), 1 day steps, quiescing between steps"
  do_action "AdvanceTime", 3 * 24.hours

  do_action "SaveSocietyCompletion", "comp_postStage5{experiment.name}.xml"
  do_action "Sleep", 10.seconds
  do_action "FullInventory", "PostStage5"
  do_action "UAInventory", "ua-PostStage5"
  do_action "FCSInventory", "fcs-PostStage5"
  do_action "Sleep", 10.seconds

  do_action "FreezeSociety"
  do_action "SynchronizeSocietyTime"
  do_action "StopSociety"
  do_action "Sleep", 5.minutes
  do_action "SavePersistenceSnapshot", "/OperatorArchive/SAVE-P-PostStage5#{experiment.name}.tgz"

#  wait_for "Command", "shutdown"
  do_action "Sleep", 30.seconds
#  do_action "StopSociety"
  do_action "StopCommunications"
}
