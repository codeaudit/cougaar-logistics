#!/usr/bin/perl
# -*- Perl -*-

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

# Parse quiescence detection logging output to Gantt CSV.
#
# Run with "--help" for usage.

my $suffix=shift(@ARGV);
my $offset=shift(@ARGV);

unless ($offset =~ /^-?\d+$/) {
  print STDERR <<EOF;
Usage:
  $0 SUFFIX OFFSET [FILES]
where:
  SUFFIX   string to append to each name
  OFFSET   millisecond time shift for each date

Parse quiescence detection logging output to Gantt CSV.

Example:
  $0 '-BLAH' 0 *.log > qgantt.csv
input:
  12:34:56,789 INFO  - QuiescenceReportServiceProvider - NodeA: <node name="NodeA" quiescent="false".*
  19:20:21,220 INFO  - QuiescenceReportServiceProvider - NodeA: <node name="NodeA" quiescent="true".*
output:
  NodeA-BLAH, 45296789, , red,
  NodeA-BLAH, 69621220, , grey,
view:
  cat qgantt.csv |\\
    java\\
      -classpath \$CIP/lib/datagrabber.jar\\
      org.cougaar.mlm.ui.newtpfdd.gui.view.FileGanttChartView\\
      -
EOF
  exit(1);
}

while (<>) {

  next unless (/^
    (\d\d):(\d\d):(\d\d),(\d\d\d)\s
    INFO\s\s-\sQuiescenceReportServiceProvider\s-\s*
    ([a-zA-Z0-9-_]+)
    :\s*<node\sname="
    ([a-zA-Z0-9-_]+)
    "\squiescent="
    ([a-z]+)
    "
    /x);

  my($h,$m,$s,$u)=($1,$2,$3,$4);
  my($us)=($h*60*60*1000)+($m*60*1000)+($s*1000)+$u;

  my $node = $6;
  my $bool = $7;

  my $color = ($bool eq "true" ? "grey" : "red");

  $node = "$node$suffix";
  $us += $offset;

  print "$node, $us, , $color, \n"
}
