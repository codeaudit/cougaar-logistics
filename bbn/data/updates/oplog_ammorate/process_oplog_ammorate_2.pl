#! /usr/bin/perl.exe

# usage:  process_oplog_ammorate_raw.pl oplog_ammorate_p0.csv  > oplog_ammorate_p1.csv

open(I, $ARGV[0]);
print "echelon,optempo,lin,dodic,rounds_per_day\n";
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);
    $rate = $fields[4];
    if ($fields[1] == 1) {
	$optempo = "HIGH";
	$updaterate = $rate;
    } elsif ($fields[1] == 4) {
	$optempo = "MEDIUM";
	if ($rate == 0) {
	    $rate = $updaterate;
	} else {
	    $updaterate = $rate;
	}
    } elsif ($fields[1] == 7) {
	$optempo = "LOW";
	if ($rate == 0) {
	    $rate = $updaterate;
	} else {
	    $updaterate = $rate;
	}
    } else {
	if ($rate != 0) {
	    $updaterate = $rate;
	}
	next;
    }
    print "$fields[0],$optempo,$fields[2],$fields[3],$rate\n";
}
close(I);

