CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')

require 'cougaar/scripting'
require 'ultralog/scripting'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file


Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("Small1AD-Pred-Rehydrate").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ad/SMALL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/1AD-SMALL-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics"
#    "#{RULES}/isat/debug/remove_classes.rule"

  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "StartJabberCommunications"

  do_action "CleanupSociety"

  do_action "VerifyHosts"

  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"

  do_action "InstallCompletionMonitor", true

  do_action "KeepSocietySynchronized"

  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds

  do_action "PublishNextStage"

  do_action "InfoMessage", "########  Starting Initial Planning Phase  Stage-1#########"

  wait_for  "SocietyQuiesced"

  wait_for  "NextOPlanStage"
  do_action "SaveSocietyCompletion", "comp_stage1#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage1"
  do_action "Sleep", 1.minutes

  do_action "InfoMessage", "##### Killing Nodes FWD-NODE, 47-FSB-NODE #####"
  do_action "KillNodes", "FWD-NODE", "47-FSB-NODE"
  do_action "Sleep", 2.minutes
  do_action "RestartNodes", "FWD-NODE", "47-FSB-NODE"
  wait_for  "SocietyQuiesced"
  
  do_action "SaveSocietyCompletion", "Stage1_AFTA_Restart#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage1_AFTA_Restart"
  do_action "Sleep", 1.minutes

#  wait_for "Command", "ok"
                                                                  
  do_action "InfoMessage", "Advancing time 20 days"
  do_action "AdvanceTime", 20 * 24.hours * 1000

  
  do_action "DisableNetworkInterfaces", "47-FSB-NODE"
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB'].uri+"/commStatus?commUp=false&connectedAgentName=47-FSB" 
    Cougaar::Communications::HTTP.get(uri)
  end

  # wait_for "Command", "ok"

  do_action "InfoMessage", "Advance August 31 days"
#  do_action "AdvanceTime", 5 * 24.hours * 1000
  do_action "AdvanceTime", 86400000, 86400000, false
  do_action "Sleep", 5.minutes

  do_action "InfoMessage", "Advance Sept 1 days"
#  do_action "AdvanceTime", 5 * 24.hours * 1000
  do_action "AdvanceTime", 86400000, 86400000, false
  do_action "Sleep", 5.minutes

#  wait_for "Command", "ok"
  
  do_action "InfoMessage", "Advance Sept 2 days"
#  do_action "AdvanceTime", 5 * 24.hours * 1000
  do_action "AdvanceTime", 86400000, 86400000, false
  do_action "Sleep", 5.minutes

 do_action "InfoMessage", "Advance Sept 3 days"
#  do_action "AdvanceTime", 5 * 24.hours * 1000
  do_action "AdvanceTime", 86400000, 86400000, false
  do_action "Sleep", 5.minutes
  
  do_action "SaveSocietyCompletion", "4Day_CommLoss#{experiment.name}.xml"
  do_action "Sleep", 1.minutes
  include "inventory.inc", "Stage1"
  do_action "Sleep", 1.minutes

  do_action "EnableNetworkInterfaces", "47-FSB-NODE"
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB'].uri+"/commStatus?commUp=true&connectedAgentName=47-FSB" 
    Cougaar::Communications::HTTP.get(uri)
  end
  
  do_action "Sleep", 5.minutes

  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  include "inventory.inc", "AFTER-NIC-RESTORE"

  wait_for "Command", "ok"

  do_action "InfoMessage", "##### Moving AGENTS to other  NODES ######"
  do_action "MoveAgent", "47-FSB", "FWD-NODE"
  do_action "MoveAgent", "123-MSB", "47-FSB-NODE"
#  wait_for  "SocietyQuiesced"
  do_action "Sleep", 2.minutes

  do_action "SaveSocietyCompletion", "comp_stage1_AFTA_Move1#{experiment.name}.xml"
  do_action "Sleep", 2.minutes
  include "inventory.inc", "Stage1_AFTA_Move1"
  do_action "Sleep", 1.minutes

  do_action "InfoMessage", "Advancing time 1 days"
  do_action "AdvanceTime", 1 * 24.hours * 1000

  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  include "inventory.inc", "AFTER-1Day-PostRestore"

  wait_for "Command", "shutdown"

  do_action "Sleep", 30.seconds
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "CleanupSociety"
  do_action "StopCommunications"
}
