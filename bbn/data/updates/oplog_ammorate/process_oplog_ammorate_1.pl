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


# usage:  process_oplog_ammorate_raw_sql.pl OPLOG_AMMORATE.csv  > oplog_ammorate_p0.sql
#         mysql -u kratkiew -p mycoug_ammo < oplog_ammorate_p0.sql

print <<EOF;
DROP TABLE if exists oplog_ammorate_tmp0;
CREATE TABLE oplog_ammorate_tmp0 (
  echelon varchar(9) default NULL,
  posture varchar(1) default NULL,
  lin varchar(6) default NULL,
  dodic varchar(4) default NULL,
  rounds_per_day decimal(14,10) default NULL
) TYPE=MyISAM;

EOF

$echelon{A} = "COMPANY";
$echelon{B} = "BATTALION";
$echelon{D} = "DIVISION";
$echelon{C} = "BRIGADE";
$echelon{E} = "ABOVE DIV";
$echelon{F} = "THTR-ARMY";
$echelon{G} = "THTR-JNT";

$posture{A} = 1;
$posture{D} = 2;
$posture{P} = 3;
$posture{H} = 4;
$posture{S} = 5;
$posture{R} = 6;
$posture{U} = 7;

open(I, $ARGV[0]);
#print "echelon,posture,lin,dodic,rounds_per_day\n";
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);
    next if $fields[3] eq "BULK";
    print "INSERT INTO oplog_ammorate_tmp0 VALUES (\'$echelon{$fields[1]}\',\'$posture{$fields[2]}\',\'$fields[3]\',\'$fields[4]',$fields[5]);\n";
}
close(I);

