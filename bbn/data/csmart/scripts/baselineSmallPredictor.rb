=begin experiment

name: BaselinePredictor
description: BaselinePredictor
script: $CIP/csmart/scripts/definitions/BaselineTemplate.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ad/SMALL-1AD-TC20.rb
  - layout_file: $CIP/operator/layouts/SMALL-1AD-TC20-PREDICTOR-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
#  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
#    parameters:
#      - location: after_stage_1
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: before_stage_3
  - script: $CIP/csmart/lib/logistics/disable_network_interfaces.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
