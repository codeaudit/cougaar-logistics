=begin experiment
####### CHANGE THE LAYOUT #########################
name: BaselineSD
description: BaselineSD
script: $CIP/csmart/scripts/definitions/BaselineTemplate-ExtOplan-W-ATDebug.rb
parameters:
  - run_count: 2
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-232a703v.plugins.rb
  - layout_file: $CIP/operator/layouts/FULL-UA-TC20-35H41N-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
  - script: $CIP/csmart/lib/logistics/postSDData.rb
  - script: $CIP/csmart/lib/logistics/providerAvail.rb
  - script: $CIP/csmart/lib/logistics/preSDData.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
