require "set"

module ACME	
  module Plugins	
    
    class AgentShortfallData
      
      attr_reader :name, :supply_types, :error, :failed_fields, :partial_fields
      
      def initialize (name)
        @name = name
        @comp_data = {}
        @supply_types = []
        @error = ShortfallTest::SUCCESS
        @failed_fields = []
        @partial_fields = []
      end

      def []=(tag, value)
        if(tag == "SupplyType") 
          supply_types<<value
        else
          @comp_data[tag]=value
        end
      end
      
      def [](tag)
        if(tag == "SupplyTypes")
          supply_types_str = ""
          if(!supply_types.empty?)
            supply_types_str = supply_types.first
            1.upto(supply_types.size-1) { |supply_type| supply_type_str += ",#{supply_type}"}
          end
          return supply_types_str
        else
          return @comp_data[tag]
        end
      end
      
      def <=>(rhs)
        return @name <=> rhs.name
      end

      def same_supply_types?(bench_agent_data) 
        supply_types_set = Set.new(@supply_types)
        bench_types_set = Set.new(bench_agent_data.supply_types)
        diff_set = supply_types_set - bench_types_set
        return diff_set.empty?
      end

      
      def get_fields
        fields = []
        @comp_data.keys.sort.each do |field|
          fields << field unless fields.include?(field)
        end
        fields << "SupplyTypes"
        return fields
      end

      def set_error(field, lvl)
        @failed_fields << field if lvl == ShortfallTest::FAIL
        @partial_fields << field if lvl == ShortfallTest::PARTIAL
        @error = lvl if @error < lvl
      end

      def field_level(field)
        return ShortfallTest::FAIL if @failed_fields.include?(field)
        return ShortfallTest::PARTIAL if @partial_fields.include?(field)
        return ShortfallTest::SUCCESS
      end
    end
    
    class ShortfallTest

      SUCCESS = 0
      PARTIAL = 1
      FAIL = 2
      

      FileData = Struct.new("FileData", :agents, :totals, :effectedSupplyTypes)
      TOLERENCE = 0.10

      def initialize(archive, plugin, ikko)
        @archive = archive
        @plugin = plugin
        @ikko = ikko
      end
      
      def perform
        shortfall_files = @archive.files_with_description(/shortfall/)
        baseline_name = @archive.group_baseline
        puts "!!! #{baseline_name}"
        baseline = @archive.open_prior_archive(baseline_name)
        puts "*** #{baseline}"
        baseline_name = "Missing Baseline" if baseline.nil?
        shortfall_files.uniq.each do |shortfall_file|
          benchmark_pattern = Regexp.new(File.basename(shortfall_file.name))
          benchmark_file = nil
          benchmark_file = baseline.files_with_name(benchmark_pattern)[0] unless baseline.nil?
          report_name = "Sh-" + File.basename(shortfall_file.name, ".xml").gsub(/[^A-Z0-9]/, "")

          @archive.add_report(report_name, @plugin.plugin_configuration.name) do |report|
            data = get_file_data(File.new(shortfall_file.name))
            next if data.agents.empty?
            
            benchmark_data = nil
            benchmark_data = get_file_data(File.new(benchmark_file.name)) unless benchmark_file.nil?
            result = analyze(data, benchmark_data)
            
            if result == SUCCESS then
              report.success
            elsif result == PARTIAL then
              report.partial_success
            else
              report.failure
            end
            output = html_output(data, report_name, baseline_name)
            outfile = "#{report_name}.html"
            report.open_file(outfile, "text/html", "Agent shortfall tests for #{report_name}") do |file|
              file.puts output
            end

            output = create_description
            report.open_file("shortfall_description.html", "text/html", "Shortfall Report Description") do |file|
              file.puts output
            end
          end
        end
      end
      
      def get_file_data(file)
        data = FileData.new([], {}, [])
        curr_agent = nil
        file.each do|line|
          if line =~ /agent=/ then
            line.chomp!
            agent_name = line.split(/=/)[1]
            agent_name.delete!("\'>")
            curr_agent = AgentShortfallData.new(agent_name)
          elsif (line =~ /<(.+)>(.+)<\/\1>/) then
            if !curr_agent.nil? then
              if ($1 == "TimeMillis") then
                curr_agent["Time"] = Time.at($2.to_i/1000).strftime("%b/%d/%Y-%H:%M:%S")
              else
                curr_agent[$1] = $2.to_i
              end
            elsif ($1 == "SupplyType") 
	      data.effectedSupplyTypes << $2.to_s
            else
              data.totals[$1] = $2.to_i
            end
          elsif line =~ /\/SimpleShortfall/ then
            data.agents << curr_agent            
            curr_agent = nil
          elsif line =~ /EffectedSupplyTypes/ then
          elsif line =~ /AllEffectedSupplyTypes/ then
          end
        end
        data.agents.sort!
        return data
      end

      def analyze(data, benchmark)
        error = (benchmark.nil? ? PARTIAL : SUCCESS) #if there's no benchmark allow partial success at best
        if (!benchmark.nil?) then
          e = field_test("NumShortfall", data, benchmark, 0, 0.10)
          error = (error > e ? error : e)
          e = field_test("NumUnexpected", data, benchmark, 0, 0.10)
          error = (error > e ? error : e)
          e = effected_supply_types(data, benchmark)
          error = (error > e ? error : e)
	else 
	  e = unexpected_test(data)
          error = (error > e ? error : e)
        end
        return error
      end     

      def unexpected_test(data)
        error = SUCCESS
        data.agents.each do |agent|
          if (agent["NumUnexpected"] > 1  && agent["NumUnexpected"] < 3) then
            error = PARTIAL if error == SUCCESS
            agent.set_error("NumUnexpected", PARTIAL)
          elsif (agent["NumUnexpected"] >= 3) then
            error = FAIL
            agent.set_error("NumUnexpected", FAIL)
          end
        end
        return error
      end
      
      def field_test(field, data, benchmark, pass_tol, partial_tol)
        error = SUCCESS
        data.agents.each do |agent|
          benchmark_agent = (benchmark.agents.collect{|x| agent.name == x.name ? x : nil}.compact)[0]
          if benchmark_agent.nil? then
	    error = FAIL
            agent.set_error(field, FAIL)
	  else
            low_pass_bound = (benchmark_agent[field] * (1 - pass_tol)).to_i
            up_pass_bound = (benchmark_agent[field] * (1 + pass_tol)).to_i
            low_partial_bound = (benchmark_agent[field] * (1 - partial_tol)).to_i
            up_partial_bound = (benchmark_agent[field] * (1 + partial_tol)).to_i
            pass_range = Range.new(low_pass_bound, up_pass_bound)
            partial_range = Range.new(low_partial_bound, up_partial_bound)
            
            if (!partial_range.include?(agent[field])) then
              error = FAIL
              agent.set_error(field, FAIL)
            elsif (!pass_range.include?(agent[field])) then
              error = PARTIAL if error == SUCCESS
              agent.set_error(field, PARTIAL)
            end
          end
        end
        return error
      end

      def effected_supply_types(data,benchmark)
	error = SUCCESS
	data.agents.each do |agent|
          benchmark_agent = (benchmark.agents.collect{|x| agent.name == x.name ? x : nil}.compact)[0]
          if benchmark_agent.nil? then
            error = FAIL
            agent.set_error("SupplyTypes", FAIL)
          elsif (!agent.same_supply_types?(benchmark_agent))
            error = FAIL
            agent.set_error("SupplyTypes", FAIL)
          end
        end
        return error
      end

      def html_output(data, stage, baseline)
        ikko_data = {}
        ikko_data["description_link"] = "shortfall_description.html"
        ikko_data["stage"] = stage
        ikko_data["baseline"] = baseline
        ikko_data["id"] = @archive.base_name
        ikko_data["totals"] = []
        data.totals.each_key do |key|
          ikko_data["totals"] << "#{key}:  #{data.totals[key]}"        
        end
        headers = ["Agent Name"]
        fields = data.agents[0].get_fields
        headers << fields
        headers.flatten!
        header_row = ""
        headers.each do |header|
          header_row << @ikko["header_template.html", {"data"=>header.gsub(/Num/, ""),"options"=>""}]
        end
        ikko_data["bad"] = create_bad_table(data.agents, fields, header_row)        
        ikko_data["table"] = create_full_table(data.agents, fields, header_row)        

        return @ikko["shortfall_report.html", ikko_data]
      end

      def color(agent, field)
        lvl = FAIL
        if (field == "NAME") then
          lvl = agent.error
        else
          lvl = agent.field_level(field)
        end

        return "BGCOLOR=#00DD00" if lvl == SUCCESS
        return "BGCOLOR=#FFFF00" if lvl == PARTIAL
        return "BGCOLOR=#FF0000"
      end

      def create_agent_row(agent, fields)
        agent_row = @ikko["cell_template.html", {"data"=>agent.name,"options"=>color(agent, "NAME")}]
        fields.each do |key|
          val = agent[key]
          val = sprintf("%.4f", val) if val.class.name == "Float"
          agent_row << @ikko["cell_template.html", {"data"=>val,"options"=>color(agent, key)}]
        end
        return agent_row
      end

      def create_bad_table(agents, fields, header_row)
        table_string = @ikko["row_template.html", {"data"=>header_row,"options"=>""}]
        bad_agents = agents.collect{|x| (x.error != SUCCESS ? x : nil)}
        bad_agents.compact!
        return "" if bad_agents.empty?
        bad_agents.each do |agent|
          agent_row = create_agent_row(agent, fields)
          table_string << @ikko["row_template.html", {"data"=>agent_row,"options"=>""}]
        end
        return table_string
      end

      def create_full_table(agents, fields, header_row)
        table_string = @ikko["row_template.html", {"data"=>header_row,"options"=>""}]
        agents.each do |agent|
          agent_row = create_agent_row(agent, fields)
          table_string << @ikko["row_template.html", {"data"=>agent_row,"options"=>""}]
        end
        return table_string
      end


      def create_description
        ikko_data = {}
        ikko_data["name"]="Shortfall Report"
        ikko_data["title"] = "Shortfall Report Description"
        ikko_data["description"] = "Creates a table from the information in shortfall report xml files.  Currently the test"
        ikko_data["description"] << " compares the fields of the runs xlm against the baseline.  The fields that are compared are"
        ikko_data["description"] << " NumShortfall, NumUnexpected, and the SupplyTypes fields.  If there is no baseline it just checks"
        ikko_data["description"] << " whether the NumUnexpected is non-zero."
        ikko_data["description"] << " An agent is green if it has a ratio of 1.0 and exactly matches the baseline in all the fields"
        ikko_data["description"] << " An agent is yellow is it has a ratio of at least 0.95 and is within 10% of the baseline in the"
        ikko_data["description"] << " NumShortfall and NumUnexpected.  An agent is red if there is no matching baseline agent or when"
        ikko_data["description"] << " comparing the NumShortfall and NumUnexpected field it is greater than 10% of the baseline.  An"
        ikko_data["description"] << " An agent may also be red if the effected supply types do not match the basline exactly."
        ikko_data["description"] << " If the benchmark data cannot be found, the row will be green if the NumUnexpected for that agent"
        ikko_data["description"] << " zero.  The row will be yellow if there are less than 3 NumUnexpected, and the field will be red"
        ikko_data["description"] << " if more than 3"

        success_table = {"success"=>"Every agent matches the baseline exactly or if no benchmark there is no unexpected shortfall",
                         "partial"=>"Evety agent matches within 10% of num shortfall and num unexpected, or no baseline and less than 3 unexpected shortfall",
                         "fail"=>"All other cases"}
        ikko_data["table"] = @ikko["success_template.html", success_table]
        return @ikko["description.html", ikko_data]
      end
    end
  end
end
