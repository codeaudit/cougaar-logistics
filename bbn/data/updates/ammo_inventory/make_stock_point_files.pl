#! /usr/bin/perl.exe

# make_stock_point_files.pl <relative path to consumer inventory files>
#                           <path & name of supplyunits.csv> 
#                           <path to output directory>
#
# Example:
#
# make_stock_point_files.pl ../results_step_5  
#                           supply_units.csv 
#                           ../results_step_6

$lev2 = "Level2Ammunition";

open(LIST, "ls $ARGV[0]/*.inv |");
while(chop($file=<LIST>)) {

    open(I, $file);
    while(chop($line=<I>)) {
	@fields = split(/,/, $line);
	if ($fields[0] ne $lev2) {
	    $dodics{$fields[0]} = 1;
	}
    }
    close(I);
}
close(LIST);


@dodics_sorted = sort keys(%dodics);

open(I, $ARGV[1]);
while(chop($unit=<I>)) {
    next if $.==1; # skip first line
	open(OUT, ">$ARGV[2]/${unit}_ammunition.inv");

    foreach (@dodics_sorted) {
	print OUT "$_,4000.0,0.0,0.0,0.0\n";
    }
    print OUT "$lev2,0.0,0.0,0.0,0.0\n";
    close OUT;
}
close(I);

