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

