=begin script

include_path: al_data_compromise.rb
description: Insert the logistics data compromise into an UA agent.

=end

insert_after :after_stage_5 do
  do_action "GLMStimulator", "FCS-C2V-0.MCG1.1-UA.ARMY.MIL" do |glms|
    glms.inputFileName = "Compromise.dat.xml"
    glms.taskParserClass = "org.cougaar.logistics.plugin.utils.ALTaskParserWrapper"
    glms.update
  end
end

