=begin script

include_path: providerAvail.rb
description: Marks the 47-FSB as unavailable as a fuel provider for a period of time. 

=end

insert_after :before_stage_4 do
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "47-FSB.DISCOM.1-AD.ARMY.MIL", "FuelSupplyProvider", "unavailable", "10/01/2005", "03/25/2006"
  wait_for "SocietyQuiesced"
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "47-FSB.DISCOM.1-AD.ARMY.MIL", "AmmunitionProvider", "unavailable", "10/01/2005", "03/25/2006"  
  wait_for "SocietyQuiesced"
end

insert_after :before_stage_6 do
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "47-FSB.DISCOM.1-AD.ARMY.MIL", "FuelSupplyProvider", "available", "11/15/2005", "03/25/2006"
  wait_for "SocietyQuiesced"
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "47-FSB.DISCOM.1-AD.ARMY.MIL", "AmmunitionProvider", "available", "11/15/2005", "03/25/2006"
  wait_for "SocietyQuiesced"
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "125-FSB.DISCOM.1-AD.ARMY.MIL", "FuelSupplyProvider", "unavailable", "11/23/2005", "03/25/2006"
  wait_for "SocietyQuiesced"
  do_action "InfoMessage", "### Starting Dynamic SD perturbation ###"
  do_action "DynamicSD", "125-FSB.DISCOM.1-AD.ARMY.MIL", "AmmunitionProvider", "unavailable", "11/23/2005", "03/25/2006"
  wait_for "SocietyQuiesced"
end