#!/usr/bin/perl -w
# $Header: /opt/rep/cougaar/logistics/datagrabber/bin/sub.pl,v 1.2 2003-02-03 22:27:58 mthome Exp $

# <copyright>
#  Copyright 2001-2003 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
# 
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
# 
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>


unless (scalar(@ARGV) >= 3) { die "Syntax: $0 <search> <replace> <files>\n"; }

$find = $ARGV[0];
$replace = $ARGV[1];

@files = @ARGV[2 .. $#ARGV];

print "find was";

foreach $_ (@files) {
  SearchAndReplace ($_);
}

sub SearchAndReplace{
  open(FILE, "<$_");
  @ORG = <FILE>;
  close FILE;
  foreach(@ORG){
     s/$find/$replace/eg;
  }
  # print to file
  open(FILE, ">$_");
  foreach(@ORG){ print FILE $_; }
  close FILE;
  print "File: $_ ... was successfully modified.\n\b";
}
