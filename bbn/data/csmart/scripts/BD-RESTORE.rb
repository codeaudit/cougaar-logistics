=begin experiment

name: BD-RESTORE
description: Stage-5-Rehydration
script: RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: /OperatorArchive/BD-SAVE-ROLLBACKTEST.tar.gz
  - archive_dir: $CIP/Logs
  - stages:
     - 5
 
include_scripts:
  - script: clearLogs.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
