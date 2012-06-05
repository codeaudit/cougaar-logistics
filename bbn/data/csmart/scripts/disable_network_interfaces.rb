=begin script

include_path: disable_network_interfaces.rb
description: Disable the network interfaces for the node given in the argument.

=end

insert_after :before_stage_3 do
  do_action "DisableNetworkInterfaces", "2BDE-NODE"
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-POL.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=false&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)
  end
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-ORD.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=false&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)
  end
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-PARTS.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=false&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)
  end
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-FOOD.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=false&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)
  end

  # wait_for "Command", "ok"

  do_action "InfoMessage", "Advance August 31 days"
  do_action "AdvanceTime", 1.days
  do_action "Sleep", 3.minutes

  do_action "InfoMessage", "Advance Sept 1 day"
  do_action "AdvanceTime", 1.days
  do_action "Sleep", 3.minutes

#  wait_for "Command", "ok"

  do_action "InfoMessage", "Advance Sept 2 day"
  do_action "AdvanceTime", 1.days
  do_action "Sleep", 3.minutes

  do_action "InfoMessage", "Advance Sept 3 day"
  do_action "AdvanceTime", 1.days
  do_action "Sleep", 3.minutes

  do_action "InfoMessage", "Advance Sept 4 day"
  do_action "AdvanceTime", 1.days
  do_action "Sleep", 3.minutes

#  wait_for "Command", "ok"

  include "#{CIP}/csmart/lib/isat/post_stage_data.inc", "5Day_CommLoss"
  do_action "EnableNetworkInterfaces", "2BDE-NODE"
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-POL.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=true&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)
  end  
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-ORD.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=true&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)  
  end  
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-PARTS.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=true&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)  
  end  
  do_action "GenericAction" do |run|
    uri=run.society.agents['123-MSB-FOOD.DISCOM.1-AD.ARMY.MIL'].uri+"/commStatus?commUp=true&connectedAgentName=47-FSB.DISCOM.1-AD.ARMY.MIL"
    Cougaar::Communications::HTTP.get(uri)  
  end  

  do_action "Sleep", 5.minutes
  wait_for "Command", "ok"
  include "#{CIP}/csmart/lib/isat/post_stage_data.inc", "AFTER_NIC_RESTORE"
end
