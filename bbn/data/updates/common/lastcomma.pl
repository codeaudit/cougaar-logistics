#! /usr/bin/perl.exe

open(I, $ARGV[0]);
while(chop($line=<I>)) {
    if (! ($line =~ /^.*\,$/)) {
	$line = "${line},";
    }
    print "$line\n";
}
close(I);

