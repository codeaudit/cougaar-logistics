require "find"
require "rexml/document"
include REXML

dir = ARGV[0]

def parse(f)
	completion = 1.00
	completion.to_f
	file = File.new(f)	
	doc = Document.new file
	doc.elements.each("CompletionSnapshot/SimpleCompletion/Ratio") do |element|
	puts f + "\t" + element.parent.attributes["agent"] + "\t" +
	element.text if element.text.to_f < completion.to_f
	end
end

Find.find(dir) do |f|
	if FileTest.file?(f)
	parse(f) if /\.xml$/ =~ f
	else
	parse(f) if /\.xml$/ =~ f
	end
end


