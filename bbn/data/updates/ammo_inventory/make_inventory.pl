#! /usr/bin/perl.exe

#make_inventory.pl compiled_combat_loads.csv unit_equipment_and_dodics.csv 


# Load combat loads
open(I, $ARGV[0]);
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);
    if (! $cl{"$fields[1]$fields[2]"}) {
	$cl{"$fields[1]$fields[2]"} = $fields[3];
    }
    if (! $cl{"$fields[1]$fields[2]$fields[5]"}) {
	$cl{"$fields[1]$fields[2]$fields[5]"} = $fields[3];
    }
}
close(I);

#print %cl;

# Load unit equipment and compute inventory
open(I, $ARGV[1]);
$last_org = "";
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);

    $org = $fields[0];
    $dodic = $fields[3];
    $lin = $fields[1];
    $qty = $fields[2];
    $wt = $fields[4];

    if ($org ne $last_org) {
	if ($last_org ne "") {
	    print OUT "DODIC/$last_dodic,$bl,0.0,0.0,0.0\n";
	    close OUT;
	    $bl = 0;
	}
	open(OUT, ">${org}_ammunition.inv");
    } elsif ($dodic ne $last_dodic) {
	print OUT "DODIC/$last_dodic,$bl,0.0,0.0,0.0\n";
	$bl = 0;
    }

    @codes = split(/-/, $org);
    $code = $codes[@codes - 1];
    $cload = $cl{"$lin$dodic$code"};
    if (! $cload) {
	$cload = $cl{"$lin$dodic"};
    }
    if (! $cload) {
	print "Error: $line\n";
    }
    $bl = $bl + ($qty * $wt * $cload / 2000.0);
    $last_org = $org;
    $last_dodic = $dodic
}
print OUT "DODIC/$last_dodic,$bl,0.0,0.0,0.0\n";
close OUT;
close(I);

#print %cl;
