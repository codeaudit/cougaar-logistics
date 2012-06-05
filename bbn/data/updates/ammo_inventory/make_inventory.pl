#! /usr/bin/perl.exe

# <copyright>
#  
#  Copyright 2004 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects
#  Agency (DARPA).
# 
#  You can redistribute this software and/or modify it under the
#  terms of the Cougaar Open Source License as published on the
#  Cougaar Open Source Website (www.cougaar.org).
# 
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
# </copyright>


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
$lev2 = "Level2Ammunition";

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
	    print OUT "$lev2,0.0,0.0,0.0,0.0\n";
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
print OUT "$lev2,0.0,0.0,0.0,0.0\n";
close OUT;
close(I);

#print %cl;
