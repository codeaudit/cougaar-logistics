=begin script

include_path: relationship.rb
description: relationship dump

=end

insert_after :before_stage_4 do
  do_action "RelationshipServlet", "1-35-ARBN.2-BDE.1-AD.ARMY.MIL", "Relationship_Schedule", "Relationship_PreSD_after_stage3_1-35-ARBN.xml"
  include "#{CIP}/csmart/lib/isat/post_stage_data.inc", "PreSD_after_stage3"
end

insert_after :before_stage_6 do
  do_action "RelationshipServlet", "1-35-ARBN.2-BDE.1-AD.ARMY.MIL", "Relationship_Schedule", "Relationship_PreSD_after_stage5_1-35-ARBN.xml"
  include "#{CIP}/csmart/lib/isat/post_stage_data.inc", "PreSD_after_stage5"
end

