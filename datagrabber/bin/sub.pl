#!/usr/bin/perl -w
# $Header: /opt/rep/cougaar/logistics/datagrabber/bin/sub.pl,v 1.3 2004-03-18 20:50:05 mthome Exp $

# <copyright>
#  
#  Copyright 2001-2004 BBNT Solutions, LLC
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
