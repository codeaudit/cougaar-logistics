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


# usage:  process_oplog_ammorate_sql.pl oplog_ammorate_p1.csv > oplog_ammorate_tmp.sql
#         mysql -u kratkiew -p mycoug_ammo < oplog_ammorate_tmp.sql

print <<EOF;
DROP TABLE oplog_ammorate_tmp;
CREATE TABLE oplog_ammorate_tmp (
  echelon varchar(9) default NULL,
  lin varchar(6) default NULL,
  dodic varchar(4) default NULL,
  optempo varchar(20) default NULL,
  rounds_per_day decimal(14,10) default NULL
) TYPE=MyISAM;

EOF

open(I, $ARGV[0]);
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);
    print "INSERT INTO oplog_ammorate_tmp VALUES (\'$fields[0]\',\'$fields[2]\',\'$fields[3]\',\'$fields[1]\',$fields[4]);\n";
}
close(I);

