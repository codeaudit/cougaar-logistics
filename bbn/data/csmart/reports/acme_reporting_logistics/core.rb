require "./plugins/acme_reporting_logistics/DeletionLogs"
require "./plugins/acme_reporting_logistics/ShortfallTest"

module ACME
  module Plugins
    
    class ReportingLogistics
      extend FreeBASE::StandardPlugin
      
      def self.start(plugin)
        self.new(plugin)
        plugin.transition(FreeBASE::RUNNING)
      end
      
      attr_reader :plugin
      
      def initialize(plugin)
        @plugin = plugin
        @reporting = @plugin['/acme/reporting']
        load_template_engine
        @reporting.manager.add_listener(&method(:process_archive))
      end
      
      def load_template_engine
        @ikko = Ikko::FragmentManager.new
        @ikko.base_path = File.join(@plugin.plugin_configuration.base_path, "templates")
      end

      def process_archive(archive)
        puts "Processing an archive #{archive.base_name}"
        begin
          DeletionLogs.new(archive, @plugin, @ikko).perform
          puts "DELETIONS"
          ShortfallTest.new(archive, @plugin, @ikko).perform
          puts "SHORTFALL"
        rescue
          error_str =  "Exception<BR>\n#{Time.now}<BR>\n#{$!.message}<BR>\n#{$!.backtrace.join("<BR>\n")}"
          puts error_str
          @plugin.log_error << error_str
          archive.add_report("Exception", @plugin.plugin_configuration.name) do |report|
            report.open_file("Exception.html", "text/html", "Exception") do |out|
              out.puts "<html>"
              out.puts "<title>Exception</title>"
              out.puts error_str
              out.puts "</html>"
            end
            report.failure
          end          
        end
      end
    end
  end       
end  

