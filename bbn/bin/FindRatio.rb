require "rexml/document"
include REXML

input = ARGV[0]
file = File.new(input)

completion = 1.00
completion.to_f

doc = Document.new file

doc.elements.each("CompletionSnapshot/SimpleCompletion/Ratio") do |element|
agent = element.parent.attributes["agent"]
ratio = element.text
puts agent + "\t" + ratio if ratio.to_f < completion.to_f
end


