=begin experiment
####### CHANGE THE LAYOUT #########################
name: Baseline
description: Baseline
script: $CIP/csmart/scripts/definitions/BaselineTemplate.rb
parameters:
  - run_count: 3
  - society_file: $CIP/csmart/config/societies/ua/small-tc20-17a12v.plugins.rb
  - layout_file: $CIP/operator/T20-SMALL-UA-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics

include_scripts:
  - script: $CIP/csmart/scripts/definitions/clearPnLogs.rb
  - script: $CIP/csmart/scripts/definitions/datagrabber_include.rb
  - script: $CIP/csmart/scripts/definitions/sdDataDump.rb
  - script: $CIP/csmart/scripts/definitions/providerAvail.rb
  - script: $CIP/csmart/scripts/definitions/relationship.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
