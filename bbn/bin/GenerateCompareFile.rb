require 'find'

dir1 = ARGV[0]
dir2 = ARGV[1]
runNumber = ARGV[2]
compareFile = ARGV[3]

#need to check here for right number of args and error if wrong
(STDERR.puts "usage: GenerateCompareFile.rb first_input_directory second_input_directory run_number_of_second_input_directory compare_output_file"; exit 1) if ARGV.size<4

#open the output file
output = File.new(compareFile, "w")

Find.find(dir1) do |f|
	if FileTest.file?(f)
	fileName = File.basename(f)
	firstRunNumber = fileName =~ /of/
	if firstRunNumber != nil 
	fileName[firstRunNumber -1] = runNumber
	end
	output.print(f + "," + dir2 + "/" + fileName + "\n");
	else
	end
end