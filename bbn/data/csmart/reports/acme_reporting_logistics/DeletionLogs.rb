require "parsedate"

# 2004-05-05 11:57:29,304 ERROR - TaskDeletionPlugin - 1-AD.ARMY.MIL: ,1-AD.ARMY.MIL,Fri Sep 30 00:00:00 GMT 2005,185, tasks deleted this cycle

module ACME
  module Plugins
    
    class DeletionLogs
      AgentDeletion = Struct.new("AgentDeletion", :name, :numTasks)

      def initialize(archive, plugin, ikko)
        @archive = archive
        @ikko = ikko
        @plugin = plugin
        @stage1Hash = Hash.new
        @stage2Hash = Hash.new
        @stage3Hash = Hash.new
        @stage4Hash = Hash.new
        @stage5Hash = Hash.new
        @stage6Hash = Hash.new
        @stage2Time = Time.mktime(2005, "aug", 14,0,0,0)
        @stage3Time = Time.mktime(2005, "sep", 28,0,0,0)
        @stage4Time = Time.mktime(2005, "oct", 10,0,0,0)
        @stage5Time = Time.mktime(2005, "oct", 14,0,0,0)
        @stage6Time = Time.mktime(2005, "oct", 15,0,0,0)
        @societyDeletions = Hash.new
      end

      def perform
        societyDeletions = Hash.new
        log_files = @archive.files_with_description(/Log4j/)
        log_files.each do |log_file|
          deletion_totals(log_file.name)
        end
        if (!@societyDeletions.empty?) then
          @archive.add_report("Deletion", @plugin.plugin_configuration.name) do |report|
              
            output = html_output(@societyDeletions)
            report.open_file("deletion.html", "text/html", "Deletion information") do |file|
              file.puts output
            end
              
            output = create_description
            report.open_file("deletion_description.html", "text/html", "Agent Deletion Totals description") do |file|
              file.puts output
            end
            report.success
          end
        end
      end

      def deletion_totals(log_file)
        thisAgent = nil
        IO.foreach(log_file) do |line|
          if line =~ /.*tasks deleted this cycle/ 
            fields = line.split(',')
            pd = ParseDate.parsedate(fields[3])
            time = Time.mktime(*pd)
            name = fields[2]
            num = fields[4].to_i
            if @societyDeletions.has_key?(name)
              thisAgent = @societyDeletions.fetch(name)
              thisAgent.numTasks += num
            else
              thisAgent = AgentDeletion.new
              thisAgent.name = name
              thisAgent.numTasks = num
              @societyDeletions[thisAgent.name] = thisAgent
            end
            if time < @stage2Time
              if @stage1Hash.has_key?(name)
                thisAgent = @stage1Hash.fetch(name)
                thisAgent.numTasks += num
              else
                thisAgent = AgentDeletion.new
                thisAgent.name = name
                thisAgent.numTasks = num
                @stage1Hash[thisAgent.name] = thisAgent
              end
            elsif time < @stage3Time
              if @stage2Hash.has_key?(name)
                thisAgent = @stage2Hash.fetch(name)
                thisAgent.numTasks += num
              else
                thisAgent = AgentDeletion.new
                thisAgent.name = name
                thisAgent.numTasks = num
                @stage2Hash[thisAgent.name] = thisAgent
              end
            elsif time < @stage4Time
              if @stage3Hash.has_key?(name)
                thisAgent = @stage3Hash.fetch(name)
                thisAgent.numTasks += num
              else
                thisAgent = AgentDeletion.new
                thisAgent.name = name
                thisAgent.numTasks = num
                @stage3Hash[thisAgent.name] = thisAgent
              end
            elsif time < @stage5Time
              if @stage4Hash.has_key?(name)
                thisAgent = @stage4Hash.fetch(name)
                thisAgent.numTasks += num
              else
                thisAgent = AgentDeletion.new
                thisAgent.name = name
                thisAgent.numTasks = num
                @stage4Hash[thisAgent.name] = thisAgent
              end
            elsif time < @stage6Time
              if @stage5Hash.has_key?(name)
                thisAgent = @stage5Hash.fetch(name)
                thisAgent.numTasks += num
              else
                thisAgent = AgentDeletion.new
                thisAgent.name = name
                thisAgent.numTasks = num
                @stage5Hash[thisAgent.name] = thisAgent
              end
            else 
              if @stage6Hash.has_key?(name)
                thisAgent = @stage6Hash.fetch(name)
                thisAgent.numTasks += num
              else
                thisAgent = AgentDeletion.new
                thisAgent.name = name
                thisAgent.numTasks = num
                @stage6Hash[thisAgent.name] = thisAgent
              end
            end # elsif
          end   # if line matches
        end     # IO foreach
      end       # deletion_totals method
      
      def html_output(data)
        ikko_data = {}
        ikko_data["id"] = @archive.base_name
        ikko_data["description_link"] = "deletion_description.html"
        
        headers = ["Agent Name", "Total Deleted Tasks", "Stage 1", "Stage 2", "Stage 3", "Stage 4", "Stage 5", "Stage 6"]
        header_string = ""
        headers.each do |h|
          header_string << @ikko["header_template.html", {"data"=>h}]
        end
        table_string = @ikko["row_template.html", {"data"=>header_string}]
        
        data.sort
        data.each_value{ |value| 
          agent_row = @ikko["cell_template.html", {"data"=>value.name,"options"=>""}]
          agent_row << @ikko["cell_template.html", {"data"=>value.numTasks,"options"=>""}]
          if @stage1Hash.has_key?(value.name)
            stagenum = @stage1Hash.fetch(value.name).numTasks
          else stagenum = 0
          end
          agent_row << @ikko["cell_template.html", {"data"=>stagenum,"options"=>""}]
          if @stage2Hash.has_key?(value.name)
            stagenum = @stage2Hash.fetch(value.name).numTasks
          else stagenum = 0
          end
          agent_row << @ikko["cell_template.html", {"data"=>stagenum,"options"=>""}]
          if @stage3Hash.has_key?(value.name)
            stagenum = @stage3Hash.fetch(value.name).numTasks
          else stagenum = 0
          end
          agent_row << @ikko["cell_template.html", {"data"=>stagenum,"options"=>""}]
          if @stage4Hash.has_key?(value.name)
            stagenum = @stage4Hash.fetch(value.name).numTasks
          else stagenum = 0
          end
          agent_row << @ikko["cell_template.html", {"data"=>stagenum,"options"=>""}]
          if @stage5Hash.has_key?(value.name)
            stagenum = @stage5Hash.fetch(value.name).numTasks
          else stagenum = 0
          end
          agent_row << @ikko["cell_template.html", {"data"=>stagenum,"options"=>""}]
          if @stage6Hash.has_key?(value.name)
            stagenum = @stage6Hash.fetch(value.name).numTasks
          else stagenum = 0
          end
          agent_row << @ikko["cell_template.html", {"data"=>stagenum,"options"=>""}]
          options = ""
           if value.name =~ /.*UA\.ARMY\.MIL/
             options << "BGCOLOR=#CCCCCC"
           else
             options << "BGCOLOR=#FFCCFF"
           end
          table_string << @ikko["row_template.html", {"data"=>agent_row,"options"=>options}]
        }
        ikko_data["table"] = table_string
        return @ikko["deletion.html", ikko_data]
      end

      def create_description
        ikko_data = {}
        ikko_data["name"]="Agent Deletion Totals"
        ikko_data["title"] = "Agent Deletion Totals Description"
        ikko_data["description"] = "Displays how many tasks were deleted from each agent during the run."
        success_table = {"success"=>"Currently this report is always successful",
                         "partial"=>"not used",
                         "fail"=>"not used"}
        ikko_data["table"] = @ikko["success_template.html", success_table]
        return @ikko["description.html", ikko_data]
      end
    end
  end
end
